# Strategy: Pre-Auth Intent via OAuth2 `state` parameter

This document describes the design and implementation for linking JIT-provisioned Google OAuth2 users with their desired Tenant properties during registration.

---

## 🧠 Core Strategy

The standard OAuth2 `state` parameter is used to propagate the registration intent context across the Google redirect loop. Spring Security forwards the parameter to `onAuthenticationSuccess()` via `request.getParameter("state")`.

---

## 🔁 Flow Diagram

```text
+------------------+     1. POST /auth/oauth2/intent       +--------+
| AuthOptionsPage  | ─────────────────────────────────────► | Redis  |
|                  |    { name, slug }                      |        |
|                  | ◄───────────────────────────────────── |        |
|                  |      { intentId: "abc-123" }           +--------+
|                  |
|                  | 2. redirect /oauth2/authorization/google?state=abc-123
|                  | ──────────────────────────────────────────────────────►
|                  |                                                      Google
|                  | ◄──────────────────────────────────────────────────────
|                  |                   callback
|                  |
|                  | 3. OAuth2AuthenticationSuccessHandler:
|                  |    - reads state = "abc-123"
|                  |    - queries Redis → gets { name, slug }
|                  |    - creates Tenant + UserTenant (Owner)
|                  |    - generates JWT containing tenantId
|                  |    - deletes key from Redis
|                  |
|                  | 4. redirect /auth/callback?token=...&refresh_token=...
|                  | ──────────────────────────────────────────────────────►
|                  |                                            AuthCallback.vue
|                  |                                            (JWT now contains tenantId)
+------------------+
```

---

## 🧩 Layer Components

### Backend Implementation
- **Intent Endpoint**: `AuthController` exposes `POST /auth/oauth2/intent` to save the tenant details in Redis and return a unique `intentId`.
- **Redis intent Service**: `OAuth2IntentService` handles key-value lifecycle in Redis.
- **Success Handler**: `OAuth2AuthenticationSuccessHandler` reads the `state` parameter on success, resolves the registration intent, and builds the `Tenant` + `UserTenant` structure before generating JWT tokens.

### Frontend Integration
- **Options Page**: `AuthOptionsPage.vue` saves intent details via POST prior to starting the Google redirect loop, constructing the redirect URI with `&state=intentId`.
- **Callback**: `AuthCallback.vue` consumes the issued JWT, which already contains the correctly linked `tenantId`.

---

## ⚠️ Edge Cases
- **Missing or Expired Intent**: If the `state` parameter is missing or the Redis key has expired, the handler falls back gracefully to check for any existing tenants associated with the authenticated Google email.
