# Analytics Module

> **Estado (2026-07-09):** Implementado en produccion — 9 motores CTE, listener conectado a FacturaCreadaEvent via debounce Redis, tabla expense_analysis con 3 columnas JSONB de supplier analytics.
> Tests: 5 unitarios + 4 JPA + 5 unitarios supplier (AnalyticsServiceImplTest).

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
| Costo Operativo | `analisisCostoOperativo()` | Gasto operativo % ventas + proyeccion mensual |
| Proyeccion | `analisisProyeccion()` | Forecast lineal 30/60/90 dias |
| Alertas | `analisisAlertas()` | Variacion >15% (CV) + alerta SUPPLIER_PREMIUM |

### Supplier Analytics (3 — V11)

| Motor | Metodo | Descripcion |
|-------|--------|-------------|
| Comparativa Proveedores | `analisisComparativaProveedores()` | avg/min/max/stddev de precio por producto-proveedor |
| Recomendaciones | `analisisRecomendacionProveedor()` | Proveedor mas barato por producto + savings_pct |
| Predicciones Precios | `analisisProyeccionPrecios()` | OLS lineal por producto, predice precio proximo mes con R2 |

### Flyway

| Migration | Contenido |
|-----------|-----------|
| V6 | Tabla `expense_analysis` (JSONB por tenant/periodo) |
| V11 | +3 columnas JSONB: `supplier_comparison`, `supplier_recommendations`, `price_predictions` |

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

### Costo Operativo

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
| Unit | `AnalyticsServiceImplTest` | 5 | 6 motores + upsert + consulta |
| JPA | `AnalyticsRepositoryTest` | 4 | saveAndFind, tenantScoped, upsertOverwrites, nonExistentPeriod |
| Unit | `AnalyticsServiceImplTest` (supplier) | 5 | comparativa, recomendaciones, predicciones, alertas supplier |

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
