# Redis — Referencia Tecnica

## Arquitectura

Instancia unica Redis 7-alpine (`pymes-redis-auth`), standalone, sin cluster ni sentinel. Persistencia RDB via volumen Docker `pymes-redis-data:/data`. Sin puertos expuestos al host — solo accesible en `pymes-internal-network`.

## Servicios y Uso

| Servicio | Tipo de Acceso | Proposito |
|----------|---------------|-----------|
| **Auth** (8081) | `RedisTemplate` / `StringRedisTemplate` | Blacklist JWT, cache de permisos, verificacion email, reset password, rate limiting (Lua), OAuth2 intent y exchange code |
| **Core** (8082) | Spring `@Cacheable` + `StringRedisTemplate` | Cache de entidades (prestamos, ventas, gastos, productos, proveedores, facturas, patrimonio) con TTL 5min global; dirty flags para recomputo de metricas/analytics |
| **Gateway** (8080, WebFlux) | `ReactiveRedisTemplate` | Solo lectura de blacklist (`hasKey`) — auth escribe, gateway lee |

Auth y core dependen de Redis via `depends_on: condition: service_started` (no `service_healthy`).

## Conexion

| Propiedad | Valor |
|-----------|-------|
| **Host** | `${REDIS_HOST}` (Docker: `pymes-redis-auth`, dev: `localhost`) |
| **Puerto** | `${REDIS_PORT:6379}` |
| **Database index** | No configurado — todo en `db 0` |
| **Password** | No configurado |
| **Timeout** | No configurado — default Lettuce (~60s) |
| **Cliente** | Lettuce (Spring Boot default), sin pool |

## Key Naming

Formato consistente `namespace:subdominio:identificador`:

| Namespace | Patron | Uso |
|-----------|--------|-----|
| `auth:token_blacklist:` | `auth:token_blacklist:{jwt}` | Blacklist (auth escribe, gateway lee) |
| `auth:permissions:` | `auth:permissions:{userId}:{tenantId}` | Cache de permisos |
| `email:verify:` | `email:verify:{token}` | Verificacion email |
| `temp-register:` | `temp-register:{token}` + `temp-register:email:{email}` | Registro temporal |
| `password:reset:` | `password:reset:{token}` | Reset de contrasena |
| `rate_limit:` | `rate_limit:{key}` | Rate limiting |
| `oauth2_intent:` | `oauth2_intent:{uuid}` | Intent OAuth2 |
| `oauth:code:` | `oauth:code:{uuid}` | Codigo exchange OAuth2 |
| `recompute:` | `recompute:metrics:{tenantId}:{period}` | Dirty flags de recomputo |
| Cache names | `prestamos`, `ventas`, `gastos`, etc. | Spring cache abstraction |

## TTL / Expiracion

| Funcionalidad | TTL | Medio |
|---------------|-----|-------|
| Blacklist JWT | Dinamico (lo que resta del token) | `TokenBlacklistService` |
| Cache permisos | 5 min | `PermissionCacheService` |
| Email verification | 15 min | `EmailVerificationServiceImpl` |
| Registro temporal | 15 min | `EmailVerificationServiceImpl` |
| Reset password | 15 min | `PasswordResetServiceImpl` |
| Rate limiting | 15 min | `RateLimitService` (Lua INCR+EXPIRE) |
| OAuth2 intent | 10 min | `OAuth2IntentServiceImpl` |
| OAuth2 exchange code | 2 min | `OAuth2AuthenticationSuccessHandler` |
| Caches core entity | 5 min global | `RedisCacheConfiguration.defaultCacheConfig()` |
| Recompute debounce | 1 h | `RecomputeDebounceService` |

Toda escritura en Redis tiene TTL. No hay datos sin expiracion.

## Estructuras de Datos

Solo **Strings** en todas las funcionalidades. Sin Hashes, Sets, Sorted Sets ni Lists. Valores JSON serializados via `GenericJackson2JsonRedisSerializer` en ambos servicios.

## Serializacion

- **Keys:** `StringRedisSerializer` (UTF-8 plano)
- **Values:** `GenericJackson2JsonRedisSerializer` (JSON con `@class` type info)
- **Cache core:** misma serializacion via `RedisCacheConfiguration`
- **Auth** usa `RedisConfig.java` con template personalizado.
- **Core** usa `CacheConfig.java` con `@EnableCaching` + `RedisCacheManager`.

## Cache Manager

Solo core define `CacheManager`:

```java
RedisCacheConfiguration.defaultCacheConfig()
    .entryTtl(Duration.ofMinutes(5))
    .disableCachingNullValues()
    .serializeValuesWith(...)
```

Auth no usa Spring cache abstraction — todo via `RedisTemplate` directo.

## Observaciones Criticas

1. **Sin password Redis** — aceptable en red interna Docker, cualquier contenedor en la misma red puede conectarse sin autenticar. No se agrega porque la red interna solo contiene servicios propios (ponytail: riesgo aceptado, agregar si un servicio externo comparte la red).
2. **Sin SSL/TLS** — trafico Redis en texto plano en red interna Docker. Aceptable (ponytail: agregar solo si Redis cruza limites de red).
3. **Sin aislamiento de database index** — auth, core y gateway comparten `db 0`. Namespace prefix (`auth:*`, `core:*`) evita colisiones (ponytail: agregar database index solo si ocurre colisión).
4. **Sin connection pooling Lettuce** — canal unico por defecto Lettuce. Aceptable para PYME (ponytail: agregar pool solo si aparece contención medida).
5. **Sin cluster/sentinel** — Redis es punto unico de fallo. Si cae: auth pierde blacklist, rate limiting, email y reset; core pierde todas las caches y recomputo; gateway no puede validar blacklist. Aceptado para escala PYME (ponytail: YAGNI hasta que downtime de Redis sea inaceptable).
6. **`maxmemory` / `maxmemory-policy`** — Configurado via `redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru` en `docker-compose.yml`. Previene OOM en escrituras. `allkeys-lru` elegido sobre `volatile-lru` como fallback seguro si alguna key no tiene TTL.
7. **`KEYS` en `RecomputeDebounceService`** — `stringRedisTemplate.keys(*)` es O(N) y bloquea Redis. Aceptable para escala PYME (<10k keys), pero migrar a `SCAN` si crece la carga. El codigo ya tiene comentario `ponytail` reconociendo el limite.
