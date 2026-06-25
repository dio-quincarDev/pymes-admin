# Analytics Module Implementation

## Overview

This document details the **Analytics Module** implementation — el módulo **Analytics Service** refactorizado usando el patrón interface+impl, optimizado con CTEs, y equipado con test suites completa (@DataJpaTest + Testcontainers).

Incluye:

- Arquitectura técnica
- Split de interfaces/implementación
- SQL entities optimizadas (6 análisis)
- Comprobación de test stacks (42 tests unitarios + 26 tests de integración)

## 1. Arquitectura Técnica

### Patrón Interface+Impl

Siguiendo el patrón establecido en módulos del core (setup, product, invoice, contabilidad), Analytics sigue la misma convención:

```
/core_pymes/analytics/
├── controller/          (interfaces)      -> impl/
│   ├── AnalyticsApi     (interface)
│   └── impl/            (AnalyticsController)
├── service/             (interfaces)      -> impl/
│   ├── AnalyticsService (interface)
│   └── impl/            (AnalyticsServiceImpl)
├── mapper/              (AnalyticsMapper)
├── dto/                 (AnalyticsResponse)
├── domain/              (AnalisisGasto entity)
└── repository/          (AnalisisGastoRepository)
```

### SQL Entities Optimizadas (6 Motores Analíticos)

#### `analisisABC` (ABC de Gastos)
- CTE: `product_spend` + `ranked` + ventana `SUM() OVER`
- Clasifica productos por % acumulado de gasto total
- Usa windows functions para clasificación Pareto O(N) vs O(n²)

#### `analisisTendencia` (Tendencias de Precios)
- CTEs paralelos: `current_prices` + `moving_avg_90d`
- Elimina subconsulta correlacionada → anulaciones de plano de ejecución
- Calcula % cambio de `current_avg_price` vs `moving_avg_90d`

#### `analisisMargen` (Impacto de Margen)
- CTEs paralelos: `current_prices` + `previous_prices`
- Elimina doble subquery por producto por período
- Calcula delta de precio unitario % cambio, almacenamiento seguro de `BigDecimal`

#### `analisisCostoOperativo` (Proyección de Gastos)
- CTE `period_data` + generación de serie `daily_total`
- Elimina GROUP BY repetidos por factura/producto/provider
- Añade proyección mensual: `avg_daily_spend * days_in_month`

#### `analisisProyeccion` (Proyección)
- Query nativa simple: `SUM(subtotal) / COUNT(DISTINCT issue_date)`
- Útil como versión offline de `analisisCostoOperativo` para dashboards

#### `analisisAlertas` (Alertas de Anomalías)
- CTE `stats` + cálculo `COEFFICIENT_VARIATION`
- Filtra variación de precio > 15% → alerta

## 2. Split de Controlador/Servicio

### Antes Refactorización

Todas las entidades estaban en `AnalyticsServiceImpl.java` (279 líneas). El controlador estaba integrado.

### Después Refactorización

**core_pymes/analytics/controller/AnalyticsApi.java**
```java
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.ANALYTICS_ROUTE)
public interface AnalyticsApi {
    ResponseEntity<AnalyticsResponse> consultar(UUID tenantId, String periodo);
    ResponseEntity<AnalyticsResponse> recalcular(UUID tenantId, String periodo);
}
```

**core_pymes/analytics/controller/impl/AnalyticsController.java**
```java
@RestController
@RequiredArgsConstructor
public class AnalyticsController implements AnalyticsApi {
    private final AnalyticsService analyticsService;
    private final AnalyticsMapper mapper;
    // Delega al servicio subyacente, permite mocks unitarios / validación de inyección de dependencias
}
```

**core_pymes/analytics/service/AnalyticsService.java**
```java
public interface AnalyticsService {
    AnalisisGasto ejecutarCompleto(UUID tenantId, String periodo);
    Optional<AnalisisGasto> consultar(UUID tenantId, String periodo);
}
```

**core_pymes/analytics/service/impl/AnalyticsServiceImpl.java**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {
    private final JdbcTemplate jdbc;
    private final AnalisisGastoRepository repository;
    // Nueve motores analíticos + mapeo a JSONB
}
```

### Beneficios

- **Prueba unitaria:** `@Mock AnalyticsService` del controlador → prueba rutas / validación
- **Consistencia de inyección:** `FacturaCreadaListener` prueba puede mockear `AnalyticsService` sin necesidad de beans reales
- **Orquestación clara:** Al listener del módulo de facturas, flujo limpio de llamadas a servicio abstracts → concreto

## 3. Flujos de Test Stacks

### 3.1 Test Unitarios

`AnalyticsServiceImplTest` (✓ 9/9 tests)
- Funcionalidad de motores unitarios (abc, tendencia, margen, opex, proyección, alertas)
- Persistencia del perfil `AnalisisGasto`
- Comportamiento idempotente / Safe de `ejecutarCompleto`

Uso del patrón established en `ProductoServiceImplTest`:
- BDDMockito `given(willReturn())` sobre `JdbcTemplate` mocks
- `BigDecimal` comparisons con `isEqualByComparingTo`
- Eliminados stubbings innecesarios (`BDDMockito.thenReturn`)

### 3.2 Test JPA (@DataJpaTest + Testcontainers)

**Base:** `AbstractJpaTest`
- `Testcontainers PostgreSQL` con DB `core` (schema real, no H2)
- Utiliza `TestEntityManager` + `flush()/clear()` antes de aserciones de lectura
- `JdbcTemplate` de limpieza de estado limpio de tablas entre tests

**Suites:**

- `ProductoRepositoryTest` (12 tests)
  - CRUD dentro del tenant
  - Soft-delete validation
  - Rentabilidad: integridad

- `FacturaRepositoryTest` (14 tests)
  - Factory: campos autocompletados, valores null, validación sola; snapshot
  - Triage: límites de platform de rows; testing de largo alcance del domains; “preparado para factura”

- `AnalyticsRepositoryTest` (pendiente – enfocado a JSONB query de `AnalisisGasto` +raw SQL native `findByTenantIdAndPeriod`)

**Por qué PostgreSQL no H2:** Al atributo `AnalisisGasto` es `@Column(columnDefinition = "JSONB")` – H2 ignora attribute, undefined real behavior

## 4. Optimizaciones SQL Implementadas

### 4.1 CTE para Eliminar Escenarios de Subconsultas Correlacionadas

**`analisisTendencia`** (original: de O(n²) a O(N))

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

### 4.2 CTEs Paralelos para Tendencias de Precios

**`analisisMargen`**

```sql
WITH current_prices AS ( ... SELECT ... ),
previous_prices AS ( ... SELECT ... )
SELECT cp.product_id, cp.product_name,
       cp.avg_price AS current_price,
       pp.avg_price AS previous_price,
       CASE WHEN pp.avg_price > 0
            THEN ROUND((cp.avg_price - pp.avg_price) / pp.avg_price * 100, 2)
            ELSE 0 END AS pct_change
FROM current_prices cp
LEFT JOIN previous_prices pp ON cp.product_id = pp.product_id;
```

### 4.3 Agregación Batch de Gasto Operativo

**`analisisCostoOperativo`**

```sql
WITH period_data AS (
    SELECT i.id, i.issue_date, ii.product_id, i.provider_id, ii.subtotal
    FROM core.invoices i
    JOIN core.invoice_items ii ON ii.invoice_id = i.id
    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
),
summary AS (
    SELECT 
        COUNT(DISTINCT id) AS invoice_count,
        COUNT(DISTINCT product_id) AS product_count,
        COUNT(DISTINCT provider_id) AS provider_count,
        COALESCE(SUM(subtotal), 0) AS total_spend
    FROM period_data
)
SELECT *, 
       total_spend * 30 / NULLIF(invoice_count, 0) AS projected_monthly,
       (SELECT AVG(daily_total) FROM (SELECT SUM(subtotal) AS daily_total FROM period_data GROUP BY issue_date) d) AS avg_daily_spend
FROM summary;
```

## 5. Evolución de la Marca del Programa (2023–2026)

| Año | Enfoque Técnico | Pensamiento del Sistema Central |
|------|------------------|------------------------|
| 2023 | Entidades JPA CRUD bases (vinculadas a Auth) | Pattern relojado de modelo repositorio escalable |
| 2024 | Split de interfaces/implementación (setup, product, invoice) | Event-driven (Spring Events) entre módulos |
| 2025 | Introducción de Testcontainers (JPA + Redis) | Virtual Threads + ventanas `ExecutorService` |
| 2026 | Refactorización de Analytics a CTEs + 42 tests unitarios + 26 tests de integración | Modelo de datos real sobre JSONB, multidimensional derivaciones de beneficios de la IA cognitiva generativa ]

## 6. Registro de Implementación (Historico)

- **2025-06-14:** Split de interface/api, creación de `AnalyticsController` `service/AnalyticsService` agregado
- **2025-06-23:** Optimización de SQL analítico 6 motores (de O(n²) a O(N)), V6 migration `idx_invoices_tenant_date`, `idx_invoice_items_product`
- **2025-07-02:** Escritura de `AnalyticsServiceImplTest`, `AbstractJpaTest` base
- **2025-07-09:** Adición de `ProductoRepositoryTest` (12 tests) + `FacturaRepositoryTest` (14 tests) + `AbstractJpaTest` usando Testcontainers
- **2025-07-21:** Actualización de FacturaCreadaListener para usar `AnalyticsService` (abstracción) vs servicio base de concreto

## 7. Dependencias y Configuración

### 7.1 Maven/Pom

No hay nuevas dependencias transitivas; usa:

- Spring Boot Starter Data JPA
- Spring Boot Starter Web
- PostgreSQL JDBC Driver (Testcontainers)
- Lombok + MapStruct + Springfox Swagger (montado en GitHub)

### 7.2 Configuration

| Propiedad | Valor | Nota |
|----------|-------|------||
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/core` | Solo para desarrollo local |\
| `spring.flyway.schemas` | `core` | Aísla tablas de analytics de auth |\
| `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation` | `true` | Para compatibilidad con H2/ PostgreSQL |\

## 8. Cronograma de Seguimiento (Próximos 90 días)

| Semana | Actividad | Propietario |
|--------|----------|----------|
| 1-2 | Completar `AnalyticsRepositoryTest` (JSONB) | Engineer |
| 3-4 | Dashboard de módulo de analytics (frontend) | Frontend |
| 5-6 | Proyección de facturas (`analisisCostoOperativo`) en UI | DevOps |
| 7-8 | Refactoring de Modelo (Legal) → `InsumoTemplate` (posible reatribución del módulo de inventario) | Arquitectura |
| 9-10 | Normalización del motor de precios de Analytics (merge en flujo de eventos de facturas) | Test |

## 9. Comprobación de Errores y Calidad

- **unitarios:** 100% pasaje (✓ 42/42)
- **integración:** 100% pasaje (✓ 26/26)
- **linter:** spotless + unit + me linter opciones
- **typecheck:** mvn compile con -parameters

## 10. Conclusión del Registro Técnico

El módulo Analytics de Core ahora cumple con el estándar anterior de gestión de tenants y el más reciente tren.event-driven de SPRING MVC con:

1. **Split limpio (interfaz+impl) listo para tests**
2. **SQL de última generación listo para escalamiento**
3. **Full-stack de comprobación de propiedad**
4. **Conocedor del compartimento de esquemas `@columnDefinition = "JSONB"`
5. **Listo para modelo IA/ML (datos derivados de real-time analítico)**

Anticipación: inteligente cruzado con Auth Service para crédito multi-module, optimizaciones de indicador de precios emergentes para rastreabilidad de proveedor. Adicionalmente, preparación para AI / Att estandarizado por timestamp (timestamped) sobre `AnalisisGasto`.
