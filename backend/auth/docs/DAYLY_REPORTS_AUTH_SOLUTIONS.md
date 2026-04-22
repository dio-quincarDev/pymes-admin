# 📊 Daily Reports & Auth Solutions — Historial de Implementaciones

> Registro histórico de decisiones técnicas, problemas resueltos y roadmap de desarrollo.

---

## 📋 ÍNDICE

### 🔲 Bugs Pendientes
- **[P2] Facebook OAuth2** — No testeado, redirect URI no configurado

### 🚧 En PROGRESO
- [2026-04-22 — InvitationService Technical Debt Coverage](#2026-04-22--invitationservice-technical-debt-coverage-)

### ✅ Bugs RESUELTOS (más reciente primero)
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

## 🔲 BUGS PENDIENTES

### 📅 [P2] Facebook OAuth2 no testeado

**Prioridad:** Baja

**Descripción:**
Facebook OAuth2 no ha sido probado. Necesita configuración en Facebook Developer Console.

**Pendiente:**
- Redirect URI esperado: `http://localhost:8080/login/oauth2/code/facebook`
- Authorized redirect URIs en Facebook Console

---

## ✅ BUGS RESUELTOS

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
