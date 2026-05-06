# 📊 Daily Reports & Auth Solutions — Historial de Implementaciones

> Registro histórico de decisiones técnicas, problemas resueltos y roadmap de desarrollo.

---

## 📋 ÍNDICE

### 🔲 Bugs Pendientes
- **[P2] Facebook OAuth2** — POSTERGADO (Meta no aprobó empresa)

### 📌 Tareas por Hacer
- [x] **[P1] Logout Global** — Cerrar todas las sesiones del usuario (COMPLETADO 2026-05-05)
- [x] **[P1] Registro Pending Token** — No crear usuario hasta verificar email (COMPLETADO 2026-05-05)

### 🚧 En PROGRESO
- [2026-05-05 — Diseño Profesionales Emails (Thymeleaf)](#2026-05-05--diseño-profesional-emails-thymeleaf-)

### ✅ Bugs RESUELTOS (más reciente primero)
- [2026-05-05 — Registro Pending Token (Strict Persistence)](#2026-05-05--registro-pending-token-strict-persistence-)
- [2026-05-05 — Email Verification Token-Email Mismatch](#2026-05-05--email-verification-token-email-mismatch-)
- [2026-05-05 — Logout Global (Multi-session Revocation)](#2026-05-05--logout-global-multi-session-revocation-)
- [2026-05-03 — Pruebas Unitarias e Integración (EmailService Refactor)](#2026-05-03--pruebas-unitarias-e-integración-emailservice-refactor-)
- [2026-05-03 — Reingeniería del Flujo de Emails (Thymeleaf)](#2026-05-03--reingeniería-del-flujo-de-emails-thymeleaf-)
- [2026-04-22 — InvitationService Technical Debt Coverage](#2026-04-22--invitationservice-technical-debt-coverage-)
- [2026-04-22 — Password Reset Notification Flow](#2026-04-22--password-reset-notification-flow-)
- [2026-04-22 — Token Exposure in Email Verification](#2026-04-22--token-exposure-in-email-verification-)
- [2026-04-21 — Prioridad de Tenants (OAuth2)](#2026-04-21--prioridad-de-tenants-oauth2-)
- [2026-04-21 — OAuth2 Intent via Cookie](#2026-04-21--oauth2-intent-via-cookie-)
- [2026-04-20 — OAuth2 Pre-Auth Intent](#2026-04-20--oauth2-pre-auth-intent-atomic-register-)
- [2026-04-20 — NoResourceFoundException /login](#2026-04-20--noresourcefoundexception-login)
- [2026-04-20 — Errores OAuth2 + LoginOauth2Controller](#2026-04-20--errores-oauth2--loginoauth2controller)
- [2026-04-17 — OAuth2 via Gateway](#2026-04-17--oauth2-via-gateway-)
- [2026-04-13 — Email Verification + HTML](#2026-04-13--email-verification--html-)
- [2026-04-13 — CORS Implementado](#2026-04-13--cors-implementado-)
- [2026-04-12 — RTR + jti + Detección de Reuso](#2026-04-12--rtr--jti--detección-de-reuso-)
- [2026-04-11 — Docker Fix](#2026-04-11--docker-fix-)
- [2026-04-11 — Email Verification Logic](#2026-04-11--email-verification-logic-)
- [2026-04-11 — Password Reset Logic](#2026-04-11--password-reset-logic-)
- [2026-04-09 — Testcontainers Setup](#2026-04-09--testcontainers-setup-)

### 📌 Features Completas
- [2026-04-16 — Roadmap Completado](#2026-04-16--roadmap-completado-)

---

## 📅 2026-05-05 — CI/CD Fix (Tests Separation) ✅

### 🎯 Problema
El pipeline de CI fallaba porque:
1. Usaba `verify` que ejecuta tanto `surefire` (unit) como `failsafe` (integration)
2. Con perfil `test` intentaba conectarse a PostgreSQL pero sin testcontainers
3. Los integration tests necesitan Docker y perfiles específicos

### 📐 Solución
1. **Separación de jobs en CI (.github/workflows/ci.yml)**:
   - `backend-auth-unit-test`: Solo unit tests (`mvn test`) with profile `test` → H2
   - `backend-auth-integration-test`: Integration tests (`mvn verify`) with profile `integration` → Testcontainers

2. **Docker service**: Añadido en workflow para testcontainers:
```yaml
services:
  docker:
    image: docker:20.10.16
    options: --privileged
```

3. **Fix en pom.xml**: Excluído `AuthApplicationTests` de surefire (extendía `AbstractIntegrationTest` con testcontainers)

### 🧪 Validación
- Unit Tests: ✅ 120 tests passing (H2 in-memory)
- Integration Tests: ✅ Working (testcontainers creates PostgreSQL + Redis)

### 📋 Cómo correr tests

**Unit Tests (sin Docker):**
```bash
cd backend/auth && ./mvnw test -B -Dspring.profiles.active=test
```

**Integration Tests (necesita Docker):**
```bash
cd backend/auth && ./mvnw verify -B -Dspring.profiles.active=integration
```

### 🔲 Pendiente
- [ ] Full CI validation in GitHub Actions

---

## 📋 TAREAS POR HACER

### 📅 [P1] Logout Global (PENDIENTE)

**Prioridad:** Alta

**Descripción:**
Cerrar TODAS las sesiones activas del usuario (no solo el device actual).

**Flujo:**
```
POST /logout
  1. Invalidar access token (blacklist) - ya implementado
  2. Eliminar TODOS refresh tokens del usuario (nuevo)
  3. Retornar { success: true, allSessionsRevoked: true }
```

**Backend:**
- `AuthServiceImpl.logout()`: + `refreshTokenRepository.deleteByUserId(userId)`
- `LogoutResponse`: + `allSessionsRevoked: true`

**Tareas:**
- [ ] Modificar `AuthServiceImpl.logout()` para eliminar todos los refresh tokens
- [ ] Actualizar `LogoutResponse` con campo `allSessionsRevoked`
- [ ] Frontend: limpiar todos los tokens del storage

**Referencia:** `backend/auth/src/main/java/auth/pymes/service/impl/AuthServiceImpl.java`

---

### 📅 [P1] Registro Pending Token (PENDIENTE)

**Prioridad:** Alta

**Descripción:**
No crear usuario en DB hasta verificar email. El usuario solo se crea DESPUÉS de verificar el enlace.

**Flujo:**
```
1. POST /register { email, password, name, company... }
   → NO crea usuario en DB
2. Guardar datos temporales en Redis (key: temp-register:{token})
   → TTL: 10 minutos
3. Enviar email verificación

4. POST /verify-email { token: "tempToken" }
5. Validar token temporal en Redis
6. Crear User + Tenant + UserTenant
7. Generar auth tokens → Login automático
8. Eliminar temp token
```

**Backend:**
- Nuevo método `verifyAndRegister(token)` en AuthService
- Almacenar datos temporales en Redis
- Adaptar EmailVerificationService para pending registration

**Tareas:**
- [ ] Modificar `/register` para guardar datos en Redis (no en DB)
- [ ] Crear método `verifyAndRegister(tempToken)` en AuthServiceImpl
- [ ] Modificar flujo `/verify-email` para completar registro
- [ ] Frontend: actualizar flujo register → login automático

**Referencia:**
- `backend/auth/src/main/java/auth/pymes/service/impl/AuthServiceImpl.java`
- `backend/auth/src/main/java/auth/pymes/service/EmailVerificationService.java`

---

### 📅 [P1] Thymeleaf Email System (COMPLETADO)

**Prioridad:** Alta (Completado 2026-05-03)

**Descripción:**
Refactorizar el sistema de emails para usar plantillas Thymeleaf component-based en lugar de HTML hardcodeado en strings Java.

**Estado actual:**
- ✅ Thymeleaf implementado y configurado.
- ✅ Estructura component-based en `templates/`.
- ✅ `_base.html` con estilos centralizados (Copper & Forest).
- ✅ Components reusables (button, header, footer).
- ✅ Templates específicos para cada tipo de email (verification, reset, invitation).
- ✅ EmailTemplateService implementado.
- ✅ EmailVerificationServiceImpl refactorizado.
- ✅ PasswordResetServiceImpl refactorizado.
- ✅ InvitationServiceImpl refactorizado.

**Tareas:**
- [x] Crear `_base.html` con estilos CSS
- [x] Crear components en `templates/components/`
- [x] Crear `templates/emails/verification.html`
- [x] Crear `templates/emails/password-reset.html`
- [x] Crear `templates/emails/invitation.html`
- [x] Crear EmailTemplateService
- [x] Refactorizar EmailVerificationServiceImpl
- [x] Refactorizar PasswordResetServiceImpl
- [x] Refactorizar InvitationServiceImpl
- [ ] Pruebas de integración E2E con frontend actualizado

**Referencia:** `backend/auth/docs/THYLEAF_EMAIL_SYSTEM.md`

---

## 🔲 BUGS PENDIENTES

### 📅 [P2] Facebook OAuth2 no testeado

**Prioridad:** Baja

**Descripción:**
Facebook OAuth2 no ha sido probado. Necesita configuración en Facebook Developer Console.

**Pendiente:**
- Redirect URI esperado: `http://localhost:8080/login/oauth2/code/facebook`
- Authorized redirect URIs en Facebook Console

**Estado:** POSTERGADO - Meta no ha aprobado la validación de la empresa. Queda pendiente indefinidamente hasta obtener credencialesvlidas de Meta/Facebook Developer Console.

**Actualización (2026-04-28):**
- Meta/Facebook Developer拒绝了 la solicitud de verificación de empresa
- No se recibieron credenciales OAuth2
- Se procedió con Google OAuth2 exclusivamente
- El slug es generado automáticamente por el frontend y enviado en el payload de registro

---

### 📅 [P1] Email Verification Token-Email Mismatch

**Prioridad:** Alta

**Descripción:**
El flujo de verificación de email no valida que el token coincida con el email del query param. El backend ignora el email enviado en la URL, permitiendo que cualquier token válido verifique cualquier cuenta asociada.

**Fallas identificadas:**
1. `VerifyEmailRequest` solo acepta `{ token }` - email ignorado
2. Redis almacena `email:verify:{token}` → email pero no hay validación cruzada
3. Frontend no puede confirmar qué email verifica antes del llamado
4. Usuario ve el resultado DESPUÉS de hacer clic, no antes

**Flujo vulnerable:**
```
Frontend envía: POST /verify-email { token: "abc123" }
Backend recibe: solo valida token en Redis → cualquier email asociado
```

**Solución implementada:** Validación token-email + respuesta enriquecida

**Tareas:**
- [ ] Agregar `email` a `VerifyEmailRequest` DTO
- [ ] Crear `VerifyEmailResponse` DTO
- [ ] Modificar `EmailVerificationServiceImpl.verifyEmail()` con validación cruzada
- [x] Actualizar `AuthApiController.verifyEmail()`
- [x] Frontend: modificar `authService.verifyEmail()` para enviar `{ token, email }`

**Referencia:** `backend/auth/docs/VERIFICATION_SECURITY_FIX.md`

---

## ✅ BUGS RESUELTOS

---

## 📅 2026-05-05 — Registro Pending Token (Strict Persistence) ✅

### 🎯 Problema
El sistema creaba el usuario y la empresa en la base de datos inmediatamente después del formulario de registro, incluso si el email nunca se verificaba. Esto ensuciaba la base de datos con cuentas "fantasma" y bots.

### 📐 Solución
1. **Persistencia Temporal (Redis)**: Se modificó `AuthServiceImpl.register()` para que los datos del formulario (`RegisterRequest`) se guarden en Redis con un TTL de 15 minutos en lugar de la DB.
2. **Registro Atómico Post-Verificación**: El método `AuthService.completeRegistration()` ahora es el encargado de crear el `User`, `Tenant` y `UserTenant` en una única transacción SQL solo después de que el email sea validado.
3. **Login Automático**: Al completar la verificación con éxito, el servidor genera y devuelve los tokens JWT inmediatamente, permitiendo al frontend iniciar sesión sin pedir credenciales de nuevo.

### 🧪 Validación
- Compilación exitosa del backend.
- Flujo de servicios verificado (Register -> Redis -> Verify -> SQL Persistence).

---

## 📅 2026-05-05 — Email Verification Token-Email Mismatch ✅

### 🎯 Problema
Vulnerabilidad de seguridad donde el sistema no validaba que el token de verificación perteneciera realmente al email proporcionado, permitiendo potenciales ataques de sustitución de tokens.

### 📐 Solución
1. **Validación Cruzada**: Se actualizó `VerifyEmailRequest` para incluir obligatoriamente el campo `email`.
2. **Security Fix en Service**: `EmailVerificationServiceImpl.verifyEmail()` ahora compara el email almacenado en Redis contra el email enviado en la petición. Si no coinciden, lanza `EmailVerificationTokenInvalidException`.
3. **Frontend Sync**: Se actualizó la página `VerifyEmailPage.vue` para extraer el email de la URL y enviarlo correctamente al backend.

---

## 📅 2026-05-05 — Logout Global (Multi-session Revocation) ✅

### 🎯 Problema
El sistema de logout solo invalidaba el access token actual en Redis, permitiendo que otras sesiones activas (en otros dispositivos o navegadores) permanecieran abiertas mediante el uso de refresh tokens válidos.

### 📐 Solución
1. **Invalidación Masiva**: Se modificó `AuthServiceImpl.logout()` para que, además de revocar el access token actual, elimine todos los refresh tokens asociados al `userId` en la base de datos.
2. **DTO Enriquecido**: Se actualizó `LogoutResponse` para incluir el campo `allSessionsRevoked`, permitiendo al cliente confirmar la seguridad del cierre de sesión.
3. **Persistencia Segura**: La operación se marcó como `@Transactional` para garantizar la atomicidad entre la revocación del JWT y la limpieza de la base de datos.

### 🧪 Validación
- **Unit Tests**: Actualización de `AuthServiceImplTest` verificando la extracción del `userId` del token y la llamada a `refreshTokenRepository.deleteByUserId()`.
- **Resultados**: 10/10 tests pasando en `AuthServiceImplTest`.

---

## 📅 2026-05-05 — Diseño Profesional Emails (Thymeleaf) 📧

### 🎯 Problema
El diseño de los emails estaba muy sobrio y no reflejaba la identidad profesional de una fintech. Necesitaba un diseño responsive, agnóstico al dispositivo y con tipografía profesional.

### 📐 Solución
1. **Diseño Profesional**:
   - Layout fluido responsive (max-width: 600px)
   - Tipografía Inter (Google Fonts)
   - Media queries para mobile
   -.Header con branding (logo PymeQ)
   -.Divisores decorativos

2. **Paleta de Colores (Design System Frontend)**:
| Variable | Hex | Uso |
|----------|-----|-----|
| `$primary` | `#A3785E` | Botones, CTAs |
| `$secondary` | `#E2E8E4` | Texto principal |
| `$accent` | `#71837F` | Texto secundario |
| `$dark` | `#1B2624` | Tarjetas |
| `$dark-page` | `#0B1210` | Fondo |

3. **Componentes Creados**:
| Archivo | Descripción |
|---------|------------|
| `_base.html` | Layout base responsive |
| `_button.html` | CTA profesional |
| `_alert.html` | Box de seguridad |
| `_divider.html` | Divisor decorativo |
| `password-reset.html` | Recuperación |
| `verification.html` | Verificación |
| `invitation.html` | Invitaciones |

### 🔲 Pendientes
- [ ] Validación visual en clientes de correo reales
- [ ] Tests de renderizado

---

## 📅 2026-05-03 — Reingeniería del Flujo de Emails (Thymeleaf) ✅

### 🎯 Problema
El sistema de correos electrónicos tenía el HTML hardcodeado como strings dentro de los servicios de negocio (`EmailVerificationServiceImpl`, `PasswordResetServiceImpl`, `InvitationServiceImpl`), lo que dificultaba el mantenimiento, la edición visual y violaba el principio de responsabilidad única. Además, las plantillas existentes eran inconsistentes y los archivos `.html` en recursos estaban vacíos.

### 📐 Solución
Se implementó un sistema robusto y escalable basado en segmentos:
1. **Infraestructura Core**:
   - `EmailTemplateService`: Servicio especializado en el renderizado de plantillas Thymeleaf.
   - `EmailService`: Fachada única para el envío de correos, abstrayendo la complejidad de `JavaMailSender`.
2. **Sistema de Componentes**:
   - `_base.html`: Layout maestro con estilos inline responsivos y paleta "Fintech Deep Forest & Copper".
   - Fragmentos reusables: `_header`, `_footer` y `_button` (estilo Copper corporativo).
3. **Migración de Contenidos**:
   - Creación de plantillas específicas: `verification.html`, `password-reset.html` e `invitation.html`.
4. **Refactorización de Servicios**:
   - Se eliminaron todos los bloques de HTML hardcodeado en los servicios de auth.
   - Se inyectó `EmailService` para delegar el envío, pasando únicamente el mapa de variables dinámicas.

### 🧪 Validación
- **Consistencia Visual**: Unificación estética entre todos los tipos de comunicación saliente.
- **Mantenibilidad**: Los cambios de diseño ahora se realizan exclusivamente en archivos `.html` sin afectar la lógica de negocio.
- **Limpieza de Código**: Reducción significativa de líneas de código y complejidad en los servicios de implementación.

### 🔲 Pendientes
- [ ] Pruebas de integración con servidor SMTP real (Mailtrap).
- [ ] Adaptación de las páginas del frontend para alinearse con este nuevo flujo (Segmentos 3-5).

---

## 📅 2026-05-03 — Pruebas Unitarias e Integración (EmailService Refactor) ✅

### 🎯 Problema
Tras refactorizar el sistema de emails para usar `EmailService` (interfaz centralizada) en lugar de `JavaMailSender` directamente, las pruebas unitarias e integración fallaron:
- Error de compilación: `JavaMailSender cannot be converted to EmailService`
- Runtime errors por campos inexistentes (`fromEmail`)

### 📐 Solución

**1. Pruebas Unitarias (`src/test/java/auth/pymes/unit/`):**
- `EmailVerificationServiceImplTest.java` - Reemplazado `JavaMailSender` → `EmailService` mock
- `InvitationServiceImplTest.java` - Reemplazado `JavaMailSender` → `EmailService` mock
- `PasswordResetServiceImplTest.java` - Reemplazado `JavaMailSender` → `EmailService` mock
- Eliminados campos `fromEmail` (ahora en `EmailServiceImpl`)
- Agregado `frontendUrl` via `ReflectionTestUtils`

**2. Pruebas Integración (`src/test/java/auth/pymes/integration/`):**
- `AbstractIntegrationTest.java` - Mock `EmailService` en vez de `JavaMailSender`
- `InvitationServiceIntegrationTest.java` - Cleanup mejorado + edge cases
- `PasswordResetIntegrationTest.java` - Edge cases agregados

**3. Edge Cases Agregados:**
| Test | Descripción |
|------|------------|
| `inviteWithInvalidEmailFormat()` | Email inválido → 400 |
| `forgotPasswordInvalidEmailFormats()` | Múltiples formatos inválidos → 400 |

### 🧪 Validación
- Unit Tests: 103/103 ✅
- Integration Tests: 5/5 ✅

### 🔲 Pendientes
- [ ] Pruebas E2E con servidor SMTP real

---

---

## 📅 2026-04-22 — InvitationService Technical Debt Coverage ✅

### 🎯 Problema
La cobertura de tests unitarios para InvitationServiceImpl estaba incompleta. Existían gaps en validaciones críticas (USER_NOT_IN_TENANT, EMAIL_ALREADY_INVITED, Roles, Transactions) y no había tests de integración.

### 📐 Solución
1. **Tests Unitarios Implementados** (23 total):
   - IdentityResolutionTests (5): OAuth2User principal, UserDetails principal, String principal, Invalid principal → throws AuthorizationException, Success flow con enrichment
   - CreateInvitationTests (7): Happy path, Role hierarchy (ADMIN→OWNER), Max users reached, USER_NOT_IN_TENANT, EMAIL_ALREADY_INVITED, User already member, Viewer insufficient role
   - AcceptInvitationTests (5): Expired invitation, Email mismatch, INVITATION_NOT_FOUND, USER_NOT_FOUND, Success (UserTenant creation + response)
   - CancelInvitationTests (6): By inviter, By another Admin, USER_NOT_FOUND, INVITATION_NOT_FOUND, Viewer cannot cancel, Non-member cannot cancel

2. **Fixes Aplicados**:
   - Mock de `mailSender.createMimeMessage()` para evitar NPE en envío de email
   - Mock de `invitationMapper.toResponse()` para test de éxito
   - Corrección de assertions (buscaban códigos `MAX_USERS_REACHED` → mensajes reales `Tenant has reached...`)
   - Corrección de assertions (`INVITATION_EXPIRED` → `Invitation has expired`)

3. **Tests de Integración Intentados**:
   - Archivo creado: `InvitationServiceIntegrationTest.java`
   - Patrón de auth verificado: Register → Verify Email (SQL) → Login → Token JWT
   - Falls con 409 Conflict debido a limitación del plan FREE

### 🧪 Validación
- Unit Tests: 23/23 ✅ pasando
- Integration Tests: ⚠️ Pendiente (ver nota)
- Gaps restantes identificados:
  * Edge cases (self-invite, token expiry exacto, email existe en otro tenant)
  * Integración real (BD, transacciones, rollback si email falla)
  * Seguridad (rate limiting, auditoría de invitaciones)

### 🔲 Pendientes
- [ ] Tests de integración con Testcontainers (Priority: High)
  * **Nota**: El plan FREE tiene maxUsers=1 hardcodeado en AuthServiceImpl:92, lo que causa 409 al intentar invitar. Requiere cambiar lógica de negocio o usar plan STARTER/PRO para tests.
- [ ] Edge case: Self-invite (owner/admin inviting themselves)
- [ ] Edge case: Token expiry exact moment
- [ ] Edge case: Email exists in another tenant (not current tenant)
- [ ] Security: Rate limiting tests
- [ ] Security: Audit logging verification

---

## 📅 2026-04-22 — Password Reset Notification Flow ✅

### 🎯 Problema
La funcionalidad de recuperación de contraseña estaba incompleta: el sistema generaba el token en Redis pero no enviaba ninguna notificación al usuario, impidiendo que el flujo pudiera completarse desde el frontend.

### 📐 Solución
Se completó la implementación en `PasswordResetServiceImpl.java`:
1. **Integración de JavaMailSender**: Inyección del servicio de correo y configuración de propiedades dinámicas.
2. **Plantilla HTML Segura**: Diseño de una nueva plantilla con acento visual en rojo (seguridad).
3. **Ofuscación de Token**: Se eliminó la exposición del token en texto plano, permitiendo el acceso únicamente a través de un botón de acción.
4. **Consistencia de Puertos**: Ajuste automático de la URL hacia el puerto `9200` (PWA).

### 🧪 Validación
- **Unit Tests**: Actualización de `PasswordResetServiceImplTest` verificando la interacción con `mailSender`.
- **Integration Tests**: Creación de `PasswordResetIntegrationTest` validando el ciclo completo (Solicitud -> Redis -> Reset -> Login) con Testcontainers.

---

## 📅 2026-04-22 — Token Exposure in Email Verification ✅

### 🎯 Problema
El correo de verificación de cuenta exponía el token de seguridad y el enlace completo en formato de texto plano, lo cual era estéticamente pobre y un riesgo de seguridad menor por visibilidad innecesaria.

### 📐 Solución
Refactorización de `EmailVerificationServiceImpl.java`:
1. **Rediseño de Plantilla**: Mejora visual utilizando el sistema de diseño de Pymes Admin (Indigo).
2. **Eliminación de Texto Plano**: Se removió el párrafo "Copia y pega este enlace", dejando el botón como único método de acción.
3. **Legibilidad**: Mejora en los estilos CSS inline para asegurar compatibilidad con clientes de correo y legibilidad del botón.

### 🧪 Validación
- **Smoke Test**: Verificación visual de la generación del HTML.
- **Regression**: Los tests existentes de verificación de email siguen pasando con la nueva estructura.

---

## 📅 2026-04-21 — Prioridad de Tenants (OAuth2) ✅

### 🎯 Problema
El sistema creaba duplicados de "Mi Empresa" o ignoraba inquilinos existentes al usar `intentId`. La lógica de prioridad no estaba clara.

### 📐 Solución
Se estableció una jerarquía de prioridades en `OAuth2AuthenticationSuccessHandler.java`:
1. **Prioridad 1 (Intent ID)**: Si hay una intención de registro, se crea la empresa solicitada (permitiendo múltiples empresas por usuario).
2. **Prioridad 2 (Existente)**: Si no hay intent, se usa el primer inquilino encontrado en la base de datos.
3. **Prioridad 3 (Fallback)**: Solo si es un usuario nuevo sin intención, se crea "Mi Empresa" por defecto.

### 🧪 Validación
- **Unit Tests**: Cobertura del 100% en los 3 escenarios de prioridad.
- **Integration Tests**: Validado con base de datos real en `OAuth2LoginIntegrationTest`.

---

## 📅 2026-04-21 — OAuth2 Intent via Cookie ✅

### 🎯 Problema
OAuth2 con intentId causaba loop infinito en `CustomAuthorizationRequestRepository` porque Spring modifica el state compuesto `uuid:intentId` y tenta guardarlo de nuevo.

### 📐 Solución

**1. OAuth2IntentCookieFilter.java** (nuevo)
... (resto del archivo original)
