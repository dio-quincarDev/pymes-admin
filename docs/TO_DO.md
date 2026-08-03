## TO_DO.md

## Pendientes

### Core (backend)

- [ ] [Alta] **CRUD configuración tenant (edición)** (2026-07) — `SetupApi` solo tiene GET + `POST /onboarding`; falta la edición (PUT/PATCH) de la config del negocio (categorías, unidades, datos del tenant).
- [ ] [Baja] **Refactor Producto → InsumoTemplate** (post-MVP)
- [ ] [Baja] **Spring Security local JWT** (post-MVP)

### Frontend

Estrategia de cierre: → [`FRONTEND_PENDIENTES_STRATEGY.md`](./frontend/pymes/docs/strategies/FRONTEND_PENDIENTES_STRATEGY.md)

**Fase 1 — Quick wins (riesgo ~0)**

- [ ] [Baja] **Deprecar GastosPage** — banner + enlace a CostosPage → Gastos Fijos + sacar de la bottom nav mobile. → EXPENSES_MODEL_STRATEGY (paso 5) / FRONTEND_PENDIENTES_STRATEGY (Fase 1).
- [ ] [Baja] **PWA: pull to refresh** — En mobile, gesto nativo para refrescar datos. Nota: `usePullToRefresh.ts` existe pero no se importa en ninguna página (código muerto). (2026-08-02) → FRONTEND_PENDIENTES_STRATEGY (Fase 1).
- [ ] [Baja] **Reportes — fix ruta QuickActions** — `ver-reportes` apunta a `/dashboard/accounting`; debe ir a `/dashboard/analisis-gastos`. Emit `ver-reportes` es código muerto en `DashboardPage` (eliminar). → FRONTEND_PENDIENTES_STRATEGY (Fase 1).

**Fase 2 — Reportes (backend listo)**

- [ ] [Alta] **Reportes — panel salud financiera en Dashboard** (2026-07) — Backend listo (`financialHealth` expuesto en `AnalyticsResponse`). Falta consumirlo en UI: tipo `financialHealth` en `types/analytics.ts`, exponer en `useAnalytics`, panel de alertas financieras en `DashboardPage` (criticalAlerts + recommendations + overallHealth). → FRONTEND_PENDIENTES_STRATEGY (Fase 2).

**Fase 3 — Cierre modelo de gastos**

- [ ] [Media] **Helper "Pago de salario" en FacturasPage** — tipo `GASTO_OPERATIVO` + colaborador DIARIO + rango de días → total precargado `días × tarifa` (editable) + descripción "Salarios — {nombre}, {rango}". Requiere `FacturaRequest` +`total`/`items` opcional (backend ya lo acepta). → EXPENSES_MODEL_STRATEGY (paso 4) / FRONTEND_PENDIENTES_STRATEGY (Fase 3).
- [ ] [Media] **Dashboard: gastos desde facturas pagadas** — `useFinancialDashboard` lee facturas `GASTO_OPERATIVO` PAGADAS (por proveedor) en vez de `operating_expenses`, para coincidir con el motor. Elimina `gastoService.getAll` duplicado. → EXPENSES_MODEL_STRATEGY (paso 6) / FRONTEND_PENDIENTES_STRATEGY (Fase 3).

**Diferido (post-MVP)**

- [ ] [Alta] **Tour guiado con Driver.js** — guía de bienvenida al dashboard post-onboarding (4-5 pasos: sidebar, período, métricas, quick actions, perfil). Disparo único vía localStorage. Botón "Ayuda" en header para reiniciar. (2026-07) → FRONTEND_PENDIENTES_STRATEGY (Diferido).
- [ ] [Alta] **Dashboard UI polish** — animaciones de entrada más pulidas, hover states en stat strip, empty states más expresivos, responsive tuning. → FRONTEND_PENDIENTES_STRATEGY (Diferido).

### Gateway

- [ ] [Media] Integration tests WebTestClient + Testcontainers (2026-07)

### Auth

- [ ] [Media] **Role change cooldown (post-MVP)** — V3 migration `last_role_change_at` ✅ ya existe (`V3__add_role_change_tracking.sql`). Falta solo el cooldown check en `MemberServiceImpl` (30d FREE). → ref: EMAIL_INVITATION_STRATEGY.md
- [ ] [Baja] **Facebook OAuth2** — postergado (Meta no aprobó verificación) (post-MVP)

---

## Completado

### Core (backend)

- [x] [Alta] **Cleanup seed: remover conceptos de stock** — `template_locations` y `template_movement_reasons` eliminadas. V17 DROP TABLE + V18 Flyway DDL (movido desde SeedDataRunner). Industry codes → 8 constantes + SQL INSERT strings → 5 constantes (java:S1192). SQL optimization: dead LEFT JOIN eliminado + O(n×m) → Collectors.groupingBy(). Ver `backend/core/docs/SEED_TEMPLATES.md`.
- [x] [Alta] **Exception system (estrategia definida)** — `ErrorResponse`, `ApiResponse`, `CodigoError`, 3 custom exceptions, `GlobalExceptionHandler` (12 handlers). Migrados 18 throws en 7 services (150/150 tests). → [`docs/EXCEPTION_STRATEGY.md`](./backend/core/docs/EXCEPTION_STRATEGY.md)
- [x] [🔴] **Validar tenantId contra JWT** — `TenantValidationFilter` que compara `X-Tenant-Id` header vs `@RequestParam tenantId`, 403 si difieren. (2026-07-24)
- [x] [🔴] **`@PreAuthorize` en endpoints sensibles** — agregar `@EnableMethodSecurity` + `@PreAuthorize` en controllers (crear/actualizar/eliminar según rol) (2026-07)
- [x] [Alta] **OLS de proyección de precios en SQL** — `analisisProyeccionPrecios` usa `regr_slope/regr_intercept/regr_r2` en la query (CTEs `daily_prices` + `ranked`, filtro `data_points>=3`, `predictedPrice = slope*(n+1)+intercept`), ~50 líneas Java menos. (2026-07-31)
- [x] [Alta] **Primeros integration tests del Analytics** — `AnalyticsIntegrationTest` (5 tests, Testcontainers PG15 + Redis7): valida los 10 campos JSONB, casos vacíos, proveedor único, idempotencia. 162 unit + 5 IT = BUILD SUCCESS. (2026-07-31)
- [x] [Media] **Verificación de índices de las CTEs analytics** — EXPLAIN ANALYZE real: sin índice nuevo necesario (`invoice_items` no tiene `tenant_id`), PG auto-corrige con el volumen. (2026-07-31)
- [x] [Media] Integration tests ejecutables en CI — Job `backend-core-integration-test` en `ci.yml` corre `./mvnw verify -Dspring.profiles.active=integration` + 6 clases IT (`Analytics`, `CostoDiario`, `Factura`, `Producto`, `SetupSeed`, `ProveedorProductoEdgeCase`). (2026-08-02)

### Core — Motor de Costos ([COSTOS_ENGINE.md](./backend/core/docs/COSTOS_ENGINE.md))

- [x] [🔴] **V2 migration: collaboradores, gastos_fijos_recurrentes, config_laboral** — 3 tablas nuevas en schema `core`. Idempotente con `IF NOT EXISTS`. (2026-07-31)
- [x] [🔴] **V3 migration: costo_operativo_diario en tenant_financial_metrics** — Columna para persistir costo diario calculado. (2026-07-31)
- [x] [🔴] **Módulo `costos/` scaffold** — 3 entities (soft-delete pattern reciclando `GastoOperativo`), 3 repositories, 7 DTOs, 1 controller con 4 grupos de endpoints, 1 service con tenant guard + caching + eventos. Sin mappers MapStruct: `toResponse` manual como en `GastoServiceImpl` (ponytail). (2026-07-31)
- [x] [Alta] **GET /costos/diario** — Motor de cálculo: suma gastos fijos activos, colaboradores con conversión por frecuencia, divide por días laborales. Cruza con ventas del día para ganancia real estimada. (2026-07-31)
- [x] [Alta] **Integrar costo diario en MetricasServiceImpl CTE** — Nueva CTE `costos AS (...)` en el query consolidado. Alimenta `costoOperativoDiario` en respuestas de accounting. (2026-07-31)
- [x] [Media] **Integrar costo diario en Financial Health Engine** — Nueva señal `DAILY_COST_CONTROL` (rojo si `costoDiario > ventaDiaria × 1.2`, verde/expansión `DAILY_COST_COVERED` si `< × 0.8`). Implementado en `AnalyticsServiceImpl.analisisSaludFinanciera`. (2026-08-02)
- [x] [Media] **Frontend: CostosPage.vue** — Nueva página con 3 tabs: Colaboradores (tabla CRUD), Gastos Fijos (agrupados por categoría), Config (días laborales). Header sticky con resumen costo diario vs ventas hoy. KPI "Costo / Día" en Dashboard. (2026-07-31)
- [x] [Baja] **Dashboard: daily cost strip** — KPI "Costo / Día" en `DashboardPage.vue` (verde/rojo según ventas vs costo). No está en `StatStrip` (el dashboard ya no lo usa), pero el KPI está en pantalla. (2026-08-02)
- [x] [Media] **Tests: JPA + unit** — `CostosRepositoryTest` (AbstractJpaTest, cubre Collaborador/GastoFijo/ConfigLaboral) + `CostoServiceImplTest` (Mockito). (2026-08-02)

### Core — Modelo de Gastos Operativos ([EXPENSES_MODEL_STRATEGY.md](./backend/core/docs/strategies/EXPENSES_MODEL_STRATEGY.md))

- [x] [Alta] **Gastos reales solo con facturas PAGADAS** — `MetricasServiceImpl.computeMetrics`: eliminado CTE `opex`, `invoices_opex` filtra por `status='PAGADA'`. Doble conteo eliminado. (2026-08-02)
- [x] [🔴] **Pago de factura dispara recálculo de métricas** — `FacturaPagadaEvent` + `FacturaPagadaListener` (`@Async` + `AFTER_COMMIT`) → `markMetricsDirty`. (2026-08-02)
- [x] [Media] **Facturas GASTO_OPERATIVO sin items** — `FacturaRequest.items` opcional + campo `total`; `createFactura`/`updateFactura` aceptan monto directo cuando `tipo=GASTO_OPERATIVO`. (2026-08-02)
- [x] [Media] **Patrimonio conectado a la salud financiera** — ~~`CAPITAL_BURN`/`CAPITAL_READINESS`~~ implementadas (2026-08-02) pero **reemplazadas el mismo día** por `PAYBACK_RECOVERY` (ver ítem Recuperación de inversión abajo).
- [x] [Media] **Alinear `analisisGastoVariable` con el modelo** — revisión SQL (2026-08-02): el dashboard de gasto variable solo cuenta gastos `GASTO_OPERATIVO` PAGADA e incluye facturas sin items vía `i.total` (UNION items/header). Antes ignoraba facturas sin items y contaba REGISTRADA. +IT `gastoVariable_alineadoConModelo`. (2026-08-02)
- [x] [Baja] **Índice covering con `status`** — `V5__invoices_status_index.sql`: `idx_invoices_tenant_date_type` pasa a `(tenant_id, issue_date, type, status) INCLUDE (total)`. Elimina heap lookup en los CTEs de facturas. (2026-08-02)
- [x] [Media] **Recuperación de inversión (payback real)** — reemplaza `CAPITAL_BURN`/`CAPITAL_READINESS` por señal `PAYBACK_RECOVERY`: `plata a recuperar = capital inicial + saldo pendiente de préstamos ACTIVOS`, `meses = plata ÷ ganancia mensual` (ingresos × margen neto). Semáforo: ganancia ≤0 rojo (crítica), ≤12 verde (expansión), >24 amarillo (solo recomendación), 12-24 neutro. `PrestamoRepository.findByTenantIdAndStatus` + `financialHealth` expuesto en `AnalyticsResponse`. Tests: 4 unit (rojo/verde/amarillo/deuda ACTIVA suma al tiempo) + 2 IT (rojo, deuda ACTIVA suma al tiempo). → [`backend/core/docs/strategies/INVESTMENT_RECOVERY_STRATEGY.md`](./backend/core/docs/strategies/INVESTMENT_RECOVERY_STRATEGY.md) (2026-08-02)

### Core — Migraciones Flyway

- [x] [Alta] **Consolidar V1–V18 → V1__core_schema.sql único** — Ejecutado 2026-07-30 (estrategia ✅). `db/migration/` queda con V1 (consolidado) + V2/V3/V4 (costos engine). → [`docs/strategies/CORE_MIGRATIONS_STRATEGY.md`](./docs/strategies/CORE_MIGRATIONS_STRATEGY.md)

### Gateway

- [x] [Alta] CORS bug fix (2026-07) — Resuelto: globalcors + DedupeResponseHeader dual layer

### Auth

- [x] [🔴] **Unificar whitelists de rutas públicas** — `SecurityConfig.WHITE_LIST` y `JwtAuthenticationFilter.publicPaths` separadas. Solución: `shouldNotFilter()` ahora lee de `SecurityConfig.WHITE_LIST` vía `AntPathMatcher`. Se eliminó `publicPaths`. (2026-07-21)
- [x] [🔴] **TOCTOU en refresh token rotation** — `JwtServiceImpl.validateAndRevokeRefreshToken()` read→check→write no atómico. Dos requests concurrentes con el mismo refresh token producen dos pares válidos. Sesión hijackeable. Fix: `@Lock(PESSIMISTIC_WRITE)` en `RefreshTokenRepository.findByTokenHash()`. Test de concurrencia en `AuthApiIntegrationTest`. (2026-07-30)
- [x] [🔴] **`deleteByUserId` revertido por rollback** — `validateAndRevokeRefreshToken()` llamaba `deleteByUserId()` y luego lanzaba `TokenRevokedException`. El rollback de Spring reviertía el DELETE. Fix: `TransactionTemplate.executeWithoutResult()` para REQUIRES_NEW. (2026-07-30)
- [x] [Media] **Email casing inconsistente** — `AuthServiceImpl.completeRegistration()` no normaliza email a lowercase, `InvitationServiceImpl.registerAndAccept()` sí. Pueden crearse duplicados por case. Fix: normalizar en `completeRegistration()`. (2026-07-30)
- [x] [Baja] **`@Transactional` en métodos Redis-only** — `EmailVerificationServiceImpl.generateVerificationToken()` y `generateAndSendPendingRegistrationEmail()` tenían `@Transactional` pero solo tocan Redis. Quitado. (2026-07-30)
- [x] [🔴] **AUTH001 retorna 400 no 401** — `CodigoError.INVALID_CREDENTIALS` usaba `HttpStatus.BAD_REQUEST`. Un 401 es semánticamente correcto. Tests de integración actualizados. (2026-07-30)
- [x] [Media] **CORS `allowed-origins` sin default** — `@Value("${app.cors.allowed-origins}")` sin fallback. App no arranca sin `.env`. Fix: default `http://localhost:9200`. (2026-07-30)
- [x] [Baja] **JWT `jti` no usado** — cerrado como diseño deliberado: RTR + validación por `jti` única (2026-04-12) + Logout Global multi-session (2026-05-05). → [`backend/auth/docs/DAILY_REPORTS_AUTH_SOLUTIONS.md`](./backend/auth/docs/DAILY_REPORTS_AUTH_SOLUTIONS.md)
- [x] [Baja] **Rediseño templates email** — verification/invitation/password-reset refactorizados a paleta DESIGN.md (bronce/near-black) + layout fragment Thymeleaf compartido (`fragments/layout.html`). (2026-07-15)

### Auth — Invitación por Email (estrategia definida)

- [x] [Alta] **MVP invitaciones: maxUsers 2 + register+accept + TeamsPage** — `Tenant.java` 1→2, `InvitationRegisterRequest`, `InvitationInfoResponse`, `GET /{token}/info` + `POST /{token}/register` públicos, `InvitationServiceImpl.registerAndAccept()` transaccional, `TeamsPage.vue`, nav por roles. Sin cooldown. → [`docs/strategies/EMAIL_INVITATION_STRATEGY.md`](./docs/strategies/EMAIL_INVITATION_STRATEGY.md)

### Frontend — Completado (Fixes generales 2026-07-21)

- [x] [🔴] **tenantId sin encode en URL** — `prestamo.service.ts`, `producto.service.ts`, `factura.service.ts` usaban `?tenantId=${tenantId}` en vez de `params` object de axios. Migrados a `{ params: { tenantId } }`. (2026-07-21)
- [x] [Alta] Factura descuento porcentaje — input `%` en vez de `$`, subtotal formula, save() convierte % a monto
- [x] [Alta] Factura precio unitario por conversión — auto-calcular `precioUnitario / conv`, badge conversión
- [x] [Alta] Quitar listas infinitas — FacturasPage: `search()` por categoría; ProductosPage: tabla paginada
- [x] Spin buttons eliminados — `type="text" inputmode="decimal"` en cantidad/precio/descuento
- [x] Docker healthcheck fix — `localhost` → `127.0.0.1` (IPv6 Alpine)
- [x] Conversion UX — helper text + preview dinámico en ProductosPage
- [x] [🔴] **ProductosPage "Cargar más" arreglado** — `load()` reemplazaba filas en vez de apilar. Fix: `rows.value = p === 0 ? res.data.content : [...rows.value, ...res.data.content]`. Agregado `totalElements` del server para condición de "load more" y conteo real. (2026-07-27)

### Frontend — Completado (UX/UI Review 2026-07-14)

- [x] [Alta] **Fix UUID visible en formulario** — `categoryNameMap` + `unitNameMap` en ProductosPage + template slots explícitos. `FacturasPage` resuelve base unit UUID vía `setupUnits`.
- [x] [Alta] **Responsive dialog factura** — `col-3` → `col-xs-6 col-sm-3` en grid de inputs.
- [x] [Alta] **Compactar dialog** — Padding reducido, `standout` removido de inputs, layout más denso y funcional.
- [x] [Media] **Simplificar CategoryTabs** — Reemplazado por `q-chip` nativo con `selectable` + `active-class` y transiciones sutiles.
- [x] [Media] **No exponer UUIDs en dropdown** — Template muestra solo `productName` + badge proveedor. `category` raw nunca visible.
- [x] [Baja] **ProductosPage pres-dialog** — Responsive: `col-4` → `col-xs-6 col-sm-4`.

### Frontend — Completado (Modernización PWA 2026-07-14)

- [x] **Bottom nav mobile** — `MainLayout.vue`: `q-footer` con `q-tabs` 5 items + `q-route-tab` solo visible `<600px`. Rutas: Home, Productos, Facturas, Gastos, Más.
- [x] **EmptyState en 6 páginas** — `ProductosPage`, `ProveedoresPage`, `GastosPage`, `VentasPage`, `PrestamosPage`, `FacturasPage`. Cada uno con icono, título, descripción y CTA contextual.
- [x] **Unsaved changes guard** — `beforeRouteLeave` en `ProductosPage` y `FacturasPage`. `hasUnsavedChanges` computed. Dialog de confirmación si hay datos sin guardar.
- [x] **Keyboard shortcuts** — `useKeyboardShortcuts` composable. `N` crear, `?` ayuda, `Esc` cerrar. `Ctrl+K` global search placeholder. Help dialog con shortcuts listados.
- [x] **Error message clarity** — `loadSetup()` error: "No se pudo cargar la configuración del negocio" + "Verificar conexión con el servidor". `loadDependencies()` errors en 4 páginas: mensajes específicos por contexto.
- [x] **KpiCard DRY** — `AccountingPage.vue`: reemplazado `summaryCards` array manual por `KpiCard` importado. Eliminado template duplicado de 120+ líneas.
- [x] **Dialog animation** — `transition-show="slide-up"` + `transition-hide="slide-down"` en todos los dialogs CRUD (6 páginas). Efecto slide-up más nativo/app-like.
- [x] **SkeletonLoader skip** — Decisión documentada: tablas con datos reales no necesitan skeleton. Se eliminaron del roadmap.
- [x] **Stagger animation skip** — Ya existía `.stagger-children` en `app.scss` aplicado a KpiCard y AccountingPage. No duplicar.

### Frontend — Completado (Swiss/Grid redesign — 2026-07-15)

Todos implementados inline en cada page (sin componentes separados).

- [x] [Alta] **ProductosPage: card grid visual** — Grid de tarjetas responsivo (col-12 col-sm-6 col-md-4) con nombre, SKU, category chip, unit chip, proveedor y presentaciones.
- [x] [Media] **FacturasPage: timeline financiero** — Feed cronológico agrupado por mes con sticky headers, status badges, search + filter. Usa `InvoiceItemCard`, `CategoryTabs`, `InvoiceDetailDialog`.
- [x] [Media] **GastosPage: cards por categoría** — Tarjetas agrupadas por categoría con subtotales por grupo.
- [x] [Media] **VentasPage: calendario** — Timeline agrupado por día con totales semanales/mensuales.
- [x] [Media] **PrestamosPage: progress cards** — CSS grid con cards de progreso (`q-linear-progress`), saldo, timeline de pagos.
- [x] [Media] **ProveedoresPage: contact cards** — Grid de cards con nombre, contacto, teléfono, email y acciones.

### Frontend — Completado (Critical Fixes 2026-07-25)

- [x] [🔴] **`isAuthError()` normalizado** — `utils/errors.ts`: todas las funciones de verificación leen del error normalizado (`ApiError.code`/`.status`) primero, fallback a raw axios. Ya no dependen de estructura `AxiosError`.
- [x] [🔴] **Listener `auth:401` duplicado** — `store/index.ts`: simplificado de 5 líneas manuales a `clearSession()` directo. Guard `typeof window` para entornos no-browser.
- [x] [🔴] **`tenantId` fallback `|| ''` eliminado en 12 archivos** — reemplazado por guard `if (!tenantId) return` temprano. Form inits usan `as string`.
- [x] [🔴] **Refresh token rotation** — `boot/axios.ts`: cola de requests + flag `isRefreshing` + raw `axios.post` para evitar loop de interceptor. Si refresh falla → `clearSession()`.
- [x] [🔴] **Cobertura de tests** — 29 tests (errors + store + composables). `vitest run` pasa limpio.

### Frontend — Completado (Analytics Suite Redesign + Invitation UX/UI — 2026-07-29)

- [x] [Alta] **Rediseñar suite de análisis** — 5 componentes nuevos (`AnalyticsHeader`, `KpiCard`, `MetricCard`, `CategoryBreakdownChart`, `DataTable`). Refactor de `DashboardPage`, `AnalisisGastosPage`, `AccountingPage`. Spec: `.ulpi/design/analytics-suite.md`.
- [x] [Alta] **BaseButton label prop fix** — Agregado `label?: string` prop + fallback render. `AcceptInvitationPage` y `TeamsPage` ahora muestran texto en botones.
- [x] [Alta] **Invitation flow dark mode forms** — 7 fields en `AcceptInvitationPage` y `TeamsPage`: `outlined dense` → `dark filled color="primary" label-color="accent"`. Spec: `.ulpi/design/invitation-flow-fix.md`.

### Frontend — Completado (UX/UI Review 2026-07-29)

- [x] [Alta] **Botones +Nuevo duplicados en EmptyState** — 5 páginas (Proveedores, Gastos, Ventas, Productos, Préstamos): toolbar muestra "+Nuevo" incondicionalmente, EmptyState también. Cuando la lista está vacía se ven ambos. Fix: `v-if="rows.length"` en toolbar button. (2026-07-30) ✅
- [x] [Alta] **Contabilidad sin estado visible** — `AccountingPage.vue`: cuando `data` es null (cargando/error), `kpis` retorna array vacío y la grilla no renderiza nada. Fix: loading skeletons + empty state. (2026-07-30) ✅
- [x] [Media] **Flechas incremento/decremento en inputs number** — 7 campos `type="number"` en Patrimonio, Ventas, Préstamos (4), Gastos muestran spinners nativos del browser. Fix: CSS global ocultando `::-webkit-inner-spin-button` + `-moz-appearance: textfield` en `app.scss`. (2026-07-30) ✅
- [x] [Media] **Botón Editar en Patrimonio usa `round`** — Botón circular fuera del diseño industrial/no-nonsense. Fix: sacar `round`, dejar flat con label consistente con el resto de la app. (2026-07-30) ✅

### Frontend — Completado (Bug fixes Patrimonio + Currency formatting + Gastos field names 2026-07-30)

- [x] [🔴] **Patrimonio: field names alineados con backend** — `initialCapital`→`capitalInicial`, `startDate`→`fechaInicio`, `notes`→`notas` (read-only). `PatrimonioRequest` ahora incluye `tenantId`. Eliminado campo `notes` del form (backend no lo persiste). Valida `capitalInicial > 0` antes de enviar (backend usa `@Positive`). Envía `null` en vez de string vacío para `fechaInicio`.
- [x] [Media] **Currency formatting input** — `type="text"` + `inputmode="decimal"` + formateo al blur con `toLocaleString`. Aplicado en Patrimonio (capitalInicial), Gastos (amount), Ventas (grossAmount). Usuario escribe "10000", al salir del campo se formatea a "10,000.00".
- [x] [🔴] **Gastos: field names alineados con backend** — `category`→`categoria`, `description`→`descripcion`, `amount`→`monto`, `expenseDate`→`fecha`, `paymentMethod`→`metodoPago`. Types (`GastoOperativo`, `GastoRequest`), page (`GastosPage.vue`), composable (`useFinancialDashboard.ts`), y chart (`ExpenseBreakdown.vue`) actualizados.
- [x] [🔴] **MetricasFinancieras: field names alineados con backend** — `totalIncome`→`totalIngresos`, `costOfGoods`→`costoMercaderia`, `operatingExpenses`→`gastosOperativos`, `loanPayments`→`pagosPrestamos`, `totalExpenses`→`totalGastos`, `grossMargin`→`margenBruto`, `grossMarginPct`→`margenBrutoPct`, `operatingMargin`→`margenOperativo`, `operatingMarginPct`→`margenOperativoPct`, `netMargin`→`margenNeto`, `netMarginPct`→`margenNetoPct`. Types + DashboardPage + AccountingPage + StatStrip actualizados. (2026-07-30)

### Frontend — Completado (Dashboard financiero)

- [x] [Media] **Dashboard: sparklines** — `analytics/KpiCard.vue` renderiza SVG sparkline; `DashboardPage` pasa `trend` por KPI. Nota: usa 2 puntos `[prev, cur]` (línea degenerada pero funcional). (2026-08-02)

### Frontend — Completado (Onboarding)

- [x] [Media] **Empty states contextuales** — cada página vacía debe tener un mensaje + CTA que guíe al usuario (ej. "Agrega tu primer producto"). (2026-07)

### Frontend — Completado (Tenant/User display)

- [x] [Media] **Mostrar tenantName y userName en layout** — `authStore` captura/persiste `tenantName` (`store/index.ts` + localStorage `pymeq_tenant_name`); `App.vue` restaura vía `ensureTenantName`; `MainLayout.vue` muestra nombre+email en el menú; `AnalyticsHeader.vue` muestra empresa. Falta el "sidebar strip" puntual (nombre/email en drawer, no en header). (2026-08-02)

### Frontend — Completado (PWA)

- [x] [Baja] **PWA: custom install prompt** — Banner "Instalar PYMEQ" con dismiss persistido (`beforeinstallprompt` + `pwa_install_dismissed` en `MainLayout.vue`). (2026-08-02)
- [x] [Baja] **PWA: transiciones direccionales** — Slide left/right por profundidad de ruta en `App.vue`. Caveat: solo rutas top-level; el `router-view` interno de `MainLayout` sigue en `fade`. (2026-08-02)
