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

## 🚀 Estado de Implementación (Actualizado 2026-05-04)

### ✅ Finalizado: Flujo "Empresa Primero" (Company First)
Se ha rediseñado el onboarding para priorizar la identidad del negocio:
1.  **Home (Punto Único de Entrada):** El registro comienza obligatoriamente con el nombre de la empresa. El `slug` se genera automáticamente y de forma invisible para reducir la carga cognitiva.
2.  **Registro Consolidado:** El `RegisterPage` actúa como el paso final de asignación de administrador, vinculando automáticamente los datos capturados en la Home.
3.  **OAuth2 Intent:** Integración total con el backend para que el login con Google respete la empresa creada en el paso previo.

### ✅ Finalizado: Estructura Visual Unificada
1.  **AuthLayout.vue:** Se implementó un layout centralizado para todas las páginas de autenticación.
2.  **Branding Persistente:** Logo PYMEQ y estética "Fintech" (Forest Deep & Copper) aplicada consistentemente.
3.  **Recordar Sesión:** Lógica de persistencia de email implementada en el login local.

---

## 🛠️ Próximos Pasos (En Curso)

### Flujo de Verificación de Email
- Implementar la lógica reactiva en `VerifyEmailPage.vue` para procesar el token que llega por URL.
- Asegurar consistencia visual con el sistema de diseño.

### Flujo de Reset Password
- Refinar `ForgotPasswordPage.vue` y `ResetPasswordPage.vue`.
- Validar la transición entre el envío del correo y el establecimiento de la nueva contraseña.

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
