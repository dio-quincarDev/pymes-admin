# PyMes Admin - Auth Microservice

Servicio de identidad y multi-tenancy para la plataforma SaaS PyMes Admin. Gestiona autenticacion, sesiones, y aislamiento de datos por empresa.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=ffffff)](https://www.oracle.com/java/technologies/downloads/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=ffffff)](https://redis.io/)
[![JWT](https://img.shields.io/badge/JJWT-0.12.6-000000?logo=json)](https://jwt.io/)

---

## Que Hace

Centro de identidad multi-tenant. Orquesta el ciclo de vida completo del usuario: registro, autenticacion, sesiones, y workspace switching con aislamiento de datos por tenant.

### Dominios

| Dominio | Responsabilidad |
|---------|----------------|
| **Auth** | Login, registro, logout global, rotacion de tokens (RTR), rate limiting |
| **User** | Perfiles de identidad globales |
| **Tenant** | Workspaces, switching de contexto, limites de plan FREE |
| **Member** | Roles jerarquicos (OWNER > ADMIN > CONTABLE > VIEWER) |
| **Invitation** | Flujo completo de invitacion para nuevos colaboradores |

### Stack Tecnico

Spring Boot 3.4.3 / Java 21 / PostgreSQL 15 / Redis / JJWT 0.12.6 / MapStruct 1.6.3 / Spring Security OAuth2 / Flyway / Thymeleaf

---

## Como Funciona

### Registro y Verificacion

```
POST /register
  -> Valida datos (nombre, email, password)
  -> Guarda en Redis: temp-register:{token} (TTL 15min)
  -> Email HTML con link de verificacion
  -> No se crea usuario en DB aun (Pending Token)

POST /verify-email (requiere email + token)
  -> Validacion cruzada token-email
  -> Crea atomiquement: User + Tenant FREE + Rol OWNER en DB
  -> Elimina temp-register:{token} (one-time use)
  -> isEmailVerified = true
```

El usuario no existe en DB hasta que verifica su email. Esto evita contaminar la base con cuentas fantasma.

### Autenticacion

```
POST /login
  -> Rate Limit: 5 intentos / 15 min por IP:Email (Redis atomic increment)
  -> Validacion: isActive + isEmailVerified
  -> Audit log: IP (X-Forwarded-For / X-Real-IP / RemoteAddr) + User-Agent
  -> Par JWT: Access Token (stateless) + Refresh Token (stateful, SHA-256 hash en DB)
```

### JWT y Tokens

- **Access Token**: Claims: sub(email), userId, tenantId, role, plan, jti. TTL configurable.
- **Refresh Token**: Payload minimo (userId). Almacenado en DB con hash SHA-256.
- **Refresh Token Rotation (RTR)**: Cada uso invalida el Refresh anterior.
- **Reuse Detection**: Si se reusa un Refresh revocado -> eliminacion de todos los tokens del usuario (full re-auth).
- **Blacklist**: Logout inmediato via Redis. Key: `auth:token_blacklist:{jwt}`, TTL = restante del token. O(1) lookup.

### Multi-tenancy

- **Registro**: Pending token en Redis, creacion atomica de User + Tenant + UserTenant al verificar email.
- **selectTenant**: Verifica membresia -> genera nuevo JWT con tenantId/role/plan -> rota Refresh Token.
- **FREE plan**: Limite de 1 tenant por usuario. Crear mas requiere upgrade.
- **Roles**: OWNER > ADMIN > CONTABLE > VIEWER. Jerarquia enforced en cambios de rol.

### OAuth2

- Google OAuth2 implementado via Gateway. JIT provisioning (find-or-create).
- **Intent Cookie**: Pre-registro de tenant en Redis antes del redirect. Flujo:
  1. `POST /oauth2/intent` -> guarda {name, slug} en Redis, retorna intentId
  2. Redirect `/oauth2/authorization/google?state={intentId}`
  3. SuccessHandler lee state -> consulta Redis -> crea Tenant + UserTenant
  4. JWT ya contiene tenantId
- **Code Exchange**: Por seguridad, el JWT no se expone en URL. Flujo:
  1. SuccessHandler guarda `{accessToken, refreshToken}` en Redis (clave `oauth:code:{uuid}`, TTL 2 min)
  2. Redirige al frontend solo con `?code=<uuid>`
  3. Frontend llama `POST /auth/exchange` con `{code}` para obtener los tokens de forma segura
- Facebook: POSTERGADO (Meta no aprobo la verificacion de la empresa. Pendiente indefinidamente hasta obtener credenciales validas).

### Email System

- Templates Thymeleaf autocontenidos (verification, password-reset, invitation).
- JavaMailSender con MimeMessage HTML responsive.
- Paleta Fintech consistente con el frontend (Deep Forest + Copper).

### Password Reset

```
POST /forgot-password -> Token en Redis (password:reset:{token}, TTL 15min)
POST /reset-password  -> Valida token -> BCrypt update -> Elimina token
```

Timing attack mitigation: respuesta consistente aunque el email no exista.

### Permission Cache

- Cache en Redis con TTL de 5 minutos.
- Key: `auth:permissions:{userId}:{tenantId}`.
- Invalidation granular (por tenant) o global (por usuario).

---

## Endpoints

Ruta base: `/api/v1`

| Metodo | Ruta | Auth | Descripcion |
|--------|------|------|-------------|
| POST | /auth/register | No | Pending token (Redis), persiste en DB tras verificar email |
| POST | /auth/login | No | Rate-limited, audit log |
| POST | /auth/logout | Si | Invalida todos los refresh tokens del usuario |
| POST | /auth/refresh | No | RTR + reuse detection |
| POST | /auth/verify-email | No | Token-Email cross validation, crea usuario en DB |
| POST | /auth/resend-verification | No | Regenera token (TTL reset a 15min) |
| POST | /auth/forgot-password | No | Token Redis TTL 15min |
| POST | /auth/reset-password | No | BCrypt update + token cleanup |
| POST | /auth/exchange | No | OAuth2 code -> JWT (code exchange) |
| GET | /users/me | Si | Perfil actual (userId, email, tenantId, role, plan) |
| GET | /tenants | Si | Lista tenants del usuario (paginado) |
| POST | /tenants | Si | Crear nuevo tenant (solo FREE) |
| POST | /tenants/select | Si | Switch contexto + JWT regeneration |
| GET | /tenants/{id}/members | Si | Lista miembros del tenant |
| PUT | /tenants/{id}/members/{userId}/role | Si | Cambio de rol (validacion jerarquica) |
| DELETE | /tenants/{id}/members/{userId} | Si | Desvincular miembro (solo OWNER) |
| GET | /oauth2/intent/{intentId} | No | Consultar intent pre-registro OAuth2 |
| GET | /invitations | Si | Invitaciones pendientes del usuario |
| POST | /invitations | Si | Crear invitacion |
| POST | /invitations/accept | Si | Aceptar invitacion |
| DELETE | /invitations/{id} | Si | Cancelar invitacion |

---

## Suite de Pruebas

### Ejecucion

| Comando | Scope | Requisitos |
|---------|-------|------------|
| `./mvnw test -B` | Unitarios | Ninguno |
| `./mvnw verify -B -Dspring.profiles.active=integration` | Integracion | Docker (Testcontainers) |

Maven Surefire ejecuta solo `**/integration/**` excluido. Failsafe ejecuta solo `**/integration/**`.

### Cobertura por Dominio

| Dominio | Unit | Integration | Consistency |
|---------|------|-------------|-------------|
| Auth (login/register/refresh/logout) | 11 | 10 | — |
| JWT (tokens/blacklist/validacion) | 20 | — | — |
| OAuth2 (intent/filter/handler/exchange) | 20 | 10 | — |
| Email (verificacion/reset) | 12 | 4 | — |
| Tenant (CRUD/select/shutdown) | 10 | — | — |
| Member (roles/delete) | 3 | — | — |
| Invitation (create/accept/cancel) | 23 | 2 | — |
| Security (constraints/RBAC) | — | 16 | — |
| User (profile) | 5 | — | — |
| Password Reset (forgot/reset) | 5 | 4 | — |
| API paths (constantes vs produccion) | — | — | 12 |
| **Total** | **114** | **47** | **12** |

### Infraestructura de Test

- **Unitarios**: H2 in-memory (PostgreSQL compat mode), Mockito, sin Docker. Archivo: `application-test.yaml`.
- **Integracion**: Testcontainers (PostgreSQL 15-alpine + Redis 7-alpine), MockMvc, `@DynamicPropertySource`.
- **Base class**: `AbstractIntegrationTest` provee lifecycle de containers, `@MockitoBean` en EmailService (suprime envios), helper `flushRedis()`.
- **TestApiPaths**: Utilidad que replica `ApiPathConstants` para tests. Evita strings hardcodeados.

### Archivos de Test

```
src/test/java/auth/pymes/
├── AuthApplicationTests.java              # Context load
├── consistency/
│   └── ApiPathConsistencyTest.java        # 12 tests: paths vs constants vs whitelist
├── integration/
│   ├── AbstractIntegrationTest.java       # Base class (Testcontainers)
│   └── api/
│       ├── AuthApiIntegrationTest.java    # 13 tests: endpoints auth
│       ├── InvitationServiceIntegrationTest.java  # 2 tests
│       ├── OAuth2IntentIntegrationTest.java       # 4 tests
│       ├── OAuth2LoginIntegrationTest.java        # 8 tests
│       ├── PasswordResetIntegrationTest.java      # 4 tests
│       └── SecurityConstraintIntegrationTest.java  # 16 tests: 401/403 + RBAC
├── testutil/
│   └── TestApiPaths.java
└── unit/
    ├── AuthServiceImplTest.java           # 11 tests
    ├── EmailVerificationServiceImplTest.java  # 12 tests
    ├── InvitationServiceImplTest.java     # 23 tests
    ├── JwtServiceImplTest.java            # 25 tests
    ├── MemberServiceImplTest.java         # 3 tests
    ├── OAuth2AuthenticationSuccessHandlerTest.java  # 4 tests
    ├── OAuth2IntentCookieFilterTest.java  # 7 tests
    ├── OAuth2IntentServiceImplTest.java   # 9 tests
    ├── PasswordResetServiceImplTest.java  # 5 tests
    ├── TenantServiceImplTest.java         # 10 tests
    └── UserServiceImplTest.java           # 5 tests
```

### Gaps Conocidos

- Sin tests directos para `TokenBlacklistService`, `JwtAuthenticationFilter`, `GlobalExceptionHandler`.
- Mappers y repositories solo cubiertos via integracion, sin unit tests dedicados.
- Happy-path integration para tenant/member/user/invitation CRUD pendiente.
- Sin test de CORS o configuracion de templates Thymeleaf.

---

## Configuracion

### Perfiles de Maven

| Perfil | Proposito | Comando |
|--------|-----------|---------|
| dev | Desarrollo local (DEBUG, .env) | `./mvnw spring-boot:run -Pdev` |
| stg | Staging (INFO) | `./mvnw package -Pstg` |
| prod | Produccion (WARN) | `./mvnw package -Pprod` |
| integration | Tests con Testcontainers | `./mvnw verify -B -Dspring.profiles.active=integration` |

### Variables de Entorno

Copiar `.env.example` a `.env` para desarrollo local. El servicio usa `spring-dotenv` para cargar variables.

| Variable | Descripcion |
|----------|-------------|
| JWT_SECRET | Secreto para firma HS256 (min 256 bits) |
| DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD | PostgreSQL |
| REDIS_HOST / REDIS_PORT | Redis (blacklist + cache) |
| SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD | SMTP para emails |
| APP_FRONTEND_URL | Base URL para links de verificacion |
| GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET | OAuth2 Google |

---

## CI/CD

GitHub Actions ejecuta `mvn verify` en cada PR a main/develop/feature/*.

---

## Roadmap

| Fase | Descripcion | Estado |
|------|-------------|--------|
| 1 | RTR + Reuse Detection + jti uniqueness | COMPLETADO |
| 2 | Verificacion de email + Pending Token | COMPLETADO |
| 3 | OAuth2 Google + Intent Cookie | COMPLETADO |
| 4 | Logout Global + Thymeleaf Email Templates | COMPLETADO |
| 5 | Member Management (roles, invitaciones) | COMPLETADO |
| 6 | Password Reset + Forgot Password | COMPLETADO |
| 7 | Facebook OAuth2 | PENDIENTE |
| 8 | MFA (TOTP), PKCE, Enterprise SSO | BACKLOG |

---

## Estado

Production-Ready

[![Build](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-PostgreSQL%20%2B%20Redis-2C8EBB?logo=testcontainers)](https://www.testcontainers.org/)
[![Flyway](https://img.shields.io/badge/Flyway-CC0200?logo=flyway)](https://flywaydb.org/)
[![JUnit5](https://img.shields.io/badge/JUnit5-25A162?logo=junit5&logoColor=ffffff)](https://junit.org/junit5/)
