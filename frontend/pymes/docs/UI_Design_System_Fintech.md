# UI Design System: Fintech Audit Toolkit

## 1. Fundamentos de Arquitectura Frontend
Este sistema prioriza la velocidad de percepción y la estabilidad visual mediante las siguientes estrategias:

* **Skeleton Screens:** Estructuras de carga que replican la disposición del grid 3:9, eliminando el parpadeo visual.
* **Optimistic UI:** Las validaciones contables se reflejan instantáneamente en la interfaz; la reversión ocurre solo ante fallos de persistencia.
* **Progressive Illusions:** Micro-interacciones escalonadas para mantener la fluidez en el procesamiento de grandes volúmenes de datos.

## 2. Sistema de Grilla (Grid)
* **Ratio:** 3:9 (Sidebar de navegación vs. Espacio de trabajo principal).
* **Gutter (Space Between):** 40px constantes para separación de módulos.

## 3. Capas y Elevación (Layering & Elevation)
* **Contrast:** Contenedores con *tight shadows* (sombras cerradas) para una delimitación clara sin difuminación excesiva.
* **Glow:** Implementación de *Brand Glows* en los puntos de interacción para reforzar la identidad.
* **Elevation:** Uso de *3D lift* para crear jerarquía en paneles de auditoría críticos y modales.

## 4. Reglas de Color y Degradados
* **Distribución:** 60% Dominante, 30% Secundario, 10% Acento.
* **Gradient Rules:** - **Hues:** 60 grados.
    - **Direction:** 135 grados.
    - **Method:** Mesh Points.
    - **Limit:** Solo aplicable en **Texto**.

## 5. Especificación de Paleta: Deep Forest & Copper (Atrevida)

| Elemento | Token | Hexadecimal | Aplicación UI |
| :--- | :--- | :--- | :--- |
| **Dominante (60%)** | `bg-forest-deep` | `#0B1210` | Fondo base de la aplicación |
| **Superficie** | `bg-surface-pine` | `#1B2624` | Tarjetas de datos y Sidebar (Elevación) |
| **Secundario (30%)** | `txt-parchment` | `#E2E8E4` | Texto principal y valores numéricos |
| **Acento (10%)** | `brand-copper` | `#A3785E` | Botones, Brand Glows y Mesh Text Gradients |
| **Soporte** | `ui-sage-muted` | `#71837F` | Estados de hover y elementos de apoyo |

## 6. Componentes de Interacción (Buttons)
* **Hover State:** Transición suave al color `ui-sage-muted`.
* **Press Feedback:** Reducción de escala y oscurecimiento del tono.
* **Ripple Easing:** Animación de expansión radial al hacer clic para confirmar la acción.
