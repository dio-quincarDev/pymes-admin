# PyMes Admin — Frontend

> Plataforma SaaS de gestion financiera para PyMEs en Latinoamerica. Modulo frontend construido con Quasar 2 (Vue 3) y Capacitor.

![Vue 3](https://img.shields.io/badge/Vue-3.5-42b883?logo=vue.js)
![Quasar 2](https://img.shields.io/badge/Quasar-2.16-1976D2?logo=quasar)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?logo=typescript)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## Quick Start

```bash
cd frontend/pymes
cp .env.example .env
npm install
npm run dev                  # http://localhost:9200
```

| Requisito | Version | Notas |
|-----------|---------|-------|
| Node.js   | >= 20.x | LTS recomendada |
| npm       | >= 9.x  | Viene con Node |
| Android Studio | 2024+ | Solo para Capacitor |

---

## Development

```bash
npm run dev          # servidor local (port 9200)
npm run lint         # ESLint
npm run format       # Prettier
npm run build        # produccion (lint + typecheck + build)
```

> Docker: `docker compose up frontend` desde la raiz del proyecto.

---

## Project Structure

```
frontend/pymes/
├── src/
│   ├── assets/              # imagenes, fuentes, iconos
│   ├── boot/                # scripts de inicializacion
│   │   ├── axios.ts         # interceptor 401/403, refresh rotation, baseURL
│   │   ├── error-handler.ts # handler global de errores
│   │   ├── i18n.ts          # internacionalizacion
│   │   └── web-vitals.ts    # metricas de rendimiento
│   ├── components/
│   │   ├── base/            # BaseButton, BaseCard, BaseBadge, BaseSkeleton
│   │   ├── landing/         # LandingHero, FeatureGrid, TrustSection
│   │   └── ui/              # BrandSplash, EmptyState, SkeletonLoader
│   ├── composables/         # useAuthForm, useLogout, useScrollReveal, useKeyboardShortcuts, useChartTheme
│   ├── design/              # tokens.ts (Swiss/Grid palette)
│   ├── i18n/                # locale en-US (placeholder)
│   ├── layouts/             # LandingLayout, AuthLayout, MainLayout
│   ├── modules/
│   │   ├── auth/            # login, register, OAuth2, forgot-password, teams
│   │   │   ├── pages/       # LoginPage, RegisterPage, OAuthCallback, AcceptInvitation, Teams, etc.
│   │   │   ├── router/      # rutas protegidas + publicas
│   │   │   ├── services/    # API: register, login, logout, refresh, invitation
│   │   │   ├── store/       # Pinia: token, user, tenant
│   │   │   └── types/       # AuthPayload, LoginCredentials
│   │   └── core/            # dashboard, productos, facturas, gastos, ventas, prestamos, patrimonio, costos, analisis, contabilidad
│   │       ├── pages/       # OnboardingPage, CatalogDashboard, ProductosPage, etc.
│   │       ├── components/  # dashboard/, facturas/, analytics/, onboarding/
│   │       ├── composables/ # useAnalytics, usePeriod, useNumberFormat, useFinancialDashboard, useChartTheme
│   │       ├── services/    # setup, producto, proveedor, factura, gasto, venta, prestamo, patrimonio, accounting, analytics
│   │       ├── router/      # rutas hijas de dashboard
│   │       └── types/       # Producto, Factura, SetupInfo, AnalyticsResponse, etc.
│   ├── router/              # router principal + guards
│   ├── stores/              # Pinia store principal
│   ├── styles/
│   │   ├── variables.scss   # tokens: colores, spacing, radius, shadows
│   │   └── app.scss         # estilos globales + Swiss/Grid + a11y utilities
│   ├── types/               # BackendError, ApiError, ERROR_CODES
│   └── utils/               # parseBackendError, isAuthError, formatCurrency, formatPct, formatDate
├── src-capacitor/           # Capacitor (Android)
├── src-pwa/                 # PWA: manifest, service worker
├── public/                  # favicon, iconos
├── Dockerfile               # multi-stage: node:20 -> nginx:alpine-slim
├── nginx.conf               # gzip, security headers, SPA fallback
└── quasar.config.ts         # configuracion Quasar
```

---

## Architecture

### Routing

Modo `hash` (`/#/login`, `/#/dashboard`). Rutas definidas en `src/router/index.ts` con meta `requiresAuth: true`.

| Ruta                          | Componente        | Descripcion                          |
|-------------------------------|-------------------|--------------------------------------|
| `/`                           | LandingLayout     | Landing page                         |
| `/login`                      | LoginPage         | Login local + OAuth2 Google          |
| `/register`                   | RegisterPage      | Registro (Company First)             |
| `/onboarding`                 | OnboardingPage    | Flujo 2 pasos (standalone)           |
| `/dashboard`                  | CatalogDashboard  | Panel principal + KPIs               |
| `/dashboard/analisis-gastos`  | AnalisisGastosPage| Analisis de gastos por categoria     |
| `/dashboard/productos`        | ProductosPage     | CRUD productos + presentaciones      |
| `/dashboard/proveedores`      | ProveedoresPage   | CRUD proveedores                     |
| `/dashboard/facturas`         | FacturasPage      | CRUD facturas + pagar + GASTO_OPERATIVO |
| `/dashboard/gastos`           | → redirect a costos?tab=gastosFijos | Redirige a gastos fijos |
| `/dashboard/ventas`           | VentasPage        | CRUD ventas diarias                  |
| `/dashboard/prestamos`        | PrestamosPage     | CRUD prestamos + pagos               |
| `/dashboard/patrimonio`       | PatrimonioPage    | Capital inicial (get-or-create)      |
| `/dashboard/costos`           | CostosPage        | Gastos fijos recurrentes + auto-fill proveedor |
| `/dashboard/accounting`       | AccountingPage    | Metricas financieras consolidadas    |
| `/teams`                      | TeamsPage         | Gestion de miembros del equipo       |

### Auth Flow

```
1. Usuario completa formulario    -> POST /api/v1/auth/register
2. Servicio retorna null          (pending registration)
3. Verifica email                 -> POST /api/v1/auth/verify-email
4. Verificacion exitosa           -> auto-login -> JWT + refresh token
5. Pinia store guarda tokens en localStorage
6. Axios interceptor agrega Authorization header automaticamente
7. En 401/403                     -> limpia tokens -> redirect a /#/login
```

**OAuth2 (Google):**

```
1. Frontend crea intent   -> POST /api/v1/auth/oauth2/intent (Redis)
2. Redirect               -> /oauth2/authorization/google?intentId={intentId}
3. Usuario autoriza en Google
4. Callback con code      -> redirige a /#/auth/callback?code={uuid}
5. Frontend intercambia   -> POST /api/v1/auth/exchange {code}
6. Backend retorna JWT + refresh token
7. Navegacion a /#/dashboard
```

> Rutas publicas: `/`, `/login`, `/register`, `/verify`, `/forgot-password`, `/reset-password`, `/accept-invitation`, `/auth/callback`

> Ruta protegida: `/dashboard` (requiere JWT valido)

> Onboarding guard: Si `onboardingCompleted=false` redirige a `/onboarding` antes de `/dashboard`.

### Onboarding

Flujo 2 pasos post-registro:

```
1. Seleccion de industria
   8 cards: restaurante, bares, salon_belleza, ferreteria,
            mini_super, taller_mecanico, farmacia, default

2. Preview interactivo
   - KPIs: Productos, Categorias, Unidades, Ubicaciones
   - Busqueda reactiva de productos base
   - Split layout con q-tabs/q-tab-panels

3. Confirmar
   POST /core/setup/{tenantId}/onboarding -> /dashboard
```

### Design System (Swiss/Grid + Institutional Warmth)

| Aspecto          | Detalle                                             |
|------------------|-----------------------------------------------------|
| Paleta           | Near-black `#08090D` + bronze `#C8963E`             |
| Tipografia       | Geist (display), Satoshi (body), Geist Mono (numbers) |
| Tokens CSS       | `pq-*` prefix para evitar collision con Quasar      |
| Componentes base | BaseButton (5 variants), BaseCard (4 variants), BaseBadge (5 variants) |
| Visual polish    | `.fade-in-up` + `.stagger-children` (10 hijos)     |
| Animaciones      | `.stagger-children` en KpiCard y AccountingPage    |

**Accesibilidad (WCAG 2.2):**

- Skip links en LandingLayout y MainLayout
- `prefers-reduced-motion` override global en app.scss
- `.focus-ring` (focus-visible outline) en campos de auth
- `.visually-hidden` utilidad screen-reader-only
- `aria-label` en botones, inputs, navegacion
- `role="alert"` offline banner, `role="navigation"` sidebar, `role="tablist"` tabs
- `role="button"` + `tabindex="0"` en IndustryCard, password toggles
- `aria-expanded` en CatalogDashboard tree rows
- `aria-hidden="true"` en iconos decorativos

### Componentes

**Globales** (`src/components/`):

| Carpeta      | Proposito                                    |
|--------------|----------------------------------------------|
| `base/`      | UI atomica reutilizable (BaseButton, BaseCard, BaseBadge) |
| `landing/`   | Pagina publica (hero, features, trust)       |
| `dashboard/` | Panel principal (stats, acciones, actividad) |
| `ui/`        | Utilidades globales (BrandSplash, EmptyState)|

**Modulo core** (`src/modules/core/components/`):

| Carpeta        | Proposito                                              |
|----------------|--------------------------------------------------------|
| `onboarding/`  | CategoryTree (arbol jerarquico), IndustryCard          |
| `facturas/`    | InvoiceItemCard, InvoiceDetailDialog, CategoryTabs, ConfirmDialog |
| `analytics/`   | AnalyticsHeader, KpiCard, MetricCard, CategoryBreakdownChart, DataTable |
| `dashboard/`   | ActivityPanel, AbcGastosChart, AlertsPanel, ExpenseBreakdown, OpexGauge, PriceTrendSparkline, ProjectionTimeline, SupplierComparisonTable, PricePredictionsTable, PeriodSelector |
| `charts/`      | BaseChart                                              |

**Composables** (`src/modules/core/composables/`):

| Composable          | Funcion                                        |
|---------------------|------------------------------------------------|
| `useAnalytics`      | Fetch + cache reactivo de analisis por periodo + financialHealth |
| `usePeriod`         | Selector de periodo con persistencia localStorage |
| `useNumberFormat`   | Formato moneda USD (en-US)                     |
| `useFinancialDashboard` | KPIs consolidados, gastos por categoria, actividad reciente |
| `useChartTheme`     | Design tokens de Chart.js desde CSS variables   |

**Utils** (`src/utils/`):

| Util     | Funcion                                      |
|----------|----------------------------------------------|
| `format.ts` | `formatCurrency()`, `formatPct()`, `formatDate()` (Intl.NumberFormat) |
| `errors.ts` | `isAuthError`, `isTokenExpiredError`, `isTokenRevokedError`, `isValidationError` |

### Analytics

10 motores CTE consumidos desde `GET /api/v1/core/analytics` (6 visibles por defecto, 4 bajo demanda):

| Motor             | Descripcion                                             | Visible |
|-------------------|---------------------------------------------------------|---------|
| Alertas           | Variacion >15% (CV), primer registro proveedor          | Siempre |
| ABC de Gastos     | Pareto: categorias A/B/C por % acumulado de gasto       | Expansion |
| Precios y Tendencias | % cambio vs media movil 90 dias                      | Expansion |
| Margenes          | Delta precio unitario periodo actual vs anterior         | Expansion |
| Costo Operativo   | Gasto operativo % ventas + proyeccion mensual           | Expansion |
| Proyeccion        | Forecast lineal 30/60/90 dias                           | Expansion |
| Gasto vs Ingreso  | Comparativa visual de ingresos vs gastos                 | Siempre |
| Supplier analytics| 3 motores: comparativa precios, recomendaciones, predicciones OLS | Expansion |
| Financial Health  | Score 0-100 con alertas criticas y recomendaciones      | Dashboard |

### SEO

| Aspecto              | Estado                                           |
|----------------------|--------------------------------------------------|
| `lang`               | `es-MX`                                          |
| `robots`             | `index, follow`                                  |
| Open Graph           | `og:title`, `og:description`, `og:type`, `og:site_name`, `og:locale` |
| Twitter Card         | `summary_large_image`                            |
| Viewport             | Sin bloqueo de zoom (`user-scalable=no` eliminado) |
| PWA                  | `apple-mobile-web-app-capable`, `theme-color`     |

### PWA / Offline

- **Service Worker:** InjectManifest con precaching de assets + `StaleWhileRevalidate` para `GET /api/v1/core/*`
- **Banner offline:** `q-banner` amarillo en MainLayout con listener `navigator.onLine`
- **Actualizacion disponible:** Auto-update silencioso via `SKIP_WAITING` (sin dialog de confirmacion)
- **Cache API:** Redis en backend (core service, TTL 5min, `@Cacheable`/`@CacheEvict`)

### Keyboard Shortcuts

| Shortcut | Accion                                    |
|----------|-------------------------------------------|
| `N`      | Crear nuevo registro                      |
| `?`      | Mostrar ayuda de shortcuts                |
| `Esc`    | Cerrar dialog                             |
| `Ctrl+K` | Busqueda global (placeholder)             |

### UX Features

- **EmptyState**: Componente reutilizable en 6 paginas CRUD
- **Unsaved changes guard**: Navigation guard en ProductosPage y FacturasPage
- **Dialog animations**: slide-up/slide-down en todos los dialogs CRUD
- **Error messages**: Propagados desde interceptor axios a 32 catch blocks
- **Currency formatting**: `type="text"` + `inputmode="decimal"` + formato al blur

---

## Configuration

### Environment Variables

| Variable       | Requerida | Descripcion                                  |
|----------------|-----------|----------------------------------------------|
| `VITE_API_URL` | Si        | URL del backend (default: `http://localhost:8080/api/v1`) |
| `VITE_APP_TITLE` | No     | Titulo de la app (default: "PyMes Admin")    |

### Docker Build

```bash
docker compose build frontend --build-arg API_URL=https://api.tudominio.com
```

---

## Testing

```bash
npm run test          # vitest (errors.spec.ts + store + composables)
npm run test:watch    # vitest en modo watch
npm run build         # incluye lint + vue-tsc
```

**Tests existentes**: 29 tests (errors 10 + store 6 + composables 3 + errors extendidos 10)

### E2E (Playwright)

```bash
npx playwright install chromium
npx playwright test                     # correr todos
npx playwright test app.spec.ts         # solo tests de carga
```

- **`app.spec.ts`** — 2 tests: landing page carga, input de empresa visible. Corre en CI.
- **`oauth2.spec.ts`** — test completo: registro → Google OAuth2 → onboarding → dashboard → logout → re-login. Requiere Chromium headed + 2FA manual. **No corre en CI.**

> Chromium headless es bloqueado por Google. Para `oauth2.spec.ts` usar `--headed` y aprobar 2FA desde celular.

---

## Build

### Produccion

```bash
npm run build
# Output: dist/pwa/
```

**Bundle:** ~385 KB JS, 32 chunks (code splitting automatico).

### Docker

```bash
docker compose up -d frontend
# Multi-stage: node:20 build -> nginx:alpine-slim
# Puerto: 9200
```

### Capacitor (Android)

```bash
npx quasar dev -m capacitor -T android --ide
# Requiere: Android Studio, SDK Android 33+
```

Ver [CAPACITOR_SETUP.md](docs/strategies/CAPACITOR_SETUP.md) para guia completa.

---

## CI/CD

GitHub Actions ejecuta lint + build en cada PR a main/develop/feature/*. Docker images multi-arch (AMD64/ARM64) se buildean y pushean en CD.

| Rama | Pipeline | Deploy |
|------|----------|--------|
| `feature/*` | CI (lint + build) | Ninguno |
| `develop` | CI + CD | Staging |
| `main` | CI + CD | Produccion |

---

## Known Issues

| Problema                          | Estado    | Notas                                                             |
|-----------------------------------|-----------|-------------------------------------------------------------------|
| Field name mismatch MetricasFinancieras | Pendiente | Frontend usa ingles, backend responde espanol. Dashboard/Accounting no muestran datos correctamente. |
| i18n placeholder                  | Pendiente | Solo locale en-US con 2 strings.                                  |
| `og:image` / `twitter:image`      | Pendiente | Social previews sin imagen.                                       |
| Brand drift manifest              | Pendiente | `manifest.json` dice "Auditoria Inteligente" pero index dice "Gestion Financiera". |

---

## Contributing

1. Crear branch desde `develop`
2. Hacer cambios
3. Ejecutar `npm run lint` y `npm run build`
4. Crear PR a `develop`

---

## License

Proprietary. Todos los derechos reservados.

---

> Ver tambien:
> - [Auth Strategy](docs/strategies/AUTH_STRATEGY.md) — Flujo de autenticacion completo
> - [Design System](docs/strategies/DESIGN_SYSTEM.md) — Paleta, tipografia, tokens
> - [Capacitor Setup](docs/strategies/CAPACITOR_SETUP.md) — Guia Android
> - [PWA / Offline](docs/PWA_OFFLINE.md) — Service Worker, cache, banner offline
> - [Analytics Dashboard](docs/ANALYTICS_DASHBOARD_STRATEGY.md) — Estrategia frontend analytics
> - [Daily Reports](docs/DAILY_REPORTS_FRONTEND.md) — Historial de desarrollo
