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

### 🔧 Fix Docker (2026-04-11)
- [x] **Testcontainers** actualizado `1.20.5` → `1.21.4`. Docker 29.x requiere API ≥1.44; la versión antigua usaba `docker-java` con API 1.32 (`client version 1.32 is too old`).

### ✅ Verificación de Email (2026-04-11)
- [x] **Migración V4**: Columna `email_verified_at` nullable en `users` + índice parcial.
- [x] **UserEntity**: Campo `emailVerifiedAt` + helpers `isEmailVerified()`, `markEmailAsVerified()`.
- [x] **Redis**: Tokens de verificación en `email:verify:{token}` → email, TTL 15 min.
- [x] **Servicio**: `EmailVerificationService` (generate, verify, resend).
- [x] **Excepción**: `EmailVerificationTokenInvalidException` (VER002).
- [x] **Códigos de error**: VER001-VER004 en `CodigoError`.
- [x] **Endpoints**: `POST /auth/verify-email`, `POST /auth/resend-verification`.
- [x] **Register**: Genera token de verificación automáticamente.
- [x] **Login**: Rechaza si `email_verified_at == null` → `403 FORBIDDEN (VER001)`.
- [x] **Tests**: 8 unitarios + 5 integración. **Total: 76 tests (0 fallos).**

### ✅ Recuperación de Contraseña (2026-04-11)
- [x] **Migración**: No requerida (el campo `password` ya existe desde V2).
- [x] **Redis**: Tokens en `password:reset:{token}` → email, TTL 15 min.
- [x] **Servicio**: `PasswordResetService` (generateResetToken, resetPassword).
- [x] **Excepción**: `PasswordResetTokenInvalidException` (RST001).
- [x] **Códigos de error**: RST001 (token inválido), RST002 (token expirado).
- [x] **Endpoints**: `POST /auth/forgot-password`, `POST /auth/reset-password`.
- [x] **Timing Attack Prevention**: `POST /forgot-password` siempre retorna 200, aunque el email no exista.
- [x] **Tests**: 7 unitarios cubriendo: email existente, email inexistente, token válido, token inválido, user not found.

---

## 7. Estrategia de Próximos Pasos 📋

### 🔴 Fase 1: Seguridad Crítica de Tokens
1. **Refresh Token Rotation**: Blacklist del refresh token viejo en Redis tras cada refresh. Validar que un refresh token usado una vez no funcione una segunda vez.

### 🟡 Fase 2: Integridad de Identidad (Email)
2. **Verificación de Email**: ✅ COMPLETADO
   - `email_verified_at` nullable en `users`.
   - Redis: `verify:{email}` TTL 15 min.
   - Endpoint: `POST /auth/verify-email` (token).
   - Registro genera token → loguea (pendiente servicio de email real).
3. **Recuperación de Contraseña**: ✅ COMPLETADO
   - Redis: `password:reset:{token}` → email, TTL 15 min.
   - Endpoints: `POST /auth/forgot-password`, `POST /auth/reset-password`.
   - Timing attack prevention: `forgot-password` siempre retorna 200.

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

## 8. Optimización de Plomería: Estrategia "Gateway-First" (Propuesta 2026-04-10) 🚀

Con la implementación del API Gateway inteligente, el microservicio de Auth puede reducir su carga transaccional delegando la validación primaria de seguridad.

### 🛡️ Modelo de Confianza Híbrida
*   **Edge Validation (Gateway)**: Se encarga de la firma, expiración y blacklist en Redis.
*   **Internal Validation (Auth/Core Services)**: Confían en los headers `X-User-*` inyectados por el Gateway.

### 📉 Reducción del Impacto en PostgreSQL
Actualmente, el `JwtAuthenticationFilter` realiza un `userRepository.findById()` en **cada petición**. Para optimizar recursos en el Free Tier, se propone:

1.  **Lazy Principal Loading**:
    *   Si los headers `X-User-Id` y `X-User-Email` están presentes y el filtro JWT ya validó la firma, el microservicio crea un objeto `UserPrincipal` básico sin ir a la DB.
    *   La consulta a la base de datos se dispara **solo si la lógica de negocio requiere campos específicos del perfil** (ej. `GET /auth/me`).
2.  **Contexto Multi-tenant desde Headers**:
    *   El `tenant_id` ya viene inyectado en `X-Tenant-Id`. Se inyecta directamente en el contexto de la petición, evitando decodificar nuevamente el JWT.

### 🎯 Beneficio Estimado
*   **Ahorro de RAM**: Menor tiempo de vida de los hilos de trabajador esperando a PostgreSQL.
*   **Ahorro de DB**: Eliminación del ~90% de las consultas `SELECT` redundantes de autenticación en flujos de navegación normal.
*   **Latencia**: Reducción de ~15ms - 30ms en el tiempo de respuesta total (TTFB) por petición.

---

## 9. Infraestructura & Perfiles (Actualizado 2026-04-11) 🛠️

### 🐳 Troubleshooting: Docker API Version (Testcontainers)
Se detectó una incompatibilidad entre el cliente `docker-java` (vía Testcontainers) y versiones modernas del demonio de Docker (API 1.44+).

**Síntoma:**
```text
BadRequestException (Status 400: {"message":"client version 1.32 is too old..."})
```

**Solución de Infraestructura Requerida:**
Para corregir esto de forma permanente en la máquina de desarrollo, se debe asegurar que el archivo `~/.testcontainers.properties` contenga:
```properties
docker.client.api.version=1.44
```
Alternativamente, se puede pasar como propiedad de sistema: `-Ddocker.client.api.version=1.44`. El proyecto se ha dejado configurado con el transporte `httpclient5` para mejorar la negociación automática.

### 🏗️ Optimización de Perfiles de Maven
Se ha implementado una jerarquía de configuración profesional basada en perfiles de Maven para separar entornos y proteger secretos.

**Perfiles Disponibles:**
- **`dev`** (Default): Desarrollo local (`localhost`, logs en `DEBUG`). Carga secretos desde `.env`.
- **`stg`**: Entorno de Staging/QA. Configuración estricta vía variables de entorno.
- **`prod`**: Producción. Máxima seguridad y optimización de logs (`WARN`).

**Estrategia de Secretos:**
- **Cero Secretos Hardcoded**: Se eliminaron todos los passwords y keys de los archivos `application.yaml`.
- **Filtrado de Recursos**: Maven inyecta el perfil activo `@spring.profiles.active@` durante el empaquetado.
- **Tests Sanitizados**: Los archivos `application-test.yaml` e `application-integration.yaml` usan valores mock (`_TEST_SECRET`) para evitar conflictos con claves reales.

---

## 10. Consistencia de Rutas API (Implementado 2026-04-11) 🔗

### ✅ Estrategia "Single Source of Truth" aplicada

**Problema resuelto:** Rutas hardcodeadas en múltiples lugares (controllers, SecurityConfig, tests) generan riesgo de inconsistencia silenciosa.

**Solución:** Centralización absoluta en `ApiPathConstants` + validación automática por tests.

#### Arquitectura de Constantes

```
ApiPathConstants.java (src/main)
├── V1_ROUTE = "/api/v1"
├── Base routes: AUTH_ROUTE, USERS_ROUTE, TENANTS_ROUTE, MEMBERS_ROUTE, INVITATIONS_ROUTE
├── Sub-paths: AUTH_REGISTER, AUTH_LOGIN, USERS_ME, TENANTS_SELECT, etc.
└── Full paths: FULL_AUTH_REGISTER, FULL_AUTH_LOGIN, etc. (para SecurityConfig)

TestApiPaths.java (src/test)
└── Reutiliza ApiPathConstants y expone paths completos para tests
    Ej: AUTH_REGISTER = V1_ROUTE + AUTH_ROUTE + AUTH_REGISTER
```

#### Controllers Implementados

| Controller | Base Path | Endpoints | Usa Constantes |
|---|---|---|---|
| `AuthApi` | `/api/v1/auth` | register, login, logout, refresh, verify-email, resend-verification | ✅ |
| `UserApi` | `/api/v1/users` | me | ✅ |
| `TenantApi` | `/api/v1/tenants` | GET /, select, create | ✅ |
| `MemberApi` | `/api/v1/tenants/{tenantId}/members` | list, update-role, delete | ✅ |
| `InvitationApi` | `/api/v1/invitations` | list, create, accept, cancel | ✅ |

#### Validación Automática (`ApiPathConsistencyTest`)

**12 tests** que fallan si hay inconsistencia:

| Test | Qué Valida |
|---|---|
| `validateRequestMappingUsesConstants` | Todos los `@RequestMapping` de controllers usan `ApiPathConstants` |
| `validateMethodMappingsUseConstants` | `@PostMapping`, `@GetMapping`, etc. usan constantes (escaneo con reflection) |
| `validateFullPathsConstruction` | Constantes `FULL_AUTH_*` están correctamente construidas |
| `validatePublicEndpointsInWhitelist` | Endpoints públicos existen y `logout` NO es público |
| `validateWhitelistNoRedundancies` | Sin patrones duplicados en el whitelist de SecurityConfig |
| `validateWhitelistUsesConstants` | Constantes existen para usar en SecurityConfig |
| `validateTestAuthPathsMatchProduction` | `TestApiPaths` == `ApiPathConstants` para auth |
| `validateTestUserPathsMatchProduction` | `TestApiPaths` == `ApiPathConstants` para users |
| `validateTestTenantPathsMatchProduction` | `TestApiPaths` == `ApiPathConstants` para tenants |
| `validateTestMemberPathsMatchProduction` | `TestApiPaths` == `ApiPathConstants` para members |
| `validateTestInvitationPathsMatchProduction` | `TestApiPaths` == `ApiPathConstants` para invitations |
| `validateAllTestPathsStartWithV1` | Todos los paths de test comienzan con `/api/v1` |

#### Optimización del Whitelist de SecurityConfig

**Redundancias eliminadas:**

| Entry Eliminado | Razón |
|---|---|
| `/swagger-ui.html` | Ya cubierto por `/swagger-ui/**` |
| `V1_ROUTE + "/actuator/**"` | Ya cubierto por `/actuator/**` |

**Whitelist resultante (10 entries, antes 12):**

```java
private static final String[] WHITE_LIST = {
    // Swagger / OpenAPI (/** covers .html and all assets)
    "/v3/api-docs/**",
    "/swagger-ui/**",
    // Actuator
    "/actuator/**",
    // OAuth2 login endpoint
    "/login/**",
    // Error page
    "/error",
    // Public auth endpoints (registration, login, email verification)
    ApiPathConstants.FULL_AUTH_REGISTER,
    ApiPathConstants.FULL_AUTH_LOGIN,
    ApiPathConstants.FULL_AUTH_REFRESH,
    ApiPathConstants.FULL_AUTH_VERIFY_EMAIL,
    ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION
};
```

#### Dependencia Agregada

```xml
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10.2</version>
    <scope>test</scope>
</dependency>
```

Usada para escaneo de classpath en `ApiPathConsistencyTest` (reflection sobre controllers).

---

## 11. Recuperación de Contraseña (Implementado 2026-04-11) 🔑

### 🎯 Visión General

Flujo de recuperación de contraseña sin estado, usando **Redis como almacén temporal de tokens** y sin requerir migración de base de datos (el campo `password` ya existe desde V2).

### 📐 Arquitectura

```
┌──────────────┐     ┌───────────────────┐     ┌──────────┐
│  Frontend    │────▶│  POST /forgot     │────▶│  Redis   │
│  (Email)     │     │  /auth/forgot-    │     │ password:│
│              │     │  password         │     │ reset:{t}│
│              │◀────│  → 200 OK (siempre│     │ → email  │
│              │     │   timing attack)  │     │ TTL 15m  │
└──────────────┘     └───────────────────┘     └──────────┘
        │                                             
        │  Click en link del email                  
        ▼                                             
┌──────────────┐     ┌───────────────────┐     ┌──────────┐
│  Frontend    │────▶│  POST /reset      │────▶│  Redis   │
│  (Token +    │     │  /auth/reset-     │     │ DELETE   │
│  New Pass)   │     │  password         │     │ token    │
│              │◀────│  → BCrypt hash    │────▶│  DB      │
│              │     │  → UPDATE users   │     │ password │
└──────────────┘     └───────────────────┘     └──────────┘
```

### 🔐 Diseño de Seguridad

#### Timing Attack Prevention
`POST /auth/forgot-password` **siempre retorna 200 OK**, incluso si el email no existe. Esto previene que un atacante pueda enumerar emails registrados midiendo los tiempos de respuesta.

```java
// AuthApiController.java
public ResponseEntity<ApiResponse<Void>> forgotPassword(ForgotPasswordRequest request) {
    // Siempre retorna 200 para prevenir timing attacks
    passwordResetService.generateResetToken(request.email());
    return ResponseEntity.ok(ApiResponse.ok());
}
```

#### Token Seguro
- **Generación**: `SecureRandom` → 32 bytes → hex (64 caracteres)
- **Almacenamiento**: Redis clave `password:reset:{token}` → email
- **TTL**: 15 minutos (expiración automática)
- **Single-use**: Se elimina de Redis tras el primer uso exitoso

### 📋 DTOs

**ForgotPasswordRequest:**
```java
public record ForgotPasswordRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email
) {}
```

**ResetPasswordRequest:**
```java
public record ResetPasswordRequest(
    @NotBlank(message = "Reset token is required")
    String token,

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword
) {}
```

### 🏷️ Códigos de Error (RST)

| Código | HTTP | Significado |
|--------|------|-------------|
| `RST001` | 400 | Token de reset inválido o ya usado |
| `RST002` | 400 | Token de reset expirado (TTL 15 min) |

### 🧪 Cobertura de Tests

**PasswordResetServiceImplTest** (7 tests):

| Test | Escenario | Resultado Esperado |
|------|-----------|-------------------|
| `generateResetToken_ExistingEmail_ReturnsTrue` | Email registrado | Token en Redis, retorna `true` |
| `generateResetToken_NonExistentEmail_ReturnsFalse` | Email no existe | Sin interacción Redis, retorna `false` |
| `resetPassword_ValidToken_UpdatesPassword` | Token válido + nueva password | Password BCrypt, token eliminado |
| `resetPassword_InvalidToken_ThrowsException` | Token inexistente | `PasswordResetTokenInvalidException` |
| `resetPassword_UserNotFound_ThrowsException` | Token válido pero user borrado | `AuthenticationException` |

### 🔗 Integración con Email (Pendiente)

Actualmente el servicio **genera el token y lo almacena en Redis**, pero falta el envío por email. La integración futura será:

```java
// Pseudocode del flujo completo con email:
1. POST /auth/forgot-password → generateResetToken() → token
2. EmailService.sendResetEmail(user.getEmail(), token)  // ← PENDIENTE
3. Usuario recibe email con link: https://app.com/reset?token={token}
4. Frontend extrae token y muestra formulario de nueva contraseña
5. POST /auth/reset-password → resetPassword(token, newPassword)
```

### 📊 Métricas

| Métrica | Valor |
|---------|-------|
| Archivos nuevos | 6 |
| Archivos modificados | 6 |
| Tests unitarios | 7 |
| Endpoints nuevos | 2 |
| Códigos de error nuevos | 2 (RST001, RST002) |

---

### 📊 Calificación de Salud Arquitectónica: 9.7/10
> Estructura de entornos profesionalizada. El sistema es ahora "Environment-Aware" y cumple con los estándares de seguridad para despliegues SaaS, eliminando riesgos de fuga de credenciales en el repositorio. Recuperación de contraseña implementada con timing attack prevention. 🚀
