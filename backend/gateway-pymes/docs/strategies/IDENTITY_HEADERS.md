# Contrato de Propagación de Identidad (Identity Headers)

Define cómo el Gateway comunica la identidad del usuario a los microservicios internos tras validación exitosa del JWT.

## Headers Obligatorios (Gateway → Microservicio)

| Header | Tipo | Descripción |
|--------|------|-------------|
| `X-User-Id` | `Long` | ID del usuario autenticado |
| `X-User-Email` | `String` | Email del usuario (Subject del JWT) |
| `X-Tenant-Id` | `Long` | ID del tenant activo |
| `X-User-Role` | `String` | Rol jerárquico (OWNER, ADMIN, etc.) |

## Cómo el Gateway los inyecta

`AuthenticationFilter` extrae los claims del JWT validado y los agrega como headers a la request downstream. Si un claim viene nulo en el JWT, el header se envía vacío (null-safe desde review C3 de 2026-06-16).

```java
// AuthenticationFilter.java
mutate.header("X-User-Id", userId)
      .header("X-User-Email", email)
      .header("X-Tenant-Id", tenantId)
      .header("X-User-Role", role);
```

## Cómo consumirlos en microservicios internos

```java
// Spring MVC
@RequestHeader("X-User-Id") Long userId,
@RequestHeader("X-Tenant-Id") Long tenantId
```

### Casos de uso

| Caso | Header |
|------|--------|
| Filtrar datos por empresa | `X-Tenant-Id` |
| Registrar auditoría | `X-User-Id` |
| Validar permisos por rol | `X-User-Role` |

## Seguridad

- Solo el Gateway está expuesto externamente.
- El Gateway **limpia** estos headers si vienen en la request original del cliente (evita spoofing).
- Los microservicios internos deben rechazar tráfico que no provenga de la red del Gateway (`pymes-internal-network`).

> **Regla:** Nunca confiar en `X-User-*` headers que lleguen desde fuera del Gateway.
