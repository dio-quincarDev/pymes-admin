# 🔐 Estrategia de Autenticación Frontend: Pymeq

Este documento detalla la integración del frontend Quasar con el API Gateway y el microservicio de Auth, incluyendo los contratos de datos y el flujo de OAuth2.

---

## 🏗️ Arquitectura de Identidad

Pymeq utiliza un modelo de **JWT (JSON Web Tokens)** con rotación de tokens y validación en el Edge (Gateway).

- **API Gateway (8080):** Único punto de entrada. Valida el `accessToken`.
- **Auth Service (8081):** Emite tokens y gestiona la persistencia de usuarios y tenants.
- **Frontend (9000):** Almacena el `accessToken` en memoria (Pinia) y el `refreshToken` en `localStorage` (o Cookie segura).

---

## 📑 Contratos de API (Payloads)

### 1. Registro de Usuario (`POST /api/v1/auth/register`)
**Request:**
```json
{
  "nombre": "Nombre Apellido",
  "email": "usuario@ejemplo.com",
  "password": "SecurePass123!",
  "companyName": "Mi Empresa Pyme",
  "companySlug": "mi-empresa-pyme"
}
```
**Response (201 Created):**
```json
{
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "def456...",
    "user": {
      "id": "uuid",
      "email": "usuario@ejemplo.com",
      "nombre": "Nombre Apellido"
    }
  },
  "codigo": "SUCCESS",
  "mensaje": "Usuario registrado exitosamente"
}
```

### 2. Login Local (`POST /api/v1/auth/login`)
**Request:**
```json
{
  "email": "usuario@ejemplo.com",
  "password": "SecurePass123!"
}
```
**Response (200 OK):** Identica al registro.

### 3. Refresco de Token (`POST /api/v1/auth/refresh`)
**Request:**
```json
{
  "refreshToken": "def456..."
}
```

---

## 🌐 Flujo OAuth2 (Google / Facebook)

Pymeq delega la autenticación social al Backend para mayor seguridad.

### Workflow:
1. **Inicio:** El usuario hace clic en "Entrar con Google".
2. **Redirección:** El frontend redirige a:
   `http://localhost:8080/api/v1/oauth2/authorization/google`
3. **Interacción:** El usuario se autentica en Google/FB.
4. **Callback Backend:** Google devuelve el control al Backend (Auth Service).
5. **Finalización:** El Backend redirige al Frontend con los tokens en la URL:
   `http://localhost:9000/#/auth/callback?token=xxx&refresh_token=yyy`
6. **Procesamiento:** La página `/auth/callback` extrae los tokens, los guarda en el Store y redirige al Dashboard.

---

## 🛠️ Implementación en Frontend (Pymeq Store)

### 1. Interceptor de Axios (`src/boot/axios.ts`)
- Inyectar `Authorization: Bearer <token>` en cada request.
- Detectar error `401` para disparar el flujo de refresco o logout.

### 2. Auth Store (Pinia)
**Estado:**
- `user`: Datos del usuario actual.
- `accessToken`: Token de corta duración para peticiones.
- `isAuthenticated`: Booleano de estado.

**Acciones:**
- `login(email, password)`
- `handleOAuthCallback(params)`
- `logout()`: Llama a `/api/v1/auth/logout` y limpia el storage.

### 3. Navigation Guards (`src/router/index.ts`)
- **Meta `requiresAuth`:** Todas las rutas del dashboard deben tener esta marca.
- **Lógica:** Si la ruta requiere auth y el store no tiene token, redirigir a `/login`.

---

## 🛡️ Rutas Públicas (White List en Gateway)
Estas rutas NO requieren token y son accesibles por el frontend:
- `/api/v1/auth/login`
- `/api/v1/auth/register`
- `/api/v1/auth/verify-email`
- `/api/v1/auth/forgot-password`
- `/api/v1/oauth2/**`

---
*Última actualización: 13 de Abril, 2026 - Pymeq Strategy Team*
