# REFACTOR STRATEGY — pymes-admin

Estrategia de refactorizacion en la rama `refactor`.

**Estado:**
| Pilar | Tema | Estado |
|-------|------|--------|
| A | Auth Flyway — Consolidar migraciones | ✅ Completo |
| B | CI/CD — Simplificacion de pipelines | ✅ Completo |
| 0 | Codigo de intercambio en vez de JWT en URL | Pendiente |
| C | Auth — Seguridad y Calidad | ✅ Completo |
| D | Defensa en profundidad (localStorage se mantiene) | Parcial |

---

## ~~Pilar A: Auth Flyway — Consolidar migraciones~~

V2–V5 fusionadas en V1, `spring.flyway.schemas: auth` configurado.

---

## ~~Pilar B: CI/CD — Simplificacion de pipelines~~

| # | Accion | Archivo |
|---|--------|---------|
| B.1 | Eliminar `env_file` | `docker-compose.yml` |
| B.2 | Eliminar `cp .env.example` (conservar `-Dspring.profiles.active=integration`) | `ci.yml` |
| B.3 | Reemplazar script por inline SSH | `cd-staging.yml` |
| B.4 | Eliminar `cp .env.example` | `cd-prod.yml` |
| B.5 | Eliminar `scripts/deploy-staging.sh` | — |
| B.6 | Agregar `.env` a `.gitignore` del gateway | `backend/gateway-pymes/.gitignore` |

---

## Pilar 0 (CRITICO) — Codigo de intercambio en vez de JWT en URL

| # | Archivo | Cambio |
|---|---------|--------|
| 0.1 | `OAuth2AuthenticationSuccessHandler.java` | Guardar tokens en Redis con key `oauth:code:<uuid>`, redirect con `?code=<uuid>` |
| 0.2 | `AuthApiController.java` | `@PostMapping("/exchange")` — recibe `{code}`, busca en Redis, devuelve tokens |
| 0.3 | `AuthCallback.vue` | Leer `route.query.code`, `POST /auth/exchange`, usar tokens |
| 0.4 | `SecurityConfig.java` | Agregar `/api/v1/auth/exchange` a WHITE_LIST |

---

## ~~Pilar C: Auth — Seguridad y Calidad~~

Timing attack, account linking OAuth2, rate limit atomico, AntPathRequestMatcher, ExpiredJwtException en WARN.

---

## Pilar D: Defensa en profundidad (localStorage se mantiene)

| # | Accion | Archivo | Como |
|---|--------|---------|------|
| D.1 | Security headers (HSTS, XFO, XCTO, Referrer-Policy) | `gateway-pymes/application.yaml` | `AddResponseHeader` default-filters, sin clase Java |
| D.2 | No loguear JWT | `AuthenticationFilter.java:67` | Quitar `{}` token del mensaje |
| D.3 | No leakear `dbMessage` | `GlobalExceptionHandler.java:253` | Quitar `Map.of("details", dbMessage)` |
| D.4 | No loguear token de verificacion | `EmailVerificationServiceImpl.java:67` | Quitar `Token: {}` del log |
| D.5 | JWT secret sin fallback | `gateway-pymes/application.yaml:47` | `${jwt.secret}` sin default — falla si no esta configurado |
| D.6 | Axios >=1.6.0 | `frontend/pymes/package.json` | `^1.2.1` → `^1.6.0` |
| D.7 | `@Pattern` en reset password | `ResetPasswordRequest.java` | Misma regex que Register |
| D.8 | CORS metodos explicitos | `gateway-pymes/application.yaml` | `"*"` → `"GET,POST,PUT,PATCH,DELETE,OPTIONS"` |
| D.9 | `replaceState` fix para hash routing | `AuthCallback.vue`, `VerifyEmailPage.vue`, `ResetPasswordPage.vue` | `location.hash.replace(/\?.*$/, '')` — limpia query params del hash |
| D.10 | No exponer token de verificacion en URL | `VerifyEmailPage.vue` | `replaceState` inmediato en `onMounted` |
| D.11 | No exponer token de reset en URL | `ResetPasswordPage.vue` | `replaceState` inmediato en `onMounted` |

---

## Post-Mortem: Merge `f959c9e` → `develop` (12-Jun-2026)

Merge de `feature/refactor` contra `develop` con `git merge -X theirs` resulto en merge contaminado que rompio tests de integracion.

### Causa raiz
`develop` tenia migrations V2–V5 (columnas `password`, `deleted_at`, `email_verified_at`, `unique token_hash`) que `feature/refactor` ya incluia en `V1__initial_schema.sql`. El merge trajo V2–V5, Flyway fallo:

```
ERROR: column "password" of relation "users" already exists
```

Flyway falla → JPA no crea EntityManagerFactory → contexto no arranca → tests de integracion fallan.

### Errores
1. `-X theirs` indiscriminado — sobrescribio cambios intencionales y trajo archivos que no debian existir.
2. No revisar migrations post-merge.
3. Perseguir sintomas en vez de causa raiz (modificar configs innecesariamente).

### Lecciones
- Nunca usar `-X theirs` ni `-X ours` sin revisar cada archivo en conflicto.
- Capturar stack trace completo antes de especular: `mvn verify -e | grep -C 10 "Caused by"`.
- Revisar `db/migration/` antes y despues de cada merge.
- Merge correcto: `git merge develop` y resolver manualmente, archivo por archivo.

### Fix
Reset a `f959c9e` (commit sano), cherry-pick de commits utiles (`adc79ae`, `076c31e`, `241ece5`). Estado: `b3e58f8`.
