# Sistema de Diseño PYMEQ — Fintech Minimalist

Referencia de tokens visuales y reglas de componentes.

---

## Paleta: Deep Forest & Copper

| Token | Hex | Aplicación |
|-------|-----|------------|
| `forest-deep` | `#0B1210` | Fondo base (60%) |
| `surface-pine` | `#1B2624` | Tarjetas, sidebar (elevación sutil) |
| `parchment` | `#E2E8E4` | Texto principal, valores numéricos (30%) |
| `brand-copper` | `#A3785E` | CTAs, mesh gradients, highlights (10%) |
| `sage-muted` | `#8A9E99` | Texto secundario, captions — ≥5.5:1 WCAG AA |

> `sage-muted` fue elevado de `#71837F` (4.2:1, falla AA) a `#8A9E99` (5.5:1) el 2026-06-16.

---

## Tipografía

- **Display / Headings:** Outfit (Google Fonts), weight 700–800
- **Body:** Source Sans 3, base 14px para sensación compacta y profesional
- **Labels:** 12px, `text-weight-bold`

---

## Tokens CSS (`quasar.variables.scss`)

Prefijo `pq-` para evitar colisión con funciones internas de Quasar.

| Grupo | Variables |
|-------|-----------|
| Spacing (8px system) | `$pq-space-4xs` … `$pq-space-3xl` |
| Border radius | `$pq-radius-xs` … `$pq-radius-full` |
| Shadows | `$pq-shadow-subtle`, `$pq-shadow-md`, `$pq-shadow-lg`, `$pq-shadow-brand` |
| Transitions | `$pq-transition-fast`, `$pq-transition-base`, `$pq-transition-smooth` |
| Z-index | `$pq-z-base` … `$pq-z-tooltip` |

---

## Clases Utilitarias (`app.scss`)

| Clase | Efecto |
|-------|--------|
| `.glass` | Glassmorphism: `rgba(27,38,36,0.7)` + `backdrop-filter: blur(12px)` |
| `.glass-light` | Variante más translúcida |
| `.brand-glow` | Box-shadow con color cobre al hover |
| `.fade-in-up` | Entrada suave: 20px → 0 en 0.4s |
| `.stagger-children` | Animación escalonada en hijos directos |
| `.skeleton` | Shimmer con gradiente cobre |
| `.hover-lift` | `translateY(-2px)` en hover |
| `.hover-scale` | `scale(1.02)` en hover |

> Todas las animaciones se desactivan con `@media (prefers-reduced-motion: reduce)`.

---

## Componentes Base

### BaseButton
Variantes: `primary`, `secondary`, `ghost`, `danger`, `success`  
Sizes: `xs`, `sm`, `md`, `lg`  
Props: `loading`, `disabled`, `iconLeft`, `iconRight`  
Estados: hover (gradiente), active (scale 0.96), loading (spinner), disabled (opacity)

### BaseCard
Variantes: `default`, `elevated`, `outlined`, `ghost`  
Glassmorphism inline (sin mixin — requisito de Sass legacy API).

### BaseSkeleton
Variantes: `text`, `circle`, `rectangle`, `card`  
Shimmer animation con gradiente cobre.

### SkeletonLoader
Wrapper para estados de carga. Layouts predefinidos: `card`, `form`, `stats`, `list`, `custom`.

---

## Glassmorphism

```scss
background: rgba(27, 38, 36, 0.7);
backdrop-filter: blur(12px);
border: 1px solid rgba(138, 158, 153, 0.1);  // sage-muted al 10%
box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
```

---

## Reglas de Degradados

- Mesh gradients: **solo en texto** (no en fondos ni bordes).
- Ángulo: 135°, variaciones de `brand-copper`.
- Degradados lineales en botones: variación ±10% del color base.

---

## Inspiración

Revolut · Stripe · Apple Card — minimalismo operativo, cada elemento justifica su existencia.
