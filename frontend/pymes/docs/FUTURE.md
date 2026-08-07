# FUTURE.md — Frontend: Modulos Core

> **Fecha:** 2026-07-15
> **Objetivo:** Documentar el estado de los modulos backend y frontend + design system actual.

---

## Estado Actual

Backend y frontend de los 5 modulos core estan completos + modernización PWA aplicada.

| Modulo backend | Ruta API | Pagina frontend | Service | Sidebar |
|----------------|----------|-----------------|---------|---------|
| Gastos | `/api/v1/core/gastos` | ✅ `GastosPage.vue` | ✅ `gasto.service.ts` | ✅ habilitado |
| Ventas | `/api/v1/core/ventas` | ✅ `VentasPage.vue` | ✅ `venta.service.ts` | ✅ habilitado |
| Prestamos | `/api/v1/core/prestamos` | ✅ `PrestamosPage.vue` | ✅ `prestamo.service.ts` | ✅ habilitado |
| Patrimonio | `/api/v1/core/patrimonio/{tenantId}` | ✅ `PatrimonioPage.vue` | ✅ `patrimonio.service.ts` | ✅ habilitado |
| Accounting | `/api/v1/core/accounting` | ✅ `AccountingPage.vue` | ✅ `accounting.service.ts` | ✅ habilitado |

### Modernización PWA aplicada (2026-07-14)

| Feature | Estado |
|---------|--------|
| Bottom nav mobile | ✅ `q-footer` con `q-tabs` visible `<600px` |
| EmptyState reutilizable | ✅ 6 páginas CRUD |
| Unsaved changes guard | ✅ ProductosPage + FacturasPage |
| Keyboard shortcuts | ✅ `useKeyboardShortcuts` composable |
| Error message clarity | ✅ loadSetup + loadDependencies en 4 páginas |
| KpiCard DRY | ✅ AccountingPage usa KpiCard importado |
| Dialog animation | ✅ slide-up/down en 6 páginas |

---

## Modulos implementados

### Gastos Operativos

- **Pagina:** `GastosPage.vue` — CRUD tabla QTable + dialog crear/editar + dialog confirmar eliminar
- **Service:** `gasto.service.ts` — getAll, getById, create, update, remove
- **Tipos:** `GastoOperativo`, `GastoRequest` en `types/index.ts`
- **Ruta:** `/dashboard/gastos`
- **Sidebar:** `{ title: 'Gastos', icon: 'money_off', path: '/dashboard/gastos' }`
- **Features:** filtros por categoría, selector de método de pago, validación QForm, responsive `width: 90vw; max-width: 480px`

### Ventas Diarias

- **Pagina:** `VentasPage.vue` — CRUD tabla QTable + dialog crear/editar + dialog confirmar eliminar
- **Service:** `venta.service.ts` — getAll, getById, create, update, remove
- **Tipos:** `VentaDiaria`, `VentaRequest` en `types/index.ts`
- **Ruta:** `/dashboard/ventas`
- **Sidebar:** `{ title: 'Ventas', icon: 'point_of_sale', path: '/dashboard/ventas' }`
- **Features:** fecha, monto bruto, descripción, validación QForm, responsive

### Préstamos + Pagos

- **Pagina:** `PrestamosPage.vue` — CRUD tabla QTable + dialog crear/editar + dialog pagos inline
- **Service:** `prestamo.service.ts` — getAll, getById, create, update, remove, getPagos, createPago
- **Tipos:** `Prestamo`, `PagoPrestamo`, `PrestamoRequest`, `PagoPrestamoRequest` en `types/index.ts`
- **Ruta:** `/dashboard/prestamos`
- **Sidebar:** `{ title: 'Préstamos', icon: 'account_balance', path: '/dashboard/prestamos' }`
- **Features:** tabla de pagos con historial, formulario de pago inline, badge de estado (ACTIVO/PAGADO), validación QForm

### Patrimonio

- **Pagina:** `PatrimonioPage.vue` — KPI cards estilo KpiCard + card de configuración editable
- **Service:** `patrimonio.service.ts` — get (get-or-create), update
- **Tipos:** `Patrimonio`, `PatrimonioRequest` en `types/index.ts`
- **Ruta:** `/dashboard/patrimonio`
- **Sidebar:** `{ title: 'Patrimonio', icon: 'savings', path: '/dashboard/patrimonio' }`
- **Features:** 3 KPI cards con accent borders (capital, fecha, estado), toggle editar/guardar, empty state, stagger animation

### Accounting (Métricas Financieras)

- **Pagina:** `AccountingPage.vue` — 6 metric cards estilo KpiCard + resumen de gastos
- **Service:** `accounting.service.ts` — consultar, recalcular
- **Tipos:** `MetricasFinancieras` en `types/index.ts`
- **Ruta:** `/dashboard/accounting`
- **Sidebar:** `{ title: 'Contabilidad', icon: 'balance', path: '/dashboard/accounting' }`
- **Features:** selector de período YYYY-MM, botón recalcular, 6 cards con accent borders (ingresos, costos, gastos, margen bruto/operativo/neto), resumen de gastos, stagger animation

---

## Otros archivos nuevos

- **`src/utils/format.ts`** — `formatCurrency()` + `formatPct()` como singletones `Intl.NumberFormat`. Usado por todas las páginas CRUD y AccountingPage.
- **`src/composables/useKeyboardShortcuts.ts`** — Composable reutilizable para atajos de teclado. `N` crear, `?` ayuda, `Esc` cerrar, `Ctrl+K` search placeholder.
- **`src/components/ui/EmptyState.vue`** — Componente reutilizable con props `icon`, `title`, `description`, `actionLabel`, `actionTo`. Usado en 6 páginas CRUD.

---

## Cambios en archivos existentes

| Archivo | Cambios |
|---------|---------|
| `types/index.ts` | +GastoOperativo, +GastoRequest, +VentaDiaria, +VentaRequest, +Prestamo, +PagoPrestamo, +PrestamoRequest, +PagoPrestamoRequest, +Patrimonio, +PatrimonioRequest, +MetricasFinancieras, +PageResponse\<T\>, +ProductOption.lastUnitPrice |
| `producto.service.ts` | +search() paginado |
| `FacturasPage.vue` | +auto-fill precioUnitario, responsive cols, compact dialog, emptyState, unsavedGuard, shortcuts, unitNameMap, dialog anim |
| `ProductosPage.vue` | -minQuantity/maxQuantity, +categoryNameMap, +unitNameMap, +emptyState, +unsavedGuard, +shortcuts, dialog anim |
| `ProveedoresPage.vue` | +emptyState, +shortcuts, dialog anim |
| `GastosPage.vue` | +emptyState, +shortcuts, dialog anim |
| `VentasPage.vue` | +emptyState, +shortcuts, dialog anim |
| `PrestamosPage.vue` | +emptyState, +shortcuts, dialog anim |
| `AccountingPage.vue` | -summaryCards template manual, +KpiCard import |
| `CategoryTabs.vue` | Reescrito con q-chip nativo |
| `MainLayout.vue` | +q-footer mobile nav, sidebar items habilitados |
| `router/routes.ts` | +5 rutas: gastos, ventas, prestamos, patrimonio, accounting |

---

## Pendiente conocido

- ConfiguracionPage: CRUD edición (pendiente backend PUT endpoint)
- Tests frontend
- SEO: og:image, meta description, JSON-LD

### Completado (2026-07-14)

- **Descuento porcentaje:** `InvoiceItemCard.vue` — input con `suffix="%"` en vez de `prefix="$"`. Subtotal usa `qty*price*(1-disc/100)`.
- **Precio unitario por conversión:** Al seleccionar presentación con `conversion>1`, `precioUnitario = lastUnitPrice/conversion`. Badge de conversión.
- **Listas infinitas:** `ProductosPage` usa `search()` paginado. `FacturasPage` carga dependencias con búsqueda paginada por categoría.
- **Bottom nav mobile:** `q-footer` con `q-tabs` 5 items visible `<600px`.
- **EmptyState:** 6 páginas con componente reutilizable.
- **Unsaved changes guard:** `beforeRouteLeave` en ProductosPage y FacturasPage.
- **Keyboard shortcuts:** `useKeyboardShortcuts` composable + help dialog.
- **Dialog animation:** slide-up/down en todos los dialogs CRUD.

---

## Plan: Rediseño Visual → Completado (Swiss/Grid)

> **Fecha:** 2026-07-15
> **Estado:** Completado

### Design system locked

- `DESIGN.md`: paleta, tipografía, escalas, component vocabulary, accessibility baseline
- `main-layout.md`: header minimal + sidebar Swiss grouped + page workspace
- `landing-page.md`: split hero + bento grid + stat strip
- `facturas.md`: flows, estados, component specs

### Paleta

| Token | Hex | Uso |
|-------|-----|-----|
| `--pq-background` | `#08090D` | Page base |
| `--pq-surface` | `#12141A` | Cards, sidebar, dialogs |
| `--pq-elevated` | `#1E2129` | Dropdowns, modals |
| `--pq-border` | `#353945` | Dividers, input borders |
| `--pq-text` | `#F5F3EF` | Primary copy |
| `--pq-text-muted` | `#9B9790` | Labels, placeholders |
| `--pq-accent` | `#C8963E` | Primary buttons, focus, key numbers |
| `--pq-success` | `#3D7A5A` | Paid, confirmed |
| `--pq-warning` | `#C8A042` | Pending, draft |
| `--pq-danger` | `#A04038` | Delete, destructive |

### Tipografía

- Display/heading: **Geist** (400-800)
- Body: **Satoshi** (400-700)
- Utility/numbers: **Geist Mono** (400-500), `tabular-nums`

### Tokens legacy eliminados

`brand-glow`, `mesh-text-gradient`, `glass-light`, `bg-forest-deep`, `bg-surface-pine`, `border-light` — ya no existen en `app.scss`. Clases referenciadas en otros archivos son deuda técnica.
