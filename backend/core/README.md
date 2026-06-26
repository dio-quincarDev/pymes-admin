# Core Service

Microservicio de negocio del SaaS Pymes Admin. Gestiona operaciones core: inventario, facturas, contabilidad y reportes.

## Arquitectura

Modular y event-driven. Cada módulo vive en su propio paquete con controller/service/domain/repository.

```
core_pymes/
├── common/         # Config compartida, constantes, clientes Feign
├── setup/          # Setup y onboarding del tenant
├── inventario/     # (futuro) Productos, stock, movimientos
├── facturas/       # (futuro) Facturas, proveedores
├── contabilidad/   # (futuro) Márgenes, COGS, flujo de caja
└── reportes/       # (futuro) Dashboards, KPIs
```

Ver [docs/CORE_STRATEGY.md](./docs/CORE_STRATEGY.md) para la visión completa.

## Tech Stack

- **Java 21** (Virtual Threads)
- **Spring Boot 3.5+** (MVC, Data JPA, Validation)
- **PostgreSQL** (schema `core`) + Flyway migrations
- **Redis** (caching)
- **MapStruct** + Lombok
- **OpenFeign** (comunicación con Auth Service)
- **Spring Events** (comunicación entre módulos)
- **Docker** (multi-stage, alpine, non-root)

## Perfiles

| Profile | Uso | DB | Logging |
|---------|-----|----|---------|
| `dev` | Local | Valores por defecto | DEBUG |
| `stg` | Staging | Variables de entorno | INFO |
| `prod` | Producción | Variables de entorno | WARN |

## Cómo ejecutar

### Local (Maven)

```bash
./mvnw spring-boot:run -Pdev
```

Requiere PostgreSQL y Redis en local. Ver `.env` en la raíz del proyecto.

### Docker

```bash
docker compose up -d core-service
```

## Rutas

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/api/v1/core/setup/{tenantId}` | GET | Obtener o inicializar setup del tenant |
| `/api/v1/core/setup/{tenantId}/onboarding` | POST | Completar onboarding |

Todas las rutas pasan por el Gateway (puerto 8080) con autenticación JWT.

## Docs

| Archivo | Descripción |
|---------|-------------|
| [docs/CORE_STRATEGY.md](./docs/CORE_STRATEGY.md) | Arquitectura event-driven |
| [docs/PROGRESS.md](./docs/DAYLY_REPORTS_CORE_SOLUTIONS.md) | Qué está hecho y qué falta |
