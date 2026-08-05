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

## Notas de viabilidad (ponytail)

- Backend de los ítems 1, 4 y 5 ya está implementado — son pura orquestación frontend.
- Ninguna fase exige migración de datos ni cambios de contrato API.
- `usePullToRefresh` y el panel de alertas ya tienen el código/composables base escritos.
