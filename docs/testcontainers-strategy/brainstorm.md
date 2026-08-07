# Estrategia de Testcontainers

## Contexto

Los microservicios del backend (auth, gateway-pymes) necesitan pruebas de integracion contra servicios reales (PostgreSQL, Redis). Los tests del auth service usan Testcontainers con PostgreSQL y Redis reales.

## Objetivo

Usar Testcontainers para levantar PostgreSQL y Redis reales durante las pruebas de integracion, garantizando que el codigo funciona contra los mismos servicios que se usan en produccion.

## Convencion de nombres

- `*Test.java` → tests unitarios, sin containers
- `*IntegrationTest.java` → tests de integracion, con containers

## Estructura de tests

```
src/test/java/
├── unit/                    # Tests existentes, no cambian
│   └── *Test.java
└── integration/
    ├── AbstractIntegrationTest.java   # Clase base con containers
    ├── repository/
    │   └── *IntegrationTest.java      # Repositorios contra PostgreSQL real
    ├── service/
    │   └── *IntegrationTest.java      # Servicios con PostgreSQL + Redis
    └── api/
        └── *IntegrationTest.java      # Endpoints completos con security
```

## Clase base reutilizable

`AbstractIntegrationTest` levanta los containers e inyecta las propiedades:

- PostgreSQL 15-alpine
- Redis 7-alpine
- Flyway habilitado (schema: `auth`)
- Propiedades dinamicas via `@DynamicPropertySource`
- `@MockitoBean` para EmailService (evita enviar emails reales)

Todos los `*IntegrationTest.java` heredan de esta clase.

## Fase 1: Auth Service ✅ Completa

1. Dependencias en `pom.xml`:
   - `spring-boot-starter-testcontainers`
   - `testcontainers:junit-jupiter`
   - `postgresql`
   - H2 eliminado

2. `AbstractIntegrationTest` creado en `auth.pymes.integration`

3. Tests de integracion por capa:
   - `AuthApiIntegrationTest` — endpoints con security chain
   - `OAuth2LoginIntegrationTest` — flujo OAuth2
   - `OAuth2IntentIntegrationTest` — intent cookies
   - `InvitationServiceIntegrationTest` — invitaciones
   - `PasswordResetIntegrationTest` — reset password
   - `SecurityConstraintIntegrationTest` — restricciones de seguridad

4. Maven configurado:
   - `mvn test` → solo `*Test.java` (Surefire excluye `**/integration/**`)
   - `mvn verify` → incluye `*IntegrationTest.java` (Failsafe)

## Fase 2: Gateway

1. Agregar dependencias testcontainers
2. Levantar auth-service como contenedor o mockear respuestas
3. Tests de routing, JWT validation, error handling

## Fase 3: CI/CD

- Job paralelo por microservicio en GitHub Actions
- Cada job levanta solo los containers que necesita
- Containers efimeros, sin reuse en CI
- Limpiar recursos post-ejecucion

## Gestion de recursos

| Entorno | Estrategia |
|---|---|
| Desarrollo | `reuse=true` en `~/.testcontainers.properties` para reutilizar containers entre ejecuciones |
| CI/CD | Containers efimeros, se crean y destruyen por job |
| Imagenes | Mismas tags que produccion: `postgres:15-alpine`, `redis:7-alpine` |
