# FUTURE.md — Frontend: Modulos Core

> **Fecha:** 2026-07-14
> **Objetivo:** Documentar el estado de los modulos backend y frontend + plan de rediseño visual.

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
| `MainLayout.vue` | +q-footer mobile nav (glass effect), sidebar items habilitados |
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

## Plan: Rediseño Visual "Copper Luxe / Dark Jungle"

> **Fecha:** 2026-07-14
> **Estado:** Propuesta — pendiente de aprobación

### Diagnóstico

El frontend actual tiene **identidad visual existente** pero subutilizada:
- Brand: Deep Forest & Copper (`$primary: #A3785E`, `$dark-page: #0B1210`)
- Glassmorphism, mesh gradients, brand glow, copper accents ya están en `app.scss`
- Outfit + Source Sans 3 tipografía implementada

**Problema:** Todas las páginas CRUD usan el mismo patrón — `q-table` dentro de `q-card`. Sin variedad visual, sin tesitura, sin identidad diferenciada. El resultado se ve genérico porque **es** genérico: mismo layout, mismos componentes, misma densidad de información en todas partes.

### Dirección estética: "Copper Luxe / Dark Jungle"

No más cambios tímidos. La identidad existe, hay que llevarla al extremo:

| Elemento | Actual | Propuesto |
|----------|--------|-----------|
| Fondos | Sólido oscuro oscuro | Mesh gradients sutiles + noise texture |
| Cards | Borde sutil | Glassmorphism con tintes cobrizos + glow en hover |
| Tipografía | Outfit solo en títulos | Outfit bold + letter-spacing agresivo en headings |
| Acentos de categoría | Solo texto | Color-coding: cada categoría = un accent distinto (copper, sage, gold, teal, rose) |
| Animaciones | Fade básico | Stagger grid, spring physics, press scale |
| Tablas | Q-table estándar | Layouts específicos por tipo de dato |

### Phase A: ProductosPage — "Boutique Display"

**Nada de tabla.** Una **grid visual de tarjetas** tipo showroom/catálogo:

```
[🔍 Buscar...]  [Chips de categoría: Todos │ 🟤 Abarrotes │ 🟢 Bebidas │ 🟡 Limpieza]
[📊 12 productos · 3 categorías · 2 proveedores]

┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐
│ ▓▓▓▓ (accent bar)  │ │ ▓▓▓▓ (accent bar)  │ │ ▓▓▓▓ (accent bar)  │
│                    │ │                    │ │                    │
│ Arroz Superior     │ │ Frijol Negro       │ │ Aceite Vegetal     │
│ `ARZ-001`          │ │ `FRJ-002`          │ │ `ACE-003`          │
│                    │ │                    │ │                    │
│ 🏪 Proveedor A     │ │ 🏪 Proveedor B     │ │ 🏪 Proveedor A     │
│ 📦 3 presentaciones│ │ 📦 1 presentación  │ │ 📦 —               │
│ $12.50             │ │ $8.00              │ │ $45.00             │
│                    │ │                    │ │                    │
│ [✏️] [📋] [🗑️]     │ │ [✏️] [📋] [🗑️]     │ │ [✏️] [📋] [🗑️]     │
└────────────────────┘ └────────────────────┘ └────────────────────┘
```

- **Desktop**: 3 columnas | **Tablet**: 2 | **Mobile**: 1
- **Entrance**: Stagger fade+slide (50ms delay/card)
- **Hover**: Lift + glow + sombra profunda
- **Accent bar**: Color por categoría (mapeado del brand: copper, sage, gold, teal, rose)
- **SKU pill**: Monospace small, estilo badge cobrizo
- **Stats bar**: Resumen rápido arriba
- **Category chips**: `q-chip` con `selected` color + transition suave

**Archivos a crear:** 2
- `ProductCardGrid.vue` — Grid container con stagger + filtros
- `ProductCard.vue` — Tarjeta individual con accent bar

### Phase B: FacturasPage — Timeline financiero

No tabla, un **feed cronológico** agrupado por mes:

```
Diciembre 2024
┌─────────────────────────────────────────┐
│ 📄 FAC-001 │ Proveedor A │ $1,200.00   │
│ 🟡 Pendiente │ 2024-12-15               │
│ [👁️] [💳] [🗑️]                          │
├─────────────────────────────────────────┤
│ 📄 FAC-002 │ Proveedor B │ $3,450.00   │
│ 🟢 Pagada   │ 2024-12-10               │
│ [👁️] [🗑️]                              │
└─────────────────────────────────────────┘

Enero 2025
...
```

- Meses como sticky headers con gradient
- Cards más compactas pero con más tesitura visual
- Status badges más prominentes
- Search + filter en toolbar

### Phase C: Otras páginas — Layouts específicos

No todas las páginas necesitan el mismo layout. Cada tipo de dato tiene su presentación natural:

| Página | Layout propuesto |
|--------|------------------|
| **Productos** | Card grid visual tipo boutique (Phase A) |
| **Facturas** | Timeline cronológico agrupado por mes (Phase B) |
| **Gastos** | Tarjetas agrupadas por categoría con subtotales |
| **Ventas** | Timeline calendario + cards de resumen semanal |
| **Préstamos** | Cards con progress bar de saldo + timeline de pagos |
| **Proveedores** | Contact cards estilo agenda con avatar inicial |
| **Dashboard** | (Ya está bien con KPI cards — ajustar glassmorphism) |

### Phase D: PWA Feel — polish nativo

| Mejora | Detalle |
|--------|---------|
| Instalación | Banner custom "Instalar PYMEQ" con dismiss persistente |
| Pull to refresh | En mobile, refrescar datos con gesto nativo |
| Status bar | `theme-color` + meta tags para Android/iOS |
| Offline | Página offline con diseño brandeado (no genérico) |
| Splash | Custom splash screen con logo + copper gradient |
| Transiciones | Direccionales (slide left/right según dirección de nav) |
| Haptic feedback | Visual press feedback en cards y botones |

### Scope y dependencias

- **No depende de backend** — todo es presentación sobre datos existentes
- **No agrega dependencias** — todo con CSS + Quasar nativo + existing brand tokens
- **Archivos nuevos:** ~4 (ProductCardGrid, ProductCard, y posiblemente 2 más para otros layouts)
- **Archivos a modificar:** ~8-10 (cada página CRUD + MainLayout + app.scss para nuevos estilos)
- **Riesgo:** Bajo — cambios puramente visuales, no afecta lógica de negocio
