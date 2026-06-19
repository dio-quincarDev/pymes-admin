# Daily Reports — Frontend PYMEQ

Registro cronológico de decisiones, problemas resueltos y estado del frontend.

---

## 2026-06-19 — Seguridad OAuth2 y replaceState

### OAuth2 Code Exchange

**Problema:** `AuthCallback.vue` recibía JWT directamente en la URL (`?token=xxx&refresh_token=yyy`), exponiéndolos en el historial del navegador, URL bar y header Referer.

**Solución:** El backend ahora emite un código de un solo uso (`?code=xxx`). El frontend lo canjea via `POST /api/v1/auth/exchange`. JWT nunca toca la URL.

**Impacto en UX:** Ninguno — flujo transparente para el usuario.

---

### replaceState para limpiar tokens en hash routing

Problema: Tres páginas recibían tokens sensibles por query param dentro del hash. En hash routing, `window.location.hash` incluye los params (`#/ruta?param=valor`), así que `window.history.replaceState` estándar no los borraba.

Fix: `hash.replace(/\?.*$/, '')` recorta el query string manteniendo el path del hash.

| Página | Tokens en URL | replaceState |
|--------|--------------|--------------|
| `AuthCallback.vue` | `?code=...` | ✅ limpiado |
| `VerifyEmailPage.vue` | `?token=...&email=...` | ✅ limpiado |
| `ResetPasswordPage.vue` | `?token=...&email=...` | ✅ limpiado |
| `AcceptInvitationPage.vue` | `?token=...` | ❌ intencional — necesita token post-login redirect |

---

## 2026-06-16 — SEO y Accesibilidad (WCAG 2.1 AA)

Implementados en una sola sesión de trabajo. Todos los ítems completados:

**Viewport / HTML base:**
- `user-scalable=no` eliminado (requisito WCAG + penalización Google)
- `<html lang="es">`, `theme-color: #0B1210`

**Landmarks:**
- `<main>` en 3 layouts; `<nav aria-label>` en sidebar y landing; `<footer>` en AuthLayout

**ARIA:**
- `aria-label` en hamburger, avatar dropdown, landing input
- `aria-hidden` en BrandSplash y BaseSkeleton
- `aria-busy` en BaseButton loading y SkeletonLoader
- `aria-live` announcer para mensajes dinámicos
- `role="group"` en BaseCard; `aria-disabled` en footer buttons

**Teclado:**
- Password toggles: `role="button"`, `tabindex="0"`, `@keydown.enter/space`
- Login envuelto en `<q-form>` con `type="submit"`
- Focus management: `<main>` recibe foco después de cada cambio de ruta

**Contraste:**
- `$accent` elevado de `#71837F` (4.2:1, falla AA) a `#8A9E99` (5.5:1, pasa AA)

**SEO:**
- `useMeta()` en todas las páginas con `titleTemplate`
- Open Graph: `og:title`, `og:description`, `og:type`

---

## 2026-06-16 — Estado del Diseño Consolidado (PYMEQ)

**Identidad:** SaaS Fintech "PYMEQ". Flujo "Empresa Primero" — registro comienza con nombre de empresa, slug generado automáticamente.

**Layout:** Todas las páginas de auth bajo `AuthLayout.vue`. Dashboard split en `DashboardStats` + `DashboardActionCard` + `RecentActivity`. Landing split en `LandingHero` + `FeatureGrid` + `TrustSection`.

**Scaffolding eliminado:** `EssentialLink.vue`, `ExampleComponent.vue`, `models.ts`, `example-store.ts`.

**Composables creados:** `useAuthForm`, `useLogout`, `useScrollReveal`.

**Build:** 385.71 KB JS, 32 chunks, 0 errores lint.

---

## 2026-05-08 — Bloqueadores Sass / Vite / Docker

### Sass: Colisión de nombres con tokens de Quasar

**Problema:** `$map: 12px is not a map` — colisión entre tokens locales (`$space-xs`) y funciones internas de Quasar.

**Solución:** Prefijado de todos los tokens con `pq-` (`$pq-space-xs`, etc.) en `quasar.variables.scss` y `app.scss`.

### Pinning de versiones

**Problema:** Vite 8 + Quasar 2.19 → bugs en generación de Service Workers PWA.

**Solución:** Fijado exacto en `package.json`:
- **Vite:** `7.x`
- **Quasar:** `2.18`
- **Sass:** `1.32.12`

Sass legacy API forzada en `quasar.config.ts` con cast TypeScript para compatibilidad con Vite 7.

### Docker

**Problema:** `npm ci` fallaba por discrepancias de versiones en el lockfile tras el pinning.

**Solución:** `Dockerfile` usa `npm install --legacy-peer-deps`.

---

## 2026-05-04 — Flujo de Auth Unificado

**Company First:** Home es el único punto de entrada. Slug generado automáticamente, invisible para el usuario.

**OAuth2 Intent:** Login con Google respeta la empresa creada en el paso previo (state parameter `intentId` sincronizado con el backend).

**Recordar sesión:** Persistencia de email en `localStorage` en login local.

**Eliminado:** Facebook OAuth — descartado de esta fase, solo Google.

---

## 2026-04-28 — Fix de Seguridad: token-email mismatch en verify-email

**Vulnerabilidad:** `authService.verifyEmail()` enviaba solo `{ token }` ignorando el email del query param. Cualquier token válido podía verificar cualquier cuenta.

**Fix:**
```typescript
// auth.service.ts — antes (vulnerable)
async verifyEmail(token: string) {
  return api.post('/auth/verify-email', { token });
}

// auth.service.ts — después (corregido)
async verifyEmail(token: string, email: string) {
  return api.post('/auth/verify-email', { token, email });
}
```

`VerifyEmailPage.vue` actualizada para extraer email del query param y enviarlo junto al token.

Estado: ✅ RESUELTO

---

## 2026-05-03 — Fix: Login no navegaba (Layout anidado)

**Problema:** `LoginPage.vue` estaba en `children` de `LandingLayout`. Ambos usaban `<q-layout view="lHh Lpr lFf">` → layouts anidados en conflicto.

**Solución:** Extraer `/login` y rutas de auth del nested children, hacerlas rutas independientes bajo `AuthLayout`.

Estado: ✅ RESUELTO

---

## 2026-05-03 — Componentes Base y Design System

Creados en una sesión:

| Componente | Props clave |
|------------|-------------|
| `BaseButton.vue` | `variant` (primary/secondary/ghost/danger/success), `size`, `loading`, `disabled`, `iconLeft/Right` |
| `BaseCard.vue` | `variant` (default/elevated/outlined/ghost), `padding` |
| `BaseSkeleton.vue` | `variant` (text/circle/rectangle/card), `size`, `width`, `height` |
| `SkeletonLoader.vue` | `isLoading`, `layout` (card/form/stats/list), `count` |

CSS tokens en `quasar.variables.scss` (prefijo `pq-`): spacing 8px system, border radius, shadows, transitions, z-index.

Clases en `app.scss`: `.glass`, `.glass-light`, `.brand-glow`, `.fade-in-up`, `.stagger-children`, `.skeleton`, `.hover-lift`, `.hover-scale`, router transitions.

PWA manifest actualizado: nombre "PYMEQ - Auditoría Inteligente", `theme_color: #A3785E`, `background_color: #0B1210`, shortcuts a Dashboard y Login.

---

## Próximos Pasos

### Testing (Fase 1)
- ✅ Vitest configurado (`vitest`, `@vue/test-utils`, `happy-dom`)
- ✅ Tests de utilidades: 7 tests en `errors.spec.ts`
- ⬜ Tests de composables: `useAuthForm`, `useLogout`, `useScrollReveal`
- ⬜ Tests de componentes
- ⬜ Tests de integración

### Performance (Fase 3)
- ✅ Lazy loading en todas las rutas
- ✅ Code splitting automático por Vite (32 JS chunks)
- ✅ PWA: Workbox `InjectManifest` configurado
- ⬜ Image optimization (lazy loading nativo, WebP)

### Pendientes conocidos
- `BrandSplash.vue` creado, pendiente de integrar en flujo de carga
- `EmptyState.vue` creado, pendiente de usar en vistas vacías
- Migrar `refreshToken` de `localStorage` a cookie `HttpOnly` en producción
- Input sanitization con DOMPurify cuando haya UGC
- Sentry cuando haya usuarios reales

---

*Creado: 2026-06-19 | Consolidación de FRONTEND_STATUS.md + AUTH_SERVICE_PLAN.md + auth-frontend-strategy-update.md + VERIFICATION_SECURITY_FIX.md + PWA_MODERNIZATION_PLAN.md*
