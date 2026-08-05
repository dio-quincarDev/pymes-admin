# Estrategia: Consolidación de Migraciones Core (18→1)

> Aplica a: `backend/core/src/main/resources/db/migration/`. Objetivo: fresh stage deploy.
>
> ✅ **EJECUTADO 2026-07-30**: V1–V18 → único `V1__core_schema.sql`. Unit tests (150) + integration (22) verdes sobre Postgres fresh (Testcontainers).

---

## Resumen Ejecutivo

Consolidar 18 migraciones de Flyway en un único `V1__core_schema.sql` idempotente (`IF NOT EXISTS`). Eliminar 2 índices redundantes. Corregir 2 sentencias sin protección `IF NOT EXISTS`.

---

## Problema actual

18 archivos de migración incremental acumulados durante el desarrollo de MVP:

| Archivo | Contenido | Origen |
|---------|-----------|--------|
| V1 | `tenant_setup` | Setup inicial |
| V2 | `industries`, `template_categories`, `template_locations` | Config |
| V3 | `products`, `product_presentations` | Productos |
| V4 | `providers`, `invoices`, `invoice_items` | Facturación |
| V5 | ALTER `invoice_items` + `expense_analysis` | Analítica |
| V6 | `idx_invoices_tenant_date` + `idx_invoice_items_product` | Indexes |
| V7 | ALTER `products` (campos gasto) | Productos extendido |
| V8 | UPDATE `products.category` (name→UUID) | Data migration |
| V9 | ALTER `providers` (contact, DROP ruc) | Providers extendido |
| V10 | ALTER `products` (provider_id FK) | Products-FK |
| V11 | ALTER `expense_analysis` (supplier JSONB) | Analytics extendido |
| V12 | `operating_expenses`, `loans`, `loan_payments`, `patrimony`, `daily_sales`, `tenant_financial_metrics` | Contabilidad |
| V13 | `idx_invoices_tenant_date_type`, `idx_loan_payments_loan_date` | Covering indexes |
| V14 | `idx_products_tenant_category`, `idx_products_active_tenant` | Search indexes |
| V15 | ALTER `invoice_items` (audit fields) | Audit |
| V16 | `idx_invoices_tenant_status_dates`, `idx_invoice_items_invoice_product` | Performance indexes |
| V17 | DROP `template_locations` | Cleanup |
| V18 | `template_units`, `template_payment_methods`, `template_products`, `template_product_presentations` | Template tables |

> **Nota de ejecución**: V17 y V18 se agregaron después de redactar esta estrategia. En la consolidación, `template_locations` NO se crea (estado final tras V17) y las tablas template de V18 SÍ se incluyen en la Sección 7.

---

## Estado final consolidado (2026-08-05)

| Migration | Contenido | Cambios |
|-----------|-----------|---------|
| V1 | Esquema consolidado: setup, providers, products, invoices (nullable provider, category, colaborador), expense_analysis (+financial_health), accounting, templates | Absorbe V3-V9: +category, +colaborador_id, +provider_id nullable, +costo_operativo_diario, +financial_health, fix idx_invoices_tenant_date_type, +FK template_product_presentations |
| V2 | Costos engine: collaboradores, gastos_fijos, config_laboral | Sin cambios |
| V3 | Performance indexes: partial (activo), covering (analytics), invoice number | Nuevo |

---

## Consolidación: 6 secciones en 1 archivo

### Sección 1 — Setup (V1+V2)

```sql
CREATE TABLE IF NOT EXISTS tenant_setup (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID NOT NULL UNIQUE,
    industry             VARCHAR(50),
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_tenant_setup_tenant_id ON tenant_setup(tenant_id);

CREATE TABLE IF NOT EXISTS industries (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS template_categories (
    id UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
    name VARCHAR(100) NOT NULL,
    parent_id UUID REFERENCES template_categories(id),
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_template_categories_industry ON template_categories(industry_code);
CREATE INDEX IF NOT EXISTS idx_template_categories_parent ON template_categories(parent_id);

CREATE TABLE IF NOT EXISTS template_locations (
    id UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_template_locations_industry ON template_locations(industry_code);
```

### Sección 2 — Products (V3+V7+V10+V14)

```sql
CREATE TABLE IF NOT EXISTS products (
    id         UUID PRIMARY KEY,
    tenant_id  UUID NOT NULL,
    name       VARCHAR(150) NOT NULL,
    sku        VARCHAR(50),
    category   VARCHAR(100),
    base_unit  VARCHAR(50),
    image_url  VARCHAR(500),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
-- idx_products_tenant: ELIMINADO — redundante con idx_products_tenant_category (V14)
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_tenant_sku ON products(tenant_id, sku) WHERE sku IS NOT NULL;

CREATE TABLE IF NOT EXISTS product_presentations (
    id          UUID PRIMARY KEY,
    product_id  UUID NOT NULL REFERENCES products(id),
    name        VARCHAR(100) NOT NULL,
    conversion  INTEGER NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_presentations_product ON product_presentations(product_id);
```

**ALTERs (V7+V10):**

```sql
ALTER TABLE core.products ADD COLUMN IF NOT EXISTS last_unit_price DECIMAL(12,4);
ALTER TABLE core.products ADD COLUMN IF NOT EXISTS total_investment DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE core.products ADD COLUMN IF NOT EXISTS last_purchase_date DATE;
ALTER TABLE core.products ADD COLUMN IF NOT EXISTS min_quantity DECIMAL(12,2);
ALTER TABLE core.products ADD COLUMN IF NOT EXISTS max_quantity DECIMAL(12,2);
ALTER TABLE core.products ADD COLUMN IF NOT EXISTS provider_id UUID REFERENCES core.providers(id);
```

**Indexes (V14):**

```sql
CREATE INDEX IF NOT EXISTS idx_products_tenant_category
    ON core.products(tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_products_active_tenant
    ON core.products(tenant_id)
    WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_products_provider ON core.products(provider_id);
```

### Sección 3 — Invoices (V4+V5+V6+V9+V15+V16)

```sql
CREATE TABLE IF NOT EXISTS providers (
    id         UUID PRIMARY KEY,
    tenant_id  UUID NOT NULL,
    name       VARCHAR(150) NOT NULL,
    ruc        VARCHAR(50),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_providers_tenant ON providers(tenant_id);

CREATE TABLE IF NOT EXISTS invoices (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    provider_id      UUID NOT NULL REFERENCES providers(id),
    invoice_number   VARCHAR(50) NOT NULL,
    issue_date       DATE NOT NULL,
    type             VARCHAR(20) NOT NULL,
    global_discount  DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_method   VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA',
    total            DECIMAL(12,2) NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_provider ON invoices(provider_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_invoices_number_tenant ON invoices(tenant_id, invoice_number);

CREATE TABLE IF NOT EXISTS invoice_items (
    id           UUID PRIMARY KEY,
    invoice_id   UUID NOT NULL REFERENCES invoices(id),
    product_id   UUID NOT NULL REFERENCES products(id),
    product_name VARCHAR(150) NOT NULL,
    quantity     DECIMAL(12,2) NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    discount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    subtotal     DECIMAL(12,2) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
-- idx_invoice_items_invoice: ELIMINADO — redundante con idx_invoice_items_invoice_product (V16)
```

**ALTERs (V5+V9+V15):**

```sql
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS presentacion_id UUID REFERENCES core.product_presentations(id);
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS conversion_factor INTEGER NOT NULL DEFAULT 1;
ALTER TABLE core.providers ADD COLUMN IF NOT EXISTS contact_name VARCHAR(150);
ALTER TABLE core.providers ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30);
ALTER TABLE core.providers ADD COLUMN IF NOT EXISTS contact_email VARCHAR(150);
ALTER TABLE core.providers DROP COLUMN IF EXISTS ruc;
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS cantidad_presentacion NUMERIC(19,6);
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS valor_presentacion NUMERIC(19,6);
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS precio_unitario_input NUMERIC(19,6);
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS descuento_input NUMERIC(19,6);
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS descuento_es_porcentaje BOOLEAN;
```

**Indexes (V6+V13+V16):**

```sql
CREATE INDEX IF NOT EXISTS idx_invoice_items_presentacion ON core.invoice_items(presentacion_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_product ON core.invoice_items(product_id);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_date ON core.invoices(tenant_id, issue_date);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_date_type
    ON core.invoices(tenant_id, issue_date, type)
    INCLUDE (total);
CREATE INDEX IF NOT EXISTS idx_loan_payments_loan_date
    ON core.loan_payments(loan_id, payment_date)
    INCLUDE (amount);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status_dates
    ON core.invoices(tenant_id, status, issue_date DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_product
    ON core.invoice_items(invoice_id, product_id);
```

### Sección 4 — Analytics (V5+V11)

```sql
CREATE TABLE IF NOT EXISTS expense_analysis (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    period      VARCHAR(7) NOT NULL,
    abc         JSONB,
    trend       JSONB,
    margin      JSONB,
    opex_pct    JSONB,
    projection  JSONB,
    alerts      JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_expense_analysis_tenant_period ON expense_analysis(tenant_id, period);
```

**ALTER (V11):**

```sql
ALTER TABLE core.expense_analysis ADD COLUMN IF NOT EXISTS supplier_comparison JSONB;
ALTER TABLE core.expense_analysis ADD COLUMN IF NOT EXISTS supplier_recommendations JSONB;
ALTER TABLE core.expense_analysis ADD COLUMN IF NOT EXISTS price_prediction JSONB;
```

### Sección 5 — Accounting (V12+V13)

```sql
CREATE TABLE IF NOT EXISTS core.operating_expenses (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    category      VARCHAR(50) NOT NULL,
    description   VARCHAR(255),
    amount        DECIMAL(12,2) NOT NULL,
    expense_date  DATE NOT NULL,
    payment_method VARCHAR(50),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_operating_expenses_tenant_date ON core.operating_expenses(tenant_id, expense_date);

CREATE TABLE IF NOT EXISTS core.loans (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    name               VARCHAR(150) NOT NULL,
    lender             VARCHAR(150),
    amount             DECIMAL(15,2) NOT NULL,
    interest_rate      DECIMAL(6,4),
    term_months        INTEGER,
    start_date         DATE NOT NULL,
    remaining_balance  DECIMAL(15,2) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    notes              TEXT,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_loans_tenant ON core.loans(tenant_id);

CREATE TABLE IF NOT EXISTS core.loan_payments (
    id               UUID PRIMARY KEY,
    loan_id          UUID NOT NULL REFERENCES core.loans(id),
    amount           DECIMAL(15,2) NOT NULL,
    interest_paid    DECIMAL(15,2) NOT NULL DEFAULT 0,
    principal_paid   DECIMAL(15,2) NOT NULL DEFAULT 0,
    payment_date     DATE NOT NULL,
    payment_method   VARCHAR(50),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_loan_payments_loan ON core.loan_payments(loan_id);

CREATE TABLE IF NOT EXISTS core.patrimony (
    tenant_id        UUID PRIMARY KEY,
    initial_capital  DECIMAL(15,2) NOT NULL DEFAULT 0,
    start_date       DATE,
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS core.daily_sales (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    sale_date     DATE NOT NULL,
    gross_amount  DECIMAL(12,2) NOT NULL,
    description   VARCHAR(255),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_daily_sales_tenant_date ON core.daily_sales(tenant_id, sale_date);

CREATE TABLE IF NOT EXISTS core.tenant_financial_metrics (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    period             VARCHAR(7) NOT NULL,
    total_income       DECIMAL(15,2) DEFAULT 0,
    cost_of_goods      DECIMAL(15,2) DEFAULT 0,
    operating_expenses DECIMAL(15,2) DEFAULT 0,
    loan_payments      DECIMAL(15,2) DEFAULT 0,
    total_expenses     DECIMAL(15,2) DEFAULT 0,
    gross_margin       DECIMAL(15,2) DEFAULT 0,
    gross_margin_pct   DECIMAL(10,4) DEFAULT 0,
    operating_margin   DECIMAL(15,2) DEFAULT 0,
    operating_margin_pct DECIMAL(10,4) DEFAULT 0,
    net_margin         DECIMAL(15,2) DEFAULT 0,
    net_margin_pct     DECIMAL(10,4) DEFAULT 0,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_metrics_tenant_period UNIQUE (tenant_id, period)
);
```

### Sección 6 — Data migration (V8)

```sql
-- Idempotente: solo actualiza filas donde category NO es UUID
UPDATE core.products
SET category = tc.id::text
FROM template_categories tc, core.tenant_setup ts
WHERE core.products.tenant_id = ts.tenant_id
  AND ts.industry = tc.industry_code
  AND tc.name = core.products.category
  AND core.products.category IS NOT NULL
  AND core.products.category !~ '^[0-9a-f]{8}-';
```

---

## Índices eliminados (redundantes)

| Índice | Origen | Motivo de eliminación |
|--------|--------|-----------------------|
| `idx_products_tenant ON products(tenant_id)` | V3 | Cubierto por `idx_products_tenant_category ON products(tenant_id, category)` (V14) — tenant_id es prefijo del composite |
| `idx_invoice_items_invoice ON invoice_items(invoice_id)` | V4 | Cubierto por `idx_invoice_items_invoice_product ON invoice_items(invoice_id, product_id)` (V16) — invoice_id es prefijo del composite |

### Verificación de cobertura

**¿Se puede eliminar `idx_products_tenant`?**
Sí. `idx_products_tenant_category` tiene `(tenant_id, category)` como columnas. PostgreSQL puede usar este índice para queries `WHERE tenant_id = ?` sin filtro de category (scan del prefijo del B-tree).

**¿Se puede eliminar `idx_invoice_items_invoice`?**
Sí. `idx_invoice_items_invoice_product` tiene `(invoice_id, product_id)`. Para el join `invoice_items WHERE invoice_id = ?`, PostgreSQL puede usar este índice directamente (invoice_id es el prefijo).

---

## Índice total después de consolidación

| Tabla | Índices | Notas |
|-------|---------|-------|
| `tenant_setup` | 1 | `idx_tenant_setup_tenant_id` |
| `template_categories` | 2 | industry + parent |
| `template_units` | 1 | industry |
| `template_payment_methods` | 1 | industry |
| `template_products` | 1 | industry |
| `template_product_presentations` | 1 | fk |
| `products` | 4 | tenant_sku (unique), tenant_category, active_tenant (partial), provider |
| `product_presentations` | 1 | product |
| `providers` | 1 | tenant |
| `invoices` | 5 | tenant, provider, number_tenant (unique), tenant_date_type (covering), tenant_status_dates |
| `invoice_items` | 3 | presentacion, product, invoice_product |
| `expense_analysis` | 1 | tenant_period |
| `operating_expenses` | 1 | tenant_date |
| `loans` | 1 | tenant |
| `loan_payments` | 2 | loan, loan_date (covering) |
| `daily_sales` | 1 | tenant_date |
| `tenant_financial_metrics` | 0 | UNIQUE constraint (tenant_id, period) actúa como índice |
| **Total** | **28** | 22 non-unique + 3 unique + 2 covering + 1 fk |

**Antes de consolidación:** 30 índices (2 redundantes eliminados = 28 efectivos)

> `template_locations` ya no existe (dropeada por V17). `idx_invoices_tenant_date` de V6 quedó absorbido en `idx_invoices_tenant_date_type` (covering con INCLUDE total).

---

## Problemas corregidos en la consolidación

| Problema | Fix | Archivo original |
|----------|-----|------------------|
| V9 `DROP COLUMN ruc` sin `IF EXISTS` | Se agrega `DROP COLUMN IF EXISTS ruc` | V9__provider_contact.sql |
| V10 `ADD COLUMN provider_id` sin `IF NOT EXISTS` | Se agrega `ADD COLUMN IF NOT EXISTS provider_id` | V10__product_provider.sql |

---

## Deploy a stage

```bash
# Stage: volumen limpio, no hay flyway_schema_history
# Docker-compose levanta core → Flyway detecta V1__core_schema.sql
# Ejecuta todo el esquema de una vez (CREATE TABLE IF NOT EXISTS + ALTER TABLE IF NOT EXISTS)
# Startup time: ~segundo (el bottleneck real es la DB, no 16 vs 1 checksum)
```

### Si hay DB existente con V1–V16 (desarrollo local)

```bash
# 1. Reemplazar V1–V16 con V1__core_schema.sql consolidado
# 2. flyway repair (borra history de archivos que ya no existen)
# 3. flyway migrate (aplica V1 consolidado — todo IF NOT EXISTS = no-op)
```

---

## Trazabilidad V1–V18 → Consolidado

| Sección consolidada | Archivos origen |
|--------------------|----------------|
| 1. Setup | V1, V2, V17 (template_locations NO creada) |
| 2. Providers | V4, V9 |
| 3. Products | V3, V7, V10, V14 |
| 4. Invoices | V4, V5, V6, V15, V16 |
| 5. Analytics | V5, V11 |
| 6. Accounting | V12, V13 |
| 7. Template tables | V18 |
| 8. Data migration | V8 |

---

*Creado: 2026-07-24 | Ejecutado: 2026-07-30 | Consolidación de 18 migraciones core en `V1__core_schema.sql`*
