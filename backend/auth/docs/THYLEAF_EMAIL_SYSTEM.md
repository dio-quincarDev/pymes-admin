# 📧 Sistema de Emails con Thymeleaf Component-Based

> Plan de reestructuración para emails profesionales usando Thymeleaf
> Actualizado: 2026-05-03
> Estado: EN PROGRESO (Segmentado)

---

## 📋 Resumen Ejecutivo

**Problema:** Los emails actuales tienen el HTML hardcodeado como strings en los servicios Java, dificultando el mantenimiento y edición.

**Solución:** Implementar un sistema de templates Thymeleaf segmentado en fases, con componentes reusables y una infraestructura centralizada.

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

## 🎨 Guía de Estilo

- **Primario:** `#A3785E` (Copper) para botones y acentos.
- **Fondo:** Gris muy claro para el cuerpo exterior y blanco puro para el contenedor del mensaje.
- **Tipografía:** Sans-serif moderna.
- **Minimalismo:** Sin sombras, bordes redondeados suaves (8px), espaciado amplio.

---

## ✅ Validación
- Pruebas unitarias para renderizado de plantillas.
- Verificación de envíos vía SMTP (Mailtrap/SMTP Local).
- Comprobación de visualización en múltiples clientes de correo.
