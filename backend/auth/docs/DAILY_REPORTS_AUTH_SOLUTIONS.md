# 📊 Daily Reports & Auth Solutions — Historial de Implementaciones

Este documento registra de manera cronológica el historial de decisiones técnicas, problemas resueltos y la evolución de la arquitectura del microservicio de autenticación (`auth`).

---

## 📋 ÍNDICE DE ROADMAP Y ESTADO

### 📌 Whitelist unificada

A partir de 2026-07-21, `JwtAuthenticationFilter` ya no mantiene su propia lista `publicPaths`. Lee directamente de `SecurityConfig.WHITE_LIST` vía `AntPathMatcher`. Fuente única.

### 📌 Estrategia CORS

A partir de 2026-07-16, CORS opera en **doble capa**:

| Capa | Rol |
|------|-----|
| **Gateway** | `globalcors` con `allowed-origins` (exacto, no pattern) → maneja OPTIONS preflight. SCG lo requiere internamente. |
| **Auth service** | `WebCorsConfig.java` + `.cors(Customizer.withDefaults())` → agrega ACAO a requests reales (POST, GET, etc.). |

**Ver también**: `backend/gateway-pymes/docs/DAILY_REPORTS_GATEWAY_SOLUTIONS.md` — 2026-07-16

### 🔲 Bugs Pendientes
- **[P2] Facebook OAuth2** — *POSTERGADO* (Meta no aprobó la verificación de la empresa. Queda pendiente indefinidamente hasta obtener credenciales válidas en la consola de Meta Developer).

### 🚧 En Progreso
- **Defensa en profundidad + Code Exchange OAuth2** — En proceso de validación y robustecimiento continuo.

### ✅ Historial de Soluciones (Orden Cronológico Inverso)
1. [2026-07-22 — Selección automática de Tenant activo tras aceptación de invitación](#-2026-07-22--selección-automática-de-tenant-activo-tras-aceptación-de-invitación)
2. [2026-07-22 — Sistema de invitación por email (Primer Intento)](#-2026-07-22--sistema-de-invitación-por-email-primer-intento)
3. [2026-07-21 — Whitelist unificada (C1 critical)](#-2026-07-21--whitelist-unificada-c1-critical)
4. [2026-07-16 — Auth criticals (JWT, logout, cookie) + CORS dual layer](#-2026-07-16--auth-criticals-jwt-logout-cookie--cors-dual-layer)
2. [2026-06-24 — Fix UserServiceImplTest (4 errores)](#-2026-06-24--fix-userserviceimpltest-4-errores)
2. [2026-06-23 — Fix OAuth2 redirect + Redis serialization + APP_FRONTEND_URL](#-2026-06-23--fix-oauth2-redirect--redis-serialization--app_frontend_url)
3. [2026-06-21 — Cleanup AuthApiController: Business Logic Extraction](#-2026-06-21--cleanup-authapicontroller-business-logic-extraction)
2. [2026-06-19 — Defensa en profundidad + Code Exchange OAuth2](#-2026-06-19--defensa-en-profundidad--code-exchange-oauth2)
3. [2026-06-16 — Code Review: Cascade, @Transactional, Dead Code & Test Cleanup](#-2026-06-16--code-review-cascade-transactional-dead-code--test-cleanup)
4. [2026-05-08 — CI Flake: InvitationServiceIntegrationTest (Redis Cleanup)](#-2026-05-08--ci-flake-invitationserviceintegrationtest-redis-cleanup)
5. [2026-05-07 — Tenant Shutdown (Soft Delete)](#-2026-05-07--tenant-shutdown-soft-delete)
6. [2026-05-06 — CI Fix & Integration Test Optimization (Singleton Containers)](#-2026-05-06--ci-fix--integration-test-optimization-singleton-containers)
7. [2026-05-05 — Registro Pending Token (Strict Persistence)](#-2026-05-05--registro-pending-token-strict-persistence)
8. [2026-05-05 — Email Verification Token-Email Mismatch](#-2026-05-05--email-verification-token-email-mismatch)
9. [2026-05-05 — Logout Global (Multi-session Revocation)](#-2026-05-05--logout-global-multi-session-revocation)
10. [2026-05-05 — Diseño Profesional de Emails (Thymeleaf)](#-2026-05-05--diseño-profesional-de-emails-thymeleaf)
11. [2026-05-03 — Reingeniería del Flujo de Emails (Thymeleaf)](#-2026-05-03--reingeniería-del-flujo-de-emails-thymeleaf)
12. [2026-05-03 — Pruebas Unitarias e Integración (EmailService Refactor)](#-2026-05-03--pruebas-unitarias-e-integración-emailservice-refactor)
13. [2026-04-22 — InvitationService Technical Debt Coverage](#-2026-04-22--invitationservice-technical-debt-coverage)
14. [2026-04-22 — Password Reset Notification Flow](#-2026-04-22--password-reset-notification-flow)
15. [2026-04-22 — Token Exposure in Email Verification](#-2026-04-22--token-exposure-in-email-verification)
16. [2026-04-21 — Prioridad de Tenants (OAuth2)](#-2026-04-21--prioridad-de-tenants-oauth2)
17. [2026-04-21 — OAuth2 Intent via Cookie](#-2026-04-21--oauth2-intent-via-cookie)
18. [2026-04-20 — OAuth2 Pre-Auth Intent (Atomic Register)](#-2026-04-20--oauth2-pre-auth-intent-atomic-register)
19. [2026-04-20 — NoResourceFoundException /login](#-2026-04-20--noresourcefoundexception-login)
20. [2026-04-20 — Errores OAuth2 + LoginOauth2Controller](#-2026-04-20--errores-oauth2--loginoauth2controller)
21. [2026-04-17 — OAuth2 via Gateway](#-2026-04-17--oauth2-via-gateway)
22. [2026-04-13 — Email Verification + HTML](#-2026-04-13--email-verification--html)
23. [2026-04-13 — CORS Implementado](#-2026-04-13--cors-implementado)
24. [2026-04-12 — RTR + jti + Detección de Reuso](#-2026-04-12--rtr--jti--detección-de-reuso)
25. [2026-04-11 — Docker Fix](#-2026-04-11--docker-fix)
26. [2026-04-11 — Email Verification Logic](#-2026-04-11--email-verification-logic)
27. [2026-04-11 — Password Reset Logic](#-2026-04-11--password-reset-logic)
28. [2026-04-09 — Testcontainers Setup](#-2026-04-09--testcontainers-setup)

## 2026-07-22 — Selección automática de Tenant activo tras aceptación de invitación

### Problema

Al aceptar una invitación a un nuevo tenant (espacio de trabajo), el usuario no podía acceder a los datos de dicha empresa. Si el usuario ya estaba autenticado, se procesaba la invitación en el backend pero su token local de acceso (JWT) seguía apuntando a su tenant anterior (o a ninguno si no tenía tenant activo). Tampoco había una forma automática en el frontend de actualizar las credenciales y seleccionar el tenant aceptado.

Además, el frontend mapeaba incorrectamente la respuesta del backend (`apiResponse.data.tenant.name` en lugar de `apiResponse.data.tenantName` y `apiResponse.data.tenantId`), y realizaba un logout innecesario en caso de error.

### Solución

Para alinear el comportamiento del frontend con el endpoint `/tenants/select` de Auth Service:
1. **Mapeo de Tipos (`types/index.ts`)**:
   - Ajustamos la interfaz `InvitationResponse` para alinearse con el record homónimo del backend (`id`, `tenantId`, `tenantName`, `email`, etc.).
2. **Acción de Selección de Empresa (`store/index.ts`)**:
   - Implementamos la acción `selectTenant(tenantId: string)` en el store de Pinia. Esta acción consume el endpoint `/tenants/select` para obtener y guardar un nuevo conjunto de JWT y Refresh Token con el contexto del tenant activo recién modificado.
3. **Flujo de Aceptación (`AcceptInvitationPage.vue`)**:
   - Corregimos la destructuración y la lectura de `tenantName` y `tenantId` de la respuesta de aceptación de invitación.
   - Si se detecta un `tenantId` en la respuesta exitosa del backend, invocamos inmediatamente `authStore.selectTenant(data.tenantId)` para actualizar la sesión activa antes de redirigir al Dashboard.
   - Removimos el cierre de sesión forzado del bloque `catch` para evitar una mala experiencia de usuario ante fallos menores de red o validación.

### Archivos modificados

```
frontend/pymes/src/modules/auth/types/index.ts
frontend/pymes/src/modules/auth/store/index.ts
frontend/pymes/src/modules/auth/pages/AcceptInvitationPage.vue
```

**Estado:** ✅ RESUELTO

---

## 2026-07-22 — Sistema de invitación por email (Primer Intento)

### Problema

Se requería implementar un sistema de invitaciones a tenants por correo electrónico. Esto implica permitir a usuarios no registrados crear una cuenta y unirse al tenant directamente desde el enlace de invitación en un solo flujo, y a usuarios ya registrados unirse de forma directa al aceptar la invitación, además de imponer límites en la modificación de roles.

### Solución

1. **Flujo de Invitación y Registro Directo (`AuthServiceImpl.java`)**:
   - Implementamos `registerWithInvitation` para permitir que usuarios que se registran usando un `invitationToken` queden directamente asignados al tenant correspondiente con el rol asignado, marcando la invitación como aceptada de forma atómica en el registro.
2. **Límite de Cooldown para Cambio de Roles (`MemberServiceImpl.java`)**:
   - Implementamos un periodo de enfriamiento (cooldown) de roles para tenants en plan `FREE` (`roleChangeCooldownDays`), previniendo modificaciones repetidas en periodos cortos.
   - Agregamos la columna `last_role_change_at` en la tabla `auth.tenants` mediante la migración de base de datos `V3__plan_cooldown.sql`.
3. **Controladores e Interfaces (`InvitationApi.java`, `InvitationApiController.java`)**:
   - Agregamos el endpoint `/invitations/{token}/info` para que el frontend pueda recuperar información básica de la invitación (email y nombre de empresa) sin requerir autenticación.
4. **Plantillas de Correo HTML (`templates/email/`)**:
   - Rediseñamos y actualizamos las plantillas de correo (`invitation.html`, `password-reset.html`, `verification.html`, `layout.html`) con un diseño institucional consistente (Swiss style con colores y fuentes institucionales del proyecto).
5. **Suite de Pruebas**:
   - Creamos `MemberRoleCooldownIntegrationTest.java` para validar el bloqueo de cambio de roles bajo periodos de cooldown en el plan gratuito.
   - Actualizamos los tests de integración en `AuthApiIntegrationTest`, `InvitationServiceIntegrationTest` y `SecurityConstraintIntegrationTest`.

### Archivos modificados

```
backend/auth/src/main/java/auth/pymes/common/config/SecurityConfig.java
backend/auth/src/main/java/auth/pymes/common/models/dto/request/RegisterRequest.java
backend/auth/src/main/java/auth/pymes/common/models/dto/response/InvitationInfoResponse.java
backend/auth/src/main/java/auth/pymes/common/models/entities/Tenant.java
backend/auth/src/main/java/auth/pymes/controller/InvitationApi.java
backend/auth/src/main/java/auth/pymes/controller/impl/InvitationApiController.java
backend/auth/src/main/java/auth/pymes/service/InvitationService.java
backend/auth/src/main/java/auth/pymes/service/impl/AuthServiceImpl.java
backend/auth/src/main/java/auth/pymes/service/impl/InvitationServiceImpl.java
backend/auth/src/main/java/auth/pymes/service/impl/MemberServiceImpl.java
backend/auth/src/main/java/auth/pymes/utils/exception/CodigoError.java
backend/auth/src/main/resources/db/migration/V3__plan_cooldown.sql
backend/auth/src/main/resources/templates/email/invitation.html
backend/auth/src/test/java/auth/pymes/integration/api/MemberRoleCooldownIntegrationTest.java
```

**Estado:** 🚧 IMPLEMENTACIÓN INICIAL (PENDIENTE DE REFACTOR EN FLUJO DE SESIÓN ACTIVA)

---

## 2026-07-21 — Whitelist unificada (C1 critical)

### Problema

`SecurityConfig.WHITE_LIST` (Spring Security) y `JwtAuthenticationFilter.publicPaths` (JWT filter) definían las rutas públicas en dos listas separadas. Al agregar endpoints públicos (exchange, oauth2/intent, swagger, actuator), solo se actualizaba `WHITE_LIST`. El filter JWT intentaba validar token en esas rutas, causando errores o DB lookups innecesarios.

**7 rutas faltaban en `publicPaths`**: `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/**`, `/error`, `/api/v1/auth/oauth2/intent`, `/api/v1/auth/oauth2/intent/**`, `/api/v1/auth/exchange`.

### Fix

- `SecurityConfig.java`: `WHITE_LIST` pasó de `private` a `public static final`
- `JwtAuthenticationFilter.java`: se eliminó la lista `publicPaths` con sus 9 `AntPathRequestMatcher`. `shouldNotFilter()` ahora usa `AntPathMatcher` para matchear contra `SecurityConfig.WHITE_LIST`

Fuente única de verdad — cualquier ruta agregada a `WHITE_LIST` queda automáticamente excluida del JWT filter.

### Gateway

Sin cambios. `RouterValidator.openEndPoints` ya cubre todas las rutas públicas correctamente.

### Archivos modificados

```
backend/auth/src/main/java/auth/pymes/common/config/SecurityConfig.java         # WHITE_LIST: private → public static final
backend/auth/src/main/java/auth/pymes/common/config/JwtAuthenticationFilter.java # publicPaths eliminado, usa WHITE_LIST vía AntPathMatcher
docs/GAPS.md                                                                     # C1 marcado ✅
docs/TO_DO.md                                                                    # marcado [x]
backend/auth/docs/DAILY_REPORTS_AUTH_SOLUTIONS.md                                # this entry
```

### Tests

130 tests, 0 fallos, BUILD SUCCESS.

**Estado:** ✅ RESUELTO

---

## 2026-07-16 — Cierre de 4 gaps críticos de seguridad

### Gap 1 — JWT secret sin validación de tamaño

**Antes:** `JwtServiceImpl.getSigningKey()` llamaba `Keys.hmacShaKeyFor()` directamente, aceptando cualquier tamaño de secret. Un secret de 5 bytes producía HMAC HS40.

**Fix:** `@PostConstruct init()` valida `secretKey.getBytes(UTF_8).length >= 32` (256 bits). Si no, lanza `IllegalArgumentException` e impide el arranque del service. La key se cachea en `this.key` para eficiencia (mismo patrón que el gateway `JwtUtils.java`).

**Test:** `shortSecretThrowsException` — setea secret `"short"`, verifica `IllegalArgumentException`.

**Archivo:** `JwtServiceImpl.java:24-31`

---

### Gap 2 — Logout traga excepciones

**Antes:** `AuthServiceImpl.logout()` tenía `jwtService.extractUserId(accessToken)` dentro de un `try { ... } catch (Exception e) { log.warn(...) }`. Si `extractUserId()` lanzaba (token malformed, claims inválidos), el catch lo tragaba y devolvía `LogoutResponse(true, ...)` — falso éxito.

**Fix:** `extractUserId(accessToken)` movido **antes** del try-block. Si falla, la excepción se propaga al controlador → `GlobalExceptionHandler` → 401/500. El try solo cubre `revokeToken` y `deleteByUserId`.

**Test:** `logout_whenExtractUserIdFails_PropagatesException` — verifica que la excepción se propaga y `revokeToken` nunca se llama.

**Archivo:** `AuthServiceImpl.java:208-219`

---

### Gap 3 — Cookie OAuth2 intent sin Secure

**Antes:** `OAuth2IntentCookieFilter` creaba cookie con `HttpOnly`, `SameSite=Lax`, pero sin flag `Secure`. En producción HTTPS, la cookie viajaba también por HTTP.

**Fix:** `cookie.setSecure(request.isSecure())` — HTTPS → `Secure=true`, HTTP → `Secure=false`. Funciona correctamente en ambos entornos.

**Test:** `cookieSecure_TrueCuandoRequestEsHttps` + `cookieSecure_FalseCuandoRequestEsHttp` — verifican la cookie según el esquema.

**Archivo:** `OAuth2IntentCookieFilter.java:41`

---

### Gap 4 — Token de reseteo en URL

**Análisis:** El token de reseteo se envía en la URL del email: `http://localhost:9200/#/reset-password?token=X`. El `?token=X` está **dentro del hash fragment** (después de `#`), por lo que:
- No se envía al servidor en requests normales
- El gateway ya tiene `Referrer-Policy: strict-origin-when-cross-origin`
- El token es one-time + TTL 15 min

**Decisión:** Sin cambio de código. La mitigación actual (hash fragment + one-time + TTL + referrer-policy) es suficiente para el perfil de riesgo de un SaaS PYME.

---

### Tests

130 tests unitarios, 0 fallos. `JwtServiceImplTest` pasó de 26 a 27 tests (+1), `AuthServiceImplTest` pasó de 12 a 13 tests (+1), `OAuth2IntentCookieFilterTest` pasó de 8 a 10 tests (+2).

**Estado:** ✅ 3 críticos resueltos con código, 1 aceptado con mitigación documentada.

---

## 2026-07-16 — CORS asumido como responsabilidad del auth service

### Contexto

El gateway eliminó su capa `globalcors` debido a un bug en SCG 2023.0.1 que impedía que CORS funcionara en requests POST (OPTIONS funcionaba, POST daba 403). El auth service ya tenía `WebCorsConfig.java` con `CorsConfigurationSource` bean configurado como defensa en profundidad, que ahora pasa a ser la capa CORS primaria.

### Impacto en auth service

Ningún cambio de código en auth. La configuración existente (`CorsConfigurationSource` + `.cors(Customizer.withDefaults())`) ya cubre todos los endpoints de auth con los orígenes configurados vía `app.cors.allowed-origins`.

### Verificaciones

- Los endpoints públicos (register, login, refresh, exchange, OAuth2) ahora reciben CORS headers directamente desde auth.
- Los endpoints protegidos (logout, change-password, users/me) también heredan CORS del auth service.

### Riesgo

Si algún servicio detrás del gateway (ej. core) no tiene su propio `CorsConfigurationSource`, las requests desde el frontend a esos endpoints fallarán por CORS. Actualmente core no tiene CORS configurado, pero el frontend no consume core directamente — solo gateway redirige. Se agregará cuando sea necesario.

**Archivos:** Ninguno modificado en auth service (solo documentación).

**Estado:** ✅ ESTRATEGIA DOCUMENTADA

---

## 2026-07-15 — Email templates refactor + OCI SMTP for staging

### Problemas

1. Plantillas de email con colores hardcodeados que no seguian la paleta DESIGN.md (bronce #C8963E sobre near-black #08090D).
2. Cada template (verification, invitation, password-reset) tenia su propio layout repetido.
3. Staging necesita SMTP de OCI Email Delivery que requiere `starttls.required=true`.

### Soluciones

1. **fragments/layout.html**: nuevo fragment Thymeleaf compartido con `head` y `page(content)` fragments. Outer table 600px, Inter via Google Fonts, zona de contenido reutilizable.
2. **email.css**: todas las hex actualizadas a DESIGN.md, radius 8px a 6px, logo PYMEQ uppercase.
3. **verification.html, invitation.html, password-reset.html**: refactorizadas para usar el layout fragment comun + DESIGN.md colors en vez de valores hardcodeados.
4. **application.yaml**: agregado `starttls.required: true` bajo `spring.mail.properties.mail.smtp.starttls`. Host/port/credentials para OCI van en GitHub Secrets.

### Files tocados

- `templates/email/fragments/layout.html` (nuevo)
- `static/css/email.css`
- `templates/email/verification.html`
- `templates/email/invitation.html`
- `templates/email/password-reset.html`
- `application.yaml`

### Tests

126 unitarios, 0 fallos.

---

## 2026-06-24 — Fix UserServiceImplTest (4 errores)

### Problemas

`UserServiceImplTest` tenia 4 fallos:
- `NullPointerException` en `registerUser_ShouldSucceed` y `getCurrentUser_ShouldSucceed`: faltaban `@Mock` para `UserTenantRepository` y `TenantRepository`
- `UnnecessaryStubbingException` en ambos tests: stubs de `userMapper` sin uso (el metodo bajo test no usa el mapper en esa rama)

### Solucion

- Agregados `@Mock UserTenantRepository userTenantRepository` y `@Mock TenantRepository tenantRepository`
- Removidos stubs de `userMapper.toResponse()` en los tests que no los requerian
- Todos los stubs ahora tienen su contraparte de verificacion (`Mockito.verify()`)
- 126 tests pasan (antes: 122)

### Files tocados

`UserServiceImplTest.java` — 4 cambios puntuales (agregar mocks + remover stubs)

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

## 📅 2026-06-19 — Defensa en profundidad + Code Exchange OAuth2 ✅

### 🎯 Problema 1: JWT expuesto en URL
`OAuth2AuthenticationSuccessHandler` redirigía al frontend con `?token=<jwt>&refresh_token=<jwt>` expuestos en URL bar, historial, logs de red y header `Referer`.

### 📐 Solución (Code Exchange)
1. **Redis Cache**: El handler guarda `{accessToken, refreshToken}` en Redis bajo la clave `oauth:code:<uuid>` (TTL 2 min).
2. **Redirección Segura**: Redirige al frontend sólo con `?code=<uuid>`.
3. **Endpoint de Canje**: El frontend solicita el canje del código mediante `POST /api/v1/auth/exchange` para obtener los tokens de sesión de manera privada.

---

### 🎯 Problema 2: Vulnerabilidades de perímetro y leaks
- Fugas de información en logs (se logueaba el JWT completo y tokens de email).
- Fugas en excepciones (`dbMessage` SQL expuesto al cliente).
- CORS configurado laxamente (`*`).
- Secrets JWT con defaults inseguros en gateway.

### 📐 Solución (Defensa en profundidad)
- **Headers de Seguridad**: Inyección de HSTS, XFO, XCTO, Referrer-Policy en la configuración del Gateway.
- **Limpieza de Logs**: Eliminación de logs que imprimían JWTs o tokens de verificación en texto plano.
- **Ofuscación de Excepciones**: Remoción del mensaje crudo de base de datos en `GlobalExceptionHandler`.
- **CORS Restringido**: Limitación de métodos a `GET,POST,PUT,PATCH,DELETE,OPTIONS`.
- **Complejidad de Contraseña**: Añadida validación estricta de complejidad en `ResetPasswordRequest`.

---

## 📅 2026-06-16 — Code Review: Cascade, @Transactional, Dead Code & Test Cleanup ✅

### 🎯 Problemas
- `CascadeType.ALL` + `orphanRemoval = true` en relaciones `@OneToMany` borraba registros de auditoría y refresh tokens al hacer soft-delete de un usuario/tenant.
- Falta de `@Transactional` en el account linking de `CustomOAuth2UserService.loadUser()`.
- Presencia de código muerto (interfaces vacías).

### 📐 Soluciones
1. **Cascade Restringido**: Se eliminó el cascade de `auditLogs` e `invitations`, manteniéndose sólo en `userTenants`.
2. **Atomicidad**: Se añadió `@Transactional` en `CustomOAuth2UserService`.
3. **Eliminación de Código Muerto**: Borrado de interfaces de servicio sin uso (`SessionService`, interfaces vacías de blacklist y permission cache).
4. **Test Cleanup**: Remoción de stubs redundantes en Mockito y optimización de assertions.

---

## 📅 2026-05-08 — CI Flake: InvitationServiceIntegrationTest (Redis Cleanup) ✅

### 🎯 Problema
Fallo intermitente en CI por mismatch de token-email durante el `setUp` debido a lecturas no determinísticas de Redis (`temp-register:*`).

### 📐 Solución
- **Captura Determinística**: Uso de `ArgumentCaptor` en el mock de `EmailService` para capturar el token generado directamente.
- **Aislamiento**: Implementación de `flushRedis()` en la clase base de integración para limpiar Redis entre ejecuciones.

---

## 📅 2026-05-07 — Tenant Shutdown (Soft Delete) ✅

### 🎯 Problema
El Owner no tenía un mecanismo para cerrar o desactivar su workspace voluntariamente.

### 📐 Solución
Implementación de `DELETE /api/v1/tenants/{tenantId}` realizando un borrado lógico (desactivación: `is_active = false`) validando rol `OWNER` y estado del tenant.

---

## 📅 2026-05-06 — CI Fix & Integration Test Optimization (Singleton Containers) ✅

### 🎯 Problemas
- Reinicios costosos de Docker en JUnit que ralentizaban la suite.
- Re-conexiones infinitas de Lettuce al apagar el contexto que bloqueaban los hilos de prueba.

### 📐 Soluciones
- **Singleton Containers**: Refactorización de `AbstractIntegrationTest` para levantar una única instancia de PostgreSQL/Redis reutilizada durante toda la ejecución de la JVM.
- **Lettuce Timeout**: Seteado `spring.data.redis.lettuce.shutdown-timeout=0ms`.

---

## 📅 2026-05-05 — Registro Pending Token (Strict Persistence) ✅

### 🎯 Problema
Creación inmediata de usuarios y empresas en base de datos antes de verificar su correo, lo que contaminaba la DB con cuentas fantasma.

### 📐 Solución
1. **Redis como Buffer**: `AuthServiceImpl.register()` guarda temporalmente los datos en Redis bajo `temp-register:{token}` (TTL 15 min).
2. **Creación Atómica**: El usuario y su tenant se guardan en la DB únicamente cuando se valida el email en `verifyEmail()`.

---

## 📅 2026-05-05 — Email Verification Token-Email Mismatch ✅

### 🎯 Problema
Vulnerabilidad que permitía usar cualquier token de verificación válido para confirmar cuentas de terceros al no verificar la concordancia del email.

### 📐 Solución
Se requiere el campo `email` en la petición de `/verify-email`. El backend compara el email recibido con el email asociado al token en Redis. Si hay mismatch, se deniega la petición.

---

## 📅 2026-05-05 — Logout Global (Multi-session Revocation) ✅

### 🎯 Problema
El cierre de sesión original sólo invalidaba el Access Token local, dejando vulnerables los Refresh Tokens en otros dispositivos del mismo usuario.

### 📐 Solución
`AuthServiceImpl.logout()` ahora invalida el Access Token en Redis y elimina en cascada lógica todos los Refresh Tokens asociados al `userId` en base de datos, revocando todas las sesiones concurrentes.

---

## 📅 2026-05-05 — Diseño Profesional de Emails (Thymeleaf) ✅

### 🎯 Problema
Emails en formato texto plano o HTML inconsistente que degradaban la imagen del producto.

### 📐 Solución
Creación de un sistema de plantillas component-based responsivo con Thymeleaf:
- Layout base `_base.html` (fluid responsive, fuentes profesionales Inter).
- Fragmentos reusables (`_header.html`, `_footer.html`, `_button.html`).
- Paleta Fintech integrada (Deep Forest y Copper).

---

## 📅 2026-05-03 — Reingeniería del Flujo de Emails (Thymeleaf) ✅

### 🎯 Problema
Código HTML hardcodeado en strings Java dentro de múltiples servicios.

### 📐 Solución
- **EmailTemplateService**: Separación lógica de procesamiento de plantillas HTML desde recursos.
- **EmailService**: Fachada centralizada que utiliza `JavaMailSender` y delega la inyección del mapa de variables al motor Thymeleaf.

---

## 📅 2026-05-03 — Pruebas Unitarias e Integración (EmailService Refactor) ✅

### 🎯 Problema
Errores de compilación y runtime en pruebas tras extraer `JavaMailSender` y `fromEmail` a la nueva abstracción `EmailService`.

### 📐 Solución
Refactorización de mocks en tests unitarios (`EmailVerificationServiceImplTest`, `InvitationServiceImplTest`, `PasswordResetServiceImplTest`) e integración (`AbstractIntegrationTest`) para utilizar `EmailService` mockeado.

---

## 📅 2026-04-22 — InvitationService Technical Debt Coverage ✅

### 🎯 Problemas
Falta de cobertura en InvitationServiceImpl. Gaps de pruebas en límites de plan, invitaciones duplicadas y jerarquía de roles.

### 📐 Solución
Creación e implementación de 23 tests unitarios cubriendo `IdentityResolution`, `CreateInvitation` (incluyendo validación de roles y cuotas), `AcceptInvitation` (tokens expirados o mismatch) y `CancelInvitation`.

---

## 📅 2026-04-22 — Password Reset Notification Flow ✅

### 🎯 Problema
El sistema creaba el token de re-establecimiento en Redis pero no notificaba al usuario por correo, dejando el flujo inutilizable.

### 📐 Solución
Se integró el servicio de correos y la plantilla HTML en `PasswordResetServiceImpl`. Se corrigió la URL del link para apuntar al puerto del frontend (`9200`).

---

## 📅 2026-04-22 — Token Exposure in Email Verification ✅

### 🎯 Problema
El email de verificación exponía el token en texto plano, lo que representaba un riesgo de seguridad en caso de visualización por terceros.

### 📐 Solución
Remoción del token e hipervínculo plano en el cuerpo del correo, forzando el botón de acción seguro como único método de verificación.

---

## 📅 2026-04-21 — Prioridad de Tenants (OAuth2) ✅

### 🎯 Problema
Google login creaba múltiples empresas duplicadas o no asociaba inquilinos pre-existentes de forma limpia.

### 📐 Solución
Lógica de prioridades en `OAuth2AuthenticationSuccessHandler`:
1. **Prioridad 1**: Si existe un `intentId` activo en Redis (intención de registro), crea esa empresa.
2. **Prioridad 2**: Si no hay intent, asocia la sesión al primer tenant activo del usuario en la base de datos.
3. **Prioridad 3**: Si es un usuario enteramente nuevo, crea una empresa por defecto ("Mi Empresa").

---

## 📅 2026-04-21 — OAuth2 Intent via Cookie ✅

### 🎯 Problema
Bucle infinito en `CustomAuthorizationRequestRepository` debido a que Spring Security altera el parámetro `state` de OAuth2 cuando se le concatena un payload personalizado.

### 📐 Solución
Se extrajo el payload del state y se almacenó en una cookie segura antes de redirigir a Google, recuperándose posteriormente en el flujo de callback.

---

## 📅 2026-04-20 — OAuth2 Pre-Auth Intent (Atomic Register) ✅

### 🎯 Problema
OAuth2 iniciaba sesión al usuario directamente sin permitirle configurar los datos iniciales de su empresa en el registro.

### 📐 Solución
Implementación de intención pre-autenticación. Se guardan temporalmente los datos de empresa ingresados en el formulario en Redis, y sólo se persiste el usuario y el workspace cuando el callback de OAuth2 finaliza de forma exitosa.

---

## 📅 2026-04-20 — NoResourceFoundException /login ✅

### 🎯 Problema
El router del frontend entraba en conflicto con los controladores de Spring MVC en la raíz.

### 📐 Solución
Configuración de `ResourceHandlerRegistry` para redirigir peticiones no resueltas de la UI hacia el index estático.

---

## 📅 2026-04-20 — Errores OAuth2 + LoginOauth2Controller ✅

### 🎯 Problema
Errores en callbacks de Google no se informaban de forma amigable al usuario en el frontend, retornando stack traces planos.

### 📐 Solución
`LoginOauth2Controller` modificado para capturar errores de federación y redirigir al frontend con parámetros de query específicos (`/login?error=...`).

---

## 📅 2026-04-17 — OAuth2 via Gateway ✅

### 🎯 Problema
Spring Cloud Gateway bloqueaba o no direccionaba correctamente los flujos de autorización y callbacks de OAuth2 hacia el servicio de identidad.

### 📐 Solución
Configuración de rutas explícitas en el Gateway para `/login/oauth2/**` y `/oauth2/**` dirigiendo tráfico de autenticación hacia `auth-service`.

---

## 📅 2026-04-13 — Email Verification + HTML ✅

### 🎯 Problema
Falta de soporte de correos en formato HTML, enviando correos planos no legibles en múltiples clientes.

### 📐 Solución
Configuración inicial de Thymeleaf templates y envío vía MimeMessageHelper.

---

## 📅 2026-04-13 — CORS Implementado ✅

### 🎯 Problema
Bloqueos del navegador (CORS) en peticiones del frontend en puerto 9200 hacia el gateway en puerto 8080.

### 📐 Solución
Configuración del Bean `CorsConfigurationSource` en el middleware de seguridad de Spring Security permitiendo los orígenes de desarrollo.

---

## 📅 2026-04-12 — RTR + jti + Detección de Reuso ✅

### 🎯 Problema
Refresh tokens estáticos y sin rotación que permitían ataques de replay indefinidos.

### 📐 Solución
Implementación de Refresh Token Rotation (RTR). El uso de un refresh token invalida ese token e interactúa emitiendo una nueva pareja. Se agregó validación por `jti` única.

---

## 📅 2026-04-11 — Docker Fix ✅

### 🎯 Problema
Fallo de conexión entre los microservicios y PostgreSQL debido a resolución incorrecta del host del contenedor.

### 📐 Solución
Uso de redes puente en Docker Compose y variables de entorno parametrizadas.

---

## 📅 2026-04-11 — Email Verification Logic ✅

### 🎯 Problema
Las cuentas de usuario se activaban por defecto sin validar la existencia real de su casilla de email.

### 📐 Solución
Creación de la entidad `EmailVerificationToken` y el endpoint `/verify-email`.

---

## 📅 2026-04-11 — Password Reset Logic ✅

### 🎯 Problema
Flujo básico de re-establecimiento de contraseña no implementado.

### 📐 Solución
Creación de endpoints para generación y validación de tokens de recuperación de contraseñas.

---

## 📅 2026-04-09 — Testcontainers Setup ✅

### 🎯 Problema
Falta de entorno determinista para pruebas de integración de base de datos y caché, forzando dependencias de instancias locales activas.

### 📐 Solución
Configuración de la clase base `AbstractIntegrationTest` para levantar PostgreSQL y Redis en Docker de forma automatizada mediante Testcontainers durante la suite de pruebas.

---

## 📅 2026-06-23 — Fix OAuth2 redirect + Redis serialization + APP_FRONTEND_URL

### 🎯 Problemas
1. OAuth2 redirect iba a `http://localhost:9000/#/auth/callback?code=xxx` pero frontend esta en puerto 9200.
2. `UriComponentsBuilder.fromUriString(...).queryParam(...)` ponia el `?code=` antes del `#` en la URL → browsers strippean el fragmento de 302 redirects.
3. `Map.of()` crea `ImmutableCollections$MapN`, clase interna JDK que `GenericJackson2JsonRedisSerializer` no puede deserializar.
4. `APP_FRONTEND_URL` no se inyectaba al auth-service en docker-compose.yml.

### 📐 Soluciones
| # | Fix | Archivo |
|---|-----|---------|
| 1 | Agregar `APP_FRONTEND_URL=http://localhost:9200` al `.env` raiz y pasarlo al auth-service en docker-compose.yml | `.env`, `docker-compose.yml:73` |
| 2 | Construccion manual de URL en vez de `UriComponentsBuilder`: `frontendUrl + "/#/auth/callback?code=" + code` | `OAuth2AuthenticationSuccessHandler.java:170` |
| 3 | `new HashMap<>(Map.of(...))` para que Jackson pueda deserializar el Map desde Redis | `OAuth2AuthenticationSuccessHandler.java:167` |
| 4 | `/users/me` ahora retorna `tenantId`, `role`, `plan` desde `UserTenantRepository` + `TenantRepository` | `UserServiceImpl.java` |

### 🔬 Lecciones
- `UriComponentsBuilder` con fragmentos: query params se agregan antes del `#`. Para URLs tipo `/#/path?key=val` hay que concatenar manualmente.
- `GenericJackson2JsonRedisSerializer` + `DefaultTyping.NON_FINAL` no soporta `ImmutableCollections` de `Map.of()`. Usar siempre `HashMap` explicito.
- Las variables de entorno en docker-compose no tienen defaults "inteligentes" — hay que pasarlas explicitamente incluso si parecen obvias.

**Archivos modificados:** `OAuth2AuthenticationSuccessHandler.java`, `.env`, `docker-compose.yml`, `UserServiceImpl.java`, `UserEntityResponse.java`, `UserMapper.java`
**Estado:** ✅ RESUELTO

---

## 2026-07-16 — Auth criticals (JWT, logout, cookie) + CORS dual layer

### 🔴 Criticals resueltos

| # | Gap | Fix |
|---|-----|-----|
| 1 | **JWT secret sin validación** — `Keys.hmacShaKeyFor()` acepta cualquier tamaño | `@PostConstruct init()` en `JwtServiceImpl.java` valida `keyBytes.length >= 32`, lanza `IllegalArgumentException`. Key cacheada en `this.key`. |
| 2 | **Logout traga excepciones** — Si `extractUserId()` falla, el catch lo traga | `extractUserId()` movido antes del try-block en `AuthServiceImpl.java`. Excepción se propaga. Try solo cubre `revokeToken` + `deleteByUserId`. |
| 3 | **Cookie OAuth2 sin Secure** — Siempre false | `cookie.setSecure(request.isSecure())` en `OAuth2IntentCookieFilter.java` — HTTPS→Secure, HTTP→false. |
| 4 | **Token reseteo en URL** — En hash fragment | Aceptado sin cambios: hash fragment + one-time + TTL 15min + referrer-policy.riesgo mitigado. |

### Tests: 130 auth tests, 0 fallos.

### 🔍 CORS — Causa raíz (doble capa)

| Intento | Resultado | Lección |
|---------|-----------|---------|
| `globalcors` con `allowed-origin-patterns` | POST devuelve 403 sin ACAO. SCG bug conocido. | `allowed-origin-patterns` no matchea literales con `allowCredentials(true)` |
| Sin `globalcors` | OPTIONS devuelve 403. SCG intercepta preflight internamente. | SCG requiere `globalcors` para responder OPTIONS. Sin él, 403 siempre. |
| **Solución final: doble capa** | ✅ OPTIONS 200 + POST 201 | Gateway: `globalcors` con `allowed-origins` (exacto). Auth: `setAllowedOrigins` + `allowCredentials(true)`. |

### Archivos modificados

```
backend/auth/src/main/java/auth/pymes/service/impl/JwtServiceImpl.java
backend/auth/src/main/java/auth/pymes/service/impl/AuthServiceImpl.java
backend/auth/src/main/java/auth/pymes/common/config/OAuth2IntentCookieFilter.java
backend/auth/src/main/java/auth/pymes/common/config/WebCorsConfig.java          # setAllowedOriginPatterns → setAllowedOrigins
backend/gateway-pymes/src/main/resources/application.yaml                       # globalcors con allowed-origins
docs/GAPS.md                                                                     # marks resolved
docs/DAILY_REPORTS_AUTH_SOLUTIONS.md                                             # this entry
```

**Estado:** ✅ RESUELTO
