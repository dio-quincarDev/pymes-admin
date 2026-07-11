# Persistencia PostgreSQL — Referencia Tecnica

## Arquitectura

Instancia unica de PostgreSQL 15 (`postgres:15-alpine`), base de datos `pymes_db`, con dos esquemas logicos:

| Esquema | Servicio | Puerto | Tablas |
|---------|----------|--------|--------|
| `auth` | auth (8081) | 8081 | 6: users, tenants, user_tenants, invitations, refresh_tokens, audit_log |
| `core` | core (8082) | 8082 | 14+: products, invoices, providers, expense_analysis, loans, etc. |

Gateway (8080, WebFlux) no tiene persistencia — solo Redis para lista negra de JWT.

## Conexion

- **Host:** `localhost` (dev) / `pymes-postgres-db` (Docker)
- **Puerto:** `5432` | **Base de datos:** `pymes_db`
- **Formato JDBC:** `jdbc:postgresql://${host}:${port}/pymes_db?currentSchema=${schema}`
- **Credenciales:** `${DB_USERNAME}` / `${DB_PASSWORD}` — externalizadas via variables de entorno.
- **Sin SSL** (`sslmode=require` no configurado en ningun perfil).

## Resolucion de Esquema (3 capas redundantes)

1. **JDBC `?currentSchema=`** a nivel de conexion
2. **Hibernate `default_schema`** en `application.yaml`
3. **Flyway `schemas`** — cada servicio migra solo su propio esquema
4. **Entidades de core ademas usan `@Table(schema = "core")`** explicitamente

## Flyway

| Servicio | Archivos | `baseline-on-migrate` | Estado |
|----------|----------|-----------------------|--------|
| auth | 2 (`V1__initial_schema.sql`, `V2__index_optimizations.sql`) | true | Estable |
| core | 13 (`V1`..`V13`) | true | Evolucion activa |

`baseline-on-migrate: true` implica que V1 siempre se asume como baseline. No hay `baseline-version` personalizado.

## JPA / Hibernate

- `ddl-auto: none` (Flyway gestiona el esquema)
- `hibernate.dialect: PostgreSQLDialect`
- HikariCP con `maximum-pool-size: 20` (configurado en auth y core). Core usa virtual threads (`spring.threads.virtual.enabled: true`) — pool 20 mitiga contención de carrier threads bajo carga concurrente.

## Multi-Inquilino (Multi-Tenancy)

- **Modelo de columna discriminadora** (nivel de fila), no esquema por inquilino.
- Toda tabla de `core` tiene `tenant_id UUID NOT NULL`. Sin restriccion FK a `auth.tenants` (esquemas cruzados).
- Validacion de inquilino en capa de aplicacion via claims del JWT + contexto de seguridad.
- Esquema auth: tabla `user_tenants` con roles (OWNER, ADMIN, CONTABLE, VIEWER).

## Dependencias

- `spring-boot-starter-data-jpa`, `postgresql` (runtime), `flyway-core`, `flyway-database-postgresql`
- `testcontainers-postgresql` para pruebas de integracion (auth y core)
- Spring Boot: auth `3.4.3`, core `3.5.14` — Java 21.

## Observaciones Criticas

1. **Sin ajuste explicito de HikariCP** — 10 conexiones por defecto compartidas entre todos los inquilinos. Bajo carga, una consulta lenta puede agotar el pool. Agregar `spring.datasource.hikari.maximum-pool-size` si aparece contencion de conexiones.
2. **Sin cifrado en la conexion** — `sslmode=require` ausente. Aceptable en red interna de Docker; obligatorio para despliegues con acceso publico.
3. **Virtual threads + JDBC bloqueante** — core habilita virtual threads pero usa JDBC (I/O bloqueante). HikariCP debe anclar virtual threads a carrier threads durante llamadas a DB. Monitorear posible inanicion de carrier threads bajo carga concurrente; considerar aumentar `spring.datasource.hikari.maximum-pool-size`.
4. **Sin FK entre esquemas** — `tenant_id` en tablas de core no tiene FK a `auth.tenants`. Solo validacion en capa de aplicacion. Posibles registros huerfanos si se elimina un inquilino sin limpieza en cascada.
5. **Riesgo de versionado Flyway** — auth y core usan secuencias de version independientes. Despues de merges, si una migracion agrega V2 a auth y V14 a core simultaneamente, ambas deben llegar sin conflicto. Verificar `db/migration/` tras cada merge.
6. **Mismas credenciales, ambos esquemas** — auth y core comparten usuario de BD. Un compromiso o bug en cualquiera de los servicios puede leer/escribir el esquema del otro (mitigado por aislamiento de esquema, pero no por autenticacion). Considerar usuarios de BD separados por servicio con restricciones via `GRANT`.
