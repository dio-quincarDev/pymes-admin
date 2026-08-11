# Daily Reports — Gateway Solutions

Registro cronológico de problemas resueltos y decisiones de implementación en **gateway-pymes**.

---

## 2026-08-10 — Caddy: subdominio pymeq + HTTPS

### Contexto

Migración del subdominio a `pymeq.dioquincar.dev`. Cambio de infraestructura en la instancia, sin código.

### Qué se hizo

- Caddyfile: bloques `https://pymeq.dioquincar.dev` con `/api/*`, `/oauth2/*` y `/login/*` → `pymes-gateway:8080`; resto → `pymes-frontend:9200`.
- HTTPS (Let's Encrypt automático) requerido por Google OAuth: no admite redirect `http://` en dominios públicos.

**Estado:** ✅ COMPLETADO

---

## 2026-07-28 — Sin cambios

TeamsPage migration fue solo frontend/auth. Gateway no requiere modificaciones.

---

## 2026-07-16 — CORS Bug: `globalcors` reinstalado con `allowed-origins` (doble capa)

### Problema

SCG tiene dos caras con CORS:
1. `globalcors` con `allowed-origin-patterns` → POST real devuelve 403 sin ACAO (bug SCG conocido). 7 intentos de fix fallaron.
2. Sin `globalcors` → OPTIONS preflight devuelve 403. SCG intercepta preflight internamente y no enruta.

### Solución

Se restauró `globalcors` con `allowed-origins` (exacto, no pattern) para OPTIONS preflight. El auth service agrega ACAO a requests reales (POST/GET). Doble capa necesaria:

| Capa | Rol | Config |
|------|-----|--------|
| **Gateway** | OPTIONS preflight → 200 + ACAO | `globalcors` con `allowed-origins` + `allow-credentials: true` |
| **Auth service** | ACAO en requests reales | `WebCorsConfig` con `setAllowedOrigins` + `allowCredentials(true)` |

### Flujo resultante

```
OPTIONS: Gateway globalcors → 200 + ACAO (no llega al auth)
POST:    Gateway enruta → Auth service → 201 + ACAO
```

### Lecciones

1. **SCG no puede delegar OPTIONS al downstream.** El handler de preflight corre antes del routing. Sin `globalcors`, OPTIONS siempre 403.
2. **`allowed-origin-patterns` vs `allowed-origins`:** Con `allowCredentials(true)`, los patterns no matchean literales. Usar `allowed-origins` con valores exactos.
3. **Frontend en puerto 9200**, no 9000 (AGENTS.md incorrecto). Origin mismatch inicial.

### Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `application.yaml` | Re-agregado `globalcors` con `allowed-origins` (exacto) + `DedupeResponseHeader` |
| `WebCorsConfig.java` (auth) | `setAllowedOriginPatterns` → `setAllowedOrigins` |
| `.env.example` | Restaurada variable `CORS_ALLOWED_ORIGINS` |

### Tests

38 gateway tests, 0 fallos. CORS verificado con curl: OPTIONS 200 + POST 201, sin ACAO duplicado.

**Estado:** ✅ RESUELTO

---

## 2026-06-23 — Open endpoints: `/api/v1/auth/exchange` + `/api/v1/auth/oauth2/intent`

### Problema
`RouterValidator.openEndPoints` no incluia `/api/v1/auth/exchange` ni `/api/v1/auth/oauth2/intent`. Usuarios nuevos (sin token en cache) recibian 401 al intentar el code exchange post-OAuth2. El auth-service tiene estos endpoints como publicos en su `SecurityConfig`, pero el gateway los interceptaba antes.

### Solucion
Agregadas ambas rutas al set `openEndPoints` en `RouterValidator.java`.

```java
private static final Set<String> openEndPoints = Set.of(
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh",
    "/api/v1/auth/verify-email",
    "/api/v1/auth/resend-verification",
    "/api/v1/auth/forgot-password",
    "/api/v1/auth/reset-password",
    "/api/v1/auth/exchange",
    "/api/v1/auth/oauth2/intent",
    // ...
);
```

### Tests
`RouterValidatorTest` actualizado: 22 cases (15 open + 6 secured + 1 query string).

**Archivos:** `RouterValidator.java:23`, `RouterValidatorTest.java`
**Estado:** ✅ RESUELTO

---

## Code Review — 2026-06-16 / 2026-06-19

Skills usadas: java-springboot, spring-webflux-testing, spring-security-testing.

### Resumen

| Severidad | Total | Resueltos |
|-----------|-------|-----------|
| Crítico | 1 | ✅ 1 |
| Medio | 5 | ✅ 5 |
| Bajo | 3 | ⬜ 3 |

---

### C1 — AuthenticationFilter ausente en profiles (CRÍTICO)

`AuthenticationFilter` definido en `application.yaml` base. Los profiles dev/stg/prod reemplazan la lista de rutas y omiten el filtro → `/api/v1/**` no valida JWT en esos perfiles.

**Solución:** Agregado `filters: [PreserveHostHeader, AuthenticationFilter]` a auth-service en los 3 profiles.

Archivos: `application-dev.yaml`, `application-stg.yaml`, `application-prod.yaml`  
Estado: ✅ RESUELTO

---

### C2 — Doble parsing del JWT

`AuthenticationFilter` parseaba el token en `isInvalid()` y luego en `getClaims()`. Misma operación dos veces por request.

**Solución:** try-catch con `getClaims()` una sola vez; claims reusados.

Archivo: `AuthenticationFilter.java:54-60`  
Estado: ✅ RESUELTO

---

### C3 — Null claims generan string "null"

`String.valueOf(claims.get("userId"))` produce `"null"` literal si el claim no existe en el JWT. Ese string viajaba a los servicios downstream como header.

**Solución:** null-check ternario en userId, tenantId y role.

Archivo: `AuthenticationFilter.java:63-66`  
Estado: ✅ RESUELTO

---

### C4 — Sin validación de issuer/audience (BAJO)

`JwtUtils` solo verifica firma y expiración. No valida `iss` ni `aud`.

**Decisión:** SKIPPED — auth-service ya valida internamente (defensa en profundidad no crítica).

Archivo: `JwtUtils.java`  
Estado: ⬜ SKIPPED

---

### C5 — Ruta whitelist sin route en YAML (BAJO)

`/api/v1/auth/oauth2/**` está en `RouterValidator.openEndPoints` pero no había ruta explícita en los YAMLs.

**Decisión:** NO ACCIÓN — la ruta `auth-service-api` con `Path=/api/v1/**` ya lo cubre + whitelist salta el filtro.

Archivo: `RouterValidator.java`  
Estado: ⬜ NO ACCIÓN

---

### C6 — Security headers como default-filters (2026-06-19)

Agregados 4 security headers vía `AddResponseHeader` en default-filters. Sin clase Java nueva.

Headers: `Strict-Transport-Security`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`.

Archivo: `application.yaml:44-49`  
Estado: ✅ RESUELTO

---

### C7 — JWT secret sin fallback (2026-06-19)

`${jwt.secret:...}` tenía default → cambiado a `${jwt.secret}`. Falla al startup si `JWT_SECRET` no está configurado (fallo rápido).

Archivo: `application.yaml:51`  
Estado: ✅ RESUELTO

---

### C8 — CORS allowed-methods restringido (2026-06-19)

`allowed-methods: "*"` → `allowed-methods: "GET,POST,PUT,PATCH,DELETE,OPTIONS"`. Lista explícita en vez de comodín.

Archivo: `application.yaml:40`  
Estado: ✅ RESUELTO

---

### T1 — Tests unitarios: de 0 a 32

Sin tests para `JwtUtils`, `AuthenticationFilter`, `RouterValidator`.

**Solución:** 32 tests unitarios creados (4 JwtUtils + 21 RouterValidator + 7 AuthenticationFilter).

Archivos creados: `JwtUtilsTest.java`, `AuthenticationFilterTest.java`, `RouterValidatorTest.java`  
Estado: ✅ RESUELTO

---

### T2 — Tests de integración (PENDIENTE)

No hay WebTestClient, Testcontainers, ni Redis real. El flujo de blacklist en Redis nunca se prueba contra una instancia real.

**Pendiente:** requiere Testcontainers + reactor-test. Agregar cuando CI tenga Redis disponible.  
Estado: ⬜ PENDIENTE

---

### T3 — Sin StepVerifier (BAJO)

Gateway usa WebFlux pero ningún test emplea `StepVerifier` o `WebTestClient`. Tests usan `.block()` (funcional pero no idiomático reactive).

**Pendiente:** agregar reactor-test + WebTestClient junto con T2.  
Estado: ⬜ PENDIENTE (con T2)

---

## 2026-04-17 — OAuth2 via Gateway: Rutas para Social Login

**Problema:** Rutas OAuth2 (`/oauth2/**`) no definidas en perfiles → 404 al intentar login con Google desde el frontend.

**Causa:** Spring Cloud Gateway no mergea listas de rutas entre `application.yaml` base y los perfiles. El perfil sobreescribe completamente.

**Solución:** En `application-dev.yaml`, `application-stg.yaml`, `application-prod.yaml`:

```yaml
routes:
  - id: auth-service-oauth2
    uri: http://${AUTH_SERVICE_HOST:localhost}:8081
    predicates:
      - Path=/oauth2/**, /login/oauth2/**, /login/**, /v3/api-docs/auth
    filters:
      - PreserveHostHeader
```

**Redirect URIs en consolas de desarrolladores:**

| Proveedor | Redirect URI |
|-----------|--------------|
| Google | `http://localhost:8080/login/oauth2/code/google` |
| Facebook | `http://localhost:8080/login/oauth2/code/facebook` |

**Estado:** ✅ Google funcionando | ⏳ Facebook pendiente

---

## 2026-04-16 — CORS "Invalid CORS request" (BUG CONOCIDO)

**Problema:**

| Petición | Resultado |
|----------|-----------|
| OPTIONS (preflight) | 200 OK ✅ |
| POST (registro) | 403 Forbidden ❌ |

**Causa raíz:** Bug en Spring Cloud Gateway 3.2.0+ — el procesador interno de CORS intercepta peticiones antes de que `globalcors` pueda procesarlas. Headers `Vary: Origin` en respuesta confirman interferencia interna.

**Intentos realizados (todos fallidos para POST):**

| # | Solución | Resultado |
|---|----------|-----------|
| 1 | Custom CorsWebFilter (AbstractGatewayFilterFactory) | ❌ No se ejecuta |
| 2 | Custom CorsGlobalFilter (@Order HIGHEST_PRECEDENCE) | ❌ Interceptado antes |
| 3 | Custom WebFilter (@Order HIGHEST_PRECEDENCE) | ⚠️ OPTIONS OK, POST 403 |
| 4 | globalcors origins específicos | ❌ 403 |
| 5 | globalcors origins: "*" | ❌ 403 |
| 6 | globalcors deshabilitado | ❌ Sin headers CORS |
| 7 | allowedOriginPatterns | ❌ Sin efecto |

**Soluciones a investigar:**
1. `default-filters: DedupeResponseHeader` para evitar duplicación de headers
2. CORS en Security del Gateway (WebFilter + SecurityWebFilterChain)
3. Deshabilitar CORS en Auth completamente (solo Gateway maneja CORS)

**Estado:** ❌ PENDIENTE

---

## 2026-04-13 — Swagger UI Agregado

**Problema:** Con múltiples microservicios, cada uno tendría Swagger en puertos internos inaccesibles.

**Solución:** Gateway como único punto de entrada para Swagger UI con dropdown multi-servicio.

```
Navegador → Gateway (:8080) → Swagger UI con dropdown:
  ├── Auth Service       (/v3/api-docs/auth → auth-service:8081)
  └── Payment Service    (/v3/api-docs/payment)  ← futuro
```

**URLs operacionales:**

| URL | Descripción |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | UI (recomendado) |
| `http://localhost:8080/webjars/swagger-ui/index.html` | Path físico del JAR |
| `http://localhost:8080/v3/api-docs/auth` | JSON OpenAPI auth-service |

> **Nota WebFlux:** `/swagger-ui/index.html` devuelve 404 en WebFlux. Usar siempre `/swagger-ui.html`.

**Problemas resueltos durante implementación:**

- `@Bean SwaggerUiConfigProperties` → `NoUniqueBeanDefinitionException`. **Fix:** usar `@Component + @PostConstruct` inyectando el bean existente.
- Profile sobrescribe rutas base → **Fix:** definir rutas Swagger en **cada** archivo de perfil.
- `springdoc.swagger-ui.urls` en YAML no se bindea en WebFlux → **Fix:** configurar URLs programáticamente en `SwaggerAggregatorConfig.java`.
- Producción: `SWAGGER_ENABLED=false` en `.env` o perfil Spring con `springdoc.swagger-ui.enabled: false`.

**Estado:** ✅ RESUELTO

---

## 2026-04-13 — CORS y Envío Real de Emails

**CORS Gateway:** Configurado vía `globalcors` en `application.yaml`. CORS del Auth-service como defensa en profundidad (eliminar `@EnableWebMvc` + `addCorsMappings` vacíos, crear `UrlBasedCorsConfigurationSource` bean).

**Email verification:** `createAndSendVerificationEmail()` solo hacía log. Fix: prefix correcto `spring.mail.*`, inyectar `JavaMailSender`, template HTML inline, token Redis con TTL 15min.

**Flujo:**
```
POST /register → crea usuario+tenant → token Redis (TTL 15min) → email HTML
→ Usuario click → POST /verify-email → Gateway → Auth → valida Redis → 200 OK
```

**Estado:** ✅ RESUELTO

---

## 2026-04-12 — Sincronización de Rutas Públicas y URISyntaxException en CI

**Rutas faltantes:** Gateway bloqueaba verificación de email y recuperación de contraseña. Añadidos a `auth-public`: `/verify-email`, `/resend-verification`, `/forgot-password`, `/reset-password`.

**URISyntaxException en CI:** Variables como `${AUTH_SERVICE_HOST}` no definidas en perfil `test` → URI malformada (`http::8081`). Fix: `application-test.yaml` con placeholders default (`AUTH_SERVICE_HOST_TEST: localhost`, `REDIS_HOST_TEST: localhost`).

**Estado:** ✅ RESUELTO

---

## 2026-04-inicial — Problemas de Bootstrap

**Lombok/SLF4J:** `@Slf4j` en `AuthenticationFilter.java` fallaba en compilación. Fix: agregar `org.projectlombok:lombok` como `<optional>true</optional>` en `pom.xml`.

**JWT WeakKeyException en tests:** Secreto de 240 bits < mínimo 256 bits requerido por RFC 7518 §3.2 para HS256. Fix: secreto de 256 bits en `application-test.yaml` + `@ActiveProfiles("test")` en la clase de test.

**Estado:** ✅ RESUELTO

---

*Creado: 2026-06-19 | Consolidación de GATEWAY-DOC.md + GATEWAY_REVIEW_REPORT.md*
