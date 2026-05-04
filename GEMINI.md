# 🏛️ Arquitectura y Convenciones del Proyecto - Pymeq Admin

Este documento contiene los mandatos y convenciones fundamentales que rigen el desarrollo de Pymeq Admin. Estas reglas son fundamentales y deben respetarse en cada cambio.

## 🔐 Módulo de Autenticación y Onboarding

### 1. Mandato "Empresa Primero" (Company First)
El flujo de onboarding está diseñado para priorizar la identidad del negocio sobre la del usuario individual.
- **Punto de Entrada:** El registro **siempre** debe iniciarse capturando el nombre de la empresa en la Home (`IndexPage.vue`).
- **Generación de Slug:** El `slug` de la empresa debe ser generado automáticamente de forma invisible para el usuario a partir del nombre ingresado. No se debe solicitar un slug manual en el UI de onboarding.
- **Registro de Administrador:** La página de registro (`RegisterPage.vue`) actúa estrictamente como el paso final para asignar un administrador a la empresa pendiente (`pendingTenant`).

### 2. Estructura de Interfaz (AuthLayout)
Todas las páginas relacionadas con la autenticación (Login, Registro, Recuperación, Verificación, etc.) deben renderizarse dentro del `AuthLayout.vue`.
- **Layout Centralizado:** El layout maneja el fondo (`Forest Deep`), el contenedor principal (`Surface Pine`) y el branding.
- **Consistencia Visual:** No duplicar etiquetas `q-layout`, `q-page` o logos dentro de los componentes individuales de auth.

### 3. Flujo OAuth2 (Google)
- **OAuth2 Intent:** Para mantener el contexto de la empresa durante el login social, se debe llamar al endpoint `/auth/oauth2/intent` antes de redirigir a Google, pasando el `intentId` resultante como parámetro `state`.

## 🎨 Sistema de Diseño (Fintech: Deep Forest & Copper)

### Paleta de Colores
- **Fondo Base (`$dark-page`):** `#0B1210` (Forest Deep)
- **Tarjetas/Superficies (`$dark`):** `#1B2624` (Surface Pine)
- **Color Primario (`$primary`):** `#A3785E` (Brand Copper)
- **Texto Primario (`$secondary`):** `#E2E8E4` (Parchment)
- **Acentos (`$accent`):** `#71837F` (Sage Muted)

### Componentes y Estilos
- **Sombras:** Usar la clase `tight-shadow` para elevaciones cerradas y técnicas.
- **Brillos:** Usar la clase `brand-glow` para destacar botones y elementos de marca en Copper.
- **Inputs:** Siempre usar el estilo `filled` con fondo oscuro (`rgba(0, 0, 0, 0.2)`) para mantener la estética fintech.

## 📁 Estructura de Archivos (Frontend)
- **Módulos:** Seguir la estructura basada en características (`src/modules/[feature]`).
- **Servicios:** Cada módulo debe tener su propio `services/[name].service.ts` para llamadas a API.
- **Store:** Usar Pinia para el estado global del módulo.

---
*Este documento es la fuente de verdad para la arquitectura del proyecto.*
