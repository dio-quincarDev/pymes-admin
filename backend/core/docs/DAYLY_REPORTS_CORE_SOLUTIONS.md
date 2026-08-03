# PROGRESS — Core Service

Registro de lo implementado y lo pendiente.

> Ver también: `CORE.md` (arquitectura + estado), `ANALYTICS.md`, `FUTURE_MODULES.md` (blueprints), `SEED_TEMPLATES.md`.

---

## Estado Rapido

| Modulo | Estado | Tests |
|--------|--------|-------|
| Setup | Implementado | 13 unit + 10 integration |
| Product | Implementado | 11 unit + 30 JPA edge cases |
| Invoice | Implementado | 7 unit + 5 integration |
| Analytics | Implementado | 6 unit + 6 integration |
| Modelo de Gastos | Implementado (backend) | 4 integration (ModeloGastosIntegrationTest) |
| Gasto | Implementado | 17 JPA |
| Prestamo | Implementado | 13 JPA |
| Inversion | Implementado | 9 JPA |
| Venta | Implementado | 13 JPA |
| Accounting | Implementado | MetricasFinanciera + CTE consolidado |
| Reportes | Pendiente | Ver FUTURE_MODULES.md |

---

## 2026-08-02 — Modelo de Gastos (backend) + revisión y optimización SQL

### Contexto

El motor de gastos operativos mezclaba dos capas (presupuesto `costos/` vs realidad `operating_expenses`) y el dashboard de gasto variable duplicaba cuentas. Se ejecutó `strategies/EXPENSES_MODEL_STRATEGY.md` (backend) y luego una revisión + optimización SQL sobre la implementación. Alcance acotado por el usuario: **solo backend**; el frontend (helpers, deprecación GastosPage, dashboard) quedó pendiente en TO_DO.

### Qué se hizo (cluster del Modelo de Gastos)

**1. Gastos reales solo con facturas PAGADAS (`MetricasServiceImpl.computeMetrics`)**

- Eliminado el CTE `opex` (duplicaba gastos). `invoices_opex` ahora filtra `type='GASTO_OPERATIVO' AND status='PAGADA'`. Doble conteo eliminado.

**2. Pago de factura dispara recálculo (`FacturaPagadaEvent` + `FacturaPagadaListener`)**

- Evento nuevo + listener `@Async @TransactionalEventListener(AFTER_COMMIT)` → `markMetricsDirty`. `pagarFactura` publica el evento. Sin esto, pagar una factura no refrescaba las métricas hasta el debounce.

**3. Facturas GASTO_OPERATIVO sin items (`FacturaRequest`)**

- `items` pasó a opcional (sin `@NotEmpty`) + campo `total` (antes de `items` en el record).
- Helpers `isGastoSinItems` + `nz` en `FacturaServiceImpl`; `createFactura`/`updateFactura` aceptan monto directo para GASTO_OPERATIVO. Validación "al menos un item" se mantiene para los demás tipos. Cierra el hueco de salarios/servicios que no pasaban por items.

**4. Salud financiera conectada al costo diario y al patrimonio (`AnalyticsServiceImpl.analisisSaludFinanciera`)**

- Nuevas señales:
  - `DAILY_COST_CONTROL` (🔴 crítica: `costoDiario > ventaDiaria × 1.2`) + expansión `DAILY_COST_COVERED`.
  - ~~`CAPITAL_BURN`/`CAPITAL_READINESS`~~ **reemplazadas** por `PAYBACK_RECOVERY` (INVESTMENT_RECOVERY_STRATEGY): `meses = (capital inicial + deuda ACTIVA) ÷ (ingresos × margen neto)`. Rojo (ganancia ≤0, crítica), verde ≤12 meses (expansión), amarillo >24 (recomendación).
- `AnalyticsServiceImpl` ganó dependencias `PatrimonioRepository` + `PrestamoRepository` → `AnalyticsServiceImplTest` actualizado (mocks en constructor).

### Revisión SQL (skill `sql-code-review`) — hallazgos

| Severidad | Hallazgo | Acción |
|-----------|----------|--------|
| ✅ 10/10 | SQL injection | 0 — todo parametrizado |
| 🔹 MEDIA | `analisisGastoVariable` no filtra PAGADA ni incluía facturas sin items → dashboard inconsistente con `operatingExpenses` | Corregido abajo |
| 🔹 BAJA | `idx_invoices_tenant_date_type` sin `status` → heap lookup | Corregido abajo |
| 🔹 BAJA | `type`/`status` VARCHAR sin CHECK | Dejado fuera (requiere validar datos existentes) |

### Optimización SQL (skill `sql-optimization`) — aplicado

**1. `analisisGastoVariable` alineado con el modelo**

- CTE partido: `items_spend` (FACTURA + GASTO_OPERATIVO PAGADA con items) + `header_spend` (GASTO_OPERATIVO PAGADA **sin items** vía `i.total`, `NOT EXISTS` items, `CAST(NULL AS UUID) AS product_id` para tipar el UNION) → `UNION ALL` → `period_data`.
- Ahora el gasto variable del dashboard incluye salarios/servicios sin items y excluye gastos REGISTRADA — consistente con `operatingExpenses`.
- Fix de aridad: el query pasó de 3 a 6 placeholders (3 por CTE) → `jdbc.query(..., tenantId, start, end, tenantId, start, end)`.
- **No tocados (por diseño):** `analisisProyeccion` (proyección de compras) y `supplierSpend` (concentración por producto) miden mercancía, no gasto operativo; filtrarlos cambiaría su semántica.

**2. Índice covering `status` (`V5__invoices_status_index.sql`)**

- `idx_invoices_tenant_date_type` reemplazado por `(tenant_id, issue_date, type, status) INCLUDE (total)` — elimina heap lookup en los CTEs de facturas. Cuidado con merges: `V1` crea el índice viejo; `V5` lo dropea y recrea con `status`.

### Tests

- `ModeloGastosIntegrationTest` (NUEVO, 6 ITs): `operatingExpenses` solo PAGADA, `DAILY_COST_CONTROL`, `paybackRecovery_perdidaSeActiva`, `paybackRecovery_deudaActivaSumaAlTiempo`, y `gastoVariable_alineadoConModelo` (PAGADA 500 + REGISTRADA 300 → invoiceCount=1, totalSpend=500). Seed arreglado insertando provider antes (FK).
- `FacturaIntegrationTest`: +`createGastoOperativoSinItems` (MockMvc).
- `FacturaServiceImplTest`: 7 constructores `new FacturaRequest(...)` actualizados (campo `total`).
- `AnalyticsServiceImplTest`: +mocks `patrimonioRepository` + `prestamoRepository`; +4 tests semáforo payback (rojo/verde/amarillo/deuda ACTIVA suma al tiempo).
- Resultado: **162 unit + 38 integration = BUILD SUCCESS** (al cerrar la revisión, 37 → 38 con el IT de gasto variable).

### Archivos modificados

```
docs/TO_DO.md                                                                   # cluster backend ✅, frontend pendiente, +2 items SQL
backend/core/docs/EXPENSES_MODEL_STRATEGY.md                                    # estado: backend implementado + nota revisión SQL
backend/core/docs/COSTOS_ENGINE.md                                              # changelog 2026-08-02
core_pymes/accounting/service/impl/MetricasServiceImpl.java                     # CTE invoices_opex filtra PAGADA, CTE opex eliminado
core_pymes/invoice/event/FacturaPagadaEvent.java                                # NUEVO
core_pymes/invoice/listener/FacturaPagadaListener.java                          # NUEVO
core_pymes/invoice/service/impl/FacturaServiceImpl.java                         # evento al pagar + helpers + branch GASTO sin items
core_pymes/invoice/dto/FacturaRequest.java                                      # items opcional + campo total
core_pymes/analytics/service/impl/AnalyticsServiceImpl.java                     # señales DAILY_COST/CAPITAL + analisisGastoVariable reescrito
resources/db/migration/V5__invoices_status_index.sql                            # NUEVO
core_pymes/integration/ModeloGastosIntegrationTest.java                         # NUEVO (4 ITs)
core_pymes/integration/FacturaIntegrationTest.java                              # +createGastoOperativoSinItems
core_pymes/invoice/service/impl/FacturaServiceImplTest.java                     # constructores actualizados
core_pymes/analytics/service/impl/AnalyticsServiceImplTest.java                 # +mock patrimonioRepository
```

### Pendiente

- Frontend del Modelo de Gastos (TO_DO L131-133): helper "Pago de salario", deprecar GastosPage, dashboard desde facturas pagadas. (a definir)
- CHECK constraints en `type`/`status` de `invoices` — add cuando se toque migración.

---

## 2026-07-31 — Analytics: OLS en SQL + primeros integration tests + verificación de índices

### Contexto

Tres frentes sobre el motor analítico del core:
1. `analisisProyeccionPrecios` aún computaba regresión OLS en Java (perezoso y frágil).
2. El módulo Analytics no tenía coverage de integración real (Testcontainers).
3. Se requería verificar con evidencia que la query OLS usa los índices correctos antes de cerrar una review.

### Qué se hizo

**1. Refactor OLS Java → SQL (`AnalyticsServiceImpl.analisisProyeccionPrecios`)**

- La regresión lineal ahora la hace PostgreSQL en la query (CTEs `daily_prices` + `ranked`):
  - `daily_prices`: precio promedio diario por producto dentro del lookback (6 meses antes del período).
  - `ranked`: `ROW_NUMBER()` por producto ordenando por `issue_date` → rn 1-based.
  - `REGR_SLOPE`, `REGR_INTERCEPT`, `COALESCE(regr_r2, 0)`, `ARRAY_AGG(price_promedio_diario ORDER BY issue_date DESC)[1]` como last_price.
  - `HAVING data_points >= 3` filtra productos con histórico insuficiente.
- Java solo calcula `predictedPrice = slope * (rn_total + 1) + intercept` (rn es 1-based; antes era 0-based) y pctChange/confidence. ~50 líneas Java menos.
- Test unitario `analisisProyeccionPrecios_computesOlsFromSqlRegression` (usa `List.<Object[]>of(...)` por el varargs de `queryForList`).

**2. Primeros integration tests del Analytics (`AnalyticsIntegrationTest`, 5 tests verdes)**

- Extiende `AbstractIntegrationTest` (Testcontainers postgres:15-alpine + redis:7-alpine via `@ServiceConnection`; Failsafe ejecuta `**/integration/**`).
- `@MockBean RecomputeDebounceService` para silenciar el `@Scheduled(fixedDelay=30000)`.
- Seed vía `JdbcTemplate` (provider, producto, factura con item, `tenant_financial_metrics.costo_operativo_diario` — obligatorio, si no `analisisGastoVariable` dispara recalculo indeterminista).
- Valida los 10 campos JSONB de `analisis_gasto` contra el esquema real.
- Aprendizajes: `opex_pct` es **array de 1 objeto** (analisisGastoVariable devuelve List), no objeto; comparativa/recomendaciones ven solo el período (junio), no el lookback.
- Resultado: 162 unit + 5 integration = BUILD SUCCESS.

**3. Verificación de índices del OLS (punto 6 de la review)**

- Test desechable `VerifyOlsIndexPlanTest`: seed 300 invoices del tenant target + 6000×3 items de noise, `EXPLAIN (ANALYZE, FORMAT TEXT)`.
- Plan real: `Bitmap Index Scan on idx_invoices_tenant` (no el compuesto) + `Seq Scan on invoice_items` (18.300 filas = tabla completa) vía hash join. **Execution: ~3.8ms.**
- **Dictamen: no hay bug, no se crea ningún índice.** A escala PYME el full-scan + hash join es el plan óptimo; al crecer la tabla PG cambia solo a nested loop con `idx_invoice_items_invoice_product` (ya existe). `idx_invoice_items_invoice_product_tenant` del análisis original es **imposible**: `invoice_items` no tiene `tenant_id` (el aislamiento multi-tenant vive en `invoices`).
- Test desechable borrado; conclusión documentada en `CORE.md`.
- Principio aplicado (del usuario): ante un test que falla, analizar la lógica antes de forzar la prueba a pasar — no se forzó nada.

### Pendiente (bug de negocio detectado, sin decidir)

~~6 queries dividen por `conversion_factor` sin `NULLIF`~~ → **RESUELTO 2026-07-31**: `NULLIF(conversion_factor, 0)` en los 6 sitios (`analisisTendencia` 128,136; `analisisMargen` 171,179; `analisisAlertas` 287,288), alineando con las 6 queries que ya lo usaban. Test `conversionFactorCero_noRompeMotores` agregado a `AnalyticsIntegrationTest` (6 IT verdes, 162 unit + 6 IT = BUILD SUCCESS). Las filas con factor 0 aportan NULL al agregado → se excluyen del AVG/STDDEV en vez de reventar el análisis.

### Archivos modificados

```
backend/core/src/main/java/core_pymes/analytics/service/impl/AnalyticsServiceImpl.java   # OLS en SQL + bug conversion_factor
backend/core/src/test/java/core_pymes/integration/AnalyticsIntegrationTest.java           # NUEVO, 5 IT verdes
backend/core/src/test/java/core_pymes/integration/VerifyOlsIndexPlanTest.java             # desechable, creado y borrado
backend/core/src/test/java/core_pymes/analytics/service/impl/AnalyticsServiceImplTest.java # test unitario OLS
backend/core/docs/CORE.md                                                                 # nota verificación de índices
```

---

## 2026-07-28 — TeamsPage migration out of core

### Cambio

`TeamsPage.vue` movido de `modules/core/pages/` → `modules/auth/pages/`. Ruta `/teams` removida de `coreRoutes` en `modules/core/router/routes.ts`.

Teams management es funcionalidad de autenticación/members, no core business logic. El módulo core ahora solo contiene: Setup, Products, Invoices, Gastos, Préstamos, Inversiones, Ventas, Contabilidad.

### Archivos modificados

```
frontend/pymes/src/modules/core/router/routes.ts   # removed /teams route
```

---

## 2026-07-21 — Exception Strategy + Frontend error consumption + SQL review

### Exception Strategy (EXCEPTION_STRATEGY.md → implementado)

Infraestructura de errores replicando el patrón de auth, 4 capas:

- **`ApiResponse<T>`**: envelope genérico `{ data, mensaje, success }` — disponible pero no forzado como wrapper global (rompe frontend existente)
- **`ErrorResponse`**: `{ codigo, mensaje, status, details }` con campo `status` int para parseo frontend
- **`CodigoError`**: enum con 10 códigos categorizados (`RES001`, `INV001`, `DUP001`, `CON001`, `VAL001-003`, `SEC001-003`)
- **`CoreApiException`**: base class con `codigo + HttpStatus` dinámico; 3 subclases:
  - `ResourceNotFoundException` (RES001 → 404)
  - `InvalidInputException` (INV001 → 400)
  - `DuplicateResourceException` (DUP001 → 409)

### GlobalExceptionHandler — reescrito

Versión anterior: Map-based con 4 handlers. Nuevo: 12 handlers con `@ExceptionHandler`:

| Handler | Input | Output |
|---------|-------|--------|
| `CoreApiException` | Cualquier subclase | `codigo` + `httpStatus` del enum |
| `MethodArgumentNotValidException` | `@Valid` falla | `details` map campo→error |
| `HttpMessageNotReadableException` | JSON malformado | 400 genérico |
| `MissingServletRequestParameterException` | Parámetro faltante | mensaje con nombre del parámetro |
| `ConstraintViolationException` | Validación Jakarta | mensaje pass-through |
| `DataIntegrityViolationException` | FK/unique violation | Mensaje user-friendly parseando `dbMsg` |
| `EntityNotFoundException` | JPA `getReferenceById` | 404 |
| `IllegalArgumentException` | Argumento inválido | 400 |
| `Exception` (catch-all) | No cubiertos | 500 + log |

**Diseño**: Los handlers de subclases de CoreApiException (`ResourceNotFound`, `InvalidInput`, `DuplicateResource`) se removieron por redundancia — el handler de `CoreApiException` las cubre a todas vía polimorfismo.

### Migración de servicios — 18 throws actualizados

| Servicio | Cambio | Archivos |
|----------|--------|----------|
| `FacturaServiceImpl` | `EntityNotFound`→`ResourceNotFound`, `IllegalState`→`InvalidInput` (8 throws) | `FacturaServiceImpl.java` |
| `InvoiceCalculator` | `IllegalArgument`→`InvalidInput` (3 throws) | `InvoiceCalculator.java` |
| `ProductoServiceImpl` | `EntityNotFound`→`ResourceNotFound`, `IllegalArgument`→`InvalidInput` (3 throws) | `ProductoServiceImpl.java` |
| `GastoServiceImpl` | `EntityNotFound`→`ResourceNotFound` (1 throw) | `GastoServiceImpl.java` |
| `VentaServiceImpl` | `EntityNotFound`→`ResourceNotFound` (1 throw) | `VentaServiceImpl.java` |
| `PrestamoServiceImpl` | `EntityNotFound`→`ResourceNotFound` (1 throw) | `PrestamoServiceImpl.java` |
| `SetupServiceImpl` | `IllegalState`→`InvalidInput` (1 throw) | `SetupServiceImpl.java` |

### Tests

- **GlobalExceptionHandlerTest**: 13 tests cubriendo todos los handlers + lógica de `DataIntegrityViolationException` message parsing
- **FacturaServiceImplTest**: 7 tests actualizados a `InvalidInputException`/`ResourceNotFoundException`
- **ProductoServiceImplTest**: 3 tests actualizados a `ResourceNotFoundException`
- **SetupServiceImplTest**: 3 tests actualizados a `InvalidInputException`
- **Total**: 150/150 tests pasando (137 originales + 13 nuevos handler)

### Skip — Fase 4 (ApiResponse wrapper en controllers)

Decidido no implementar. Razón: el frontend consume los ~20 endpoints core directo (recibe `{ id, nombre, ... }`). Envolver en `ApiResponse { data: {...} }` requiere reescribir cada controller y cada llamado frontend para extraer `.data`. Error handling ya está cubierto por `GlobalExceptionHandler` + `ErrorResponse`. `ponytail: agregar por endpoint cuando frontend lo pida explícitamente.`

### Frontend — 32 catches migrados de genérico a backend message

Las páginas core usaban `catch { }` sin parámetro y mostraban mensajes fijos. Migrados a `catch (err) { $q.notify({ message: err instanceof Error ? err.message : 'fallback' }) }`.

El interceptor axios (`boot/axios.ts`) ya normaliza errores a `new Error(mensaje_del_backend)` con propiedades `code/status/details/isBackendError`. Las páginas auth ya consumían esto; ahora las 12 páginas core también.

**Archivos**: `PatrimonioPage`, `PrestamosPage`, `AnalisisGastosPage`, `ProductosPage`, `ProveedoresPage`, `ConfiguracionPage`, `AccountingPage`, `FacturasPage`, `VentasPage`, `GastosPage`, `OnboardingPage`, `PresentacionesDialog`

### SQL Review — Hallazgos (ver SKILL.md output completo)

| Severidad | Hallazgo | Acción |
|-----------|----------|--------|
| ✅ (ninguno) | SQL injection | 0 — todo parametrizado |
| ✅ (ninguno) | Índices duplicados V16 | Corregido en sesión anterior |
| 🔹 LOW | `conversion INTEGER` no soporta factores fraccionarios | Pendiente: cambiar a `NUMERIC(10,4)` |
| 🔹 LOW | `loans(tenant_id)` single-column | Aceptado: PYME, <500 loans |

### Archivos creados

```
common/dto/ApiResponse.java              # Envelope genérico
common/dto/ErrorResponse.java            # { codigo, mensaje, status, details }
common/exception/CodigoError.java        # 10 códigos categorizados
common/exception/CoreApiException.java   # Base class con HttpStatus dinámico
common/exception/custom/ResourceNotFoundException.java
common/exception/custom/InvalidInputException.java
common/exception/custom/DuplicateResourceException.java
common/exception/GlobalExceptionHandler.java  # 12 handlers
unit/GlobalExceptionHandlerTest.java     # 13 tests
```

### Archivos modificados

```
7 servicios migrados a nuevas excepciones
12 páginas/vue + 1 componente core actualizados a catch con err.message
GAPS.md actualizado (gap EXCEPTION_STRATEGY.md marcado ✅)
```

---

## 2026-07-24 — TenantId validation filter (gap crítico)

### TenantValidationFilter

`X-Tenant-Id` header (inyectado por gateway desde JWT) vs `?tenantId=` param (frontend). Si difieren → 403.

- `OncePerRequestFilter` no, `Filter` simple — Spring Boot lo registra automáticamente con `@Component`
- Si alguno falta (rutas públicas, POST sin param), pasa sin validar
- Sin nuevas dependencias, sin cambios en controllers
- 150 tests, 0 fallos

### Archivos

```
common/config/TenantValidationFilter.java    # +1 archivo, ~25 líneas
```

---

## 2026-07-27 — @PreAuthorize + role-based access control (gap crítico)

### Problema

El gateway inyecta `X-User-Role` (OWNER/ADMIN/CONTABLE/VIEWER) en todos los requests que llegan al core, pero el core nunca lo leía. Cualquier usuario autenticado podía crear, actualizar o eliminar datos sin importar su rol.

### Solución

Agregado Spring Security al core service para authorization basada en roles, sin JWT validation (eso lo sigue haciendo el gateway).

**Componentes:**

| Componente | Archivo | Responsabilidad |
|------------|---------|-----------------|
| `RoleHeaderFilter` | `common/config/RoleHeaderFilter.java` | `OncePerRequestFilter` que lee `X-User-Role` → crea `UsernamePasswordAuthenticationToken` con `ROLE_<rol>` → lo setea en `SecurityContextHolder` |
| `SecurityConfig` | `common/config/SecurityConfig.java` | `@EnableMethodSecurity`, `SecurityFilterChain` stateless, CSRF off, todas las rutas `permitAll()` (gateway ya autentica), registra `RoleHeaderFilter` antes de `UsernamePasswordAuthenticationFilter` |
| `@PreAuthorize` | 10 controllers (interfaces `*Api.java`) | `@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")` en 18 endpoints WRITE |

**Matriz de permisos:**

| Rol | Leer (GET) | Crear/Editar/Eliminar |
|-----|-----------|----------------------|
| OWNER | ✅ | ✅ |
| ADMIN | ✅ | ✅ |
| CONTABLE | ✅ | ❌ |
| VIEWER | ✅ | ❌ |

**Endpoints protegidos (18 WRITE):**

| Controller | Endpoints con `@PreAuthorize` |
|------------|------------------------------|
| `SetupApi` | `POST /{tenantId}/onboarding` |
| `ProductoApi` | `POST`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/presentaciones`, `DELETE /presentaciones/{id}` |
| `ProveedorApi` | `POST`, `PUT /{id}`, `DELETE /{id}` |
| `FacturaApi` | `POST`, `PUT /{id}`, `POST /{id}/pagar`, `DELETE /{id}` |
| `GastoApi` | `POST`, `PUT /{id}`, `DELETE /{id}` |
| `VentaApi` | `POST`, `PUT /{id}`, `DELETE /{id}` |
| `PrestamoApi` | `POST`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/pagos` |
| `MetricasApi` | `POST /recalcular` |
| `AnalyticsApi` | `POST /recalcular` |
| `PatrimonioApi` | `PUT /{tenantId}` |

**Dependencias agregadas:**

- `spring-boot-starter-security` en `core/pom.xml`
- `spring-security-test` (scope test) en `core/pom.xml`

**Tests:**

- 4 integration tests actualizados: `@WithMockUser(roles = "OWNER")` en `ProductoIntegrationTest`, `FacturaIntegrationTest`, `ProveedorProductoEdgeCaseIntegrationTest`, `SetupSeedIntegrationTest`
- Fix pre-existente: `$.error` → `$.message` en `SetupSeedIntegrationTest.completeOnboarding_InvalidIndustry_ReturnsBadRequest` (ErrorResponse record no tiene campo `error`)
- Resultado: 172 tests (150 unit + 22 integration), 0 failures

**Archivos creados (2):**

```
common/config/RoleHeaderFilter.java
common/config/SecurityConfig.java
```

**Archivos modificados (15):**

```
pom.xml                                    +spring-boot-starter-security +spring-security-test
product/controller/ProductoApi.java        +@PreAuthorize en 5 endpoints
invoice/controller/FacturaApi.java         +@PreAuthorize en 4 endpoints
invoice/controller/ProveedorApi.java       +@PreAuthorize en 3 endpoints
gasto/controller/GastoApi.java             +@PreAuthorize en 3 endpoints
venta/controller/VentaApi.java             +@PreAuthorize en 3 endpoints
prestamo/controller/PrestamoApi.java       +@PreAuthorize en 4 endpoints
inversion/controller/PatrimonioApi.java    +@PreAuthorize en 1 endpoint
accounting/controller/MetricasApi.java     +@PreAuthorize en 1 endpoint
analytics/controller/AnalyticsApi.java     +@PreAuthorize en 1 endpoint
setup/controller/SetupApi.java             +@PreAuthorize en 1 endpoint
integration/ProductoIntegrationTest.java   +@WithMockUser(roles = "OWNER")
integration/FacturaIntegrationTest.java    +@WithMockUser(roles = "OWNER")
integration/ProveedorProductoEdgeCaseIntegrationTest.java  +@WithMockUser(roles = "OWNER")
integration/SetupSeedIntegrationTest.java  +@WithMockUser(roles = "OWNER") + fix $.error→$.message
```

---

## 2026-07-15 — Invoice Update + Audit Fields

### Feature: `PUT /api/v1/core/invoices/{id}?tenantId=...`

Nuevo endpoint para editar facturas en estado `REGISTRADA`. La facturas pagadas o eliminadas no se pueden editar.

**Flujo:**
1. Verifica status = `REGISTRADA` (si no, `IllegalStateException`)
2. `reverseProductStats()` — revierte `total_investment` de los items viejos, subquery para recuperar `last_unit_price`/`last_purchase_date` anterior
3. `factura.getItems().clear()` — orphanRemoval deletes from DB
4. Reconstruye items con `InvoiceCalculator.resolve()` — batch-loads presentaciones
5. Actualiza header (proveedor, fecha, tipo, descuento, método de pago)
6. Recalcula total

**Archivos:**

| Archivo | Cambio |
|---------|--------|
| `FacturaApi.java` | `update(@PathVariable UUID id, @RequestParam UUID tenantId, @Valid @RequestBody FacturaRequest)` |
| `FacturaController.java` | Implementa `update()` |
| `FacturaService.java` | `updateFactura(UUID id, UUID tenantId, FacturaRequest)` |
| `FacturaServiceImpl.java` | `updateFactura()` + `buildItem()` extraído + `reverseProductStats()` |

### Audit fields en `ItemFactura`

5 columnas nuevas para preservar input crudo del usuario:

| Columna | Tipo | Uso |
|---------|------|-----|
| `cantidad_presentacion` | NUMERIC(19,6) | Cantidad en presentación (no convertida) |
| `valor_presentacion` | NUMERIC(19,6) | Valor de la presentación |
| `precio_unitario_input` | NUMERIC(19,6) | Precio unitario crudo del input |
| `descuento_input` | NUMERIC(19,6) | Descuento crudo del input |
| `descuento_es_porcentaje` | BOOLEAN | Si el descuento es % o monto fijo |

`presentacion_id` ahora nullable (antes `NOT NULL`). Legacy fields (`cantidad`, `precioUnitario`, `descuento`) ahora opcionales — mínimo 2 requeridos en service.

**Archivos:**

| Archivo | Cambio |
|---------|--------|
| `ItemFactura.java` | 5 columnas nuevas, `presentacion_id` nullable |
| `ItemFacturaRequest.java` | 5 campos nuevos, `@Positive` eliminado |
| `ItemFacturaResponse.java` | 5 campos nuevos en response |

### `InvoiceCalculator` — lógica de cálculo extraída

Nuevo archivo `InvoiceCalculator.java` centraliza resolución de cantidades y descuentos. Usado tanto por `createFactura` como por `updateFactura`. `buildItem()` extraído como método privado reutilizable.

### `reverseProductStats()` en `deleteFactura`

`deleteFactura()` ahora llama `reverseProductStats()` antes de borrar — antes solo hacía `repository.delete()` y los stats del producto quedaban inconsistentes.

### Migraciones

- `V15__invoice_item_audit_fields.sql` — 5 columnas audit en `core.invoice_items`
- `V16__invoice_performance_indexes.sql` — índices para queries de `reverseProductStats`

### Tests

- `mockPresentaciones()` helper: mockea `findAllById` + `findById` con `lenient()`
- `ItemFacturaRequest` constructor actualizado con 5 campos nuevos (nullable)

### Refactoring

- `createFactura()` y `updateFactura()` comparten `buildItem()` — antes la lógica estaba duplicada
- `presentacionRepository.findById()` → batch `findAllById()` + Map lookup (1 query en vez de N)

---

### Concepto

Motor de inteligencia financiera compuesto que cruza datos de los 9 motores SQL + accounting para producir:
- **Alertas críticas** (NEGATIVE_OPERATING_MARGIN, MARGIN_EROSION, SUPPLIER_CONCENTRATION, OVER_LEVERAGED, OPEX_CREEP, DEAD_INVENTORY)
- **Señales de inversión** (HEALTHY_MARGIN_STACK, POSITIVE_CASH_FLOW, LOW_CONCENTRATION, DEBT_CAPACITY)
- **Readiness de expansión** (SUSTAINED_PROFITABILITY, OPERATING_LEVERAGE, SUPPLIER_MATURITY, DEBT_CUSHION)
- **Scoring compuesto** 0-100: profitability(35%) + efficiency(25%) + stability(25%) + growth(15%)

### Arquitectura

- No es un motor SQL independiente — es un motor compuesto que lee resultados pre-computados
- Inputs: `MetricasRepository` (3 márgenes) + `AnalisisGasto` (ABC, supplier, trend, alerts) + `ProductoRepository` (lastPurchaseDate)
- Salida: JSONB nullable en `expense_analysis.financial_health` (V15)
- Trigger: hereda debounce del analytics (Redis SETNX + @Scheduled)

### Documentación

- `CORE.md` §Motor de Salud Financiera — sección completa con inputs, señales, scoring, JSON de salida
- `ANALYTICS.md` — Motor #10 agregado a tabla de motores + Flyway V15
- `KPIs.md` — pendiente de actualizar estado a "implementado" con referencias

### Archivos tocados

```
backend/core/docs/CORE.md                    # +sección Financial Health Engine, diagrama actualizado
backend/core/docs/ANALYTICS.md               # +Motor #10, V15, registro histórico
```

### Pendiente (implementación)

- [ ] V15 migration: `ALTER TABLE core.expense_analysis ADD COLUMN financial_health JSONB;`
- [ ] `AnalisisGasto.java`: +`financialHealth` campo JSONB
- [ ] `AnalyticsResponse.java`: +`FinancialHealthResponse` DTO + campo
- [ ] `AnalyticsMapper.java`: mapear nuevo campo
- [ ] `AnalyticsServiceImpl.java`: +`analisisSaludFinanciera()` (método compuesto)
- [ ] `types/analytics.ts` (frontend): +`FinancialHealth` interface
- [ ] `useAnalytics.ts`: +`financialHealth` computed
- [ ] `AnalisisGastosPage.vue`: nueva sección "Salud Financiera"

---

## 2026-07-12 — Paginated product search + lazy-load categories + batch fetch + edge cases

### Feature — `GET /core/productos/search`

- `ProductoService.search(tenantId, category, search, pageable)`: 4 paths → repositorio derivado con `ContainingIgnoreCase`
- `ProductoApi.search()`: endpoint paginado con filtros opcionales (category, name)
- Batch fetch en `search()`: 3 queries vs N+1 anterior. `mapPresentacionesBatch()` + `mapProveedoresBatch()` reemplazan llamada perezosa por `findByProductoIdIn`/`findByIdIn` con Map lookup
- Fix: `HashMap` en vez de `Map.of()` para `proveedoresMap` — `Map.of()` lanza NPE con `providerId=null`

### Feature — `GET /core/setup/{tenantId}/categories`

- `SetupService.getCategories()`: lightweight tree builder (category + count), sin cargar productos

### SQL

- `V14__product_search_indexes.sql`: `idx_products_tenant_category` (composite tenant+category) + `idx_products_active_tenant` (partial WHERE is_active)

### Tests — Edge cases con Testcontainers

- `ProductoRepositoryTest`: +9 edge cases (134 total)
- Paginación extrema: page beyond total (page=99), page size 1 (5 páginas)
- SQL special chars: `%`, `_`, `'`, `O'Brien` — no rompen query
- Empty search string → all results, no match → empty page
- Tenant sin productos, producto con `providerId=null`
- Batch: IDs inexistentes, cross-tenant isolation
- Unit tests (`ProductoServiceImplTest`): 4 filter combinations + batch fetch verification

### Files

```
product/controller/ProductoApi.java                + GET /search
product/controller/impl/ProductoController.java    delegación
product/service/ProductoService.java               + search()
product/service/impl/ProductoServiceImpl.java      search + batch fetch helpers
product/repository/ProductoRepository.java         +4 paginated query methods
product/repository/PresentacionRepository.java     +findByProductoIdInAndIsActiveTrue
invoice/repository/ProveedorRepository.java        +findByIdIn
setup/controller/SetupApi.java                     + GET /{tenantId}/categories
setup/controller/impl/SetupController.java         delegación
setup/service/SetupService.java                    + getCategories()
setup/service/impl/SetupServiceImpl.java           getCategories implementation
common/constant/CorePath.java                      + CATEGORIES_ROUTE
db/migration/V14__product_search_indexes.sql       +2 indexes
test/.../jpa/ProductoRepositoryTest.java           +9 edge case tests
test/.../unit/ProductoServiceImplTest.java         5 tests (search delegation + batch)
test/.../unit/SetupServiceImplTest.java            +getCategories tests
```

---

### Nuevos modulos

- `gasto/`: GastoOperativo entity + CategoriaGasto enum (SALARIOS, AGUA, LUZ, INTERNET, ALQUILER, MANTENIMIENTO, PUBLICIDAD, OTROS) + CRUD endpoints
- `prestamo/`: Prestamo entity + PagoPrestamo entity + EstadoPrestamo enum (ACTIVO, PAGADO, CANCELADO) + CRUD + pagos
- `inversion/`: Patrimonio entity (PK=tenant_id, 1 por tenant) + get-or-create + update
- `venta/`: VentaDiaria entity + CRUD endpoints
- `accounting/`: MetricasFinanciera entity + consultar + recalcular endpoints

### Migraciones

- V12: 6 tablas (operating_expenses, loans, loan_payments, patrimony, daily_sales, tenant_financial_metrics)
- V13: 2 covering indexes (idx_invoices_tenant_date_type INCLUDE total, idx_loan_payments_loan_date INCLUDE amount)

### SQL review

- Fix division por cero en analisisABC (grand_total > 0 guard)
- Removidos indices redundantes: idx_operating_expenses_tenant, idx_daily_sales_tenant

### Redis debounce

- RecomputeDebounceService: SETNX con TTL 1h, @Scheduled(fixedDelay=30s)
- Listeners (FacturaCreadaListener, GastoCreadaListener, VentaCreadaListener) ahora marcan dirty en Redis en vez de llamar services directamente
- AbstractIntegrationTest: +Redis Testcontainer (redis:7-alpine + @ServiceConnection)

### Tests

- 104 tests total (60 existentes + 44 nuevos), 0 failures
- JPA tests: GastoRepositoryTest (17), PrestamoRepositoryTest (13), VentaRepositoryTest (13), PatrimonioMetricasRepositoryTest (9)

### Files

```
gasto/           controller/domain/dto/event/listener/mapper/repository/service
prestamo/        controller/domain/dto/mapper/repository/service
inversion/       controller/domain/dto/mapper/repository/service
venta/           controller/domain/dto/event/listener/mapper/repository/service
accounting/      controller/domain/dto/mapper/repository/service
common/service/  RecomputeDebounceService.java
V12__expense_sales_accounting.sql
V13__performance_indexes.sql
AbstractIntegrationTest.java (actualizado con Redis container)
```

---

## 2026-07-08 — Supplier analytics: cross-supplier comparison + price predictions + recommendations

- `V11__analytics_supplier_fields.sql`: 3 columnas JSONB en `expense_analysis` — `supplier_comparison`, `supplier_recommendations`, `price_predictions`
- `AnalisisGasto.java`: +3 String con `@JdbcTypeCode(SqlTypes.JSON)`
- `AnalyticsServiceImpl.java`:
  - `analisisComparativaProveedores()`: SQL parametrizado — avg/min/max/stddev de precio por producto-proveedor
  - `analisisRecomendacionProveedor()`: recomienda proveedor más barato + `savings_pct`
  - `analisisProyeccionPrecios()`: OLS lineal por producto, predice precio próximo mes con R²
  - `analisisAlertas()`: nueva alerta `SUPPLIER_PREMIUM` (>15% sobre avg del producto)
- `FacturaServiceImpl.createFactura()`: bugfix — +`.proveedor(proveedor)` para `providerName` en items
- `ProveedorProductoEdgeCaseIntegrationTest`: 4 tests (producto sin proveedor, proveedor sin facturas, precio mínimo, todas las alertas)
- `ProductoIntegrationTest.createAndGetProduct`: removido assertion `$.createdAt isNotEmpty`
- Tests: 60 unit + 22 integration (82 total, all green)

### Files
```
core/analytics/domain/AnalisisGasto.java
core/analytics/service/impl/AnalyticsServiceImpl.java
core/analytics/dto/AnalyticsResponse.java
core/analytics/mapper/AnalyticsMapper.java
core/invoice/service/impl/FacturaServiceImpl.java
resources/db/migration/V11__analytics_supplier_fields.sql
```

---

## 2026-07-06 — Fix: productos guardan category_id (UUID) en vez de category_name

### Backend

- `SetupServiceImpl.completeOnboarding()`: Query cambió de `tc.name AS category_name` a `COALESCE(tp.category_id::text, tc.id::text) AS category_code`. `TemplateProductRow.categoryName` renombrado a `categoryCode`.
- `V8__fix_product_category.sql`: Migration que actualiza productos existentes con nombre de categoría → UUID vía JOIN con `template_categories` + `core.tenant_setup`.
- Tests: 60 unit tests core siguen pasando.

### Archivos modificados

```
core/setup/service/impl/SetupServiceImpl.java
resources/db/migration/V8__fix_product_category.sql
```

---

## 2026-07-01 — presentacionId opcional + SKU auto-generado + @Valid enforcement

### Backend

- `ItemFacturaRequest.presentacionId` → `@NotNull` removido, ahora es `UUID` nullable
- `FacturaServiceImpl.createFactura()`: cuando `presentacionId` es null, usa `conversionFactor = 1` y no busca presentación
- `FacturaController.create()` → `@Valid` agregado para validación 400 en vez de 500
- `ProductoServiceImpl.create()`: genera SKU `P-XXXX` secuencial cuando `sku` es null/blank
- `ProductoRepository` → +`countByTenantId(UUID)` para secuencia de SKU

### Archivos modificados

```
core/invoice/dto/ItemFacturaRequest.java       # presentacionId @NotNull removido
core/invoice/service/impl/FacturaServiceImpl.java  # handle null presentacionId
core/invoice/controller/impl/FacturaController.java # +@Valid on create()
core/product/service/impl/ProductoServiceImpl.java  # auto SKU generation
core/product/repository/ProductoRepository.java     # +countByTenantId()
```

---

## 2026-06-30 — Template Products: seed, copia al onboarding, frontend

### Implementado

- DDL `template_products` + `template_product_presentations` en `SeedDataRunner.createTables()` (CREATE TABLE IF NOT EXISTS, sin FK)
- ~20-25 productos por industria con 1-2 presentaciones c/u (~160 productos + ~280 presentaciones total)
- `addProd()` helper para batch arrays de producto + presentaciones en una línea
- `SetupResponse.ProductTemplateDTO(id, name, baseUnit, categoryName)` + campo `products` + factory `preview()`
- `SetupMapper` firma actualizada con 5to parámetro `products`
- `SetupServiceImpl.completeOnboarding()` — copia template_products → `core.products` con SKU auto `P-0001` secuencial + `core.product_presentations` batch insert
- `SetupServiceImpl.loadIndustryData()` — query con JOIN a template_categories para categoryName
- Frontend: `ProductTemplateDTO` type, sección "Productos precargados (N)" en OnboardingPage step 2
- 5 tests nuevos en `SetupSeedIntegrationTest`: seed counts, preview products, onboarding copy con SKU, tenant isolation
- Los tests verifican: 25 productos restaurante, SKU P-0001..P-0025, presentaciones > productos, 2 tenants aislados

### Decisiones

- **Opción A (SeedDataRunner DDL)** sobre Flyway V7/V8 — menos archivos, mismo patrón que template_units existente
- **Sin SKU en template** — se genera `P-%04d` al copiar al tenant
- **Sin FK** en template_product_presentations — datos readonly, orphan aceptable
- **JdbcTemplate batch** en vez de JPA para la copia — más directo para copia plana sin lógica de negocio

---

## Implementado

### Setup (`core_pymes/setup/`)
- `TenantSetup` entity JPA (tenantId, industry, onboardingCompleted)
- `TenantSetupRepository` (findByTenantId, existsByTenantId)
- `SetupService` interface + `SetupServiceImpl` (getOrInitialize lazy, completeOnboarding)
- `SetupApi` / `SetupController`
  - `GET /api/v1/core/setup/{tenantId}` — lazy init + template data (incl. products)
  - `POST /api/v1/core/setup/{tenantId}/onboarding` — completa onboarding + copia productos con SKU auto
  - `GET /api/v1/core/setup/preview/{industry}` — preview categorías + productos
- `SetupResponse` record DTO (id, tenantId, industry, onboardingCompleted, categories[], units[], locations[], products[])
- `SetupMapper` (MapStruct, 5 parámetros)
- Flyway V1: `core.tenant_setup`
- `buildCategoryTree()` con mapa O(n)

### Product (`core_pymes/product/`)
- `Producto` entity JPA (UUID, tenantId, soft-delete `@SQLDelete`+`@Where`, timestamps)
- `Presentacion` entity JPA (FK → producto, soft-delete, dual-field pattern)
- `ProductoRepository` + `PresentacionRepository`
- `ProductoService` / `ProductoServiceImpl` (CRUD + tenant isolation)
- `ProductoApi` / `ProductoController`
  - `GET/POST/PUT/DELETE /api/v1/core/productos`
  - `GET/POST /api/v1/core/productos/{id}/presentaciones`
  - `DELETE /api/v1/core/productos/presentaciones/{id}`
- DTOs: `ProductoRequest/Response`, `PresentacionRequest/Response` (Java records)
- `ProductoMapper` (MapStruct)
- Eventos: `ProductoCreadoEvent`, `PresentacionCreadaEvent`
- Flyway V3: `core.products`, `core.product_presentations`

### Invoice (`core_pymes/invoice/`)
- `Proveedor` entity JPA (UUID, tenantId, soft-delete)
- `Factura` entity JPA (UUID, items cascade ALL, FK → proveedor + productos)
- `ItemFactura` entity JPA (productName snapshot, subtotal, `presentacionId`+`conversionFactor`)
- `ProveedorRepository` + `FacturaRepository`
- `FacturaService` / `FacturaServiceImpl` (CRUD + total auto-calc + estado workflow)
- `ProveedorApi` / `ProveedorController`
- `FacturaApi` / `FacturaController`
  - `GET/POST/PUT/DELETE /api/v1/core/proveedores`
  - `GET/POST /api/v1/core/facturas`
  - `POST /api/v1/core/facturas/{id}/pagar`
  - `DELETE /api/v1/core/facturas/{id}`
- DTOs: `ProveedorRequest/Response`, `FacturaRequest/Response`, `ItemFacturaRequest/Response`
- `FacturaMapper` (MapStruct)
- Eventos: `FacturaCreadaEvent`
- Flyway V4: `core.providers`, `core.invoices`, `core.invoice_items`
- Invoice number: `F-PROV-{year}-{sequential:04d}` por tenant

### Analytics (`core_pymes/analytics/`)
- `AnalisisGasto` entity (JSONB por tenant/periodo)
- 6 motores CTE: ABC, tendencias, márgenes, opex, proyección, alertas
- Listener conectado a FacturaCreadaEvent
- Ver `ANALYTICS.md` para detalles

### Infraestructura
- Dockerfile multi-stage (sin `dependency:go-offline`)
- docker-compose.yml: core-service con healthcheck, depende de postgres+redis+auth
- Gateway route `/api/v1/core/**` → `pymes-core-service:8082`
- EventConfig: `@EnableAsync` + virtual threads
- CorePath.java con constantes de ruta
- Perfiles dev/stg/prod
- MapStruct + Lombok + annotation processor
- OpenFeign configurado (sin clientes aún)
- Actuator habilitado (health)
- `CacheConfig.java`: `@EnableCaching` + `RedisCacheManager` (TTL 5min, JSON serializer)
- `@Cacheable` en findAll/findById de productos, proveedores, facturas
- `@CacheEvict(allEntries=true)` en writes
- `@ConditionalOnBean(RedisConnectionFactory.class)` — solo con Redis

### PWA / Offline
- `StaleWhileRevalidate` para `/api/v1/core/*` GETs
- Banner offline en `MainLayout.vue`
- Diálogo de actualización con `SKIP_WAITING`

### Seed Data
- Flyway V2: `industries`, `template_categories`, `template_locations`
- `SeedDataRunner`: idempotente, 8 industrias, 8 tablas template
- Crea via DDL: `template_units`, `template_movement_reasons`, `template_payment_methods`, `template_products`, `template_product_presentations`
- ~160 productos + ~280 presentaciones en seed

### Testing
- `SetupServiceImplTest`: 5 unit tests (Mockito)
- `AbstractIntegrationTest`: base class Testcontainers PostgreSQL
- `SetupSeedIntegrationTest`: 10 integration tests (6 original + 4 nuevos: preview products, onboarding copy SKU, tenant isolation)
- `CoreApplicationTests`: smoke test contexto
- `ProductoServiceImplTest`: 6 unit tests (CRUD + eventos + tenant isolation)
- `FacturaServiceImplTest`: 7 unit tests (total calc + descuento + estados + tenant isolation)
- `AnalyticsServiceImplTest`: 5 unit tests (6 motores + upsert + consulta)
- `AnalyticsRepositoryTest`: 4 JPA tests (CRUD JSONB + tenant isolation)
- `ProductoIntegrationTest`: 4 integration tests (pendiente Docker)
- `FacturaIntegrationTest`: 4 integration tests (pendiente Docker)

### Refactoring (2026-06)
- Eliminado `FacturaPagadaEvent` — evento sin listener
- JSONB: `AnalisisGasto` actualizado a `@JdbcTypeCode(SqlTypes.JSON)`

---

## Pendiente

### Inmediato
- [ ] Accounting — `core_pymes/accounting/`: MetricaFinanciera + listener (ver FUTURE_MODULES.md)
- [ ] Ejecutar integration tests (requiere Docker)

### Mediate
- [ ] Reports — dashboard consolidado con KPIs, alertas
- [ ] CRUD configuración (edición por tenant)
- [ ] Módulo Ventas
- [ ] Movimientos de stock

### Infraestructura
- [ ] Spring Security (JWT validation local)
- [ ] FeignClient para Auth

### Frontend
- [x] Onboarding post-login
- [x] Módulo Core frontend (Productos, Proveedores, Facturas)
- [x] Onboarding 2 pasos con preview de categorías + productos precargados
