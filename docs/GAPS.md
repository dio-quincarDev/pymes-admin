# GAPS — Discrepancias entre Documentación y Realidad

## Auth Service

| Gap | Documentación dice | Realidad es | Impacto | Estado |
|-----|--------------------|-------------|---------|--------|
| — | *(sin gaps tras correcciones de esta sesión)* | — | — | ✅ |
| — | V1 sin composite/partial indexes para consultas frecuentes | V2 agregó índices en user_tenants, refresh_tokens, invitations, audit_log | Medio | ✅ |

## Core Service

| Gap | Documentación dice | Realidad es | Impacto |
|-----|--------------------|-------------|---------|
| — | *(sin gaps)* | — | — |

## Gateway

| Gap | Documentación dice | Realidad es | Impacto |
|-----|--------------------|-------------|---------|
| — | *(sin gaps)* | — | — |

## Frontend

| Gap | Documentación dice | Realidad es | Impacto |
|-----|--------------------|-------------|---------|
| — | *(sin gaps tras correcciones de esta sesión)* | — | — |

## PostgreSQL

| # | Gap | Documentación dice | Realidad es | Impacto |
|---|-----|--------------------|-------------|---------|
| 1 | Sin SSL | `sslmode=require` ausente en todos los perfiles | Conexión texto plano entre servicios | Medio — aceptado (ponytail: red interna Docker) |
| 2 | Sin FK entre esquemas | `tenant_id` en core no tiene FK a `auth.tenants` | Validación solo en capa de aplicación | Medio — aceptado (ponytail: validación app-layer suficiente para PYME) |
| 3 | Mismas credenciales DB | auth y core compuran usuario BD | Servicios pueden leer/escribir schema del otro | Medio — aceptado (ponytail: schema isolation + red interna) |
| 4 | Sin límite HikariCP | Pool default 10 sin ajuste | Consulta lenta puede agotar pool | Bajo — configurado `maximum-pool-size: 20` en auth + core | ✅ |
| 5 | SQL injection en tests (4 archivos) | `jdbcTemplate.execute()` con string concat | Tests de integración | Medio — corregido en esta sesión | ✅ |
| 6 | Auth: índices faltantes | V1 sin composite/partial indexes | 6 consultas frecuentes sin cobertura óptima | Bajo — corregido con V2 migration | ✅ |

## Redis

| # | Gap | Documentación dice | Realidad es | Impacto |
|---|-----|--------------------|-------------|---------|
| 1 | Sin password | No configurado en compose ni app | Cualquier contenedor en la red interna puede conectarse | Medio — aceptado (ponytail: red interna solo con servicios propios) |
| 2 | Sin database isolation | Todo en `db 0` | auth, core y gateway comparten el mismo db index | Bajo — aceptado (ponytail: namespace prefix evita colisiones) |
| 3 | Sin `maxmemory` / `maxmemory-policy` | No configurado | Redis default `noeviction` — escrituras fallan si se llena RAM | Medio — configurado `--maxmemory 256mb --maxmemory-policy allkeys-lru` | ✅ |
| 4 | `KEYS` en RecomputeDebounceService | `stringRedisTemplate.keys(*)` es O(N) | Bloquea Redis en datasets grandes | Bajo — aceptado (ponytail: <10k keys, comentario en código) |
| 5 | Sin connection pooling Lettuce | Sin pool configurado | Canal único por defecto puede ser cuello de botella bajo carga | Bajo — aceptado (ponytail: agregar si hay contención) |

## Docker

| # | Gap | Documentación dice | Realidad es | Impacto |
|---|-----|--------------------|-------------|---------|
| 1 | `JAVA_OPTS` sin configurar | INFRA_STRATEGY.md recomienda `-Xmx384m` | docker-compose.yml no tenía límites JVM | Alto — configurado `-Xmx384m -XX:+UseG1GC` en gateway, auth, core | ✅ |
| 2 | Logging sin límites | INFRA_STRATEGY.md recomienda `max-size: 10m` | docker-compose.yml no tiene bloque logging | Bajo — aceptado (ponytail: logs de PYME no llenan disco rápido) |
| 3 | Healthchecks más frecuentes que lo recomendado | INFRA_STRATEGY.md sugiere `interval: 30s` | Gateway 10s, Auth/Core 15s | Bajo — aceptado (ponytail: 15s no causa picos medibles) |
| 4 | Core service sin build en docker-build-check | CI.yml solo construye auth, gateway, frontend | CI no verifica que core construya imagen Docker | Bajo — aceptado (ponytail: gap de CI, no de compose) |

## GitHub Actions

| # | Gap | Documentación dice | Realidad es | Impacto |
|---|-----|--------------------|-------------|---------|
| 1 | Core service ausente de CI | CI.yml testea auth, gateway, frontend | No había jobs para core | **Crítico** — corregido: job unit tests + docker build check | ✅ |
| 2 | Core service ausente de CD staging | cd-staging.yml build & test auth + gateway | No corría tests de core ni construía imagen | **Crítico** — corregido: build+test + docker push | ✅ |
| 3 | Core service ausente de CD production | cd-prod.yml misma estructura | No corría tests de core ni construía imagen | **Crítico** — corregido: build+test + docker push | ✅ |
| 4 | CI no ejecuta integration tests de core | Solo auth tiene integration tests en CI | Core tiene tests no ejecutados | Medio — aceptado (ponytail: requieren Docker-in-Docker) |
