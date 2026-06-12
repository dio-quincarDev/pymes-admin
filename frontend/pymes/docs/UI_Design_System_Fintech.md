# 💎 Sistema de Diseño PYMEQ: Fintech Minimalist High-End

Este documento unifica la visión arquitectónica y la estética visual de la plataforma, estableciendo las reglas para una interfaz profesional, moderna y eficiente.

---

## 1. Fundamentos Visuales (Look & Feel)
- **Minimalismo Operativo:** Menos es más. Cada elemento debe justificar su existencia.
- **Estética "High-End":** Inspirado en estándares premium (Revolut, Stripe, Apple Card).
- **Dinámico, no Aburrido:** Uso de micro-interacciones, profundidad (depth) y capas para evitar una interfaz plana.

## 2. Paleta de Colores: Deep Forest & Copper
Basado en la regla 60/30/10 para balance visual.

| Categoría | Token | Hex | Aplicación |
| :--- | :--- | :--- | :--- |
| **Dominante (60%)** | `forest-deep` | `#0B1210` | Fondo base (Inmersivo) |
| **Superficie** | `surface-pine` | `#1B2624` | Tarjetas, Sidebar (Elevación sutil) |
| **Secundario (30%)** | `parchment` | `#E2E8E4` | Texto principal, valores numéricos |
| **Acento (10%)** | `brand-copper` | `#A3785E` | Botones (CTA), Mesh Gradients, Highlights |
| **Soporte** | `sage-muted` | `#71837F` | Hovers, iconos secundarios, captions |

---

## 3. Tipografía de Precisión
Se abandona el escalado genérico por uno refinado y responsivo.
- **Fuente Principal:** **Inter** o **Plus Jakarta Sans** (Legibilidad superior).
- **Escalado Responsivo:**
    - **Logo/Títulos:** `text-h5` (24px) con `weight: 800` (reemplaza al h3 gigante).
    - **Cuerpo:** Base de **14px** para una sensación más compacta y profesional.
    - **Labels:** **12px** con `text-weight-bold` y `text-accent`.

---

## 4. Espaciado y Grilla (8px System)
Se elimina el exceso de separación para evitar el efecto "estirado".
- **Gutter:** Reducido a **16px** (móvil) y **24px** (escritorio).
- **Escala Proporcional:** 4px (xs), 8px (sm), 16px (md), 24px (lg).
- **Layout:** Contenedores centrados con `max-width` inteligente para no desbordar en monitores ultra-wide.

---

## 5. Modernidad y Capas (Glassmorphism)
- **Glass Effect:** Contenedores con `background: rgba(27, 38, 36, 0.7)` y `backdrop-filter: blur(12px)`.
- **Border-light:** Bordes sutiles de 1px en `sage-muted` (opacidad 0.1) para delimitar sin ensuciar.
- **Sombras:** De sombras negras pesadas a `box-shadow: 0 10px 30px rgba(0,0,0,0.2)`.

---

## 6. Arquitectura de Interacción (UX)
- **Skeleton Screens:** Cargas que replican la estructura real, eliminando el parpadeo.
- **Micro-interacciones:**
    - **Fade-in Up:** Entradas suaves (20px desplazamiento, 0.4s).
    - **Brand Glow:** Brillo suave de color cobre al pasar el cursor por elementos clave.
    - **Press Feedback:** Escala sutil (0.98) al hacer clic para confirmar intención.

---

## 7. Reglas de Degradados
- **Mesh Gradients:** Solo permitidos en **Texto**.
- **Configuración:** Ángulo de 135°, usando variaciones del `brand-copper`.
