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
- **Sin SSL, por decision** (2026-10-27) — `sslmode` no se configura en ningun perfil porque Postgres solo escucha en `pymes-internal-network` y no se publica: solo 22/80/443 salen a Internet. Reabrir si la base sale de la red interna, se mueve a un servicio gestionado o el trafico cruza una red no confiable; entonces `sslmode=require` en `stg`/`prod`.

## Resolucion de Esquema (3 capas redundantes)

1. **JDBC `?currentSchema=`** a nivel de conexion
2. **Hibernate `default_schema`** en `application.yaml`
3. **Flyway `schemas`** — cada servicio migra solo su propio esquema
4. **Entidades de core ademas usan `@Table(schema = "core")`** explicitamente

## Flyway

| Servicio | Archivos | `baseline-on-migrate` | Estado |
|----------|----------|-----------------------|--------|
| auth | 3 (`V1__initial_schema.sql`, `V2__index_optimizations.sql`, `V3__add_role_change_tracking.sql`) | true | Estable |
| core | 8 (`V1`..`V8`, ultima `V8__report_log.sql`) | true | Evolucion activa |

`baseline-on-migrate: true` implica que V1 siempre se asume como baseline. No hay `baseline-version` personalizado.

Cada servicio tiene su propia secuencia de versiones y su propio `schemas:`. Auth y core nunca comparten numero de migracion, asi que un merge no genera conflicto mientras se copien archivos completos (no merges parciales de una carpeta). Verificar `db/migration/` de ambos servicios tras cada merge.

## JPA / Hibernate

- `ddl-auto: none` (Flyway gestiona el esquema)
- `hibernate.dialect: PostgreSQLDialect`
- HikariCP con `maximum-pool-size: 20` (configurado en auth y core). Core usa virtual threads (`spring.threads.virtual.enabled: true`) — pool 20 mitiga contención de carrier threads bajo carga concurrente.

## Multi-Inquilino (Multi-Tenancy)

- **Modelo de columna discriminadora** (nivel de fila), no esquema por inquilino.
- Toda tabla de `core` tiene `tenant_id UUID NOT NULL`. **Sin FK a `auth.tenants` por decision** (2026-10-27): una FK cross-schema acopla los dos servicios al orden de migracion y no aporta nada que el filtro por `tenant_id` no haga; la validacion vive en la capa de aplicacion (JWT + contexto de seguridad).
- **Sin huerfanos por construccion:** los tenants no se borran. `shutdown` es soft delete (`TenantServiceImpl.shutdown` → `tenants.is_active = false`) y las tablas de `auth` con `ON DELETE CASCADE` solo se disparan dentro de su propio esquema (`user_tenants`, `invitations`, `audit_log`). Si algun dia se agrega hard delete de tenants, hay que borrar tambien sus filas de `core` en la misma transaccion o agregar la FK.
- Esquema auth: tabla `user_tenants` con roles (OWNER, ADMIN, CONTABLE, VIEWER).

## Dependencias

- `spring-boot-starter-data-jpa`, `postgresql` (runtime), `flyway-core`, `flyway-database-postgresql`
- `testcontainers-postgresql` para pruebas de integracion (auth y core)
- Spring Boot: auth `3.4.3`, core `3.5.14` — Java 21.

## Observaciones Criticas

1. **Pool fijo de 20 por servicio** — auth y core comparten la misma instancia de Postgres (`max_connections` 100 por defecto): 40 conexiones de aplicacion + pooling de tests dejan margen corto. Si se agrega un tercer servicio o se sube el pool, revisar `max_connections` o pasar a PgBouncer.
2. **Sin cifrado en la conexion (decision 2026-10-27)** — ver seccion Conexion: aceptable solo mientras Postgres viva en la red interna de Docker.
3. **Virtual threads + JDBC bloqueante** — core habilita virtual threads pero usa JDBC (I/O bloqueante). HikariCP ancla los virtual threads a carrier threads durante las llamadas a DB. Monitorear inanicion de carriers bajo carga; el pool de 20 ya acota la concurrencia real.
4. **Sin FK entre esquemas (decision 2026-10-27)** — el riesgo residual es un `tenant_id` valido pero ajeno escrito por un bug de query. Mitigado por el filtro obligatorio por tenant y porque no existe hard delete; no se compensa con FK cross-schema.
5. **Riesgo de versionado Flyway** — auth y core usan secuencias de version independientes. Despues de merges, si una migracion agrega V4 a auth y V9 a core simultaneamente, ambas deben llegar sin conflicto. Verificar `db/migration/` tras cada merge.
6. **Mismas credenciales, ambos esquemas** — auth y core comparten usuario de BD. Un compromiso o bug en cualquiera de los servicios puede leer/escribir el esquema del otro (mitigado por aislamiento de esquema, pero no por autenticacion). Considerar usuarios de BD separados por servicio con restricciones via `GRANT`.
