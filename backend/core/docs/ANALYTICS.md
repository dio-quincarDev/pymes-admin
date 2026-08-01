# Analytics Module

> **Estado (2026-07-31):** Implementado en produccion — 9 motores CTE + 1 motor compuesto (Financial Health), listener conectado a FacturaCreadaEvent via debounce Redis, tabla expense_analysis con 10 columnas JSONB.
> Tests: 6 unitarios + 5 integration (AnalyticsServiceImplTest + AnalyticsIntegrationTest con Testcontainers PG15 + Redis7).

---

## Arquitectura

```
/core_pymes/analytics/
├── controller/
│   ├── AnalyticsApi (interface)
│   └── impl/AnalyticsController
├── service/
│   ├── AnalyticsService (interface)
│   └── impl/AnalyticsServiceImpl
├── mapper/AnalyticsMapper
├── dto/AnalyticsResponse
├── domain/AnalisisGasto.java (JSONB)
└── repository/AnalisisGastoRepository
```

| Endpoint | Metodo | Descripcion |
|----------|--------|-------------|
| `/analytics/consultar` | GET | Consulta analisis guardado por tenant/periodo |
| `/analytics/recalcular` | POST | Fuerza recalculation de los 9 motores |

---

## Motores Analiticos

### Originales (6)

| Motor | Metodo | Descripcion |
|-------|--------|-------------|
| ABC de Gastos | `analisisABC()` | Pareto: categorias A/B/C por % acumulado de gasto |
| Tendencias Precios | `analisisTendencia()` | % cambio vs media movil 90 dias |
| Impacto Margenes | `analisisMargen()` | Delta precio unitario periodo actual vs anterior |
| Gasto Variable | `analisisGastoVariable()` | Gasto operativo % ventas + proyeccion mensual |
| Proyeccion | `analisisProyeccion()` | Forecast lineal 30/60/90 dias |
| Alertas | `analisisAlertas()` | Variacion >15% (CV) + alerta SUPPLIER_PREMIUM |

### Supplier Analytics (3 — V11)

| Motor | Metodo | Descripcion |
|-------|--------|-------------|
| Comparativa Proveedores | `analisisComparativaProveedores()` | avg/min/max/stddev de precio por producto-proveedor |
| Recomendaciones | `analisisRecomendacionProveedor()` | Proveedor mas barato por producto + savings_pct |
| Predicciones Precios | `analisisProyeccionPrecios()` | OLS en SQL (`regr_slope/regr_intercept/regr_r2` en CTEs `daily_prices`+`ranked`), `predictedPrice = slope*(n+1)+intercept` (rn 1-based), filtro `data_points>=3` |

### Financial Health Engine (Motor #10 — V4)

Motor compuesto, no SQL independiente. Cruza datos de los 9 motores + `MetricasRepository` para producir inteligencia accionable.

| Señal | Tipo | Criterio |
|-------|------|----------|
| `NEGATIVE_OPERATING_MARGIN` | 🔴 Crítica | `operatingMarginPct < 0` |
| `MARGIN_EROSION` | 🔴 Crítica | `grossMarginPct` bajando >15% vs período anterior, 3+ meses |
| `SUPPLIER_CONCENTRATION` | 🔴 Crítica | 1 proveedor >50% del gasto total |
| `OVER_LEVERAGED` | 🔴 Crítica | `loanPayments / operatingMargin > 0.30` |
| `OPEX_CREEP` | 🔴 Crítica | Gastos operativos creciendo más rápido que ingresos, 3+ meses |
| `DEAD_INVENTORY` | 🔴 Crítica | Productos con inversión alta + 0 compras >60d |
| `HEALTHY_MARGIN_STACK` | 🟢 Inversión | Bruto >30% Y Operativo >10% Y Neto >5% |
| `POSITIVE_CASH_FLOW` | 🟢 Inversión | Operating margin positivo 3+ meses |
| `LOW_CONCENTRATION` | 🟢 Inversión | Ningún proveedor >30%, ningún producto >20% |
| `DEBT_CAPACITY` | 🟢 Inversión | Loan payments <15% del margen operativo |
| `SUSTAINED_PROFITABILITY` | 🟣 Expansión | Net margin positivo 6+ meses |
| `OPERATING_LEVERAGE` | 🟣 Expansión | OpEx creciendo más lento que revenue |
| `SUPPLIER_MATURITY` | 🟣 Expansión | 3+ proveedores en categorías top (ABC-A) |
| `DEBT_CUSHION` | 🟣 Expansión | Debt service <10% de revenue |

Scoring compuesto: `Health = profitability(35%) + efficiency(25%) + stability(25%) + growth(15%)`

Ver `CORE.md` §Motor de Salud Financiera para JSON de salida, inputs y diseño completo.

### Flyway

| Migration | Contenido |
|-----------|-----------|
| V1 | Esquema consolidado (V1–V18, 2026-07-30): `expense_analysis` + índices |
| V2 | Costos engine: `collaboradores`, `gastos_fijos_recurrentes`, `config_laboral` |
| V3 | `costo_operativo_diario` en `tenant_financial_metrics` |
| V4 | +1 columna JSONB: `financial_health` (nullable, Motor #10) |

---

## SQL Clave

### ABC (division por cero protegida)

```sql
WITH product_spend AS (...),
ranked AS (...),
grand AS (SELECT SUM(total_spend) AS grand_total FROM ranked)
SELECT product_id, product_name, total_spend,
       CASE WHEN grand_total > 0
            THEN ROUND(total_spend * 100.0 / grand_total, 2)
            ELSE 0 END AS pct,
       CASE
           WHEN grand_total > 0 AND SUM(total_spend) OVER (...) / grand_total <= 0.80 THEN 'A'
           WHEN grand_total > 0 AND SUM(total_spend) OVER (...) / grand_total <= 0.95 THEN 'B'
           ELSE 'C'
       END AS category
FROM ranked, grand
ORDER BY total_spend DESC
```

### Tendencia (CTEs paralelos)

```sql
WITH current_prices AS (
    SELECT ii.product_id, p.name, AVG(ii.unit_price / ii.conversion_factor) AS current_avg_price
    FROM core.invoice_items ii
    JOIN core.invoices i ON ii.invoice_id = i.id
    JOIN core.products p ON ii.product_id = p.id
    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
    GROUP BY ii.product_id, p.name
),
moving_avg AS (
    SELECT ii.product_id, AVG(ii.unit_price / ii.conversion_factor) AS moving_avg_90d
    FROM core.invoice_items ii
    JOIN core.invoices i ON ii.invoice_id = i.id
    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
    GROUP BY ii.product_id
)
SELECT cp.product_id, cp.product_name, cp.current_avg_price, ma.moving_avg_90d
FROM current_prices cp
LEFT JOIN moving_avg ma ON cp.product_id = ma.product_id
```

### Gasto Variable

```sql
WITH period_data AS (
    SELECT i.id, i.issue_date, ii.product_id, i.provider_id, ii.subtotal
    FROM core.invoices i
    JOIN core.invoice_items ii ON ii.invoice_id = i.id
    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
)
SELECT COUNT(DISTINCT id) AS invoice_count,
       COUNT(DISTINCT product_id) AS product_count,
       COUNT(DISTINCT provider_id) AS provider_count,
       COALESCE(SUM(subtotal), 0) AS total_spend,
       COALESCE((SELECT AVG(daily_total) FROM (
           SELECT SUM(subtotal) AS daily_total FROM period_data GROUP BY issue_date
       ) d), 0) AS avg_daily_spend
FROM period_data
```

---

## Performance

| Aspecto | Estado |
|---------|--------|
| Indexes | `idx_invoices_tenant_date_type INCLUDE (total)` (V13) — covering index para ABC/opex |
| Rango fechas | `>= ? AND < ?` (sargable) en vez de `DATE(ts) = ?` |
| Division por cero | `CASE WHEN grand_total > 0` guard en analisisABC |
| Redundant indexes | Removidos `idx_operating_expenses_tenant` y `idx_daily_sales_tenant` (composite los cubre) |

---

## Tests

| Tipo | Clase | Tests | Descripcion |
|------|-------|-------|-------------|
| Unit | `AnalyticsServiceImplTest` | 5 | 9 motores (6 originales + 3 supplier) + upsert + consulta |
| JPA | `AnalyticsRepositoryTest` | 4 | saveAndFind, tenantScoped, upsertOverwrites, nonExistentPeriod |

> AnalisisGasto usa `@Column(columnDefinition = "JSONB")` — H2 no soporta JSONB correctamente, por eso los tests unitarios mockean JdbcTemplate.

---

## Dependencias

No hay dependencias adicionales:
- Spring Boot Starter Data JPA
- Spring Boot Starter Web
- PostgreSQL JDBC Driver (Testcontainers)
- Lombok + MapStruct
- JdbcTemplate (para queries nativas)

---

## Registro

| Fecha | Evento |
|-------|--------|
| 2025-06-14 | Split interface/api, creacion de AnalyticsController |
| 2025-06-23 | Optimizacion SQL 6 motores (de O(n2) a O(N)), V6 migration |
| 2025-07-02 | AnalyticsServiceImplTest, AbstractJpaTest base |
| 2025-07-09 | ProductoRepositoryTest (12) + FacturaRepositoryTest (14) |
| 2025-07-21 | FacturaCreadaListener usa AnalyticsService (abstraccion) |
| 2026-07-08 | V11: supplier analytics (comparativa, recomendaciones, predicciones OLS) |
| 2026-07-09 | SQL review: fix division por cero, removal redundants, covering indexes V13 |
| 2026-07-14 | Motor #10: Financial Health Engine — scoring compuesto, alertas críticas, señales inversión/expansión. V15 (financial_health JSONB). Documentado en CORE.md. |
