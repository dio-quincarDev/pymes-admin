# GAPS — Discrepancias entre Documentación y Realidad

## Auth Service

| Gap | Documentación dice | Realidad es | Impacto | Estado |
|-----|--------------------|-------------|---------|--------|
| — | *(sin gaps tras correcciones de esta sesión)* | — | — | ✅ |
| — | V1 sin composite/partial indexes para consultas frecuentes | V2 agregó índices en user_tenants, refresh_tokens, invitations, audit_log | Medio | ✅ |
| — | *(sin gaps funcionales nuevos)* | — | — | — |
| 🔴 | Refresh token rotation tiene TOCTOU — `JwtServiceImpl.validateAndRevokeRefreshToken()` | read→check→write no atómico permite doble uso concurrente del mismo refresh token | **Crítico** — sesión hijackeable | ✅ `@Lock(PESSIMISTIC_WRITE)` en `RefreshTokenRepository.findByTokenHash()` (2026-07-30) |

### Code Review Findings (2026-07-21)

#### 🔴 Critical

| # | Gap | Fix | Severidad | Estado |
|---|-----|-----|-----------|--------|
| 1 | **Dos whitelists divergentes** — `SecurityConfig.WHITE_LIST` (Spring Security) y `JwtAuthenticationFilter.publicPaths` (filtro JWT) definen rutas públicas por separado. Si se agrega un endpoint público y se olvida actualizar una, el resultado es 401 del filter o 403 de security. | Unificar en una sola fuente. `JwtAuthenticationFilter.shouldNotFilter()` ahora lee de `SecurityConfig.WHITE_LIST` vía `AntPathMatcher`. Se eliminó `publicPaths` duplicada. | 🔴 Critical | ✅ 2026-07-21 |

### Code Review Findings (2026-07-29)

#### 🔴 Critical

| # | Gap | Fix | Severidad | Estado |
|---|-----|-----|-----------|--------|
| 1 | **TOCTOU en refresh token rotation** — `JwtServiceImpl.validateAndRevokeRefreshToken()` (L197-218) ejecuta read→check→write no atómico. Dos requests concurrentes con el mismo refresh token (ya usado en rotación legítima) pasan ambos el `revoked=false` check antes de que el primero escriba `revoked=true`. Cada uno genera un par nuevo de tokens. El atacante que robe un refresh token puede hacer refresh simultáneo con la víctima y mantener acceso indefinido sin ser detectado. | `@Version` optimistic locking, o `@Lock(PESSIMISTIC_WRITE)`, o SQL atómico `UPDATE refresh_tokens SET revoked=true WHERE token_hash=? AND revoked=false RETURNING *`. La solución debe garantizar que solo un hilo pueda rotar un token dado. | 🔴 Critical | ✅ `@Lock(PESSIMISTIC_WRITE)` en `RefreshTokenRepository.findByTokenHash()` → `SELECT FOR UPDATE` serializa el read-check-write. Perdedor ve `revoked=true` → REUSE DETECTED + `deleteByUserId`. Test de concurrencia en `AuthApiIntegrationTest` (2026-07-30) |

#### 🟡 Suggestions

| # | Gap | Fix | Categoría |
|---|-----|-----|-----------|
| 16 | **Email casing inconsistente** — `AuthServiceImpl.completeRegistration()` usa `request.email()` directo, mientras `InvitationServiceImpl.registerAndAccept()` usa `.toLowerCase()`. PostgreSQL `=` es case-sensitive en VARCHAR. Un usuario registrado con "User@Example.com" no puede loguear con "user@example.com". | Normalizar email a lowercase en `completeRegistration()` o en `RegisterRequest` vía DTO (`@JsonDeserialize`). | Correctitud | ✅ `.toLowerCase()` en `completeRegistration()` (2026-07-30) |
| 17 | **JWT `jti` generado pero no usado** — `JwtServiceImpl.createToken()` genera `.id(UUID.randomUUID())` pero el `jti` no se persiste ni expone. Sin él, `logout()` no puede identificar qué refresh token corresponde a la sesión actual y debe eliminar TODOS los refresh tokens del usuario (logout global forzado). No hay "cerrar sesión de este dispositivo". | Persistir `jti` en `RefreshToken` y exponerlo al cliente. `logout()` recibe `jti` y borra solo ese token. O documentar como diseño deliberado. | Seguridad | ✅ documentado como diseño deliberado — Logout Global multi-session + `jti` en RTR (DAILY 2026-04-12 / 2026-05-05) |
| 18 | **`@Transactional` en métodos Redis-only** — `PasswordResetServiceImpl.generateResetToken()`, `EmailVerificationServiceImpl.generateAndSendPendingRegistrationEmail()` y `EmailVerificationServiceImpl.generateVerificationToken()` tienen `@Transactional` pero solo interactúan con Redis, no con la DB. Consumen conexiones del pool HikariCP (`max 20`) innecesariamente. | Mover `@Transactional` solo a métodos que realmente escriben en DB, o usar `@Transactional(propagation = Propagation.NOT_SUPPORTED)`. | Performance | ✅ Quitado de `generateVerificationToken()` y `generateAndSendPendingRegistrationEmail()` (2026-07-30) |
| 19 | **`deleteByUserId` revertido por rollback en reuse detection** — `JwtServiceImpl.validateAndRevokeRefreshToken()` llama `deleteByUserId()` y luego lanza `TokenRevokedException` (unchecked). La excepción hace rollback de toda la transacción, incluyendo el DELETE. El token viejo queda `revoked=true` pero la familia NO se borra. | Cambiar a `REQUIRES_NEW` propagation en el `deleteByUserId`, o usar un `@Async` audit event que corra después del commit. | Seguridad | ✅ `TransactionTemplate.executeWithoutResult()` para REQUIRES_NEW (2026-07-30) |

#### 🟡 Suggestions

| # | Gap | Fix | Categoría |
|---|-----|-----|-----------|
| 1 | **`app.cors.allowed-origins` sin default** (`OAuth2AuthenticationSuccessHandler.java:60-61`) — `@Value("${app.cors.allowed-origins}")` no aparece en `application.yaml` ni tiene default. Si no está en `.env`, la app no arranca. | Agregar default: `@Value("${app.cors.allowed-origins:http://localhost:9200}")` | Correctitud | ✅ 2026-07-30 |
| 2 | **`AUTH001` retorna 400 en vez de 401** (`CodigoError.java:14`) — `INVALID_CREDENTIALS` usa `HttpStatus.BAD_REQUEST`. Un 401 es semánticamente correcto para credenciales inválidas. | Cambiar `HttpStatus.BAD_REQUEST` → `HttpStatus.UNAUTHORIZED` | Correctitud | ✅ 2026-07-30 |
| 3 | **DB lookup en cada request autenticado** (`JwtAuthenticationFilter.java:84`) — `userRepository.findById(validated.userId)` en cada request. Si la DB tiene latencia, afecta a todos los endpoints protegidos. | Cache de usuarios activos (Caffeine) con TTL corto o solo validar si el usuario existe en Redis. | Performance | Diferido (ponytail) — no es bug; agregar cache cuando el load lo demande (DAILY 2026-07-30) |
| 4 | **JWT access-expiration default 1h, refresh 24h** (`application.yaml:85-86`) — Refresh token con TAN larga. | Reducir refresh a 12h o 6h según política de seguridad. | Security |
| 5 | **Cookie OAuth2 intent sin `Secure` flag detrás del gateway** (`OAuth2IntentCookieFilter.java:42`) — `request.isSecure()` es `false` si el gateway reenvía en HTTP interno. | Forzar `cookie.setSecure(true)` siempre, o configurable. | Security | Fix parcial: `setSecure(request.isSecure())` (2026-07-16); detrás del gateway (HTTP interno) `isSecure()`=false → sigue abierto |
| 6 | **`@Lazy` circular** (`EmailVerificationServiceImpl.java:41`) — `EmailVerificationServiceImpl` ↔ `AuthServiceImpl` | Extraer lógica de registro a tercer service | Mantenibilidad |
| 7 | **`extractEmail()` duplicado** (`InvitationServiceImpl.java:184`, `MemberServiceImpl.java:143`) — misma lógica en 3 servicios | Utility class o leer directo de `SecurityContextHolder` | Mantenibilidad |
| 8 | **`/exchange` sin rate limit** (`AuthApiController.java:81-83`) — cualquiera puede brute-forcear códigos OAuth | Rate limit por IP | Security | Diferido (ponytail) — hardening, no bug; requiere filter config + Redis en gateway (DAILY 2026-07-30) |
| 9 | **CORS permisivo con credenciales** (`WebCorsConfig.java:21,24`) — `allowedOriginPatterns` permite `*` | Separar orígenes productivos y dev | Security |
| 10 | **JWT completo como key Redis** (`TokenBlacklistService.java:31`) — tokens largos = keys largas | Hashear el token como key | Performance |
| 11 | **`Thread.sleep(200)` anti-enumeration** (`PasswordResetServiceImpl.java:96-100`) — primitivo | Delay constante async + respuesta genérica | Security |
| 12 | **Método logueado doble** (`GlobalExceptionHandler.java:200-201`) — `request.getMethod()` impreso 2 veces | Corregir formato | Mantenibilidad |
| 13 | **Falta índice en refresh_tokens** (`V2__index_optimizations.sql:16-18`) — query de reuso busca por `token_hash + revoked` | Índice compuesto `(token_hash, revoked)` | Performance |
| 14 | **companySlug sin validación de formato** (`RegisterRequest.java:28`) — solo valida size | `@Pattern(regexp = "^[a-z0-9-]+$")` | Correctitud |
| 15 | **`CODE_TTL` constante no usada** (`OAuth2AuthenticationSuccessHandler.java:63,169`) — hardcodea `Duration.ofMinutes(2)` | Usar `CODE_TTL` | Mantenibilidad |

## Core Service

| # | Gap | Documentación dice | Realidad es | Impacto |
|---|-----|--------------------|-------------|---------|
| 🔴 | **Sin modelo de estructura de costos** | Sistema trata gastos como entradas planas sin recurrencia ni colaboradores | No hay entidades para salarios con frecuencias distintas, gastos fijos con día de ejecución, ni configuración laboral. El motor analítico no puede razonar sobre estructura de costos real | **Diferenciador** — imposibilita daily cost vs daily sales, break-even, pricing basado en costos reales. El Financial Health Engine opera con datos incompletos | ✅ Implementado (2026-07-31) — ver `COSTOS_ENGINE.md` |
| 🔴 | **División por cero en `conversion_factor`** | 6 queries de `analisisTendencia` (L128,136), `analisisMargen` (L171,179) y `analisisAlertas` (L287,288) dividen `unit_price / conversion_factor` sin `NULLIF` | `conversion_factor INTEGER NOT NULL DEFAULT 1` admite valor 0. Si un producto tiene `conversion_factor = 0`, esos 3 motores fallan con division-by-zero y **matan `ejecutarCompleto` completo**. Las otras 6 queries ya usan `NULLIF` (L316,326,368-371,436) | **Alto** — fix: envolver en `NULLIF(conversion_factor, 0)` como el resto | ✅ `NULLIF(conversion_factor, 0)` en los 6 sitios + test `conversionFactorCero_noRompeMotores` (6 IT verdes, 2026-07-31) |
| — | *(sin gaps funcionales)* | — | — | — |
| 1 | Endpoints no documentados | CORE.md/ARCHITECTURE.md listan solo CRUD clásico | `GET /search` (paginado) y `GET /{tenantId}/categories` existen | Bajo — endpoints internos, frontend los conoce | ✅ |
| 2 | Todos los gaps de code review cerrados en 2026-07-21 | — | — | — | ✅ |

### Code Review Findings (2026-07-21)

#### 🔴 Critical

| # | Gap | Fix | Severidad |
|---|-----|-----|-----------|
| 1 | **`tenantId` no validado contra JWT** — Todos los controllers reciben `@RequestParam UUID tenantId` del frontend. **No se valida que el tenantId del JWT (inyectado por el gateway como `X-Tenant-Id`) coincida con el tenantId de la request**. Un usuario autenticado en tenant A podría enviar `?tenantId=B` y acceder a datos de otro tenant. Solo algunos services verifican manualmente (FacturaService, GastoService). | `TenantValidationFilter` — Filter que compara `X-Tenant-Id` header vs `?tenantId=` param, 403 si difieren. | 🔴 Critical | ✅ 2026-07-24 |
| 2 | **Sin `@PreAuthorize`, `@Secured` ni `@EnableMethodSecurity`** — El gateway inyecta `X-User-Role` (OWNER, ADMIN, CONTABLE, VIEWER) pero el core nunca lo lee. Cualquier usuario autenticado puede ejecutar cualquier endpoint (crear, actualizar, eliminar) sin importar su rol. | `RoleHeaderFilter` lee `X-User-Role` → `SecurityContext` con `ROLE_<rol>`. `SecurityConfig` con `@EnableMethodSecurity` + `spring-boot-starter-security`. `@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")` en 18 endpoints WRITE across 10 controllers. CONTABLE/VIEWER solo leen. Tests: +`spring-security-test` + `@WithMockUser(roles = "OWNER")` en 4 integration tests. | 🔴 Critical | ✅ 2026-07-27 |

#### 🟡 Sugerencias

| # | Gap | Fix | Categoría |
|---|-----|-----|-----------|
| 1 | **OpenFeign declarado sin uso** — `spring-cloud-starter-openfeign` en pom.xml con `services.auth.base-url` en config, pero no hay ningún `@FeignClient` en el código. | Remover dependencia muerta o implementar el FeignClient si se necesita. | Mantenibilidad |
| 2 | **SeedDataRunner ~1040 líneas** — Ejecuta en cada startup. En PROD intenta insertar seeds aunque falla silenciosamente por conflictos. | Ejecutar solo en dev o con flag `app.seed.enabled=false`. | Performance |
| 3 | **UPDATE por item en loop** (`FacturaServiceImpl.java:287-294`) — `buildItem()` ejecuta un `jdbc.update()` individual por cada item de la factura para actualizar `last_unit_price`, `total_investment`. Si una factura tiene 50 items, son 50 UPDATEs. | Batchificar en lote único, similar a `reverseProductStats`. | Performance |
| 4 | **Cache stampede sin protección** — 7 caches con TTL fijo de 5 min. Si el TTL expira y 10 requests llegan simultáneamente, todas ejecutan la consulta DB. | Agregar `sync = true` en `@Cacheable` o usar locking. | Performance |
| 5 | **Analytics CTEs sin índices covering verificados** — 9 engines hacen `AVG()`, `STDDEV()`, `GROUP BY` sobre `invoice_items` JOIN `invoices`. Verificar que los índices covering (V16) cubren estas consultas. | Revisar plan de ejecución de las 9 CTEs y ajustar índices si es necesario. | Performance | ✅ 2026-07-31 — verificado el motor más exigente (OLS proyección) con EXPLAIN ANALYZE real (Testcontainers PG15, 18.3k items / 2 tenants): a escala PYME PG elige `idx_invoices_tenant` + hash join full-scan de `invoice_items` (~4ms, óptimo); al crecer la tabla cambia solo al nested loop con `idx_invoice_items_invoice_product` (ya existe, V1:133). `idx_invoice_items_invoice_product_tenant` del análisis previo es **imposible**: `invoice_items` no tiene columna `tenant_id` (el aislamiento multi-tenant vive en `invoices`). **Sin índice nuevo.** |

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

1. **Blacklist Redis con JWT completo** (`AuthenticationFilter.java:64`) — Token ~1KB como key Redis. Fix: hashear token con SHA-256. — **Estado:** Diferido (ponytail) — aceptado; keys ≤1h, memoria trivial a escala PYME.

2. **Blacklist check = RTT por request** (`AuthenticationFilter.java:64`) — Sin cache local. Fix: añadir Caffeine cache con TTL 30s. — **Estado:** Diferido (ponytail) — aceptado; performance, no bug. Caffeine cuando el load lo demande.

3. **`/exchange` sin rate limit** (`RouterValidator.java:15-30`) — Ruta pública permite brute-force sobre códigos OAuth (TTL 2 min). Fix: rate limit por IP en gateway. — **Estado:** Diferido (ponytail) — aceptado; el código es UUID de alta entropía, hardening no bug. Requiere Redis en gateway.

4. **Swagger solo agrega Auth** (`SwaggerAggregatorConfig.java:28`) — Core Service no aparece. Fix: agregar URL de core si tiene OpenAPI docs, o ruta en gateway.

5. **Property name inconsistente** (`JwtUtils.java:17` vs `.env.example:11`) — Realidad: Spring Boot relaxed binding mapea `JWT_SECRET` → `jwt.secret`. No hay inconsistencia real. Sin acción.

6. **Error antes de `flatMap` no detiene flujo** — Ya resuelto en código previo: `return onError(...)` en líneas 45, 50, 60. Sin acción.

7. **Faltan tests de integración** — Solo unitarios con mocks. Fix: tests con `WebTestClient` simulando requests HTTP reales.

8. **Default JWT secret conocido** (`.env.example:11`) — String público como default permite forjar JWTs. Fix: generar random con `openssl rand -base64 32` y poner el valor generado. — **Estado:** Aceptado — el código ya falla rápido sin `JWT_SECRET` (C7, sin default); el placeholder solo vive en `.env.example` (git-ignored). Preocupación operacional: reemplazar al generar el `.env` real.

## Frontend

| # | Gap | Documentación dice | Realidad es | Impacto | Prioridad |
|---|-----|--------------------|-------------|---------|-----------|
| — | *(sin gaps funcionales)* | — | — | — | — |
| 1 | ConfiguracionPage read-only | UI muestra campos editables (categorías, unidades, ubicaciones) | No hay endpoints PUT ni diálogos de edición | Medio — usuario espera poder editar. Pendiente backend. | ⬜ Non-priority → eliminar como ruta (Fase 4c) |
| 2 | Frontend no usa búsqueda paginada | Backend tiene `GET /search` paginado desde 2026-07-12 | `ProductosPage.vue` usaba `getAll()` sin paginación; "Cargar más" reemplazaba filas en vez de apilar | Alto — degradación en catálogos grandes (>100 productos) | ✅ 2026-07-27 |
| 3 | Descuento en Factura es monto fijo | UI muestra `prefix="$"` — usuario espera porcentaje (5%, 10%) | Input trata descuento como monto, no como porcentaje | Medio — UX confusa, usuario no puede poner descuentos promocionales | ⬜ Non-priority (ponytail: backend ya soporta %, cambio UI trivial) |
| 4 | precioUnitario no usa conversión | Presentaciones tienen `conversion` factor | `precioUnitario` se auto-llena con `lastUnitPrice` sin dividir por conversión | Alto — precio unitario no refleja costo real por unidad base | ⬜ Non-priority (ponytail: requiere backend change, no es solo frontend) |

### Workflow / UX Findings (audit 2026-08-14)

#### 🔴 Cognitive overload — Dashboard

| # | Gap | Realidad es | Impacto | Fix propuesto |
|---|-----|-------------|---------|---------------|
| 1 | **7 KPI cards** — Margen Bruto, Operativo, Neto son 3 derivados del mismo dato | Wall of numbers. Un PYME owner no procesa 7 métricas de un vistazo | Alto | Colapsar a 1 card "Ganancia del mes" (reduce 7→5) |
| 2 | **QuickActions muerto** — 2 acciones duplican sidebar, emit `exportar` nadie lo escucha | Código muerto en dashboard. Confunde al usuario con opciones que no funcionan | Medio | Eliminar completamente |
| 3 | **Sparklines de 2 puntos** — `sparkline()` produce `[prev, cur]`, renderiza SVG polygon | Ruido visual. Delta % ya comunica dirección + magnitud | Bajo | Eliminar sparklines |
| 4 | **7 secciones apiladas antes del fold** — header + error + KPIs + chart + activity + pending + health + quickactions | El usuario no sabe dónde mirar | Alto | Reducir a 4-5 secciones (merge activity+pending, eliminar quickactions) |
| 5 | **CSS duplicado 4 veces** — skeleton, section-title, empty-state, list-row en cada panel dashboard | ~200 líneas repetidas. Mantenibilidad | Bajo | Extraer a clases globales en app.scss |

#### 🟡 Navigation overload

| # | Gap | Realidad es | Impacto | Fix propuesto |
|---|-----|-------------|---------|---------------|
| 6 | **12 items en sidebar** — 3 grupos × 4-5 items | Un PYME owner no necesita rutas separadas para "Patrimonio", "Contabilidad", "Análisis de Gastos" | Alto | Reducir a ~7 items fusionando sub-vistas |
| 7 | **Bottom nav: "More" circular** — 4 tabs + "More" que abre sidebar con 12 items | El "More" es un catch-all que revela que la nav está sobrecargada | Medio | Actualizar tabs: Dashboard, Productos, Facturas, Costos |

#### 🟡 Jargon / copy

| # | Gap | Realidad es | Impacto | Fix propuesto |
|---|-----|-------------|---------|---------------|
| 8 | **"GASTO_OPERATIVO" como label** — tipo de factura expone código interno | Usuario ve un término técnico en vez de "Gasto" | Alto | Renombrar a "Gasto" |
| 9 | **"REGISTRADA" status** — factura pendiente de pago muestra estado de BD | No es natural. "Pendiente" comunica mejor | Medio | Renombrar a "Pendiente" |
| 10 | **"Colaboradores"** — empleados del negocio | Término corporativo. "Equipo" o "Empleados" es más natural | Bajo | Renombrar |
| 11 | **"Margen Operativo/Neto"** — jargon contable | Un PYME owner conoce "ganancia", no "margen" | Medio | Renombrar a "Ganancia bruta/neta" |
| 12 | **"Inversion Total"** — total gastado en compras de producto | "Inversión" implica capital, no gasto corriente | Medio | Renombrar a "Total gastado" |
| 13 | **"cross-supplier"** — inglés en UI en español | Inconsistente con el resto del idioma | Bajo | Traducir |

#### 🟡 Page structure

| # | Gap | Realidad es | Impacto | Fix propuesto |
|---|-----|-------------|---------|---------------|
| 14 | **FacturasPage 989 líneas** — 3 formularios en 1 diálogo (factura, gasto, salario) | Complejidad cognitiva extrema. Un usuario no debería distinguir 3 tipos de "crear" | Alto | Separar flujos: "Gasto rápido" vs "Factura con items" |
| 15 | **AnalisisGastosPage: supplier analysis visible por defecto** — comparison + recommendations + predictions | Data science dashboard overwhelming para PYME owner que quiere "quién es más barato" | Alto | Sub-sección colapsable, mostrar solo inversión + alerts por defecto |
| 16 | **CostosPage: Config tab con 1 input** — "días laborales" justifica un tab completo | Overkill | Bajo | Mover a inline o sección within page |
| 17 | **ConfiguracionPage read-only** — 85 líneas, 3 cards, sin edición | Dead end. El usuario ve algo que no puede cambiar | Bajo | Eliminar ruta, mover a menú usuario |

### Code Review Findings (2026-07-15)

#### 🔴 Critical

| # | File | Line | Issue | Severity |
|---|------|------|-------|----------|
| 1 | `prestamo.service.ts`, `producto.service.ts`, `factura.service.ts` | 24, 26, 21 | **`tenantId` interpolado directo en URL string** — `?tenantId=${tenantId}` en vez de `params` object de axios. Si `tenantId` contiene caracteres especiales, causa encoding incorrecto. Inconsistente con el resto de servicios que usan `params`. | 🔴 Critical | ✅ 2026-07-21 |
| 2 | Todas las páginas CRUD | ~14 | **`tenantId` fallback a `''` vacío** — `authStore.user?.tenantId \|\| ''` pasa string vacío al backend si no hay sesión. Backend responde 400/403, pero el error no se maneja como "no autenticado". Mejor guard en router o early-return. | 🔴 Critical | ✅ 2026-07-25 |
| 3 | — | — | **Cero tests** para 6 páginas nuevas + 5 servicios nuevos. `npm run test` es placeholder según proyecto. | 🔴 Critical | ✅ 2026-07-25 |

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

### Code Review Findings (2026-07-30)

#### 🔴 Critical

| # | File | Line | Issue | Severidad |
|---|------|------|-------|-----------|
| 1 | `types/index.ts` + `GastosPage.vue` + `gasto.service.ts` | — | **Field name mismatch Gastos** — Frontend envía `{ category, description, amount, expenseDate, paymentMethod }` pero backend `GastoRequest` espera `{ categoria, descripcion, monto, fecha, metodoPago }`. POST/PUT fallan con 400. Mismo patrón que el bug de Patrimonio. | 🔴 Critical | ✅ 2026-07-30 |
| 2 | `types/index.ts` + `accounting.service.ts` + AccountingPage/DashboardPage | — | **Field name mismatch MetricasFinancieras** — Frontend `MetricasFinancieras` usa inglés (`totalIncome`, `costOfGoods`, `operatingExpenses`, `loanPayments`), backend `MetricasResponse` responde español (`totalIngresos`, `costoMercaderia`, `gastosOperativos`, `pagosPrestamos`). GET /accounting/consultar devuelve datos que no se mapean. | 🔴 Critical | ✅ 2026-07-30 |

#### 🟡 Suggestions

| # | File | Line | Suggestion | Categoría |
|---|------|------|------------|-----------|
| 1 | `GastoServiceImpl.java` + `GastoController.java` | — | **Date range query existe pero no se expone** — `GastoRepository` tiene `findByTenantIdAndExpenseDateBetween` pero ningún endpoint ni el frontend la usan. No hay forma de filtrar gastos por período desde la UI. | Funcional |
| 2 | `GastosPage.vue` | — | **Sin paginación** — Trae todos los gastos del tenant. Sin `page`/`size`. Degradación en catálogos grandes. | Performance |
| 3 | `AnalisisGastosPage.vue` | — | **Misnomer** — A pesar del nombre "Análisis de Gastos", no analiza gastos operativos. Analiza inversión en productos (`totalInvestment`). El nombre confunde al usuario. | UX |

### Code Review Findings (2026-07-21)

#### 🔴 Critical

| # | File | Line | Issue | Severidad |
|---|------|------|-------|-----------|
| 1 | `boot/axios.ts` | — | **Sin refresh token rotation** — el interceptor captura 401 y borra sesión, nunca intenta renovar con refresh token. Token expira en 1h, usuario force login cada hora. | 🔴 Critical | ✅ 2026-07-25 |
| 2 | — | — | **1 test para 106 archivos** — solo existe `errors.spec.ts`. Cero tests para composables, stores, pages, services. | 🔴 Critical | ✅ 2026-07-25 |

#### 🟡 Suggestions

| # | File | Line | Suggestion | Categoría |
|---|------|------|------------|-----------|
| 1 | `store/index.ts` | 148 | **Listener `auth:401` duplica lógica** — replica `clearSession()` parcialmente. `clearSession()` ya hace `window.location.href = '#/login'`, lo que recarga la store. | Mantenibilidad | ✅ 2026-07-25 |
| 2 | Todas las pages | ~14 | **`tenantId` fallback a `''`** — `authStore.user?.tenantId \|\| ''` en todas las pages. Si store vacío, se envía `?tenantId=` → 400. | Correctitud | ✅ 2026-07-25 |
| 3 | `utils/errors.ts` | — | **`isAuthError()` lee del raw axios** en vez del error normalizado — el interceptor normaliza a `Error` con `code/status/details/isBackendError`, pero `isAuthError` analiza `error.response?.data?.codigo` directamente. | Mantenibilidad | ✅ 2026-07-25 |
| 4 | `LoginPage.vue` | — | **Redirect query ignorado** — `Router.beforeEach` redirige a `/login?redirect=/dashboard/...` pero LoginPage nunca lo lee. | Correctitud |

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
