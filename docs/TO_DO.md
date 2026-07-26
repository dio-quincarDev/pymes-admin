## TO_DO.md

### Core

- [ ] [Alta] **Cleanup seed: remover conceptos de stock** — `template_locations` y `template_movement_reasons` no pertenecen a un sistema financiero. Incluye: V17 migration (DROP TABLE locations), SeedRunner cleanup (industry codes → constantes java:S1192), SetupServiceImpl/Response/Mapper, frontend types+pages. Ver `backend/core/docs/SEED_TEMPLATES.md` → Cleanup 2026-07.
- [x] [Alta] **Exception system (estrategia definida)** — `ErrorResponse`, `ApiResponse`, `CodigoError`, 3 custom exceptions, `GlobalExceptionHandler` (12 handlers). Migrados 18 throws en 7 services (150/150 tests). → [`docs/EXCEPTION_STRATEGY.md`](./backend/core/docs/EXCEPTION_STRATEGY.md)
- [ ] [🔴] **Validar tenantId contra JWT** — interceptor/filtro que compare `X-Tenant-Id` header (del gateway) vs `@RequestParam tenantId`. O migrar a extraer tenantId directo del `Authentication` (2026-07)
- [ ] [🔴] **`@PreAuthorize` en endpoints sensibles** — agregar `@EnableMethodSecurity` + `@PreAuthorize` en controllers (crear/actualizar/eliminar según rol) (2026-07)
- [ ] [Alta] Reportes — dashboard consolidado KPIs + alertas (2026-07)
- [ ] [Alta] CRUD configuración tenant (edición) (2026-07)
- [ ] [Media] Integration tests ejecutables en CI (2026-07)
- [ ] [Baja] Refactor Producto → InsumoTemplate (post-MVP)
- [ ] [Baja] Spring Security local JWT (post-MVP)

### Frontend — Completado

- [x] [🔴] **tenantId sin encode en URL** — `prestamo.service.ts`, `producto.service.ts`, `factura.service.ts` usaban `?tenantId=${tenantId}` en vez de `params` object de axios. Migrados a `{ params: { tenantId } }`. (2026-07-21)
- [x] [Alta] Factura descuento porcentaje — input `%` en vez de `$`, subtotal formula, save() convierte % a monto
- [x] [Alta] Factura precio unitario por conversión — auto-calcular `precioUnitario / conv`, badge conversión
- [x] [Alta] Quitar listas infinitas — FacturasPage: `search()` por categoría; ProductosPage: tabla paginada
- [x] Spin buttons eliminados — `type="text" inputmode="decimal"` en cantidad/precio/descuento
- [x] Docker healthcheck fix — `localhost` → `127.0.0.1` (IPv6 Alpine)
- [x] Conversion UX — helper text + preview dinámico en ProductosPage

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

### Frontend — Pendiente (Analytics Suite Redesign)

- [ ] [Alta] **Rediseñar suite de análisis** — Implementar spec completo en `.ulpi/design/analytics-suite.md`. Incluye:
  - `AnalyticsHeader` compartido (título narrativo + período global + recalcular)
  - `KpiCard` refinado con sparkline inline opcional
  - `CategoryBreakdownChart` (barras comparativas período anterior)
  - `DataTable` wrapper de QTable con tokens de DESIGN.md
  - Reestructurar DashboardPage, AnalisisGastosPage, AccountingPage
  - Estados loading/empty/error/stale en cada página
  - Referencia: `.ulpi/design/analytics-suite.md` (spec completo)

### Frontend — Pendiente (Dashboard financiero)

- [ ] [Alta] **Dashboard UI polish** — El dashboard base funciona pero tiene espacio de mejora: animaciones de entrada más pulidas, hover states en stat strip, empty states más expresivos, responsive tuning.
- [ ] [Media] **Dashboard: sparklines** — Agregar mini-gráficos de tendencia en el stat strip (Geist Mono number + sparkline inline por métrica). Véase spec en `.ulpi/design/analytics-suite.md` (componente KpiCard con sparkline).

### Frontend — Pendiente (Critical)

- [x] [🔴] **Refresh token rotation** — interceptor captura 401, encola requests fallidas, renueva con refresh token, replay. Si falla refresh → clearSession(). Implementado en `boot/axios.ts`. (2026-07-25)
- [x] [🔴] **Cobertura de tests** — 29 tests (errors 10 + store 6 + composables 3 + errores extendidos 10). Tirados con vitest en node env. (2026-07-25)

### Frontend — Pendiente (Tutorial onboarding)

- [ ] [Alta] **Tour guiado con Driver.js** — guía de bienvenida al dashboard post-onboarding (4-5 pasos: sidebar, período, métricas, quick actions, perfil). Disparo único vía localStorage. Botón "Ayuda" en header para reiniciar. (2026-07)
- [ ] [Media] **Empty states contextuales** — cada página vacía debe tener un mensaje + CTA que guíe al usuario (ej. "Agrega tu primer producto"). (2026-07)

### Frontend — Pendiente (Tenant/User display)

- [ ] [Media] **Mostrar tenantName y userName en Dashboard/Configuración** — Opción A (recomendada): agregar `tenantName` a `UserEntityResponse` (1 campo backend) + mostrar en MainLayout, DashboardPage, ConfiguracionPage. Opción B: solo frontend guardando `activeTenant.name` en localStorage. (2026-07)

### Frontend — Pendiente (PWA)

- [ ] [Baja] **PWA: pull to refresh** — En mobile, gesto nativo para refrescar datos.
- [ ] [Baja] **PWA: custom install prompt** — Banner "Instalar PYMEQ" con dismiss persistente.
- [ ] [Baja] **PWA: transiciones direccionales** — Slide left/right según dirección de navegación.

### Core — Migraciones Flyway

- [ ] [Alta] **Consolidar V1–V16 → V1__core_schema.sql único** — 16 archivos → 1. 27 índices → 25 (eliminar `idx_products_tenant` y `idx_invoice_items_invoice` redundantes). Agregar `IF NOT EXISTS` faltantes en V9/V10. Idempotente para stage deploy. (2026-07) → [`docs/strategies/CORE_MIGRATIONS_STRATEGY.md`](./docs/strategies/CORE_MIGRATIONS_STRATEGY.md)

### Gateway

- [x] [Alta] CORS bug fix (2026-07) — Resuelto: globalcors + DedupeResponseHeader dual layer
- [ ] [Media] Integration tests WebTestClient + Testcontainers (2026-07)

### Auth

- [x] [🔴] **Unificar whitelists de rutas públicas** — `SecurityConfig.WHITE_LIST` y `JwtAuthenticationFilter.publicPaths` separadas. Solución: `shouldNotFilter()` ahora lee de `SecurityConfig.WHITE_LIST` vía `AntPathMatcher`. Se eliminó `publicPaths`. (2026-07-21)
- [ ] [Baja] Facebook OAuth2 — postergado (Meta no aprobó verificación) (post-MVP)

### Auth — Invitación por Email (estrategia definida)

- [ ] [Alta] **MVP invitaciones: maxUsers 2 + register+accept + TeamsPage** — `Tenant.java` 1→2, `InvitationRegisterRequest`, `InvitationInfoResponse`, `GET /{token}/info` + `POST /{token}/register` públicos, `InvitationServiceImpl.registerAndAccept()` transaccional, `TeamsPage.vue`, nav por roles. Sin cooldown. → [`docs/strategies/EMAIL_INVITATION_STRATEGY.md`](./docs/strategies/EMAIL_INVITATION_STRATEGY.md)
- [ ] [Media] **Role change cooldown (post-MVP)** — V3 migration `last_role_change_at`, `MemberServiceImpl` cooldown check (30d FREE). → ref: EMAIL_INVITATION_STRATEGY.md
- [ ] [Baja] **Rediseño templates email** — Swiss style branding PymeQ en invitation/verification/password-reset. → ref: EMAIL_INVITATION_STRATEGY.md
