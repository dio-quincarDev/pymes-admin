# Core Service Route

Documenta la ruta del gateway hacia el microservicio core.

---

## Configuracion

```yaml
- id: core-service
  uri: http://${CORE_SERVICE_HOST:localhost}:8082
  predicates:
    - Path=/api/v1/core/**
  filters:
    - PreserveHostHeader
    - AuthenticationFilter
```

| Variable | Valor en Docker | Default local |
|----------|----------------|---------------|
| `CORE_SERVICE_HOST` | `pymes-core-service` | `localhost` |

---

## Orden de rutas

La ruta core esta definida **antes** que la ruta generica auth (`Path=/api/v1/**`). Spring Cloud Gateway evalua en orden, por lo que `/api/v1/core/**` matchea primero y no es atrapada por auth.

```
1. core-service    -> /api/v1/core/**  -> core:8082   (primero)
2. auth-service    -> /api/v1/**       -> auth:8081   (solo si no es core)
```

---

## Flujo de autenticacion

```
Cliente -> Gateway -> AuthenticationFilter -> Core
                          |
                    JWT validation
                    Redis blacklist check
                    Identity header injection
```

El `AuthenticationFilter` valida el JWT y, si es valido, inyecta estos headers al core:

| Header | Origen |
|--------|--------|
| `X-User-Id` | `claims.userId` |
| `X-User-Email` | `claims.sub` |
| `X-Tenant-Id` | `claims.tenantId` |
| `X-User-Role` | `claims.role` |

> Los microservicios internos deben rechazar trafico que no provenga del Gateway o que intente suplantar estos headers desde el exterior.

Referencia: [Contrato de Identity Headers](./strategies/IDENTITY_HEADERS.md)

---

## Endpoints Core (44)

### Configuracion (3)

```
GET    /api/v1/core/setup/{tenantId}
POST   /api/v1/core/setup/{tenantId}/onboarding
GET    /api/v1/core/setup/preview/{industry}
```

### Productos (8)

```
POST   /api/v1/core/productos
GET    /api/v1/core/productos
GET    /api/v1/core/productos/{id}
PUT    /api/v1/core/productos/{id}
DELETE /api/v1/core/productos/{id}
POST   /api/v1/core/productos/{id}/presentaciones
GET    /api/v1/core/productos/{id}/presentaciones
DELETE /api/v1/core/presentaciones/{presentacionId}
```

### Proveedores (5)

```
POST   /api/v1/core/proveedores
GET    /api/v1/core/proveedores
GET    /api/v1/core/proveedores/{id}
PUT    /api/v1/core/proveedores/{id}
DELETE /api/v1/core/proveedores/{id}
```

### Facturas (5)

```
POST   /api/v1/core/facturas
GET    /api/v1/core/facturas
GET    /api/v1/core/facturas/{id}
DELETE /api/v1/core/facturas/{id}
POST   /api/v1/core/facturas/{id}/pagar
```

### Gastos Operativos (5)

```
POST   /api/v1/core/gastos
GET    /api/v1/core/gastos
GET    /api/v1/core/gastos/{id}
PUT    /api/v1/core/gastos/{id}
DELETE /api/v1/core/gastos/{id}
```

### Prestamos (7)

```
POST   /api/v1/core/prestamos
GET    /api/v1/core/prestamos
GET    /api/v1/core/prestamos/{id}
PUT    /api/v1/core/prestamos/{id}
DELETE /api/v1/core/prestamos/{id}
POST   /api/v1/core/prestamos/{id}/pagos
GET    /api/v1/core/prestamos/{id}/pagos
```

### Patrimonio (2)

```
GET    /api/v1/core/patrimonio/{tenantId}
PUT    /api/v1/core/patrimonio/{tenantId}
```

### Ventas (5)

```
POST   /api/v1/core/ventas
GET    /api/v1/core/ventas
GET    /api/v1/core/ventas/{id}
PUT    /api/v1/core/ventas/{id}
DELETE /api/v1/core/ventas/{id}
```

### Accounting (2)

```
GET    /api/v1/core/accounting/consultar?tenantId={uuid}&periodo=YYYY-MM
POST   /api/v1/core/accounting/recalcular?tenantId={uuid}&periodo=YYYY-MM
```

### Analytics (2)

```
GET    /api/v1/core/analytics?tenantId={uuid}&periodo=YYYY-MM
POST   /api/v1/core/analytics/recalcular?tenantId={uuid}&periodo=YYYY-MM
```

---

## Como probar

```bash
# 1. Login
TOKEN=$(curl -s http://localhost:8080/api/v1/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"pass"}' \
  | jq -r '.data.accessToken')

# 2. Core setup (lazy init)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/core/setup/{tenantId}

# 3. Onboarding
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"industry":"RESTAURANT"}' \
  http://localhost:8080/api/v1/core/setup/{tenantId}/onboarding

# 4. Crear factura
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"...","providerId":"...","invoiceNumber":"F-001","issueDate":"2026-07-09","type":"FACTURA","items":[...]}' \
  http://localhost:8080/api/v1/core/facturas
```

---

## Health check

```bash
curl -s http://localhost:8080/actuator/health | jq .status
# -> "UP"
```
