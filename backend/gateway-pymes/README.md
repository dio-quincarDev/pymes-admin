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

## ⚙️ Configuración & Perfiles

El Gateway utiliza **Perfiles de Maven** para gestionar la conectividad y el nivel de logging por entorno.

| Perfil | Propósito | Comando |
|--------|-----------|---------|
| **`dev`** | Desarrollo local (localhost, logging DEBUG). | `./mvnw spring-boot:run -Pdev` |
| **`stg`** | Staging (Red interna, logging INFO). | `./mvnw package -Pstg` |
| **`prod`** | Producción (Seguridad máxima, logging WARN). | `./mvnw package -Pprod` |

### 🔐 Variables de Entorno Críticas

| Variable | Descripción | Valor recomendado |
|----------|-------------|-------------------|
| `JWT_SECRET` | **Mismo secreto que el microservicio Auth**. | (Inyectar vía Secret) |
| `REDIS_HOST` | Host para validación de blacklist (Logout). | `localhost` (dev) / `pymes-redis-auth` (docker) |
| `AUTH_SERVICE_HOST`| Dirección interna del servicio de autenticación. | `pymes-auth-service` |
| `CORS_ALLOWED_ORIGINS`| Orígenes permitidos (CORS). | `http://localhost:5173` (dev) |

---

## 🚀 Optimización de Recursos

*   **Runtime Reactivo**: Construido sobre Netty y WebFlux, permitiendo manejar alta concurrencia con un Heap Size de solo **384MB**.
*   **Redis Reactive**: El uso de `ReactiveRedisTemplate` evita el bloqueo de hilos durante la validación de la blacklist.
*   **Aislamiento de Red**: Solo el Gateway tiene puertos expuestos al exterior (vía Nginx Proxy Manager). La comunicación interna ocurre en la red `pymes-internal-network`.

---

<div align="center">

**PyMes Admin - API Gateway** | Seguridad de Alto Rendimiento 🔒

</div>
