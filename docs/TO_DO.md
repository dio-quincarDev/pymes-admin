## TO_DO.md

### Core

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

- [x] **Bottom nav mobile** — `MainLayout.vue`: `q-footer` con `q-tabs` 5 items + `q-route-tab` solo visible `<600px`. Glass effect. Rutas: Home, Productos, Facturas, Gastos, Más.
- [x] **EmptyState en 6 páginas** — `ProductosPage`, `ProveedoresPage`, `GastosPage`, `VentasPage`, `PrestamosPage`, `FacturasPage`. Cada uno con icono, título, descripción y CTA contextual.
- [x] **Unsaved changes guard** — `beforeRouteLeave` en `ProductosPage` y `FacturasPage`. `hasUnsavedChanges` computed. Dialog de confirmación si hay datos sin guardar.
- [x] **Keyboard shortcuts** — `useKeyboardShortcuts` composable. `N` crear, `?` ayuda, `Esc` cerrar. `Ctrl+K` global search placeholder. Help dialog con shortcuts listados.
- [x] **Error message clarity** — `loadSetup()` error: "No se pudo cargar la configuración del negocio" + "Verificar conexión con el servidor". `loadDependencies()` errors en 4 páginas: mensajes específicos por contexto.
- [x] **KpiCard DRY** — `AccountingPage.vue`: reemplazado `summaryCards` array manual por `KpiCard` importado. Eliminado template duplicado de 120+ líneas.
- [x] **Dialog animation** — `transition-show="slide-up"` + `transition-hide="slide-down"` en todos los dialogs CRUD (6 páginas). Efecto slide-up más nativo/app-like.
- [x] **SkeletonLoader skip** — Decisión documentada: tablas con datos reales no necesitan skeleton. Se eliminaron del roadmap.
- [x] **Stagger animation skip** — Ya existía `.stagger-children` en `app.scss` aplicado a KpiCard y AccountingPage. No duplicar.

### Frontend — Pendiente (Rediseño Copper Luxe — plan en FUTURE.md)

- [ ] [Alta] **ProductosPage: card grid visual** — Reemplazar q-table por grid de tarjetas tipo boutique. Accent bars por categoría, stagger animation, stats bar, category chips. Archivos nuevos: `ProductCardGrid.vue`, `ProductCard.vue`.
- [ ] [Media] **FacturasPage: timeline financiero** — Reemplazar q-table por feed cronológico agrupado por mes. Sticky headers, status badges prominentes, search + filter en toolbar.
- [ ] [Media] **GastosPage: cards por categoría** — Reemplazar q-table por tarjetas agrupadas por categoría con subtotales. Accent bars + glassmorphism.
- [ ] [Media] **VentasPage: calendario** — Reemplazar q-table por timeline calendario + cards de resumen semanal.
- [ ] [Media] **PrestamosPage: progress cards** — Cards con progress bar de saldo + timeline de pagos.
- [ ] [Media] **ProveedoresPage: contact cards** — Cards estilo agenda con avatar inicial + datos de contacto.
- [ ] [Baja] **PWA: pull to refresh** — En mobile, gesto nativo para refrescar datos.
- [ ] [Baja] **PWA: custom install prompt** — Banner "Instalar PYMEQ" con dismiss persistente.
- [ ] [Baja] **PWA: transiciones direccionales** — Slide left/right según dirección de navegación.
- [ ] [Baja] **PWA: splash screen brandeado** — Custom splash con logo + copper gradient.

### Gateway

- [ ] [Alta] CORS bug fix (2026-07)
- [ ] [Media] Integration tests WebTestClient + Testcontainers (2026-07)

### Auth

- [ ] [Baja] Facebook OAuth2 — postergado (Meta no aprobó verificación) (post-MVP)
