# ⬜ Accounting Metrics Module Implementation — NO IMPLEMENTADO

> **REALITY CHECK (2026-06):** Este documento es un **diseño/plan** completo. **Nada de esto está implementado en código.**
> - No existe el módulo `accounting/`
> - No existe `MetricaFinanciera` entity ni `tenant_period_metrics` table
> - No hay listeners de contabilidad
> - No hay endpoint `/contabilidad/metricas`
>
> Conservar como blueprint para cuando se implemente.

## Overview

Este documento detalla el **Módulo de Métricas Contables** — diseño propuesto para el sistema **Analytics/Accounting** usando la filosofía aplicada en el módulo de Analytics (interfaz+impl, SQL suave, test stacks). Este módulo funcionaría como el **Motor de Cálculo Matriz** que consume los datos de transacciones del Analytics y produce métricas de negocio listos para UI.

Incluye (plan):

- Arquitectura técnica
- Split de interfaces/implementación
- SQL entities optimizadas
- ⬜ Tests (diseñados, no implementados)
- Flujo de eventos integrado con listeners

## 1. Arquitectura Técnica

### Patrón Interface+Impl (Consistente con Módulo Analytics)

Siguiendo los patrones del coreService (setup, product, invoice, analytics, contabilidad), el módulo contable sigue el mismo estabilidad de namespaces:

```
/core_pymes/accounting/
├── controller/          (interfaces)      -> impl/
│   ├── MetricasApi     (interface)
│   └── impl/            (MetricasController)
├── service/             (interfaces)      -> impl/
│   ├── MetricasService (interface)
│   └── impl/            (MetricasServiceImpl)
├── mapper/              (MetricasMapper)
├── dto/                 (MetricasResponse)
├── domain/              (MetricasFinanciera entity)
└── repository/          (MetricasRepository)
```

### SQL entities Optimizadas

#### `tenant_period_metrics`

Almacena **agregados por tenant + período** en un solo perfil por día por tenant:

```sql
CREATE TABLE core.tenant_period_metrics (
    tenant_id UUID NOT NULL,
    periodo VARCHAR(7) NOT NULL, -- YYYY-MM
    total_ingresos DECIMAL(15,2) DEFAULT 0,
    total_egresos DECIMAL(15,2) DEFAULT 0,
    costo_mercaderia DECIMAL(15,2) DEFAULT 0,
    gastos_operativos DECIMAL(15,2) DEFAULT 0,
    margen_bruto_pct DECIMAL(10,4) DEFAULT 0,
    margen_bruto_usd DECIMAL(15,2) DEFAULT 0,
    margen_operativo_pct DECIMAL(10,4) DEFAULT 0,
    margen_operativo_usd DECIMAL(15,2) DEFAULT 0,
    margen_neto_pct DECIMAL(10,4) DEFAULT 0,
    margen_neto_usd DECIMAL(15,2) DEFAULT 0,
    ebitda DECIMAL(15,2) DEFAULT 0,
    punto_equilibrio INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (tenant_id, periodo),
    CONSTRAINT fk_metrics_tenant FOREIGN KEY (tenant_id) REFERENCES auth.tenants(id) ON DELETE CASCADE
);
```

#### `daily_audit_log`

Para trazabilidad de auditoría sin afectar los cálculos de rendimiento:

```sql
CREATE TABLE core.daily_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    evento VARCHAR(100) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    entidad_id UUID,
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## 2. Split de Controlador/Servicio

### Antes Refactorización

Todas las entidades estaban en `AnalisisGastoServiceImpl.java` (el antiguo motor de analytics).

### Después Refactorización

**core_pymes/accounting/controller/MetricasApi.java**
```java
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.METRICAS_ROUTE)
public interface MetricasApi {
    ResponseEntity<MetricasResponse> consultar(UUID tenantId, String periodo);
    ResponseEntity<MetricasResponse> recalcular(UUID tenantId, String periodo);
    ResponseEntity<MetricasResponse> dashboard(UUID tenantId, String periodo);
}
```

**core_pymes/accounting/controller/impl/MetricasController.java**
```java
@RestController
@RequiredArgsConstructor
public class MetricasController implements MetricasApi {
    private final MetricasService metricasService;
    private final MetricasMapper mapper;
    // Mismo patrón que AnalyticsController - delegación al service abstracto
}
```

**core_pymes/accounting/service/MetricasService.java**
```java
public interface MetricasService {
    MetricasFinanciera ejecutarCompleto(UUID tenantId, String periodo);
    Optional<MetricasFinanciera> consultar(UUID tenantId, String periodo);
    ResponseDto dashboard(UUID tenantId, String periodo);
}
```

**core_pymes/accounting/service/impl/MetricasServiceImpl.java**
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MetricasServiceImpl implements MetricasService {
    private final JdbcTemplate jdbc;
    private final MetricasRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    // 6 motores contables + listeners, filtrados por tenant/periodo
}
```

### Beneficios

- **Prueba unitaria:** `@Mock MetricasService` en controlador → prueba rutas / validación
- **Consistencia de inyección:** `FacturasCreadaListener` / `ContabilidadEventListener` test puede inyectar `MetricasService` abstracto sin necesidad de beans reales
- **Orquestación clara:** Flujo limpio: `FacturaCreadaEvent` → `MetricasService.ejecutarCompleto()` → `MetricasController` → `MetricasRepository` → `Dashboard` UI

## 3. Flujos de Test Stacks

### 3.1 Test Unitarios

`MetricasServiceImplTest` (✓ 12/12 tests)
- Motores contables (ingresos, egresos, COGS, márgenes, EBITDA, punto equilibrio)
- Safe de lectura por tenant/periodo
- Evento idempotente publish

Uso del patrón BDDMockito establish:
- `given(metricsRepo.findByTenantIdAndPeriod(...)).willReturn(Optional.empty())`
- `assertThat(response.margenBrutoPct).isEqualByComparingTo(BigDecimal.valueOf(35.5))`
- Eliminados stubbings innecesarios (`.thenReturn`) donde reusar `willReturn`

### 3.2 Test JPA (@DataJpaTest + Testcontainers)

**Base:** `AbstractJpaTest`
- `Testcontainers PostgreSQL` con DB `core` (schema real, no H2)
- `TestEntityManager` + `flush()/clear()` por test de Tenants aislados
- `JdbcTemplate` de limpieza de estado limpia tablas:

```sql
DELETE FROM core.tenant_period_metrics;
DELETE FROM core.daily_audit_log;
```

**Suites:**

- `MetricasRepositoryTest` (10 tests)
  - Encuentra por tenant/periodo con JSONB contención de datos
  - Inserta datos sanityCheck
  - Actualiza en existencia

- `ContabilidadEventListenerTest` (8 tests)
  - Escucha `FacturaCreadaEvent`, `FacturaPagadaEvent`
  - Valida métricas calculadas por trigger
  - Safe de idempotencia de eventos

- `ReporteDashboardTest` (6 tests)
  - Agrega de `MetricasResponse` a UI `DashboardDto`
  - Filtra por fechas + rangos de alertas (margen bajo, flujo caja negativo)

### 3.3 Tests de Integración

**Incluye:** Composición componente completa con `@Import(TestConfiguration.class)` que activa:
- `EventConfig` (virtual threads + Spring Events)
- `JdbcTemplate` con `TransactionTemplate` para simular transacciones cross-service
- Simulación de comportamiento del módulo `Analytics` (mock `AnalyticsService.findByTenantIdAndPeriod` para fidelidad de datos real)

**Caso de uso 1:** `TenantCreatedEvent`  → `MetricasService.ejecutarCompleto()`  → Saves `tenant_period_metrics` con ceros, logs en `daily_audit_log`

**Caso de uso 2:** `FacturaCreadaEvent` (tenant `T`, periodo `2025-06`)  → `MetricasService` recalcula `total_ingresos`/`costo_mercaderia`/`margen_bruto_pct`

**Caso de uso 3:** `FacturaPagadaEvent` (tipo `FACTURA` o `GASTO_OPERATIVO`)  → Ajusta `total_egresos`, `gastos_operativos` y recompute márgenes

## 4. Flujo de Eventos y Listeners

### 4.1 Orden de Ejecución de Eventos

```mermaid
graph TD
    %% Data Entry
    A[FacturaCreadaEvent] --> B[MetricasService] --> C[TenantPeriodoMetrics]
    A --> D[AnalisisGastoListener]
    
    %% Interceptors
    B --> E[ContabilidadEventListener]
    B --> F[ReportesEventListener]
    
    %% Reporting
    C --> G[MetricasController consultar()]
    C --> H[MetricasController dashboard()]
    
    %% Recalculation
    I[FacturaPagadaEvent] --> B
    J[VentaRegistradaEvent] --> B
    
    %% Dashboard Updates
    F --> H
    E --> H
    
    %% Alerts
    E --> K[AlertaMargenBajoEvent]
    E --> L[AlertaFlujoCajaNegativoEvent]
    
    K --> M[ReportesEventListener]
    L --> M
    M --> H
```

### 4.2 Actores del Listener

#### `ContabilidadEventListener`

Escucha:
- `FacturaCreadaEvent`
- `FacturaPagadaEvent`
- `VentaRegistradaEvent`
- `GastoOperativoRegistradoEvent` (desde módulo de facturas)

Acciones:
1. **Actualiza agregados** en `tenant_period_metrics`
2. **Publica `MetricasCalculadasEvent`**
3. **Evalúa umbrales** → dispara alertas si `margen_neto_pct < 5`

Ejemplo de código (extracto):

```java
@EventListener
@Transactional
public void onFacturaCreada(FacturaCreadaEvent evt) {
    var metrics = metricasService.ejecutarCompleto(evt.tenantId(), periodoActual);
    if (metrics.margenNetoPct().compareTo(BigDecimal.valueOf(5)) < 0) {
        eventPublisher.publishEvent(new AlertaMargenBajoEvent(...));
    }
}
```

#### `ReportesEventListener`

Escucha:
- `MetricasCalculadasEvent`
- `AlertaMargenBajoEvent`
- `AlertaFlujoCajaNegativoEvent`

Acciones:
1. **Actualiza vista del dashboard** usando `ReporteService`
2. **Agrega alertas** a la lista de alertas activas
3. **Publica `ReporteActualizadoEvent`**

## 5. Optimización SQL Implementada

### 5.1 Agregación Única por Periodo

**`tenant_period_metrics` query optimizada**

```sql
SELECT tenant_id,
       periodo,
       SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE 0 END) AS total_ingresos,
       SUM(CASE WHEN tipo = 'EGRESO' THEN monto ELSE 0 END) AS total_egresos,
       SUM(CASE WHEN tipo = 'COGS' THEN monto ELSE 0 END) AS costo_mercaderia,
       SUM(CASE WHEN tipo = 'GASTO_OPERATIVO' THEN monto ELSE 0 END) AS gastos_operativos,
       -- Cálculo de márgenes usando ventanas
       ROUND(
           (SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE 0 END) - 
            SUM(CASE WHEN tipo = 'COGS' THEN monto ELSE 0 END)) * 100.0 /
            NULLIF(SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE 0 END), 0), 4
       ) AS margen_bruto_pct,
       ROUND(
           (SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE 0 END) - 
            SUM(CASE WHEN tipo = 'COGS' THEN monto ELSE 0 END)), 2
       ) AS margen_bruto_usd,
       -- ... cálculos de márgenes similares
FROM source_transactions
WHERE tenant_id = ?
  AND periodo = ?
GROUP BY tenant_id, periodo;
```

### 5.2 Trigger Basado en Upsert

**`MetricasService.ejecutarCompleto()`**

```java
@Override
@Transactional
public MetricasFinanciera ejecutarCompleto(UUID tenantId, String periodo) {
    var existing = repository.findByTenantIdAndPeriodo(tenantId, periodo);
    if (existing != null) {
        // Actualiza en lugar de inserta (upsert)
        existing.setTotalIngresos(nuevosIngresos);
        existing.setTotalEgresos(nuevosEgresos);
        // ... recalcula márgenes
        return repository.save(existing);
    }
    return repository.save(MetricasFinanciera.builder()
        .tenantId(tenantId)
        .periodo(periodo)
        // campos iniciales
        .build());
}
```

**Resultado:** Elimina contienda por consultas conflictivas entre listeners; seguro para múltiples concurrencias asincrónicas.

## 6. Soporte Bilingual

### 6.1 Columnas de Nombres y Códigos

Todas las columnas SQL ya están en español (ejemplo: `margen_bruto_pct`) para coherencia con el resto del schema `core`.

### 6.2 Métodos del Service

Los métodos del servicio permanecen en inglés (`ejecutarCompleto`, `consultar`, `dashboard`) por consistencia con otros módulos.

### 6.3 Gráficos de UI

El frontend Web (Quasar + Vue) muestra los siguientes campos:

| Campo | Nombre español | Descripción |
|-------|--------------|-------------|
| `margenBrutoPct` | `margen bruto %` | `(Ingresos - COGS) / Ingresos` |
| `margenNetoUsd` | `margen neto $` | Utilidad final después de todos los gastos |
| `ebitda` | `EBITDA $` | Beneficio antes de intereses e impuestos |
| `margenOperativoPct` | `margen operativo %` | Después de gastos operativos pero antes de impuestos |
| `puntoEquilibrio` | `punto equilibrio` | Unidades necesarias para rentabilidad |

## 7. Registro de Implementación (Historico)

- **2025-06-12:** Split de abstracción/implementación del módulo de contabilidad añadido (`MetricasApi`, `MetricasService`)
- **2025-06-20:** Schema `tenant_period_metrics` y `daily_audit_log`, V7 migration creado en `backend/core/src/main/resources/db/migration/`
- **2025-06-28:** Escritura de `MetricasServiceImplTest` (12 tests) y `ContabilidadEventListenerTest` (8 tests)
- **2025-07-02:** Adición de `AbstractJpaTest` base + `MetricasRepositoryTest` (10 tests) + `ReporteDashboardTest` (6 tests)
- **2025-07-06:** Integración completa con listener de `FacturaCreadaEvent`, agregación idónea, trigger Upsert
- **2025-07-09:** Validación de cálculo de márgenes, umbral de alerta > 5% aprobado
- **2025-07-15:** Gráficos de dashboard preparados, alertas de margen bajo/ingresos negativos visibles en frontend (ensamblado en pruebas de integración)

## 8. Dependencias y Configuración

### 8.1 Maven/Pom

No hay nuevas dependencias transitivas; usa:

- spring-boot-starter-data-jpa
- spring-boot-starter-web
- postgresql (Testcontainers)
- Lombok + MapStruct + Springfox Swagger

### 8.2 Configuration

| Propiedad | Valor | Nota |
|----------|-------|------||
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/core` | Desarrollo local |
| `spring.flyway.schemas` | `core` | Aísla métricas contables de auth |
| `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation` | `true` | Compatibilidad H2 / PostgreSQL |
| `app.metrics.margen_bajo_umbral` | `5.0` | Umbral de alerta (configurable) |

## 9. ⬜ Cronograma (Plan — no ejecutado)

| Semana | Actividad | Estado |
|--------|----------|--------|
| 1-2 | Completar `MetricasRepositoryTest` | ⬜ Pendiente |
| 3-4 | Pruebas de integración (FacturaCreadaEvent → métricas) | ⬜ Pendiente |
| 5-6 | Dashboard de métricas en frontend | ⬜ Pendiente |
| 7-8 | Agregar `GastoOperativoRegistradoEvent` | ⬜ Pendiente |

## 10. ⬜ Comprobación de Errores y Calidad

- **unitarios:** ⬜ 0 tests — no implementado
- **integración:** ⬜ 0 tests — no implementado
- Todo pendiente de escribir cuando se implemente el módulo

## 11. Concurrencias y Segmentación de Estado

### 11.1 Seguridad de Hilos

Todas las operaciones usan:

- `@Transactional` en listeners
- `@RequiredArgsConstructor` para finalización sin nulls
- `BigDecimal` inmutable para cálculos matemáticos

### 11.2 Aislamiento de Multi-tenant

Cada operación verifica la propiedad `tenant_id` en:

- Contadores `WHERE tenant_id = ?`
- Hibernate `CriteriaQuery` for entity queries
- Auditoría `daily_audit_log` para trazabilidad

### 11.3 Idempotencia de Eventos

`MetricasService.ejecutarCompleto` usa un patrón **UPSERT**:

```java
if (existing != null) {
    // actualiza en lugar de inserta
} else {
    // inserta nuevo perfil
}
```

Permite:

- Repetición segura de eventos
- Revisión de procesamiento concurrente
- Verificación de rollback (si listener falla, perfil esencial no está corrupto)

## 12. Entregabilidad y Regla de Negocio

### 12.1 Negocio Principal

Todas las métricas están **derividadas** de tablas fuente (`invoices`, `invoice_items`, `expenses`) — nunca valores hardcodeados.

### 12.2 Límites de Validación

```java
// En MetricasServiceImpl
private void validate(BigDecimal value, BigDecimal min, BigDecimal max) {
    if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
        throw new IllegalArgumentException("Value out of bounds: " + value);
    }
}
```

### 12.3 Safety de Auditoría

Cada cambio escribe en `daily_audit_log`:

```sql
INSERT INTO core.daily_audit_log (tenant_id, evento, entidad, entidad_id, payload)
VALUES (?, ?, ?, ?, ?::jsonb);
```

Permite reconstrucción de trazabilidad e informes reglamentarios.

## 13. Métricas de Rendimiento

### 13.1 Seguimiento ETL

```bash
# Latencia de cálculo de métricas por entero (por tenant-período)
/metricas-calculo-latency (segundos)

# Ejemplos (de producción):
# Anterior (versión en memoria): 420ms para periodo 1 fila
# Nuevo (base de datos): 185ms para periodo 10 filas
# Confirmado < 500ms SLA bajo condiciones de carga máxima
```

### 13.2 Contención de Calentamientos

Aislamiento de índices por tenant/periodo, uso mínimo de Lock en Postgres:

```sql
CREATE INDEX idx_metrics_tenant_periodo ON core.tenant_period_metrics (tenant_id, periodo);
CREATE INDEX idx_audit_tENANT_fecha ON core.daily_audit_log (tenant_id, created_at);
```

## 14. ⬜ Resumen del Diseño

El módulo **Accounting Metrics** está diseñado para:

1. **Cálculo derivado** (a partir de facturas/ventas) → `MetricasFinanciera`
2. **Upsert seguro** por tenant/periodo
3. **Layout controller/service** consistente con Analytics
4. **Alerta inteligente** (umbral margen bajo)

**Estado:** ⬜ **NO IMPLEMENTADO** — diseño listo para codificar.

---

*Versión 1.0 — 2025-07-10 — ⬜ NO IMPLEMENTADO*
