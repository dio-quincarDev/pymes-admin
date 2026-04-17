# Estrategia de Testcontainers

## Contexto

Los microservicios del backend (auth, gateway-pymes) necesitan pruebas de integración contra servicios reales (PostgreSQL, Redis). Actualmente los tests del auth service usan H2, lo que no valida migraciones Flyway ni comportamiento real de PostgreSQL.

## Objetivo

Usar Testcontainers para levantar PostgreSQL y Redis reales durante las pruebas de integración, garantizando que el código funciona contra los mismos servicios que se usan en producción.

## Convención de nombres

- `*Test.java` → tests unitarios, sin containers
- `*IT.java` → tests de integración, con containers

## Estructura de tests

```
src/test/java/
├── unit/                    # Tests existentes, no cambian
│   └── *Test.java
└── integration/
    ├── repository/
    │   └── *IT.java         # Repositorios contra PostgreSQL real
    ├── service/
    │   └── *IT.java         # Servicios con PostgreSQL + Redis
    └── api/
        └── *IT.java         # Endpoints completos con security
```

## Clase base reutilizable

Una clase `AbstractIntegrationTest` levanta los containers e inyecta las propiedades:

- PostgreSQL 15-alpine
- Redis 7-alpine
- Flyway habilitado
- Propiedades dinámicas via `@DynamicPropertySource`

Todos los `*IT.java` heredan de esta clase.

## Fase 1: Auth Service

1. Agregar dependencias al `pom.xml`:
   - `spring-boot-starter-testcontainers`
   - `testcontainers:junit-jupiter`
   - `postgresql`
   - Eliminar H2

2. Crear `AbstractIntegrationTest`

3. Crear tests de integración por capa:
   - Repositorios → validar queries y migraciones
   - Servicios → validar lógica con datos reales
   - API → validar endpoints con security chain

4. Configurar Maven Surefire:
   - `mvn test` → solo `*Test.java`
   - `mvn verify` → incluye `*IT.java`

## Fase 2: Gateway

1. Agregar dependencias testcontainers
2. Levantar auth-service como contenedor o mockear respuestas
3. Tests de routing, JWT validation, error handling

## Fase 3: CI/CD

- Job paralelo por microservicio en GitHub Actions
- Cada job levanta solo los containers que necesita
- Containers efímeros, sin reuse en CI
- Limpiar recursos post-ejecución

## Gestión de recursos

| Entorno | Estrategia |
|---|---|
| Desarrollo | `reuse=true` en `~/.testcontainers.properties` para reutilizar containers entre ejecuciones |
| CI/CD | Containers efímeros, se crean y destruyen por job |
| Imágenes | Mismas tags que producción: `postgres:15-alpine`, `redis:7-alpine` |
