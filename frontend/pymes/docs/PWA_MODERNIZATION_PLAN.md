# 🚀 Plan de Modernización UX/UI - PWA PYMEQ

## Resumen

Implementación de la Full Modernization del frontend según el roadmap definido en `docs/FRONTEND_STATUS.md` y el design system en `docs/UI_Design_System_Fintech.md`.

## Arquitectura

```
frontend/pymes/
├── src/
│   ├── css/
│   │   ├── quasar.variables.scss    # Tokens: spacing, shadows, radii, transitions
│   │   └── app.scss                 # Classes: glass, brand-glow, fade-in-up, skeleton
│   ├── components/
│   │   ├── base/
│   │   │   ├── BaseCard.vue         # Card con glassmorphism
│   │   │   ├── BaseButton.vue       # Botón con estados completos
│   │   │   └── BaseSkeleton.vue      # Skeleton loader con shimmer
│   │   └── ui/
│   │       └── SkeletonLoader.vue   # Wrapper reusable para estados de carga
│   ├── App.vue                      # Router transitions
│   └── pages/                       # Integración pendiente
├── src-pwa/
│   └── manifest.json                # Branding PYMEQ (actualizado)
└── quasar.config.ts                # Fonts agregados
```

---

## Cambios Realizados

### Fase 1 - Fundamentos CSS ✅

**1. `src/css/quasar.variables.scss`** - Agregados tokens:
- Spacing system 8px: `$space-4xs` a `$space-3xl`
- Border radius: `$radius-xs` a `$radius-full`
- Shadows: `$shadow-subtle` a `$shadow-lg`, `$shadow-brand`
- Transitions: `$transition-fast`, `$transition-base`, `$transition-smooth`
- Z-index scale: `$z-base` a `$z-tooltip`

**2. `src/css/app.scss`** - Agregadas clases/utilidades:
- `.glass`, `.glass-light` - Glassmorphism
- `.brand-glow` - Efecto de brillo cobre
- `.fade-in-up` - Animación de entrada
- `.stagger-children` - Animación escalonada
- `.skeleton` - Loader base con shimmer
- `.hover-lift`, `.hover-scale` - Efectos hover
- Transiciones de router: `.fade-enter-active`, etc.

**3. `src-pwa/manifest.json`** - Actualizado:
- `name`: "PYMEQ - Auditoría Inteligente"
- `theme_color`: `#A3785E` (Brand Copper)
- `background_color`: `#0B1210` (Forest Deep)
- Agregados `shortcuts` para Dashboard y Login
- Agregado `scope`, `lang`, `categories`

**4. `quasar.config.ts`** - Actualizado:
- Agregadas fuentes: `roboto-font-latin-ext`, `fontawesome-v6`

### Fase 2 - Componentes Base ✅

**5. `src/components/base/BaseCard.vue`** - Creado
- Props: `variant` (default/elevated/outlined/ghost), `padding`
- Glassmorphism inline (sin mixin para evitar error de sass)

**6. `src/components/base/BaseButton.vue`** - Creado
- Props: `variant` (primary/secondary/ghost/danger/success), `size` (xs/sm/md/lg), `loading`, `disabled`, `iconLeft`, `iconRight`
- Estados completos: hover, active (scale 0.96), disabled, loading
- Colores fijos (sin color.adjust de sass)

**7. `src/components/base/BaseSkeleton.vue`** - Creado
- Props: `variant` (text/circle/rectangle/card), `size` (xs/sm/md/lg/xl), `width`, `height`
- Shimmer animation con gradiente cobre

**8. `src/components/ui/SkeletonLoader.vue`** - Creado
- Props: `isLoading`, `layout` (card/form/stats/list/custom), `count`, `loadingId`
- Templates predefinidos para cada layout

**9. `src/App.vue`** - Modificado
- `<Transition name="fade" mode="out-in">` envolviendo router-view
- Scroll suave al cambiar ruta

---

## SOLUCIÓN DE BLOQUEOS: Sass & Docker ✅ (08 de Mayo, 2026)

### 1. Sass Compatibility & Name Collision
- **Problema:** `$map: 12px is not a map` causado por colisión de nombres entre tokens locales (`$space-xs`) y funciones internas de Quasar.
- **Solución:** Prefijado de todos los tokens locales con `pq-` (ej. `$pq-space-xs`) en `quasar.variables.scss` y `app.scss`.
- **Configuración:** Forzado de API `legacy` en `quasar.config.ts` con cast de TypeScript para compatibilidad con Vite 7.

### 2. Estabilización de Versiones
- **Problema:** Vite 8 y Quasar 2.19 introdujeron bugs en la generación de Service Workers para PWA.
- **Solución:** Reversión y fijación exacta (pinning) de dependencias a **Vite 7** y **Quasar 2.18**.
- **Sass:** Fijado a `sass: 1.32.12` exacto.

### 3. Docker Sync
- **Problema:** `npm ci` fallaba por discrepancias de versiones en el lockfile.
- **Solución:** Actualización del `Dockerfile` para usar `npm install --legacy-peer-deps`, permitiendo la instalación con versiones de Sass compatibles aunque no sean las preferidas por Vite.

---

## Estado de Testing

| Herramienta | Estado |
|-------------|--------|
| `npm run lint` | ✅ Sin errores |
| `npx vue-tsc --noEmit` | ✅ Sin errores |
| `quasar build -m pwa` | ✅ Éxito (100%) |
| `docker compose build`| ✅ Éxito (100%) |

---

## Pendiente (Fase 3)

- [x] Integración de componentes base en Auth Pages
- [x] Skeletons en LoginPage, RegisterPage, VerifyPage
- [ ] Modernización de IndexPage (Bento Grid Style)
- [ ] Verificación de glassmorphism en móvil

---

*Última actualización: 08 de Mayo, 2026 14:50*
*Estado: ACTIVO - Blockers resueltos*