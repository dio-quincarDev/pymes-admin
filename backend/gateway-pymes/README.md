# PyMes Admin - API Gateway 🚪

> **Spring Cloud Gateway** - Punto de entrada único, validador de seguridad en el "Edge" y gestor de identidad.

---

## 🏗️ Arquitectura de Seguridad (Edge Validation)

El Gateway actúa como el **guardia de seguridad principal** de la plataforma, realizando validaciones costosas antes de que la petición toque los microservicios de negocio.

### Flujo de Autenticación (`AuthenticationFilter`)

1.  **Extracción**: Captura el `Authorization: Bearer <token>` de la cabecera.
2.  **Validación Criptográfica**: Verifica la firma y expiración del JWT localmente (usando `JWT_SECRET`).
3.  **Verificación en Redis (Blacklist)**: Consulta de forma reactiva a Redis para asegurar que el token no haya sido revocado (Logout).
    *   Key: `auth:token_blacklist:<token>`
4.  **Inyección de Identidad (Header Propagation)**: Si es válido, inyecta los siguientes headers en la petición hacia el microservicio destino:
    *   `X-User-Id`: ID único del usuario.
    *   `X-User-Email`: Email del usuario.
    *   `X-Tenant-Id`: ID del tenant (empresa) activo.
    *   `X-User-Role`: Rol jerárquico del usuario.

---

## 🌐 Configuración de Rutas

| Tipo | Prefijo de Ruta | Seguridad | Destino |
|------|-----------------|-----------|---------|
| **Públicas** | `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/oauth2/**` | Ninguna | `auth-service` |
| **Protegidas** | `/api/v1/auth/logout`, `/api/v1/auth/me`, `/api/v1/tenants/**`, `/api/v1/invitations/**` | **JWT + Redis Blacklist** | `auth-service` |

---

## ⚙️ Variables de Entorno Clave

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `REDIS_HOST` | Host de la instancia compartida de Redis | `pymes-redis-auth` |
| `JWT_SECRET` | Clave secreta para firmar/validar tokens | (Ver `application.yaml`) |
| `AUTH_SERVICE_HOST` | Hostname interno del microservicio de Auth | `pymes-auth-service` |

---

## 🚀 Optimización de Recursos

*   **Runtime Reactivo**: Construido sobre Netty y WebFlux, permitiendo manejar alta concurrencia con un Heap Size de solo **384MB**.
*   **Redis Reactive**: El uso de `ReactiveRedisTemplate` evita el bloqueo de hilos durante la validación de la blacklist.
*   **Aislamiento de Red**: Solo el Gateway tiene puertos expuestos al exterior (vía Nginx Proxy Manager). La comunicación interna ocurre en la red `pymes-internal-network`.

---

<div align="center">

**PyMes Admin - API Gateway** | Seguridad de Alto Rendimiento 🔒

</div>
