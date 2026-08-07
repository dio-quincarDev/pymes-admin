-- Consolidado V1–V9 → V1. Fresh stage deploy.
-- Idempotente (IF NOT EXISTS): local con DB existente = no-op tras flyway repair.

-- ============ 1. Setup ============

CREATE TABLE IF NOT EXISTS core.tenant_setup (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID NOT NULL UNIQUE,
    industry             VARCHAR(50),
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_tenant_setup_tenant_id ON core.tenant_setup(tenant_id);

CREATE TABLE IF NOT EXISTS core.industries (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS core.template_categories (
    id            UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES core.industries(code),
    name          VARCHAR(100) NOT NULL,
    parent_id     UUID REFERENCES core.template_categories(id),
    sort_order    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_template_categories_industry ON core.template_categories(industry_code);
CREATE INDEX IF NOT EXISTS idx_template_categories_parent ON core.template_categories(parent_id);

-- ============ 2. Providers ============

CREATE TABLE IF NOT EXISTS core.providers (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    name          VARCHAR(150) NOT NULL,
    contact_name  VARCHAR(150),
    contact_phone VARCHAR(30),
    contact_email VARCHAR(150),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_providers_tenant ON core.providers(tenant_id);

-- ============ 3. Products ============

CREATE TABLE IF NOT EXISTS core.products (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    name               VARCHAR(150) NOT NULL,
    sku                VARCHAR(50),
    category           VARCHAR(100),
    base_unit          VARCHAR(50),
    image_url          VARCHAR(500),
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    last_unit_price    DECIMAL(12,4),
    total_investment   DECIMAL(12,2) NOT NULL DEFAULT 0,
    last_purchase_date DATE,
    min_quantity       DECIMAL(12,2),
    max_quantity       DECIMAL(12,2),
    provider_id        UUID REFERENCES core.providers(id),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_products_tenant_sku ON core.products(tenant_id, sku) WHERE sku IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_tenant_category ON core.products(tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_products_active_tenant ON core.products(tenant_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_products_provider ON core.products(provider_id);

CREATE TABLE IF NOT EXISTS core.product_presentations (
    id         UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES core.products(id),
    name       VARCHAR(100) NOT NULL,
    conversion INTEGER NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_presentations_product ON core.product_presentations(product_id);

-- ============ 4. Invoices (V6: provider_id nullable, V8: category, V9: colaborador) ============

CREATE TABLE IF NOT EXISTS core.invoices (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    provider_id     UUID REFERENCES core.providers(id),
    invoice_number  VARCHAR(50) NOT NULL,
    issue_date      DATE NOT NULL,
    type            VARCHAR(20) NOT NULL,
    category        VARCHAR(50),
    global_discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_method  VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA',
    total           DECIMAL(12,2) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    colaborador_id  UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invoices_tenant ON core.invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_provider ON core.invoices(provider_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_invoices_number_tenant ON core.invoices(tenant_id, invoice_number);
CREATE INDEX IF NOT EXISTS idx_invoices_colaborador_id ON core.invoices(colaborador_id);

CREATE TABLE IF NOT EXISTS core.invoice_items (
    id                     UUID PRIMARY KEY,
    invoice_id             UUID NOT NULL REFERENCES core.invoices(id),
    product_id             UUID NOT NULL REFERENCES core.products(id),
    product_name           VARCHAR(150) NOT NULL,
    quantity               DECIMAL(12,2) NOT NULL,
    unit_price             DECIMAL(12,2) NOT NULL,
    discount               DECIMAL(12,2) NOT NULL DEFAULT 0,
    subtotal               DECIMAL(12,2) NOT NULL,
    presentacion_id        UUID REFERENCES core.product_presentations(id),
    conversion_factor      INTEGER NOT NULL DEFAULT 1,
    cantidad_presentacion  NUMERIC(19,6),
    valor_presentacion     NUMERIC(19,6),
    precio_unitario_input  NUMERIC(19,6),
    descuento_input        NUMERIC(19,6),
    descuento_es_porcentaje BOOLEAN,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_presentacion ON core.invoice_items(presentacion_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_product ON core.invoice_items(product_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_product ON core.invoice_items(invoice_id, product_id);

-- ============ 5. Analytics ============

CREATE TABLE IF NOT EXISTS core.expense_analysis (
    id                       UUID PRIMARY KEY,
    tenant_id                UUID NOT NULL,
    period                   VARCHAR(7) NOT NULL,
    abc                      JSONB,
    trend                    JSONB,
    margin                   JSONB,
    opex_pct                 JSONB,
    projection               JSONB,
    alerts                   JSONB,
    supplier_comparison      JSONB,
    supplier_recommendations JSONB,
    price_prediction         JSONB,
    financial_health         JSONB,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_expense_analysis_tenant_period ON core.expense_analysis(tenant_id, period);

-- ============ 6. Accounting ============

CREATE TABLE IF NOT EXISTS core.operating_expenses (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL,
    category       VARCHAR(50) NOT NULL,
    description    VARCHAR(255),
    amount         DECIMAL(12,2) NOT NULL,
    expense_date   DATE NOT NULL,
    payment_method VARCHAR(50),
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_operating_expenses_tenant_date ON core.operating_expenses(tenant_id, expense_date);

CREATE TABLE IF NOT EXISTS core.loans (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL,
    name              VARCHAR(150) NOT NULL,
    lender            VARCHAR(150),
    amount            DECIMAL(15,2) NOT NULL,
    interest_rate     DECIMAL(6,4),
    term_months       INTEGER,
    start_date        DATE NOT NULL,
    remaining_balance DECIMAL(15,2) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    notes             TEXT,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_loans_tenant ON core.loans(tenant_id);

CREATE TABLE IF NOT EXISTS core.loan_payments (
    id             UUID PRIMARY KEY,
    loan_id        UUID NOT NULL REFERENCES core.loans(id),
    amount         DECIMAL(15,2) NOT NULL,
    interest_paid  DECIMAL(15,2) NOT NULL DEFAULT 0,
    principal_paid DECIMAL(15,2) NOT NULL DEFAULT 0,
    payment_date   DATE NOT NULL,
    payment_method VARCHAR(50),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_loan_payments_loan ON core.loan_payments(loan_id);

CREATE TABLE IF NOT EXISTS core.patrimony (
    tenant_id       UUID PRIMARY KEY,
    initial_capital DECIMAL(15,2) NOT NULL DEFAULT 0,
    start_date      DATE,
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS core.daily_sales (
    id           UUID PRIMARY KEY,
    tenant_id    UUID NOT NULL,
    sale_date    DATE NOT NULL,
    gross_amount DECIMAL(12,2) NOT NULL,
    description  VARCHAR(255),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_daily_sales_tenant_date ON core.daily_sales(tenant_id, sale_date);

CREATE TABLE IF NOT EXISTS core.tenant_financial_metrics (
    id                     UUID PRIMARY KEY,
    tenant_id              UUID NOT NULL,
    period                 VARCHAR(7) NOT NULL,
    total_income           DECIMAL(15,2) DEFAULT 0,
    cost_of_goods          DECIMAL(15,2) DEFAULT 0,
    operating_expenses     DECIMAL(15,2) DEFAULT 0,
    loan_payments          DECIMAL(15,2) DEFAULT 0,
    total_expenses         DECIMAL(15,2) DEFAULT 0,
    gross_margin           DECIMAL(15,2) DEFAULT 0,
    gross_margin_pct       DECIMAL(10,4) DEFAULT 0,
    operating_margin       DECIMAL(15,2) DEFAULT 0,
    operating_margin_pct   DECIMAL(10,4) DEFAULT 0,
    net_margin             DECIMAL(15,2) DEFAULT 0,
    net_margin_pct         DECIMAL(10,4) DEFAULT 0,
    costo_operativo_diario DECIMAL(12,2),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_metrics_tenant_period UNIQUE (tenant_id, period)
);

-- ============ 7. Covering indexes ============

CREATE INDEX IF NOT EXISTS idx_loan_payments_loan_date
    ON core.loan_payments(loan_id, payment_date)
    INCLUDE (amount);

-- V5: status added for GASTO_OPERATIVO + PAGADA filter
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_date_type
    ON core.invoices(tenant_id, issue_date, type, status)
    INCLUDE (total);

CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status_dates
    ON core.invoices(tenant_id, status, issue_date DESC, created_at DESC);

-- ============ 8. Template tables ============

CREATE TABLE IF NOT EXISTS core.template_units (
    id            UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES core.industries(code),
    name          VARCHAR(100) NOT NULL,
    sort_order    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_template_units_industry ON core.template_units(industry_code);

CREATE TABLE IF NOT EXISTS core.template_payment_methods (
    id            UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES core.industries(code),
    name          VARCHAR(100) NOT NULL,
    sort_order    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_template_payment_methods_industry ON core.template_payment_methods(industry_code);

CREATE TABLE IF NOT EXISTS core.template_products (
    id            UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES core.industries(code),
    category_id   UUID,
    name          VARCHAR(150) NOT NULL,
    base_unit     VARCHAR(50),
    min_quantity  DECIMAL(12,2),
    max_quantity  DECIMAL(12,2),
    sort_order    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_template_products_industry ON core.template_products(industry_code);

CREATE TABLE IF NOT EXISTS core.template_product_presentations (
    id                  UUID PRIMARY KEY,
    template_product_id UUID NOT NULL REFERENCES core.template_products(id),
    name                VARCHAR(100) NOT NULL,
    conversion          INTEGER NOT NULL DEFAULT 1,
    sort_order          INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_template_product_presentations_fk ON core.template_product_presentations(template_product_id);
