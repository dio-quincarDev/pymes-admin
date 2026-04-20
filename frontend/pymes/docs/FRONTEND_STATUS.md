# 📄 Estado del Frontend - Pymeq (13 de Abril, 2026)

## 🎯 Resumen de la Identidad Visual y Arquitectura
Se ha realizado una transición completa desde un "boilerplate" genérico hacia una identidad de **SaaS Fintech** bajo el nombre **Pymeq**.

### 1. Arquitectura Modular (Feature-based)
- **Ubicación:** `src/modules/auth`
- **Componentes:**
    - `store/`: Estado de sesión centralizado (Pinia).
    - `services/`: Capa de red desacoplada (Axios).
    - `pages/`: Vistas de Login, Register y AuthCallback.
    - `types/`: Contratos de TypeScript para el dominio de identidad.
- **Impacto:** El proyecto está preparado para escalar añadiendo módulos como `audit`, `inventory` o `billing` sin colisiones.

### 2. Sistema de Diseño: Deep Forest & Copper
- **Fondo:** Forest Deep (`#0B1210`).
- **Acento:** Brand Copper (`#A3785E`).
- **Layout:** Proporción 3:9 (Sidebar vs Espacio de Trabajo) con Gutter de 40px.
- **Tipografía:** Roboto con espaciado amplio y gradientes de malla en títulos.

---

## 🛠️ Funcionalidades Implementadas (Actualizado 20 de Abril, 2026)
- **Login Local:** Formulario integrado con `authStore.login`.
- **Registro de Entidad:** Formulario con creación automática de `slug` de empresa.
- **Registro Atómico OAuth2 (Intent System):** 
    - Implementado `authService.createOAuth2Intent` para persistir datos de empresa antes del redirect.
    - `AuthOptionsPage.vue` ahora genera un `intentId` y lo envía como `state` a Google/Facebook.
    - El backend procesa el intent y crea la empresa automáticamente tras el login exitoso.
- **Auth Callback Simplificado:** `AuthCallback.vue` ahora solo maneja el guardado de tokens y limpieza de estado persistente, delegando la creación de la empresa al backend.
- **Protección de Rutas:** Navigation Guard activo que redirige a `/login` si no hay sesión.
- **Modo PWA:** Configuración de Docker y Quasar ajustada para construir y servir una Progressive Web App.

---

## ⚠️ Inconvenientes Actuales (Bloqueos)

### 🚩 El Problema de la "Pantalla Negra"
Tras la reconstrucción en Docker (modo PWA), el navegador accede a `http://localhost:9200/#/login?redirect=/` pero solo renderiza el color de fondo, sin el contenido de la página.

**Causas Probables bajo Investigación:**
...
3. **Configuración de Puerto:** Conflicto entre el puerto por defecto de Quasar (9000) y el puerto mapeado/detectado (9200). Se ha intentado forzar el puerto 9000 pero se ha revertido para mantener compatibilidad con el entorno actual.

---

## 📋 Próximos Pasos Recomendados
...
4. **Implementar VerifyEmailPage.vue:** Crear la página y ruta necesaria para completar el flujo de verificación de email local.
---

## 🚩 Feature Pendiente: Verificación de Email (2026-04-16)
...
**Esperado:**
1. Usuario se registra
2. Sistema envia email con link: `http://localhost:9200/#/verify?token=xxx`
3. Usuario hace clic en el enlace
...
**Actual:**
1. Usuario se registra ✅
2. Sistema envia email con link ❌ **El enlace no funciona**
...
```typescript
// src/modules/auth/router/routes.ts - FALTA:
{
  path: '/verify',
  name: 'verify-email',
  component: () => import('../pages/VerifyEmailPage.vue'),
}
```
...
*Documento generado por el equipo de arquitectura de Pymeq.*
