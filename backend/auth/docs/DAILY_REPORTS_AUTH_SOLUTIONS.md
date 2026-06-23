# 📊 Daily Reports & Auth Solutions — Historial de Implementaciones

Este documento registra de manera cronológica el historial de decisiones técnicas, problemas resueltos y la evolución de la arquitectura del microservicio de autenticación (`auth`).

---

## 📋 ÍNDICE DE ROADMAP Y ESTADO

### 🔲 Bugs Pendientes
- **[P2] Facebook OAuth2** — *POSTERGADO* (Meta no aprobó la verificación de la empresa. Queda pendiente indefinidamente hasta obtener credenciales válidas en la consola de Meta Developer).

### 🚧 En Progreso
- **Defensa en profundidad + Code Exchange OAuth2** — En proceso de validación y robustecimiento continuo.

### ✅ Historial de Soluciones (Orden Cronológico Inverso)
1. [2026-06-23 — Fix OAuth2 redirect + Redis serialization + APP_FRONTEND_URL](#-2026-06-23--fix-oauth2-redirect--redis-serialization--app_frontend_url)
2. [2026-06-21 — Cleanup AuthApiController: Business Logic Extraction](#-2026-06-21--cleanup-authapicontroller-business-logic-extraction)
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
