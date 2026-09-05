# Pendientes Frontend — Estrategia de Cierre

Estado: **plan definido** (2026-08-02). Ejecución por fases contra la sección `### Frontend` del TO_DO.
Objetivo: cerrar los 7 pendientes frontend priorizando backend listo primero, riesgo ~0 antes que código nuevo, y deuda que ya tiene el código escrito sobre trabajo vago.

## Mapa de pendientes (estado real)

| # | Ítem | Esfuerzo | Dependencia backend | Archivos clave |
|---|------|----------|---------------------|----------------|
| 1 | Simplificar UI + panel `financialHealth` | Medio | ✅ listo (`financialHealth` en `/core/analytics`) | `AccountingPage.vue`, `DashboardPage.vue`, `useAnalytics.ts`, `types/analytics.ts` |
| 2 | Dashboard UI polish | Bajo (subjetivo) | — | `DashboardPage.vue` |
| 3 | Tour guiado Driver.js | Medio + nueva dep | — | `MainLayout.vue` |
| 4 | Helper "Pago de salario" | Medio-alto | ✅ listo (`GASTO_OPERATIVO` sin items + `total`) | `FacturasPage.vue`, `types/index.ts` |
| 5 | Gastos desde facturas pagadas | Bajo-medio | ✅ listo | `useFinancialDashboard.ts` |
| 6 | Deprecar GastosPage | Chico | — | `GastosPage.vue`, `MainLayout.vue` |
| 7 | PWA: pull to refresh | Chico | — | `composables/usePullToRefresh.ts`, `DashboardPage.vue` |

## Fase 1 — Quick wins (riesgo ~0, 1 sesión)

Ítems: **6** (deprecar GastosPage), **7** (pull to refresh), parte de **1** (fix ruta QuickActions).

1. **Deprecar GastosPage** — banner con enlace a CostosPage → tab Gastos Fijos. Sacar de la bottom nav mobile (`MainLayout.vue:144`, q-tab `money_off` → `/dashboard/gastos`). No tocar la ruta `/dashboard/gastos` (el banner sigue funcionando).
2. **Pull to refresh** — `composables/usePullToRefresh.ts` existe completo y no se importa en ninguna página (código muerto). Cablearlo en `DashboardPage.vue` + indicador visual (pullDistance / isRefreshing).
3. **Fix ruta QuickActions** — `QuickActions.vue` "Ver reportes" apunta a `/dashboard/accounting` (Contabilidad); debe ir a `/dashboard/analisis-gastos` (donde vive `useAnalytics`). Emit `ver-reportes` es código muerto en `DashboardPage.vue` — eliminar.

## Fase 2 — Simplificación UI + Salud financiera

Backend 100% listo. Oportunidad de eliminar redundancia: Dashboard y Contabilidad comparten 4 KPIs idénticos de la misma fuente. Estrategia: cada página se queda con su responsabilidad.

1. **Eliminar 4 KPIs duplicados de `AccountingPage.vue`** — quitar Ingresos, Costos, Margen Bruto, Gastos Operativos (mismos que Dashboard). Queda como vista de márgenes consolidados.
2. **Agregar Margen Operativo + Margen Neto a `DashboardPage.vue`** — `KpiCard` con `margenOperativoPct` y `margenNetoPct` (ya en `MetricasFinancieras`).
3. **Tipo `financialHealth`** en `types/analytics.ts` — shape verificado en `AnalyticsServiceImpl.analisisSaludFinanciera`: `overallHealth` (0-100), `criticalAlerts`, `investmentSignals`, `expansionReadiness`, `recommendations`.
4. **Exponerlo en `useAnalytics.ts`** — computed `financialHealth` (y derivados `criticalAlerts`/`recommendations`).
5. **Panel de salud financiera en `DashboardPage.vue`** — card con overallHealth, criticalAlerts, recommendations. Solo visible con datos.

## Fase 3 — Cierre del modelo de gastos (EXPENSES_MODEL_STRATEGY pasos 4/5/6)

Ítems: **5** (gastos desde facturas) y **4** (helper salario). Hacen que el frontend coincida con lo que cuenta el motor backend (capa 2: facturas `GASTO_OPERATIVO` PAGADAS).

1. **Gastos desde facturas pagadas (paso 6)** — `useFinancialDashboard.ts:106-107` llama `gastoService.getAll` 2 veces (período + prev). Las facturas ya se traen en el mismo `Promise.all` (`facturaService.getAll`). Cambiar `gastos`/`gastosPrev` por computeds derivados de `facturas` (solo `GASTO_OPERATIVO` PAGADAS, agrupadas por proveedor). Eliminar las llamadas a `gastoService` y el import.
   - **Ojo**: `CategoryBreakdownChart` agrupa por categoría; al derivar de facturas pasa a proveedor. Ajustar la semántica del chart o del título.
2. **Helper "Pago de salario" (paso 4)** — en `FacturasPage.vue`, cuando `tipo=GASTO_OPERATIVO`: select de colaborador (solo `DIARIO`, vía `costoService.getAllCollaboradores`) + rango de días → precarga `días × tarifa` en el total (editable) y descripción `"Salarios — {nombre}, {rango}"`. Requiere `FacturaRequest` + `total` opcional e `items` opcional en `types/index.ts` (backend ya lo acepta).

## Diferido (post-MVP)

Ítems: **3** (Tour Driver.js — nueva dependencia, menor valor relativo) y **2** (Dashboard UI polish — sin spec concreta). Se ejecutan si queda tiempo tras las fases 1-3 o cuando haya spec.

**ACTUALIZADO (2026-08-14):** Estos ítems se reemplazan por la **Fase 4 — Consolidación del Workflow** (ver TO_DO.md). El tutorial guiado y el dashboard polish se diseñan DESPUÉS de completar la Fase 4, ya que el workflow depurado define qué elementos quedan y cómo se presentan.

## Fase 4 — Consolidación del Workflow (2026-08-17)

Objetivo: depurar la info visual, quitar redundancias, dejarlo "casi para dummies". **Principio: una pantalla, una pregunta** — el dueño abre la app y responde en 5 segundos "¿estoy ganando? ¿qué tengo que pagar?". Todo lo demás es drill-down bajo demanda.

**Modelo de capas** (regla para todo lo que se agrega o mueve):
- **Vital** (siempre visible) → lo que se decide/acciona a diario.
- **Bajo demanda** (colapsado, 1 clic) → análisis útil pero no diario.
- **Fuera de UI** → se elimina del frontend; la API sigue exponiéndolo.

**Auditoría backend (2026-08-17):** el core computa 10 motores (9 CTE + financialHealth) pero la UI solo muestra 3 supplier + financialHealth. Los 6 restantes — **ABC (Pareto), tendencias precios (media 90d), impacto márgenes, gasto variable/opex, proyección 30/60/90 y el motor de alertas** — están calculados y tipados (`types/analytics.ts`) y expuestos en `useAnalytics`, pero nunca se renderizan: `AnalyticsDashboard.vue` + 6 hijos (`AbcGastosChart`, `PriceTrendSparkline`, `MarginImpactTable`, `OpexGauge`, `ProjectionTimeline`, `AlertsPanel`) son código huérfano. **Decisión: los 6 motores se conservan y todos quedan BAJO DEMANDA** (colapsados en AnalisisGastosPage) — ninguno se borra, ninguno es visible por defecto. Además AnalisisGastosPage usa alertas locales (inferiores) en vez del motor `alerts` del backend → reemplazar.

**Ejecución por secciones (cada una cierra con `npm run lint` + `npm run build`):**

### Sección 0 — Spec de consolidación
`docs/.ulpi/design/workflow-consolidation.md` (contrato, vincula a `.ulpi/design/DESIGN.md`): tabla de jargon (GASTO_OPERATIVO→"Gasto", REGISTRADA→"Pendiente", Colaboradores→"Equipo", Margen Operativo→"Ganancia bruta", costo operativo diario→"Costo del día"), specs de los componentes nuevos (ActivityPanel, KPI Ganancia del mes, secciones colapsables) y la decisión de navegación.

### Sección 1 — Dashboard (4a)
QuickActions fuera (borrar archivo) · 3 KPIs de margen → 1 "Ganancia del mes" = `formatCurrency(margenNeto)` con delta vs prev (KPI row 7→5: Ingresos, Costos, Gastos Operativos, Ganancia del mes, Costo/Día) · sparklines fuera (`sparkline()` y SVG en KpiCard) · merge RecentActivity+PendingInvoices → **ActivityPanel** (borrar PendingInvoices; formatDate pasa a `utils/format.ts`) · CSS duplicado → clases globales en `app.scss`.

### Sección 2 — Navegación (4b)
Sidebar 12→8 items (Operaciones: Dashboard, Productos, Proveedores, Facturas, Costos; Análisis: Análisis, Préstamos; Sistema: Equipo). Ventas/Patrimonio/Contabilidad conservan ruta+archivo pero salen del sidebar; **ConfiguracionPage se borra** (ruta + archivo, read-only). Bottom nav mobile: Dashboard, Productos, Facturas, Costos.

### Sección 3 — AnalisisGastosPage: vital + todo bajo demanda (4c-3)
Default visible: métricas de inversión + chart de categorías + top productos. **Colapsados por defecto** (`q-expansion-item`): ABC, Tendencias/Impacto márgenes (tabs), Costo Operativo, Proyección, Alertas (motor backend, reemplaza las locales), y supplier (comparativa/recomendaciones/predicciones). Refurbish de los 6 componentes huérfanos al design system actual.

### Sección 4 — Páginas restantes (4c 1/2/4/5)
Jargon aplicado en toda la UI · FacturasPage: dual-flow "Gasto rápido" vs "Factura con items" (sin select Tipo) · CostosPage: Config tab → inline · renombrar jargon en AccountingPage (Margen Operativo→"Ganancia bruta", etc.).

### Sección 5 — Limpieza (4d)
Dead code: `mounted` ref en KpiCard, `compact` variant, `handleExportar` emit, `useAuthStore` innecesario en AnalyticsHeader · unificar `formatDate` en `utils/format.ts`.

### Post-Fase 4: Tutorial Guiado + Dashboard polish
Diseñados SOBRE el resultado de Fase 4. El workflow depurado define:
- Qué 3-4 elementos son esenciales para el tour
- Dónde va el botón "Ayuda"
- Qué hover states y empty states sobreviven la depuración

## Fase 5 — Design System de Charts + Botones/Iconos (2026-08-17)

**Objetivo:** Unificar el sistema visual de charts, botones e iconos. Eliminar inconsistencias (dos sistemas de botones paralelos, tres formas de colorear iconos, hardcoded hex).

### Fase 5a — Charts Design System ✅ COMPLETADA

**Problema:** 3 enfoques coexisten para charts (Chart.js, CSS puro, SVG inline). Hardcoded hex en vez de tokens. `vue-chartjs` instalado pero nunca importado (dead weight).

**Solución:**
1. **Chart tokens** en `app.scss` (13 variables CSS: `--pq-chart-bar`, `--pq-chart-line`, `--pq-chart-area`, `--pq-chart-grid`, `--pq-chart-text`, `--pq-chart-tooltip-*`, `--pq-chart-positive/negative`, `--pq-chart-abc-a/b/c`).
2. **Composable `useChartTheme.ts`** — retorna `colors` + `defaults` de Chart.js desde tokens CSS.
3. **BaseChart.vue** actualizado para usar `useChartTheme()` en vez de hardcoded.
4. **Migrados a Chart.js:** VentasVsCostosChart (CSS→bar), CategoryBreakdownChart (CSS→horizontal bar), ExpenseBreakdown (CSS→doughnut).
5. **Refactorizados con tokens:** AbcGastosChart, PriceTrendSparkline, ProjectionTimeline, OpexGauge, SupplierComparisonTable, PricePredictionsTable.
6. **Eliminado:** `vue-chartjs` de package.json (dead weight).

### Fase 5b — Botones e Iconos ✅ CERRADO (2026-08-18 `f31a561`)

**Problema:** Dos sistemas paralelos (`BaseButton` 68 usos + `q-btn` 81 usos). Tres formas de colorear iconos (Quasar prop, CSS class, inline style). `BaseButton` hardcodea hex. Colores fuera de tema (`red`, `amber`).

**Solución:**
1. **Unificar en `q-btn`** — migrar 68 usos de `BaseButton` a `q-btn`, eliminar `BaseButton.vue`. ✅ (`git show f31a561 --stat` -184, `grep BaseButton 0`, `grep q-btn 167`)
2. **Icon utility classes** en `app.scss` — `text-icon-accent`, `text-icon-danger`, etc.
3. **Global button overrides** en `app.scss` — `q-btn--primary`, `q-btn--positive`, etc. con tokens CSS. ✅ (`app.scss` +44 en `f31a561`)
4. **Reemplazar inline styles** en ~20 iconos (10 archivos).
5. **Fix non-theme colors** — `color="red"` → `color="negative"`, `color="amber"` → `color="warning"`. ⏳ residual: `AuthOptionsPage.vue:25 color="red"` + `AcceptInvitationPage.vue:35 color="amber"` (2 casos) + `text-icon-*` utils — polish bajo, no bloquea Fase 5b.

## Notas de viabilidad (ponytail)

- Backend de los ítems 1, 4 y 5 ya está implementado — son pura orquestación frontend.
- Ninguna fase exige migración de datos ni cambios de contrato API.
- `usePullToRefresh` y el panel de alertas ya tienen el código/composables base escritos.
- **Fase 4 es puramente frontend** — no toca backend, ni migraciones, ni contratos API.
- **Fase 5a completada** — chart tokens + migración CSS→Chart.js + eliminación vue-chartjs.
- **Fase 5b cerrada 2026-08-18 `f31a561`** — `BaseButton` eliminado, `167 q-btn`, polish residual 2 colores + icon utils.
