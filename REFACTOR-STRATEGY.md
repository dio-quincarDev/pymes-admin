# REFACTOR STRATEGY — pymes-admin

Estrategia de refactorización en la rama `refactor`.

---

## Pilar A: Auth Flyway — Consolidar migraciones

✅ COMPLETO — V2–V5 fusionadas en V1, `spring.flyway.schemas: auth` configurado.

---

## Pilar B: CI/CD — Simplificación de pipelines

| # | Acción | Archivo | Estado |
|---|--------|---------|--------|
| B.1 | Eliminar `env_file` | `docker-compose.yml` | ✅ |
| B.2 | Eliminar `cp .env.example` (conservar `-Dspring.profiles.active=integration`) | `ci.yml` | ✅ |
| B.3 | Reemplazar script por inline SSH | `cd-staging.yml` | ✅ |
| B.4 | Eliminar `cp .env.example` | `cd-prod.yml` | ✅ |
| B.5 | Eliminar `scripts/deploy-staging.sh` | — | ✅ |
| B.6 | Agregar `.env` a `.gitignore` del gateway | `backend/gateway-pymes/.gitignore` | ✅ |

---

## Pilar 0 (CRÍTICO) — Código de intercambio en vez de JWT en URL

| # | Archivo | Cambio |
|---|---------|--------|
| 0.1 | `OAuth2AuthenticationSuccessHandler.java` | Guardar tokens en Redis con key `oauth:code:<uuid>`, redirect con `?code=<uuid>` |
| 0.2 | `AuthApiController.java` | `@PostMapping("/exchange")` — recibe `{code}`, busca en Redis, devuelve tokens |
| 0.3 | `AuthCallback.vue` | Leer `route.query.code`, `POST /auth/exchange`, usar tokens |
| 0.4 | `SecurityConfig.java` | Agregar `/api/v1/auth/exchange` a WHITE_LIST |

---

## Pilar C: Auth — Seguridad y Calidad

✅ COMPLETO — timing attack, account linking OAuth2, rate limit atómico, AntPathRequestMatcher, ExpiredJwtException en WARN.

---

## Pilar D: Defensa en profundidad (localStorage se mantiene)

| # | Acción | Archivo | Cómo |
|---|--------|---------|------|
| D.1 | Security headers (HSTS, XFO, XCTO, Referrer-Policy) | `gateway-pymes/application.yaml` | `AddResponseHeader` default-filters, sin clase Java |
| D.2 | No loguear JWT | `AuthenticationFilter.java:67` | Quitar `{}` token del mensaje |
| D.3 | No leakear `dbMessage` | `GlobalExceptionHandler.java:253` | Quitar `Map.of("details", dbMessage)` |
| D.4 | No loguear token de verificación | `EmailVerificationServiceImpl.java:67` | Quitar `Token: {}` del log |
| D.5 | JWT secret sin fallback | `gateway-pymes/application.yaml:47` | `${jwt.secret}` sin default — falla si no está configurado |
| D.6 | Axios >=1.6.0 | `frontend/pymes/package.json` | `^1.2.1` → `^1.6.0` |
| D.7 | `@Pattern` en reset password | `ResetPasswordRequest.java` | Misma regex que Register |
| D.8 | CORS métodos explícitos | `gateway-pymes/application.yaml` | `"*"` → `"GET,POST,PUT,PATCH,DELETE,OPTIONS"` |
| D.9 | `replaceState` fix para hash routing | `AuthCallback.vue`, `VerifyEmailPage.vue`, `ResetPasswordPage.vue` | `location.hash.replace(/\?.*$/, '')` — limpia query params del hash |
| D.10 | No exponer token de verificación en URL | `VerifyEmailPage.vue` | `replaceState` inmediato en `onMounted` |
| D.11 | No exponer token de reset en URL | `ResetPasswordPage.vue` | `replaceState` inmediato en `onMounted` |

---

## Post-Mortem: Merge `f959c9e` → `develop` (12-Jun-2026)

### Resumen
Se realizó merge de `feature/refactor` (commit `f959c9e`) contra `develop` usando `git merge -X theirs` para resolver conflictos automáticamente. El resultado fue un merge contaminado que rompió los tests de integración.

### Causa raíz
`develop` tenía migrations V2–V5 que agregaban columnas (`password`, `deleted_at`, `email_verified_at`, `unique token_hash`) que `feature/refactor` ya incluía dentro de `V1__initial_schema.sql`. El merge con `-X theirs` trajo V2–V5 a `feature/refactor`, causando que Flyway fallara al ejecutar `ALTER TABLE ... ADD COLUMN` sobre columnas ya existentes.

```
ERROR: column "password" of relation "users" already exists
```

→ Flyway falla → JPA no crea EntityManagerFactory → contexto no arranca → todos los tests de integración fallan.

### Errores cometidos
1. **`-X theirs` indiscriminado**: resolvió TODOS los conflictos tomando la versión de `develop`, sobrescribiendo cambios intencionales de `feature/refactor` y trayendo archivos que no debían existir (V2–V5).
2. **No revisar migrations post-merge**: no se verificó que V2–V5 eran incompatibles con V1 de `feature/refactor`.
3. **Perseguir síntomas en vez de causa raíz**: se modificaron configs (`application.yaml`, `application-integration.yaml`) innecesariamente, cuando el único problema era V2–V5.

### Lecciones
- **Nunca usar `-X theirs` ni `-X ours`** sin revisar cada archivo en conflicto.
- **Ante error de contexto en tests**, capturar el stack trace completo (`mvn verify -e | grep -C 10 "Caused by"`) antes de especular.
- **Revisar `db/migration/`** antes y después de cada merge para detectar migrations duplicadas o conflictivas.
- **El merge correcto** es `git merge develop` y resolver manualmente, archivo por archivo.

### Fix aplicado
- Reset de `feature/refactor` a `f959c9e` (commit sano previo al merge).
- Cherry-pick de commits post-merge que sí eran útiles:
  - `adc79ae` — CI/CD workflows + SECRETS.md
  - `076c31e` — WebCorsConfig + CORS env vars
  - `241ece5` — Best logic for CI/CD
- Sin V2–V5, sin cambios extra en configs.
- Estado actual: `b3e58f8`
