# 📊 Daily Reports & Auth Solutions — Historial de Implementaciones

> Registro histórico de decisiones técnicas, problemas resueltos y roadmap de desarrollo.

---

## 📋 ÍNDICE

### 🔲 Bugs Pendientes
- **[P2] Facebook OAuth2** — No testeado, redirect URI no configurado

### 🚧 En PROGRESO
- (vacío)

### ✅ Bugs RESUELTOS (más reciente primero)
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
