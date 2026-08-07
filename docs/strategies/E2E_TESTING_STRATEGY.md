# E2E Testing Strategy

Estrategia global de testing para PYMEQ. Cubre 3 capas: unit tests (por servicio), backend integration tests (WebTestClient + Testcontainers), y E2E browser tests (Playwright).

## State actual (2026-08-05)

| Servicio | Unit tests | Integration tests | Gaps abiertos |
|---|---|---|---|
| Gateway | 37 | 0 | 0 integration, CORS, route ordering |
| Auth | 138 | 47 | 5 endpoints sin integration, 4 services sin unit test |
| Core | 173 | 45 | Pendiente lectura completa |
| Frontend | 29 (vitest) | 0 | — |
| **Total** | **377** | **92** | |

---

## Capa 1: Backend Integration Tests

Tests HTTP reales contra DB+Redis vía Testcontainers. Sin frontend.

### Gateway — 9 tests nuevos

| # | Test | Qué valida |
|---|---|---|
| G-IT1 | Proxy routing a core | `GET /api/v1/core/setup/config` llega a core |
| G-IT2 | Proxy routing a auth | `POST /api/v1/auth/login` llega a auth |
| G-IT3 | Route ordering | `/api/v1/core/products` matchea core, NO auth |
| G-IT4 | CORS preflight OPTIONS | `OPTIONS` retorna `Access-Control-Allow-Origin` |
| G-IT5 | Security headers | HSTS, X-Frame-Options, X-Content-Type-Options |
| G-IT6 | JWT missing → 401 | Sin header → 401 JSON |
| G-IT7 | JWT expired → 401 | Token expirado → 401 JSON |
| G-IT8 | Identity headers forward | Token válido → X-User-Id, X-Tenant-Id llegan al upstream |
| G-UT1 | SwaggerAggregatorConfig | URLs de auth y core configuradas |

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

### Core — pendiente lectura completa

Tests conocidos de sesiones anteriores:

| # | Test | Qué valida |
|---|---|---|
| C-IT1 | Setup CRUD completo | GET config → POST onboarding → GET verify |
| C-IT2 | Factura with items | Create factura, verify stock updates |
| C-IT3 | Costos engine | GET /costos/diario, verify cálculo |
| C-IT4 | Analytics engines | GET /analytics/completo, verify 9 engines |

---

## Capa 2: Playwright E2E Tests

Flujos de usuario reales: browser → gateway → backend.

### Fixture de autenticación

```
e2e/fixtures/auth.ts
├── login(page, email, password) → storageState
├── register(page, data) → verification token via API
└── storageState guardado en e2e/.auth/user.json
```

**Enfoque**: Login vía API directa (rápido, confiable), no vía UI (lento, frágil).

### Flujos

| # | Flujo | Archivo | Prioridad | Depende de |
|---|---|---|---|---|
| P-1 | Login → dashboard | `auth-login.spec.ts` | Alta | Auth funcional |
| P-2 | Registro → verificación → login | `auth-register.spec.ts` | Alta | Auth + email mock |
| P-3 | Onboarding (crear empresa) | `setup-onboarding.spec.ts` | Alta | Core setup |
| P-4 | CRUD Productos | `productos.spec.ts` | Media | Core productos |
| P-5 | CRUD Facturas | `facturas.spec.ts` | Media | Core facturas |
| P-6 | Dashboard KPIs visibles | `dashboard.spec.ts` | Media | Core analytics |
| P-7 | Navegación sidebar/bottom nav | `navigation.spec.ts` | Baja | — |
| P-8 | Logout → sesión inválida | `auth-logout.spec.ts` | Baja | Auth |

### Configuración Playwright

- **Browser**: Chromium only (post-MVP: Firefox + WebKit)
- **Base URL**: `http://localhost:9200`
- **Locale**: `es-VE`, timezone `America/Caracas`
- **Auth**: `storageState` global desde `e2e/.auth/user.json`
- **Retry**: 1 retry on failure
- **Reporter**: HTML + list

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
Fase 1 (ahora)         Fase 2 (después)        Fase 3 (CI)
─────────────────      ─────────────────       ──────────────
Auth integration       Gateway integration     Playwright E2E
(填补 5 gaps)          (9 tests nuevos)        (8 flujos)
↓                      ↓                       ↓
Auth unit              Core integration        Docker compose
(4 services nuevos)    (pendiente lectura)     + Playwright
```

## Archivos a crear/modificar

| Archivo | Acción |
|---|---|
| `e2e/playwright.config.ts` | Ajustar `storageState` global |
| `e2e/fixtures/auth.ts` | Implementar login vía API |
| `e2e/tests/auth-login.spec.ts` | Crear |
| `e2e/tests/auth-register.spec.ts` | Crear |
| `e2e/tests/setup-onboarding.spec.ts` | Crear |
| `e2e/tests/productos.spec.ts` | Crear |
| `e2e/tests/facturas.spec.ts` | Crear |
| `e2e/tests/dashboard.spec.ts` | Crear (reemplazar placeholder) |
| `e2e/tests/navigation.spec.ts` | Crear |
| `e2e/tests/auth-logout.spec.ts` | Crear |
| `backend/gateway-pymes/src/test/.../GatewayIntegrationTest.java` | Crear |
| `backend/auth/src/test/.../TenantMemberIntegrationTest.java` | Crear |
| `backend/auth/src/test/.../PermissionCacheServiceTest.java` | Crear |
| `backend/auth/src/test/.../RateLimitServiceTest.java` | Crear |
| `backend/auth/src/test/.../CustomOAuth2UserServiceTest.java` | Crear |
| `backend/auth/src/test/.../TokenBlacklistServiceTest.java` | Crear |
| `.github/workflows/ci.yml` | Agregar job `e2e-tests` |

## Notas

- Los gaps abiertos en GAPS.md (S3-S15 en auth, S1-S4 en gateway) son hardening diferido, no bugs. Se cerrarán cuando el load lo demande.
- Core requiere lectura completa de sus .md y tests antes de definir tests específicos (pendiente).
- Los E2E tests dependen de que el stack Docker levante correctamente (frontend:9200, gateway:8080, auth:8081, core:8082).
