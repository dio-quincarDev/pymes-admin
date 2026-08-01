# Core Service — Estado Actual

> **Estado (2026-07-09):** Todos los modulos de negocio implementados: `setup/`, `product/`, `invoice/`, `analytics/`, `gasto/`, `prestamo/`, `inversion/`, `venta/`, `accounting/`. Pendiente: `reportes/`.
> Ver `FUTURE_MODULES.md` para blueprints originales y `DAYLY_REPORTS_CORE_SOLUTIONS.md` para historial.

---

## Vision General

```
Frontend (PWA)
    | JWT
Gateway (8080)
    |
Core Service (8082)
├── setup/      configuracion/inventario inicial
├── product/    catalogo productos y presentaciones
├── invoice/    facturas de compra y proveedores
├── analytics/  10 motores de analisis + motor de salud financiera
├── gasto/      gastos operativos
├── prestamo/   prestamos y pagos
├── inversion/  patrimonio
├── venta/      ventas diarias
├── accounting/ metricas financieras consolidadas
└── reportes/   (pendiente)
```

Todos comunican via Spring Events (no bloqueantes). Paquete base: `core_pymes.*`.

---

## Modulos Implementados

### Setup (`core_pymes/setup/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `TenantSetup` (tenantId, industry, onboardingCompleted) |
| Service | `SetupService` / `SetupServiceImpl` (lazy init + completeOnboarding) |
| Endpoints | `GET /setup/{tenantId}`, `POST /setup/{tenantId}/onboarding`, `GET /setup/preview/{industry}` |
| Flyway | V1: `core.tenant_setup` |
| Templates | Categorias jerarquicas (3 niveles), unidades, productos precargados por industria |
| SKU auto | `P-0001` secuencial al copiar template_products en onboarding |

### Product (`core_pymes/product/`)

| Aspecto | Detalle |
|---------|---------|
| Entidades | `Producto` (UUID, soft-delete), `Presentacion` (FK, conversionFactor, soft-delete) |
| Endpoints | CRUD `/productos`, CRUD `/productos/{id}/presentaciones`, `DELETE /presentaciones/{id}` |
| Eventos | `ProductoCreadoEvent`, `PresentacionCreadaEvent` |
| Flyway | V3: `core.products`, `core.product_presentations` |

### Invoice (`core_pymes/invoice/`)

| Aspecto | Detalle |
|---------|---------|
| Entidades | `Factura` (UUID, items cascade ALL), `ItemFactura` (snapshot, subtotal, audit fields), `Proveedor` (soft-delete) |
| Endpoints | CRUD `/facturas`, `PUT /facturas/{id}?tenantId=`, `POST /facturas/{id}/pagar`, CRUD `/proveedores` |
| Eventos | `FacturaCreadaEvent` (escuchado por Analytics + debounce) |
| Invoice number | `F-PROV-{year}-{sequential:04d}` por tenant |
| Update logic | Solo `REGISTRADA`. `reverseProductStats()` → `clear items` → `buildItem()` con `InvoiceCalculator` → recalc total |
| Audit fields | `cantidad_presentacion`, `valor_presentacion`, `precio_unitario_input`, `descuento_input`, `descuento_es_porcentaje` (raw user input) |
| Flyway | V4: schema, V15: audit fields, V16: performance indexes |

### Analytics (`core_pymes/analytics/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `AnalisisGasto` (JSONB por tenant/periodo) |
| Endpoints | `GET /analytics/consultar`, `POST /analytics/recalcular` |
| Motores | ABC, tendencias, margenes, opex, proyeccion, alertas, comparativa proveedores, recomendaciones, predicciones, **salud financiera** |
| Flyway | V1 (esquema consolidado V1–V18), V4 (financial_health JSONB) |
| OLS predicción | `analisisProyeccionPrecios`: OLS en SQL (`regr_slope/regr_intercept/regr_r2`) sobre agrupación diaria; `predictedPrice = slope*(n+1)+intercept` (rn 1-based), filtro `data_points >= 3` en `HAVING`. Verificado con EXPLAIN ANALYZE: a escala PYME PG usa `idx_invoices_tenant` + hash join full-scan de `invoice_items` (óptimo, ~4ms); al crecer la tabla cambia solo al nested loop con `idx_invoice_items_invoice_product`. Sin índice nuevo — `invoice_items` no tiene `tenant_id`. |
| Guard `conversion_factor` | Todas las divisiones por `conversion_factor` usan `NULLIF(conversion_factor, 0)` — filas con factor 0 se excluyen del agregado en vez de lanzar division-by-zero que mata `ejecutarCompleto`. Test `conversionFactorCero_noRompeMotores`. |

#### Motor de Salud Financiera (Motor #10)

**Propósito:** Capa de interpretación que cruza datos de los 9 motores + accounting para producir inteligencia accionable: alertas críticas, señales de inversión, readiness de expansión.

**Arquitectura:**

```
Motores 1-9 (SQL CTE) ──┐
Accounting (márgenes) ───┼──▶ analisisSaludFinanciera() ──▶ financial_health JSONB
Productos (inventario) ──┘
```

No es un motor SQL independiente. Es un motor compuesto que **lee resultados pre-computados** de otros motores + `MetricasRepository` para cruzarlos y producir señales de negocio.

**Inputs (motores existentes):**

| Fuente | Campo leído | Para qué |
|--------|------------|----------|
| `MetricasRepository` | `grossMarginPct`, `operatingMarginPct`, `netMarginPct` | Scores de rentabilidad |
| `MetricasRepository` | `loanPayments`, `operatingMargin` | Ratio deuda/margen |
| `AnalisisGasto.abc` | Productos categoría A/B/C | Concentración de gasto |
| `AnalisisGasto.supplierComparison` | Distribución por proveedor | Concentración de proveedor |
| `AnalisisGasto.trend` | `pctChange` por producto | Estabilidad de precios |
| `AnalisisGasto.alerts` | Alertas existentes | Severidad combinada |
| `ProductoRepository` | `lastPurchaseDate`, `totalInvestment` | Productos muertos |

**Señales que produce:**

##### 🔴 Críticas (matan el negocio)

| Señal | Criterio | Datos |
|-------|----------|-------|
| `NEGATIVE_OPERATING_MARGIN` | `operatingMarginPct < 0` | Accounting — cada venta genera pérdida neta |
| `MARGIN_EROSION` | `grossMarginPct` bajando >15% vs período anterior (3+ meses) | Accounting — pouvoir de fijación de precio colapsando |
| `SUPPLIER_CONCENTRATION` | 1 proveedor >50% del gasto total | supplierComparison — riesgo de interrupción |
| `OVER_LEVERAGED` | `loanPayments / operatingMargin > 0.30` | Accounting — deuda insostenible |
| `OPEX_CREEP` | gastos operativos creciendo más rápido que ingresos, 3+ meses | Accounting histórico — ineficiencia creciente |
| `DEAD_INVENTORY` | productos con inversión alta + 0 compras >60d | Productos + ABC — capital estancado |

##### 🟢 Inversión (es seguro invertir)

| Señal | Threshold |
|-------|-----------|
| `HEALTHY_MARGIN_STACK` | Bruto >30% Y Operativo >10% Y Neto >5% |
| `POSITIVE_CASH_FLOW` | Operating margin positivo 3+ meses consecutivos |
| `LOW_CONCENTRATION` | Ningún proveedor >30%, ningún producto >20% del gasto |
| `DEBT_CAPACITY` | Loan payments <15% del margen operativo |

##### 🟣 Expansión (listo para crecer)

| Señal | Threshold |
|-------|-----------|
| `SUSTAINED_PROFITABILITY` | Net margin positivo 6+ meses |
| `OPERATING_LEVERAGE` | OpEx creciendo más lento que revenue |
| `SUPPLIER_MATURITY` | 3+ proveedores en categorías top (ABC-A) |
| `DEBT_CUSHION` | Debt service <10% de revenue |

**Scoring compuesto (0-100):**

```
Health Score = profitability(35%) + efficiency(25%) + stability(25%) + growth(15%)
```

Cada sub-score tiene drivers que explican *por qué* está en ese nivel. No es un número mágico — es un diagnóstico con contexto.

**Salida JSON (`financial_health`):**

```json
{
  "overallHealth": 72,
  "breakdown": {
    "profitability": { "score": 65, "drivers": ["grossMarginPct: 32%", "operatingMarginPct: 8%", "netMarginPct: 5%"] },
    "efficiency": { "score": 80, "drivers": ["opexRatio: 22%", "opexGrowth < revenueGrowth: true"] },
    "stability": { "score": 70, "drivers": ["supplierConcentration: 35%", "priceStability: 0.92"] },
    "growth": { "score": 55, "drivers": ["revenueTrend: +5%", "inventoryTurnover: 0.8"] }
  },
  "criticalAlerts": [
    { "type": "NEGATIVE_OPERATING_MARGIN", "severity": "CRITICAL",
      "title": "Margen Operativo Negativo",
      "message": "Tu negocio está perdiendo $X por cada $100 vendidos. A este ritmo, tu capital se agotará en ~N meses.",
      "metric": -3.5, "threshold": 0,
      "action": "Revisa tu estructura de precios o reduce gastos operativos" }
  ],
  "investmentSignals": [
    { "type": "HEALTHY_MARGIN_STACK", "status": "met",
      "label": "Margen Bruto >30%", "current": "32%", "threshold": "30%" }
  ],
  "expansionReadiness": {
    "score": 45,
    "status": "EN_DESARROLLO",
    "requirements": [
      { "met": false, "label": "6 meses de rentabilidad sostenida", "current": "3 meses" },
      { "met": true, "label": "Margen bruto >30%", "current": "32%" }
    ]
  },
  "recommendations": [
    "Reduce dependencia de Distribuidora XYZ (35% del gasto) — negocia con 2 alternativas.",
    "Tus 3 productos A representan el 60% del gasto. Asegura su stock prioritario."
  ]
}
```

**Persistencia:** JSONB nullable en `expense_analysis.financial_health` (V4). Filas existentes → null, mapper retorna objeto vacío.

**Trigger de recomputo:** Se ejecuta al final de `ejecutarCompleto()` después de los 9 motores. No tiene debounce propio — hereda el del analytics.

### Gasto (`core_pymes/gasto/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `GastoOperativo` (soft-delete) |
| Enum | `CategoriaGasto`: SALARIOS, AGUA, LUZ, INTERNET, ALQUILER, MANTENIMIENTO, PUBLICIDAD, OTROS |
| Endpoints | CRUD `/gastos` |
| Eventos | `GastoCreadoEvent` -> debounce Redis |

### Prestamo (`core_pymes/prestamo/`)

| Aspecto | Detalle |
|---------|---------|
| Entidades | `Prestamo` (soft-delete), `PagoPrestamo` |
| Enum | `EstadoPrestamo`: ACTIVO, PAGADO, CANCELADO |
| Endpoints | CRUD `/prestamos`, `POST /prestamos/{id}/pagos`, `GET /prestamos/{id}/pagos` |

### Inversion (`core_pymes/inversion/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `Patrimonio` (PK = tenant_id, 1 fila por tenant) |
| Endpoints | `GET /patrimonio/{tenantId}` (get-or-create), `PUT /patrimonio/{tenantId}` |

### Venta (`core_pymes/venta/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `VentaDiaria` (soft-delete) |
| Endpoints | CRUD `/ventas` |
| Eventos | `VentaCreadaEvent` -> debounce Redis |

### Accounting (`core_pymes/accounting/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `MetricasFinanciera` (tenant_id + period, UNIQUE) |
| Endpoints | `GET /accounting/consultar`, `POST /accounting/recalcular` |
| Query | CTE consolidado (5 fuentes: ventas, facturas, gastos, prestamos, patrimonio) en 1 round-trip |

---

## Estructura de Paquetes

```
core_pymes/
├── CoreApplication.java
├── common/
│   ├── config/
│   │   ├── EventConfig.java        # @EnableAsync + @EnableScheduling
│   │   └── CacheConfig.java        # @EnableCaching + RedisCacheManager
│   ├── constant/
│   │   └── CorePath.java           # rutas API
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   ├── seed/
│   │   └── SeedDataRunner.java     # 8 industrias, INSERT data (sin DDL)
│   └── service/
│       └── RecomputeDebounceService.java  # Redis debounce para metricas/analytics
│
├── setup/       controller/domain/dto/mapper/repository/service
├── product/     controller/domain/dto/event/mapper/repository/service
├── invoice/     controller/domain/dto/event/listener/mapper/repository/service
├── analytics/   controller/domain/dto/mapper/repository/service
├── gasto/       controller/domain/dto/event/listener/mapper/repository/service
├── prestamo/    controller/domain/dto/mapper/repository/service
├── inversion/   controller/domain/dto/mapper/repository/service
├── venta/       controller/domain/dto/event/listener/mapper/repository/service
└── accounting/  controller/domain/dto/mapper/repository/service
```

Controller pattern: interface (`XxxApi`) + impl (`XxxController`) dentro del modulo.
DTOs: Java records. Mapper: MapStruct.

---

## Flujo de Eventos

### Tenant Se Registra

```
Auth Service crea Tenant
  └── Core no recibe evento (lazy init)
  └── Primer GET /setup/{tenantId}:
      └── SetupService.getOrInitialize() -> crea TenantSetup si no existe
```

### Tenant Registra Factura

```
1. Invoice: POST /api/v1/core/facturas
   └── Valida datos
   └── Persiste Factura + Items
   └── Publica: FacturaCreadaEvent

2. [Async] FacturaCreadaListener -> RecomputeDebounceService.markAnalyticsDirty()
   └── SETNX recompute:analytics:{tenantId}:{period} en Redis (1ms)

3. [Scheduled @30s] RecomputeDebounceService.processPending()
   └── Barre keys pendientes
   └── 1 recompute por (tipo, tenant, periodo) unico
   └── AnalyticsService.ejecutarCompleto() -> 7 motores CTE
   └── MetricasService.recalcular() -> 1 CTE consolidado
```

### Registrar Gasto / Venta

```
POST /gastos o /ventas
  └── Persiste entidad
  └── Publica: GastoCreadoEvent / VentaCreadaEvent

[Async] GastoCreadaListener / VentaCreadaListener
  └── RecomputeDebounceService.markMetricsDirty()
  └── Redis SETNX -> 1ms, scheduler recalc cada 30s
```

### Debounce (Redis)

| Key pattern | Tipo | Service llamado |
|-------------|------|-----------------|
| `recompute:metrics:{tenantId}:{period}` | gasto/venta | `MetricasService.recalcular()` |
| `recompute:analytics:{tenantId}:{period}` | factura | `AnalyticsService.ejecutarCompleto()` |

TTL: 1 hora. Retry: key se conserva si falla (reintenta en proximo ciclo).

---

## Arquitectura Tecnica

### Spring Events

- Simple, sin dependencias externas, suficiente para MVP
- Migrar a RabbitMQ en Fase 3+ sin cambiar codigo de eventos

### Virtual Threads

- `@EventListener` + `@Async` -> Virtual Thread automatico
- Concurrencia trivial con recursos minimos (Java 21)

### Transaccionalidad

1. Operacion persiste en BD (COMMIT)
2. Evento se publica DESPUES del COMMIT
3. Listeners procesan async (pueden fallar sin afectar persistencia)
4. Idempotencia: debounce Redis deduplica por (tipo, tenant, periodo)

### Cache

| Aspecto | Detalle |
|---------|---------|
| Config | `CacheConfig.java`: `@EnableCaching` + `RedisCacheManager` (TTL 5min) |
| Lecturas | `@Cacheable` en findAll/findById de productos, proveedores, facturas |
| Escrituras | `@CacheEvict(allEntries=true)` en writes |
| Condicion | `@ConditionalOnBean(RedisConnectionFactory.class)` |

### Security

Core no valida JWT (eso lo hace el gateway). Core confia en los headers inyectados por el gateway:

| Header | Uso | Validacion |
|--------|-----|------------|
| `X-Tenant-Id` | Tenant isolation | `TenantValidationFilter`: compara header vs `?tenantId=` param, 403 si difieren |
| `X-User-Role` | Role-based access | `RoleHeaderFilter`: lee header → `SecurityContext` con `ROLE_<rol>` |
| `X-User-Id` | User identification | Disponible en request para services si se necesita |

**Authorization:**

- `@EnableMethodSecurity` en `SecurityConfig.java`
- `@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")` en todos los endpoints WRITE (POST/PUT/DELETE) — 18 endpoints across 10 controllers
- Endpoints READ (GET) no tienen restriccion de rol — cualquier autenticado puede leer
- Roles: `OWNER(4)` > `ADMIN(3)` > `CONTABLE(2)` > `VIEWER(1)` — definidos en auth service (`RoleName.java`)

**Archivos:**

```
common/config/SecurityConfig.java        # @EnableMethodSecurity, SecurityFilterChain, stateless
common/config/RoleHeaderFilter.java      # OncePerRequestFilter: X-User-Role → SecurityContext
common/config/TenantValidationFilter.java  # X-Tenant-Id vs ?tenantId= comparison
```

---

## Endpoints

### Configuracion

```
GET    /api/v1/core/setup/{tenantId}
POST   /api/v1/core/setup/{tenantId}/onboarding
GET    /api/v1/core/setup/preview/{industry}
```

### Productos

```
POST   /api/v1/core/productos
GET    /api/v1/core/productos
GET    /api/v1/core/productos/{id}
PUT    /api/v1/core/productos/{id}
DELETE /api/v1/core/productos/{id}

POST   /api/v1/core/productos/{id}/presentaciones
GET    /api/v1/core/productos/{id}/presentaciones
DELETE /api/v1/core/presentaciones/{presentacionId}
```

### Proveedores

```
POST   /api/v1/core/proveedores
GET    /api/v1/core/proveedores
GET    /api/v1/core/proveedores/{id}
PUT    /api/v1/core/proveedores/{id}
DELETE /api/v1/core/proveedores/{id}
```

### Facturas

```
POST   /api/v1/core/facturas
GET    /api/v1/core/facturas
GET    /api/v1/core/facturas/{id}
PUT    /api/v1/core/facturas/{id}?tenantId=
DELETE /api/v1/core/facturas/{id}
POST   /api/v1/core/facturas/{id}/pagar
```

### Gastos Operativos

```
POST   /api/v1/core/gastos
GET    /api/v1/core/gastos
GET    /api/v1/core/gastos/{id}
PUT    /api/v1/core/gastos/{id}
DELETE /api/v1/core/gastos/{id}
```

### Prestamos

```
POST   /api/v1/core/prestamos
GET    /api/v1/core/prestamos
GET    /api/v1/core/prestamos/{id}
PUT    /api/v1/core/prestamos/{id}
DELETE /api/v1/core/prestamos/{id}
POST   /api/v1/core/prestamos/{id}/pagos
GET    /api/v1/core/prestamos/{id}/pagos
```

### Patrimonio

```
GET    /api/v1/core/patrimonio/{tenantId}
PUT    /api/v1/core/patrimonio/{tenantId}
```

### Ventas

```
POST   /api/v1/core/ventas
GET    /api/v1/core/ventas
GET    /api/v1/core/ventas/{id}
PUT    /api/v1/core/ventas/{id}
DELETE /api/v1/core/ventas/{id}
```

### Accounting

```
GET    /api/v1/core/accounting/consultar?tenantId={uuid}&periodo=YYYY-MM
POST   /api/v1/core/accounting/recalcular?tenantId={uuid}&periodo=YYYY-MM
```

### Analytics

```
GET    /api/v1/core/analytics?tenantId={uuid}&periodo=YYYY-MM
POST   /api/v1/core/analytics/recalcular?tenantId={uuid}&periodo=YYYY-MM
```

---

## Seed Data

| Aspecto | Detalle |
|---------|---------|
| Flyway V2 | `industries`, `template_categories` |
| Flyway V18 | `template_units`, `template_payment_methods`, `template_products`, `template_product_presentations` |
| V17 | `DROP TABLE IF EXISTS template_locations` (cleanup stock) |
| SeedDataRunner | `@Component` idempotente, inserta via JdbcTemplate al startup (sin DDL) |
| Constantes | Industry codes (8) + SQL INSERT strings (5) — java:S1192 cleanup |
| Industrias | 8: restaurante, bares, salon_belleza, ferreteria, mini_super, taller_mecanico, farmacia, default |
| Productos | ~160 productos + ~280 presentaciones en seed |

Ver `SEED_TEMPLATES.md` para detalle completo.

---

## Decisiones Tecnicas

| Decision | Opcion elegida | Alternativa descartada |
|----------|---------------|----------------------|
| Inicializacion | Lazy (primer GET) | Webhook/evento TenantCreated |
| Estructura | Modular por modulo (`setup/`) | Layer-based plana |
| Docker | `COPY . .` + `mvn package` | `dependency:go-offline` separado |
| Comunicacion | Spring Events | RabbitMQ (post-MVP) |
| Concurrencia | Virtual Threads + @Async | Thread pool tradicional |
| Seed data | Java `ApplicationRunner` + JdbcTemplate | Flyway SQL, JPA entities |
| Test infra | Testcontainers PostgreSQL + Redis | Docker Compose externo |
| Mapper | MapStruct con interface + @Mapping | Manual setters, Lombok Builder |
| Financial Health | Motor compuesto que cruza datos existentes (no SQL nuevo) | Motor SQL independiente con tablas propias |
| Intelligence layer | JSONB nullable en tabla existente | Nueva tabla + schema separado |
| Relaciones FK | Dual-field pattern (UUID field + @ManyToOne read-only) | @ManyToOne con cascade completo |
| Entidades | Espanol (Producto, Factura, Proveedor) | Ingles (Product, Invoice) |
| Invoice numbering | Native query secuencial por tenant/año | UUID, secuencia global, logica en app |
| Soft-delete | @SQLDelete + @Where(clause = "is_active = true") | @ManyToMany, tabla aparte |
| Response wrapper | Sin wrapper (response directo) | ApiResponse envelope generico |
| Categorias jerarquicas | Nested DTO (`children` list) en el mismo `ItemDTO` | DTO separado por nivel |
| Template products | Genericos por industria (sin marca), copiados al onboarding | Productos especificos con marca |
| Gastos Operativos | Type libre en Factura (ya en codigo) | Enum cerrado |
| Recompute debounce | Redis SETNX + @Scheduled(fixedDelay=30s) | Kafka, in-memory debounce |
| SQL metrics | CTE consolidado (1 round-trip) | 5 queries separadas |

---

## Pendientes

### Inmediato

- [x] Accounting — `core_pymes/accounting/`: MetricaFinanciera + CTE consolidado
- [x] Gasto — `core_pymes/gasto/`: GastoOperativo + CategoriaGasto enum
- [x] Prestamo — `core_pymes/prestamo/`: Prestamo + PagoPrestamo + EstadoPrestamo
- [x] Inversion — `core_pymes/inversion/`: Patrimonio (1 por tenant)
- [x] Venta — `core_pymes/venta/`: VentaDiaria
- [x] Redis debounce — RecomputeDebounceService + @Scheduled
- [x] Testcontainers Redis en AbstractIntegrationTest
- [x] SQL review — division por cero en analisisABC, indices redundantes removidos
- [x] **Cleanup seed: remover stock** — `template_locations` + `template_movement_reasons` eliminadas, industry codes → constantes (java:S1192). Flyway V18 para DDL. Ver `SEED_TEMPLATES.md` → Cleanup 2026-07.
- [x] **Financial Health Engine** — Motor #10: scoring compuesto + alertas críticas + señales inversión/expansión. V4 (columna `financial_health JSONB`), `analisisSaludFinanciera()` en AnalyticsServiceImpl. Pendiente: `FinancialHealthResponse` DTO + `useAnalytics` + `AnalisisGastosPage.vue`.

### Mediate

- [ ] Reports — dashboard consolidado con KPIs, alertas
- [ ] CRUD de configuracion (edicion por tenant)

### Infraestructura

- [x] Spring Security — `@EnableMethodSecurity` + `@PreAuthorize` en endpoints WRITE (OWNER/ADMIN). `RoleHeaderFilter` lee `X-User-Role` del gateway.
- [ ] FeignClient para Auth
- [x] Cache con Redis
- [x] Sistema de eventos cross-module (Spring Events)
- [x] Redis debounce para recomputo de metricas/analytics

### Frontend

- [x] Onboarding post-login
- [x] Modulo Core frontend — Productos, Proveedores, Facturas
- [x] Onboarding 2 pasos con preview de categorias
- [x] Onboarding con preview de productos precargados
