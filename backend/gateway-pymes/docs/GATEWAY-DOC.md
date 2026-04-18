# Documentación Técnica del API Gateway

Este documento detalla problemas críticos corregidos y decisiones de implementación en el microservicio **gateway-pymes**.

---

## 🛠️ Correcciones Técnicas

### 1. Gestión de Dependencias (Lombok/SLF4J)
- **Problema:** Errores de compilación en `AuthenticationFilter.java`. El compilador de Java no encontraba los símbolos generados por la anotación `@Slf4j`.
- **Causa:** El proyecto utilizaba anotaciones de **Lombok** para abstraer la instanciación de loggers, pero la dependencia no estaba declarada en el `pom.xml`.
- **Solución:** Se integró la dependencia `org.projectlombok:lombok` como dependencia opcional (`<optional>true</optional>`). Esto permite al procesador de anotaciones (`javac`) inyectar el código del logger (`private static final org.slf4j.Logger log = ...`) durante la fase de compilación.

### 2. Seguridad JWT y Estándar JWA (RFC 7518)
- **Problema:** Fallo en la carga del `ApplicationContext` en las pruebas unitarias debido a una `WeakKeyException`.
- **Causa Técnica:** El secreto JWT utilizado tenía una longitud de **240 bits**. De acuerdo con el **RFC 7518 (sección 3.2)**, los algoritmos HMAC-SHA (como HS256) **DEBEN** utilizar claves con un tamaño mayor o igual al tamaño de salida del hash ($\ge$ 256 bits o 32 bytes) para garantizar la seguridad criptográfica.
- **Implementación Correctiva:**
    - Se configuró `src/test/resources/application-test.yaml` con un secreto seguro generado aleatoriamente de **256 bits**.
    - Se activó el perfil `test` en la clase de prueba principal `GatewayPymesApplicationTests` mediante la anotación `@ActiveProfiles("test")`.
    - Esto asegura que las pruebas se ejecuten en un entorno con configuraciones de seguridad robustas que cumplan con la validación estricta de la librería `jjwt-api`.

### 3. Sincronización de Rutas y Consistencia API (2026-04-12)
- **Problema:** El Gateway bloqueaba por defecto los nuevos flujos de **Verificación de Email** y **Recuperación de Contraseña**, ya que no estaban definidos en la ruta `auth-public`.
- **Solución:** Se sincronizó el archivo `application.yaml` con las constantes definidas en `ApiPathConstants.java` del microservicio de Auth.
- **Endpoints Añadidos a `auth-public`:**
    - `/api/v1/auth/verify-email`
    - `/api/v1/auth/resend-verification`
    - `/api/v1/auth/forgot-password`
    - `/api/v1/auth/reset-password`
- **Impacto:** Estos endpoints ahora son accesibles desde el exterior a través del Gateway (puerto 8080) sin requerir autenticación previa, permitiendo el flujo completo de onboarding y soporte al usuario.

### 4. Resolución de URISyntaxException en CI (2026-04-12)
- **Problema:** El pipeline de GitHub Actions y los tests locales fallaban con una `URISyntaxException: Expected scheme-specific part` al intentar levantar el contexto de Spring.
- **Causa:** Las rutas del Gateway en `application.yaml` utilizan variables de entorno como `${AUTH_SERVICE_HOST}`. Al ejecutar los tests con el perfil `test`, estas variables no estaban definidas, resultando en una URI malformada (`http::8081`).
- **Solución:** Se actualizaron los recursos de prueba (`src/test/resources/application-test.yaml`) incluyendo placeholders por defecto para todas las variables de infraestructura:
    - `AUTH_SERVICE_HOST_TEST: localhost`
    - `REDIS_HOST_TEST: localhost`
- **Resultado:** El comando `./mvnw verify` ahora completa el build con éxito (**BUILD SUCCESS**), garantizando que el CI sea estable e independiente del entorno de ejecución.

---

### 5. Configuración CORS (2026-04-13)
- **Problema:** El CORS estaba configurado en `application.yaml` del Gateway pero sin archivo `.env.example` que documentara las variables de entorno necesarias. Además, el microservicio Auth tenía un `WebCorsConfig` con strings vacíos y `@EnableWebMvc` que causaba conflictos.
- **Solución en Gateway:**
    - CORS ya configurado via `globalcors` en `application.yaml`:
      ```yaml
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: ${CORS_ALLOWED_ORIGINS:"*"}
            allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
            allowedHeaders: "*"
      ```
    - Se creó `.env.example` con las variables documentadas: `SERVER_PORT=8080`, `CORS_ALLOWED_ORIGINS=http://localhost:9000`, `REDIS_HOST`, `JWT_SECRET`, `AUTH_SERVICE_HOST`.
- **Solución en Auth (defensa en profundidad):**
    - Se eliminó `@EnableWebMvc` y `addCorsMappings` con valores vacíos.
    - Se creó un `UrlBasedCorsConfigurationSource` bean en `WebCorsConfig.java` que lee `${app.cors.allowed-origins}`.
    - Se vinculó al `SecurityFilterChain` en `SecurityConfig.java`.
- **Arquitectura CORS resultante:**
  ```
  Frontend (:9000) → [CORS Gateway: permite origins] → Gateway:8080 → [HTTP interno, sin CORS] → Auth:8081
                                                              ↑
                                                        CORS principal (funciona)

                                                    Si acceden Auth directo:
                                                    CORS de defensa en profundidad (bloquea)
  ```
- **Puertos definidos:** Frontend Quasar `9000`, Gateway `8080`, Auth `8081`.

---

### 6. Verificación de Email — Envío Real (2026-04-13)
- **Problema:** El flujo de verificación de email existía solo en lógica interna. `createAndSendVerificationEmail()` solo hacía log — no enviaba emails. El `spring-boot-starter-mail` estaba en `pom.xml` pero nunca se usaba `JavaMailSender`.
- **Causa:** Faltaba integración con el servicio de mail de Spring y la configuración YAML usaba el prefix incorrecto (`mail.*` en vez de `spring.mail.*`).
- **Solución:**
    - Corregido `application.yaml`: `mail.*` → `spring.mail.*` (prefix correcto para Spring Boot).
    - Agregada propiedad `app.frontend.url` para construir URLs de verificación en emails.
    - `EmailVerificationServiceImpl` ahora inyecta `JavaMailSender` y envía emails HTML reales con:
      - Template HTML inline con estilos (botón de verificación, branding).
      - Token almacenado en Redis con TTL de 15 minutos.
      - Manejo de errores con `MessagingException`.
    - `AuthServiceImpl.register()` ahora llama a `createAndSendVerificationEmail()` en lugar de solo generar el token en Redis.
    - `.env.example` actualizado con `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `APP_FRONTEND_URL`.
- **Flujo completo resultante:**
  ```
  POST /api/v1/auth/register → Auth crea usuario + tenant
    → Genera token Redis (email:verify:xxx, TTL 15min)
    → Envía email HTML al usuario con link: http://localhost:9000/verify?token=xxx
    → Usuario click → Frontend Quasar muestra UI
    → Frontend → POST http://localhost:8080/api/v1/auth/verify-email → Gateway → Auth
    → Auth valida token en Redis → marca email como verificado → 200 OK
  ```

---

### 7. Agregación Unificada de Swagger UI (2026-04-13)
- **Problema:** Con múltiples microservicios, cada uno tendría su propio Swagger UI en puertos internos no accesibles. No existía un punto centralizado para consultar la documentación de todos los servicios.
- **Solución:** Se implementó una estrategia de **agregación unificada** donde el Gateway actúa como único punto de entrada para Swagger UI, con un dropdown que permite cambiar entre servicios.

---

#### 📐 Arquitectura Swagger Agregada

```
Navegador → Gateway (:8080) → Swagger UI con dropdown:
  ├── Auth Service       (/v3/api-docs/auth → auth-service:8081)
  ├── Payment Service    (/v3/api-docs/payment → payment-service:8082) ← futuro
  └── Notification Svc   (/v3/api-docs/notify → notification-service)  ← futuro
```

**Endpoints funcionales (Punto Único de Acceso):**

| Tipo de Acceso | Endpoint (URL) | Descripción |
|---|---|---|
| **Interfaz Visual (UI)** | `http://localhost:8080/swagger-ui.html` | **Recomendado:** Alias de redirección fácil de recordar. |
| **Path Real (UI)** | `http://localhost:8080/webjars/swagger-ui/index.html` | Ruta física donde reside el dashboard en el JAR de Spring. |
| **Especificación (JSON)** | `http://localhost:8080/v3/api-docs/auth` | JSON puro de OpenAPI del Auth Service. |
| **Configuración UI** | `http://localhost:8080/v3/api-docs/swagger-config` | Configuración interna que carga el dropdown de servicios. |

---

#### 💡 Nota Técnica sobre Rutas en el Gateway (WebFlux)

Es importante notar que en el Gateway (WebFlux), a diferencia de los microservicios tradicionales (WebMVC), la ruta `/swagger-ui/index.html` **no funciona** y devolverá un **404**.

**¿Por qué?**
*   **`/webjars/...`**: Es la ruta física real donde Spring Boot sirve los archivos estáticos de la librería Swagger desde el classpath.
*   **`/swagger-ui.html`**: Es un alias (redirección 302) configurado explícitamente en `application.yaml` para facilitar el acceso.
*   **`/swagger-ui/index.html`**: No existe físicamente ni tiene un mapeo automático en WebFlux, por lo que el Gateway no sabe cómo resolverla.

**Regla de Oro:** Usa siempre `/swagger-ui.html` para acceder visualmente o las rutas `/v3/api-docs/*` para obtener los JSON de especificación.

---

#### 🚀 Guía Paso a Paso: Agregar un Nuevo Microservicio

Para integrar tanto el JSON como la visualización en el UI de un nuevo microservicio (ej. `payment-service`), sigue estos pasos:

**Paso 1: Configurar el Microservicio (`payment-service`)**
En su `application.yaml`, define el path único para su documentación:
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs/payment  # Debe ser único
  server:
    url: http://localhost:8080  # Apunta siempre al Gateway
```
Asegúrate de añadir `/v3/api-docs/**` a la lista blanca de seguridad del microservicio.

**Paso 2: Registrar la Ruta en el Gateway**
En `backend/gateway-pymes/src/main/resources/application.yaml` (y en sus perfiles `-dev`, `-prod`, etc.), añade la ruta de proxy:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: swagger-api-docs-payment
          uri: http://${PAYMENT_SERVICE_HOST}:8082
          predicates:
            - Path=/v3/api-docs/payment
```

**Paso 3: Registrar el Servicio en el Agregador**
En `backend/gateway-pymes/src/main/java/dev/dioquincar/gateway_pymes/config/SwaggerAggregatorConfig.java`, añade el servicio al Set de URLs:
```java
@PostConstruct
public void init() {
    swaggerUiConfig.setUrls(Set.of(
            new SwaggerUrl("auth", "/v3/api-docs/auth", "Auth Service"),
            new SwaggerUrl("payment", "/v3/api-docs/payment", "Payment Service") // Nueva entrada
    ));
}
```

---

#### ⚙️ Implementación Técnica del Gateway

**Gateway (`application.yaml`):**
```yaml
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html  # Forzamos path para evitar 404 de redirección
    config-url: /v3/api-docs/swagger-config
```

---

#### 📋 Resumen de Convenciones
| Elemento | Convención | Ejemplo |
|---|---|---|
| API Docs path | `/v3/api-docs/{nombre-servicio}` | `/v3/api-docs/payment` |
| Route ID | `swagger-api-docs-{nombre}` | `swagger-api-docs-payment` |
| SwaggerUrl ID | `{nombre}` (minúsculas) | `payment` |
| SwaggerUrl Name | `{Nombre} Service` | `Payment Service` |

---

#### ⚠️ Problemas Conocidos y Soluciones

**1. `/swagger-ui.html` → 404 en el navegador**
- **Causa:** Redirección automática inconsistente en entornos Docker/WebFlux.
- **Solución:** Se definió explícitamente `springdoc.swagger-ui.path: /swagger-ui.html` en el `application.yaml` del Gateway. Esto garantiza que el navegador siempre encuentre el punto de entrada y lo redirija correctamente a `/webjars/swagger-ui/index.html`.

**2. Error de Conexión (Connection Refused)**
- **Causa:** El Gateway intenta resolver el host del microservicio fuera de la red de Docker.
- **Solución:** Asegurar que el Gateway y los microservicios compartan la misma red interna (`pymes-internal-network`) y que el Gateway use el nombre del servicio de Docker (ej. `pymes-auth-service`) como host en sus rutas.

**2. `@Bean SwaggerUiConfigProperties` crea conflicto**
- **Causa:** Springdoc ya registra un bean de este tipo → `NoUniqueBeanDefinitionException`
- **Solución:** Usar `@Component` + `@PostConstruct` inyectando el bean existente (`SwaggerAggregatorConfig.java`)

**3. Perfil dev/stg/prod reemplaza rutas del `application.yaml` base**
- **Causa:** Spring Cloud Gateway no mergea listas de rutas entre perfiles
- **Solución:** Definir rutas de Swagger en **cada** archivo de perfil

**4. Swagger UI dropdown vacío**
- **Causa:** `springdoc.swagger-ui.urls` en YAML no se bindea en WebFlux
- **Solución:** Configurar URLs programáticamente en `SwaggerAggregatorConfig.java`

**5. Producción - Swagger no debería ser público**
- **Solución:** `SWAGGER_ENABLED=false` en el `.env` de cada servicio
  O deshabilitar en producción via perfil Spring:
  ```yaml
  springdoc:
    swagger-ui:
      enabled: false
    api-docs:
      enabled: false
  ```
---

### 9. Configuración CORS - Problema "Invalid CORS request" (2026-04-16)

**Estado:** ❌ PENDIENTE - Necesita solución

#### Problema Actual

| Petición | Resultado | Headers CORS |
|----------|----------|-------------|
| **OPTIONS** (preflight) | 200 OK | ✅ Enviados |
| **POST** (registro) | 403 Forbidden | ❌ Bloqueado con "Invalid CORS request" |

#### Síntomas

```
HTTP/1.1 403 Forbidden
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: *
...
Invalid CORS request
```

#### Intentos de Solución Realizados

| # | Solución Intentada | Ubicación | Resultado |
|---|------------------|-----------|----------|
| 1 | Custom CorsWebFilter (AbstractGatewayFilterFactory) | `gateway-pymes/filter/CorsWebFilter.java` | ❌ No se ejecuta - llega OPTIONS al backend |
| 2 | Custom CorsGlobalFilter (GlobalFilter + @Order(HIGHEST_PRECEDENCE)) | `gateway-pymes/filter/CorsGlobalFilter.java` | ❌ No se ejecuta - processor interno de Spring lo intercepta antes |
| 3 | Custom WebFilter (@Order(Ordered.HIGHEST_PRECEDENCE)) | `gateway-pymes/filter/CorsGlobalFilter.java` | ⚠️ OPTIONS = 200 OK, POST = 403 |
| 4 | globalcors con allowedOrigins específicos | `application.yaml` | ❌ 403 "Invalid CORS request" |
| 5 | globalcors con allowedOrigins: "\*" | `application.yaml` | ❌ 403 "Invalid CORS request" |
| 6 | globalcors deshabilitado | `application.yaml` | ❌ OPTIONS llega al backend sin headers CORS |
| 7 | Handler HttpRequestMethodNotSupportedException | `GlobalExceptionHandler.java` (auth) | ✅ Agregado pero no resuelve CORS |
| 8 | Corrección variable JWT_SECRET | `.env` + `docker-compose.yml` | ✅ Resuelve error de inicio |

#### Archivos Modificados Durante la Investigación

- `backend/auth/.../GlobalExceptionHandler.java` - Agregado handler para `HttpRequestMethodNotSupportedException`
- `backend/auth/.../CodigoError.java` - Agregado código `METHOD_NOT_ALLOWED` (RSC002)
- `.env` - Actualizado con `CORS_ALLOWED_ORIGINS=http://localhost:9200`, `jwt.secret`
- `docker-compose.yml` - Agregado `JWT_SECRET` para gateway
- `gateway-pymes/application.yaml` - globalcors configurado, comentado y variado múltiples veces

#### Causa Raíz Identificada (Investigación)

- **Bug conocido en Spring Cloud Gateway 3.2.0+**: El procesador interno de CORS de Spring Web intercepta las peticiones CORS antes de que `globalcors` pueda procesarlas correctamente.
- Los headers `Vary: Origin` en la respuesta indican que el processor interno de Spring está interfiriendo.
- Referencias: GitHub Issues #31839, #1690, #2472, #3435 de Spring Cloud Gateway

#### Soluciones a Investigar Próximas

1. Agregar `default-filters: DedupeResponseHeader` para evitar duplicación de headers CORS
2. Configurar CORS en Security del Gateway explícitamente (WebFilter + SecurityWebFilterChain)
3. Deshabilitar CORS en microservicio Auth completamente (dejar que solo el gateway maneje CORS)
4. Usar `allowedOriginPatterns` en lugar de `allowedOrigins`

---

*Última actualización: 16 de Abril, 2026*

---

### 10. OAuth2 via Gateway - Rutas para Social Login (2026-04-17)

**Problema:** Las rutas OAuth2 (`/oauth2/**`) no estaban definidas en los perfiles del Gateway, causando 404 al intentar login con Google/Facebook desde el frontend.

**Causa:** Spring Cloud Gateway no mergea listas de rutas entre el `application.yaml` base y los perfiles (dev/stg/prod). Las rutas del perfil sobrescriben completamente las del base.

**Solución implementada:**

En `application-dev.yaml`, `application-stg.yaml`, `application-prod.yaml`:
```yaml
routes:
  - id: auth-service-oauth2
    uri: http://${AUTH_SERVICE_HOST:localhost}:8081
    predicates:
      - Path=/oauth2/**, /login/oauth2/**, /login/**, /v3/api-docs/auth
    filters:
      - PreserveHostHeader
```

**Validación requerida en consoles de desarrolladores:**

| Proveedor | Redirect URI |
|-----------|--------------|
| **Google** | `http://localhost:8080/login/oauth2/code/google` |
| **Facebook** | `http://localhost:8080/login/oauth2/code/facebook` |

**Flujo resultante:**
```
Frontend (:9200) 
  → Gateway (:8080/oauth2/authorization/google) 
    → Auth Service (8081) 
      → Google OAuth 
        → Redirect a :8080/login/oauth2/code/google
```

**Variables de entorno relacionadas:**
```env
# En backend/auth/.env
OAUTH2_REDIRECT_URI=http://localhost:8080
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

---

**Estado:** ✅ Google funcionando | ⏳ Facebook pendiente

*Última actualización: 17 de Abril, 2026*
