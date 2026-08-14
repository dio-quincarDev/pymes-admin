# Daily Reports — Frontend PYMEQ

Registro cronológico de decisiones, problemas resueltos y estado del frontend.

---

## 2026-08-14 — Fix OAuth2 regresión (AuthCallback + store)

### Contexto

Fix de las 2 regresiones introducidas en `a0cc92d` (2026-08-13). El commit anterior rompió el flujo OAuth2 al agregar un `else` branch incorrecto y llamadas innecesarias a `fetchCurrentUser()`.

### Qué se hizo

- **`AuthCallback.vue`**: eliminado `else` branch (líneas 68-73) que forzaba redirect a `/onboarding` cuando `tenantId` era null. Usuarios con workspace existente ahora caen al dashboard correctamente.
- **`store/index.ts`**: eliminado `await this.fetchCurrentUser()` de `login()` y `selectTenant()`. Solo se ejecuta en `handleOAuthCallback()`, que es el único camino OAuth2. Evita race condition donde `/users/me` sobreescribe `user` con `tenantId: null`.

### Root cause

`fetchCurrentUser()` llama `GET /users/me` y sobreescribe `this.user`. Si el JWT no tiene `tenantId` (OAuth2 directo sin intent), el user resultante tiene `tenantId: null` y todos los flujos dependientes silenciosamente fallan.

### Archivos modificados

```
src/modules/auth/pages/AuthCallback.vue     — eliminado else branch
src/modules/auth/store/index.ts            — eliminado fetchCurrentUser() de login() y selectTenant()
```

### Pendiente

- TeamsPage: `isOwner` depende de `user.role`; verificar que `fetchCurrentUser()` en `handleOAuthCallback` devuelve el role correctamente.

**Estado:** ✅ COMPLETADO

---

## 2026-08-13 — Regresiones, seguridad debilitada y pérdida de tiempo

### Contexto

Sesión de "fixes" que arregló 2 cosas, rompió el flujo OAuth2, debilitó una protección de seguridad existente y dejó pendiente el borrado de miembros. No se probó el flujo completo antes de commitear. No se revisó el diff propio antes de commitear. Se perdió tiempo buscando archivo por archivo en vez de hacer greps generalizados.

### Qué se hizo

- **PWA cache fix (nginx.conf + MainLayout.vue)** — ✅ funcionó. `sw.js` ahora se sirve con `no-cache` (`location ^~ /sw.js`) y el SW se actualiza sin dialog de bloqueo (`SKIP_WAITING` automático).
- **Quasar Dialog plugin** — ✅ se agregó `'Dialog'` a `quasar.config.ts`. `TeamsPage` puede abrir dialogs de nuevo.
- **fetchCurrentUser() en login() y selectTenant()** — 🟡 innecesario y contraproducente. `handleOAuthCallback()` ya lo tenía. Agregarlo en otros caminos crea ejecución extra y potenciales race conditions con `clearSession()`.
- **OAuth2: se eliminó Prioridad 3 (auto-crear "Mi Empresa")** — 🔴 correcto en intención, pero se agregó un `else` en `AuthCallback.vue` que redirige a `/onboarding` cuando `tenantId` es null. **Esto rompió el flujo**: usuarios con intent que ya tenían workspace caen al onboarding en vez del dashboard.

### Los problemas del usuario (estado actual)

1. **El flujo OAuth2 ya no lleva al workspace** (invoice de industrias) — redirige a `/onboarding` / "main" y permite "terminar de registrarse". 🔴 REGRESIÓN de la sesión. Causa: `else` branch en `AuthCallback.vue` + posible timing en `fetchCurrentUser()`.

2. **Miembro invitado no se puede borrar desde TeamsPage.vue** — el botón `person_remove` tiene `v-if="isOwner"`, y `isOwner` depende de `authStore.user?.role`. Si el role no llega (fallo de timing/token en `fetchCurrentUser()`), el botón no aparece. 🔴 Existía antes, pero la sesión no lo resolvió y pudo empeorarlo.

3. **Tokens y UUIDs visibles en el navegador** — el usuario reportó que ya existía una "estrategia" para ocultarlos y volvió a encontrarlos visibles. ❓ NO SE RESOLVIÓ. No se localizó ni documentó dicha estrategia; se perdió tiempo buscando sin un plan claro.

### Incompetencia de la sesión (honestidad brutal)

- Cada fix generó un bug nuevo → el "loop" del usuario es real (arreglar A rompe B).
- No se revisó el `git diff` propio antes de commitear. El usuario tuvo que señalarlo explícitamente.
- Se buscaron archivos uno por uno en vez de usar grep/glob de forma eficiente. El usuario tuvo que reclamar el desperdicio de tiempo.
- No se resolvió el borrado de miembros — solo se documentó.
- No se resolvió la visibilidad de tokens/UUIDs — ni siquiera se identificó la estrategia previa.
- Se commiteó (`a0cc92d`, 12 archivos) sin verificar el flujo completo (login OAuth2 → workspace).

### Pendiente / próxima sesión

1. Revertir el `else` branch de `AuthCallback.vue` (volver a caer al dashboard cuando no hay tenantId).
2. Revertir `fetchCurrentUser()` de `login()` y `selectTenant()` en el store (era innecesario; solo vive en `handleOAuthCallback`).
3. Investigar la "estrategia de ocultar tokens/UUIDs" preguntando al usuario qué era antes de tocar nada.
4. Arreglar `isOwner` en TeamsPage: verificar que `fetchCurrentUser()` devuelve el role y que el botón de borrar aparece.
5. **Regla de oro: NO commitear sin probar el flujo completo antes.**

**Estado:** ❌ SESIÓN REGRESIVA — 2 fixes ok, 2 regresiones, 1 sin resolver.

---
## 2026-08-10 — OAuth2 URLs por origin + PWA install banner

### Contexto

Migración del subdominio a `pymeq.dioquincar.dev`. El flujo "Google register/login" apuntaba hardcodeado a `http://localhost:8080`, quebrado desde cualquier navegador remoto. Además, el banner de instalación PWA aparecía sin prompt real (botón "Instalar" muerto) y no existía soporte iOS.

### Qué se hizo

- **OAuth2 por origin**: `LoginPage.vue`, `RegisterPage.vue` y `AuthOptionsPage.vue` reemplazan `http://localhost:8080/oauth2/authorization/google` por `${window.location.origin}/oauth2/authorization/google`.
- **AuthOptionsPage**: además se corrigió la ruta `/api/v1/oauth2/authorization/google` (inexistente en el gateway) por `/oauth2/authorization/google` (pública).
- **PWA banner** (`MainLayout.vue`):
  - Se muestra solo cuando hay `beforeinstallprompt` (Android) o es iOS.
  - iOS: instrucciones "Compartir → Agregar a pantalla de inicio" (sin botón Instalar, que era muerto).
  - Android: si toca "Instalar" sin prompt aún, notifica que aparecerá tras usar la app.

### Archivos modificados

```
modules/auth/pages/LoginPage.vue       → oauth URL por window.location.origin
modules/auth/pages/RegisterPage.vue    → oauth URL por window.location.origin
modules/auth/pages/AuthOptionsPage.vue → oauth URL por origin + ruta corregida
layouts/MainLayout.vue                 → banner PWA condicional (prompt/iOS) + feedback
```

### Verificación

- Frontend lint: clean

### Notas

- El prompt nativo de Chrome requiere "engagement" del usuario (no aparece en la primera visita).
- iOS no soporta `beforeinstallprompt`: se instala solo por Share → Add to Home Screen.

**Estado:** ✅ COMPLETADO

---

## 2026-08-05 — Cierre Fase 2 + Fase 3 + subtítulos descriptivos

### Contexto

Cierre de las fases 2 y 3 del TO_DO.md frontend. Fase 2: simplificación UI (eliminar duplicación Dashboard/Contabilidad + panel salud financiera). Fase 3: cierre del modelo de gastos (facturas como fuente única).

### Fase 2 — Simplificación UI + Salud financiera

**AccountingPage.vue:**
- Eliminados 4 KPIs duplicados (Ingresos, Costos, Margen Bruto, Gastos Operativos). Quedan solo Margen Operativo + Margen Neto.
- Grid CSS: `repeat(6, 1fr)` → `repeat(2, 1fr)`.
- Skeleton: `v-for="i in 6"` → `v-for="i in 2"`.

**DashboardPage.vue:**
- Agregados 2 KPIs nuevos: Margen Operativo + Margen Neto (datos ya en `MetricasFinancieras`).
- Import de `useAnalytics` para obtener `financialHealth`.
- Import + render de `FinancialHealthPanel` en sidebar.

**types/analytics.ts:**
- +5 interfaces: `FinancialHealthBreakdown`, `FinancialHealthAlert`, `FinancialHealthExpansionRequirement`, `FinancialHealthExpansion`, `FinancialHealth`.
- `AnalyticsResponse` +`financialHealth?: FinancialHealth`.

**useAnalytics.ts:**
- +`financialHealth` computed (lee `data.value?.financialHealth`).
- +`criticalAlerts` computed (deriva de `financialHealth`).
- +`recommendations` computed (deriva de `financialHealth`).

**FinancialHealthPanel.vue (nuevo):**
- Score circular 0-100 con color dinámico (rojo <40, amarillo 40-70, verde >70).
- Lista de `criticalAlerts` (dot + título + acción).
- Lista de `recommendations`.
- Loading skeleton + empty state.

### Fase 3 — Cierre modelo de gastos

**useFinancialDashboard.ts:**
- Eliminado `gastoService` (import + 2 llamadas `getAll` en `Promise.all`).
- Eliminados refs `gastos` y `gastosPrev`.
- `gastosPorCategoria` ahora deriva de `facturas` filtradas por `type === 'GASTO_OPERATIVO' && status === 'PAGADA'`, agrupadas por `category`.
- `gastosPorCategoriaPrev` retorna `[]` (comparación por período no estaba filtrada antes).
- `actividadReciente` ahora usa facturas en vez de gastos.
- Reducido `Promise.all` de 7 a 5 llamadas.

### Subtítulos descriptivos

- Dashboard: "Resumen financiero del período" → "Cómo está mi negocio hoy"
- Análisis: "¿Dónde va tu plata?" → "Dónde gasto y qué proveedores me convienen"
- Contabilidad: "Rendimiento financiero consolidado" → "Cuánto gano después de todos los costos"
- `AnalyticsHeader.vue`: eliminado `display: none` del subtítulo en mobile.

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

**Estado:** ✅ FASE 2 + FASE 3 CERRADAS

---

## 2026-08-04 — Colaborador en facturas SALARIOS + auto-fill desde gastos fijos + mini-dialog proveedor

### Contexto

Se completó el flujo de facturación GASTO_OPERATIVO con vinculación a colaboradores para la categoría SALARIOS, auto-fill de proveedor desde gastos fijos recurrentes, y mini-dialog de creación de proveedor desde CostosPage.

### Colaborador en FacturasPage (SALARIOS)

- `FacturaRequest`: +`colaboradorId?: string | null`.
- `Factura`: +`colaboradorId: string | null`, +`collaboradorName: string | null`.
- `save()`: envía `colaboradorId: gastoOperativo ? form.value.colaboradorId : null`.
- El backend solo guarda `colaboradorId` cuando `isGastoSinItems=true` (GASTO_OPERATIVO sin items).

### Auto-fill proveedor desde gastos fijos

- `gastoFijoCategorias` ya no deduplica — muestra cada gasto fijo como opción separada con nombre de proveedor (ej: `LUZ — Naturgy · $100`).
- `applyCategoria()` auto-fill `total` + `proveedorId` desde el gasto fijo seleccionado + resetea `providerFilteredOptions`.

### Mini-dialog creación de proveedor (CostosPage)

- Botón `q-btn icon="add"` (`type="button"`) + `q-input` name + "Crear" button.
- `saveNewProveedor()` llama `proveedorService.create()`, agrega a lista, auto-selecciona.

### InvoiceDetailDialog — collaborador

- Cuando `category === 'SALARIOS'`: muestra "Colaborador: {name}" en vez de "Proveedor: —".

### Types

- `Factura`: +`colaboradorId: string | null`, +`collaboradorName: string | null`.
- `FacturaRequest`: +`colaboradorId?: string | null`.

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

**Estado:** ✅ RESUELTO

---

## 2026-08-04 — Cierre Fase 1: redirect GastosPage + fix QuickActions

### Contexto

Fase 1 del TO_DO.md (Quick wins) estaba al 70%. Faltaba cerrar la ruta `gastos` y el botón "Registrar gasto" en QuickActions.

### Cambios

**`core/router/routes.ts`:** ruta `gastos` reemplazada por redirect a `/dashboard/costos?tab=gastosFijos`. Maneja bookmarks viejos sin romper.

**`QuickActions.vue`:** `handleNuevoGasto()` navega a `/dashboard/costos?tab=gastosFijos` en vez de `/dashboard/gastos`.

**No se tocó:** `GastosPage.vue` (tree-shaking lo excluye del bundle), `gasto.service.ts` (lo usa el dashboard).

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

**Estado:** ✅ FASE 1 CERRADA

---

## 2026-08-04 — GASTO_OPERATIVO suite + Proveedor en gastos fijos + Bug fix q-select

### Contexto

Se completó la suite de facturación GASTO_OPERATIVO con soporte para categorías de gastos fijos, salario por rango de días, y se vinculó proveedor a gastos fijos recurrentes desde CostosPage.

### Suite GASTO_OPERATIVO en FacturasPage

Al elegir tipo `GASTO_OPERATIVO` se ocultan items/CategoryTabs/"Agregar item" y se muestra sección Categoría + Total/Monto:

- **Categoría** (opciones: Salarios + categorías de gastos fijos activos + Otro, con monto en label).
- **Salarios** → select colaborador + rango Desde/Hasta para DIARIO (precarga `días × monto`, hint del cálculo).
- **Total** siempre editable.

### Bug fix: q-select tipo con opciones objeto

Root cause: `q-select` de tipo con opciones objeto sin `map-options emit-value`, dejaba `form.tipo` como objeto → condiciones `=== 'GASTO_OPERATIVO'` fallaban. Fix: revertido a opciones strings `['FACTURA','GASTO_OPERATIVO']`. `map-options emit-value` añadido a selects Categoría y Colaborador.

### Proveedor en gastos fijos (CostosPage)

- Import `proveedorService`. Fetch en `loadAll()` via `Promise.all`.
- Select "Proveedor" opcional (`clearable`, `map-options emit-value`) en dialog de gasto fijo.
- ProveedorName en tarjeta: Día · método · proveedor.

### Enum GAS

- `GAS` añadido a `categoriaOptions` en CostosPage (antes faltaba pese al enum backend).

### Types

- `GastoFijoRecurrente`: +`proveedorId: string | null`, +`proveedorName: string | null`.
- `GastoFijoRequest`: +`proveedorId?: string | null`.

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

**Estado:** ✅ RESUELTO

---

## 2026-07-30 — Bug fixes Patrimonio + Currency formatting + Gastos/Metricas gap discovery

### Contexto

Revisión y corrección del flujo de Patrimonio, más descubrimiento de gaps en Gastos y Metricas.

### Patrimonio — Bug fixes

El frontend de Patrimonio tenía field names en inglés (`initialCapital`, `startDate`) que no coincidían con el backend (español: `capitalInicial`, `fechaInicio`). Fix:

| Campo | Antes | Después |
|-------|-------|---------|
| Capital | `initialCapital` | `capitalInicial` |
| Fecha | `startDate` | `fechaInicio` |
| Notas | `notes` (enviado en PUT) | eliminado (backend no lo persiste) |
| tenantId | solo path param | también en body (`@NotNull` en backend) |
| fechaInicio vacío | `""` | `null` (backend rechaza string vacío) |
| capitalInicial ≤ 0 | se enviaba → 400 | validación pre-save con notificación |

Además se eliminó el textarea de Notas del formulario y se agregó la validación `capitalInicial > 0`.

### Currency formatting input — 3 páginas

Se reemplazó `type="number"` por `type="text"` + `inputmode="decimal"` con formateo al blur:

| Página | Campo | Comportamiento |
|--------|-------|----------------|
| PatrimonioPage | Capital Inicial | Escribís `10000` → se formatea a `10,000.00` |
| GastosPage | Monto | Ídem |
| VentasPage | Monto Bruto | Ídem |

Solo acepta dígitos y un punto decimal mientras se escribe.

### Gaps descubiertos — Gastos + Metricas

1. **🔴 Field name mismatch en Gastos** — Frontend envía `{ category, amount, expenseDate }`, backend espera `{ categoria, monto, fecha }`. POST/PUT probablemente fallan con 400.
2. **🔴 Field name mismatch en MetricasFinancieras** — Frontend usa inglés (`totalIncome`, `operatingExpenses`), backend responde español (`totalIngresos`, `gastosOperativos`). DashboardPage y AccountingPage no muestran datos correctamente.
3. **🟡 Date range no expuesto** — `GastoRepository` tiene `findByTenantIdAndExpenseDateBetween` pero ningún endpoint lo usa.
4. **🟡 Sin paginación** en gastos.
5. **🟡 AnalisisGastosPage es misnomer** — analiza productos, no gastos operativos.

### Archivos modificados

```
src/modules/core/pages/PatrimonioPage.vue       # field names alineados, +currency formatting
src/modules/core/pages/GastosPage.vue            # +currency formatting
src/modules/core/pages/VentasPage.vue            # +currency formatting
src/modules/core/types/index.ts                  # Patrimonio/PatrimonioRequest fields renamed
docs/GAPS.md                                     # +gastos/metricas field mismatch gaps
docs/TO_DO.md                                    # +session completed items
frontend/pymes/docs/DAILY_REPORTS_FRONTEND.md    # esta entrada
```

### Build

- `npm run lint`: ✅
- `quasar build -m pwa`: ❌ (TS errors por mismatch en Gastos/Metricas aún no corregido — pendiente)

**Estado:** ✅ PARCIAL — bugs de Patrimonio + currency formatting corregidos. Pendiente fix de field names en Metricas.

---

## 2026-07-30 (2) — Fix field name mismatch Gastos

### Contexto

Mismo patrón que Patrimonio: frontend enviaba campos en inglés, backend esperaba español. Bug 🔴 que rompía POST/PUT de gastos.

### Cambios

| Archivo | Cambio |
|---------|--------|
| `types/index.ts` | `GastoOperativo` y `GastoRequest`: `category`→`categoria`, `description`→`descripcion`, `amount`→`monto`, `expenseDate`→`fecha`, `paymentMethod`→`metodoPago` |
| `GastosPage.vue` | Form init, template bindings, computed → campos renombrados |
| `useFinancialDashboard.ts` | `g.category`→`g.categoria`, `g.amount`→`g.monto`, etc. + `GastoPorCategoria.category`→`categoria` |
| `ExpenseBreakdown.vue` | `item.category`→`item.categoria`, `{ category: 'Otros' }`→`{ categoria: 'Otros' }` |
| `DashboardPage.vue` | `g.category`→`g.categoria`, `p.category`→`p.categoria` |

### Build

- `vue-tsc --noEmit`: ✅
- `npm run lint`: ✅

**Estado:** ✅ Gastos fix completo. Pendiente: field name mismatch MetricasFinancieras.

---

## 2026-07-29 — Analytics Suite + Invitation UX/UI fixes

### Contexto

Se implementó el rediseño de la suite de analytics (spec en `.ulpi/design/analytics-suite.md`) y se corrigieron 2 bugs UX/UI en el flujo de invitación/registro.

### Analytics Suite — componentes nuevos

5 componentes creados en `src/modules/core/components/analytics/`:

| Componente | Props | Descripción |
|------------|-------|-------------|
| `AnalyticsHeader.vue` | title, subtitle, period, loading, showRecalculate | Header compartido con selector de período y botón recalcular |
| `KpiCard.vue` | label, value, delta, deltaLabel, accent, loading | Card de métrica con sparkline inline opcional |
| `MetricCard.vue` | label, value, icon, trend, previousAmount | Card de métrica con tendencia |
| `CategoryBreakdownChart.vue` | categories, loading, title | Gráfico de barras comparativo por período |
| `DataTable.vue` | columns, rows, loading, title, search, pagination | Wrapper de QTable con tokens de DESIGN.md |

### Páginas refactorizadas

**DashboardPage.vue:**
- Reemplazado header manual + `StatStrip` + `PeriodSelector` por `AnalyticsHeader` + 4 `KpiCard` + `CategoryBreakdownChart`
- Eliminado gradient text, layout 2 columnas para chart + actividad reciente
- Composable `useFinancialDashboard.ts` ya tenía datos de período anterior — sin cambios necesarios

**AnalisisGastosPage.vue:**
- Reemplazado header por `AnalyticsHeader`, 3 `MetricCard`, `CategoryBreakdownChart`, `DataTable`, panel de alertas con severity badge

**AccountingPage.vue:**
- Reemplazado header por `AnalyticsHeader`, 6 `KpiCard` con delta, `SummaryCard` refinado
- Eliminado gradient text

### Invitation flow — UX/UI fixes

**BaseButton.vue — bug fix:**
- Agregado `label?: string` prop + fallback `<span v-if="!$slots.default && label">{{ label }}</span>` después del `<slot />`
- Antes: `AcceptInvitationPage` y `TeamsPage` pasaban `label="..."` pero el componente solo renderizaba via `<slot />` → botones vacíos

**AcceptInvitationPage.vue — dark mode forms:**
- 4 `q-input` (name, email, password, confirmPassword): `outlined dense` → `dark filled color="primary" label-color="accent"`

**TeamsPage.vue — dark mode forms:**
- 3 fields (email `q-input`, rol `q-select` invite dialog, rol `q-select` role dialog): `outlined dense` → `dark filled color="primary" label-color="accent"`

### Archivos modificados

```
src/modules/core/components/analytics/AnalyticsHeader.vue     # refactor: periodOptions a script setup
src/modules/core/components/analytics/KpiCard.vue             # nuevo
src/modules/core/components/analytics/MetricCard.vue          # nuevo
src/modules/core/components/analytics/CategoryBreakdownChart.vue  # nuevo
src/modules/core/components/analytics/DataTable.vue           # nuevo
src/pages/DashboardPage.vue                                   # refactor con analytics components
src/modules/core/pages/AnalisisGastosPage.vue                 # refactor con analytics components
src/modules/core/pages/AccountingPage.vue                     # refactor con analytics components
src/components/base/BaseButton.vue                            # +label prop + fallback render
src/modules/auth/pages/AcceptInvitationPage.vue               # dark mode forms
src/modules/auth/pages/TeamsPage.vue                          # dark mode forms
```

### Design spec

- `.ulpi/design/analytics-suite.md` — spec completo del rediseño
- `.ulpi/design/invitation-flow-fix.md` — spec de los fixes de invitación

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

**Estado:** ✅ RESUELTO

---

## 2026-07-28 — TeamsPage migrado a auth + onAccept fix

### Problema

1. `TeamsPage.vue` estaba en `modules/core/pages/` — pero es funcionalidad de autenticación/members, no core business.
2. `AcceptInvitationPage.vue` tenía bug crítico: `onAccept()` hacía `response as AuthResponse` pero el endpoint de aceptar invitación no retorna `AuthResponse`.
3. No existía manejo de mismatch de email entre usuario autenticado y email de la invitación.

### Solución

**AcceptInvitationPage.vue:**
- `onAccept()` ahora hace `await invitationService.accept(token)` + `fetchCurrentUser()` (sin cast)
- Nuevo computed `emailMismatch`: compara `currentUser.email` con `invitationInfo.email`
- Muestra alerta "Email no coincide" con botón "Cerrar sesión y registrarme"

**TeamsPage.vue:**
- Movido de `modules/core/pages/` → `modules/auth/pages/`
- Imports actualizados: `useAuthStore` en vez de `useSetupStore`
- `fetchMembers()` usa `authStore.user.tenantId`

**Router:**
- Ruta `/teams` removida de `coreRoutes`
- Creado `authDashboardRoutes` en `modules/auth/router/routes.ts`
- Importado en `src/router/routes.ts`

### Archivos modificados

```
src/modules/auth/pages/AcceptInvitationPage.vue   # onAccept fix + email mismatch
src/modules/auth/pages/TeamsPage.vue               # moved from core
src/modules/auth/store/index.ts                   # selectTenant action
src/modules/auth/router/routes.ts                 # authDashboardRoutes
src/modules/core/router/routes.ts                 # removed /teams route
src/router/routes.ts                              # imports authDashboardRoutes
```

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

**Estado:** ✅ RESUELTO

---

## 2026-07-27 — Fix paginación ProductosPage

### Problema

`ProductosPage.vue` tenía un bug en la paginación: el botón "Cargar más" llamaba `load(page + 1)` pero `load()` reemplazaba `rows.value` en vez de apilar resultados. Cada click cargaba la siguiente página pero descartaba las anteriores.

Además, el conteo de productos mostraba `filteredRows.length` (solo la página actual) en vez del total real del server.

### Solución

| Cambio | Antes | Después |
|--------|-------|---------|
| `load()` | `rows.value = res.data.content` | `rows.value = p === 0 ? res.data.content : [...rows.value, ...res.data.content]` |
| `totalElements` | no existía | `totalElements.value = res.data.totalElements` |
| Conteo | `filteredRows.length` | `totalElements` |
| Condición "load more" | `rows.length >= 30` | `totalElements > rows.length` |

### Archivos modificados

```
src/modules/core/pages/ProductosPage.vue    # +totalElements, fix load() append, fix count, fix load-more condition
```

### Build

- `npm run lint`: ✅

**Estado:** ✅ RESUELTO

---

## 2026-07-25 — Critical fixes: refresh rotation, tenantId guard, error normalization, tests

### Que se hizo

5 items críticos del TO_DO.md y GAPS.md resueltos en una sesión:

**1. `isAuthError()` normalizado** — `utils/errors.ts`: `isAuthError`, `isTokenExpiredError`, `isTokenRevokedError`, `isValidationError` ahora leen del error normalizado (`ApiError.code`/`.status`) primero, fallback a raw axios. Ya no dependen de estructura `AxiosError`.

**2. Listener `auth:401` duplicado** — `store/index.ts`: el listener replicaba `clearSession()` parcialmente. Simplificado a `clearSession()` directo + guard `typeof window` para entornos no-browser (tests).

**3. `tenantId` fallback `|| ''` eliminado en 12 archivos** — `authStore.user?.tenantId || ''` reemplazado por guard `if (!tenantId) return` antes del primer llamado API. Form inits usan `as string`.

**4. Refresh token rotation** — `boot/axios.ts`: interceptor captura 401, encola requests fallidas, renueva con refresh token via raw `axios.post` (evita loop de interceptor). Si refresh falla → `clearSession()` + redirect.

**5. Tests** — 29 tests (errors 10 existentes + store 6 + composables 3 + errores extendidos 10). `vitest run` pasa limpio. `npm run lint`: 0 errores.

### Archivos modificados

```
src/utils/errors.ts                                    # error checks normalizados
src/modules/auth/store/index.ts                         # listener simplificado + guard window
src/modules/auth/pages/LoginPage.vue                    # tenantId guard
src/modules/auth/pages/RegisterPage.vue                 # tenantId guard
src/modules/core/pages/ProductosPage.vue                # tenantId guard
src/modules/core/pages/ProveedoresPage.vue              # tenantId guard
src/modules/core/pages/FacturasPage.vue                 # tenantId guard
src/modules/core/pages/GastosPage.vue                   # tenantId guard
src/modules/core/pages/VentasPage.vue                   # tenantId guard
src/modules/core/pages/PrestamosPage.vue                # tenantId guard
src/modules/core/pages/PatrimonioPage.vue               # tenantId guard
src/modules/core/pages/AccountingPage.vue               # tenantId guard
src/modules/core/pages/AnalisisGastosPage.vue           # tenantId guard
src/modules/core/pages/OnboardingPage.vue               # tenantId guard
src/modules/core/pages/ConfiguracionPage.vue            # tenantId guard
src/modules/auth/pages/AcceptInvitationPage.vue         # tenantId guard
src/boot/axios.ts                                       # refresh rotation + cola
src/modules/auth/store/__tests__/index.spec.ts           # store test (nuevo)
src/composables/__tests__/useAuthForm.spec.ts            # composable test (nuevo)
src/utils/__tests__/errors.spec.ts                      # tests extendidos
```

### Build

- `npm run lint`: ✅
- `npx vitest run`: ✅ 29 tests passed

---

## 2026-07-24 — Fix OAuth2 intentId: `?state=` → `?intentId=`

### Bug
Registro por Google ignoraba el nombre de empresa escrito por el usuario, creando "Mi Empresa" como default.

### Causa raíz
El frontend pasaba el `intentId` como `?state=${intentId}` al redirect de Google, pero `OAuth2IntentCookieFilter` (auth service) busca el param `?intentId=`. Spring Security usa `state` internamente para CSRF y lo sobreescribe — el param nunca llegaba al filter, la cookie `oauth2_intent` no se seteaba, y `OAuth2AuthenticationSuccessHandler` caía a Prioridad 3 (default "Mi Empresa").

### Fix
Cambiar `?state=${intentId}` → `?intentId=${intentId}` en ambas páginas.

### Archivos
```
modules/auth/pages/LoginPage.vue:218
modules/auth/pages/RegisterPage.vue:262
```

---

## 2026-07-21 — Brainstorm: Tutorial onboarding para nuevos usuarios

### Contexto

Usuarios nuevos pasan por: Landing → Register → Verify email → Onboarding (industria) → Dashboard. Tras el onboarding no hay guía, el usuario llega al dashboard sin saber por dónde empezar.

### Enfoques evaluados

| Enfoque | Descripción | Esfuerzo |
|---------|-------------|----------|
| **A — Tour guiado con Driver.js** | Pasos con overlay resaltando sidebar, período, métricas, quick actions. 7KB gzipped, framework-agnostic. Se dispara 1 vez post-onboarding. | Bajo |
| **B — Descubrimiento progresivo** | Mensajes contextuales en empty states de cada página. Sin overlay ni tour formal. | Muy bajo |
| **C — Checklist primeros pasos** | Panel colapsable en dashboard con tareas (agregar producto, crear factura, etc.) que se marcan al completarse. | Medio |
| **D — Híbrido** | Tour bienvenida rápido (4-5 pasos) + empty states contextuales + botón "Ayuda" para repasar. | Bajo |

### Recomendación

**Enfoque A (Driver.js)** como punto de partida:
- Driver.js no necesita wrapper Vue, se usa directo en un composable `useTour()`
- Se almacena `tourCompletado` en localStorage
- Botón "Ayuda" en header del MainLayout para reiniciar
- 4-5 pasos: sidebar, selector período, quick actions, menú perfil

### Referencias

- Driver.js: https://driverjs.com
- TO_DO.md: sección `Frontend — Pendiente (Tutorial)`

**Estado:** 📝 BRAINSTORM — pendiente de implementación

---

## 2026-07-21 — Core error messages propagados a páginas core

### Problema

Las páginas core usaban `catch { }` sin parámetro. El interceptor axios (`boot/axios.ts`) ya normaliza errores backend a `new Error(mensaje)` con propiedades `code`, `status`, `details`, `isBackendError`, pero ~30 catch blocks descartaban el error y mostraban mensajes fijos como `'Error al cargar productos'`.

### Cambio

32 catch blocks migrados en 12 archivos:

| Archivo | Catches | Patrón |
|---------|---------|--------|
| `PatrimonioPage.vue` | 2 | `catch { msg }` → `catch (err) { err instanceof Error ? err.message : msg }` |
| `PrestamosPage.vue` | 5 | Ídem |
| `AnalisisGastosPage.vue` | 1 | Ídem |
| `ProductosPage.vue` | 3 (1 skip: `/* non-critical */`) | Ídem |
| `ProveedoresPage.vue` | 3 | Ídem |
| `ConfiguracionPage.vue` | 1 | Ídem |
| `AccountingPage.vue` | 2 | Ídem |
| `FacturasPage.vue` | 5 (incl. 1 `.catch(() => {})`) | `(err: unknown)` + mismo patrón |
| `VentasPage.vue` | 3 | Ídem |
| `GastosPage.vue` | 3 | Ídem |
| `OnboardingPage.vue` | 2 | Ídem |
| `PresentacionesDialog.vue` | 2 | Ídem |

**Patrón aplicado**:
```ts
// Antes
} catch {
    $q.notify({ type: 'negative', message: 'Error al guardar' })
}

// Después
} catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al guardar' })
}
```

### Dependencias externas

- El interceptor axios (`src/boot/axios.ts:40-73`) ya devuelve `Promise.reject(Object.assign(new Error(mensaje), { code, status, details, isBackendError: true }))` — ningún cambio necesario en infraestructura.
- Los tipos (`BackendError`, `ERROR_CODES`) ya estaban definidos en `src/types/error.ts`.

### No cambiado

- `ProductosPage.vue:73` — `catch { /* non-critical */ }` intencionalmente silencioso (loadSetup de datos secundarios)
- Auth pages (Login, Register, VerifyEmail, ResetPassword, AcceptInvitation) — ya usaban `catch (err)` con verificación de códigos específicos

---

## 2026-07-15 — Rediseño Swiss/Grid + fix tenantId

### Design system overhaul

- **IDENTITY.md** → `DESIGN.md`: Swapped "Copper Luxe" for "Swiss / Grid + Institutional Warmth". Near-black `#08090D` + bronze `#C8963E`. Geist (display) + Satoshi (body) + Geist Mono (numbers). Eliminated glassmorphism, brand-glow, mesh-text-gradient.
- **`app.scss`**: Stripped legacy classes (`brand-glow`, `glass-light`, `glass`, `mesh-text-gradient`). New `:root` block with full `--pq-*` tokens: shadow, z-index, motion, border-radius.
- **`quasar.variables.scss`**: Updated `$pq-*` Sass vars to match `DESIGN.md` palette.
- **`tokens.ts`**: Updated TS tokens — same palette, Geist/Satoshi/Geist Mono stack.

### Components rewritten

- **`BaseButton.vue`**: Flat solid bg, no gradients. 5 variants (primary, secondary, outline, danger, ghost). 4 sizes with line-height/letter-spacing/height tokens.
- **`BaseCard.vue`**: Solid `var(--pq-surface)` fill, no backdrop-blur, no glow. 4 variants (default, elevated, accent, interactive).
- **`BaseBadge.vue`**: Solid 20% alpha bg, no border, radius `full`. 5 semantic variants (default, success, warning, danger, info).

### Layouts rewritten

- **`MainLayout.vue`**: Minimal header (hamburger + logo + avatar, no title, no theme toggle). Sidebar grouped into 3 sections (Operaciones/Análisis/Sistema). Active state = left border accent + bold text, no bg fill. No disabled items, no plan card, no upgrade CTA.
- **`LandingLayout.vue`**: Removed glassmorphism, mesh gradients, brand-glow. Uses `--pq-surface` for footer bg.
- **`LandingHero.vue`**: Split-asymmetric hero (text left, KPI strip right). KPI cards use `--pq-surface` fill. No centered-over-mesh layout.
- **`FeatureGrid.vue`**: Bento grid, no brand-glow on hover, plain surface cards.
- **`TrustSection.vue`**: Stat strip with Geist Mono large numbers, bronze accent.

### Bug fix

- **`proveedor.service.ts`, `producto.service.ts`, `factura.service.ts`**: Backend update endpoints require `@RequestParam UUID tenantId`. Added `{ params: { tenantId: data.tenantId } }` to all `update()` methods. Frontend was sending tenantId in body, backend expects query param.

### Documentation

- `.ulpi/` added to `.gitignore` — local-only design specs
- `.ulpi/design/DESIGN.md`: Locked design language (Swiss/grid)
- `.ulpi/design/main-layout.md`: MainLayout feature spec
- `.ulpi/design/landing-page.md`: Landing page feature spec
- `.ulpi/design/facturas.md`: Facturas feature spec
- `docs/TO_DO.md`: "Copper Luxe" section → "Swiss/Grid", updated token refs
- `frontend/pymes/docs/FUTURE.md`: Old plan section replaced with reference to DESIGN.md
- `frontend/pymes/docs/strategies/DESIGN_SYSTEM.md`: Full rewrite — Swiss/grid identity

### Files changed

| Archivo | Acción |
|---------|--------|
| `.gitignore` | `.ulpi/` added |
| `src/css/app.scss` | Legacy classes stripped, new `:root` tokens |
| `src/css/quasar.variables.scss` | Palette update |
| `src/design/tokens.ts` | Palette update |
| `src/components/base/BaseButton.vue` | Rewrite: flat solid colors |
| `src/components/base/BaseCard.vue` | Rewrite: solid surface, no glass |
| `src/components/base/BaseBadge.vue` | Rewrite: semantic colors |
| `src/layouts/MainLayout.vue` | Rewrite: Swiss sidebar, minimal header |
| `src/layouts/LandingLayout.vue` | Rewrite: no glassmorphism |
| `src/components/landing/LandingHero.vue` | Rewrite: split hero |
| `src/components/landing/FeatureGrid.vue` | Rewrite: no brand-glow |
| `src/components/landing/TrustSection.vue` | Rewrite: stat strip |
| `src/pages/IndexPage.vue` | `bg-forest-deep` removed |
| `src/modules/core/services/proveedor.service.ts` | tenantId query param |
| `src/modules/core/services/producto.service.ts` | tenantId query param |
| `src/modules/core/services/factura.service.ts` | tenantId query param |

---

### Fase 1: Corrección de bugs UX/UI (6 items)

**Fix UUID visible en formulario:**
- `ProductosPage.vue`: `categoryNameMap` y `unitNameMap` construidos desde setup para resolver UUID → nombre en template slots de las columnas Categoría y Unidad.
- `FacturasPage.vue`: `unitNameMap` resuelve base unit UUID → nombre en selector de items; `setupUnits` ref almacena las unidades del setup.

**Responsive dialog factura:**
- `FacturasPage.vue`: `col-3` → `col-xs-6 col-sm-3` en grid de inputs del form de factura.

**Compactar dialog:**
- Padding reducido en cards de dialogs (`14px 16px 12px` → más compacto).
- `standout` removido de todos los `q-input`.
- Layout más denso y funcional.

**Simplificar CategoryTabs:**
- `CategoryTabs.vue`: reescrito usando `q-chip` nativo con `selectable`, `active-class` y transiciones sutiles. Eliminados botones custom con HTML/CSS innecesario.

**No exponer UUIDs en dropdown:**
- Template del dropdown: solo muestra `productName` + badge proveedor. Campo `category` raw nunca visible para el usuario.

**ProductosPage pres-dialog responsive:**
- `col-4` → `col-xs-6 col-sm-4` en layout de presentaciones.

### Fase 2: Modernización PWA (8 items)

**Bottom nav mobile:**
- `MainLayout.vue`: nuevo `q-footer` con `q-tabs` 5 items usando `q-route-tab`. Visible solo `<600px`. Glass effect con `background: rgba($dark-surface, 0.95)`.
- Rutas: Home, Productos, Facturas, Gastos, Más (menú contextual).

**EmptyState en 6 páginas:**
- Nuevo componente `EmptyState.vue` reutilizable con props `icon`, `title`, `description`, `actionLabel`, `actionTo`.
- Implementado en: `ProductosPage`, `ProveedoresPage`, `GastosPage`, `VentasPage`, `PrestamosPage`, `FacturasPage`. Cada uno con contexto específico.

**Unsaved changes guard:**
- `ProductosPage.vue` y `FacturasPage.vue`: `beforeRouteLeave` navigation guard.
- `hasUnsavedChanges` computed que detecta campos modificados.
- Dialog de confirmación antes de salir si hay datos sin guardar.

**Keyboard shortcuts:**
- Nuevo composable `useKeyboardShortcuts.ts`.
- Atajos: `N` crear, `?` ayuda, `Esc` cerrar. `Ctrl+K` global search (placeholder).
- Help dialog muestra todos los shortcuts disponibles.

**Error message clarity:**
- `loadSetup()` error: "No se pudo cargar la configuración del negocio" + "Verificar conexión con el servidor".
- `loadDependencies()` errors en 4 páginas: mensajes específicos por contexto.

**KpiCard DRY:**
- `AccountingPage.vue`: reemplazado `summaryCards` array manual + template iterado por `KpiCard` importado.
- Eliminado template duplicado de 120+ líneas.

**Dialog animation:**
- `transition-show="slide-up"` + `transition-hide="slide-down"` en todos los dialogs CRUD (6 páginas).
- Efecto slide-up más nativo/app-like vs el fade anterior.

### Decisiones documentadas

- **SkeletonLoader skip:** Tablas con datos reales no necesitan skeleton. Se eliminaron del roadmap.
- **Stagger animation skip:** Ya existía `.stagger-children` en `app.scss` aplicado a KpiCard y AccountingPage. No duplicar.

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

### Files nuevos

```
src/composables/useKeyboardShortcuts.ts
src/components/ui/EmptyState.vue
```

### Files modificados

```
src/layouts/MainLayout.vue                  # +q-footer mobile nav + glass effect
src/modules/core/pages/ProductosPage.vue    # +categoryNameMap +unitNameMap +emptyState +unsavedGuard +shortcuts +dialog anim
src/modules/core/pages/ProveedoresPage.vue  # +emptyState +shortcuts +dialog anim
src/modules/core/pages/GastosPage.vue       # +emptyState +shortcuts +dialog anim
src/modules/core/pages/VentasPage.vue       # +emptyState +shortcuts +dialog anim
src/modules/core/pages/PrestamosPage.vue    # +emptyState +shortcuts +dialog anim
src/modules/core/pages/FacturasPage.vue     # +responsive cols +compact dialog +emptyState +unsavedGuard +shortcuts +unitNameMap +dialog anim
src/modules/core/pages/AccountingPage.vue   # -summaryCards template, +KpiCard import
src/modules/core/components/facturas/CategoryTabs.vue  # reescrito con q-chip
```

---

## 2026-07-15 — Auth pages redesign + tenantId bugfix

### Rediseno visual de paginas de auth

Posterior al redesign del Design System (Swiss/Grid), se aplicaron los mismos tokens a todas las paginas de autenticacion:

- **AuthLayout.vue**: logo solido con `var(--pq-accent)`, tagline con `var(--pq-text-muted)`, fondo `var(--pq-background)`. Accesibilidad por teclado (`role="button"`, `tabindex`, `keydown.enter/space`).
- **LoginPage.vue**: dos modos toggle. `oauth-primary` por defecto (boton Google hero + link a email). Al hacer clic en "Iniciar sesion con email y contrasena" cambia al formulario clasico con back link. Boton Google con estado de carga.
- **RegisterPage.vue**: mismo patron que LoginPage.
- **AuthCallback.vue**: eliminadas clases `bg-forest-deep`, `brand-glow`, `text-secondary`. Mensajes de estado rotativos cada 2s con `role="status"` y `aria-live="polite"`.
- **VerifyEmailPage.vue**: eliminado `brand-glow`. Agregado `BroadcastChannel` postMessage para sincronizar verificacion entre pestanas.
- **ForgotPasswordPage.vue, ResetPasswordPage.vue, AcceptInvitationPage.vue**: clases legacy (`brand-glow`, `bg-surface-pine`, `text-accent`) reemplazadas por CSS variables `var(--pq-text-muted)`, `var(--pq-text-subtle)`, `BaseCard` en vez de `q-card`.

### Bugfix: tenantId no persistia post-login

**Causa raiz:** El backend devuelve `user.tenantId=null` y `activeTenant.id` como campos separados (por diseno, `UserMapper` ignora `tenantId`). `authStore.login()` llamaba `setSession(data.accessToken, data.refreshToken, data.user)` sin mergear `activeTenant.id`. Al llegar al dashboard, los componentes leian `authStore.user.tenantId` = `undefined` y hacian peticiones como `/api/v1/core/proveedores?tenantId=` (vacio) → 500.

**Fix:** merge de `activeTenant.id` en user antes de `setSession()`, exactamente como ya lo hacia `verifyEmail()`:

```ts
const user = data.activeTenant
  ? { ...data.user, tenantId: data.activeTenant.id }
  : data.user;
this.setSession(data.accessToken, data.refreshToken, user);
```

Mismo fix aplicado a `register()`.

### Files modificados

- `layouts/AuthLayout.vue`
- `modules/auth/pages/LoginPage.vue`
- `modules/auth/pages/RegisterPage.vue`
- `modules/auth/pages/AuthCallback.vue`
- `modules/auth/pages/VerifyEmailPage.vue`
- `modules/auth/pages/ForgotPasswordPage.vue`
- `modules/auth/pages/ResetPasswordPage.vue`
- `modules/auth/pages/AcceptInvitationPage.vue`
- `modules/auth/store/index.ts` (tenantId merge en login/register)
- `App.vue` (BroadcastChannel refactor)

### Build

- lint: OK
- build: OK

---

## 2026-07-12 — Fix filteredByProvider + remoción minQuantity/maxQuantity + docs

### Fix filteredByProvider en FacturasPage

**Bug:** Al seleccionar un proveedor en el formulario de factura, el selector de productos mostraba productos de otros proveedores. El filtro `filteredByProvider` incluía productos sin proveedor (`!p.proveedorId || p.proveedorId === providerId`) mezclándolos con los del proveedor seleccionado.

**Fix:** `FacturasPage.vue:235-238` — removida la condición `!p.proveedorId ||`. Ahora solo muestra productos del proveedor seleccionado.

### Remoción minQuantity/maxQuantity de ProductosPage

**Motivo:** Los campos `minQuantity`/`maxQuantity` en el form de producto resultaron confusos en la UX. El análisis de stock con estas alertas puede resolverse de otra forma (pendiente).

**Cambios:**
- `ProductosPage.vue` — removidos 2 `<q-input>` del template (bloque de `minQuantity`/`maxQuantity`)
- `openCreate()` — eliminados `minQuantity: null, maxQuantity: null` del form reset
- `openEdit()` — eliminados `minQuantity: p.minQuantity, maxQuantity: p.maxQuantity` del form populate

### Documentación actualizada

- `docs/TO_DO.md` — 3 items nuevos en Frontend: descuento%, precioUnitario conversión, listas infinitas
- `docs/GAPS.md` — 3 gaps nuevos: búsqueda paginada no usada, descuento es monto fijo, precioUnitario no usa conversión
- `frontend/pymes/docs/FUTURE.md` — Pendiente conocido expandido + cambios en tabla de archivos existentes

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

### Files modificados

```
src/modules/core/pages/FacturasPage.vue       # filteredByProvider fix
src/modules/core/pages/ProductosPage.vue       # -minQuantity/maxQuantity
docs/TO_DO.md                                  # +3 items pendientes
docs/GAPS.md                                   # +3 gaps frontend
frontend/pymes/docs/FUTURE.md                  # pendiente conocido + tabla cambios
```

---

## 2026-07-12 — Módulos Core completos: 5 páginas CRUD + util + diseño

### Implementación de módulos core

**5 páginas CRUD nuevas** con service + types + ruta + sidebar habilitado:
- `GastosPage.vue` — CRUD gastos operativos (QTable + form con categorías: SALARIOS, AGUA, LUZ, etc.)
- `VentasPage.vue` — CRUD ventas diarias (fecha + monto bruto + descripción)
- `PrestamosPage.vue` — CRUD préstamos + pagos (dialog de pagos con historial)
- `PatrimonioPage.vue` — capital inicial (get-or-create + edición inline)
- `AccountingPage.vue` — métricas financieras consolidadas (6 cards + resumen)

**5 services creados:**
- `gasto.service.ts`, `venta.service.ts`, `prestamo.service.ts`, `patrimonio.service.ts`, `accounting.service.ts`

**Tipos agregados a `types/index.ts`:**
- `GastoOperativo`, `GastoRequest`, `VentaDiaria`, `VentaRequest`, `Prestamo`, `PagoPrestamo`, `PrestamoRequest`, `PagoPrestamoRequest`, `Patrimonio`, `PatrimonioRequest`, `MetricasFinancieras`, `PageResponse<T>`, `ProductOption.lastUnitPrice`

**Shared util:** `src/utils/format.ts` — `formatCurrency()` + `formatPct()` como singletones `Intl.NumberFormat`.

**Mejoras a páginas existentes:**
- `FacturasPage.vue`: auto-fill `precioUnitario` desde `lastUnitPrice` al seleccionar producto
- `ProductosPage.vue`: campos `minQuantity`/`maxQuantity` en form de producto
- `producto.service.ts`: nuevo método `search()` paginado
- `router/routes.ts`: +5 rutas nuevas
- `MainLayout.vue`: sidebar habilitado (5 items antes `disabled: true`)

### Vue best practices review

Aplicado skill `vue-best-practices`:
- `ref` → `shallowRef` para primitives en ProductosPage (loading, filter, pagination, dialog states)
- Import order corregido en FacturasPage y ProductosPage (useMeta después de imports)
- PrestamosPage pago dialog: removido `maximized` (contradecía `max-width: 600px`)
- Dialog widths: cambiado de `min-width: Npx` a `width: 90vw; max-width: Npx` (mobile-friendly)

### Quasar best practices review

Aplicado skill `quasar-skilld`:
- QForm validation: `formRef` + `await formRef.value?.validate()` antes de `save()` en 4 páginas CRUD
- Dialogs: glass morphism via `backdrop-filter: blur(16px)` en scoped `:deep`

### Frontend design polish

Aplicado skill `frontend-design`:
- **AccountingPage**: 6 metric cards estilo KpiCard (accent border, glass, hover lift, stagger animation)
- **PatrimonioPage**: 3 KPI cards estilo KpiCard + status badge pill + glass config card
- **Títulos de página**: mesh-text-gradient (copper-to-gold) en vez de flat `text-primary`
- **Skeleton loaders**: durante estados de loading en AccountingPage y PatrimonioPage
- **Summary card**: glass morphism + grid layout en AccountingPage

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

### Files nuevos

```
src/modules/core/pages/GastosPage.vue
src/modules/core/pages/VentasPage.vue
src/modules/core/pages/PrestamosPage.vue
src/modules/core/pages/PatrimonioPage.vue
src/modules/core/pages/AccountingPage.vue
src/modules/core/services/gasto.service.ts
src/modules/core/services/venta.service.ts
src/modules/core/services/prestamo.service.ts
src/modules/core/services/patrimonio.service.ts
src/modules/core/services/accounting.service.ts
src/utils/format.ts
```

### Files modificados

```
src/modules/core/types/index.ts
src/modules/core/services/producto.service.ts
src/modules/core/pages/FacturasPage.vue
src/modules/core/pages/ProductosPage.vue
src/modules/core/router/routes.ts
src/layouts/MainLayout.vue
```

---

## 2026-07-09 — SEO + A11y audit + visual polish refinements

### SEO refinements

- `index.html`: `robots` cambiado de `noindex, nofollow` a `index, follow`
- `lang` cambiado de `es` a `es-MX` (locale regional correcto)
- Open Graph completado: `og:site_name`, `og:locale` agregados
- Twitter Card: `twitter:card`, `twitter:title`, `twitter:description` agregados
- `apple-mobile-web-app-status-bar-style: black-translucent` agregado
- `viewport` sin bloqueo de zoom (`user-scalable=no` / `maximum-scale=1` eliminados)

### Accessibility (WCAG 2.2)

- **Skip links**: `<a href="#main-content" class="skip-link">` en `LandingLayout.vue` y `MainLayout.vue`
- **`aria-label`**: nav items, menu buttons, password toggles, table actions, KPI cards, form inputs, skeleton loaders
- **`role="alert"`**: offline banner en `MainLayout.vue`
- **`role="navigation"`**: sidebar menu en `MainLayout.vue`
- **`role="button"` + `tabindex="0"`**: `IndustryCard.vue`, `LoginPage.vue`, `RegisterPage.vue`, `ResetPasswordPage.vue`, password toggles
- **`role="tablist"` / `role="tab"` / `aria-selected`**: `CategoryTabs.vue`
- **`role="group"`**: `BaseCard.vue`, `InvoiceItemCard.vue`
- **`role="status"`**: `SkeletonLoader.vue`, `CatalogDashboard.vue`, `OnboardingPage.vue`
- **`aria-busy`**: `BaseButton.vue` loading state
- **`aria-expanded`**: `CatalogDashboard.vue` tree rows
- **`aria-hidden="true"`**: `FeatureGrid.vue` decorative icons
- **`aria-haspopup="menu"`**: `MainLayout.vue` user menu
- **`id="main-content"` + `tabindex="-1"`**: `LandingLayout.vue`, `MainLayout.vue`, `App.vue` — focus target para skip links
- **`prefers-reduced-motion`**: override global en `app.scss` — desactiva animaciones y transiciones
- **`.focus-ring`**: clase CSS con `focus-visible` outline para campos de formulario auth
- **`.visually-hidden`**: utilidad screen-reader-only

### Visual polish (app.scss)

- **Glassmorphism**: `.glass` con `backdrop-blur` + pseudo-elemento SVG noise texture
- **Brand glow**: mixin `brand-glow` con `box-shadow` + intensificación en hover
- **Mesh gradient text**: `.mesh-text-gradient` para marca
- **Fade-in-up**: keyframes `fadeInUp` + `.fade-in-up` + `.stagger-children` (stagger en 10 hijos)
- **Shimmer**: skeleton loader con `shimmer` keyframes
- **Press feedback**: `.press-feedback` con `scale(0.96)` en `:active`
- **Shadow system**: `.tight-shadow`, `.shadow-subtle`, `.shadow-md`, `.shadow-lg`
- **Hover effects**: `.hover-lift` (translateY + shadow), `.hover-scale` (scale 1.02)
- **Interactive states**: `.interactive` con cursor, hover bg, active scale
- **Auth form fields**: deep selectors para Quasar field hover/focus glow

### Pendiente conocido

- `og:image` / `twitter:image` — social previews sin imagen
- `manifest.json` dice "Auditoria Inteligente" pero `index.html` dice "Gestion Financiera" — brand drift menor
- Sin `<meta name="description">` estática (depende de template variable)
- Sin structured data (JSON-LD)
- Sin `<noscript>` fallback

---

## 2026-07-08 — Supplier analytics UI: 3 componentes + optimización + invoice fixes

### Supplier analytics

- `types/analytics.ts`: +`SupplierComparisonItem`, `SupplierRecommendation`, `PricePredictionItem`, +`supplierComparison`, `supplierRecommendations`, `pricePredictions` en `AnalyticsResponse`
- `composables/useAnalytics.ts`: +3 computed slices (`supplierComparison`, `supplierRecommendations`, `pricePredictions`)
- `SupplierComparisonTable.vue`: QTable — avg/min/max price por producto-proveedor con % diff
- `SupplierRecommendationsCard.vue`: QCard — proveedor más barato por producto + savings_pct
- `PricePredictionsTable.vue`: QTable — precio actual vs predicho + R²
- `AnalisisGastosPage.vue`: 3 nuevas secciones (supplier analytics) + filtro all-time en período

### Optimization

- `shallowRef` aplicado en: `ProveedoresPage.vue`, `FacturasPage.vue`, `AnalisisGastosPage.vue`

### InvoiceItemCard fix

- `InvoiceItemCard.vue`: restaurados `search` ref, `filteredProducts` computed, `filterProducts` fn, `:options="filteredProducts"`; template simplificado — solo `productName` + badge `proveedorName`
- `FacturasPage.vue`: label en `loadDependencies` sin SKU — `${p.name}${p.proveedorName ? ' · ' + p.proveedorName : ''}`

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

### Files

```
types/analytics.ts
composables/useAnalytics.ts
components/analytics/SupplierComparisonTable.vue
components/analytics/SupplierRecommendationsCard.vue
components/analytics/PricePredictionsTable.vue
pages/AnalisisGastosPage.vue
pages/ProveedoresPage.vue
pages/FacturasPage.vue
components/facturas/InvoiceItemCard.vue
```

---

## 2026-07-06 — Fix UUID display en catálogo/productos/gastos + Invoice detail dialog

### Fix UUIDs visibles

- `CatalogDashboard.vue`: `categoryNameMap` aplanado del árbol resuelve UUID → nombre en search results.
- `ProductosPage.vue`: `categoryNameMap` + `unitNameMap` con template slots para columnas Categoría y Unidad.
- `AnalisisGastosPage.vue`: carga setup, `categoryNameMap` agrupa por nombre en vez de UUID.
- `FacturasPage.vue`: `unitNameMap` resuelve base unit UUID → nombre en selector de items; `setupUnits` ref almacena las unidades del setup.

### Invoice detail dialog

- `InvoiceDetailDialog.vue` (nuevo): muestra información completa de factura (proveedor, fecha, tipo friendly vía `tipoLabel`, estado, método de pago, items con Unidad resuelta vía `presentationNameMap`, descuento global, total).
- `FacturasPage.vue`: botón 👁️ en acciones + `detailDialog`/`detailItem` state + `presentationNameMap` construido desde `Producto.presentaciones`.

### Type fixes

- `Factura.tipo` → `Factura.type` (coincide con JSON del backend `FacturaResponse.type`).
- `ItemFactura` +`presentacionId: string | null` + `conversionFactor: number`.

### Archivos nuevos

```
src/modules/core/components/facturas/InvoiceDetailDialog.vue
```

### Archivos modificados

```
src/modules/core/types/index.ts
src/modules/core/components/dashboard/CatalogDashboard.vue
src/modules/core/pages/ProductosPage.vue
src/modules/core/pages/AnalisisGastosPage.vue
src/modules/core/pages/FacturasPage.vue
src/modules/core/components/facturas/InvoiceDetailDialog.vue
```

---

## 2026-07-05 — Onboarding preview dashboard + invoice spin buttons hidden

### Qué se hizo

**Simulador de Panel de Control en Onboarding** (interactive preview):
- Reemplazada la presentación estática de vista previa de plantilla en `OnboardingPage.vue` con un simulador interactivo de panel de control dividido (Split Layout) usando `q-tabs` y `q-tab-panels` nativos de Quasar.
- Agregada búsqueda reactiva local para los productos base de la plantilla de industria.
- Añadidos KPIs (Productos, Categorías, Unidades, Ubicaciones) y flujo de tareas automatizado.
- Contenedor con transición de ancho dinámico (`max-width: 1100px`) durante el Paso 2.
- Rediseñado `CategoryTree.vue` con líneas guía (`dashed`), iconos y desplazamiento lateral (`transform: translateX`) al pasar el ratón (`hover`).

**Ocultación de Spin Buttons en Facturas**:
- Añadido selector CSS `:deep(input[type="number"])` en `InvoiceItemCard.vue` para ocultar las flechas incrementales y decrementales en los campos de cantidad, precio unitario y descuento.

### Archivos modificados

```
src/components/onboarding/CategoryTree.vue
src/modules/core/pages/OnboardingPage.vue
src/modules/core/components/facturas/InvoiceItemCard.vue
```

---

## 2026-07-01 — Component split + accessibility + SEO + visual polish

### Qué se hizo

**Component split** (vue-best-practices):
- `FacturasPage.vue` (568 líneas) → 3 componentes extraídos:
  - `InvoiceItemCard.vue` — card por item (producto, cantidad, unidad, precio, descuento, subtotal)
  - `CategoryTabs.vue` — tabs de categoría con `role="tablist"` + `aria-selected`
  - `ConfirmDialog.vue` — diálogo reutilizable para pagar/eliminar
- `ProductOption` type agregado a `types/index.ts`

**Accessibility** (WCAG 2.2):
- `aria-label` en 9+ botones de icono (close, edit, delete, paid, layers)
- `role="group"` + `aria-label` en invoice item cards
- `role="tablist"` + `aria-selected` en category tabs
- `prefers-reduced-motion` ya existía en `app.scss`

**SEO** (`index.html`):
- `<html lang="es">` ✅
- `<meta name="robots" content="noindex, nofollow">` (SaaS autenticado)
- `<meta name="apple-mobile-web-app-capable">` para PWA
- OG tags existentes

**Visual polish** (frontend-design):
- Category cards: gradient bg, copper accent line, hover glow, staggered cardReveal animation, pill count badges
- Invoice item cards: gradient bg, focus-within glow, remove button opacity transition
- Category tabs: border transition en hover

### Archivos nuevos

```
src/modules/core/components/facturas/InvoiceItemCard.vue
src/modules/core/components/facturas/CategoryTabs.vue
src/modules/core/components/facturas/ConfirmDialog.vue
```

### Archivos modificados

```
src/modules/core/pages/FacturasPage.vue          # refactor: usa 3 componentes extraídos
src/modules/core/types/index.ts                  # +ProductOption interface
index.html                                       # +robots, apple-mobile-web-app meta
```

---

## 2026-07-01 — SKU automático + proveedor fix + unidades en items + preview cards

### Qué se hizo

**SKU automático en creación de productos**:
- Input de SKU eliminado del formulario de ProductosPage
- Backend genera `P-XXXX` secuencial cuando sku es null/blank
- `ProductoRequest.sku` ahora es opcional

**Categoría requerida en productos**:
- `:rules="[v => !!v || 'Requerido']"` agregado al q-select de categoría

**Fix proveedor UUID visible**:
- Después de crear proveedor inline, `providerFilteredOptions` se actualiza con el nuevo option
- El q-select ahora resuelve correctamente el label del UUID

**Unidades de medida en items de factura**:
- `ItemForm` ahora incluye `presentacionId: string | null`
- `productPresentationsMap` construido desde `Producto.presentaciones` + base unit
- `q-select` de unidad por item: muestra base unit + presentaciones del producto
- `presentacionId` se resetea al cambiar de producto
- `presentacionId` se envía en el payload (opcional en backend)

**Preview de onboarding rediseñado**:
- Antes: chips de productos agrupados por categoría
- Después: grid de cards de categoría con icono, nombre y count de productos

### Archivos modificados

```
src/modules/core/types/index.ts                      # ProductoRequest.sku → opcional, ItemFacturaRequest +presentacionId
src/modules/core/pages/ProductosPage.vue              # -SKU input, +category rules
src/modules/core/pages/FacturasPage.vue               # +unit selector, +providerFilteredOptions fix, +productPresentationsMap
src/modules/core/pages/OnboardingPage.vue             # preview → category cards grid
```

---

## 2026-07-01 — Dashboard reemplazado por Catálogo + proveedor inline + items UX + USD

### Qué se hizo

**Dashboard**: reemplazo de `AnalyticsDashboard` por `CatalogDashboard` en la ruta raíz.
- Árbol de categorías colapsable con productos agrupados (jerarquía `SetupCategory`)
- 4 KPIs: Productos, Categorías, Proveedores, Inversión Total
- Search bar que filtra productos por nombre/SKU y los muestra planos
- Estados: loading (skeleton shimmer), error con retry, empty con CTA
- Los análisis quedaron en la ruta `/analisis-gastos`

**Proveedor inline en facturas**: type-to-create desde el `q-select` de proveedor.
- Al tipear un nombre sin coincidencia aparece `+ Crear "Nombre"`
- Al seleccionarlo → `POST /proveedores` → asigna el ID al form
- No requiere diálogo separado

**Items del formulario de factura**: rediseño completo del layout.
- Antes: 6 columnas en una fila (`Producto col-4`, `Cant. col-2`, `P.Unit. col-2`, `Desc. col-2`, subtotal `col-1`, ✕ `col-1`)
- Después: cada item es un bloque card de 2 filas con fondo sutil y borde
  - Fila 1: Producto (col-10) + ✕ (col-2)
  - Fila 2: Cantidad (col-4) + Precio Unit. (col-3) + Descuento (col-3) + Subtotal (col-2)
- Labels completos: `Cant.` → `Cantidad`, `P.Unit.` → `Precio Unit.`, `Desc.` → `Descuento`
- `baseUnit` del producto se muestra bajo el input de cantidad al seleccionar producto
- Subtotal con label propio y formato USD bold
- Inputs de precio/descuento con prefijo `$`
- Botón "Agregar item" pasa a `outline`

**Moneda**: todo el frontend cambió de PEN/PYG a USD.
- `useNumberFormat.ts`: locale `en-US`, currency `USD`
- `FacturasPage.vue`: `formatCurrency` propio cambió de `es-PY`/`PYG` a `en-US`/`USD`

### Archivos nuevos

```
src/modules/core/components/dashboard/CatalogDashboard.vue
```

### Archivos modificados

```
src/modules/core/pages/DashboardPage.vue               # AnalyticsDashboard → CatalogDashboard
src/modules/core/pages/FacturasPage.vue                 # proveedor inline + items redesign + USD + unit hints
src/modules/core/composables/useNumberFormat.ts          # PEN→USD
```

---

## 2026-06-30 — Template Products: Frontend preview en onboarding completado

### Qué se hizo

La sección "Productos precargados (N)" en el paso 2 del onboarding, planificada el 2026-06-29, se implementó:

- **`types/index.ts`** — agregado `ProductTemplateDTO { id, name, baseUnit, categoryName }` + campo `products` en `SetupInfo`
- **`OnboardingPage.vue`** — en step 2, después del árbol de categorías, se agrega sección "Productos precargados (25)" con tabla de nombre, unidad y categoría
- Backend devuelve los productos en preview y post-onboarding

### Flujo UX actualizado

```
Onboarding → Paso 1: industria → Paso 2: preview árbol + productos → "Comenzar" → POST → /dashboard
```

### Files modificados

```
frontend/pymes/src/modules/core/types/index.ts       # +ProductTemplateDTO
frontend/pymes/src/modules/core/pages/OnboardingPage.vue  # +sección productos
```

---

## 2026-06-29 — Plan: Template Products para Onboarding

### Problema
El onboarding carga categorías, unidades y ubicaciones, pero no productos. El usuario debe crear productos uno por uno después del onboarding. La idea es que al completar onboarding ya haya un catálogo genérico precargado con SKU y unidad, listo para facturar.

### Plan (3 fases)

**Fase 1: Backend — Tabla y Seed**
- Flyway V7: `template_products` (industry_code, category_id, name, sku, base_unit, sort_order)
- Flyway V8: `template_product_presentations` (template_product_id, name, conversion)
- SeedDataRunner: `seedXxxProducts()` para cada industria (~30-50 productos genéricos, ~2 presentaciones c/u)

**Fase 2: Backend — Onboarding copia productos**
- `SetupServiceImpl.completeOnboarding()` → copia `template_products` → `core.products` + `core.product_presentations`
- `SetupResponse` → nuevo campo `products: List<ProductTemplateDTO>`
- `loadIndustryData()` → extender para query de productos

**Fase 3: Frontend**
- `OnboardingPage.vue` step 2 → sección "Productos" con tabla resumen
- `SetupInfo` type → agregar `products: ProductTemplate[]`

### Files a modificar
```
backend/core/src/main/resources/db/migration/V7__template_products.sql       # nuevo
backend/core/src/main/resources/db/migration/V8__template_product_presentations.sql  # nuevo
backend/core/src/main/java/core_pymes/common/seed/SeedDataRunner.java        # +seed products
backend/core/src/main/java/core_pymes/setup/service/impl/SetupServiceImpl.java  # +copy on onboarding
backend/core/src/main/java/core_pymes/setup/dto/SetupResponse.java           # +products field
frontend/pymes/src/modules/core/pages/OnboardingPage.vue                     # +product preview
frontend/pymes/src/modules/core/types/index.ts                               # +ProductTemplate type
```

### Escala
- ~30-50 productos × 8 industrias = ~240-400 inserts
- ~2 presentaciones × ~300 productos = ~600-1200 inserts
- Total: ~1000-1600 inserts en SeedDataRunner

### Referencia
- `SEED_TEMPLATES.md` §Plantillas de Productos (schema + detalle)
- `CORE.md` §Seed Data + §Pendientes

---

## 2026-06-26 — PWA offline + Redis cache

### Offline / PWA

- Worker Service: `StaleWhileRevalidate` para GETs de API (`/api/v1/core/*`), off‑line banner en `MainLayout.vue`, diálogo de actualización disponible con `SKIP_WAITING` → recarga (`custom-service-worker.ts`, `register-service-worker.ts`, `MainLayout.vue`)
- `MainLayout.vue`: estado `online` con listener de eventos, `q-banner` amarillo compacto con ícono `wifi-off`, captura `sw-update-ready` → `$q.dialog`
- `register-service-worker.ts`: dispacha eventos DOM personalizados `sw-update‑ready`, `sw-update‑found`
- Redis + caché en back‑end para perf de productos/proveedores/facturas

### Backend (Core)

- Refactoring:
  - Eliminado `FacturaPagadaEvent` — evento sin listener
  - Actualizado entidades JSONB `AnalisisGasto` → `@JdbcTypeCode(SqlTypes.JSON)`
  - Agregado tests (`AnalyticsServiceImplTest`, `AnalyticsRepositoryTest`)
- Redis: `CacheConfig.java` `@EnableCaching` + `RedisCacheManager` (TTL 5min), `@Cacheable` en `findAll`/`findById` → `ProductoServiceImpl.java`, `FacturaServiceImpl.java` + `@CacheEvict` en writes
- Todas las entidades definidas usando Java Records (`ItemDTO`)

### 2026-06-24 — Onboarding 2 pasos: preview de categorías/subcategorías

### Contexto

El onboarding actual muestra 8 cards de industria y al hacer clic llama `POST /onboarding`. El usuario no sabe qué categorías cargará antes de confirmar. Se necesita un preview de las categorías jerárquicas (categorías → subcategorías → ítems) antes de que el usuario confirme.

### Plan

1. **`CategoryTree.vue`** — Componente que recibe un array de categorías con estructura `{ code, name, children[] }` y renderiza el árbol visual con indentación. Solo lectura (sin checkbox ni edición).

2. **`OnboardingPage.vue`** — Flujo de 2 pasos con `step` ref:
   - Paso 1: Selección de industria (cards existentes)
   - Paso 2: Preview del árbol de categorías usando `CategoryTree` + botón "Comenzar"

3. **`setup.service.ts`** — Nuevo método `preview(industry: string)` que llama `GET /setup/preview/{industry}`.

4. **`types/index.ts`** — Actualizar `SetupInfo.categories` con `parentId?: string` y `children?: SetupInfo['categories']`.

### Arquitectura del componente

```
CategoryTree.vue
├── Props: categories (array con children nested)
├── Render recursivo: <CategoryNode> que se llama a sí mismo
├── Estilo: borde izquierdo copper (#A3785E), padding indentado
└── Solo lectura — sin emits ni interacción
```

### Flujo UX

```
Onboarding → Paso 1: industria → Paso 2: preview árbol → "Comenzar" → POST → /dashboard
```

### Files a crear/modificar

```
frontend/pymes/src/components/onboarding/CategoryTree.vue      # nuevo
frontend/pymes/src/modules/core/pages/OnboardingPage.vue       # flujo 2 pasos
frontend/pymes/src/modules/core/services/setup.service.ts      # +preview()
frontend/pymes/src/modules/core/types/index.ts                 # +parentId, +children
```

---

## 2026-06-24 — Onboarding auto-redirect post verifyEmail + ProductosPage template options

### Problemas

1. **Local registration saltaba onboarding**: Tras verificar email, `VerifyEmailPage.vue` redirigia a `/dashboard` sin pasar por onboarding. OAuth2 si lo hacía via `AuthCallback.vue`.
2. **`authStore.user?.tenantId` undefined**: `UserMapper` en backend ignora `tenantId`, y el store no leía `activeTenant` del `AuthResponse`.
3. **ProductosPage campos libres**: category y `baseUnit` eran `<q-input>` en vez de `<q-select>` con opciones del template.

### Soluciones

| # | Fix | Archivo |
|---|-----|---------|
| 1 | `VerifyEmailPage.vue`: después de verify exitoso, redirige a `/onboarding` via `setupService.get(tenantId)` | `VerifyEmailPage.vue` |
| 2 | `auth store`: mergea `authData.activeTenant.id` en user antes de `setSession` | `store/index.ts` |
| 2b | `types/index.ts`: agregado `activeTenant?: { id, name, slug }` a `AuthResponse` | `types/index.ts` |
| 3 | `ProductosPage.vue`: category y baseUnit como `<q-select>` con opciones de template (filterable) | `ProductosPage.vue` |
| 3b | `setup.service.ts`: `completeOnboarding` return type cambiado a `SetupInfo` | `setup.service.ts` |

### Arquivos tocados

`VerifyEmailPage.vue`, `store/index.ts`, `types/index.ts`, `ProductosPage.vue`, `setup.service.ts`

---

## 2026-06-23 — Fix OAuth2 callback + QPage standalone + hash redirect

### Problemas
1. **OAuth2 redirect sin hash**: Backend redirigia a `/auth/callback?code=xxx` en vez de `/#/auth/callback?code=xxx`. Vue Router en hash mode ignora el path → caia a IndexPage → redirect a login.
2. **Code en query param no en hash**: `/#/auth/callback?code=xxx` se construia con `UriComponentsBuilder.queryParam()` que pone el `?code=` antes del `#` → browsers strippean fragmento de 302 redirects.
3. **AuthCallback `<q-page>` sin `<q-layout>`**: Quasar requiere `<q-layout>` ancestro. AuthCallback era ruta standalone sin layout → runtime error.
4. **OnboardingPage mismo error**: `<q-page>` sin `<q-layout>`.
5. **`/auth/me` no existe**: `auth.service.ts` llamaba a `/auth/me` pero el endpoint real es `/users/me`.

### Soluciones
| # | Fix | Archivo |
|---|-----|---------|
| 1 | Backend redirige a `frontendUrl + "/#/auth/callback?code=" + code` (code dentro del hash) | `OAuth2AuthenticationSuccessHandler.java:170` (backend) |
| 2 | AuthCallback lee code de `route.query.code` con fallback a `window.location.search` | `AuthCallback.vue:30` |
| 3 | `<q-page>` reemplazado por `<div>` con `min-height: 100vh` en AuthCallback | `AuthCallback.vue:2` |
| 4 | `<q-page>` reemplazado por `<div>` con `min-height: 100vh` en OnboardingPage | `OnboardingPage.vue:44` |
| 5 | `auth.service.ts` cambiado de `/auth/me` a `/users/me` | `auth.service.ts:18` |

### Lecciones
- **Hash mode + redirects**: Vue Router en hash mode usa `location.hash`. Redirects desde el backend deben poner TODO (path + query) dentro del `#/...`.
- **`<q-page>` no es standalone**: Siempre necesita un `<q-layout>` ancestro. Para paginas sin layout usar `<div>` con `min-height: 100vh`.
- **Service Worker cache**: Tras cambios al frontend, el service worker de la PWA puede servir versiones viejas. Usar Ctrl+F5 o `caches.keys().then(keys => keys.forEach(k => caches.delete(k)))` en DevTools.

**Archivos modificados:** `AuthCallback.vue`, `OnboardingPage.vue`, `auth.service.ts`
**Estado:** ✅ RESUELTO

---

## 2026-07-22 — ❌ FAILED: Invitation AcceptPage Register Flow

### Objetivo

`AcceptInvitationPage.vue` debía permitir registro + aceptación en un solo paso, usando un endpoint separado en vez de contaminar el flujo normal de registro.

### Cambios hechos

- `types/index.ts`: `RegisterRequest` sin `invitationToken?`, +`InvitationRegisterRequest` type
- `invitation.service.ts`: +`registerAndAccept(token, data)` → `POST /api/v1/invitations/{token}/register`
- `AcceptInvitationPage.vue`: `onRegister()` usa `invitationService.registerAndAccept()` + `authStore.setSession()`
- `store/index.ts`: `setSession()` se usaba directamente con la respuesta del backend

### Por qué se abandonó

El backend cambió demasiadas cosas para soportar esto (RegisterRequest, AuthServiceImpl, tests). El approach era correcto (endpoint separado) pero la ejecución fue desastrosa y contaminó código que funcionaba. Se revirtió todo al commit `3354165`.

### Ramas preservadas

- `refactor/invitation-attempt` — commit `956584c` contiene todos los cambios

**Estado:** ❌ ABANDONADO — archivado en `refactor/invitation-attempt`

---

## 2026-06-23 — Onboarding Post-Login (Selección de Industria)

### Problema

Después de OAuth2 con Google, el usuario cae al `/dashboard` sin industria configurada. El backend tiene `POST /core/setup/{tenantId}/onboarding` pero el frontend nunca lo llamaba.

### Solución

Página `/onboarding` + router guard que redirige si `onboardingCompleted=false`.

### Archivos creados

- `src/modules/core/services/setup.service.ts` — `GET /core/setup/{tenantId}` + `POST /core/setup/{tenantId}/onboarding`
- `src/modules/core/pages/OnboardingPage.vue` — 8 cards de industria (restaurante, bares, salon_belleza, ferreteria, mini_super, taller_mecanico, farmacia, default)

### Archivos modificados

- `src/modules/core/router/routes.ts` — exporta `onboardingRoute`
- `src/router/routes.ts` — `/onboarding` como ruta standalone (fuera de `/dashboard`)
- `src/modules/auth/pages/AuthCallback.vue` — después del exchange, llama `GET /core/setup/{tenantId}` y redirige a `/onboarding` si `onboardingCompleted=false`

### Flujo

```
OAuth2 → AuthCallback → handleOAuthCallback() → fetchCurrentUser()
→ GET /core/setup/{tenantId}
  → onboardingCompleted=false → /onboarding → seleccionar industria → /dashboard
  → onboardingCompleted=true → /dashboard
```

### Gateway

Ambas rutas `/api/v1/core/setup/**` pasan con JWT (confirmado en `RouterValidator` + `AuthenticationFilter`).

---

## 2026-06-23 — Módulo Core: Productos, Proveedores, Facturas, Configuración

### Qué se hizo

Módulo `src/modules/core/` completo con 4 páginas CRUD, rutas y servicios API.

### Estructura

```
src/modules/core/
├── types/index.ts          # DTOs: Producto, Presentacion, Proveedor, Factura, ItemFactura, SetupInfo
├── services/
│   ├── producto.service.ts  # CRUD productos + presentaciones
│   ├── proveedor.service.ts # CRUD proveedores
│   └── factura.service.ts   # CRUD facturas + pagar
├── pages/
│   ├── ProductosPage.vue    # QTable + CRUD + dialog presentaciones
│   ├── ProveedoresPage.vue  # QTable + CRUD
│   ├── FacturasPage.vue     # QTable + CRUD + pagar + filtros por estado/proveedor/fecha
│   └── ConfiguracionPage.vue # Vista-only: categorías, unidades, ubicaciones, motivos, métodos pago
└── router/routes.ts         # /core/productos, /core/proveedores, /core/facturas, /core/configuracion
```

### Integración

- Router: `coreRoutes` importado en `src/router/routes.ts`, merged en dashboard children
- Sidebar: MainLayout linksList actualizado con Productos, Proveedores, Facturas + separadores
- TenantId: derivado de `authStore.user?.tenantId`
- API: servicios apuntan a `/core/...` via `api` instance de `boot/axios` (baseURL `http://localhost:8080/api/v1`)

### Stack usado

- `<script setup lang="ts">` Composition API
- QTable + QInput + QBtn + QDialog + QForm + QSelect + QChip (Quasar)
- `shallowRef` para state que no necesita deep reactivity
- `computed` para datos derivados
- Dark theme: `dark` prop en QTable/QDialog/QInput, `bg-surface-pine` en cards

### Archivos tocados

```
src/modules/core/types/index.ts          (nuevo)
src/modules/core/services/*.service.ts   (3 archivos nuevos)
src/modules/core/pages/*.vue             (4 archivos nuevos)
src/modules/core/router/routes.ts        (nuevo)
src/router/routes.ts                     (+coreRoutes import)
src/layouts/MainLayout.vue               (+sidebar links, +separadores)
```

### Pendiente

- FacturasPage.vue y ConfiguracionPage.vue: reorder SFC a `<script>` → `<template>` → `<style>`
- MainLayout: reemplazar `Math.random()` como key en v-for del sidebar

---

## 2026-06-19 — Seguridad OAuth2 y replaceState

### OAuth2 Code Exchange

**Problema:** `AuthCallback.vue` recibía JWT directamente en la URL (`?token=xxx&refresh_token=yyy`), exponiéndolos en el historial del navegador, URL bar y header Referer.

**Solución:** El backend ahora emite un código de un solo uso (`?code=xxx`). El frontend lo canjea via `POST /api/v1/auth/exchange`. JWT nunca toca la URL.

**Impacto en UX:** Ninguno — flujo transparente para el usuario.

---

### replaceState para limpiar tokens en hash routing

Problema: Tres páginas recibían tokens sensibles por query param dentro del hash. En hash routing, `window.location.hash` incluye los params (`#/ruta?param=valor`), así que `window.history.replaceState` estándar no los borraba.

Fix: `hash.replace(/\?.*$/, '')` recorta el query string manteniendo el path del hash.

| Página | Tokens en URL | replaceState |
|--------|--------------|--------------|
| `AuthCallback.vue` | `?code=...` | ✅ limpiado |
| `VerifyEmailPage.vue` | `?token=...&email=...` | ✅ limpiado |
| `ResetPasswordPage.vue` | `?token=...&email=...` | ✅ limpiado |
| `AcceptInvitationPage.vue` | `?token=...` | ❌ intencional — necesita token post-login redirect |

---

## 2026-06-16 — SEO y Accesibilidad (WCAG 2.1 AA)

Implementados en una sola sesión de trabajo. Todos los ítems completados:

**Viewport / HTML base:**
- `user-scalable=no` eliminado (requisito WCAG + penalización Google)
- `<html lang="es">`, `theme-color: #0B1210`

**Landmarks:**
- `<main>` en 3 layouts; `<nav aria-label>` en sidebar y landing; `<footer>` en AuthLayout

**ARIA:**
- `aria-label` en hamburger, avatar dropdown, landing input
- `aria-hidden` en BrandSplash y BaseSkeleton
- `aria-busy` en BaseButton loading y SkeletonLoader
- `aria-live` announcer para mensajes dinámicos
- `role="group"` en BaseCard; `aria-disabled` en footer buttons

**Teclado:**
- Password toggles: `role="button"`, `tabindex="0"`, `@keydown.enter/space`
- Login envuelto en `<q-form>` con `type="submit"`
- Focus management: `<main>` recibe foco después de cada cambio de ruta

**Contraste:**
- `$accent` elevado de `#71837F` (4.2:1, falla AA) a `#8A9E99` (5.5:1, pasa AA)

**SEO:**
- `useMeta()` en todas las páginas con `titleTemplate`
- Open Graph: `og:title`, `og:description`, `og:type`

---

## 2026-06-16 — Estado del Diseño Consolidado (PYMEQ)

**Identidad:** SaaS Fintech "PYMEQ". Flujo "Empresa Primero" — registro comienza con nombre de empresa, slug generado automáticamente.

**Layout:** Todas las páginas de auth bajo `AuthLayout.vue`. Dashboard split en `DashboardStats` + `DashboardActionCard` + `RecentActivity`. Landing split en `LandingHero` + `FeatureGrid` + `TrustSection`.

**Scaffolding eliminado:** `EssentialLink.vue`, `ExampleComponent.vue`, `models.ts`, `example-store.ts`.

**Composables creados:** `useAuthForm`, `useLogout`, `useScrollReveal`.

**Build:** 385.71 KB JS, 32 chunks, 0 errores lint.

---

## 2026-05-08 — Bloqueadores Sass / Vite / Docker

### Sass: Colisión de nombres con tokens de Quasar

**Problema:** `$map: 12px is not a map` — colisión entre tokens locales (`$space-xs`) y funciones internas de Quasar.

**Solución:** Prefijado de todos los tokens con `pq-` (`$pq-space-xs`, etc.) en `quasar.variables.scss` y `app.scss`.

### Pinning de versiones

**Problema:** Vite 8 + Quasar 2.19 → bugs en generación de Service Workers PWA.

**Solución:** Fijado exacto en `package.json`:
- **Vite:** `7.x`
- **Quasar:** `2.18`
- **Sass:** `1.32.12`

Sass legacy API forzada en `quasar.config.ts` con cast TypeScript para compatibilidad con Vite 7.

### Docker

**Problema:** `npm ci` fallaba por discrepancias de versiones en el lockfile tras el pinning.

**Solución:** `Dockerfile` usa `npm install --legacy-peer-deps`.

---

## 2026-05-04 — Flujo de Auth Unificado

**Company First:** Home es el único punto de entrada. Slug generado automáticamente, invisible para el usuario.

**OAuth2 Intent:** Login con Google respeta la empresa creada en el paso previo (state parameter `intentId` sincronizado con el backend).

**Recordar sesión:** Persistencia de email en `localStorage` en login local.

**Eliminado:** Facebook OAuth — descartado de esta fase, solo Google.

---

## 2026-04-28 — Fix de Seguridad: token-email mismatch en verify-email

**Vulnerabilidad:** `authService.verifyEmail()` enviaba solo `{ token }` ignorando el email del query param. Cualquier token válido podía verificar cualquier cuenta.

**Fix:**
```typescript
// auth.service.ts — antes (vulnerable)
async verifyEmail(token: string) {
  return api.post('/auth/verify-email', { token });
}

// auth.service.ts — después (corregido)
async verifyEmail(token: string, email: string) {
  return api.post('/auth/verify-email', { token, email });
}
```

`VerifyEmailPage.vue` actualizada para extraer email del query param y enviarlo junto al token.

Estado: ✅ RESUELTO

---

## 2026-05-03 — Fix: Login no navegaba (Layout anidado)

**Problema:** `LoginPage.vue` estaba en `children` de `LandingLayout`. Ambos usaban `<q-layout view="lHh Lpr lFf">` → layouts anidados en conflicto.

**Solución:** Extraer `/login` y rutas de auth del nested children, hacerlas rutas independientes bajo `AuthLayout`.

Estado: ✅ RESUELTO

---

## 2026-05-03 — Componentes Base y Design System

Creados en una sesión:

| Componente | Props clave |
|------------|-------------|
| `BaseButton.vue` | `variant` (primary/secondary/ghost/danger/success), `size`, `loading`, `disabled`, `iconLeft/Right` |
| `BaseCard.vue` | `variant` (default/elevated/outlined/ghost), `padding` |
| `BaseSkeleton.vue` | `variant` (text/circle/rectangle/card), `size`, `width`, `height` |
| `SkeletonLoader.vue` | `isLoading`, `layout` (card/form/stats/list), `count` |

CSS tokens en `quasar.variables.scss` (prefijo `pq-`): spacing 8px system, border radius, shadows, transitions, z-index.

Clases en `app.scss`: `.glass`, `.glass-light`, `.brand-glow`, `.fade-in-up`, `.stagger-children`, `.skeleton`, `.hover-lift`, `.hover-scale`, router transitions.

PWA manifest actualizado: nombre "PYMEQ - Auditoría Inteligente", `theme_color: #A3785E`, `background_color: #0B1210`, shortcuts a Dashboard y Login.

---

---

## 2026-06-30 — Análisis de Gastos: Nueva página dashboard + tipos actualizados

### Qué se hizo

**Nueva página: `AnalisisGastosPage.vue`**
- Ruta `/dashboard/analisis-gastos`
- 4 cards resumen: Inversión Total, Productos, Categorías, Alertas
- Inversión por Categoría — agrupación client-side con `q-linear-progress`
- Alertas por producto: excedió presupuesto max, debajo de min, sin compras >60d
- Tabla Últimos Precios Unitarios (QTable con filtro, sort, formato moneda)
- Footer con targets Min/Max editables

**Tipos actualizados:**
- `Producto`: + `lastUnitPrice`, `totalInvestment`, `lastPurchaseDate`, `minQuantity`, `maxQuantity`
- `ProductoRequest`: + `minQuantity`, `maxQuantity`

**Navegación:**
- Sidebar: nuevo link "Análisis de Gastos" con icono `analytics`

### Arquitectura

Sin endpoint nuevo — todo se computa client-side desde `GET /productos`:
- Total inversión: `sum(totalInvestment)`
- Por categoría: `groupBy('category')` con % del total
- Alertas: filtros inline sobre `minQuantity`/`maxQuantity`/`lastPurchaseDate`
- Menos de 200 productos por tenant, suficiente para MVP

### Files tocados

```
src/modules/core/pages/AnalisisGastosPage.vue     # nuevo
src/modules/core/types/index.ts                    # +campos Producto/ProductoRequest
src/modules/core/router/routes.ts                  # +ruta
src/layouts/MainLayout.vue                         # +nav link
```

### Build

- `npm run lint`: ✅
- `npm run build`: ✅

## Issues detectados (post-deploy)

**CRÍTICO — POST /facturas → 500:**
- `ItemFacturaRequest` TS no tiene `presentacionId` → no se envía → backend falla
  con NPE en `presentacionRepository.findById(null)`
- Fix: agregar campo al tipo + `<q-select>` de presentación

## Próximos Pasos en Frontend

### Inmediatos (fix crítico)
1. `types/index.ts` — agregar `presentacionId: string` a `ItemFacturaRequest`
2. `FacturasPage.vue` — al seleccionar producto, cargar presentaciones en un
   4to `<q-select>`
3. Cascade `@Valid` en backend `FacturaRequest.items`

### UX — Facturas
4. **Cascada Categoría→Subcategoría→Producto**: reemplazar select plano por
   3 selects jerárquicos. Parsear `category` (`"Bebidas > Gaseosas > Colas"`)
   para poblar los niveles. Al seleccionar subcategoría, filtrar productos.
5. **Auto-fill precio unitario**: al seleccionar producto, precargar
   `lastUnitPrice` (si existe) como `precioUnitario` sugerido.
6. **Watcher tiempo real**: `subtotal = cantidad * precioUnitario` y viceversa.
7. **Quick-add proveedor inline**: botón "+" junto al select de proveedor,
   mini dialog con nombre + RUC, recargar y seleccionar al guardar.

### UX — Productos
8. Formulario de producto: agregar campos `minQuantity` / `maxQuantity`
   (ahora existen en backend pero UI no los expone).

### Testing (Fase 1)
- ✅ Vitest configurado (`vitest`, `@vue/test-utils`, `happy-dom`)
- ✅ Tests de utilidades: 7 tests en `errors.spec.ts`
- ⬜ Tests de composables: `useAuthForm`, `useLogout`, `useScrollReveal`
- ⬜ Tests de componentes
- ⬜ Tests de integración

### Performance (Fase 3)
- ✅ Lazy loading en todas las rutas
- ✅ Code splitting automático por Vite (32 JS chunks)
- ✅ PWA: Workbox `InjectManifest` configurado
- ⬜ Image optimization (lazy loading nativo, WebP)

### Pendientes conocidos
- `BrandSplash.vue` creado, pendiente de integrar en flujo de carga
- `EmptyState.vue` creado, pendiente de usar en vistas vacías
- Migrar `refreshToken` de `localStorage` a cookie `HttpOnly` en producción
- Input sanitization con DOMPurify cuando haya UGC
- Sentry cuando haya usuarios reales

---

*Creado: 2026-06-19 | Consolidación de FRONTEND_STATUS.md + AUTH_SERVICE_PLAN.md + auth-frontend-strategy-update.md + VERIFICATION_SECURITY_FIX.md + PWA_MODERNIZATION_PLAN.md*

---

## 2026-07-30 — MetricasFinancieras field name mismatch fix

### Contexto

Bug crítico: `GET /core/accounting/consultar` devuelve campos en español (`totalIngresos`, `costoMercaderia`, etc.) pero el frontend `MetricasFinancieras` los definía en inglés (`totalIncome`, `costOfGoods`, etc.). Resultado: todos los KPIs de AccountingPage y DashboardPage mostraban `undefined`/`$0.00`.

### Qué se hizo

Renombrar campos del type `MetricasFinancieras` en `types/index.ts` al español para alinear con el backend `MetricasResponse.java`:

| Antes (inglés) | Después (español) |
|----------------|-------------------|
| `totalIncome` | `totalIngresos` |
| `costOfGoods` | `costoMercaderia` |
| `operatingExpenses` | `gastosOperativos` |
| `loanPayments` | `pagosPrestamos` |
| `totalExpenses` | `totalGastos` |
| `grossMargin` | `margenBruto` |
| `grossMarginPct` | `margenBrutoPct` |
| `operatingMargin` | `margenOperativo` |
| `operatingMarginPct` | `margenOperativoPct` |
| `netMargin` | `margenNeto` |
| `netMarginPct` | `margenNetoPct` |

### Archivos modificados

```
types/index.ts                              → MetricasFinancieras: 11 campos renombrados
pages/DashboardPage.vue                     → totalIngresos, costoMercaderia, gastosOperativos, margenBrutoPct
modules/core/pages/AccountingPage.vue       → +pagosPrestamos, margenOperativoPct, margenNetoPct, margenNeto
modules/core/components/dashboard/StatStrip.vue → totalIngresos, totalGastos, margenNetoPct, margenBrutoPct, margenOperativoPct
```

### Verificación

- Frontend lint: clean
- Frontend vue-tsc: clean (sin errores de tipo)

**Estado:** ✅ COMPLETADO
