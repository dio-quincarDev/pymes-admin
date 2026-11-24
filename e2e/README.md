# E2E — Playwright

Regresión de flujos críticos del frontend PWA. Chromium only, `es-VE`, `America/Caracas`.

## Correr

```bash
# desde la raíz del repo (levanta docker compose y espera http://localhost:9200)
npx playwright test -c e2e/playwright.config.ts

# solo los manuales (OAuth2 real contra Google: requiere e2e/.env + 2FA humana)
E2E_MANUAL=1 npx playwright test -c e2e/playwright.config.ts --grep @manual
```

Por defecto `@manual` queda excluido (`grepInvert`), así la suite normal no toca Google ni espera 2FA.

## CI vs manual

| Spec | Corre en CI | Qué valida |
|---|---|---|
| `app.spec.ts` | Sí | Landing carga |
| `auth-session.spec.ts` | Sí (API mockeada) | Login/logout/re-login, tokens frescos y `tenantId` mergeado desde `activeTenant` |
| `oauth2-callback.spec.ts` | Sí (API mockeada) | Code exchange del callback, `tenantId` y dashboard |
| `invoice-itbms.spec.ts` | Sí (API mockeada) | ITBMS 0/7/10 por ítem: desglose, total y payload |
| `pwa-cache.spec.ts` | Sí | `auth:401` limpia `core-api-cache` desde el scope del SW |
| `logout-cycle.spec.ts` | Sí | `auth:401` limpia localStorage y no deja tokens stale |
| `oauth2.spec.ts` (`@manual`) | No | Flujo real Google: registro → OAuth2 → onboarding → logout → re-login. Google bloquea headless y exige 2FA; correrlo es decisión humana pre-merge |

El job `e2e-tests` en `ci.yml` aún no existe; su integración es tarea aparte (Media).
