# GAPS — Discrepancias entre Documentación y Realidad

## Auth Service

| Gap | Documentación dice | Realidad es | Impacto | Estado |
|-----|--------------------|-------------|---------|--------|
| — | *(sin gaps tras correcciones de esta sesión)* | — | — | ✅ |
| — | V1 sin composite/partial indexes para consultas frecuentes | V2 agregó índices en user_tenants, refresh_tokens, invitations, audit_log | Medio | ✅ |

### Code Review Findings (2026-07-15)

#### 🔴 Critical — Todos resueltos

| # | Gap | Fix | Archivos afectados | Estado |
|---|-----|-----|--------------------|--------|
| 1 | **JWT secret sin validación** (`JwtServiceImpl.java:85-88`) — `Keys.hmacShaKeyFor()` acepta cualquier tamaño | `@PostConstruct init()` valida `keyBytes.length >= 32`, lanza `IllegalArgumentException` si no. Key cacheada en campo `this.key` | `JwtServiceImpl.java` | ✅ |
| 2 | **Logout traga excepciones** (`AuthServiceImpl.java:210-223`) — Si `extractUserId()` lanza, el catch traga todo | `extractUserId()` movido antes del try. Si falla, la excepción se propaga al caller. El try solo cubre `revokeToken` + `deleteByUserId` | `AuthServiceImpl.java` | ✅ |
| 3 | **Cookie OAuth2 intent sin Secure** (`OAuth2IntentCookieFilter.java:39-43`) | `cookie.setSecure(request.isSecure())` — HTTPS → Secure=true, HTTP → false | `OAuth2IntentCookieFilter.java` | ✅ |
| 4 | **Token de reseteo en URL** (`PasswordResetServiceImpl.java:64`) — En hash fragment + one-time + TTL 15min + gateway ya tiene `Referrer-Policy: strict-origin-when-cross-origin` | Sin cambio de código. El token ya está en hash fragment (`#/reset-password?token=X`). Riesgo mitigado: no se envía al server, one-time, TTL corto, referrer-policy estricto | Ninguno | ✅ Aceptado — mitigación suficiente |

#### 🟡 Suggestions

1. **N+1 en invitaciones pendientes** (`InvitationServiceImpl.java:56-59`) — Por cada invitación hace `findById` individual de tenant + user. Fix: JOIN fetch o `@EntityGraph`.

2. **`@Lazy` circular** (`EmailVerificationServiceImpl.java:41`) — `EmailVerificationServiceImpl` ↔ `AuthServiceImpl`. Fix: extraer lógica de registro a tercer service.

3. **`extractEmail()` duplicado** (`InvitationServiceImpl.java:184`, `MemberServiceImpl.java:143`) — Misma lógica en 3 servicios. Fix: utility class o leer directo de `SecurityContextHolder`.

4. **Puerto hardcodeado** (`PasswordResetServiceImpl.java:63`) — `frontendUrl.replace(":9000", ":9200")`. Fix: propiedad dedicada en config.

5. **`/exchange` sin rate limit** (`AuthApiController.java:81-83`) — Cualquiera puede brute-forcear códigos de OAuth. Fix: aplicar rate limit por IP.

6. **Rutas públicas duplicadas** (`SecurityConfig.java:51-75` vs `JwtAuthenticationFilter.java:49-59`) — Dos listas separadas pueden desincronizarse. Fix: usar `WHITE_LIST` como fuente única.

7. **CORS permisivo con credenciales** (`WebCorsConfig.java:21,24`) — `allowedOriginPatterns` permite `*` como patrón si se configura así. Fix: separar orígenes productivos y dev.

8. **JWT completo como key Redis** (`TokenBlacklistService.java:31`) — Tokens largos = keys largas. Fix: hashear el token como key.

9. **`Thread.sleep(200)` anti-enumeration** (`PasswordResetServiceImpl.java:96-100`) — Primitivo. Fix: delay constante async + respuesta genérica idéntica.

10. **Método logueado doble** (`GlobalExceptionHandler.java:200-201`) — `request.getMethod()` impreso 2 veces. Fix: corregir formato.

11. **Falta índice en refresh_tokens** (`V2__index_optimizations.sql:16-18`) — Query de reuso busca por `token_hash + revoked`. Fix: índice compuesto `(token_hash, revoked)`.

12. **companySlug sin validación de formato** (`RegisterRequest.java:28`) — Solo valida size, acepta espacios/caracteres inválidos. Fix: `@Pattern(regexp = "^[a-z0-9-]+$")`.

13. **`CODE_TTL` constante no usada** (`OAuth2AuthenticationSuccessHandler.java:63,169`) — Línea 169 hardcodea `Duration.ofMinutes(2)` en vez de usar `CODE_TTL`.

## Core Service

| # | Gap | Documentación dice | Realidad es | Impacto |
|---|-----|--------------------|-------------|---------|
| — | *(sin gaps funcionales)* | — | — | — |
| 1 | Endpoints no documentados | CORE.md/ARCHITECTURE.md listan solo CRUD clásico | `GET /search` (paginado) y `GET /{tenantId}/categories` existen | Bajo — endpoints internos, frontend los conoce | ✅ |

### Code Review Findings (2026-07-15)

#### 🔴 Critical

1. **IN clause vacía crashea si no hay productIds** (`FacturaServiceImpl.java:188`) — `updateFactura()` construye `WHERE id IN (?)` sin validar que `productIds` no esté vacío. Si `request.items()` está vacío, se ejecuta `SELECT ... WHERE id IN ()` → SQL error. Fix: validar items no vacío al inicio del método.

2. **`reverseProductStats` = N+1 UPDATEs con subqueries** (`FacturaServiceImpl.java:295-317`) — Por cada item ejecuta un UPDATE con 2 subqueries correlacionadas. Para facturas con 20+ items son 20 queries separadas. Fix: convertir a un solo UPDATE con `FROM` subquery o batching.

3. **Sin tests para `updateFactura` ni `InvoiceCalculator`** — `InvoiceCalculator.resolve()` tiene 7+ ramas condicionales (null checks, división, conversión), pero cero tests. `reverseProductStats` tampoco está testeado.

#### 🟡 Suggestions

1. **Indentación rota en `buildItem()`** (`FacturaServiceImpl.java:228`) — La declaración del método tiene 0 espacios de indentación. Fix: indentar 4 espacios.

2. **`productoName` puede ser null** (`FacturaServiceImpl.java:234`) — `productNameMap.get()` retorna null si el producto no existe en la BD. Fix: lanzar `EntityNotFoundException`.

3. **Subtotal calculado 2 veces en `InvoiceCalculator`** (`InvoiceCalculator.java:67-71, 112-113`) — Primero en step 3 para derivación y después en step 7 como `gross - discount`. El primero se descarta. Fix: unificar en un solo cálculo.

4. **`reverseProductStats` debería ser batchable** (`FacturaServiceImpl.java:295-317`) — Las subqueries por item se pueden convertir a un solo UPDATE con `DISTINCT ON`. Fix: refactorizar a query única.

5. **Sin `@PreAuthorize` en PUT** (`FacturaController.java:38`) — Endpoint update no verifica rol. Consistente con el resto del controller, pero gap transversal.

6. **`tenantId` como `@RequestParam` en PUT vs dentro del body en POST** (`FacturaApi.java:34`) — Inconsistencia en diseño de API. Fix: mover `tenantId` al body o estandarizar ambas.

7. **`reverseProductStats` ejecutado antes de save exitoso** (`FacturaServiceImpl.java:181, 339`) — Si `save()` falla después de reverseProductStats, los stats quedan revertidos pero la transacción no se completa (rollback solo si no hay commit explícito). Riesgo de inconsistencia si hay error fuera de la transacción.

8. **EXCEPTION_STRATEGY.md sin implementación** — Documentación completa pero sin código de `GlobalExceptionHandler`, `ApiResponse`, etc. Fix: implementar o marcar como deuda con fecha.

9. **Índices potencialmente duplicados** (`V16__invoice_performance_indexes.sql`) — Verificar que migraciones anteriores no hayan creado `idx_products_tenant_id` u otros índices. `IF NOT EXISTS` mitiga, pero revisar.

## Gateway

| # | Gap | Documentación dice | Realidad es | Impacto |
|---|-----|--------------------|-------------|---------|
| — | *(sin gaps tras correcciones)* | — | — | — |
| 1 | CORS bug | Spring Cloud Gateway `globalcors` con `allowed-origin-patterns` causaba 403 en POST; sin `globalcors` causaba 403 en OPTIONS preflight | Solución dual: gateway `globalcors` con `allowed-origins` (exacto, no pattern) para OPTIONS + auth service CORS (`setAllowedOrigins`) para requests reales | **Alto** — bloquea frontend en auth | ✅ RESUELTO — SCG requiere `globalcors` para OPTIONS; auth requiere CORS propio para ACAO en POST |

### Code Review Findings (2026-07-16)

#### 🔴 Critical — Todos resueltos

| # | Gap | Fix | Archivos afectados | Estado |
|---|-----|-----|--------------------|--------|
| 1 | **Gateway no verifica tipo de token** — Refresh token carece de `role`/`tenantId`/`plan` pero pasaba el filtro | Validar `claims.get("role") != null` después de `getClaims()`. Refresh tokens son rechazados con 401 antes de llegar a Redis/blacklist | `AuthenticationFilter.java:65-67` | ✅ |
| 2 | **Header `"null"` literal** — `.header(name, null)` en downstream serializaba string `"null"`. `getSubject()` nullable no verificado. `tenantId` puede ser null en access tokens válidos | Método helper `addIfNotNull()` que solo llama `.header()` si valor != null. Headers condicionales para userId, email, tenantId, role, plan | `AuthenticationFilter.java:73-82` | ✅ |
| 3 | **JWT secret sin validación** — `Keys.hmacShaKeyFor()` acepta cualquier tamaño | `@PostConstruct` valida `keyBytes.length >= 32`, lanza `IllegalArgumentException` si no | `JwtUtils.java:24-27` | ✅ |
| 4 | **Error response sin body** — Cliente recibía 401 vacío sin JSON | `onError()` escribe body JSON `{"error":"...","status":401}` con `Content-Type: application/json` usando `response.writeWith()` | `AuthenticationFilter.java:96-103` | ✅ |

#### 🟡 Suggestions

1. **Blacklist Redis con JWT completo** (`AuthenticationFilter.java:64`) — Token ~1KB como key Redis. Fix: hashear token con SHA-256.

2. **Blacklist check = RTT por request** (`AuthenticationFilter.java:64`) — Sin cache local. Fix: añadir Caffeine cache con TTL 30s.

3. **`/exchange` sin rate limit** (`RouterValidator.java:15-30`) — Ruta pública permite brute-force sobre códigos OAuth (TTL 2 min). Fix: rate limit por IP en gateway.

4. **Swagger solo agrega Auth** (`SwaggerAggregatorConfig.java:28`) — Core Service no aparece. Fix: agregar URL de core si tiene OpenAPI docs, o ruta en gateway.

5. **Property name inconsistente** (`JwtUtils.java:17` vs `.env.example:11`) — Realidad: Spring Boot relaxed binding mapea `JWT_SECRET` → `jwt.secret`. No hay inconsistencia real. Sin acción.

6. **Error antes de `flatMap` no detiene flujo** — Ya resuelto en código previo: `return onError(...)` en líneas 45, 50, 60. Sin acción.

7. **Faltan tests de integración** — Solo unitarios con mocks. Fix: tests con `WebTestClient` simulando requests HTTP reales.

8. **Default JWT secret conocido** (`.env.example:11`) — String público como default permite forjar JWTs. Fix: generar random con `openssl rand -base64 32` y poner el valor generado.

## Frontend

| # | Gap | Documentación dice | Realidad es | Impacto |
|---|-----|--------------------|-------------|---------|
| — | *(sin gaps funcionales)* | — | — | — |
| 1 | ConfiguracionPage read-only | UI muestra campos editables (categorías, unidades, ubicaciones) | No hay endpoints PUT ni diálogos de edición | Medio — usuario espera poder editar. Pendiente backend. |
| 2 | Frontend no usa búsqueda paginada | Backend tiene `GET /search` paginado desde 2026-07-12 | `ProductosPage.vue`, `FacturasPage.vue` y `CatalogDashboard.vue` siguen usando `getAll()` sin paginación | Alto — degradación en catálogos grandes (>100 productos) | Pendiente |
| 3 | Descuento en Factura es monto fijo | UI muestra `prefix="$"` — usuario espera porcentaje (5%, 10%) | Input trata descuento como monto, no como porcentaje | Medio — UX confusa, usuario no puede poner descuentos promocionales | Pendiente |
| 4 | precioUnitario no usa conversión | Presentaciones tienen `conversion` factor | `precioUnitario` se auto-llena con `lastUnitPrice` sin dividir por conversión | Alto — precio unitario no refleja costo real por unidad base | Pendiente |

### Code Review Findings (2026-07-15)

#### 🔴 Critical

| # | File | Line | Issue | Severity |
|---|------|------|-------|----------|
| 1 | `prestamo.service.ts`, `producto.service.ts`, `factura.service.ts` | 24, 26, 21 | **`tenantId` interpolado directo en URL string** — `?tenantId=${tenantId}` en vez de `params` object de axios. Si `tenantId` contiene caracteres especiales, causa encoding incorrecto. Inconsistente con el resto de servicios que usan `params`. | 🔴 Critical |
| 2 | Todas las páginas CRUD | ~14 | **`tenantId` fallback a `''` vacío** — `authStore.user?.tenantId \|\| ''` pasa string vacío al backend si no hay sesión. Backend responde 400/403, pero el error no se maneja como "no autenticado". Mejor guard en router o early-return. | 🔴 Critical |
| 3 | — | — | **Cero tests** para 6 páginas nuevas + 5 servicios nuevos. `npm run test` es placeholder según proyecto. | 🔴 Critical |

#### 🟡 Suggestions

| # | File | Line | Suggestion | Category |
|---|------|------|------------|----------|
| 1 | `factura.service.ts`, `producto.service.ts`, `proveedor.service.ts` | 22, 20, 15 | **`tenantId` en params del PUT, pero en body del POST** — inconsistencia en diseño de API. Estandarizar. | Maintainability |
| 2 | `patrimonio.service.ts` | 6 | **Path param vs query param** — único servicio que usa path param para tenantId. Inconsistente con gastos, prestamos, ventas. | Maintainability |
| 3 | `accounting.service.ts` vs `analytics.service.ts` | — | **Posible duplicado** — ambos tienen `consultar` + `recalcular`. Verificar si endpoints son distintos o uno sobra. | Maintainability |
| 4 | `PrestamosPage.vue` | 115 | **Double-fetch en `savePago`** — recarga pagos + loans completos. Podría ser update optimista. | Performance |
| 5 | `PatrimonioPage.vue` | 36 | **`toggleEdit` guarda sin validación** — no hay `formRef.validate()` ni rules en inputs. | Correctness |
| 6 | `AccountingPage.vue` | 52 | **Period mask `####-##` permite `2025-13`** — meses inválidos pasan la máscara. | Correctness |
| 7 | `EmptyState.vue` | 42 | **CSS animation 8s infinita sin `prefers-reduced-motion`** — el ring decorativo gira siempre, incluso fuera de viewport. | Accessibility |
| 8 | `PresentacionesDialog.vue` | 118 | **`conversion` input `type="text"`** en vez de `type="number"`. No valida ≤ 0. | Correctness |
| 9 | `types/index.ts` | — | **Breaking rename de campos** — `productoId`→`productId`, `cantidad`→`quantity`, `precioUnitario`→`unitPrice`, `descuento`→`discount`, `metodoPago`→`paymentMethod`, `descuentoGlobal`→`globalDiscount`. Correcto para alinear con backend, pero rompe compatibilidad con código previo. | Maintainability |
| 10 | `ProductosPage.vue` | — | **Search filtra en frontend sobre `getAll()`** — para catálogos grandes debería usar `GET /search` paginado del backend (gap #2 existente en tabla superior). | Performance |
| 11 | `PrestamosPage.vue` | 298-301 | **`pagoForm` sin validación** — amount=0 pasa sin error. | Correctness |

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
