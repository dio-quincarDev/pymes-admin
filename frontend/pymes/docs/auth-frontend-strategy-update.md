# 🔐 Estrategia de Autenticación Unificada y Email Flow

> Plan de adaptación para el frontend siguiendo el sistema de diseño "Fintech: Deep Forest & Copper".
> Fecha: 2026-05-03
> Estado: EN PROGRESO (Segmentado)

---

## 📋 Resumen Ejecutivo

**Objetivo:** Rediseñar y completar el flujo de autenticación para que sea más intuitivo ("Negocio Primero"), eliminando integraciones no deseadas (Facebook) y unificando el acceso local con Google OAuth2.

---

## 🎨 Principios de Diseño

- **Enfoque:** Sobrio y Minimalista.
- **Flujo de Registro:**
    1.  **Nombre del Negocio/Empresa:** Primer campo mandatorio.
    2.  **Datos Personales / Google:** Paso secundario.
- **Visibilidad:** Ocultar el campo de `slug`. El sistema lo manejará internamente.
- **Unificación:** Integrar el botón de Google OAuth2 de forma armónica dentro del formulario de Login/Registro.
- **Social:** Únicamente Google. Facebook queda descartado para esta fase.

---

## 🧩 Plan de Implementación Frontend

### Segmento 3: Configuración y Servicios
- Actualización de `auth.service.ts` con nuevos endpoints (`forgotPassword`, `resetPassword`, `acceptInvitation`).
- Limpieza de modelos para eliminar campos de slug y Facebook.
- Definición de nuevas rutas en el router de autenticación.

### Segmento 4: Interfaces Unificadas
- **Registro:** Componente de dos pasos o formulario fluido que priorice el nombre de la empresa.
- **Login:** Diseño centrado, fondo forest deep, con integración clara de Google.
- **Minimalismo:** Eliminación de sombras pesadas, uso de tipografía limpia y acentos en cobre.

### Segmento 5: Flujos de Soporte
- Implementación de `ForgotPasswordPage.vue`, `ResetPasswordPage.vue` y `AcceptInvitationPage.vue`.
- Asegurar que el diseño sea consistente con `VerifyEmailPage.vue`.

---

## 🎨 Paleta de Colores (Referencia)

- **Forest Deep (Base):** `#0B1210`
- **Surface Pine (Tarjetas):** `#1B2624`
- **Copper (Acentuado):** `#A3785E`
- **Parchment (Texto):** `#E2E8E4`

---

## ✅ Verificación
- Navegación completa sin errores de consola.
- Responsividad en móviles y tablets.
- Coherencia visual total entre el frontend y los nuevos emails generados por el backend.
