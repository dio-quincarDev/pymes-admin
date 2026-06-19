# 📚 Centro de Documentación — Auth Service

Bienvenido al centro de documentación técnica del microservicio de autenticación y multi-tenancy (`auth`) para la plataforma SaaS PyMes Admin.

---

## 🗂️ Estructura del Directorio

El directorio de documentación se organiza de la siguiente manera:

* **[README.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/README.md)**: Este archivo (índice y mapa del directorio).
* **[DAILY_REPORTS_AUTH_SOLUTIONS.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/DAILY_REPORTS_AUTH_SOLUTIONS.md)**: Historial completo de soluciones aplicadas y roadmap cronológico.
* **[CLASS_REFERENCE.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/CLASS_REFERENCE.md)**: Referencia consolidada de diseño de clases, estructuras de caché en Redis, filtros y rotación de tokens.
* **[`strategies/`](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/strategies)**: Directorio con propuestas de diseño y especificaciones arquitectónicas:
  - **[CONSISTENCY_STRATEGY.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/strategies/CONSISTENCY_STRATEGY.md)**: Arquitectura de consistencia de rutas de API.
  - **[LOCAL_AUTH_STRATEGY.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/strategies/LOCAL_AUTH_STRATEGY.md)**: Flujos de registro, login y jerarquías.
  - **[MULTI_SCHEMA_STRATEGY.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/strategies/MULTI_SCHEMA_STRATEGY.md)**: Configuración del aislamiento de datos por esquema.
  - **[STRATEGY_OAUTH2_TENANT.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/strategies/STRATEGY_OAUTH2_TENANT.md)**: Vinculación de inquilinos en logins federados mediante `state`.
  - **[THYMELEAF_EMAIL_SYSTEM.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/strategies/THYMELEAF_EMAIL_SYSTEM.md)**: Plantillas y diseño responsive de emails.
  - **[VERIFICATION_SECURITY_FIX.md](file:///home/dio/desarrollo/side-projects/pymes-admin/backend/auth/docs/strategies/VERIFICATION_SECURITY_FIX.md)**: Detalles sobre el fix contra la vulnerabilidad de mismatch de token-email.

---

## 🛠️ Ejecución y Pruebas Rápidas

Para validar la correcta configuración de cualquiera de estos componentes descritos:

* **Pruebas unitarias** (sin Docker / H2):
  ```bash
  ./mvnw test -B -Dspring.profiles.active=test
  ```
* **Pruebas de integración** (requiere Docker / Testcontainers):
  ```bash
  ./mvnw verify -B -Dspring.profiles.active=integration
  ```
