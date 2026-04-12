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
*Última actualización: 12 de Abril, 2026*
