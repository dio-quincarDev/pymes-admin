# FUTURE.md — Frontend: Modulos Core

> **Fecha:** 2026-09-18 (actualizado)
> **Objetivo:** Documentar el estado de los modulos backend y frontend + design system actual.

---

## Estado Actual

Backend y frontend de los 5 modulos core estan completos + modernización PWA aplicada.

| Modulo backend | Ruta API | Pagina frontend | Service | Sidebar | Estado |
|----------------|----------|-----------------|---------|---------|---------|
| Gastos | `/api/v1/core/gastos` | ⚠️ `GastosPage.vue` deprecada 2026-08-04 → redirect `/dashboard/costos?tab=gastosFijos` | ✅ `gasto.service.ts` | ❌ fuera bottom nav | Deprecated |
| Ventas | `/api/v1/core/ventas` | ✅ `VentasPage.vue` | ✅ `venta.service.ts` | ✅ habilitado | Activo |
| Prestamos | `/api/v1/core/prestamos` | ✅ `PrestamosPage.vue` | ✅ `prestamo.service.ts` | ✅ habilitado | Activo |
| Patrimonio | `/api/v1/core/patrimonio/{tenantId}` | ✅ `PatrimonioPage.vue` `data-tour=patrimonio` | ✅ `patrimonio.service.ts` | ✅ habilitado | Activo |
| Costos | `/api/v1/core/costos/diario` | ✅ `CostosPage.vue` 220l composition + `useCostos` + `CostSummaryBar` `data-tour=costos` | ✅ `costo.service.ts` | ✅ `mobile-bottom-nav` | Activo 2026-09-12 |
| Facturas | `/api/v1/core/facturas` | ✅ `FacturasPage.vue` `InvoiceItemCard` ITBMS 0/7/10 `data-tour=facturas` | ✅ `factura.service.ts` | ✅ | Activo V6 |
| Analytics | `/api/v1/core/analytics` | ✅ `DashboardPage.vue` + `AnalisisGastosPage.vue` `data-tour=dashboard/analisis` + 10 motores | ✅ `analytics.service.ts` | ✅ Análisis/Dashboard | 924KB |
| Tutorial | — | ✅ `useTutorial.ts` `driver.js` 7 pasos `data-tour` + `mini hint` | — | `?` header | 2026-09-18 |
| Legales BETA | — | ✅ `pages/legal/TerminosPage.vue` `PrivacidadPage.vue` `BETA v0.1` | — | Footer | 2026-09-18 |
| Monetización score 12m | `POST /core/analytics/share` (futuro) | `ConsentShareCard` opt-in red aliada | pendiente | FUTURO — 12m opt-in red aliada |

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

## Pendiente conocido (2026-09-18)

- ConfiguracionPage: CRUD edición (pendiente backend PUT `/setup/{tenantId}`) + Picker 40+ categorías
- Fase 7 Amortización francesa (8 tasks Alta, ver TO_DO.md)
- Tests frontend 29 vitest + E2E `377/92` stale → `536 (207/263/37+29)` actualizado
- Offline PWA: 1/2 hecho (refresh no logout + banner lastSync), pendiente cola + borrador (defer hasta feedback real)
- Legales BETA hecho (`dio-quincar@outlook.com` temporal), SEO og:image pendiente
- **Monetización score 12m — red de financieras aliadas** (post-MVP, 12m comportamiento): individual identificado solo con consentimiento expreso separado “compartir mi score 68/100 con cualquier financiera aliada para evaluación crediticia”, a los 12 meses, revocable en Configuración, log consentId. Sin “sí” no se vende. Agregado anónimo: sin nombre/email/RUC, ≥10 PYMEs por celda → no es dato personal (Ley 81), vendible desde mes 1. Base legal Panamá: Ley 81 + Decreto 285, ANTAI. Para implementar: V7 `analytics_consent/share_log` + 3 endpoints + rewrite `PrivacidadPage.vue:32` + contrato red. Scope mínimo: `score 0-100 + drivers` (no facturas crudas).

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
