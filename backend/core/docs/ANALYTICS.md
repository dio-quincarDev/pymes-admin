# Analytics Module

> **Estado (2026-06):** ✅ Implementado en producción — 6 motores CTE, listener conectado a FacturaCreadaEvent, tabla expense_analysis.
> Tests: ✅ 5 unitarios + 4 JPA (Testcontainers).

## 1. Arquitectura

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

---

## 2. SQL Entities — 6 Motores Analíticos

### `analisisABC` (ABC de Gastos — Pareto)
- CTE: `product_spend` + `ranked` + ventana `SUM() OVER`
- Clasifica productos por % acumulado de gasto total
- O(N) vs O(n²) con window functions

### `analisisTendencia` (Tendencias de Precios)
- CTEs paralelos: `current_prices` + `moving_avg_90d`
- Calcula % cambio de `current_avg_price` vs `moving_avg_90d`

### `analisisMargen` (Impacto en Márgenes)
- CTEs paralelos: `current_prices` + `previous_prices`
- Delta de precio unitario % cambio, BigDecimal seguro

### `analisisCostoOperativo` (Costo Operativo % Ventas)
- CTE `period_data` + serie `daily_total`
- Proyección mensual: `avg_daily_spend * days_in_month`

### `analisisProyeccion` (Proyección de Gastos)
- Query nativa simple: `SUM(subtotal) / COUNT(DISTINCT issue_date)`

### `analisisAlertas` (Alertas de Anomalías)
- CTE `stats` + `COEFFICIENT_VARIATION`
- Filtra variación > 15% → alerta

---

## 3. Controller/Service Split

**AnalyticsApi.java**
```java
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.ANALYTICS_ROUTE)
public interface AnalyticsApi {
    ResponseEntity<AnalyticsResponse> consultar(UUID tenantId, String periodo);
    ResponseEntity<AnalyticsResponse> recalcular(UUID tenantId, String periodo);
}
```

**AnalyticsService.java**
```java
public interface AnalyticsService {
    AnalisisGasto ejecutarCompleto(UUID tenantId, String periodo);
    Optional<AnalisisGasto> consultar(UUID tenantId, String periodo);
}
```

---

## 4. CTEs Clave

### Tendencia (O(n²) → O(N))
```sql
WITH current_prices AS (
    SELECT ii.product_id, p.name AS product_name,
           AVG(ii.unit_price / ii.conversion_factor) AS current_avg_price
    FROM core.invoice_items ii
    JOIN core.invoices i ON ii.invoice_id = i.id
    JOIN core.products p ON ii.product_id = p.id
    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
    GROUP BY ii.product_id, p.name
),
moving_avg AS (
    SELECT ii.product_id,
           AVG(ii.unit_price / ii.conversion_factor) AS moving_avg_90d
    FROM core.invoice_items ii
    JOIN core.invoices i ON ii.invoice_id = i.id
    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
    GROUP BY ii.product_id
)
SELECT cp.product_id, cp.product_name, cp.current_avg_price, ma.moving_avg_90d
FROM current_prices cp
LEFT JOIN moving_avg ma ON cp.product_id = ma.product_id;
```

### Margen (CTEs paralelos)
```sql
WITH current_prices AS ( ... ),
previous_prices AS ( ... )
SELECT cp.product_id, cp.product_name,
       cp.avg_price AS current_price,
       pp.avg_price AS previous_price,
       CASE WHEN pp.avg_price > 0
            THEN ROUND((cp.avg_price - pp.avg_price) / pp.avg_price * 100, 2)
            ELSE 0 END AS pct_change
FROM current_prices cp
LEFT JOIN previous_prices pp ON cp.product_id = pp.product_id;
```

### Costo Operativo
```sql
WITH period_data AS (
    SELECT i.id, i.issue_date, ii.product_id, i.provider_id, ii.subtotal
    FROM core.invoices i
    JOIN core.invoice_items ii ON ii.invoice_id = i.id
    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
),
summary AS (
    SELECT COUNT(DISTINCT id) AS invoice_count,
           COUNT(DISTINCT product_id) AS product_count,
           COUNT(DISTINCT provider_id) AS provider_count,
           COALESCE(SUM(subtotal), 0) AS total_spend
    FROM period_data
)
SELECT *,
       total_spend * 30 / NULLIF(invoice_count, 0) AS projected_monthly,
       (SELECT AVG(daily_total) FROM (
           SELECT SUM(subtotal) AS daily_total FROM period_data GROUP BY issue_date
       ) d) AS avg_daily_spend
FROM summary;
```

---

## 5. Tests

- **Unitarios (5):** `AnalyticsServiceImplTest` — mockea JdbcTemplate + AnalisisGastoRepository + ObjectMapper
  - ejecutarCompleto con/sin análisis existente (creación vs upsert)
  - ejecutarCompleto invoca los 6 motores
  - consultar con/sin resultado
- **JPA (4):** `AnalyticsRepositoryTest` — PostgreSQL real via Testcontainers
  - saveAndFind, tenantScoped, upsertOverwrites, nonExistentPeriod

**Nota:** `AnalisisGasto` usa `@Column(columnDefinition = "JSONB")` — H2 no soporta JSONB correctamente.

---

## 6. Dependencias

No hay nuevas dependencias transitivas:
- Spring Boot Starter Data JPA
- Spring Boot Starter Web
- PostgreSQL JDBC Driver (Testcontainers)
- Lombok + MapStruct

---

## 7. Registro de Implementación

| Fecha | Evento |
|-------|--------|
| 2025-06-14 | Split interface/api, creación de AnalyticsController |
| 2025-06-23 | Optimización SQL 6 motores (de O(n²) a O(N)), V6 migration |
| 2025-07-02 | AnalyticsServiceImplTest, AbstractJpaTest base |
| 2025-07-09 | ProductoRepositoryTest (12) + FacturaRepositoryTest (14) |
| 2025-07-21 | FacturaCreadaListener usa AnalyticsService (abstracción) |

---

## 8. Cronograma

| Semana | Actividad |
|--------|-----------|
| 1-2 | Completar AnalyticsRepositoryTest (JSONB) |
| 3-4 | Dashboard frontend |
| 5-6 | Proyección en UI |
| 7-8 | Refactoring a InsumoTemplate |
| 9-10 | Normalización motor de precios |
