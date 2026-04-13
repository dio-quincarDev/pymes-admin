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
*Documento generado por el equipo de arquitectura de Pymeq.*
