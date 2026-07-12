# Sistema de Diseño PYMEQ — Deep Forest & Copper

> **Versión:** 2.0 — Julio 2026
> **Concepto:** Terrestrial Treasury — financiero orgánico para la pyme real

---

## 1. Manifiesto Visual

PYMEQ no es azul corporativo ni púrpura cripto. Es **bosque profundo + cobre artesanal**. Una identidad para negocios que crecen de verdad — lentos, estables, reales.

Tres decisiones fundacionales:

- **Oscuridad como lienzo.** El blanco grita. El negro profundo (`#0B1210`) escucha. En la penumbra, los datos hablan primero.
- **Cobre como acento único.** Un solo color de acento, aplicado con precisión quirúrgica. 10% de la interfaz, 100% de la atención.
- **Sin ruido.** Sin gradientes decorativos, sin sombras innecesarias, sin iconos que parpadean. Cada elemento en la pantalla debe responder a una pregunta del usuario.

> *"Dinero que crece, no que especula."*

---

## 2. Principios de Diseño

| # | Principio | Significado |
|---|-----------|-------------|
| 1 | **Cada pixel justifica su existencia** | Si un elemento no informa, guía o aporta contexto, no pertenece a la interfaz. |
| 2 | **El dinero real no parpadea** | Nada parpadea, nada late, nada respira. La animación solo existe para informar transiciones de estado. |
| 3 | **Profundidad, no decoración** | El glassmorphism expresa jerarquía: más blur = más cerca del usuario. No se usa por estética, se usa por claridad. |
| 4 | **Texto primero, gráfico después** | El número es la verdad. La visualización es su resumen. Siempre mostrar el valor absoluto junto a cualquier gráfico. |
| 5 | **Un acento por vista** | Copper se usa una vez por pantalla, o dos si la segunda es una CTA secundaria. Más que eso compite consigo mismo. |

---

## 3. Paleta de Colores

### 3.1 Base (60/30/10)

| Token | Hex | Rol | Aplicación |
|-------|-----|-----|------------|
| `forest-deep` | `#0B1210` | Fondo base (60%) | Body, zonas de contenido, layouts |
| `surface-pine` | `#1B2624` | Superficies (30%) | Cards, sidebar, modales, dropdowns |
| `parchment` | `#E2E8E4` | Texto principal (30%) | Body text, headings, valores numéricos |
| `brand-copper` | `#A3785E` | Acento (10%) | CTAs, mesh gradients, highlights, focus ring |
| `sage-muted` | `#8A9E99` | Texto secundario | Captions, labels, metadata, hints |
| `sage-light` | `#A8B8B3` | Texto hover, links | Hover states sobre sage-muted |

> `sage-muted` fue elevado de `#71837F` (4.2:1, falla AA) a `#8A9E99` (5.5:1) el 2026-06-16.

### 3.2 Estados semánticos

| Token | Hex | Uso |
|-------|-----|-----|
| `positive` | `#2D5A27` | Éxito, crecimiento, completado |
| `negative` | `#8B4513` | Error, pérdida, alerta crítica |
| `warning` | `#C5A059` | Advertencia, atención, límite próximo |
| `info` | `#71837F` | Información neutral, hints |

Solo aplicarlos con fondo transparente o tintado al 15% de opacidad. Ej: `rgba(139, 69, 19, 0.15)` para fondo de error.

### 3.3 Paleta para Data Visualization

Series de 5 tonos, ordenados por intensidad:

**Cobre (primera serie — para datos primarios):**
| Rango | Hex |
|-------|-----|
| 1 (más claro) | `#D4956B` |
| 2 | `#C5A059` |
| 3 | `#A3785E` |
| 4 | `#8B6B4A` |
| 5 (más oscuro) | `#6E5540` |

**Sage (segunda serie — para datos secundarios/comparativa):**
| Rango | Hex |
|-------|-----|
| 1 | `#C2D0CC` |
| 2 | `#A8B8B3` |
| 3 | `#8A9E99` |
| 4 | `#6B8079` |
| 5 | `#526B65` |

**Semántico para charts:**
- Tendencia positiva: `#2D5A27`
- Tendencia negativa: `#8B4513`
- Neutro/advertencia: `#C5A059`

---

## 4. Sistema Tipográfico

### 4.1 Stack

| Rol | Fuente | Peso | Tamaño base |
|-----|--------|------|-------------|
| Display (hero, page titles) | Outfit | 800 | 2rem (32px) |
| Heading 1 | Outfit | 700 | 1.5rem (24px) |
| Heading 2 | Outfit | 600 | 1.125rem (18px) |
| Subheading | Outfit | 600 | 1rem (16px) |
| Body | Source Sans 3 | 400 | 0.875rem (14px) |
| Small / Caption | Source Sans 3 | 400 | 0.75rem (12px) |
| Numeric / Data | JetBrains Mono | 500 | 0.875rem (14px) |

### 4.2 Reglas

- **Headings en UI** (cards, secciones): Outfit 600, 1rem (16px), sin tracking extra.
- **Títulos de página**: Outfit 700, 1.5rem (24px), con margen inferior de `$pq-space-md`.
- **Labels de formulario**: Source Sans 3 Bold, 0.75rem (12px), uppercase, letter-spacing 0.05em.
- **Valores numéricos en dashboards**: JetBrains Mono 500, 1.25rem (20px) — consistencia en ancho de caracteres.
- **Contraste**: Texto ≥4.5:1 WCAG AA sobre fondo (14px+). Texto pequeño (≤12px) ≥5.5:1.

---

## 5. Sistema de Tokens CSS

Declarados en `quasar.variables.scss` con prefijo `pq-`.

### 5.1 Espaciado (Base 8px)

| Token | Valor | Uso típico |
|-------|-------|------------|
| `$pq-space-4xs` | 4px | Íconos dentro de botones |
| `$pq-space-3xs` | 6px | Padding interno comprimido |
| `$pq-space-2xs` | 8px | Gap entre ícono y texto |
| `$pq-space-xs` | 12px | Padding interno cards pequeñas |
| `$pq-space-sm` | 16px | Padding interno estándar |
| `$pq-space-md` | 24px | Gutter entre columnas, padding cards |
| `$pq-space-lg` | 32px | Separación entre secciones |
| `$pq-space-xl` | 40px | Margen entre cards |
| `$pq-space-2xl` | 48px | Separación de página |
| `$pq-space-3xl` | 64px | Separación hero |

### 5.2 Border Radius

| Token | Valor | Uso |
|-------|-------|------|
| `$pq-radius-2xs` | 2px | Badges minúsculos |
| `$pq-radius-xs` | 4px | Inputs, botones pequeños |
| `$pq-radius-sm` | 6px | Botones estándar |
| `$pq-radius-md` | 8px | Cards, modales |
| `$pq-radius-lg` | 12px | Sidebar, dropdowns |
| `$pq-radius-xl` | 16px | Diálogos grandes |
| `$pq-radius-2xl` | 20px | Drawers, sheets |
| `$pq-radius-full` | 9999px | Avatares, pills |

### 5.3 Sombras

| Token | Valor | Uso |
|-------|-------|------|
| `$shadow-subtle` | `0 2px 8px rgba(0,0,0,0.15)` | Cards default |
| `$shadow-sm` | `0 4px 10px rgba(0,0,0,0.3)` | Dropdowns, elevated cards |
| `$shadow-md` | `0 10px 30px rgba(0,0,0,0.2)` | Modales, popovers |
| `$shadow-lg` | `0 20px 50px rgba(0,0,0,0.25)` | Drawers, dialogs grandes |
| `$shadow-brand` | `0 0 15px rgba(163,120,94,0.3)` | Hover en CTAs |
| `$shadow-brand-hover` | `0 0 20px rgba(163,120,94,0.5)` | Hover intenso |

### 5.4 Transiciones

| Token | Valor | Uso |
|-------|-------|------|
| `$transition-fast` | `0.15s ease` | Hover en íconos, opacidad |
| `$transition-base` | `0.2s cubic-bezier(0.4,0,0.2,1)` | Default para todas las interacciones |
| `$transition-smooth` | `0.3s cubic-bezier(0.4,0,0.2,1)` | Apertura de modales, transiciones de ruta |
| `$transition-slow` | `0.4s ease` | Animaciones de entrada de página |

### 5.5 Z-Index

| Token | Valor |
|-------|-------|
| `$z-base` | 1 |
| `$z-dropdown` | 100 |
| `$z-sticky` | 200 |
| `$z-fixed` | 300 |
| `$z-modal-backdrop` | 400 |
| `$z-modal` | 500 |
| `$z-popover` | 600 |
| `$z-tooltip` | 700 |

### 5.6 Tokens de scrollbar y focus

No hay variables Sass para estos (usar raw CSS). Reglas en app.scss:

```scss
// Scrollbar (webkit)
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb {
  background: rgba(138, 158, 153, 0.3);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover { background: rgba(138, 158, 153, 0.5); }

// Selection
::selection { background: rgba(163, 120, 94, 0.3); }

// Focus ring
:focus-visible {
  outline: 2px solid $primary;
  outline-offset: 2px;
}
```

---

## 6. Atmósfera Envolvente (Profundidad por Capas)

La interfaz construye profundidad en 4 capas:

| Capa | Elemento | CSS |
|------|----------|-----|
| **Fondo** | Body, zonas vacías | `bg-forest-deep` (`#0B1210`) |
| **Superficie** | Cards, sidebar, inputs | `surface-pine` (`#1B2624`) + glass |
| **Contenido** | Texto, íconos, datos | `parchment` (`#E2E8E4`) |
| **Acento** | CTAs, hover, foco | `brand-copper` (`#A3785E`) |

### Glassmorphism

```scss
.glass {
  background: rgba(27, 38, 36, 0.7);    // surface-pine al 70%
  backdrop-filter: blur(12px);
  border: 1px solid rgba(113, 131, 127, 0.1);  // sage-muted al 10%
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}
```

Regla: **a mayor elevación, mayor blur y mayor opacidad.** Card default → blur(12px), modal backdrop → blur(20px). La transparencia no se usa por estética sino para indicar jerarquía: el usuario siempre sabe qué capa está tocando.

---

## 7. Motion & Transiciones

### 7.1 Principios

- **Nada pulse. Nada parpadee.** Movimiento = información, no decoración.
- **50% de los usuarios odia las animaciones.** Todas se desactivan con `prefers-reduced-motion: reduce`.
- **Una animación por evento.** No combinar fade + slide + scale en el mismo elemento.

### 7.2 Timing Chart

| Evento | Efecto | Duración | Easing |
|--------|--------|----------|--------|
| Página entra | fadeInUp (10px) | 0.3s | `cubic-bezier(0.4,0,0.2,1)` |
| Página sale | fadeOut (up 10px) | 0.2s | `ease` |
| Hover en card | translateY(-2px) + shadow | 0.2s | `$transition-base` |
| Hover en CTA | brand-glow intensity | 0.15s | `$transition-fast` |
| Clic en botón | scale(0.96) | 0.15s | `$transition-fast` |
| Stagger (grid) | fadeInUp, delay 0.1s c/u | 0.4s total | `ease` |
| Modal abre | scale(1.02→1) + fade | 0.3s | `$transition-smooth` |
| Skeleton shimmer | desplazamiento 200% | 1.5s infinite | `ease-in-out` |

### 7.3 Clases utilitarias

Ver `app.scss`:
- `.fade-in-up` — entrada suave 0.4s
- `.stagger-children` — delay progresivo en hijos directos
- `.hover-lift` / `.hover-scale` — interacción en hover
- `.press-feedback` — scale(0.96) en active
- Transiciones de ruta: `.fade-*` y `.page-*`

---

## 8. Iconografía

- **Set:** Material Icons Rounded (built-in en Quasar)
- **Peso visual:** Relleno (`filled: true` o `name="sym_r_*"`) para navegación y CTAs; outline para contenido secundario
- **Tamaño estándar:** 20px (íconos inline), 24px (navbar/sidebar), 18px (dentro de botones xs/sm)
- **Color por defecto:** `$accent` (`#8A9E99`) en contenido estático; `$primary` (`#A3785E`) en interactivos y CTAs
- **Sin iconos decorativos.** Cada icono debe tener función semántica (tooltip o label asociado)

---

## 9. Data Visualization

### 9.1 Estilo general

- Gridlines apenas visibles: `rgba(113, 131, 127, 0.08)`
- Sin ejes decorativos — solo ticks esenciales
- Tooltips en glassmorphism (`.glass`)
- Leyendas: texto en `sage-muted`, 12px, alineado a la izquierda
- Sin sombras en áreas/barras — el color es suficiente

### 9.2 Tipos de gráfico por contexto

| Tipo | Cuándo | Colores |
|------|--------|---------|
| Barra | Comparar categorías (gastos por mes) | Cobre 1-5 |
| Línea | Tendencias en el tiempo (ventas, ROI) | Cobre 3 + gradiente de área |
| Dona | Proporciones (ABC de gastos) | Cobre 1-3 + Sage 4-5 |
| Gauge | Indicador único (opex, margen) | Cobre (`$primary`) → Warning → Negative |
| Sparkline | Mini tendencias inline | Cobre 3 (`#A3785E`) |

### 9.3 Umbrales de color en gauges

| Rango | Color |
|-------|-------|
| 0–70% (saludable) | `$positive` (#2D5A27) |
| 70–85% (advertencia) | `$warning` (#C5A059) |
| 85–100% (crítico) | `$negative` (#8B4513) |

---

## 10. Layout Grid & Espaciado

### 10.1 Sistema de columnas

- **Dashboard:** 12 columnas, gutter `$pq-space-md` (24px)
- **Auth / landing:** Centrado, max-width 450px (auth) o 1000px (landing)
- **Móvil:** 8 columnas, gutter `$pq-space-sm` (16px)
- **Breakpoints:** Quasar estándar (xs < 600, sm < 1024, md < 1440, lg < 1920)

### 10.2 Zonas de contenido

| Zona | Ancho | Centrado |
|------|-------|----------|
| Sidebar | 280px fijo | No |
| Main workspace | max-width 1400px | `margin: 0 auto` |
| Auth forms | max-width 450px | `flex: center` |
| Landing | max-width 1000px | `margin: 0 auto` |

### 10.3 Reglas de espaciado

- Separación entre cards: `$pq-space-lg` (32px)
- Padding interno de cards: `$pq-space-md` (24px)
- Separación entre secciones en una página: `$pq-space-xl` (40px)
- Separación entre ítems de formulario: `$pq-space-sm` (16px)

---

## 11. Componentes Base

### 11.1 BaseButton (`src/components/base/BaseButton.vue`)

| Variante | Fondo | Texto | Hover |
|----------|-------|-------|-------|
| `primary` | Gradiente copper | White | Intensificar gradiente |
| `secondary` | Surface-pine | Parchment | Copper tint 15% |
| `ghost` | Transparente | Copper | Copper tint 10% |
| `danger` | Gradiente saddle brown | White | Intensificar |
| `success` | Gradiente sage darker | White | Intensificar |

Sizes: `xs` (24px), `sm` (32px), `md` (40px), `lg` (48px).

Props: `loading` (spinner reemplaza icono), `disabled` (opacidad 0.5), `iconLeft`, `iconRight`.

### 11.2 BaseCard (`src/components/base/BaseCard.vue`)

| Variante | Uso |
|----------|-----|
| `default` | Glass dark — card estándar de dashboard |
| `elevated` | Más opaco + shadow — modales, cards destacadas |
| `outlined` | Transparente + borde — secciones agrupadas |
| `ghost` | Transparente + sin borde — contenedores de layout |

**Regla:** Usar `BaseCard` en vez de glassmorphism inline. Si necesitas glass personalizado, copia el patrón de `BaseCard` — no reescribes el glass desde cero en cada componente.

### 11.3 BaseSkeleton (`src/components/base/BaseSkeleton.vue`)

Variantes: `text`, `circle`, `rectangle`, `card`. Shimmer animation con gradiente cobre.

### 11.4 SkeletonLoader (`src/components/ui/SkeletonLoader.vue`)

Wrapper con layouts predefinidos: `card`, `form`, `stats`, `list`, `custom`.

---

## 12. Inspiración

**Revolut · Stripe · Apple Card** — minimalismo operativo donde cada elemento justifica su existencia.

Referentes visuales para la sensación *Terrestrial Treasury*:
- Banca privada suiza: tipografía refinada, silencio visual, jerarquía clara
- Diseño editorial oscuro: grids asimétricos pero funcionales, whitespace generoso
- Artesanía de cobre: calidez en el acento, imperfección controlada, peso material

**Lo que PYMEQ no debe parecer:**
- Fintech genérico (púrpura + blanco + Inter)
- Crypto dashboard (neón + dark mode extremo + animaciones constantes)
- Banco tradicional (azul + blanco + serif institucional)

PYMEQ es el escritorio de un sastre financiero — herramientas precisas, madera oscura, un detalle de cobre que no necesita anunciarse.

---

## Apéndice: Archivos de implementación

| Archivo | Contenido |
|---------|-----------|
| `src/css/quasar.variables.scss` | Tokens Sass (colores, spacing, radius, shadows, transitions, z-index) |
| `src/css/app.scss` | Clases utilitarias, glassmorphism, animaciones, scrollbar, focus ring |
| `src/components/base/BaseButton.vue` | Botón con 5 variantes, 4 tamaños, loading |
| `src/components/base/BaseCard.vue` | Card con 4 variantes |
| `src/components/base/BaseSkeleton.vue` | Skeleton con 4 variantes |
| `src/components/ui/SkeletonLoader.vue` | Wrapper de skeleton con layouts predefinidos |
