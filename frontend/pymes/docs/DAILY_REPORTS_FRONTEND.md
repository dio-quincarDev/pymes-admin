# Daily Reports — Frontend PYMEQ

Registro cronológico de decisiones, problemas resueltos y estado del frontend.

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
