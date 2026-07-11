# PWA / Offline

Service Worker en modo `InjectManifest`. Archivos fuente en `src-pwa/`.

## Implementado

| Aspecto | Archivo | Detalle |
|---------|---------|---------|
| Precaching de assets | `custom-service-worker.ts` | `precacheAndRoute(self.__WB_MANIFEST)` — todo JS/CSS/imgs |
| Cache de API (lecturas) | `custom-service-worker.ts` | `StaleWhileRevalidate` para `GET /api/v1/core/*` |
| Banner offline | `MainLayout.vue` | `q-banner` con `navigator.onLine` listener |
| Actualización disponible | `MainLayout.vue` + `register-service-worker.ts` | Dialog "Actualizar ahora" → `SKIP_WAITING` → `controllerchange` → reload |
| Iconos + manifest | `src-pwa/manifest.json` + `public/icons/` | standalone, 5 tamaños, shortcuts |
| Nginx SW header | `nginx.conf` | `Cache-Control: no-cache` para `service-worker.js` |

## Flujo offline

1. Usuario inicia sesión online → JWT en `localStorage`
2. SW intercepta GETs a `/api/v1/core/*` y cachea con `StaleWhileRevalidate`
3. Sin red: GETs se sirven desde cache, writes (POST/PUT/DELETE) fallan
4. Banner amarillo "Sin conexión" visible en todas las rutas
5. Token expira offline → redirige a login, muestra error al reconectar

## Backend

Redis cache (core service, TTL 5min, `@Cacheable`/`@CacheEvict`) reduce viajes a DB. Ver `backend/core/docs/CORE.md`.
