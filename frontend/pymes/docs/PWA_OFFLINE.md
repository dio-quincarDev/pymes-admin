# PWA / Offline — 2026-09-18

Service Worker en modo `InjectManifest` (`quasar.config.ts workboxMode InjectManifest`). Archivos fuente `src-pwa/`.

## Implementado (2026-09-18)

| Aspecto | Archivo | Detalle |
|---------|---------|---------|
| Precaching de assets | `custom-service-worker.ts` | `precacheAndRoute(self.__WB_MANIFEST)` — JS/CSS/imgs + `cleanupOutdatedCaches()` + `skipWaiting()+clientsClaim()` |
| Cache de API (lecturas) | `custom-service-worker.ts` | `StaleWhileRevalidate` `GET /api/v1/core/*` `core-api-cache` |
| SW no intercepta OAuth | `custom-service-worker.ts:54` | `NavigationRoute denylist [/^\/oauth2/,/^\/login/,/workbox-.*\.js$/]` (2026-08-30 fix `200` en `/oauth2/authorization/google`) |
| SW cache limpieza logout | `custom-service-worker.ts` + `store/index.ts` | `postMessage CLEAR_API_CACHE` desde SW scope (no `caches.delete` main thread, 2026-08-29) |
| Manifest cache-busting | `quasar.config.ts` | `extendManifestJson ?v=Date.now()` por icono (2026-08-29) |
| Viewport + safe-area | `index.html` + `app.scss` + `MainLayout` | `viewport-fit=cover` siempre (2026-09-18) + `env(safe-area-inset-bottom)` en `mobile-bottom-nav` y `pymeq-tour-popover` |
| Banner offline | `MainLayout.vue` | `q-banner` `v-if="!online"` `role=alert` + chip `cacheado` + `última sync: HH:mm es-PA` + `lastSync` 30s poll + notify `Conexión de vuelta — datos al día` (2026-09-18) |
| Offline no logout | `src/boot/axios.ts` | `isOffline+isNetworkError` no borra sesión, `refreshTokens` early-return `!navigator.onLine`, `pymeq_last_sync` en cada response OK (2026-09-18, avoids metro logout) |
| Actualización disponible | `MainLayout.vue` + `register-service-worker.ts` | Auto `SKIP_WAITING` silencioso → `controllerchange` → reload (sin dialog, 2026-08-29) |
| Iconos + manifest | `src-pwa/manifest.json` + `public/icons/` | standalone, 15 PNGs regenerados desde `pymeq-app-icon.svg` + SVG any, `theme-color #08090D` |
| Nginx/Caddy SW/SVG | `nginx.conf` + `Caddyfile` | `location ^~ /sw.js` `no-cache` + `path_regexp \.svg$` `no-cache` para SW/SVG/HTML (2026-08-27, Cloudflare purge) |

> **2026-08-11 — Fix PWA sw.js** (ver histórico): `sw.js` excluido de `.js immutable 1y` con `^~` → `no-cache`.
> **2026-08-27 — SVG no-cache** + **2026-08-29 — postMessage** + **2026-08-30 — denylist oauth2** + **2026-09-18 — viewport-fit + offline-fix** todos verificados `npm run build` PWA 924KB.

## Flujo offline (2026-09-18)

1. Online: JWT en `localStorage` + `pymeq_last_sync` update por cada `api` response OK
2. SW cachea `GET /api/v1/core/*` con `StaleWhileRevalidate` → offline sirve cache + banner `datos desactualizados · última sync HH:mm`
3. Sin red: writes fallan con `OFFLINE` (`ERR_NETWORK`/`!navigator.onLine`) sin hacer logout; token expira offline → encola, no revoca, reintenta al volver
4. Al volver: `online` event → notify + `lastSync` update → siguiente `GET` refresca SWR

## Backend

Redis `core-api-cache` (core, 5min `@Cacheable`/`@CacheEvict` facturas+productos). Ver `backend/core/docs/CORE.md`. Idempotencia `POST Idempotency-Key` 6h `SET NX` + `pg_advisory_xact_lock` (no duplica retry).
