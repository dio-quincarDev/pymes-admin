# 🔐 Diagnóstico Completo

Confirmado. Tengo todo lo necesario. El stack está listo para la solución: **Redis** ya tiene dos templates operativos, y el `OAuth2AuthenticationSuccessHandler` ya es *custom*.  
✅ El costo de implementación es **bajo**.

---

## 🧠 Estrategia: Pre-Auth Intent via `state` parameter

### 📌 Por qué funciona

El parámetro `state` de OAuth2 es el mecanismo **estándar del protocolo** para transportar contexto a través del redirect.  
Spring Security lo propaga intacto hasta `onAuthenticationSuccess()` vía `request.getParameter("state")`.

---

## 🔁 Flujo completo

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
|                  |    - lee state = "abc-123"
|                  |    - consulta Redis → obtiene { name, slug }
|                  |    - crea Tenant + UserTenant
|                  |    - genera JWT con tenantId
|                  |    - borra key de Redis
|                  |
|                  | 4. redirect /auth/callback?token=...&refresh_token=...
|                  | ──────────────────────────────────────────────────────►
|                  |                                            AuthCallback.vue
|                  |                                            (JWT ya contiene tenantId)
+------------------+
```

---

## 🧩 Cambios por capa

### 🔧 Backend — 3 modificaciones


| Qué              | Dónde                               | Detalle                                                             |
| ----------------- | ------------------------------------ | ------------------------------------------------------------------- |
| Nuevo endpoint    | `AuthController`                     | `POST /auth/oauth2/intent` → guarda en Redis, retorna `intentId`   |
| Nuevo service     | `OAuth2IntentService`                | CRUD del intent en Redis con`StringRedisTemplate`                   |
| Modificar handler | `OAuth2AuthenticationSuccessHandler` | Lee`state`, resuelve intent, crea `Tenant` + `UserTenant` si existe |

---

### 🎨 Frontend — 2 modificaciones


| Qué                            | Dónde                | Detalle                                                                       |
| ------------------------------- | --------------------- | ----------------------------------------------------------------------------- |
| Llamar intent antes de redirect | `AuthOptionsPage.vue` | `POST intent` → recibir `intentId` → construir URL con `&state=intentId`    |
| Simplificar callback            | `AuthCallback.vue`    | Eliminar la lógica compensatoria del`pendingTenant` — el JWT ya trae tenant |

---

## ⚠️ Caso edge — usuario OAuth2 que ya existe (re‑login)

El `state` puede llegar vacío o el intent ya no existe en Redis.
El handler debe manejar esto **gracefully**: si no hay intent, continuar con el flujo actual (buscar tenants existentes del usuario).

---
