# Daily Reports — pymes-admin

Registro cronológico de decisiones técnicas, refactors y post-mortems del proyecto.

---

## 2026-08-13 — Sync feature/core ← develop + docs de infra al día

### Contexto

`feature/core` estaba desactualizado (8 commits atrás de `develop`). Se hizo `git merge develop` con resolución manual de conflictos y se pusieron al día los docs de infraestructura.

### Qué se hizo

**Merge develop → feature/core:**
- Conflicto resuelto en `backend/auth/README.md` (env vars table): se fusionó la descripción detallada de `APP_FRONTEND_URL` de feature/core + filas nuevas de develop (`OAUTH2_REDIRECT_URI`, `FACEBOOK_*`).

**Redes Docker renombradas:**
- `pymes-global-network` (Nginx Proxy Manager) → **`proxy-caddy-network`** (Caddy). `setup-server.sh` y doc infra desactualizados corregidos en `.github/` + `docs/strategies/INFRA_STRATEGY.md`.

**Docs actualizados:**
- `docs/strategies/INFRA_STRATEGY.md`: sección producción alineada al deploy real (Caddy HTTPS + Let's Encrypt en 80/443, no Cloudflare HTTP-only), redes corregidas, lista de secrets completa.
- `.github/SECRETS.md`: +`APP_FRONTEND_URL`, +`SPRING_MAIL_HOST/PORT`, +`SPRING_PROFILES_ACTIVE_STAGING/PROD`; CORS examples al dominio real; security list corregida (22/80/443).
- Daily reports de servicios: entries de deploy agregados.

### Deploy fixes documentados (de develop, 2026-08-10/11)

- `workflow_run.head_sha` como base del version tag (antes `GITHUB_SHA=main` daba tags de rama equivocada).
- `SPRING_PROFILES_ACTIVE` inyectado a staging/prod en CD.
- `nginx.conf`: `sw.js` con `no-cache` (antes matcheaba la regla `immutable` de `.js`) + `/` con `no-cache`.
- CORS con fallback `http://localhost:9200` en `application-stg.yaml`.

**Estado:** ✅ COMPLETADO

---

## 2026-08-10 — Migración de dominio a pymeq.dioquincar.dev + PWA install

### Contexto

Subdominio migrado de `pymes.dioquincar.dev` a `pymeq.dioquincar.dev` con HTTPS, para habilitar el login/registro con Google (OAuth2).

### Qué se hizo

- **GCP**: nuevo Authorized redirect URI `https://pymeq.dioquincar.dev/login/oauth2/code/google`.
- **Secrets GitHub**: `OAUTH2_REDIRECT_URI`, `CORS_ALLOWED_ORIGINS_STAGING`, `APP_FRONTEND_URL` → `https://pymeq.dioquincar.dev`.
- **Caddy (instancia)**: bloques https del nuevo subdominio, rutas `/api/*`, `/oauth2/*`, `/login/*` → gateway.
- **Frontend**: URLs de OAuth por `window.location.origin` (`LoginPage`, `RegisterPage`, `AuthOptionsPage`) + banner PWA corregido (prompt real en Android, instrucciones iOS).
- **Docs**: `DEPLOYMENT.md`, `QUICK_START.md`, `auth/README.md` actualizados al nuevo dominio.

**Estado:** ✅ COMPLETADO

---

## 2026-08-05 — Consolidación de migraciones Core V1–V9 + Performance Indexes

### Contexto

Las migraciones V1–V9 del core service acumulaban contradicciones de esquema y operaciones redundantes. V1 definía columnas/índices que V3–V9 modificaban inmediatamente. Fresh deploy ejecutaba 9 archivos con DDL contradictorio.

### Qué se hizo

- **V1 reescrito**: absorbidas V3–V9 (columnas, índices, FK). Estado final limpio en un solo archivo.
- **V2 extendido**: `provider_id` en `gastos_fijos_recurrentes` + FK `invoices→collaboradores`.
- **V3 nueva**: 5 performance indexes (2 partial para costos CTE, 2 covering para analytics, 1 para invoice number).
- **V3–V9 originales eliminadas** (7 archivos).
- Docs actualizados: `CORE.md`, `ANALYTICS.md`, `COSTOS_ENGINE.md`, `CORE_MIGRATIONS_STRATEGY.md`, `TO_DO.md`.

### Resultado

```
db/migration/
├── V1__core_schema.sql         (12.7 KB) — Esquema consolidado
├── V2__costos_engine.sql       (1.7 KB)  — Costos engine + FK
└── V3__performance_indexes.sql (1.2 KB)  — 5 indexes
```

173 unit + 45 integration = **218 tests, 0 failures**.

---

## 2026-07-28 — TeamsPage migration + onAccept email mismatch fix

### Contexto

UX/UI issues reportados por usuario: colores invertidos en formulario, nombres de botones no visibles. Se procedió a investigar y corregir bugs críticos del sistema de invitación.

### Qué se hizo

**Backend (auth):**
- `selectTenant(tenantId)` agregado a `AuthStore` — llama `tenantService.selectTenant(tenantId)`

**Frontend:**
- `TeamsPage.vue` movido de `modules/core/pages/` → `modules/auth/pages/` (es funcionalidad auth, no core)
- `AcceptInvitationPage.vue` bug fix: `onAccept()` ya no castea como `AuthResponse`; ahora hace `await invitationService.accept(token)` + `fetchCurrentUser()`
- Nuevo computed `emailMismatch`: valida que email autenticado coincida con email de la invitación
- Router: `/teams` removido de `coreRoutes`, nuevo `authDashboardRoutes` en auth

**UX/UI issues identificados (pendientes):**
- BaseButton usa `<slot />` pero AcceptInvitationPage/TeamsPage pasan `label="..."` → botones renderizan vacíos
- Form fields usan `outlined dense` sin `dark filled color="primary" label-color="accent"` → colores invertidos en dark mode

### Archivos modificados

```
frontend/pymes/src/modules/auth/pages/AcceptInvitationPage.vue   # onAccept fix + email mismatch
frontend/pymes/src/modules/auth/pages/TeamsPage.vue               # moved from core
frontend/pymes/src/modules/auth/store/index.ts                   # selectTenant action
frontend/pymes/src/modules/auth/router/routes.ts                 # authDashboardRoutes
frontend/pymes/src/modules/core/router/routes.ts                 # removed /teams route
frontend/pymes/src/router/routes.ts                              # imports authDashboardRoutes
backend/auth/docs/DAILY_REPORTS_AUTH_SOLUTIONS.md                # entry added
frontend/pymes/docs/DAILY_REPORTS_FRONTEND.md                    # entry added
```

### Tests

140 auth + 37 gateway unit tests + 9 integration tests = todos pasan. Lint + build frontend limpio.

---

## 2026-07-29 — Invitación: accept endpoint quitado de WHITE_LIST (CI fix)

### Contexto

3 tests de `SecurityConstraintIntegrationTest$InsufficientRoleTests.setUpRoles` fallaban en CI con 400. Causa raíz: `POST /api/v1/invitations/accept` estaba en WHITE_LIST (permitAll), el JWT filter lo saltaba, Spring Security creaba autenticación anónima con principal `"anonymousUser"`, el email no coincidía con la invitación → 400.

### Qué se hizo

**Auth `SecurityConfig.java`:** eliminado `/invitations/accept` de WHITE_LIST
**Gateway `RouterValidator.java`:** eliminado `/api/v1/invitations/accept` de `openEndPoints`
**Frontend:** sin cambios (el interceptor de axios inyecta el token automáticamente)

### Archivos modificados

```
backend/auth/src/main/java/auth/pymes/common/config/SecurityConfig.java
backend/gateway-pymes/src/main/java/dev/dioquincar/gateway_pymes/filter/RouterValidator.java
```

### Tests

54 integration tests (3 de InsufficientRoleTests ahora pasan ✅), 140 auth unit, 37 gateway unit, frontend lint + build = todo verde.

---

## 2026-07-27 — CI/CD Security Hardening + Efficiency

### Contexto

Dos revisiones independientes (GitHub Actions Efficiency + GitHub Actions Hardening) identificaron vulnerabilidades de seguridad y desperdicio de tiempo en los 3 workflows (`ci.yml`, `cd-staging.yml`, `cd-prod.yml`).

### Qué se hizo

**Security (PR1):**

1. **`permissions: {}` (deny-all)** — Todos los workflows ahora empiezan con permisos denegados. Cada job declara solo `contents: read`. Si un step es comprometido, el token no puede pushear código ni crear releases.

2. **`persist-credentials: false`** — Todos los `actions/checkout` ahora setean esta opción. El `GITHUB_TOKEN` ya no se escribe en `.git/config`, evitando que código no confiable lo robe.

3. **SHA-pinning de acciones de terceros** — `mikepenz/action-junit-report@v4` → SHA fijo (v6.4.1), `appleboy/ssh-action@v1.0.3` → SHA fijo (v1.2.5). Tags mutables ya no se usan para acciones de terceros.

**Efficiency (PR2):**

4. **CD tests redundantes eliminados** — `cd-staging.yml` y `cd-prod.yml` ya no ejecutan `mvn test -B`. CI ya validó estos tests. Ahorro: ~12-16 min/deploy.

**Efficiency (PR3):**

5. **`docker-build-check` limitado a develop/main** — Ya no corre en feature branches. Ahorro: ~3-5 min/CI en feature branches.

### Archivos modificados

```
.github/workflows/ci.yml          # +permissions, +persist-credentials, SHA-pin junit-report, +docker-build-check condition
.github/workflows/cd-staging.yml  # +permissions, +persist-credentials, SHA-pin ssh-action, -3 steps mvn test
.github/workflows/cd-prod.yml     # +permissions, +persist-credentials, SHA-pin ssh-action, -3 steps mvn test
```

### Ahorro estimado

| Concepto | Ahorro |
|----------|--------|
| CD tests redundantes | ~12-16 min/deploy |
| CI docker-build-check | ~3-5 min/CI (feature branches) |
| Seguridad | Tokens least-privilege, supply chain protegida |

---

## 2026-06-24 — Onboarding: preview de categorías/subcategorías por industria

### Contexto

El sistema PYMEQ analiza gastos en facturas, no inventario. El onboarding actual carga categorías planas sin jerarquía. Para una mejor experiencia de usuario, se quiere mostrar un preview de las categorías y subcategorías antes de confirmar el onboarding.

### Qué se planea

1. **Backend**: Agregar `GET /setup/preview/{industry}` — endpoint de solo lectura que retorna categorías jerárquicas sin persistir.
2. **Backend**: Actualizar `SetupResponse.ItemDTO` con `parentId` (nullable) + `children` (nested list) para representar la jerarquía de 3 niveles.
3. **Backend**: Método `buildCategoryTree()` que convierte la lista plana de la query en un árbol anidado.
4. **Frontend**: Componente `CategoryTree.vue` que muestra el árbol de categorías visualmente (solo lectura).
5. **Frontend**: OnboardingPage flujo de 2 pasos: paso 1 seleccionar industria, paso 2 preview del árbol de categorías → "Comenzar" → POST onboarding → dashboard.
6. **Frontend**: Nuevo método `preview()` en `setup.service.ts`.
7. **Frontend**: Actualizar tipo `SetupInfo` con categorías jerárquicas (`parentId` + `children`).

### Decisión clave

`ItemDTO` se mantiene como el mismo DTO para categorías, unidades y ubicaciones. Las categorías anidan en `children`, unidades y ubicaciones siguen siendo listas planas. No se crea un DTO separado por nivel (YAGNI).

### Files a crear/modificar

```
backend/core/src/main/java/core_pymes/setup/dto/SetupResponse.java          # +parentId, +children
backend/core/src/main/java/core_pymes/setup/service/impl/SetupServiceImpl.java  # +buildCategoryTree, +previewIndustry
backend/core/src/main/java/core_pymes/setup/controller/SetupApi.java         # +GET /preview/{industry}
backend/core/src/main/java/core_pymes/setup/controller/impl/SetupController.java  # impl del endpoint
backend/core/src/test/java/core_pymes/unit/SetupServiceImplTest.java         # +buildCategoryTree test
backend/core/src/test/java/core_pymes/integration/SetupSeedIntegrationTest.java  # +preview integration test

frontend/pymes/src/modules/core/types/index.ts                               # +parentId, +children en SetupInfo
frontend/pymes/src/modules/core/services/setup.service.ts                    # +preview()
frontend/pymes/src/components/onboarding/CategoryTree.vue                    # nuevo componente
frontend/pymes/src/modules/core/pages/OnboardingPage.vue                     # flujo 2 pasos
```

### Test plan

- Unit: `buildCategoryTree()` con lista de 3 niveles → árbol anidado correcto.
- Unit: `previewIndustry()` retorna setup con categorías jerárquicas.
- Integration: `GET /setup/preview/restaurante` → 200 + categorías con `children`.
- Frontend: `CategoryTree` renderiza subcategorías indentadas.
- Frontend: `OnboardingPage` paso 1 → paso 2 → "Comenzar" → dashboard.

---

## 2026-06-24 — Docker PostgreSQL rename + Template loading + Auth test fix + Frontend onboarding redirect

### Que se hizo

1. **Docker Compose: rename PostgreSQL container/service**
   - Service `postgres-auth:` → `postgres:`
   - Container name `pymes-postgres-auth` → `pymes-postgres-db`
   - Volume `pymes-postgres-auth-data` → `pymes-postgres-db-data`
   - `DB_HOST=pymes-postgres-auth` → `pymes-postgres-db` en auth-service y core-service
   - `depends_on: postgres-auth` → `postgres:` en ambos services

2. **Core: SetupResponse DTO + template loading en endpoints**
   - `SetupResponse` record DTO con `categories[]`, `units[]`, `locations[]`
   - `SetupMapper` (MapStruct) que transforma entity + lists → DTO
   - `GET /core/setup/{tenantId}` y `POST /core/setup/{tenantId}/onboarding` devuelven `SetupResponse` con template data filtrada por industry

3. **Auth: UserServiceImplTest fix (4 errores)**
   - `@Mock` faltantes para `UserTenantRepository` y `TenantRepository`
   - Stubs huérfanos de `userMapper` removidos
   - 126 tests unitarios pasan (antes: 122)

4. **Frontend: Onboarding auto-redirect post verifyEmail**
   - `auth store`: mergea `authData.activeTenant.id` en user para poblarlo con `tenantId` (UserMapper lo ignoraba)
   - `VerifyEmailPage.vue`: después de verify exitoso, llama `setupService.get(tenantId)` → redirect automático a `/onboarding` si `!onboardingCompleted`
   - `types/index.ts`: se agrega `activeTenant?: { id, name, slug }` a `AuthResponse`

5. **Frontend: ProductosPage con template options**
   - category y `baseUnit` cambiaron de `<q-input>` a `<q-select>` con opciones cargadas desde `GET /setup/{tenantId}`
   - `setup.service.ts`: `completeOnboarding` devuelve `SetupInfo` en vez de `TenantSetup`

### Files tocados

```
docker-compose.yml                              # rename postgres-auth → postgres
backend/core/src/main/java/core_pymes/setup/    # SetupResponse, SetupMapper, SetupService, SetupController
backend/core/src/test/java/core_pymes/unit/     # SetupServiceImplTest actualizado
backend/auth/src/test/java/auth/pymes/unit/     # UserServiceImplTest fix
frontend/pymes/src/modules/auth/store/index.ts  # activeTenant merge en verifyEmail
frontend/pymes/src/modules/auth/types/index.ts  # +activeTenant field
frontend/pymes/src/modules/auth/pages/VerifyEmailPage.vue  # onboarding redirect
frontend/pymes/src/modules/core/pages/ProductosPage.vue    # q-select con template options
frontend/pymes/src/modules/core/services/setup.service.ts  # completeOnboarding return type
```

### Tests

| Suite | Tests | Resultado |
|-------|-------|-----------|
| Core unit | 18 | ✅ |
| Auth unit | 126 | ✅ |
| Frontend build | — | ✅ |

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

---

## 2026-06-23 — Core: Módulos Product + Invoice (Fase 1 - Parte 2)

### Qué se hizo

Dos módulos modulares completos con eventos asíncronos, siguiendo patrón `setup/`.

### Módulo Product (`core_pymes/product/`)

- `Producto` + `Presentacion` entidades JPA (soft-delete, timestamps, dual-field FK)
- CRUD completo: `GET/POST/PUT/DELETE /api/v1/core/productos`, `GET/POST/DELETE presentaciones`
- `ProductoMapper` (MapStruct — primer mapper real del proyecto)
- Eventos: `ProductoCreadoEvent`, `PresentacionCreadaEvent`
- Flyway V3: `core.products`, `core.product_presentations`

### Módulo Invoice (`core_pymes/invoice/`)

- `Proveedor`, `Factura`, `ItemFactura` entidades JPA (cascade, snapshots, soft-delete)
- CRUD completo: `GET/POST/PUT/DELETE /api/v1/core/proveedores`, `GET/POST /api/v1/core/facturas`, `POST /api/v1/core/facturas/{id}/pagar`
- Invoice number auto-generado: `F-PROV-{year}-{sequential:04d}` por tenant
- Eventos: `FacturaCreadaEvent`, `FacturaPagadaEvent`
- Flyway V4: `core.providers`, `core.invoices`, `core.invoice_items`

### Testing

- `ProductoServiceImplTest`: 6 unit tests (CRUD + eventos + tenant isolation)
- `FacturaServiceImplTest`: 7 unit tests (total calc + descuento + estados + tenant isolation)
- `ProductoIntegrationTest` (4 tests) + `FacturaIntegrationTest` (4 tests) — pendientes de Docker
- Total: 18 unit tests, 0 fallos, BUILD SUCCESS

### Arquitectura

- Estructura modular: `setup/`, `product/`, `invoice/` con controller/domain/dto/event/mapper/repository/service
- Todos los controllers con interface+impl pattern
- EventConfig con `@EnableAsync` + virtual threads
- `CorePath.java` actualizado con rutas de producto, proveedor, factura

### Decisiones clave

- Soft-delete con `@SQLDelete` + `@Where` en todas las entities
- Dual-field relation pattern (UUID field + @ManyToOne read-only) para FK
- Response sin wrapper (consistente con módulo setup existente)
- Nomenclatura en español (Producto, Factura, Proveedor)

### Files creados/tocados

```
backend/core/src/main/java/core_pymes/product/     # 12 archivos (controller/, domain/, dto/, event/, mapper/, repository/, service/)
backend/core/src/main/java/core_pymes/invoice/     # 14 archivos (misma estructura)
backend/core/src/main/resources/db/migration/V3__product_schema.sql
backend/core/src/main/resources/db/migration/V4__invoice_schema.sql
backend/core/src/main/java/core_pymes/common/constant/CorePath.java
backend/core/src/test/java/core_pymes/unit/ProductoServiceImplTest.java  (6 tests)
backend/core/src/test/java/core_pymes/unit/FacturaServiceImplTest.java   (7 tests)
backend/core/src/test/java/core_pymes/integration/ProductoIntegrationTest.java  (4 tests)
backend/core/src/test/java/core_pymes/integration/FacturaIntegrationTest.java   (4 tests)
backend/core/docs/PROGRESS.md       (actualizado)
backend/core/docs/FASE1_CORE.md     (checklist actualizado)
```

### Próximo

- Módulo Accounting: MetricaFinanciera + listener + endpoint de resumen financiero
- Pendiente decisión: Gastos Operativos (tipo=GASTO vs módulo aparte)
- Pendiente decisão: Ventas (módulo propio vs desde Movimientos)
- Integration tests contra Docker real

---

## 2026-06-23 — Frontend: Onboarding Post-Login

### Qué se hizo

Página `/onboarding` con selección de industria + router guard que redirige si `onboardingCompleted=false`.

### Detalle

- **Setup service**: `GET /core/setup/{tenantId}` + `POST /core/setup/{tenantId}/onboarding`
- **OnboardingPage.vue**: 8 cards clickeables (restaurante, bares, salon_belleza, ferreteria, mini_super, taller_mecanico, farmacia, default)
- **Check de onboarding**: en `AuthCallback.vue` después del exchange (no en router guard — evitar async en cada navegación)
- **Ruta `/onboarding`**: standalone (fuera de `/dashboard`), `meta: { requiresAuth: true }`

### Gateway

Ambas rutas `/api/v1/core/setup/**` pasan con JWT — confirmado en `RouterValidator` (no están en `openEndPoints`) + `AuthenticationFilter` (valida firma + blacklist).

### Files creados/tocados

```
frontend/pymes/src/modules/core/services/setup.service.ts      (nuevo)
frontend/pymes/src/modules/core/pages/OnboardingPage.vue       (nuevo)
frontend/pymes/src/modules/core/router/routes.ts               (+onboardingRoute export)
frontend/pymes/src/router/routes.ts                            (+onboardingRoute import)
frontend/pymes/src/modules/auth/pages/AuthCallback.vue         (+setup check post-login)
```

---

## 2026-06-23 — Frontend: Módulo Core (Productos, Proveedores, Facturas)

### Qué se hizo

Módulo `src/modules/core/` con 4 páginas CRUD, servicios API y rutas.

### Detalle

- **Types**: DTOs (`Producto`, `Presentacion`, `Proveedor`, `Factura`, `ItemFactura`, `SetupInfo`)
- **Services**: `producto.service.ts` (CRUD + presentaciones), `proveedor.service.ts`, `factura.service.ts` (CRUD + pagar)
- **Pages**: ProductosPage, ProveedoresPage, FacturasPage, ConfiguracionPage — todas con QTable + QForm + QDialog
- **Router**: `/core/productos`, `/core/proveedores`, `/core/facturas`, `/core/configuracion`
- **Integración**: `coreRoutes` merge en dashboard children, sidebar links actualizados
- **TenantId**: derivado de `authStore.user?.tenantId`

### Build

- `vue-tsc --noEmit`: ✅ 0 errores
- `npm run lint`: ✅ 0 errores
- `npm run build`: ✅ exitoso

### Files creados/tocados

```
frontend/pymes/src/modules/core/         # 9 archivos nuevos
frontend/pymes/src/router/routes.ts      # +coreRoutes import
frontend/pymes/src/layouts/MainLayout.vue # +sidebar links
```

### Pendiente menor

- Reorder SFC a `<script>` → `<template>` → `<style>` en FacturasPage + ConfiguracionPage
- Reemplazar `Math.random()` como key en MainLayout sidebar v-for

---

---

## 2026-06-30 — Motor de Análisis de Gastos por Producto

### Contexto

El sistema tenía analytics a nivel de factura (ABC, tendencias, márgenes) pero no
un análisis granular por producto. La visión es un motor de gastos donde cada
producto acumula inversión total, precio unitario del último pedido y fecha de
última compra, alimentado automáticamente desde las facturas.

### Qué se hizo

**Data model (V7):**
- `core.products`: `last_unit_price`, `total_investment`, `last_purchase_date`,
  `min_quantity`, `max_quantity`
- `core.template_products`: `min_quantity`, `max_quantity`
- `Producto.java` / `ProductoResponse` / `ProductoRequest` / `ProductoMapper`
  actualizados con los 5 nuevos campos

**Motor de gastos en facturas:**
- `FacturaServiceImpl.createFactura()` — por cada item, ejecuta
  ```sql
  UPDATE core.products SET last_unit_price = ?,
    total_investment = total_investment + ?,
    last_purchase_date = ?
  WHERE id = ? AND tenant_id = ?
  ```
- Inline en el loop existente, misma transacción, sin nuevo batch

**Onboarding:**
- `SetupServiceImpl.completeOnboarding()` copia `min_quantity`/`max_quantity`
  desde template al completar onboarding
- SeedDataRunner actualiza INSERT de template_products con los 2 nuevos campos

**Frontend — Análisis de Gastos (nueva página):**
- `AnalisisGastosPage.vue` en `/dashboard/analisis-gastos`
- 4 cards resumen: Inversión Total, Productos, Categorías, Alertas
- Inversión por Categoría con barras de progreso (client-side)
- Alertas: excedió max, debajo de min, sin compras >60d
- Tabla de Últimos Precios Unitarios con filtro y sort
- Nav link en sidebar

### Files creados/modificados

```
backend/core/src/main/resources/db/migration/V7__product_expense_fields.sql  # nuevo
backend/core/src/main/java/core_pymes/product/domain/Producto.java           # +5 campos
backend/core/src/main/java/core_pymes/product/dto/ProductoResponse.java      # +5 campos
backend/core/src/main/java/core_pymes/product/dto/ProductoRequest.java       # +minQuantity, maxQuantity
backend/core/src/main/java/core_pymes/product/mapper/ProductoMapper.java     # mapeo nuevos campos
backend/core/src/main/java/core_pymes/product/service/impl/ProductoServiceImpl.java  # create/update maneja min/max
backend/core/src/main/java/core_pymes/invoice/service/impl/FacturaServiceImpl.java   # product stats update
backend/core/src/main/java/core_pymes/common/seed/SeedDataRunner.java        # columns en template_products
backend/core/src/main/java/core_pymes/setup/service/impl/SetupServiceImpl.java  # copia min/max al onboarding
backend/core/src/test/java/core_pymes/unit/ProductoServiceImplTest.java      # constructores actualizados

frontend/pymes/src/modules/core/types/index.ts                               # +5 campos en Producto
frontend/pymes/src/modules/core/pages/AnalisisGastosPage.vue                 # nuevo dashboard
frontend/pymes/src/modules/core/router/routes.ts                             # +ruta analisis-gastos
frontend/pymes/src/layouts/MainLayout.vue                                    # +nav link
```

### Tests

| Suite | Tests | Resultado |
|-------|-------|-----------|
| Core unit | 60 | ✅ |
| Frontend build | — | ✅ |
| Frontend lint | — | ✅ |

### Issues detectados (post-deploy)

**CRÍTICO — XHR POST /facturas → 500:**
- Causa raíz: `presentacionId` no se envía desde frontend
  (`ItemFacturaRequest` TS carece del campo, UI no tiene selector de presentación).
  Backend recibe `null` → `findById(null)` → NPE no manejado.
- Fix pendiente: agregar `presentacionId` al tipo TS + `<q-select>` de presentación
  en `FacturasPage.vue` + `@Valid` cascade en `FacturaRequest.java`

### Próximo

1. **Fix crítico**: `presentacionId` en frontend + cascade `@Valid`
2. **Cascada Categoría→Subcategoría→Producto** en facturas — reemplazar select
   plano de productos por 3 selects jerárquicos. El campo `category` en productos
   tiene formato `"Categoría > Subcategoría > Ítem"` (del template), se parsea
   para poblar los niveles.
3. **Selector de presentación** — 4to `<q-select>` con presentaciones del producto
   seleccionado. Auto-fill `precioUnitario` desde `lastUnitPrice` del producto.
4. **Quick-add proveedor inline** — botón "+" junto al select de proveedor,
   mini dialog con nombre + RUC, al guardar recarga y selecciona el nuevo.
5. **Auto-cálculo precio unitario** — watcher en tiempo real:
   `subtotal / cantidad = precioUnitario` o viceversa.
6. **Template defaults de min_quantity/max_quantity por industria** — opcional,
   usuarios configuran via edit form por ahora.
7. **Gráficos interactivos** en Análisis de Gastos (post-MVP).

---

## 2026-07-06 — Email Verification: flujo cross-tab + auto-navegación a onboarding

### Contexto

Después de registrar un usuario, el backend envía un email con link de verificación. Los clientes de correo **siempre** abren links en nueva pestaña (comportamiento del navegador, no del código). El problema: el usuario termina con 2 pestañas de la app, y la pestaña original (registro) no sabe que se verificó.

### Cambios realizados

#### Backend (sin cambios)

El flujo existente funciona correctamente:
- `POST /auth/register` → guarda en Redis (`temp-register:{token}`) con TTL 15min → envía email
- Email contiene link `/#/verify?token=...&email=...` (plain `<a>`, sin `target="_blank"`)
- `POST /auth/verify-email` → completa registro → retorna `AuthResponse` con tokens + `activeTenant`

#### Frontend

**`VerifyEmailPage.vue`** — auto-navegación post-verificación:
- Removido `window.close()` y `setTimeout` que mataban la pestaña antes del redirect
- Después de verificar: check `setupService.get(tenantId)` → si `onboardingCompleted === false` → navega a `/onboarding`, sino a `/dashboard`
- `goToDashboard()` también checkea onboarding antes de navegar
- Restaurado `pymeq_email_verified` flag en localStorage para storage event de App.vue

**`App.vue`** — sincronización cross-tab:
- Agregado `storage` event listener que detecta `pymeq_email_verified=true` en otra pestaña
- Sincroniza Pinia store desde localStorage (la otra pestaña escribió los tokens)
- Navega a `/dashboard` automáticamente
- Removido `setupService` import (ya no se necesita aquí)

**`auth store/index.ts`** — sincronización en 401:
- Agregado listener de `auth:401` CustomEvent para limpiar `accessToken` y `user` del store cuando axios interceptor limpia localStorage

**`axios.ts`** — evento de 401:
- Interceptor de 401 dispatcha `auth:401` CustomEvent además de limpiar localStorage

**`OnboardingPage.vue`** — fix cascade 401:
- Removido `fetchCurrentUser()` después de `completeOnboarding()` — causaba 401 → `clearSession()` → redirect a login

### Flujo resultante

```
Pestaña Original (registro)          Pestaña Nueva (email link)
────────────────────────────         ──────────────────────────────
1. POST /auth/register              1. GET /#/verify?token=...&email=...
2. Muestra "Revisa tu correo"       2. POST /auth/verify-email
3. ...esperando...                   3. setSession() → localStorage + Pinia
4. storage event detecta            4. setupService.get() check
   pymeq_email_verified             5. router.push('/onboarding') o
5. Sincroniza store                    router.push('/dashboard')
6. router.push('/dashboard')
```

### Problema pendiente

Las 2 pestañas permanecen abiertas. `window.close()` no funciona en pestañas abiertas desde enlaces de correo (solo con `window.open()`). No hay solución frontend — es comportamiento estándar PWA para links externos. El usuario cierra manualmente la pestaña que no necesita.

### Decisiones técnicas

1. **No se puede prevenir 2 pestañas** — comportamiento del navegador/email client
2. **La pestaña nueva es la "buena"** — hace verify → auto-login → onboarding → dashboard
3. **La pestaña original se sincroniza** via storage event → navega a `/dashboard`
4. **`auth:401` CustomEvent** evita dependencia circular entre axios interceptor y auth store

### Archivos modificados

```
frontend/pymes/src/modules/auth/pages/VerifyEmailPage.vue    # auto-navegación post-verificación
frontend/pymes/src/App.vue                                   # storage event listener
frontend/pymes/src/modules/auth/store/index.ts               # auth:401 event listener
frontend/pymes/src/boot/axios.ts                             # dispatch auth:401 en 401
frontend/pymes/src/modules/core/pages/OnboardingPage.vue     # sin fetchCurrentUser()
```

### Tests

| Suite | Resultado |
|-------|-----------|
| Lint | ✅ |
| Backend verificación (curl) | ✅ |

### Próximo

1. **Aceptar 2 pestañas** como estándar PWA, o evaluar `window.open()` en registro para controlar la pestaña (cambia UX).
2. **Fix crítico pendiente**: `presentacionId` en facturas (ver entry 2026-06-26).

---

## 2026-07-16 — Auth criticals + CORS root cause

### Qué se hizo

**Auth: 4 críticos corregidos (130 tests, 0 fallos):**
1. **JWT secret sin validación** — `@PostConstruct init()` en `JwtServiceImpl.java` valida key ≥ 32 bytes
2. **Logout traga excepción** — `extractUserId()` movido antes del try-block en `AuthServiceImpl.java`
3. **Cookie OAuth2 sin Secure** — `cookie.setSecure(request.isSecure())` en `OAuth2IntentCookieFilter.java`
4. **Token reset en URL** — aceptado como está (hash fragment + one-time + TTL + referrer-policy)

**CORS: diagnóstico completo**
- Posta 1 (equivocada): `globalcors` con `allowed-origin-patterns` rompía POST → se quitó
- Posta 2 (descubierta): sin `globalcors`, SCG intercepta OPTIONS internamente y retorna 403 incluso antes de enrutar
- **Doble capa CORS necesaria:**
  - Gateway: `globalcors` con `allowed-origins` (exacto, no pattern) → OPTIONS preflight retorna 200
  - Auth service: `setAllowedOrigins` + `allowCredentials(true)` → ACAO en POST

### Archivos modificados

```
backend/auth/src/main/java/auth/pymes/service/impl/JwtServiceImpl.java              # @PostConstruct init()
backend/auth/src/main/java/auth/pymes/service/impl/AuthServiceImpl.java              # extractUserId fuera del try
backend/auth/src/main/java/auth/pymes/common/config/OAuth2IntentCookieFilter.java    # cookie.setSecure
backend/auth/src/main/java/auth/pymes/common/config/WebCorsConfig.java               # setAllowedOriginPatterns → setAllowedOrigins
backend/gateway-pymes/src/main/resources/application.yaml                            # globalcors con allowed-origins
docs/GAPS.md                                                                         # marks resolved
docs/DAILY_REPORTS_PROJECT.md                                                        # this entry
```

### Tests

| Suite | Resultado |
|-------|-----------|
| Auth unit (130) | ✅ |

### Próximo

1. Frontend CORS test real desde navegador
2. Retomar gaps 🟡 (N+1, rate limit, etc.)
3. Si aplica, mergar a `develop`

---

## 2026-07-24 — Estrategia de Invitación por Email (post-mortem del intento anterior)

### Contexto

Se intentó implementar el sistema de invitación por email en `refactor/invitation-attempt`. El usuario reporta que "rompi todo" al reintentar directamente sobre `feature/core`. Se analizó el diff completo entre ambas ramas para extraer la estrategia correcta.

### Lección principal

**Endpoints separados para flujos separados.** El intento anterior mezcló registro normal + registro por invitación en un solo `POST /auth/register` con `RegisterRequest` condicional. Esto creaba un DTO con campos que solo aplicaban a un camino, validación inconsistente, y errores difíciles de trazar.

La corrección: `POST /invitations/{token}/register` es un endpoint público independiente con su propio DTO `InvitationRegisterRequest`. El registro normal (`POST /auth/register`) queda intacto.

### Estrategia extraída

Ver `docs/strategies/EMAIL_INVITATION_STRATEGY.md` para el documento completo.

### Archivos del intento anterior (22 total)

```
# Backend Auth (10)
backend/auth/.../Tenant.java                           → maxUsers 1 → 2
backend/auth/.../V3__plan_cooldown.sql                 → NUEVO
backend/auth/.../InvitationRegisterRequest.java         → NUEVO
backend/auth/.../InvitationInfoResponse.java            → NUEVO
backend/auth/.../CodigoError.java                      → +ROLE_CHANGE_COOLDOWN
backend/auth/.../InvitationApi.java                    → +2 endpoints
backend/auth/.../InvitationApiController.java          → +impl
backend/auth/.../InvitationServiceImpl.java            → +getInvitationInfo, +registerAndAccept
backend/auth/.../MemberServiceImpl.java                → +role cooldown
backend/auth/.../SecurityConfig.java                   → +WHITE_LIST entries

# Backend Gateway (1)
gateway-pymes/.../RouterValidator.java                 → +3 openEndPoints

# Frontend (8)
auth/types/index.ts                                    → +plan, +DTOs
auth/store/index.ts                                    → +selectTenant
auth/services/invitation.service.ts                    → +2 métodos
auth/services/member.service.ts                        → NUEVO
auth/pages/AcceptInvitationPage.vue                    → refactor completo
core/pages/TeamsPage.vue                               → NUEVO (493 líneas)
core/router/routes.ts                                  → +teams route
layouts/MainLayout.vue                                 → +nav item Teams
```

### Decisión clave

`registerAndAccept()` es transaccional: crea User + UserTenant + marca invitation en una sola transacción. Si falla en cualquier paso, todo revierte. No queda usuario parcial.

### Próximo

1. Implementar MVP (sin cooldown): Tenant maxUsers → 2, register+accept, TeamsPage, nav por roles
2. Post-MVP: cooldown de cambio de rol (V3 migration)
3. Opcional: rediseño de templates email (Swiss style)

---

*Creado: 2026-06-19 | Consolidacion de REFACTOR-STRATEGY.md + .github/REFACTOR.md*

---

## 2026-07-30 — Security + Correctness batch fix (auth + frontend)

### Contexto

Auditoría completa cruzando GAPS.md y TO_DO.md con código real. Se encontraron 6 bugs activos (4 críticos, 2 medios). Se resolvieron los 6 en una sola sesión. Skipped: cache en JwtFilter y rate limit en `/exchange` (ponytail: no son bugs, son optimizaciones).

### Bugs corregidos

| # | Severidad | Bug | Service | Fix |
|---|-----------|-----|---------|-----|
| 1 | 🔴 | `deleteByUserId` revertido por rollback | Auth | `TransactionTemplate` REQUIRES_NEW |
| 2 | 🔴 | MetricasFinancieras field name mismatch | Frontend | Renombrar campos al español |
| 3 | 🔴 | Email casing inconsistente | Auth | `.toLowerCase()` en `completeRegistration()` |
| 4 | 🔴 | AUTH001 retorna 400 no 401 | Auth | `HttpStatus.UNAUTHORIZED` |
| 5 | 🟡 | CORS `allowed-origins` sin default | Auth | `@Value` con fallback |
| 6 | 🟡 | `@Transactional` en métodos Redis-only | Auth | Quitado de 2 métodos |

### Archivos modificados (12 total)

**Auth (7):**
- `JwtServiceImpl.java` — `deleteByUserId` en transacción separada
- `AuthServiceImpl.java` — `.toLowerCase()` en email
- `CodigoError.java` — 400 → 401
- `OAuth2AuthenticationSuccessHandler.java` — default CORS
- `EmailVerificationServiceImpl.java` — quitar `@Transactional` Redis-only
- `JwtServiceImplTest.java` — mock `TransactionTemplate`
- `AuthApiIntegrationTest.java` — expect 401

**Frontend (5):**
- `types/index.ts` — `MetricasFinancieras` campos español
- `DashboardPage.vue` — campos español
- `AccountingPage.vue` — campos español
- `StatStrip.vue` — campos español

### Tests

| Suite | Tests | Estado |
|-------|-------|--------|
| Auth unit | 140 | ✅ |
| Auth integration | 55 | ✅ |
| Core unit | 150 | ✅ |
| Core integration | 22 | ✅ |
| Gateway | 37 | ✅ |
| Frontend lint | — | ✅ |
| Frontend vue-tsc | — | ✅ |

### Skipped (ponytail)

- Cache en JwtFilter: no hay Caffeine, agregar cuando load lo demande
- Rate limit `/exchange`: requiere gateway filter + Redis config

**Estado:** ✅ COMPLETADO
