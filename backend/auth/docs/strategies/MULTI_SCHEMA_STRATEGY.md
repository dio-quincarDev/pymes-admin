# Multi-Schema Strategy — Microservicios con PostgreSQL compartida

## Contexto

Varios microservicios (`auth`, `core`, etc.) comparten una misma instancia PostgreSQL. Para evitar colisión de tablas, cada microservicio usa su propio schema:

| Microservicio | Schema |
|---------------|--------|
| Auth | `auth` |
| Core | `core` |

## Configuración por microservicio

Cada microservicio debe configurar 3 capas para que todas las operaciones apunten al schema correcto:

### 1. Flyway (migraciones)

`application.yaml`:
```yaml
spring:
  flyway:
    schemas: auth
```

Flyway crea el schema si no existe y ejecuta las migraciones dentro de él.

### 2. JPA/Hibernate (queries ORM)

`application.yaml`:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_schema: auth
```

Sin esto, Hibernate resuelve `@Table(name = "users")` como `public.users` en vez de `auth.users`.

### 3. JDBC directo / JdbcTemplate (queries nativas)

Para código que usa `JdbcTemplate` o SQL nativo, se debe agregar `?currentSchema=auth` a la URL del datasource.

`application-dev.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:pymes_db}?currentSchema=auth
```

## Tests de Integración

Los tests de integración usan Testcontainers. El `@ServiceConnection` de Spring Boot no permite agregar `currentSchema` a la URL, por lo que se debe usar `@DynamicPropertySource`:

```java
@ActiveProfiles("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "?currentSchema=auth");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "auth");
    }
}
```

`application-integration.yaml`:
```yaml
spring:
  flyway:
    schemas: auth
  jpa:
    properties:
      hibernate:
        default_schema: auth
```

## Resumen de configuración

| Archivo | Flyway `schemas` | JPA `default_schema` | URL `currentSchema` |
|---------|------------------|----------------------|---------------------|
| `application.yaml` | ✅ `auth` | ✅ `auth` | ❌ (heredado del perfil activo) |
| `application-dev.yaml` | ❌ (hereda) | ❌ (hereda) | ✅ `auth` |
| `application-integration.yaml` | ✅ `auth` | ✅ `auth` | ✅ vía `@DynamicPropertySource` |
| `application-prod.yaml` / `stg` | ❌ (hereda) | ❌ (hereda) | ✅ según entorno |

## Buenas prácticas

1. **No usar schema en `@Table`** — deja que `hibernate.default_schema` lo resuelva, así el código es portable entre entornos.
2. **`flyway.schemas` siempre en el base** — así el perfil por defecto (dev) ya crea las tablas en el schema correcto.
3. **`currentSchema` solo en perfiles concretos** — cada entorno (dev, stg, prod, integration) define su propia URL.
4. **Un schema por microservicio** — evita prefijos en nombres de tablas y mantiene el aislamiento limpio.
5. **Misma versión de PostgreSQL** — todos los microservicios deben usar la misma versión mayor para evitar incompatibilidades de migration.
