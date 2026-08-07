# Email Verification Flow — Cross-Tab Behavior

## Contexto

Los clientes de correo (Gmail, Outlook, etc.) **siempre** abren links en una nueva pestaña. Esto es comportamiento del navegador, no del código. No existe forma de prevenir esto desde el frontend.

## Flujo actual

### Backend (`EmailVerificationServiceImpl.java`)

1. **Registro**: El usuario completa el formulario → `POST /auth/register` guarda los datos en Redis (`temp-register:{token}`) con TTL de 15 minutos → envía email con link `/#/verify?token=...&email=...`

2. **Verificación**: El usuario hace click en el email → nueva pestaña se abre en `/#/verify?token=...&email=...` → `POST /auth/verify-email` retorna `AuthResponse` con `accessToken`, `refreshToken`, `user`, `activeTenant`

3. **Login automático**: El backend retorna tokens y el frontend ejecuta `setSession()` que escribe en localStorage y Pinia store

### Frontend

| Componente | Qué hace |
|-----------|----------|
| `VerifyEmailPage.vue` | Recibe `token` y `email` de query params → llama `authStore.verifyEmail()` → check `setupService.get(tenantId)` → auto-navega a `/onboarding` o `/dashboard` |
| `App.vue` | Listener de `storage` event: cuando detecta `pymeq_email_verified=true` en otra pestaña, sincroniza el store y navega a `/dashboard` |
| `axios.ts` | Interceptor de 401: limpia localStorage + dispatcha `auth:401` CustomEvent |
| `auth store` | Listener de `auth:401`: limpia `accessToken` y `user` del store |

### Flujo cross-tab

```
Pestaña Original (registro)          Pestaña Nueva (email link)
────────────────────────────         ──────────────────────────────
1. POST /auth/register              1. GET /#/verify?token=...&email=...
2. Muestra "Revisa tu correo"       2. POST /auth/verify-email
3. ...esperando...                   3. setSession() → localStorage + Pinia
4. storage event detecta            4. setupService.get() check
   pymeq_email_verified             5. router.push('/onboarding') o
5. Sincroniza store                    router.push('/dashboard')
6. router.push('/dashboard')
```

## Archivos modificados

### `frontend/pymes/src/modules/auth/pages/VerifyEmailPage.vue`

**Cambios:**
- Removido `window.close()` y `setTimeout` que mataban la pestaña antes del redirect
- Agregado `setupService.get(tenantId)` check después de verificar: si `onboardingCompleted === false`, navega a `/onboarding`
- `goToDashboard()` también checkea onboarding antes de navegar
- Restaurado `pymeq_email_verified` flag en localStorage para el storage event de App.vue

**Líneas clave:**
- `:92` — import de `setupService`
- `:114` — `localStorage.setItem('pymeq_email_verified', 'true')`
- `:124-136` — check de onboarding después de verificar
- `:188-200` — `goToDashboard()` con check de onboarding

### `frontend/pymes/src/App.vue`

**Cambios:**
- Agregado `storage` event listener que detecta verificación cross-tab
- Sincroniza Pinia store desde localStorage cuando la otra pestaña escribe tokens
- Navega a `/dashboard` automáticamente
- Removido `setupService` import (ya no se necesita aquí)

**Líneas clave:**
- `:27-42` — `onStorage` handler

### `frontend/pymes/src/modules/auth/store/index.ts`

**Cambios:**
- Agregado listener de `auth:401` CustomEvent para sincronizar store cuando axios interceptor limpia localStorage

**Líneas clave:**
- `:152-158` — `window.addEventListener('auth:401', ...)`

### `frontend/pymes/src/boot/axios.ts`

**Cambios:**
- Interceptor de 401 ahora dispatcha `auth:401` CustomEvent además de limpiar localStorage

**Líneas clave:**
- `:47-53` — limpieza de localStorage + `window.dispatchEvent(new CustomEvent('auth:401'))`

### `frontend/pymes/src/modules/core/pages/OnboardingPage.vue`

**Cambios:**
- Removido `fetchCurrentUser()` después de `completeOnboarding()` — causaba 401 cascade que limpiaba la sesión

**Líneas clave:**
- `:86-98` — `confirm()` sin `fetchCurrentUser()`

## Problemas pendientes

| Problema | Causa | Posible solución |
|----------|-------|-----------------|
| 2 pestañas abiertas después de verificar | Email clients abren links en nueva pestaña | Aceptar (estándar PWA) o usar `window.open()` en registro para controlar la pestaña |
| No se puede cerrar la pestaña original | `window.close()` solo funciona con `window.open()` | No hay solución frontend |
| `storage` event no funciona en misma pestaña | Especificación del navegador | Por diseño — el storage event es para cross-tab |

## Decisiones técnicas

1. **No se puede prevenir 2 pestañas** — es comportamiento del navegador/email client
2. **La pestaña nueva es la "buena"** — hace verify → auto-login → onboarding → dashboard
3. **La pestaña original se sincroniza** via storage event → navega a `/dashboard`
4. **El usuario cierra manualmente** la pestaña que no necesita (estándar para apps con links externos)
5. **`auth:401` CustomEvent** evita dependencia circular entre axios interceptor y auth store
