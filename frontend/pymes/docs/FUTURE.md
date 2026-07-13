# FUTURE.md — Frontend: Modulos Core

> **Fecha:** 2026-07-12
> **Objetivo:** Documentar el estado de los modulos backend y frontend.

---

## Estado Actual

Backend y frontend de los 5 modulos core estan completos.

| Modulo backend | Ruta API | Pagina frontend | Service | Sidebar |
|----------------|----------|-----------------|---------|---------|
| Gastos | `/api/v1/core/gastos` | ✅ `GastosPage.vue` | ✅ `gasto.service.ts` | ✅ habilitado |
| Ventas | `/api/v1/core/ventas` | ✅ `VentasPage.vue` | ✅ `venta.service.ts` | ✅ habilitado |
| Prestamos | `/api/v1/core/prestamos` | ✅ `PrestamosPage.vue` | ✅ `prestamo.service.ts` | ✅ habilitado |
| Patrimonio | `/api/v1/core/patrimonio/{tenantId}` | ✅ `PatrimonioPage.vue` | ✅ `patrimonio.service.ts` | ✅ habilitado |
| Accounting | `/api/v1/core/accounting` | ✅ `AccountingPage.vue` | ✅ `accounting.service.ts` | ✅ habilitado |

---

## Modulos implementados

### Gastos Operativos

- **Pagina:** `GastosPage.vue` — CRUD tabla QTable + dialog crear/editar + dialog confirmar eliminar
- **Service:** `gasto.service.ts` — getAll, getById, create, update, remove
- **Tipos:** `GastoOperativo`, `GastoRequest` en `types/index.ts`
- **Ruta:** `/dashboard/gastos`
- **Sidebar:** `{ title: 'Gastos', icon: 'money_off', path: '/dashboard/gastos' }`
- **Features:** filtros por categoría, selector de método de pago, validación QForm, glass morphism en dialogs, responsive `width: 90vw; max-width: 480px`

### Ventas Diarias

- **Pagina:** `VentasPage.vue` — CRUD tabla QTable + dialog crear/editar + dialog confirmar eliminar
- **Service:** `venta.service.ts` — getAll, getById, create, update, remove
- **Tipos:** `VentaDiaria`, `VentaRequest` en `types/index.ts`
- **Ruta:** `/dashboard/ventas`
- **Sidebar:** `{ title: 'Ventas', icon: 'point_of_sale', path: '/dashboard/ventas' }`
- **Features:** fecha, monto bruto, descripción, validación QForm, glass morphism en dialogs

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
- **Features:** 3 KPI cards con accent borders (capital, fecha, estado), toggle editar/guardar, empty state con glass, stagger animation

### Accounting (Métricas Financieras)

- **Pagina:** `AccountingPage.vue` — 6 metric cards estilo KpiCard + resumen de gastos
- **Service:** `accounting.service.ts` — consultar, recalcular
- **Tipos:** `MetricasFinancieras` en `types/index.ts`
- **Ruta:** `/dashboard/accounting`
- **Sidebar:** `{ title: 'Contabilidad', icon: 'balance', path: '/dashboard/accounting' }`
- **Features:** selector de período YYYY-MM, botón recalcular, 6 cards con accent borders (ingresos, costos, gastos, margen bruto/operativo/neto), resumen de gastos en glass card, stagger animation

---

## Otros archivos nuevos

- **`src/utils/format.ts`** — `formatCurrency()` + `formatPct()` como singletones `Intl.NumberFormat`. Usado por todas las páginas CRUD y AccountingPage.

---

## Cambios en archivos existentes

| Archivo | Cambios |
|---------|---------|
| `types/index.ts` | +GastoOperativo, +GastoRequest, +VentaDiaria, +VentaRequest, +Prestamo, +PagoPrestamo, +PrestamoRequest, +PagoPrestamoRequest, +Patrimonio, +PatrimonioRequest, +MetricasFinancieras, +PageResponse\<T\>, +ProductOption.lastUnitPrice |
| `producto.service.ts` | +search() paginado |
| `FacturasPage.vue` | +auto-fill precioUnitario via onProductoChange(), import order fix, shared formatCurrency, filteredByProvider fix (quitar !p.proveedorId del filtro) |
| `ProductosPage.vue` | -minQuantity/maxQuantity del form (removido del template + openCreate + openEdit), shared formatCurrency, import order fix |
| `router/routes.ts` | +5 rutas: gastos, ventas, prestamos, patrimonio, accounting |
| `MainLayout.vue` | sidebar: 5 items habilitados (antes `disabled: true`) |

---

## Pendiente conocido

- **Descuento porcentaje:** `InvoiceItemCard.vue` — input con `suffix="%"` en vez de `prefix="$"`. Subtotal usa `qty*price*(1-disc/100)`. Backend recibe monto calculado sin cambios.
- **Precio unitario por conversión:** Al seleccionar presentación con `conversion>1`, `precioUnitario = lastUnitPrice/conversion`. Badge de conversión. Cantidad siempre en unidades base. Escala a gramos/kilos/unidades/cajas.
- **Listas infinitas:** Quitar `productoService.getAll()` de `FacturasPage.vue`. Usar `search()` paginado por categoría en `loadDependencies()` + watch sobre `activeCategory`. `ProductosPage.vue`: tabla con `search()` paginado en vez de `getAll()`.
- ConfiguracionPage: CRUD edición (pendiente backend PUT endpoint)
- Tests frontend
- SEO: og:image, meta description, JSON-LD
