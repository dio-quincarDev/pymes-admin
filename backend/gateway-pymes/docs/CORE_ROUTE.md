# Core Service Route

Documenta la ruta del gateway hacia el microservicio core.

---

## Configuración

```yaml
- id: core-service
  uri: http://${CORE_SERVICE_HOST:localhost}:8082
  predicates:
    - Path=/api/v1/core/**
  filters:
    - PreserveHostHeader
    - AuthenticationFilter
```

## Variable de entorno

| Variable | Valor en Docker | Default local |
|----------|----------------|---------------|
| `CORE_SERVICE_HOST` | `pymes-core-service` | `localhost` |

## Orden de rutas

La ruta core está definida **antes** que la ruta genérica auth (`Path=/api/v1/**`). Spring Cloud Gateway evalúa en orden, por lo que `/api/v1/core/**` matchea primero y no es atrapada por auth.

```
1. core-service    → /api/v1/core/**  → core:8082   ✅
2. auth-service    → /api/v1/**       → auth:8081   (solo si no es core)
```

## Flujo de autenticación

```
Cliente → Gateway → AuthenticationFilter → Core
                          |
                    JWT validation
                    Redis blacklist check
                    Identity header injection
```

El `AuthenticationFilter` valida el JWT y, si es válido, inyecta estos headers al core:

| Header | Origen |
|--------|--------|
| `X-User-Id` | `claims.userId` |
| `X-User-Email` | `claims.sub` |
| `X-Tenant-Id` | `claims.tenantId` |
| `X-User-Role` | `claims.role` |

Referencia: [Contrato de Identity Headers](./strategies/IDENTITY_HEADERS.md)

## Cómo probar

```bash
# 1. Login
TOKEN=$(curl -s http://localhost:8080/api/v1/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"pass"}' \
  | jq -r '.data.accessToken')

# 2. Core setup endpoint (lazy init)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/core/setup/{tenantId}

# 3. Onboarding
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"industry":"RESTAURANT"}' \
  http://localhost:8080/api/v1/core/setup/{tenantId}/onboarding
```

## Health check

```bash
curl -s http://localhost:8080/actuator/health | jq .status
# → "UP"
```
