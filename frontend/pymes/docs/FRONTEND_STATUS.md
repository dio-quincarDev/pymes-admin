# 📄 Estado del Frontend - Pymeq (16 de Junio, 2026)

## 🎯 Resumen de la Identidad Visual y Arquitectura
Se ha consolidado la identidad de **SaaS Fintech** bajo el nombre **PYMEQ**, centrada en un flujo de usuario simplificado y un diseño minimalista de alta gama.

### 1. Mandato "Empresa Primero" (Company First)
- **Home (`IndexPage`):** Único punto de inicio para el registro, capturando exclusivamente el nombre de la empresa. El slug se genera de forma robusta e invisible (remoción de acentos y caracteres especiales).
- **Registro Atómico:** `RegisterPage` simplificada como "Paso Final" para asignar el administrador. No hay campos de empresa en el formulario de registro; estos se heredan del estado global (`pendingTenant`).

### 2. Estructura de Interfaz Unificada (AuthLayout)
- **Centralización:** Todas las páginas de autenticación se renderizan dentro de `AuthLayout.vue`.
- **Beneficios:** Eliminación de inconsistencias visuales en "Olvide mi contraseña" y otros flujos de soporte. Branding y seguridad (AES-256) persistentes.

---

## 🛠️ Funcionalidades Implementadas (Actualizado 04 de Mayo, 2026)

### 🔐 Autenticación y Onboarding
- **Onboarding Obligatorio:** Flujo Home -> Registro bloqueado si no hay empresa definida.
- **Login Inteligente:** 
    - **Recordar mi sesión:** Persistencia de email en `localStorage`.
    - **Google OAuth2 + Intent:** Sincronización de identidad empresarial mediante el sistema de `intentId` (state parameter) del backend.
- **Support Pages:** 
    - `VerifyEmailPage`: Verificación reactiva con manejo de tokens expirados.
    - `ForgotPasswordPage`: Solicitud de recuperación integrada al diseño.
    - `ResetPasswordPage`: Cambio de contraseña maestra con validación.

### 🎨 Sistema de Diseño: Fintech Core
- **Paleta:** Forest Deep (`#0B1210`), Surface Pine (`#1B2624`), Brand Copper (`#A3785E`).
- **Interactividad:** Efectos `brand-glow`, transiciones suaves y estados de carga personalizados.

---

## ✅ Problemas Críticos Resueltos
- **Inconsistencias en UI de Soporte:** Resuelto mediante la unificación en `AuthLayout`.
- **Conflictos de Registro:** Eliminado el campo manual de slug y la posibilidad de registrarse sin empresa.
- **Build Errors:** Limpieza total de errores de ESLint (unused vars, unbound methods, explicit any).

---

## 📋 Próximos Pasos Prioritarios
1.  **Dashboard Shell:** Implementación del contenedor principal tras el login exitoso.
2.  **Multitenancy UI:** Selector de empresas para usuarios con múltiples entornos.
3.  **Audit Logs:** Primera fase de visualización de trazabilidad de seguridad.
---

*Documento que refleja la arquitectura final del módulo de identidad.*

---

## 🔧 Estado del Setup de Desarrollo - Pymeq (07 de Mayo, 2026)

### Herramientas Instaladas
| Herramienta | Ruta | Estado |
|-------------|------|--------|
| Node.js | v22.22.2 | ✅ OK |
| npm | 11.13.0 | ✅ OK |
| Quasar CLI | `/usr/local/bin/quasar` | ✅ OK |
| Gradle (global) | `/snap/bin/gradle` v8.14.4 | ✅ OK |
| Android SDK | `/home/dio/Android/Sdk` | ✅ OK |
| Emulador | Pixel_7 (API 33) | ✅ Disponible |
| Android Studio | `/snap/bin/android-studio` | ✅ Vinculado |

### Estructura del Proyecto (Corregida)
```
frontend/pymes/
├── src/              # Código fuente Vue/Quasar
├── src-pwa/          # Configuración PWA
├── src-capacitor/    
│   ├── android/      # Proyecto Android RESTRUCTURADO (Gradle OK)
│   └── capacitor.config.json
└── quasar.config.ts  # Configuración (bin.linuxAndroidStudio añadido)
```

---

## ✅ Correcciones Técnicas Realizadas
1.  **Saneamiento de Directorios:** Eliminación de `src-capacitor/android` y `android/` que carecían de Gradle Wrapper y tenían conflictos de plugins.
2.  **Re-generación Nativa:** Ejecución de `npx cap add android` para crear la estructura estándar con `gradlew`.
3.  **Sincronización de Puente:** Uso de `quasar build -m capacitor` para inyectar assets web en el entorno nativo.
4.  **Automatización de IDE:** Configuración de la ruta de Snap de Android Studio en `quasar.config.ts` para habilitar el flag `--ide`.

---

## 🔄 Comandos de Desarrollo Atualizados

### Desarrollo PWA (Navegador)
```bash
cd frontend/pymes
quasar dev -m pwa
# Puerto: 9200
# URL: http://localhost:9200/
```

### Desarrollo Android (Emulador)
```bash
# Opción 1: Quasar (falla por Gradle)
quasar dev -m capacitor -T android

# Opción 2: Android Studio (alternativa)
quasar dev -m capacitor -T android --ide

# Opción 3: Build manual
npm run build
cd src-capacitor && npx cap sync android
cd ../android && gradle assembleDebug
```

### Verificación de Emulador
```bash
# Listar emuladores
emulator -list-avds

# Iniciar emulador en segundo plano
emulator @Pixel_7 &
```

---

## 📋 Acciones Pendientes (Actualizado 16 de Junio, 2026)

1. **Reconstruir estructura Android:** Eliminar y recrear proyecto Capacitor desde cero
2. **Verificar repo Gradle:** Asegurar que `google()` y `mavenCentral()` estén accesibles
3. **Probar con Android Studio:** Usar `--ide` para verificar desde el IDE
4. **Aplicar mejoras de auditoría frontend:** Ver sección "Roadmap de Mejoras" más abajo (A, B, C, D)

---

## 📋 Roadmap de Mejoras (16 de Junio, 2026)

> Auditoría completa del frontend. Fuente: skills `quasar-skilld` (v2.19.3), `vue-best-practices`, `frontend-design`.

---

### A. Quasar Upgrade Readiness (v2.19.3)

Items necesarios para alinear con Quasar 2.19.3 — evitar deprecaciones y adoptar nuevas APIs.

| # | Item | Justificacion | Prioridad | Archivos Afectados |
|---|---|---|---|---|
| A.1 | Reemplazar `content-class`/`content-style` por `class`/`style` en QDrawer/QDialog/QMenu/QTooltip | Deprecado — en actualizaciones futuras dejara de funcionar | Alta | `MainLayout.vue` (QDrawer) |
| A.2 | Adoptar `useMeta` composable para meta tags | Reemplaza la property `meta` en componentes (deprecada) | Media | `IndexPage.vue`, `LoginPage.vue` |
| A.3 | Integrar Regle para validacion de QInput/QField | Validacion robusta y externalizada vs. validacion inline manual | Media | `LoginPage.vue`, `RegisterPage.vue`, `ForgotPasswordPage.vue`, `ResetPasswordPage.vue` |
| A.4 | Agregar Loading Bar Plugin | Progreso global Ajax sin instanciar QAjaxBar manualmente | Baja | `quasar.config.ts`, `boot/axios.ts` |
| A.5 | Auditar QImg por props deprecadas | `transition`, `basic`, `no-default-spinner` estan deprecados | Baja | Componentes con QImg |
| A.6 | Auditar QScrollArea por API cambiada | `getScrollPosition` devuelve `{top, left}`, `setScrollPosition` requiere `axis` | Baja | Componentes con QScrollArea |

---

### B. Vue Best Practices / Arquitectura

Mejoras de mantenibilidad, composables, y estructura de componentes.

| # | Item | Justificacion | Prioridad | Archivos Afectados |
|---|---|---|---|---|
| B.1 | Crear directorio `src/composables/` | Logica compartida (formularios, errores, auth) no tiene composables — todo esta en componentes | Alta | Nuevo: `composables/useAuthForm.ts`, `useAuthError.ts` |
| B.2 | Dividir `DashboardPage.vue` | 3+ secciones (stats, accion principal, actividad) — viola single-responsibility | Alta | `DashboardPage.vue` → `components/dashboard/DashboardStats.vue`, `DashboardActionCard.vue`, `RecentActivity.vue` |
| B.3 | Dividir `IndexPage.vue` | Fusiona hero, feature grid (4 cards), trust section | Alta | `IndexPage.vue` → `components/landing/LandingHero.vue`, `FeatureGrid.vue`, `TrustSection.vue` |
| B.4 | Eliminar scaffold remnants | `EssentialLink.vue`, `ExampleComponent.vue`, `models.ts`, `example-store.ts` — codigo muerto | Alta | 4 archivos a eliminar |
| B.5 | Consolidar SkeletonLoader + BaseSkeleton | Ambos tienen overlap en variantes/tipos — BaseSkeleton podria ser interno de SkeletonLoader | Media | `BaseSkeleton.vue`, `SkeletonLoader.vue` |
| B.6 | Extraer composable `useLogout()` | Logica de logout (authStore.logout + Notify + router) duplicada en MainLayout y potencialmente otros lados | Media | Nuevo: `composables/useLogout.ts` |
| B.7 | Type-safe `ref()` con genericos | Varios `ref('')` sin tipo explicito — en strict mode `ref<string>('')` es mas seguro | Baja | Paginas de auth module |
| B.8 | Eliminar delays simulados en SkeletonLoader | Delays fijos (600-800ms) crean percepcion de lentitud innecesaria — usar estado real de carga | Baja | `LoginPage.vue`, `ForgotPasswordPage.vue`, `VerifyEmailPage.vue` |

---

### C. Frontend Design / Identidad Visual

Mejoras de calidad visual, tipografia, interaccion, y experiencia.

| # | Item | Justificacion | Prioridad |
|---|---|---|---|
| C.1 | Cambiar tipografia de Inter a una mas distintiva | Inter esta clasificado como "generic AI font" — proponer pairing Satoshi (display) + HK Grotesk (body) o Instrument Sans + Source Serif 4 | Alta |
| C.2 | Agregar micro-interacciones en botones | BaseButton solo tiene `active: scale(0.97)` — agregar ripple effect, transicion de fondo en hover, estado focus mas visible | Alta |
| C.3 | Scroll-triggered animations en landing | Hero actual es estatico — agregar staggered reveal en secciones con IntersectionObserver composable | Media |
| C.4 | Refinar glassmorphism con grain texture | `.glass` usa `rgba(255,255,255,0.05)` muy sutil — agregar noise texture SVG (grain overlay) para dar textura atmosferica | Media |
| C.5 | Brand loading screen entre rutas protegidas | No hay transicion de carga — splash con logo mesh-gradient y animacion de respiracion | Media |
| C.6 | Empty states con ilustracion | Dashboard muestra datos dummy — componente `EmptyState.vue` con ilustracion mesh-gradient + copy generico | Media |
| C.7 | Refinar fondo de AuthLayout | 100vh + centered box funcional pero generico — patron geometrico sutil o gradient mesh mas dramatico | Baja |
| C.8 | Auditar jerarquia tipografica | No hay clases tipograficas semanticas (`.text-display`, `.text-title`, `.text-body`) en app.scss | Baja |

---

### D. Estructura y Arquitectura

| # | Item | Justificacion | Prioridad |
|---|---|---|---|
| D.1 | Crear `.env.example` y validar variables de entorno | No existe `.env` — `API_URL` se resuelve a `localhost` si `process.env.API_URL` es undefined | Media |
| D.2 | API service layer centralizado | `auth.module/services/` vive dentro del modulo — para escalar, crear `src/services/` base que module-specific services extiendan | Baja |
| D.3 | Confirmar decision hash routing | Hash routing correcto segun AGENTS.md, pero limita SSR futuro — documentar decision arquitectonica | Baja |

---

### Prioridad de Implementacion Sugerida

**Fase 1 — Quick wins (1-2 dias)**
- B.4 Eliminar scaffold remnants
- B.1 Crear composables
- B.6 Composable `useLogout()`
- B.7 Type-safe `ref()`

**Fase 2 — Arquitectura (3-5 dias)**
- B.2 Dividir DashboardPage
- B.3 Dividir IndexPage
- B.5 Consolidar skeletons
- D.1 Crear `.env.example`

**Fase 3 — Quasar upgrade prep (2-3 dias)**
- A.1 content-class → class/style
- A.2 useMeta composable
- A.3 Regle para validacion

**Fase 4 — Visual polish (3-5 dias)**
- C.1 Cambio tipografico
- C.2 Micro-interacciones en botones
- C.3 Scroll animations en landing
- C.4 Grain texture en glassmorphism
- C.5 Brand loading screen
- C.6 Empty states

---

## ✅ Verificaciones Exitosas (Actualizado 08 de Mayo, 2026)

- [x] **Estabilización de Dependencias:** Reversión a Vite 7 y Quasar 2.18 para evitar bugs de PWA en versiones superiores.
- [x] **Fix de Sass:** Versión fijada a `sass@1.32.12` (exacta) con prefijo `pq-` en variables personalizadas para evitar colisiones con Quasar.
- [x] **Sincronización de Lockfile:** `package-lock.json` regenerado y verificado con `npm ci` localmente.
- [x] **Modernización UI:** `BaseCard`, `BaseButton` y `SkeletonLoader` integrados en todo el flujo de Auth y Dashboard.
- [x] **Linter/TS:** Limpieza de errores en `axios.ts` y `ResetPasswordPage.vue`.

---

## ⚠️ Estado del Docker
- **Problema Detectado:** Inconsistencia persistente en `npm ci` dentro del contenedor a pesar de la sincronización local.
- **Acción:** Forzar reconstrucción limpia y verificación de caché del demonio de Docker.

*Última actualización: 16 de Junio, 2026*
  