# 📄 Estado del Frontend - Pymeq (03 de Mayo, 2026)

## 🎯 Resumen de la Identidad Visual y Arquitectura
Se ha consolidado la identidad de **SaaS Fintech** bajo el nombre **PYMEQ**, centrada en un flujo de usuario simplificado y un diseño minimalista.

### 1. Arquitectura Modular (Feature-based)
- **Ubicación:** `src/modules/auth`
- **Componentes:**
    - `store/`: Estado de sesión centralizado (Pinia).
    - `services/`: Capa de red unificada y métodos de recuperación/invitación añadidos.
    - `pages/`: Vistas de Login, Register, Recovery, Reset, Invitation y Verify.
- **Impacto:** Arquitectura robusta y preparada para la expansión de módulos de negocio.

### 2. Sistema de Diseño: Deep Forest & Copper
- **Base:** Forest Deep (`#0B1210`) y Surface Pine (`#1B2624`).
- **Acento:** Brand Copper (`#A3785E`).
- **Enfoque:** Sobrio y minimalista, optimizado para legibilidad y profesionalismo.

---

## 🛠️ Funcionalidades Implementadas (Actualizado 03 de Mayo, 2026)

### 🚀 Flujo "Negocio Primero"
- **IndexPage:** El usuario inicia ingresando el nombre de su empresa. El `slug` se genera internamente y es invisible, eliminando fricción técnica.
- **Registro Atómico:** Redirección directa al registro con los datos de la empresa pre-cargados en el `authStore`.

### 🔐 Autenticación Unificada
- **RegisterPage:** Unifica el registro nativo (JWT) y Google OAuth2. Diseño centrado y minimalista.
- **LoginPage:** Acceso simplificado con Local + Google. Incluye link de recuperación de acceso.
- **Sin Facebook:** Eliminación completa de la integración con Facebook en todas las interfaces de autenticación.

### 📧 Flujo de Soporte y Emails
- **VerifyEmailPage:** Implementada y funcional con validación de token.
- **ForgotPasswordPage:** Nueva página para solicitar recuperación de contraseña.
- **ResetPasswordPage:** Interfaz para establecer nueva contraseña mediante token seguro.
- **AcceptInvitationPage:** Landing para unirse a equipos de trabajo mediante invitaciones por correo.

### ⚙️ Servicios y Rutas
- **auth.service.ts:** Actualizado con métodos `forgotPassword`, `resetPassword` y `acceptInvitation`.
- **invitation.service.ts:** Nuevo servicio para gestionar invitaciones.
- **Router:** Rutas configuradas para todos los nuevos flujos de identidad.

---

## ✅ Problemas Resueltos

- **🚩 Pantalla Negra:** Resuelto mediante la correcta configuración de puertos y el uso de hash routing (`/#/`) compatible con el servidor Nginx y Capacitor.
- **🚩 Verificación de Email:** Flujo completado. El backend ahora envía correos con plantillas Thymeleaf profesionales y el frontend procesa la verificación correctamente.

---

## 📋 Actualización 03 Mayo 2026 - Alineación Backend

### Cambios Realizados

**1. auth.service.ts:**
- `verifyEmail()` ahora acepta `{token, email}` para validación cruzada

**2. invitation.service.ts (NUEVO):**
- `getPendingInvitations(page, size)` - Lista invitaciones pendientes
- `createInvitation(data)` - Crea nueva invitación
- `cancelInvitation(id)` - Cancela invitación
- `acceptInvitation(token)` - Acepta invitación

### Endpoints Backend Consumidos

| Método | Endpoint | Servicio |
|--------|----------|---------|
| POST | `/auth/verify-email` | authService |
| GET | `/invitations` | invitationService |
| POST | `/invitations` | invitationService |
| DELETE | `/{id}` | invitationService |
| POST | `/invitations/accept` | authService |

---

## 📋 Próximos Pasos Recomendados
1.  **Dashboard Shell:** Comenzar con la estructura del panel principal tras el login exitoso.
2.  **Multitenancy UI:** Implementar el selector de empresas para usuarios que pertenecen a múltiples negocios.
3.  **Audit Logs UI:** Primera fase de visualización de actividad del sistema.

### 📌 Plan de Mejora Auth Service
Ver documento: `AUTH_SERVICE_PLAN.md`

---
*Documento actualizado tras la reestructuración completa de emails y frontend.*
