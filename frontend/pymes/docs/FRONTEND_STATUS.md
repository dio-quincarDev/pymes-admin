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

## 🛠️ Funcionalidades Implementadas
- **Login Local:** Formulario integrado con `authStore.login`.
- **Registro de Entidad:** Formulario con creación automática de `slug` de empresa.
- **Flujo OAuth2:** Preparado el componente `AuthCallback.vue` para recibir tokens de Google/Facebook.
- **Protección de Rutas:** Navigation Guard activo que redirige a `/login` si no hay sesión.
- **Modo PWA:** Configuración de Docker y Quasar ajustada para construir y servir una Progressive Web App.

---

## ⚠️ Inconvenientes Actuales (Bloqueos)

### 🚩 El Problema de la "Pantalla Negra"
Tras la reconstrucción en Docker (modo PWA), el navegador accede a `http://localhost:9000/#/login?redirect=/` pero solo renderiza el color de fondo, sin el contenido de la página.

**Causas Probables bajo Investigación:**
1. **Errores de Runtime en JS:** Posible fallo en la inicialización del `authStore` (ej. error al parsear `localStorage` o variables de entorno no definidas).
2. **Resolución de Rutas Modulares:** Vite/Quasar podrían estar fallando al resolver los imports dinámicos `import('../pages/LoginPage.vue')` dentro de los contenedores si la estructura de carpetas tiene discrepancias de permisos.
3. **Configuración de PWA/Service Worker:** El Service Worker podría estar sirviendo una versión corrupta o incompleta del índice.

---

## 📋 Próximos Pasos Recomendados
1. **Depuración en Consola (Navegador):** Revisar errores de JavaScript (F12) para confirmar si es un fallo de inicialización de la App.
2. **Validación de Componentes:** Verificar que los componentes de Quasar se cargan correctamente en la arquitectura modular (posible necesidad de imports explícitos en `quasar.config.ts`).
3. **Limpieza de Persistencia:** Forzar borrado de `localStorage` y Service Workers en el navegador para asegurar un arranque limpio.
---

## 🚩 Feature Pendiente: Verificación de Email (2026-04-16)

### Problema Identificado

El sistema de autenticación **SÍ requiere verificación de email**, pero la implementación está incompleta en el frontend:

| Etapa | Estado | Descripción |
|------|--------|-------------|
| **Registro** | ✅ | Backend envía email de verificación (`AuthServiceImpl.java:110`) |
| **Login** | ✅ | Verifica `isEmailVerified()` antes de autenticar (líneas 137-140) |
| **API Gateway** | ✅ | Ruta `/api/v1/auth/verify-email` expuesta como pública |
| **Frontend** | ❌ | **NO existe** ruta `/verify` ni página de verificación |

### Flujo Esperado vs Actual

**Esperado:**
1. Usuario se registra
2. Sistema envia email con link: `http://localhost:9000/#/verify?token=xxx`
3. Usuario hace clic en el enlace
4. Frontend muestra página de verificación
5. Sistema valida token → marca email como verificado
6. Usuario puede hacer login

**Actual:**
1. Usuario se registra ✅
2. Sistema envia email con link ❌ **El enlace no funciona**
3. Intenta hacer login → Error "Email not verified" ❌ **Usuario bloqueado**

### Rutas Faltantes en Frontend

```typescript
// src/modules/auth/router/routes.ts - FALTA:
{
  path: '/verify',
  name: 'verify-email',
  component: () => import('../pages/VerifyEmailPage.vue'),
}
```

### Endpoints Involucrados

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/api/v1/auth/verify-email` | POST | Valida token y marca email como verificado |
| `/api/v1/auth/resend-verification` | POST | Reenvía token de verificación |

### Impacto

- **Usuario nuevo:** Queda bloqueado tras registro - no puede fazer login
- **Experiencia de usuario:** No hay forma de completar la verificación

### Solución Sugerida

1. Crear página `VerifyEmailPage.vue`
2. Agregar ruta `/verify` al router
3. Manejar query param `token`
4. Llamar a `/api/v1/auth/verify-email`
5. Mostrar UI de éxito/error al usuario

---

*Documento generado por el equipo de arquitectura de Pymeq.*
