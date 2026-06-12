# ✦ Feedback Crudo — Auth Microservice (Re-evaluación 2026-06-10)

---

## 🟢 Lo Bueno (Estado del Arte)

- **Refresh Token Rotation (RTR) con Detección de Reuso**
  Lógica robusta de rotación atómica e invalidación masiva de la familia de tokens ante sospecha de reuso. Es el pilar más sólido de la seguridad de sesión actual.

- **Unicidad Criptográfica (`jti`)**
  Mitigación de colisiones de tokens mediante identificador único (`jti`), garantizando estabilidad bajo alta concurrencia.

- **Arquitectura limpia por dominios**
  Separación clara de responsabilidades: Auth, User, Tenant, Member, e Invitations. El código es altamente legible y modular.

- **Soft delete + logs de auditoría**
  Implementación funcional de `deleted_at` mediante anotaciones de Hibernate y rastreo de acciones de login/registro para auditorías.

---

## 🔴 Lo Malo (Deuda Técnica y Agujeros Críticos)

- **Falsa Mitigación de Timing Attacks en Password Reset**
  El código en [PasswordResetServiceImpl.java](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/src/main/java/auth/pymes/service/impl/PasswordResetServiceImpl.java#L45-L49) tiene un `return false` inmediato si el usuario no existe. Si el usuario existe, ejecuta generación de tokens, Redis y un envío de correo (que en producción tarda cientos de milisegundos). Esto crea una vulnerabilidad crítica que permite enumerar emails de la base de datos a través del tiempo de respuesta del endpoint `/forgot-password`.

- **Crash (500 Error) en Login de Google con Cuenta Existente**
  En [CustomOAuth2UserService.java](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/src/main/java/auth/pymes/service/impl/CustomOAuth2UserService.java#L37-L48), si un usuario se registró de forma `LOCAL` y luego intenta iniciar sesión con `GOOGLE` usando el mismo correo, la aplicación intenta insertar un nuevo registro con el mismo email debido a que la consulta por proveedor y ID retorna vacía. Esto viola la restricción `UNIQUE` de la tabla de usuarios, provocando una caída con error 500 (`DataIntegrityViolationException`) en lugar de enlazar cuentas o controlar el error.

- **Tests "Falsos" en la Suite de Integración**
  - El test `registerDuplicateEmail` en [AuthApiIntegrationTest.java](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/src/test/java/auth/pymes/integration/api/AuthApiIntegrationTest.java#L97-L109) aserta `200 OK` para ambas peticiones consecutivas a pesar de que su display name dice esperar un `409 Conflict`. Esto oculta que el endpoint de registro permite contaminar Redis con múltiples registros pendientes concurrentes para el mismo email.
  - El test `forgotPasswordInvalidEmailFormats` aserta `400 Bad Request` pero su display name afirma esperar un `200 OK`.

- **Bypass Potencial de Filtro de Seguridad**
  [JwtAuthenticationFilter.java](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/src/main/java/auth/pymes/common/config/JwtAuthenticationFilter.java#L43-L49) utiliza `path.contains(...)` para omitir la validación de tokens. Es un antipatrón de seguridad. Si una ruta protegida contiene un subsegmento como `/login` (ej: `/api/v1/tenants/login/settings`), omitirá la verificación del token en este filtro.

- **Condición de Carrera en Rate Limiting (Bloqueo Permanente)**
  En [RateLimitService.java](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/src/main/java/auth/pymes/common/config/RateLimitService.java#L28-L43), si la petición incrementa la clave de Redis a 1 pero el servidor sufre una interrupción o latencia antes de llamar a `expire(...)`, la clave se guardará en Redis **sin tiempo de vida (TTL)**, bloqueando indefinidamente a ese usuario/IP. Además, el limitador es de ventana fija (no sliding window) y no protege los endpoints de registro o recuperación de contraseña.

- **Ruido en Logs de Producción**
  Capturar la expiración normal de tokens (`ExpiredJwtException`) y registrarla a nivel `ERROR` genera ruido innecesario en los sistemas de monitoreo y logs de producción.

---

## 🟡 Lo que se Arregló (Puntos superados del reporte anterior)

- **CORS funcional**
  Implementado correctamente como un Spring Bean (`CorsConfigurationSource`) en `SecurityConfig`.
- **Integración de Email Real**
  Migrado del simple log a envío real de correos mediante HTML y el bean de `JavaMailSender`.

---

## 📊 Score por Dimensión

| Dimensión | Anterior | Actual | Razón del cambio |
|---|---|---|---|
| **Arquitectura** | 10/10 | **9/10** | Degradado levemente por el bypass inseguro de `path.contains` en el filtro de seguridad. |
| **Seguridad de Sesión / Auth** | 10/10 | **6/10** | Caída drástica por la timing attack en password reset, la ausencia de control en duplicados de OAuth2 y el riesgo de bloqueo permanente en Redis. |
| **Seguridad de Perímetro** | 9/10 | **8/10** | Falta rate limiting en endpoints costosos de registro y recuperación de contraseña. |
| **Testing** | 10/10 | **7/10** | Presencia de tests mal diseñados con aserciones dummy (ej. registro duplicado que da 200 pero afirma esperar 409). |
| **DevOps** | 9/10 | **9/10** | CI/CD funcional con GitHub Actions. |
| **Production Readiness** | 9.5/10 | **8/10** | Los errores de duplicidad en inicio con Google y de lógica en rate limits comprometen la estabilidad en producción. |

**Promedio General: 7.8/10** (Bajó de 9.5/10 por descubrimientos de lógica crítica y fallos en tests)

---

## ⚡ Prioridades Reales (Sin complacencias)

| # | Acción | Impacto | Esfuerzo |
|---|--------|---------|----------|
| 1 | **Corregir timing attack en Password Reset** | 🔴 Crítico (Seguridad) | Bajo (1 hr) |
| 2 | **Implementar fusión / account linking en OAuth2** | 🔴 Crítico (Estabilidad) | Medio (3 hrs) |
| 3 | **Hacer atómico el seteo de TTL en RateLimitService** | 🔴 Crítico (Disponibilidad) | Bajo (1 hr) |
| 4 | **Corregir lógica del test de registro duplicado y assertions** | 🟡 Importante (Calidad) | Medio (2 hrs) |
| 5 | **Eliminar bypass laxo de `path.contains` en Jwt Filter** | 🟡 Importante (Seguridad) | Bajo (1 hr) |
| 6 | **Reducir logging de `ExpiredJwtException` a nivel DEBUG** | 🟢 Mantenimiento | Bajo (10 min) |
