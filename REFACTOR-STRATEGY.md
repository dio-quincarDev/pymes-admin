# REFACTOR STRATEGY — pymes-admin

Estrategia de refactorización en la rama `refactor`, basada en `DB_STRATEGY.md`, `REFACTOR.md` y `FEEDBACK.md`.

---

## Pilar A: Auth Flyway — Consolidar migraciones

| # | Acción | Archivo | Detalle técnico |
|---|--------|---------|-----------------|
| A.1 | Fusionar V2-V5 en V1 | `V1__initial_schema.sql` | Merge de columnas: `password`, `deleted_at`, `email_verified_at`, unique `token_hash` |
| A.2 | Eliminar V2, V3, V4, V5 | `db/migration/` | Borrar físicamente los 4 archivos |
| A.3 | Agregar `spring.flyway.schemas: auth` | `application.yaml` | Línea bajo `flyway:` para que las tablas se creen en schema `auth` |

---

## Pilar B: CI/CD — Simplificación de pipelines

| # | Acción | Archivo | Detalle técnico |
|---|--------|---------|-----------------|
| B.1 | Eliminar `env_file` | `docker-compose.yml` | Quitar de gateway (L37-38), auth-service (L64-65) y postgres-auth (L85-86). Pasar variables esenciales a `environment:` directo |
| B.2 | Eliminar `cp .env.example` y `-Dspring.profiles.active` | `ci.yml` | Quitar paso en unit tests (L72-73) e integration (L119-120). Quitar flags en L86 y L136 |
| B.3 | Reemplazar script por inline | `cd-staging.yml` | SSH inline: generar `.env` con secrets, `docker compose pull && up -d && prune` |
| B.4 | Eliminar `cp .env.example` | `cd-prod.yml` | Quitar paso L36-38 |
| B.5 | Eliminar `scripts/deploy-staging.sh` | — | Reemplazado por inline en B.3 |
| B.6 | Agregar `.env` a `.gitignore` | `backend/gateway-pymes/.gitignore` | Prevenir commits accidentales de credenciales |

---

---

## Anexo: Mapeo de variables por contexto

Eliminar `env_file: backend/auth/.env` de `docker-compose.yml` (B.1) requiere definir explícitamente de dónde obtiene cada variable cada servicio. A continuación, el mapeo completo:

### A. Dev Local (IDE — Spring Boot directo)

Se conserva `backend/auth/.env` cargado manualmente por el desarrollador. Sin cambios.

### B. Docker Compose (Root `.env` + `environment:`)

El root `.env` se ubica en la raíz del proyecto y Docker Compose lo lee automáticamente. Las variables se pasan a cada servicio vía `environment:`.

**Root `.env` (raíz del proyecto):**

```env
# Docker
DOCKER_USERNAME=pymes
TAG=latest

# Puertos
FRONTEND_PORT=9200
GATEWAY_PORT=8080

# Base de datos
DB_NAME=pymes_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:9200

# JWT
JWT_SECRET=change-this-secret-key-in-production-minimum-256-bits
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
FACEBOOK_CLIENT_ID=your-facebook-client-id
FACEBOOK_CLIENT_SECRET=your-facebook-client-secret
OAUTH2_REDIRECT_URI=http://localhost:8080

# SMTP
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

**`docker-compose.yml` — `environment:` por servicio (además del root `.env`):**

| Servicio | Variables en `environment:` |
|----------|----------------------------|
| `gateway` | `AUTH_SERVICE_HOST=pymes-auth-service`, `REDIS_HOST=pymes-redis-auth`, `SERVER_PORT=8080` |
| `auth-service` | `DB_HOST=pymes-postgres-auth`, `REDIS_HOST=pymes-redis-auth`, `SERVER_PORT=8081` |
| `postgres-auth` | `POSTGRES_DB=${DB_NAME}`, `POSTGRES_USER=${DB_USERNAME}`, `POSTGRES_PASSWORD=${DB_PASSWORD}` |
| `redis` | *(ninguna — usa valores por defecto)* |

### C. GitHub Secrets (CI/CD)

Secrets que deben crearse en `Settings → Secrets and variables → Actions` del repositorio:

| Secret | Propósito |
|--------|-----------|
| `DOCKER_USERNAME` | Usuario de Docker Hub |
| `DOCKER_PASSWORD` | Token/contraseña de Docker Hub |
| `STAGING_HOST` | IP o dominio del servidor staging |
| `STAGING_USER` | Usuario SSH para staging |
| `STAGING_SSH_KEY` | Clave privada SSH para staging |
| `PROD_HOST` | IP o dominio del servidor producción |
| `PROD_USER` | Usuario SSH para producción |
| `PROD_SSH_KEY` | Clave privada SSH para producción |
| `DB_NAME` | Nombre de la base de datos |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `JWT_SECRET` | Secreto para firmar JWT |
| `JWT_ACCESS_EXPIRATION` | Duración del access token (ms) |
| `JWT_REFRESH_EXPIRATION` | Duración del refresh token (ms) |
| `CORS_ALLOWED_ORIGINS_STAGING` | Origen CORS para staging |
| `CORS_ALLOWED_ORIGINS_PROD` | Origen CORS para producción |
| `GOOGLE_CLIENT_ID` | OAuth2 Google Client ID |
| `GOOGLE_CLIENT_SECRET` | OAuth2 Google Client Secret |
| `FACEBOOK_CLIENT_ID` | OAuth2 Facebook Client ID |
| `FACEBOOK_CLIENT_SECRET` | OAuth2 Facebook Client Secret |
| `SPRING_MAIL_USERNAME` | Usuario SMTP |
| `SPRING_MAIL_PASSWORD` | Contraseña SMTP |
| `OAUTH2_REDIRECT_URI` | URI de redirección OAuth2 |

### D. Archivo local `backend/auth/.env` (dev local)

Se conserva como está. Contiene las mismas variables que el root `.env` pero con valores de desarrollo local. Este archivo está en `.gitignore` y nunca se committea.

---

## Pilar C: Auth — Seguridad y Calidad

| # | Prioridad | Issue | Archivo | Solución |
|---|-----------|-------|---------|----------|
| C.1 | 🔴 Crítico | Timing attack en forgot-password | `PasswordResetServiceImpl:45-49` | Ejecutar dummy delay cuando usuario no existe para uniformar tiempo de respuesta |
| C.2 | 🔴 Crítico | Crash 500 en login Google con cuenta LOCAL existente | `CustomOAuth2UserService:37-48` | Buscar por email primero; si existe, enlazar cuenta (account linking) en vez de insertar |
| C.3 | 🔴 Crítico | Race condition en rate limit (TTL no atómico) | `RateLimitService:28-43` | Usar `SETEX` o script Lua para increment + expire atómico. Sliding window como mejora |
| C.4 | 🟡 Importante | Tests con assertions incorrectas | `AuthApiIntegrationTest:97-109` | Assert `409 Conflict` en register duplicado. Assert `400` en forgotPasswordInvalidEmail |
| C.5 | 🟡 Importante | Bypass `path.contains` en filtro JWT | `JwtAuthenticationFilter:43-49` | Usar `path.startsWith()` o `AntPathMatcher` en vez de `contains` |
| C.6 | 🟢 Mantenimiento | Logs `ExpiredJwtException` en ERROR | `JwtAuthenticationFilter` (o handler) | Bajar a DEBUG o WARN — la expiración es esperada y no es error |

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
