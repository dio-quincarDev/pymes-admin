# Estrategia de Autenticación Frontend — PYMEQ

Integración del frontend Quasar con el API Gateway (8080) y el Auth Service (8081). Estado actual.

---

## Flujo General

```
Usuario → Frontend (9200) → API Gateway (8080) → Auth Service (8081)
```

- `accessToken`: en memoria (Pinia). Vida corta.
- `refreshToken`: en `localStorage`. Enviado al endpoint de refresh cuando `accessToken` expira.
- El Gateway valida el `accessToken` en el edge; el Auth Service no necesita hacerlo.

---

## Contratos de API

### Registro (`POST /api/v1/auth/register`)

```json
// Request
{
  "nombre": "Nombre Apellido",
  "email": "usuario@ejemplo.com",
  "password": "SecurePass123!",
  "companyName": "Mi Empresa Pyme",
  "companySlug": "mi-empresa-pyme"   // generado automáticamente, invisible para el usuario
}

// Response 201
{
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "def456...",
    "user": { "id": "uuid", "email": "...", "nombre": "..." }
  },
  "codigo": "SUCCESS"
}
```

### Login local (`POST /api/v1/auth/login`)

Request: `{ email, password }` — Response idéntica al registro.

### Refresh (`POST /api/v1/auth/refresh`)

Request: `{ refreshToken }` — Response con nuevos tokens.

### Logout (`POST /api/v1/auth/logout`)

Requiere `Authorization: Bearer <accessToken>`. Backend añade el token a la blacklist Redis.

---

## Flujo OAuth2 — Google

```
1. Usuario click "Entrar con Google"
2. Frontend redirige a: GET /oauth2/authorization/google (via Gateway :8080)
3. Google autentica al usuario
4. Auth Service recibe callback → emite código de un solo uso
5. Frontend (AuthCallback.vue) recibe: /#/auth/callback?code=xxx
6. Frontend: POST /api/v1/auth/exchange { code } → recibe { accessToken, refreshToken }
7. Limpia ?code= de la URL (replaceState) → redirige al Dashboard
```

> JWT nunca aparece en la URL. El `?code=` es de un solo uso y expira en segundos.

**Facebook:** descartado en esta fase.

---

## Rutas Públicas (no requieren token)

| Ruta | Descripción |
|------|-------------|
| `/api/v1/auth/register` | Registro |
| `/api/v1/auth/login` | Login local |
| `/api/v1/auth/refresh` | Refresh de tokens |
| `/api/v1/auth/verify-email` | Verificación de email |
| `/api/v1/auth/resend-verification` | Reenvío de verificación |
| `/api/v1/auth/forgot-password` | Solicitar reset |
| `/api/v1/auth/reset-password` | Aplicar reset |
| `/oauth2/**` | Inicio OAuth2 |
| `/api/v1/auth/exchange` | Code exchange OAuth2 |

---

## Implementación en Frontend

### `src/boot/axios.ts`
- Inyecta `Authorization: Bearer <token>` en cada request desde el store Pinia.
- Interceptor 401: intenta refresh automático; si falla, ejecuta logout y redirige a `/login`.

### Auth Store (Pinia — `src/modules/auth/store/index.ts`)

```typescript
// Estado
user: User | null
accessToken: string | null   // en memoria
pendingTenant: { name, slug } | null
isAuthenticated: boolean

// Acciones
login(email, password)
register(data)
verifyEmail(token, email)
handleOAuthCallback(token, refreshToken)
fetchCurrentUser()
logout()
setPendingTenant(name, slug)
setSession(token, refreshToken, user)
clearSession()
```

### Navigation Guards (`src/router/index.ts`)

Rutas del dashboard tienen meta `requiresAuth: true`. Si no hay token en el store → redirect a `/login`.

### `src/composables/useLogout.ts`

Lógica de logout extraída del store para reuso en navbar, menú de usuario, etc. Llama `POST /logout`, limpia store, redirige.

---

## Flujo "Empresa Primero"

1. **Home (`IndexPage`):** Único punto de entrada. El usuario ingresa el nombre de la empresa. El slug se genera automáticamente (remoción de acentos + slugify invisible).
2. **Registro (`RegisterPage`):** "Paso Final" — datos del administrador. El nombre de empresa viene del estado global (`pendingTenant`). Sin campo manual de slug.
3. **OAuth2 Intent:** Si el usuario elige Google desde el registro, el `intentId` en el state parameter sincroniza la empresa creada con la identidad OAuth2 del backend.

> Regla: no se puede llegar a `RegisterPage` sin empresa definida. El guard lo bloquea y redirige a Home.

---

## Decisiones Históricas

| Fecha | Decisión |
|-------|----------|
| 2026-06-24 | `verifyEmail` auto-redirect a `/onboarding` si tenant no completó setup — store mergea `activeTenant.id` en user |
| 2026-04-13 | Hash routing elegido (`/#/ruta`) — documentado en AGENTS.md |
| 2026-05-03 | LoginPage extraída del nested layout (conflicto de dos `<q-layout>`) |
| 2026-04-28 | `verifyEmail()` incluye email junto al token (fix token-email mismatch) |
| 2026-06-19 | OAuth2 migrado a code exchange — JWT eliminado de URLs |
| 2026-06-19 | `replaceState` usando `hash.replace(/\?.*$/, '')` para hash routing |
