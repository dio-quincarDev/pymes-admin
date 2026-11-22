# API Reference — contratos generados

Los archivos `openapi-auth.json` y `openapi-core.json` se **generan desde el código**
(SpringDoc), nunca se escriben a mano. `Source:` `AuthApiController`, `XxxController` y los DTOs
record de cada servicio.

## Regenerar (un comando por servicio)

Con el stack levantado (`docker compose up -d postgres redis auth-service core-service`):

```bash
# Auth (8081, no expuesto al host → se genera desde dentro de la red Docker)
docker exec pymes-auth-service wget -qO- http://localhost:8081/v3/api-docs/auth \
  > docs/api/openapi-auth.json

# Core (8082, ídem)
docker exec pymes-core-service wget -qO- http://localhost:8082/v3/api-docs/core \
  > docs/api/openapi-core.json
```

> El gateway **solo agrega el spec de Auth** (`/v3/api-docs/auth`). `/v3/api-docs/core`
> da 404 desde el gateway: falta la ruta `swagger-api-docs-core` y el entry en
> `springdoc.swagger-ui.urls` (`backend/gateway-pymes/src/main/resources/application.yaml`).
> Mientras tanto, Core se captura directo desde su contenedor.

## Validar

```bash
npx -y @redocly/cli@latest lint docs/api/openapi-auth.json docs/api/openapi-core.json
```

## Estado de la validación (2026-11-22)

| Ítem | Auth | Core |
|------|------|------|
| OpenAPI | 3.1.0 | 3.1.0 |
| Paths | 23 | 34 |
| Schemas | 43 | 38 |
| Refs rotos | 0 | 0 |
| `securitySchemes` | `bearerAuth` | none (el core confía en los headers del gateway) |
| Errores `struct` del generador | 2 (`name`/`in` dentro de `bearerAuth`, inválido en 3.1) | 0 |
| Warnings | `server.url: localhost`, 25 endpoints sin 4xx | `server.url: localhost`, ~34 endpoints sin 4xx |

Los errores `struct` y los warnings **vienen de SpringDoc**, no del contrato real. No se
parchean a mano: requieren anotaciones en el código (ver regla 7 de la skill de documentación).

## Contrato de autenticación (verificado en código)

- Gateway valida JWT (firma + expiración + blacklist Redis) e inyecta
  `X-User-Id`, `X-User-Email`, `X-Tenant-Id`, `X-User-Role`, `X-User-Plan`.
- Auth: `Authorization: Bearer <access_token>` (`securitySchemes.bearerAuth`).
- Core: sin esquema de seguridad propio — confía en los headers internos del gateway.
  `TenantValidationFilter` devuelve 403 si `X-Tenant-Id` difiere de `?tenantId=`.
