# Sistema de Diseño PYMEQ — Swiss / Grid

> **Versión:** 3.0 — Julio 2026
> **Fuente de truth:** `.ulpi/design/DESIGN.md`

---

## 1. Identidad

Swiss / grid + Institutional Warmth. Precisión financiera, no neobank. Bronze on near-black dice "seguridad" sin un pixel azul.

**Signature:** `#C8963E` (bronze) on `#08090D` (near-black). Números en Geist Mono, tabular-nums, weight completo.

---

## 2. Paleta

60-30-10. Neutrals tinted warm (lightness axis, no chroma). WCAG AA+ verified.

| Token | Hex | Uso | Contraste |
|-------|-----|-----|-----------|
| `--pq-background` | `#08090D` | Page base | — |
| `--pq-surface` | `#12141A` | Cards, sidebar, dialogs | 16:1 |
| `--pq-elevated` | `#1E2129` | Dropdowns, modals | 13:1 |
| `--pq-border` | `#353945` | Dividers, input borders | 4.5:1 |
| `--pq-text` | `#F5F3EF` | Primary copy | 16:1 |
| `--pq-text-muted` | `#9B9790` | Labels, placeholders | 6.5:1 |
| `--pq-text-subtle` | `#6B6863` | Captions, disabled | 3.5:1 |
| `--pq-accent` | `#C8963E` | CTAs, focus, key numbers | 5.5:1 |
| `--pq-accent-hover` | `#D4A552` | Button hover | — |
| `--pq-accent-muted` | `#8B6B3A` | Badge/chip backgrounds | — |
| `--pq-success` | `#3D7A5A` | Paid, confirmed | 4.5:1 |
| `--pq-warning` | `#C8A042` | Pending, draft | 4.8:1 |
| `--pq-danger` | `#A04038` | Delete, destructive | 4.6:1 |
| `--pq-info` | `#6E8BB8` | Neutral info | 5.2:1 |

---

## 3. Tipografía

Eje de contraste: geométrica técnica (display) + humanista cálida (body) + utilitaria precisa (mono).

| Rol | Familia | Tamaño/peso | Uso |
|-----|---------|-------------|-----|
| display | **Geist** | 32/800, 24/700, 20/600 | Títulos, KPI numbers |
| heading | **Geist** | 18/600, 16/600 | Headers de sección |
| body | **Satoshi** | 14/400, 14/500 | Todo texto UI |
| body-strong | **Satoshi** | 14/700 | Énfasis |
| caption | **Satoshi** | 12/400 | Meta, timestamps |
| utility | **Geist Mono** | 13/400, 13/500 | Moneda, números, códigos |

**Fallback body:** `-apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif`
**Fallback mono:** `ui-monospace, 'SF Mono', Menlo, monospace`

---

## 4. Tokens CSS

Declarados en `app.scss` (`:root`) y `quasar.variables.scss`.

### 4.1 Spacing (base 4px)

`0, 4, 8, 12, 16, 24, 32, 40, 48, 64, 80, 96, 128` — mapeados a `$pq-space-*`.

### 4.2 Border Radius (preciso, no redondeado)

`xs: 2px, sm: 4px, md: 6px, lg: 8px, xl: 12px, full: 9999px`

### 4.3 Sombras (elevación only, no glow)

| Token | Valor |
|-------|-------|
| `--pq-shadow-subtle` | `0 1px 3px rgba(0,0,0,0.3)` |
| `--pq-shadow-md` | `0 8px 24px rgba(0,0,0,0.25)` |
| `--pq-shadow-lg` | `0 16px 48px rgba(0,0,0,0.3)` |

### 4.4 Z-Index

`base: 0, dropdown: 100, sticky: 200, fixed: 300, modalBackdrop: 400, modal: 500, popover: 600, tooltip: 700, skipLink: 800`

### 4.5 Motion

| Token | Valor | Uso |
|-------|-------|-----|
| `--pq-motion-fast` | `80ms ease` | Micro feedback |
| `--pq-motion-base` | `160ms cubic-bezier(0.4, 0, 0.2, 1)` | Transiciones estándar |
| `--pq-motion-emphasis` | `240ms cubic-bezier(0.16, 1, 0.3, 1)` | Page load reveal |

Sin bounce/elastic. Exit ≈ 75% de enter. `prefers-reduced-motion` honored.

---

## 5. Componentes

| Componente | Estilo |
|------------|--------|
| **BaseButton** | Flat bg (sin gradients), radius `md: 6px`, heights 28/34/40/46px |
| **BaseCard** | Solid `var(--pq-surface)`, `1px solid var(--pq-border)`, radius `md`, sin backdrop-blur |
| **BaseBadge** | Radius `full`, solid bg 20% alpha, text = parent color |
| **Inputs** | Filled, `1px solid var(--pq-border)`, focus `2px solid var(--pq-accent)`, radius `sm` |
| **Tables** | QTable dense, row border `var(--pq-border)`, hover `var(--pq-surface)` tint |
| **Dialogs** | QDialog + Card, fade+scale enter, fade exit, max-width `640px` |
| **Icons** | Material Icons outline, `18px` default |
| **Skeletons** | `var(--pq-surface)` → `var(--pq-elevated)` shimmer, radius `xs` |

---

## 6. Accessibility

- Focus: `outline: 2px solid var(--pq-accent); outline-offset: 2px`
- Skip link: `#main-content` en cada layout
- Touch targets: ≥44×44px mobile
- `prefers-reduced-motion`: desactiva transiciones
- ARIA: roles en tablist, dialog, menu, alert
- Live regions: `aria-live="polite"` para toasts

---

## 7. Voz

Plain, decisive, technical. Action vocabulary consistente: Create → Created, Edit → Updated, Delete → Deleted, Pay → Paid.

---

## 8. Tokens legacy eliminados

Estas clases/variables ya **no existen** en `app.scss`:

| Token eliminado | Reemplazo |
|-----------------|-----------|
| `brand-glow` | Ninguno (eliminado) |
| `mesh-text-gradient` | `var(--pq-accent)` solid |
| `glass-light` | `var(--pq-surface)` plano |
| `glass` | `var(--pq-surface)` plano |
| `bg-forest-deep` | `var(--pq-background)` |
| `bg-surface-pine` | `var(--pq-surface)` |
| `border-light` | `var(--pq-border)` |
| `$shadow-brand` | Eliminado (sin glow) |
| `$shadow-brand-hover` | Eliminado (sin glow) |

---

## 9. Archivos de implementación

| Archivo | Contenido |
|---------|-----------|
| `.ulpi/design/DESIGN.md` | Fuente de truth: paleta, tipo, escalas, signature |
| `src/css/quasar.variables.scss` | Tokens Sass (colores, spacing, radius, shadows, motion) |
| `src/css/app.scss` | CSS custom properties (`:root`), utilidades, skeleton, transitions |
| `src/design/tokens.ts` | Tokens TypeScript para uso en lógica |
| `src/components/base/BaseButton.vue` | Botón flat, 5 variantes, 4 tamaños |
| `src/components/base/BaseCard.vue` | Card sólida, 4 variantes |
| `src/components/base/BaseBadge.vue` | Badge semántico, 5 variantes |
