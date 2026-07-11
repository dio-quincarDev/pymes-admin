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
│   │   ├── axios.ts         # interceptor 401/403, baseURL
│   │   ├── error-handler.ts # handler global de errores
│   │   ├── i18n.ts          # internacionalizacion
│   │   └── web-vitals.ts    # metricas de rendimiento
│   ├── components/
│   │   ├── base/            # BaseButton, BaseCard, BaseSkeleton
│   │   ├── dashboard/       # DashboardStats, DashboardActionCard, RecentActivity
│   │   ├── landing/         # LandingHero, FeatureGrid, TrustSection
│   │   └── ui/              # BrandSplash, EmptyState, SkeletonLoader
│   ├── composables/         # useAuthForm, useLogout, useScrollReveal
│   ├── i18n/                # locale en-US (placeholder)
│   ├── layouts/             # LandingLayout, AuthLayout, MainLayout
│   ├── modules/
│   │   ├── auth/            # login, register, OAuth2, forgot-password
│   │   │   ├── pages/       # LoginPage, RegisterPage, OAuthCallback, etc.
│   │   │   ├── router/      # rutas protegidas + publicas
│   │   │   ├── services/    # API: register, login, logout, refresh
│   │   │   ├── store/       # Pinia: token, user, tenant
│   │   │   └── types/       # AuthPayload, LoginCredentials
│   │   └── core/            # dashboard, productos, proveedores, facturas, analisis
│   │       ├── pages/       # OnboardingPage, CatalogDashboard, ProductosPage, etc.
│   │       ├── components/  # dashboard/, facturas/, analytics/, onboarding/
│   │       ├── composables/ # useAnalytics, usePeriod, useNumberFormat
│   │       ├── services/    # setup, producto, proveedor, factura, analytics
│   │       ├── router/      # rutas hijas de dashboard
│   │       └── types/       # Producto, Factura, SetupInfo, AnalyticsResponse
│   ├── router/              # router principal + guards
│   ├── stores/              # Pinia store principal
│   ├── styles/
│   │   ├── variables.scss   # tokens: colores, spacing, radius, shadows
│   │   └── app.scss         # estilos globales + glassmorphism + a11y utilities
│   ├── types/               # BackendError, ApiError, ERROR_CODES
│   └── utils/               # parseBackendError, isAuthError, etc.
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
| `/login`                      | LoginPage         | Login local                          |
| `/register`                   | RegisterPage      | Registro (Company First)             |
| `/onboarding`                 | OnboardingPage    | Flujo 2 pasos (standalone)           |
| `/dashboard`                  | CatalogDashboard  | Panel principal                      |
| `/dashboard/analisis-gastos`  | AnalisisGastosPage| Analisis de gastos por categoria     |
| `/dashboard/productos`        | ProductosPage     | CRUD productos + presentaciones      |
| `/dashboard/proveedores`      | ProveedoresPage   | CRUD proveedores                     |
| `/dashboard/facturas`         | FacturasPage      | CRUD facturas + pagar                |
| `/dashboard/configuracion`    | ConfiguracionPage | Categorias, unidades, ubicaciones    |

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
2. Redirect               -> /api/v1/auth/oauth2/authorize/google?state={intentId}
3. Usuario autoriza en Google
4. Callback con code      -> POST /api/v1/auth/oauth2/code/google
5. Backend retorna JWT + refresh token
6. Navegacion a /#/dashboard
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

2. Preview
   Categorias jerarquicas (arbol con subcategorias)
   + Productos precargados (20-25 por industria, SKU auto P-0001)

3. Confirmar
   POST /core/setup/{tenantId}/onboarding -> /dashboard
```

### Analytics

9 motores CTE consumidos desde `GET /api/v1/core/analytics`:

| Motor             | Descripcion                                             |
|-------------------|---------------------------------------------------------|
| ABC de Gastos     | Pareto: categorias A/B/C por % acumulado de gasto       |
| Tendencias        | % cambio vs media movil 90 dias                         |
| Margenes          | Delta precio unitario periodo actual vs anterior         |
| Costo Operativo   | Gasto operativo % ventas + proyeccion mensual           |
| Proyeccion        | Forecast lineal 30/60/90 dias                           |
| Alertas           | Variacion >15% (CV), primer registro proveedor          |

Supplier analytics (3 motores adicionales): comparativa precios por proveedor, recomendaciones con savings_pct, predicciones OLS con R2.

| Composable       | Funcion                                        |
|------------------|------------------------------------------------|
| `useAnalytics`   | Fetch + cache reactivo de analisis por periodo |
| `usePeriod`      | Selector de periodo con persistencia localStorage |
| `useNumberFormat`| Formato moneda USD (en-US)                     |

### Design System

| Aspecto          | Detalle                                             |
|------------------|-----------------------------------------------------|
| Paleta           | Deep Forest `#0B1210` + Copper `#A3785E`           |
| Tipografia       | Outfit (headings), Source Sans 3 (body)             |
| Tokens CSS       | `pq-*` prefix para evitar collision con Quasar      |
| Glassmorphism    | `.glass` con `backdrop-blur` + SVG noise texture    |
| Componentes base | BaseButton (5), BaseCard (4), BaseSkeleton          |
| Visual polish    | `.brand-glow`, `.mesh-gradient-text`, `.hover-lift` |
| Animaciones      | `.fade-in-up` + `.stagger-children` (10 hijos)     |

**Accesibilidad (WCAG 2.2):**

- Skip links en LandingLayout y MainLayout
- `prefers-reduced-motion` override global en app.scss
- `.focus-ring` (focus-visible outline) en campos de auth
- `.visually-hidden` utilidad screen-reader-only
- `aria-label` en botones, inputs, navegacion
- `role="alert"` offline banner, `role="navigation"` sidebar, `role="tablist"` tabs

Ver [DESIGN_SYSTEM.md](docs/strategies/DESIGN_SYSTEM.md) para detalle completo.

### Componentes

**Globales** (`src/components/`):

| Carpeta      | Proposito                                    |
|--------------|----------------------------------------------|
| `base/`      | UI atomica reutilizable (buttons, cards)     |
| `landing/`   | Pagina publica (hero, features, trust)       |
| `dashboard/` | Panel principal (stats, acciones, actividad) |
| `ui/`        | Utilidades globales (BrandSplash, EmptyState)|

**Modulo core** (`src/modules/core/components/`):

| Carpeta        | Proposito                                              |
|----------------|--------------------------------------------------------|
| `onboarding/`  | CategoryTree (arbol jerarquico), IndustryCard          |
| `facturas/`    | InvoiceItemCard, InvoiceDetailDialog, CategoryTabs     |
| `analytics/`   | SupplierComparisonTable, SupplierRecommendationsCard, PricePredictionsTable |
| `dashboard/`   | CatalogDashboard (arbol productos + KPIs + busqueda)   |

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
- **Actualizacion disponible:** Dialog "Actualizar ahora" -> `SKIP_WAITING` -> reload
- **Cache API:** Redis en backend (core service, TTL 5min, `@Cacheable`/`@CacheEvict`)

Ver [PWA_OFFLINE.md](docs/PWA_OFFLINE.md) para detalle completo.

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
npm run test          # vitest (errors.spec.ts)
npm run test:watch    # vitest en modo watch
npm run build         # incluye lint + vue-tsc
```

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

## Known Issues

| Problema                          | Estado    | Notas                                                             |
|-----------------------------------|-----------|-------------------------------------------------------------------|
| CORS con Spring Cloud Gateway 3.2.0 | Pendiente | OPTIONS funciona, POST retorna 403. Bug conocido del framework. |
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
