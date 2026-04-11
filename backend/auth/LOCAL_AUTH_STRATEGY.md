# Estrategia Maestra: Auth Local, Multi-tenant SaaS & IA Ready 🔐🚀

Este documento define la arquitectura para integrar la autenticación por usuario/contraseña y el sistema de permisos jerárquicos, alineado con la visión de la plataforma SaaS PyMes Admin.

## 1. Evolución del Modelo de Datos (IA & SaaS Ready)

### UserEntity (Identidad Universal)
- **Campo `password`**: Almacenar hash BCrypt.
- **Flexibilidad de Proveedor**: `providerId` opcional para permitir registros locales y OAuth2 híbrido.
- **Auditoría Forense**: Implementar `deleted_at` (ZonedDateTime) en lugar de un simple boolean `is_active`.

### UserTenant (Soft Delete & Jerarquía)
- **Anotaciones Hibernate**: `@SQLDelete` y `@Where` para desvinculaciones lógicas.
- **Peso Jerárquico**: Definir un orden de poder: `OWNER (4) > ADMIN (3) > CONTABLE (2) > VIEWER (1)`.

---

## 2. Onboarding de Tenant (Aprovisionamiento SaaS)

El registro local no es solo un `INSERT` de usuario, es el **Aprovisionamiento del SaaS**:
- **Registro Atómico (`POST /api/v1/auth/register`)**:
  1. Validar unicidad de email y fortaleza de contraseña.
  2. Crear `UserEntity` (Dueño).
  3. Crear `Tenant` (Empresa) con **Plan FREE** por defecto (`maxUsers = 1`).
  4. Vincular mediante `UserTenant` con rol **`OWNER`**.
- **Límites de Plan**: 
  - El `AuthService` validará que las invitaciones no superen el `maxUsers` del Tenant.
  - El `TenantService` validará que un usuario en Plan FREE no pueda crear más de 1 empresa como OWNER.

---

## 3. JWT Enriquecido (Pasaporte Multi-service)

El JWT emitido por el Auth Service servirá como pasaporte para los futuros servicios:
- **Claims Requeridos**: `userId`, `email`, `tenantId`, `role`, `plan`.

---

## 4. Seguridad de Jerarquía B2B

Se implementará seguridad en dos capas:
### A. Capa de Acceso (Controller - `@PreAuthorize`)
### B. Capa de Lógica (Service - Jerarquía Crítica)
- Un `ADMIN` no puede tocar a un `OWNER`.
- El `OWNER` no puede borrarse sin transferir la propiedad.

---

## 5. Testing con Testcontainers (Implementado 2026-04-09)

### ✅ Infraestructura de tests de integración configurada

**Estructura:**
```
src/test/java/
├── unit/                    # Unit tests existentes (Mockito) → 39 tests
└── integration/             # Integration tests (Testcontainers) → 17 tests
    ├── AbstractIntegrationTest.java  # Clase base con PostgreSQL + Redis
    ├── AuthApplicationTests.java     # Context load test
    └── api/
        └── AuthApiIntegrationTest.java  # Tests de endpoints Auth API
```

**Lo que se eliminó:**
- [x] **H2** → Eliminado del pom.xml
- [x] **application-test.yaml** → Se mantiene para compatibilidad pero ya no se usa en integración

**Lo que se agregó:**
- [x] **Dependencias**: `spring-boot-testcontainers`, `testcontainers:junit-jupiter`, `testcontainers:postgresql`
- [x] **Maven Failsafe Plugin** → Corre tests del paquete `integration` en `mvn verify`
- [x] **Maven Surefire** → Excluye paquete `integration` en `mvn test`
- [x] **`.testcontainers.properties`** → `reuse.enable=true` para desarrollo
- [x] **`application-integration.yaml`** → Config con Flyway activo, sin H2
- [x] **`AbstractIntegrationTest`** → Clase base con contenedores `postgres:15-alpine` + `redis:7-alpine`, `@DynamicPropertySource`

**Cambios en código de producción necesarios para los tests:**
- [x] **`SecurityConfig`** → `AuthenticationEntryPoint` + `AccessDeniedHandler` custom (401/403 JSON en vez de redirecciones 302)
- [x] **`SecurityConfig`** → `/api/v1/auth/refresh` agregado a white list (valida su propio token del body)
- [x] **`GlobalExceptionHandler`** → `handleInvalidInput` usa `ex.getHttpStatus()` en vez de siempre `badRequest()` (para que RATE_LIMIT_EXCEEDED retorne 429)
- [x] **`/api/v1/auth/refresh`** → Agregado a white list en SecurityConfig

**Cobertura de tests de integración (`AuthApiIntegrationTest`):**
- **REGISTER**: Happy path, email duplicado, password corta, email inválido, sin companySlug
- **LOGIN**: Happy path, email inexistente, contraseña incorrecta, rate limiting (5 intentos → 429)
- **LOGOUT**: Sin token, token malformado, logout exitoso, token ya revocado (→ `TOKEN_REVOKED`), token expirado
- **REFRESH TOKEN**: Happy path, token inválido

### ✅ Limpieza completada (2026-04-10)
- [x] **`JwtAuthenticationFilter`** → Delega validación completa a `JwtServiceImpl.validateToken()`. De 6 catch blocks de JJWT a 1 solo `catch (AuthApiException)`.
- [x] **`JwtService.validateToken()`** → Nuevo método que retorna `ValidatedToken(userId, tenantId, role, email)` o lanza excepciones del dominio (`TokenExpiredException`, `TokenInvalidException`, `TokenRevokedException`).
- [x] **Bug de flujo en filtro** → Corregido: tras `sendErrorResponse()` ahora hace `return` para no continuar la cadena de filtros.
- [x] **`AuthApiController`** → Eliminado `RequestContextHolder` manual. `register` y `login` reciben `HttpServletRequest` como parámetro explícito (consistente con `logout`).
- [x] **Tests actualizados** → +6 tests unitarios para `validateToken()`. Fix en `LogoutTests.logoutWithAlreadyRevokedToken` (esperaba `AUTH002`, ahora `AUTH005`).

---

## 6. Roadmap de Ejecución (Actualizado 2026-04-10)

### ✅ Completado recientemente
- [x] **Desacoplamiento total**: Separación en dominios Auth, User, Tenant, Member e Invitation.
- [x] **Estandarización de Mappers**: Uso profesional de MapStruct en todos los servicios.
- [x] **Refactor de DTOs**: Eliminación de redundancia entre `UserTenantResponse` y `MemberResponse`.
- [x] **Unit Tests JWT**: Cobertura del 100% en generación, validación y seguridad de firmas.
- [x] **Eliminar `JwtTokenProvider`**: Bean zombie legacy eliminado. ✅ COMPLETADO.
- [x] **Refactor `OAuth2AuthenticationSuccessHandler`**: `RuntimeException` → `ResourceNotFoundException`. ✅ COMPLETADO.
- [x] **Validación de Password**: Regex en `RegisterRequest` (mínimo 1 letra + 1 número). ✅ COMPLETADO.
- [x] **Límite de Plan FREE**: Usuario OWNER solo puede crear 1 tenant FREE. ✅ COMPLETADO.
- [x] **Rate Limiting IP + Email**: Bloqueo por combinación `IP:email` en login. ✅ COMPLETADO.
- [x] **Testcontainers**: Tests de integración contra PostgreSQL real + Redis real con Flyway. ✅ COMPLETADO.
- [x] **SecurityConfig API-REST**: AuthenticationEntryPoint + AccessDeniedHandler (sin redirecciones 302). ✅ COMPLETADO.
- [x] **Refactor `JwtAuthenticationFilter`**: 6 catch blocks de JJWT → 1 `catch (AuthApiException)`. Delegación a `JwtService.validateToken()`. ✅ COMPLETADO.
- [x] **Bug `filterChain.doFilter` tras error**: Corregido con `return` inmediato. ✅ COMPLETADO.
- [x] **Refactor `AuthApiController`**: Eliminado `RequestContextHolder`. Inyección explícita de `HttpServletRequest`. ✅ COMPLETADO.

---

## 7. Estrategia de Próximos Pasos 📋

### 🔴 Fase 1: Seguridad Crítica de Tokens
1. **Refresh Token Rotation**: Blacklist del refresh token viejo en Redis tras cada refresh. Validar que un refresh token usado una vez no funcione una segunda vez.

### 🟡 Fase 2: Integridad de Identidad (Email)
2. **Verificación de Email**:
   - Agregar `email_verified_at` nullable a `users`.
   - Redis: `verify:{email}` TTL 15 min.
   - Endpoint: `POST /auth/verify-email` (token).
   - Registro genera token → loguea (pendiente servicio de email real).
3. **Recuperación de Contraseña**:
   - Redis: `reset:{email}` TTL 15 min.
   - Endpoints: `POST /auth/forgot-password`, `POST /auth/reset-password`.
   - Rate limiting + auditoría en `audit_log`.

### 🟢 Fase 3: Cierre del Flujo de Invitaciones
4. **Invitación completa**:
   - `createInvitation` → loguea token (pendiente email real).
   - `acceptInvitation` → si usuario no existe, permite registro con rol/tenant pre-asignado.
   - Auditoría de invitaciones en `audit_log`.

### 🔵 Fase 4: Enterprise (post-MVP)
5. **Transfer Ownership**: Endpoint para ceder el rol de OWNER.
6. **Dashboard de Auditoría**: API paginada para consultar `audit_log`.
7. **CI/CD**: GitHub Actions con `mvn verify`.
8. **CORS + JWT Secret desde entorno**.

---

### 📊 Calificación de Salud Arquitectónica: 8/10
> Deuda técnica crítica eliminada (`JwtTokenProvider`, `RuntimeException`, H2, 6 catch blocks de JJWT). Tests de integración con Testcontainers validando PostgreSQL real + Redis real + Flyway (62 tests: 45 unit + 17 integ). Seguridad API-REST sin redirecciones. Rate limiting implementado. Filtro JWT con un solo catch y validación delegada al dominio. Controller sin `RequestContextHolder`. **Pendiente**: Refresh Token Rotation, password recovery, CI/CD, CORS configurable, JWT secret desde entorno.
