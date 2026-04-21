# 📊 Daily Reports & Auth Solutions — Historial de Implementaciones

> Registro histórico de decisiones técnicas, problemas resueltos y roadmap de desarrollo.

---

## 2026-04-17 — OAuth2 via Gateway 🌐🔐

### 🎯 Problema Inicial
El flujo OAuth2 no funcionaba porque:
1. Las rutas OAuth2 (`/oauth2/**`) no estaban definidas en los perfiles del Gateway
2. El Auth Service no tenía `/oauth2/**` en la whitelist de SecurityConfig
3. El redirect URI apuntaba al puerto interno (8081) en lugar del Gateway (8080)

### 📐 Solución Implementada

**1. Gateway Routes:**
```yaml
routes:
  - id: auth-service-oauth2
    uri: http://${AUTH_SERVICE_HOST:localhost}:8081
    predicates:
      - Path=/oauth2/**, /login/oauth2/**, /login/**, /v3/api-docs/auth
```

**2. Auth Service SecurityConfig:**
```java
private static final String[] WHITE_LIST = {
    "/oauth2/**",   // Agregado
    "/login/**",
};
```

**3. Redirect URI:**
```yaml
security:
  oauth2:
    client:
      registration:
        google:
          redirect-uri: "${OAUTH2_REDIRECT_URI:http://localhost:8080}/login/oauth2/code/google"
```

### ✅ Validaciones Requeridas

**Google Cloud Console → APIs & Services → Credentials:**

1. **Authorized JavaScript origins:** `http://localhost:9200`
2. **Authorized redirect URIs:** `http://localhost:8080/login/oauth2/code/google`
3. **OAuth consent screen:** Agregar email como test user

### 🔲 Pendiente: Facebook OAuth2
- Estado: ⏳ No testeado
- Redirect URI esperado: `http://localhost:8080/login/oauth2/code/facebook`

---

## 2026-04-20 — OAuth2 Pre-Auth Intent (Atomic Register) 🚀🔐

### 🎯 Problema Inicial (Resuelto)
Al registrarse con Google/Facebook, el sistema no sabía a qué empresa (tenant) asociar al usuario porque el flujo OAuth2 es atómico y no permite enviar datos adicionales (como nombre de empresa) en el redirect estándar.
- **Resultado anterior**: El usuario quedaba sin tenant (`tenant_id = null`) y debía crearlo manualmente después.

### 📐 Solución Implementada: Estrategia `state` + Redis Intent

Se implementó un mecanismo de "Intención de Registro" que persiste los datos de la empresa temporalmente antes de saltar a Google.

**1. Backend — OAuth2IntentService:**
- Nuevo endpoint: `POST /api/v1/auth/oauth2/intent` (Público).
- Guarda `{ companyName, companySlug }` en Redis con un TTL de 10 min.
- Retorna un `intentId` (UUID).

**2. Backend — OAuth2AuthenticationSuccessHandler:**
- Lee el parámetro `state` enviado por Google.
- Si el `state` coincide con un `intentId` en Redis:
    1. Crea el `Tenant` automáticamente.
    2. Crea el `UserTenant` vinculando al usuario como `OWNER`.
    3. Genera el JWT incluyendo ya el `activeTenantId`.
    4. Limpia el intent de Redis.

**3. Frontend — Pre-Auth Flow:**
- `AuthOptionsPage.vue` llama al intent antes de `openURL`.
- Construye la URL de Google incluyendo `&state=intentId`.
- `AuthCallback.vue` simplificado: ya no necesita lógica compensatoria de creación de tenant.

### 📊 Validación y Tests
- **Unit Tests**: `OAuth2IntentServiceImplTest` (4 tests pasando).
- **Integration Tests**: `OAuth2IntentIntegrationTest` (2 tests con Testcontainers pasando).
- **Security**: Whitelist de `/api/v1/auth/oauth2/intent` en `SecurityConfig`.
- **Docker Deployment**: Verificado con `docker compose up`. Se corrigió un error de compilación en el Frontend (PWA mode) agregando `package.json` y `capacitor.config.json` faltantes en `src-capacitor`.

---

## 2026-04-20 — NoResourceFoundException: /login 🐛

### 🎯 Problema Actual
Al intentar login con Google/Facebook OAuth2, el flujo falla con:
```
NoResourceFoundException: No static resource login
org.springframework.web.servlet.resource.ResourceHttpRequestHandler.handleRequest
```

### 📊 Diagnóstico

| Componente | Path Esperado | Path Recibido |
|------------|---------------|---------------|
| Gateway | `/login/**` → Auth:8081 | ✓ rutea |
| Auth Service | `/api/v1/auth/login` | `/login` ❌ |
| Controller | `@PostMapping("/login")` en `/api/v1/auth/login` | NO existe `/login` |

**Causa raíz:** El Auth controller está en `/api/v1/auth/login`, pero el Gateway rutea `/login/**` al auth service. Spring MVC no encuentra handler para `/login` y busca recurso estático.

### 📐 Solución Propuesta: 3 Opciones

#### Opción A: Thymeleaf Login Page (Recomendada)
- Dependencia: `spring-boot-starter-thymeleaf`
- Template: `src/main/resources/templates/login.html`
- Spring Security maneja todo automáticamente
- **Pros:** Funciona out-of-the-box, mínima complejidad
- **Cons:** 51KB extra en JAR

#### Opción B: Controller Explícito `/login`
- Nuevo controller `LoginController.java`
- GET `/login` → redirect frontend
- POST `/login` → delegar a AuthService
- **Pros:** Sin dependencias extra, control total
- **Cons:** Más código, duplicación

#### Opción C: Gateway Route Change
- Cambiar route `/login/**` → frontend directamente
- Cambiar redirect URIs OAuth2 en Google Console
- **Pros:** Sin cambios en auth service
- **Cons:** Separa responsabilidades, config extra

---

### 📋 Detalle Opción A - Thymeleaf

**Qué agregar:**
- Dependencia: `spring-boot-starter-thymeleaf`
- Template: `src/main/resources/templates/login.html`

**Cómo funciona:**
- Spring Security OAuth2 detecta `/login` y sirve página automáticamente
- OAuth2 authorization endpoint redirect → Google callback → Success Handler

**Pros:**
- Spring Security maneja flujo completo automáticamente
- Mínima configuración

**Cons:**
- Dependencia extra

---

## 2026-04-17 — OAuth2 via Gateway 🌐🔐

### 🎯 El Problema
Existía dependencia de `OAuth2User` en servicios de Tenant. Usuarios registrados localmente (JWT) no podían crear empresas.

### 📐 Solución
Se desacopló la capa de servicio de Spring Security:
1. Controladores reciben `Authentication` genérico
2. `TenantServiceImpl` inspecciona el principal:
   - Si `OAuth2User`, extrae `email`
   - Si `UserEntity`, extrae `getEmail()`
   - Fallback a `getName()`

### 🧪 Tests
- 6 nuevos unitarios para extracción de identidad híbrida

---

## 2026-04-13 — Email Verification + HTML 📧

### ✅ Implementado
- **JavaMailSender**: Inyectado en `EmailVerificationServiceImpl`
- **Template HTML**: Email inline con estilos, botón de verificación
- **Flujo**: Email → Frontend `:9000/verify?token=xxx` → Gateway → Auth
- **.env.example**: `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `APP_FRONTEND_URL`

---

## 2026-04-13 — CORS Implementado 🛡️

### ✅ Implementado
- **Gateway**: `globalcors` en `application.yaml` con `${CORS_ALLOWED_ORIGINS}`
- **Auth**: `UrlBasedCorsConfigurationSource` bean en `WebCorsConfig.java`
- **SecurityConfig**: Vinculado `.cors(cors -> cors.configurationSource(...))`
- **Puertos**: Frontend Quasar `:9000`, Gateway `:8080`, Auth `:8081`

---

## 2026-04-12 — RTR + jti + Detección de Reuso 🔄🛡️

### 🎯 El Problema
Refresh Tokens eran estáticos hasta expiración. Un atacante robaba un Refresh Token y podía generar Access Tokens indefinidamente.

### 📐 Arquitectura RTR

1. **Solicitud de Refresco**: Cliente envía `oldRefreshToken`
2. **Validación Atómica**:
   - Verifica firma y expiración del JWT
   - Busca hash en PostgreSQL
   - **Detección de Reuso**: Si `revoked = true`, alarma de seguridad
3. **Estrategia de Mitigación**:
   - Revoca automáticamente todos los tokens del usuario (`deleteByUserId`)
   - Fuerza re-login en todos sus dispositivos
4. **Emisión**: Token viejo marcado `revoked`, nueva pareja (Access + Refresh)

### 💎 Unicidad Criptográfica (`jti`)

**Problema**: Colisión de tokens (dos idénticos en mismo milisegundo)

**Solución**: Claim `jti` con `UUID.randomUUID()` para unicidad absoluta.

### 🧪 Tests
- Detección de reuso: verifica eliminación masiva
- Transaccionalidad: token marcado y nuevo persistido en una operación

---

## 2026-04-11 — Docker Fix 🔧

### ✅ Problema Resuelto
**Testcontainers** `1.20.5` → `1.21.4`
- Docker 29.x requiere API ≥1.44
- Versión antigua usaba `docker-java` con API 1.32

---

## 2026-04-11 — Email Verification Logic 📧

### ✅ Implementado
- **Migración V4**: Columna `email_verified_at` nullable + índice parcial
- **UserEntity**: Campo `emailVerifiedAt` + helpers `isEmailVerified()`, `markEmailAsVerified()`
- **Redis**: Tokens en `email:verify:{token}` → email, TTL 15 min
- **Servicio**: `EmailVerificationService` (generate, verify, resend)
- **Excepciones**: `EmailVerificationTokenInvalidException` (VER002)
- **Códigos**: VER001-VER004
- **Endpoints**: `POST /auth/verify-email`, `POST /auth/resend-verification`
- **Login**: Rechaza si `email_verified_at == null` → `403 FORBIDDEN (VER001)`

### 📊 Tests
- 8 unitarios + 5 integración = 76 tests (0 fallos)

---

## 2026-04-11 — Password Reset Logic 🔐

### ✅ Implementado
- **Redis**: Tokens en `password:reset:{token}` → email, TTL 15 min
- **Servicio**: `PasswordResetService` (generateResetToken, resetPassword)
- **Excepciones**: `PasswordResetTokenInvalidException` (RST001)
- **Códigos**: RST001 (inválido), RST002 (expirado)
- **Endpoints**: `POST /auth/forgot-password`, `POST /auth/reset-password`
- **Timing Attack Prevention**: `POST /forgot-password` siempre retorna 200

### 📊 Tests
- 7 unitarios

---

## 2026-04-09 — Testcontainers Setup 🧪

### ✅ Infraestructura Configurada

**Estructura:**
```
src/test/java/
├── unit/                    # Unit tests (Mockito) → 39 tests
└── integration/             # Integration tests (Testcontainers) → 17 tests
    ├── AbstractIntegrationTest.java  # PostgreSQL + Redis
    ├── AuthApplicationTests.java     # Context load
    └── api/
        └── AuthApiIntegrationTest.java  # Endpoints
```

**Lo que se eliminó:**
- H2 → Eliminado del pom.xml
- `application-test.yaml` → Ya no se usa

**Lo que se agregó:**
- Dependencias: `spring-boot-testcontainers`, `testcontainers:junit-jupiter`, `testcontainers:postgresql`
- Maven Failsafe Plugin → Corre tests de `integration` en `mvn verify`
- Maven Surefire → Excluye `integration` en `mvn test`
- `.testcontainers.properties` → `reuse.enable=true`
- `application-integration.yaml` → Config con Flyway, sin H2
- `AbstractIntegrationTest` → `postgres:15-alpine` + `redis:7-alpine`

---

## 2026-04-16 — Roadmap Completado ✅

### ✅ Completados
- [x] Desacoplamiento total (Auth, User, Tenant, Member, Invitation)
- [x] Estandarización de Mappers (MapStruct)
- [x] Refactor de DTOs
- [x] Unit Tests JWT (100% cobertura)
- [x] Eliminar `JwtTokenProvider`
- [x] Refactor `OAuth2AuthenticationSuccessHandler`
- [x] Validación de Password (regex mínimo 1 letra + 1 número)
- [x] Límite Plan FREE (1 tenant por OWNER)
- [x] Rate Limiting IP + Email
- [x] Testcontainers (PostgreSQL + Redis reales)
- [x] SecurityConfig API-REST
- [x] Refactor `JwtAuthenticationFilter`
- [x] Bug `filterChain.doFilter` tras error
- [x] Refactor `AuthApiController`
- [x] **RTR**: Rotación atómica + detección de reuso
- [x] **jti**: Incorporación para unicidad
- [x] **Data Integrity**: V5 UNIQUE en `token_hash`
- [x] **Flexibilidad TenantService**: JWT/OAuth2

---

*Documentado: 2026-04-17*