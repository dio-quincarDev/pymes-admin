# PyMes Admin — Frontend

> Plataforma SaaS de gestion financiera para PyMEs en Latinoamerica. Modulo frontend construido con Quasar 2 (Vue 3) y Capacitor.

![Vue 3](https://img.shields.io/badge/Vue-3.5-42b883?logo=vue.js)
![Quasar 2](https://img.shields.io/badge/Quasar-2.18-1976D2?logo=quasar)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?logo=typescript)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## Quick Start

```bash
cd frontend/pymes
cp .env.example .env        # configurar variables
npm install
npm run dev                  # http://localhost:9200
```

## Prerequisites

| Requisito | Version | Notas |
|-----------|---------|-------|
| Node.js | >= 20.x | LTS recomendada |
| npm | >= 9.x | Viene con Node |
| Android Studio | 2024+ | Solo para Capacitor |

## Development

```bash
npm run dev          # servidor local (port 9200)
npm run lint         # ESLint
npm run format       # Prettier
npm run build        # produccion (lint + typecheck + build)
```

### Docker

```bash
docker compose up frontend   # desde raiz del proyecto
```

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
│   │   └── auth/            # login, register, OAuth2, forgot-password
│   │       ├── pages/       # LoginPage, RegisterPage, OAuthCallback, etc.
│   │       ├── router/      # rutas protegidas + publicas
│   │       ├── services/    # API: register, login, logout, refresh
│   │       ├── store/       # Pinia: token, user, tenant
│   │       └── types/       # AuthPayload, LoginCredentials
│   ├── router/              # router principal + guards
│   ├── stores/              # Pinia store principal
│   ├── styles/
│   │   ├── variables.scss   # tokens: colores, spacing, radius, shadows
│   │   └── app.scss         # estilos globales + glassmorphism
│   ├── types/               # BackendError, ApiError, ERROR_CODES
│   └── utils/               # parseBackendError, isAuthError, etc.
├── src-capacitor/           # Capacitor (Android)
├── src-pwa/                 # PWA: manifest, service worker
├── public/                  # favicon, iconos
├── Dockerfile               # multi-stage: node:20 → nginx:alpine-slim
├── nginx.conf               # gzip, security headers, SPA fallback
└── quasar.config.ts         # configuracion Quasar
```

## Architecture

### Routing

Modo `hash` (`/#/login`, `/#/dashboard`). Rutas definidas en `src/router/index.ts` con meta `requiresAuth: true`.

### Auth Flow

```
1. Usuario completa formulario → POST /api/v1/auth/register
2. Servicio retorna null (pending registration, usuario no puede login aun)
3. Usuario verifica email → POST /api/v1/auth/verify-email
4. Verificacion exitosa → auto-login → JWT + refresh token
5. Pinia store guarda tokens en localStorage
6. Axios interceptor agrega Authorization header automaticamente
7. En 401/403: limpia tokens → redirect a /#/login
```

**OAuth2 (Google):**
```
1. Frontend crea intent → POST /api/v1/auth/oauth2/intent (guarda tenant en Redis)
2. Redirect a /api/v1/auth/oauth2/authorize/google?state={intentId}
3. Usuario autoriza en Google
4. Callback con code → POST /api/v1/auth/oauth2/code/google
5. Backend retorna JWT + refresh token
6. Navegacion a /#/dashboard
```

**Rutas publicas:** `/`, `/login`, `/register`, `/verify`, `/forgot-password`, `/reset-password`, `/accept-invitation`, `/auth/callback`

**Ruta protegida:** `/dashboard` (requiere JWT valido)

### Design System

- **Paleta:** Deep Forest (`#0B1210`) + Copper (`#A3785E`)
- **Tipografia:** Outfit (headings), Source Sans 3 (body)
- **Tokens CSS:** `pq-*` prefix para evitar collision con Quasar
- **Glassmorphism:** `background: rgba(11,18,16,0.65); backdrop-filter: blur(24px)`
- **Componentes base:** BaseButton (5 variantes), BaseCard (4 variantes), BaseSkeleton

Ver [DESIGN_SYSTEM.md](docs/strategies/DESIGN_SYSTEM.md) para detalle completo.

### Componentes

| Carpeta | Proposito |
|---------|-----------|
| `base/` | UI atomica reutilizable (buttons, cards, skeletons) |
| `landing/` | Pagina publica (hero, features, trust) |
| `dashboard/` | Panel principal (stats, acciones, actividad) |
| `ui/` | Utilidades globales (BrandSplash, EmptyState) |

## Configuration

### Environment Variables

| Variable | Requerida | Descripcion |
|----------|-----------|-------------|
| `VITE_API_URL` | Si | URL del backend (default: `http://localhost:8080/api/v1`) |
| `VITE_APP_TITLE` | No | Titulo de la app (default: "PyMes Admin") |

### Docker Build

```bash
docker compose build frontend --build-arg API_URL=https://api.tudominio.com
```

## Testing

```bash
npm run test          # vitest (errors.spec.ts)
npm run test:watch    # vitest en modo watch
npm run build         # incluye lint + vue-tsc
```

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
# Multi-stage: node:20 build → nginx:alpine-slim
# Puerto: 9200
```

### Capacitor (Android)

```bash
npx quasar dev -m capacitor -T android --ide
# Requiere: Android Studio, SDK Android 33+
```

Ver [CAPACITOR_SETUP.md](docs/strategies/CAPACITOR_SETUP.md) para guia completa.

## Known Issues

| Problema | Estado | Notas |
|----------|--------|-------|
| CORS con Spring Cloud Gateway 3.2.0 | Pendiente | OPTIONS funciona, POST retorna 403. Bug conocido del framework. |
| i18n placeholder | Pendiente | Solo locale en-US con 2 strings. |

## Contributing

1. Crear branch desde `develop`
2. Hacer cambios
3. Ejecutar `npm run lint` y `npm run build`
4. Crear PR a `develop`

## License

Proprietary. Todos los derechos reservados.

---

Ver tambien:
- [Auth Strategy](docs/strategies/AUTH_STRATEGY.md) — Flujo de autenticacion completo
- [Design System](docs/strategies/DESIGN_SYSTEM.md) — Paleta, tipografia, tokens
- [Capacitor Setup](docs/strategies/CAPACITOR_SETUP.md) — Guia Android
- [Daily Reports](docs/DAILY_REPORTS_FRONTEND.md) — Historial de desarrollo
