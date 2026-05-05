# 📧 Sistema de Emails con Thymeleaf Component-Based

> Plan de reestructuración para emails profesionales usando Thymeleaf
> Actualizado: 2026-05-05
> Estado: EN REVISIÓN

---

## 📋 Resumen Ejecutivo

**Problema:** Los emails actuales tienen el HTML hardcodeado como strings en los servicios Java, dificultando el mantenimiento y edición.

**Solución:** Implementar un sistema de templates Thymeleaf segmentado en fases, con componentes reusables y una infraestructura centralizada.

**Implementado:** Sistema de templates Thymeleaf con diseño profesional, responsive y paleta de colores del frontend.

---

## 🏗️ Arquitectura de Servicios

Se implementará una capa de servicios dedicada exclusivamente a la gestión de correos:

1.  **`EmailTemplateService`**: Encargado de procesar los archivos `.html` de la carpeta `templates/` utilizando el `SpringTemplateEngine` de Thymeleaf para inyectar variables dinámicas.
2.  **`EmailService`**: Fachada única para el envío de correos que utiliza `JavaMailSender`. Este servicio recibe el nombre de la plantilla, el asunto y el mapa de variables, coordinando con `EmailTemplateService` para obtener el cuerpo del mensaje.

---

## 🧩 Plan de Implementación (Segmentado)

### Segmento 1: Infraestructura (Backend)
- Implementación de `EmailTemplateService` y `EmailService`.
- Creación de Layout Base (`_base.html`) y componentes reusables (`_header`, `_footer`, `_button`).
- Estilo visual: Sobrio, minimalista, centrado en legibilidad.

### Segmento 2: Refactorización (Backend)
- Migración de `EmailVerificationServiceImpl`, `PasswordResetServiceImpl` e `InvitationServiceImpl`.
- Eliminación total de código HTML dentro de las clases Java.
- Creación de plantillas finales: `verification.html`, `password-reset.html`, `invitation.html`.

---

## 🎨 Guía de Estilo (Actualizada)

### Paleta de Colores (Design System Frontend)

| Variable | Hex | Uso |
|----------|-----|-----|
| `$primary` | `#A3785E` | Botones, CTAs |
| `$secondary` | `#E2E8E4` | Texto principal |
| `$accent` | `#71837F` | Texto secundario, bordes |
| `$dark` | `#1B2624` | Tarjetas, containers |
| `$dark-page` | `#0B1210` | Fondo |
| `$positive` | `#2D5A27` | Success |
| `$warning` | `#C5A059` | Warnings |

### Componentes Creados

| Archivo | Descripción |
|---------|------------|
| `_base.html` | Layout base responsive con header branding |
| `_button.html` | CTA profesional |
| `_alert.html` | Box de seguridad/alerta |
| `_divider.html` | Divisor decorativo |
| `password-reset.html` | Recuperación de contraseña |
| `verification.html` | Verificación de email |
| `invitation.html` | Invitaciones |

### Características del Diseño

- **Tipografía:** Inter (Google Fonts)
- **Layout:** Fluid responsive (max-width: 600px)
- **Mobile:** Media queries para adaptación
- **Sin iconos:** Solo tipografía y layout profesional

---

## ✅ Validación

- Pruebas unitarias para renderizado de plantillas.
- Verificación de envíos vía SMTP (Mailtrap/SMTP Local).
- Comprobación de visualización en múltiples clientes de correo.