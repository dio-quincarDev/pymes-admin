# Arquitectura

> **Estado:** Implemented · **Última verificación:** 2026-11-22 · Fuente de verdad: el código.
> Esta página es el mapa canonical. El detalle vive en los documentos enlazados — no se copia aquí.

## Contexto

SaaS de gestión financiera para PyMEs en Latinoamérica, multi-tenant, con un motor
analítico que convierte facturas, gastos y ventas en una salud financiera 0-100 con
alertas accionables. Un solo despliegue Docker en Oracle Cloud Free Tier.

`Source: README.md`

## Componentes

| Componente | Tecnología | Puerto | Responsabilidad |
|------------|-----------|--------|-----------------|
| Frontend | Quasar 2 / Vue 3, PWA, routing `hash` | 9200 | UI del negocio; workbox + caché offline |
| Gateway | Spring Cloud Gateway (WebFlux) | 8080 | Valida JWT, consulta blacklist Redis, inyecta headers de identidad, agrega Swagger de Auth |
| Auth | Spring Boot MVC | 8081 | OAuth2 (local + Google/Facebook), JWT + rotación de refresh, RBAC, invitaciones, correo |
| Core | Spring Boot MVC | 8082 | Módulos de negocio, motores analíticos, reportes PDF, idempotencia, checks de tenant/rol |
| PostgreSQL | 15, una instancia `pymes_db` | 5432 | Dos esquemas aislados: `auth` y `core` |
| Redis | 7 | 6379 | Blacklist de tokens, permisos, caché analítica, debounce de recomputo |

**El gateway es WebFlux; Auth y Core son MVC (servlet). No mezclar sus starters.**

`Source: README.md`, `AGENTS.md`, `docker-compose.yml`

## Flujo de una request

1. El frontend llama a `/api/v1/**` (mismo origen; en local `VITE_API_URL`).
2. El gateway valida el JWT y la blacklist en Redis. Si el token es de refresh
   (sin `role`/`tenantId`/`plan`), lo rechaza con 401 antes de seguir.
3. El gateway inyecta los headers `X-User-Id`, `X-User-Email`, `X-Tenant-Id`,
   `X-User-Role` y `X-Plan` (solo si el valor no es nulo).
4. Según la ruta, reenvía a Auth (`/api/v1/auth/**`) o Core (`/api/v1/core/**`).
5. En Core, `TenantValidationFilter` compara `X-Tenant-Id` contra el `?tenantId=`
   de la query (403 si difieren) y `RoleHeaderFilter` arma el `SecurityContext`
   con `ROLE_<rol>`. Los endpoints WRITE llevan `@PreAuthorize`; CONTABLE y
   VIEWER solo leen.
6. `IdempotencyFilter` aplica la ventana de 6 h en POST con `Idempotency-Key`.

`Source: backend/gateway-pymes/src/main/java/dev/dioquincar/gateway_pymes/filter/AuthenticationFilter.java`,
`backend/core/src/main/java/core_pymes/common/config/{TenantValidationFilter,RoleHeaderFilter,IdempotencyFilter}.java`

## Multi-tenancy

El tenant viene del JWT, viaja en `X-Tenant-Id` y se valida contra cada request.
Cada servicio usa **su propio esquema**: Auth nunca toca `core` y Core nunca toca
`auth`. No hay FK entre esquemas — la validación es en capa de aplicación.
Las credenciales DB son compartidas y la conexión es texto plano dentro de la red
interna de Docker (riesgo aceptado).

`Source: docs/strategies/DB_STRATEGY.md`, `docs/GAPS.md` (PostgreSQL 1-3)

## Autenticación y roles

- Registro local e ingreso con Google/Facebook; el correo debe verificarse.
- Access token 1 h, refresh 24 h, con rotación. La rotación usa
  `@Lock(PESSIMISTIC_WRITE)` para serializar read-check-write (evita doble uso).
- Logout es **global**: el `jti` se genera pero no se persiste, así que se borran
  todos los refresh del usuario (diseño deliberado).
- Jerarquía: OWNER(4) > ADMIN(3) > CONTABLE(2) > VIEWER(1). No se puede invitar
  a un rol igual o superior al propio.

`Source: backend/auth/docs/strategies/STRATEGY_OAUTH2_TENANT.md`,
`backend/auth/src/main/java/auth/pymes/common/models/enums/RoleName.java`, `docs/GAPS.md`

## Modelo de datos

| Esquema | Migraciones | Tablas principales |
|---------|-------------|--------------------|
| `auth` | V1..V4 | `users`, `tenants`, `user_tenants`, `invitations`, `refresh_tokens`, `audit_log` |
| `core` | V1..V9 | `tenant_setup`, `industries`, `providers`, `products`, `product_presentations`, `invoices`, `invoice_items`, `operating_expenses`, `loans`, `loan_payments`, `patrimony`, `daily_sales`, `tenant_financial_metrics`, `report_log` |

Flyway corre en cada servicio con su esquema. **Nunca cambiar
`spring.flyway.schemas: auth` sin verificar el aislamiento de Core.** No agregar
migraciones duplicadas: el arranque falla.

`Source: backend/auth/src/main/resources/db/migration/`, `backend/core/src/main/resources/db/migration/`

## Decisiones (ADR resumidas)

| Decisión | Por qué | Detalle |
|----------|---------|---------|
| Una DB, dos esquemas | Free Tier: evita otro servidor de base de datos | `docs/strategies/DB_STRATEGY.md` |
| Gateway WebFlux, servicios MVC | El gateway es solo enrutamiento; los servicios son servlet | `README.md` |
| Aislamiento por header + filtro | `tenantId` llega del frontend; no se confía en él | `docs/GAPS.md` (Core 1) |
| ITBMS por ítem, HALF_UP | Concuerda con DGI Panamá y evita diferencias redondeando el total | `backend/core/docs/CORE.md` |
| Idempotencia por clave 6 h | Evita facturas duplicadas por reintentos del cliente | `backend/core/docs/CORE.md` |
| Crecimiento solo con meses cerrados | No comparar un mes parcial contra uno completo | `backend/core/docs/CORE.md` |
| Swagger agregado solo para Auth | Core expone su spec propia; `/v3/api-docs/core` en gateway da 404 | `docs/api/README.md` |
| PWA con routing `hash` | Evita reescrituras en el hosting estático | `frontend/pymes/docs/PWA_OFFLINE.md` |

## Ver también

- [Guía de operación](../guides/operations-guide.md) — despliegue, puertos, runbooks.
- [Guía de administración](../guides/admin-guide.md) — roles, secretos, configuración.
- [Contrato API](../api/README.md) — specs OpenAPI generadas desde el código.
- [Características](../product/features.md) y [Roadmap](../product/roadmap.md).
- [GAPS](../GAPS.md) — discrepancias documento/código y su estado.
