# Daily Reports — pymes-admin

Registro cronológico de decisiones técnicas, refactors y post-mortems del proyecto.

---

## 2026-06-21 — Core: Global Exception Handler + Industry Validation

### Que se hizo

1. **GlobalExceptionHandler** (`common/exception/`)
   - `EntityNotFoundException` → 404
   - `MethodArgumentNotValidException` → 400 (validation errors)
   - `IllegalArgumentException` → 400
   - `Exception` → 500 (log + mensaje generico)

2. **Industry validation** en `completeOnboarding`
   - SetupServiceImpl inyecta `JdbcTemplate`
   - Verifica `SELECT COUNT(*) FROM industries WHERE code = ?` antes de settear industry
   - Si no existe → lanza `IllegalArgumentException("Industry not found: ...")`

3. **No se copia seed data a tablas tenant-specific**
   - Templates se usan globales filtrados por `industry_code`
   - Decisión: Opcion C del analisis (YAGNI)

### Tests agregados

- Unit: `completeOnboarding_InvalidIndustry_Throws` (verifica excepcion + no llamadas a repo)
- Integration: POST con industry inexistente → 400 + mensaje de error

### Files tocados

- `common/exception/GlobalExceptionHandler.java` (nuevo)
- `setup/service/impl/SetupServiceImpl.java` (+JdbcTemplate, +validation)
- `test/unit/SetupServiceImplTest.java` (+mock JdbcTemplate, +invalid test)
- `test/integration/SetupSeedIntegrationTest.java` (+invalid industry test)

---

---

## 2026-06-21 — Cleanup AuthApiController: Business Logic Extraction

### Problemas

AuthApiController tenia logica de negocio que no le correspondia:
- Redis access directo en exchange (get/delete ops)
- Bearer token parsing manual en logout
- Condicion muerta `if (accessToken != null)` en register (siempre null)

### Solucion

- `AuthServiceImpl.exchange()` encapsula acceso a Redis y construccion de DTOs
- `AuthServiceImpl.logout(HttpServletRequest)` extrae el Bearer token internamente
- `register`: eliminada condicion muerta, retorna 200 directamente
- Eliminadas importaciones innecesarias (RedisTemplate, StringUtils, HttpStatus)
- Controller: **0 logica de negocio**, 9 metodos, 9 one-liners de delegacion

### Decision clave

No se fusionaron `EmailVerificationService` ni `PasswordResetService` en `AuthService` — son dominios distintos (YAGNI). Controller con 3 services inyectados es Spring idiomatico.

### Files tocados

- `controller/impl/AuthApiController.java`
- `service/AuthService.java`
- `service/impl/AuthServiceImpl.java`
- `test/unit/AuthServiceImplTest.java`

### Resultado

126 tests unitarios, 0 fallos, BUILD SUCCESS.

---

## 2026-06-21 — Core Service: Seed Data y Test Infrastructure

### Problema

Core service solo tenia la tabla `tenant_setup` sin datos de referencia.
Al registrar un tenant no habia categorias, ubicaciones ni configuracion
precargada por industria.

### Solucion

- Flyway V2: tablas `industries`, `template_categories` (3 niveles con
  padre auto-ref), `template_locations`
- `SeedDataRunner`: componente idempotente que inserta seed data al
  startup via JdbcTemplate
- 3 industrias seedadas: restaurante (~59 categorias, 5 ubicaciones),
  bares (~61, 5), salon belleza (~68, 4)
- Seed data en Java (ApplicationRunner), no en SQL, para mantener
  flexibilidad
- `SetupServiceImplTest`: 4 tests unitarios con Mockito
- `AbstractIntegrationTest`: clase base con Testcontainers PostgreSQL
- `SetupSeedIntegrationTest`: 5 tests de integracion (seed + API)
- Testcontainers 1.21.4 agregado a pom.xml

### Decision clave

Seed data se implemento como Java component (no Flyway SQL) y solo
categorias/locations (sin units ni movement reasons) porque el modulo
inventario aun no existe (YAGNI).

### Files tocados

- `db/migration/V2__config_schema.sql`
- `common/seed/SeedDataRunner.java`
- `test/java/core_pymes/unit/SetupServiceImplTest.java`
- `test/java/core_pymes/integration/AbstractIntegrationTest.java`
- `test/java/core_pymes/integration/SetupSeedIntegrationTest.java`
- `pom.xml` (testcontainers dependency)

### Resultado

10 tests (4 unit + 6 integration), 0 fallos, BUILD SUCCESS.

---

## 2026-06-12 — Post-Mortem: Merge catastrófico `feature/refactor` → `develop`

### Qué pasó

Merge de `feature/refactor` (commit `f959c9e`) contra `develop` usando `git merge -X theirs` para resolver conflictos automáticamente. Resultado: merge contaminado que rompió todos los tests de integración.

### Causa raíz

`develop` tenía migrations V2–V5 que agregaban columnas (`password`, `deleted_at`, `email_verified_at`, `token_hash`) que `feature/refactor` ya incluía dentro de `V1__initial_schema.sql` (consolidada). El merge con `-X theirs` trajo V2–V5, causando que Flyway intentara `ALTER TABLE ... ADD COLUMN` sobre columnas ya existentes:

```
ERROR: column "password" of relation "users" already exists
```

→ Flyway falla → JPA no crea EntityManagerFactory → contexto no arranca → todos los tests de integración fallan.

### Errores cometidos

1. **`-X theirs` indiscriminado** — resolvió TODOS los conflictos tomando la versión de `develop`, sobrescribiendo cambios intencionales de `feature/refactor`.
2. **No revisar migrations post-merge** — no se verificó que V2–V5 eran incompatibles con V1 consolidada.
3. **Perseguir síntomas** — se modificaron `application.yaml` y `application-integration.yaml` innecesariamente. El único problema era V2–V5.

### Lecciones

- **Nunca usar `-X theirs` ni `-X ours`** sin revisar cada archivo en conflicto.
- **Ante error de contexto en tests**, capturar el stack trace completo antes de especular: `mvn verify -e | grep -C 10 "Caused by"`.
- **Revisar `db/migration/`** antes y después de cada merge.
- El merge correcto: `git merge develop` y resolver manualmente, archivo por archivo.

### Fix aplicado

- Reset de `feature/refactor` a `f959c9e` (commit sano previo al merge).
- Cherry-pick de commits post-merge útiles:
  - `adc79ae` — CI/CD workflows + SECRETS.md
  - `076c31e` — WebCorsConfig + CORS env vars
  - `241ece5` — Best logic for CI/CD
- Sin V2–V5, sin cambios extra en configs.
- Estado post-fix: `b3e58f8`

---

## 2026-06-16 — Refactor CI/CD y Simplificación de Pipelines

### Problemas resueltos

| Componente | Problema | Fix |
|------------|----------|-----|
| `docker-compose.yml` | `env_file: backend/auth/.env` no existe en CI ni en el servidor | Eliminado — variables via `${VAR}` resueltas desde `.env` generado dinámicamente |
| `ci.yml` | `-Dspring.profiles.active=test/integration` conflictuaba con `@DynamicPropertySource` | Eliminado — Testcontainers inyecta URLs automáticamente |
| `cd-staging.yml` / `cd-prod.yml` | Llamada a `scripts/deploy-staging.sh` externo | Reemplazado por inline SSH con heredoc que genera `.env` desde GitHub Secrets |
| Todos los workflows | `cp .env.example .env` en pipeline | Eliminado — credenciales reales no deben generarse desde un ejemplo |

### Cómo funciona el deploy ahora

El workflow SSH genera el `.env` en el servidor sobre la marcha:
```bash
cat > .env <<EOF
DOCKER_USERNAME=${{ secrets.DOCKER_USERNAME }}
TAG=${{ needs.build-and-test.outputs.version }}
DB_USERNAME=${{ secrets.DB_USERNAME }}
DB_PASSWORD=${{ secrets.DB_PASSWORD }}
JWT_SECRET=${{ secrets.JWT_SECRET }}
CORS_ALLOWED_ORIGINS=${{ secrets.CORS_ALLOWED_ORIGINS_STAGING }}
EOF
docker compose pull
docker compose up -d
docker image prune -af
```

### Archivos eliminados

- `scripts/deploy-staging.sh` — sustituido por inline SSH

---

## 2026-06-16 — Refactor de Seguridad (Pilar D)

Implementados como default-filters sin nueva clase Java:

| Item | Cambio | Archivo |
|------|--------|---------|
| Security headers | HSTS, X-Frame-Options DENY, X-Content-Type-Options nosniff, Referrer-Policy | `gateway-pymes/application.yaml` |
| JWT sin fallback | `${jwt.secret}` sin default — falla en startup si no está configurado | `gateway-pymes/application.yaml` |
| No loguear JWT | Quitado `{}` token del mensaje de log | `AuthenticationFilter.java` |
| No leakear dbMessage | Quitado `Map.of("details", dbMessage)` | `GlobalExceptionHandler.java` |
| No loguear token de verificación | Quitado `Token: {}` del log | `EmailVerificationServiceImpl.java` |
| CORS métodos explícitos | `"*"` → `"GET,POST,PUT,PATCH,DELETE,OPTIONS"` | `gateway-pymes/application.yaml` |
| `@Pattern` en reset password | Misma regex que Register | `ResetPasswordRequest.java` |
| Axios ≥1.6.0 | `^1.2.1` → `^1.6.0` | `frontend/pymes/package.json` |

---

## 2026-04-inicial — Refactor Auth Flyway + Seguridad

### Flyway consolidada (Pilar A)

V2–V5 fusionadas en V1, `spring.flyway.schemas: auth` configurado. Una sola migración de estado final.

### OAuth2 Code Exchange (Pilar 0 — CRÍTICO)

JWT en URL → código de un solo uso. Implementado en:
- `OAuth2AuthenticationSuccessHandler.java` — guarda tokens en Redis con key `oauth:code:<uuid>`, redirect con `?code=<uuid>`
- `AuthApiController.java` — `POST /exchange`: recibe `{code}`, busca en Redis, devuelve tokens
- `AuthCallback.vue` — lee `route.query.code`, `POST /auth/exchange`, usa tokens
- `SecurityConfig.java` — `/api/v1/auth/exchange` en WHITE_LIST

### Auth Seguridad (Pilar C)

Timing attack, account linking OAuth2, rate limit atómico, `AntPathRequestMatcher`, `ExpiredJwtException` en WARN. Todo implementado.

---

## 2026-03 — Problemas de Infraestructura OCI Resueltos

### ARM64 vs AMD64

**Problema:** GitHub Actions construía imágenes para AMD64. Oracle Cloud Free Tier usa ARM64 → `no matching manifest for linux/arm64/v8`.

**Fix:** Docker Buildx con `platforms: linux/amd64,linux/arm64` en los workflows.

### Red Docker "already exists"

**Problema:** `network pymes-internal-network exists but was not created by compose` — la red fue creada manualmente por `setup-server.sh`.

**Fix:** `pymes-internal-network` marcada como `external: true` en `docker-compose.yml`.

### Re-compilación en servidor

**Problema:** `docker compose up` disparaba build local por falta de directiva `image:`.

**Fix:** Tags explícitos `${DOCKER_USERNAME}/pymes-auth:${TAG}` en `docker-compose.yml`.

---

---

*Nota: El post-mortem del merge catastrofico (2026-06-12) esta consolidado en `REFACTOR-STRATEGY.md` (raiz). Este archivo mantiene el registro historico completo.*

*Creado: 2026-06-19 | Consolidacion de REFACTOR-STRATEGY.md + .github/REFACTOR.md*
