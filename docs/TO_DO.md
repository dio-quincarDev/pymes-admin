## TO_DO.md

### Core

- [x] [Alta] **Exception system (estrategia definida)** — `ErrorResponse`, `ApiResponse`, `CodigoError`, 3 custom exceptions, `GlobalExceptionHandler` (12 handlers). Migrados 18 throws en 7 services (150/150 tests). → [`docs/EXCEPTION_STRATEGY.md`](./backend/core/docs/EXCEPTION_STRATEGY.md)
- [ ] [🔴] **Validar tenantId contra JWT** — interceptor/filtro que compare `X-Tenant-Id` header (del gateway) vs `@RequestParam tenantId`. O migrar a extraer tenantId directo del `Authentication` (2026-07)
- [ ] [🔴] **`@PreAuthorize` en endpoints sensibles** — agregar `@EnableMethodSecurity` + `@PreAuthorize` en controllers (crear/actualizar/eliminar según rol) (2026-07)
- [ ] [Alta] Reportes — dashboard consolidado KPIs + alertas (2026-07)
- [ ] [Alta] CRUD configuración tenant (edición) (2026-07)
- [ ] [Media] Integration tests ejecutables en CI (2026-07)
- [ ] [Baja] Refactor Producto → InsumoTemplate (post-MVP)
- [ ] [Baja] Spring Security local JWT (post-MVP)

### Frontend — Completado

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

### Frontend — Pendiente (Dashboard financiero)

- [ ] [Alta] **Dashboard UI polish** — El dashboard base funciona pero tiene espacio de mejora: animaciones de entrada más pulidas, hover states en stat strip, empty states más expresivos, responsive tuning, posible sparkline en métricas de tendencia.
- [ ] [Media] **Dashboard: sparklines** — Agregar mini-gráficos de tendencia en el stat strip (Geist Mono number + sparkline inline por métrica).
- [ ] [Media] **Dashboard: expense doughnut** — Opción de vista doughnut chart para desglose de gastos (reutilizar `BaseChart` existente).

### Frontend — Pendiente (Critical)

- [ ] [🔴] **Refresh token rotation** — el interceptor debe capturar 401, intentar renovar con refresh token, y solo si falla, borrar sesión (2026-07)
- [ ] [🔴] **Cobertura de tests** — 1 test para 106 archivos. Prioridad: services, stores, composables (2026-07)

### Frontend — Pendiente (PWA)

- [ ] [Baja] **PWA: pull to refresh** — En mobile, gesto nativo para refrescar datos.
- [ ] [Baja] **PWA: custom install prompt** — Banner "Instalar PYMEQ" con dismiss persistente.
- [ ] [Baja] **PWA: transiciones direccionales** — Slide left/right según dirección de navegación.

### Core

- [ ] [Media] **Dashboard聚合 endpoint** — Endpoint que una `MetricasFinancieras` + gastos + ventas + facturas en una sola llamada (actualmente el frontend hace 4 requests paralelos). Optimización para latency.

### Gateway

- [x] [Alta] CORS bug fix (2026-07) — Resuelto: globalcors + DedupeResponseHeader dual layer
- [ ] [Media] Integration tests WebTestClient + Testcontainers (2026-07)

### Auth

- [ ] [🔴] **Unificar whitelists de rutas públicas** — `SecurityConfig.WHITE_LIST` y `JwtAuthenticationFilter.publicPaths` separadas. Crear fuente única o hacer que `shouldNotFilter` lea de `WHITE_LIST`. (2026-07)
- [ ] [Baja] Facebook OAuth2 — postergado (Meta no aprobó verificación) (post-MVP)
