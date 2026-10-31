# E2E Testing Strategy

Estrategia global de testing para PYMEQ. Cubre 3 capas: unit tests (por servicio), backend integration tests (WebTestClient + Testcontainers), y E2E browser tests (Playwright).

## State actual (2026-10-26)

| Servicio | Unit tests | Integration tests | Gaps abiertos |
|---|---|---|---|
| Gateway | 48 (5 JwtUtils +22 RouterValidator +9 AuthenticationFilter +1 context +10 GatewayIntegrationTest +1 SwaggerAggregator, 2026-10-26) | 10 (`GatewayIntegrationTest`) | 0 — cerrado 2026-10-26 |
| Auth | 207 (138 unit +56 integration +12 consistency +1 auth, 2026-09-17) | 56 | 5 endpoints sin integration, 3 services sin unit test |
| Core | 263 (85 unit +11 analytics +104 JPA +62 integration +1, 2026-09-17, V6 ITBMS 0/7/10) | 62 | C-IT1..4 listos |
| Frontend | 76 (vitest) + 7 pasos tour 960KB | 9 E2E Chromium (`e2e/`) + 1 manual | Job `e2e-tests` en CI pendiente; specs de registro/onboarding/productos/dashboard |
| **Total** | **518 backend + 76 fe** | **118 backend + 9 E2E** | |

---

## Capa 1: Backend Integration Tests

Tests HTTP reales contra DB+Redis vía Testcontainers. Sin frontend.

### Gateway — 11 tests nuevos (✅ 2026-10-26)

| # | Test | Qué valida |
|---|---|---|
| G-IT1 | Proxy routing a core | `GET /api/v1/core/setup/config` llega a core |
| G-IT2 | Proxy routing a auth | `POST /api/v1/auth/login` llega a auth |
| G-IT3 | Route ordering | `/api/v1/core/products` matchea core, NO auth |
| G-IT4 | CORS preflight OPTIONS | `OPTIONS` retorna `Access-Control-Allow-Origin` |
| G-IT4b | CORS origen no permitido → 403 | `Origin` ajeno → `403` |
| G-IT5 | Security headers | HSTS, X-Frame-Options, X-Content-Type-Options |
| G-IT6 | JWT missing → 401 | Sin header → 401 JSON |
| G-IT7 | JWT expired → 401 | Token expirado → 401 JSON |
| G-IT8 | Identity headers forward | Token válido → X-User-Id, X-Tenant-Id llegan al upstream |
| Actuator | Health público | `GET /actuator/health` → 200 UP |
| G-UT1 | SwaggerAggregatorConfig | URL real de Auth registrada (core pendiente hasta exponer OpenAPI) |

**Infra**: `@SpringBootTest(RANDOM_PORT)` + `WebTestClient` + MockWebServer como upstream mock.

### Auth — 10 tests nuevos

| # | Test | Qué valida |
|---|---|---|
| A-IT1 | `POST /exchange` | Code exchange retorna JWT tokens |
| A-IT2 | `POST /tenants/select` | Select tenant retorna token con nuevo tenantId |
| A-IT3 | `GET /invitations` | Lista invitaciones pendientes del tenant |
| A-IT4 | `DELETE /invitations/{id}` | Cancelar invitación |
| A-IT5 | `GET /tenants/{id}/shutdown` | Shutdown exitoso |
| A-IT6 | Full CRUD happy-path | Register→Verify→Login→Create tenant→Invite→Accept→List→Delete |
| A-UT1 | `PermissionCacheService` | Cache hit/miss, invalidación por rol |
| A-UT2 | `RateLimitService` | Ventana fija, Redis counter |
| A-UT3 | `CustomOAuth2UserService` | Find-or-create, JIT provisioning |
| A-UT4 | `TokenBlacklistService` | Add/check/revoke, TTL |

### Core — V6 ITBMS + Costos engine (lectura 2026-09-15)

Tests reales:

| # | Test | Qué valida |
|---|---|---|
| C-IT1 | Setup CRUD completo | GET config → POST onboarding → GET verify |
| C-IT2 | Factura with items | Create factura, verify stock updates |
| C-IT3 | Costos engine | GET /costos/diario, verify cálculo |
| C-IT4 | Analytics engines | GET /analytics/completo, verify 9 engines |

---

## Capa 2: Playwright E2E Tests

Flujos de usuario reales: browser → gateway → backend.

### Estado actual (2026-10-26)

9 tests automáticos en Chromium, **9/9 verde en 24.3 s** con el stack Docker completo:

| Spec | Cubre | Backend |
|---|---|---|
| `app.spec.ts` | Landing y campo de empresa | Real |
| `auth-session.spec.ts` | Login UI → dashboard; merge `activeTenant.id → user.tenantId` antes de que resuelva `/users/me`; logout → re-login con tokens frescos y mismo tenant | Mockeado |
| `oauth2-callback.spec.ts` | Callback OAuth2: code exchange, `tenantId`, dashboard | Mockeado |
| `invoice-itbms.spec.ts` | ITBMS 0/7/10 por ítem: desglose $100/$200/$17, total $317 y payload con tasas individuales | Mockeado |
| `pwa-cache.spec.ts` | `auth:401` borra `core-api-cache` desde el scope del SW | Real |
| `logout-cycle.spec.ts` | `auth:401` limpia localStorage; re-login sin tokens stale | Real |
| `oauth2.spec.ts` (**@manual**) | Flujo real Google: registro → OAuth2 → onboarding → logout → re-login | Real |

`@manual` se excluye por defecto (`grepInvert: /@manual/`); `E2E_MANUAL=1` lo habilita y requiere `.env` + 2FA humana. Google bloquea headless.

### Fixture de autenticación

Mocks compartidos en `e2e/tests/support/mocks.ts`: `mockAuth`, `mockCore`, `seedSession`, `disableTutorial`. Login determinista sin datos reales en PostgreSQL; el tenant es fijo (`TENANT_ID`) y `disableTutorial` apaga `pymeq_tour_seen`/`pymeq_hint_seen` para que el overlay no intercepte los selects de Quasar.

### Fixture vía API (pendiente)

Hoy cada spec arma sus mocks; el fixture con `storageState` global sigue sin implementar.

```
e2e/fixtures/auth.ts
├── login(page, email, password) → storageState
├── register(page, data) → verification token via API
└── storageState guardado en e2e/.auth/user.json
```

**Enfoque**: Login vía API directa (rápido, confiable), no vía UI (lento, frágil).

### Flujos

| # | Flujo | Archivo | Prioridad | Estado |
|---|---|---|---|---|
| P-1 | Login → dashboard | `auth-session.spec.ts` | Alta | ✅ 2026-10-26 |
| P-2 | Registro → verificación → login | `auth-register.spec.ts` | Alta | Pendiente |
| P-3 | Onboarding (crear empresa) | `setup-onboarding.spec.ts` | Alta | Pendiente |
| P-4 | CRUD Productos | `productos.spec.ts` | Media | Pendiente |
| P-5 | CRUD Facturas | `facturas.spec.ts` | Media | Solo ITBMS (`invoice-itbms.spec.ts`) |
| P-6 | Dashboard KPIs visibles | `dashboard.spec.ts` | Media | Pendiente |
| P-7 | Navegación sidebar/bottom nav | `navigation.spec.ts` | Baja | Pendiente |
| P-8 | Logout → sesión inválida | `auth-session.spec.ts` + `logout-cycle.spec.ts` | Baja | ✅ 2026-10-26 |

### Configuración Playwright

- **Browser**: Chromium only (post-MVP: Firefox + WebKit)
- **Base URL**: `http://localhost:9200`
- **Locale**: `es-VE`, timezone `America/Caracas`
- **Auth**: mocks por spec (`support/mocks.ts`); `storageState` global descartado por ahora
- **Retry**: 1 retry on failure
- **Reporter**: HTML + list
- **`@manual`**: excluido por defecto (`grepInvert`), `E2E_MANUAL=1` lo habilita
- **webServer**: `docker compose up` en foreground desde la raíz (`cwd`), `url: http://localhost:9200`; headless automático con `CI`

---

## Capa 3: CI Integration

```yaml
# .github/workflows/ci.yml — job nuevo
e2e-tests:
  needs: [auth-unit, auth-integration, gateway-build, core-build, frontend-build]
  runs-on: ubuntu-latest
  steps:
    - checkout
    - docker compose up -d
    - wait-for-healthchecks (curl health endpoints)
    - cd frontend/pymes && npm ci && npm run build
    - cd / && npm ci && npx playwright install chromium
    - npx playwright test
    - docker compose down
  if: github.ref == 'refs/heads/develop' || github.ref == 'refs/heads/main'
```

**Regla**: E2E solo corre en PRs a `develop`/`main`, no en cada push.

---

## Orden de ejecución

```
Fase 1 (hecha)         Fase 2 (en curso)       Fase 3 (pendiente)
─────────────────      ─────────────────       ──────────────
Auth integration       Gateway integration     Playwright en CI
(pendiente 5 gaps)     (11 tests, ✅)          (job e2e-tests)
↓                      ↓                       ↓
Auth unit              Core integration        Docker compose
(3 services)           (pendiente lectura)     + Playwright
```

## Archivos a crear/modificar

| Archivo | Acción |
|---|---|
| `e2e/playwright.config.ts` | ✅ `grepInvert @manual`, `cwd` raíz, headless en CI |
| `e2e/tests/support/mocks.ts` | ✅ mocks auth/core, `seedSession`, `disableTutorial` |
| `e2e/tests/auth-session.spec.ts` | ✅ P-1 + P-8 |
| `e2e/tests/oauth2-callback.spec.ts` | ✅ callback OAuth2 |
| `e2e/tests/invoice-itbms.spec.ts` | ✅ ITBMS 0/7/10 |
| `e2e/tests/pwa-cache.spec.ts` | ✅ limpieza `core-api-cache` |
| `e2e/README.md` | ✅ comandos + matriz CI/manual |
| `e2e/fixtures/auth.ts` | Pendiente: login vía API + `storageState` |
| `e2e/tests/auth-register.spec.ts` | Pendiente |
| `e2e/tests/setup-onboarding.spec.ts` | Pendiente |
| `e2e/tests/productos.spec.ts` | Pendiente |
| `e2e/tests/facturas.spec.ts` | Pendiente (CRUD; ITBMS ya cubierto) |
| `e2e/tests/dashboard.spec.ts` | Pendiente |
| `e2e/tests/navigation.spec.ts` | Pendiente |
| `backend/gateway-pymes/src/test/.../GatewayIntegrationTest.java` | ✅ 2026-10-26 |
| `backend/gateway-pymes/src/test/.../config/SwaggerAggregatorConfigTest.java` | ✅ 2026-10-26 |
| `backend/auth/src/test/.../TokenBlacklistServiceTest.java` | ✅ 2026-10-26 |
| `backend/auth/src/test/.../TenantMemberIntegrationTest.java` | Pendiente |
| `backend/auth/src/test/.../PermissionCacheServiceTest.java` | Pendiente |
| `backend/auth/src/test/.../RateLimitServiceTest.java` | Pendiente |
| `backend/auth/src/test/.../CustomOAuth2UserServiceTest.java` | Pendiente |
| `.github/workflows/ci.yml` | Pendiente: job `e2e-tests` |

## Notas

- Los gaps abiertos en GAPS.md (S3-S15 en auth, S1-S4 en gateway) son hardening diferido, no bugs. Se cerrarán cuando el load lo demande.
- Core requiere lectura completa de sus .md y tests antes de definir tests específicos (pendiente).
- Los E2E tests dependen de que el stack Docker levante correctamente (frontend:9200, gateway:8080, auth:8081, core:8082).
