# PyMes Admin - API Gateway

Spring Cloud Gateway (WebFlux). Punto de entrada unico, validador JWT en el edge, enrutador a microservicios internos.

[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0-6DB33F?logo=spring)](https://spring.io/projects/spring-cloud-gateway)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=ffffff)](https://www.oracle.com/java/technologies/downloads/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=ffffff)](https://redis.io/)

---

## Que Hace

Guardia de seguridad de la plataforma. Todas las peticiones externas pasan por este servicio antes de llegar a auth-service o core-service. Realiza validaciones costosas (firma JWT, blacklist Redis) en el edge para que los microservicios internos no tengan que hacerlo.

Responsabilidades:
- Validacion JWT (firma + expiracion + blacklist Redis)
- Inyeccion de identidad via headers downstream
- Enrutamiento a microservicios internos
- Whitelist de rutas publicas
- CORS global
- Swagger UI agregado para todos los servicios

---

## Como Funciona

### Flujo de Autenticacion (AuthenticationFilter)

```
Request externo
  -> AuthenticationFilter
    1. RouterValidator.isSecured(path)?
       - Si es publico: skip auth, continua al downstream
       - Si es protegido: continua validacion
    2. Extrae Authorization: Bearer <token>
       - Sin header o vacio -> 401
       - Sin prefijo "Bearer " -> 401
    3. JwtUtils.getClaims(token)
       - Firma invalida -> 401
       - Token expirado -> 401
       - Token malformado -> 401
    4. Redis blacklist check (ReactiveRedisTemplate)
       - Token revocado -> 401
    5. Token valido -> inyecta headers en request downstream:
       X-User-Id: {userId}
       X-User-Email: {email}
       X-Tenant-Id: {tenantId}
       X-User-Role: {role}
```

### RouterValidator (Whitelist)

Rutas que no requieren autenticacion JWT:

| Ruta | Servicio |
|------|----------|
| /api/v1/auth/register | auth-service |
| /api/v1/auth/login | auth-service |
| /api/v1/auth/refresh | auth-service |
| /api/v1/auth/verify-email | auth-service |
| /api/v1/auth/resend-verification | auth-service |
| /api/v1/auth/forgot-password | auth-service |
| /api/v1/auth/reset-password | auth-service |
| /oauth2/** | auth-service |
| /login/oauth2/** | auth-service |
| /login/** | auth-service |
| /swagger-ui.html | Agregador |
| /v3/api-docs/** | Agregador |
| /actuator/** | Monitoreo |

### Identity Headers (Contrato)

Gateway -> Microservicio:

| Header | Tipo | Descripcion |
|--------|------|-------------|
| X-User-Id | Long | ID del usuario autenticado |
| X-User-Email | String | Email (subject del JWT) |
| X-Tenant-Id | Long | Tenant activo |
| X-Tenant-Role | String | Rol jerarquico |

Los microservicios internos deben rechazar trafico que no provenga del Gateway o que intente suplantar estos headers desde el exterior.

### Rutas configuradas

| Tipo | Prefijo | Seguridad | Destino |
|------|---------|-----------|---------|
| Publicas Auth | /api/v1/auth/register, /login, /refresh, /verify-email, /forgot-password, /reset-password | Ninguna | auth-service:8081 |
| Publicas OAuth2 | /oauth2/**, /login/oauth2/**, /login/** | Ninguna | auth-service:8081 |
| Protegidas | /api/v1/auth/logout, /me, /tenants/**, /invitations/** | JWT + Redis | auth-service:8081 |
| Swagger | /v3/api-docs/auth, /swagger-ui.html | Ninguna | Agregador |

### CORS

Configurado via `globalcors` en `application.yaml`:

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins: ${CORS_ALLOWED_ORIGINS:"*"}
      allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
      allowedHeaders: "*"
```

Nota: CORS en el Gateway es el punto principal. Auth-service tiene un `WebCorsConfig` de defensa en profundidad.

---

## Stack Tecnico

Spring Cloud Gateway / WebFlux / Netty / ReactiveRedisTemplate / JJWT / Lombok

### Recursos

- Heap: 384MB (reactivo, no bloquea hilos)
- Redis: ReactiveRedisTemplate (operaciones no bloqueantes)
- Red: Solo Gateway expuesto externamente (via Nginx Proxy Manager). Comunicacion interna en `pymes-internal-network`.

---

## Suite de Pruebas

### Ejecucion

```bash
./mvnw test -B
```

No requiere Docker. Todos los tests son unitarios con mocks.

### Cobertura

| Archivo | Tests | Que valida |
|---------|-------|------------|
| `AuthenticationFilterTest` | 7 | Whitelist, 401 en token faltante/invalido/expirado/revocado, inyeccion de headers |
| `RouterValidatorTest` | 21 | Rutas publicas vs protegidas (14 open + 6 secured + 1 query string) |
| `JwtUtilsTest` | 4 | JWT valido, expirado, firma invalida, malformado |
| `GatewayPymesApplicationTests` | 1 | Context carga sin errores |
| **Total** | **33** | — |

### Detalle de Tests

**AuthenticationFilterTest** (7 tests):
- `whitelistedPathSkipsAuth` - Ruta publica no ejecuta validacion JWT
- `missingAuthHeaderReturns401` - Sin header Authorization retorna 401
- `invalidBearerTokenReturns401` - Bearer con token vacio retorna 401
- `expiredTokenReturns401` - Token expirado retorna 401
- `revokedTokenReturns401` - Token valido pero en blacklist Redis retorna 401
- `validTokenWithNullClaimsSetsNullHeaders` - Claims nulos setean headers nulos
- `validTokenInjectsClaimHeaders` - Happy path: headers X-User-Id, X-User-Email, X-Tenant-Id, X-User-Role inyectados correctamente

**RouterValidatorTest** (21 cases parametrizados):
- 14 rutas publicas verificadas (auth, OAuth2, Swagger, actuator, /error)
- 6 rutas protegidas verificadas (companies, products, users/me, logout, change-password)
- 1 ruta con query string (`/verify-email?token=abc`)

**JwtUtilsTest** (4 tests):
- Token con firma correcta y expiry futuro -> subject extraido
- Token expirado -> excepcion
- Firma con secret distinta -> excepcion
- Token malformado -> excepcion

### Infraestructura

- JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- Sin Testcontainers (el gateway no tiene DB, Redis mockeado en tests)
- `application-test.yaml` con valores dummy: `JWT_SECRET`, `REDIS_HOST: localhost`, `AUTH_SERVICE_HOST: localhost`

### Archivos de Test

```
src/test/java/dev/dioquincar/gateway_pymes/
├── GatewayPymesApplicationTests.java       # Context load (1 test)
├── filter/
│   ├── AuthenticationFilterTest.java       # 7 tests
│   └── RouterValidatorTest.java            # 21 cases (parametrizados)
└── util/
    └── JwtUtilsTest.java                   # 4 tests
```

### Gaps Conocidos

- Sin integration tests. No hay tests de routing end-to-end, CORS, o actuator.
- `JwtUtils.isInvalid()` no tiene test dedicado (wrapper de 2 lineas sobre getClaims).
- `SwaggerAggregatorConfig` no tiene tests.
- Sin test para esquemas de auth no-Bearer (Basic, etc.).
- Tests usan `.block()` en vez de `StepVerifier` (funcional pero no idiomatico reactive).
- Sin test para header Authorization con valor null (key presente, value null).

---

## Swagger UI

Punto unico de acceso a documentacion de todos los microservicios via dropdown:

| URL | Descripcion |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | UI (recomendado) |
| http://localhost:8080/webjars/swagger-ui/index.html | Path real del JAR |
| http://localhost:8080/v3/api-docs/auth | JSON OpenAPI auth-service |
| http://localhost:8080/v3/api-docs/swagger-config | Config del dropdown |

En WebFlux, `/swagger-ui/index.html` no funciona. Usar siempre `/swagger-ui.html`.

### Agregar un nuevo microservicio

1. En el servicio: definir `springdoc.api-docs.path: /v3/api-docs/{nombre}`
2. En Gateway: agregar ruta en `application.yaml` y perfiles
3. En `SwaggerAggregatorConfig.java`: agregar entrada al Set de URLs

---

## Configuracion

### Perfiles

| Perfil | Proposito | Comando |
|--------|-----------|---------|
| dev | Local (localhost, DEBUG) | `./mvnw spring-boot:run -Pdev` |
| stg | Staging (red interna, INFO) | `./mvnw package -Pstg` |
| prod | Produccion (WARN, Swagger deshabilitado) | `./mvnw package -Pprod` |

### Variables de Entorno

| Variable | Descripcion | Ejemplo |
|----------|-------------|---------|
| JWT_SECRET | Misma secret que auth-service (min 256 bits) | (inyectar via Secret) |
| REDIS_HOST | Host Redis para blacklist | localhost (dev) / pymes-redis-auth (docker) |
| AUTH_SERVICE_HOST | Host interno auth-service | localhost (dev) / pymes-auth-service (docker) |
| CORS_ALLOWED_ORIGINS | Origenes permitidos | http://localhost:9200 (dev) |
| SERVER_PORT | Puerto | 8080 |

---

## Known Issues

**CORS (Spring Cloud Gateway 3.2.0+)**: Bug conocido donde OPTIONS (preflight) funciona pero POST retorna 403 "Invalid CORS request". El procesador interno de CORS de Spring intercepta antes de que `globalcors` procese. Pendiente resolucion. Ver `gateway-pymes/docs/GATEWAY-DOC.md` seccion 9 para detalle de intentos.

---

## Estado

Production-Ready

[![Build](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![WebFlux](https://img.shields.io/badge/WebFlux-Reactivo-6DB33F?logo=spring)](https://docs.spring.io/spring-framework/docs/web/reactive-web/)
