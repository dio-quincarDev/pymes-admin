# Core Service

> Microservicio de negocio del SaaS Pymes Admin. Gestiona operaciones core: setup, productos, facturas, gastos, prestamos, inversiones, ventas, contabilidad y analisis.

---

## Tech Stack

| Tecnologia | Version | Uso |
|------------|---------|-----|
| Java | 21 | Virtual Threads |
| Spring Boot | 3.5.14 | MVC, Data JPA, Validation |
| PostgreSQL | 15 | Schema `core` + Flyway migrations |
| Redis | 7 | Caching + debounce para recomputo |
| MapStruct | 1.6.3 | DTO mapping |
| Lombok | -- | Boilerplate reduction |
| OpenFeign | -- | Comunicacion con Auth Service |
| Spring Events | -- | Comunicacion entre modulos |
| Docker | -- | Multi-stage, alpine, non-root |

---

## Quick Start

```bash
cd backend/core
./mvnw spring-boot:run -Pdev
```

> Requiere PostgreSQL y Redis en local. Ver `.env` en la raiz del proyecto.

### Docker

```bash
docker compose up -d core-service
```

---

## Perfiles

| Profile | Uso | DB | Logging |
|---------|-----|----|---------|
| `dev`   | Local | Valores por defecto | DEBUG |
| `stg`   | Staging | Variables de entorno | INFO |
| `prod`  | Produccion | Variables de entorno | WARN |

---

## Arquitectura

Modular y event-driven. Cada modulo vive en su propio paquete con controller/service/domain/repository.

```
core_pymes/
├── common/
│   ├── config/
│   │   ├── EventConfig.java        # @EnableAsync + @EnableScheduling
│   │   └── CacheConfig.java        # @EnableCaching + RedisCacheManager
│   ├── constant/CorePath.java      # rutas API
│   ├── exception/                  # GlobalExceptionHandler
│   ├── seed/SeedDataRunner.java    # 8 industrias, 6 tablas template
│   └── service/
│       └── RecomputeDebounceService.java  # Redis debounce
│
├── setup/       configuracion/inventario inicial
├── product/     catalogo productos y presentaciones
├── invoice/     facturas de compra y proveedores
├── analytics/   9 motores CTE de analisis de gastos
├── gasto/       gastos operativos
├── prestamo/    prestamos y pagos
├── inversion/   patrimonio
├── venta/       ventas diarias
└── accounting/  metricas financieras consolidadas
```

Controller pattern: interface (`XxxApi`) + impl (`XxxController`) dentro del modulo.
DTOs: Java records. Mapper: MapStruct.

Ver [docs/CORE.md](./docs/CORE.md) para arquitectura completa.

---

## Modulos

| Modulo | Endpoints | Descripcion |
|--------|-----------|-------------|
| setup | 3 | Onboarding lazy + plantillas por industria |
| product | 8 | CRUD productos + presentaciones (soft-delete) |
| invoice (facturas) | 5 | CRUD facturas + pagar |
| invoice (proveedores) | 5 | CRUD proveedores (soft-delete) |
| gasto | 5 | Gastos operativos con categorias (soft-delete) |
| prestamo | 7 | Prestamos + pagos + estados (soft-delete) |
| inversion | 2 | Patrimonio por tenant (1 fila) |
| venta | 5 | Ventas diarias (soft-delete) |
| analytics | 2 | 9 motores CTE (ABC, tendencias, margenes, opex, proyeccion, alertas, supplier analytics) |
| accounting | 2 | Metricas financieras consolidadas (CTE 1 round-trip) |

> **Total: 44 endpoints**

---

## Eventos y Debounce

```
Factura/Gasto/Venta creado
  └── Listener async (AFTER_COMMIT)
      └── RecomputeDebounceService.markDirty()
          └── Redis SETNX recompute:{tipo}:{tenantId}:{period} (1ms)

@Scheduled(fixedDelay=30s)
  └── processPending() barre keys
  └── 1 recompute por (tipo, tenant, periodo) unico
  └── MetricasService.recalcular() o AnalyticsService.ejecutarCompleto()
```

| Key pattern | Tipo | Service |
|-------------|------|---------|
| `recompute:metrics:{tenantId}:{period}` | gasto/venta | MetricasService |
| `recompute:analytics:{tenantId}:{period}` | factura | AnalyticsService |

---

## Testing

```bash
./mvnw test -B                          # unit tests
./mvnw verify -B -Dspring.profiles.active=integration  # integration (requiere Docker)
```

### Testcontainers

| Container | Imagen | Puerto |
|-----------|--------|--------|
| PostgreSQL | `postgres:15-alpine` | dinamico |
| Redis | `redis:7-alpine` | 6379 |

> `AbstractIntegrationTest`: base class que arranca ambos containers via `@ServiceConnection`.

### Cobertura por Tipo

| Tipo | Tests | Tecnologia |
|------|-------|------------|
| Unit | 60 | Mockito, JUnit 5 |
| JPA | 88 | @DataJpaTest + Testcontainers PostgreSQL |
| Integration | 45 | @SpringBootTest + Testcontainers PG + Redis |
| Analytics | 5 | Mockito + JdbcTemplate mock |
| Context | 1 | Application context load |
| Seed | 19 | Setup + Seed data integration |
| **Total** | **218** | |

---

## Endpoints Principales

### Configuracion

```
GET    /api/v1/core/setup/{tenantId}
POST   /api/v1/core/setup/{tenantId}/onboarding
GET    /api/v1/core/setup/preview/{industry}
```

### Productos

```
POST   /api/v1/core/productos
GET    /api/v1/core/productos
GET    /api/v1/core/productos/{id}
PUT    /api/v1/core/productos/{id}
DELETE /api/v1/core/productos/{id}
POST   /api/v1/core/productos/{id}/presentaciones
GET    /api/v1/core/productos/{id}/presentaciones
DELETE /api/v1/core/presentaciones/{presentacionId}
```

### Proveedores

```
POST   /api/v1/core/proveedores
GET    /api/v1/core/proveedores
GET    /api/v1/core/proveedores/{id}
PUT    /api/v1/core/proveedores/{id}
DELETE /api/v1/core/proveedores/{id}
```

### Facturas

```
POST   /api/v1/core/facturas
GET    /api/v1/core/facturas
GET    /api/v1/core/facturas/{id}
DELETE /api/v1/core/facturas/{id}
POST   /api/v1/core/facturas/{id}/pagar
```

### Gastos / Prestamos / Ventas / Patrimonio

```
POST/GET/PUT/DELETE /api/v1/core/gastos
POST/GET/PUT/DELETE /api/v1/core/prestamos
POST               /api/v1/core/prestamos/{id}/pagos
GET                /api/v1/core/prestamos/{id}/pagos
POST/GET/PUT/DELETE /api/v1/core/ventas
GET/PUT            /api/v1/core/patrimonio/{tenantId}
```

### Accounting / Analytics

```
GET    /api/v1/core/accounting/consultar?tenantId={uuid}&periodo=YYYY-MM
POST   /api/v1/core/accounting/recalcular?tenantId={uuid}&periodo=YYYY-MM
GET    /api/v1/core/analytics?tenantId={uuid}&periodo=YYYY-MM
POST   /api/v1/core/analytics/recalcular?tenantId={uuid}&periodo=YYYY-MM
```

> Todas las rutas pasan por el Gateway (puerto 8080) con autenticacion JWT.

---

## CI/CD

GitHub Actions ejecuta `mvn verify` en cada PR a main/develop/feature/*. Docker images multi-arch (AMD64/ARM64) se buildean y pushean en CD.

| Rama | Pipeline | Deploy |
|------|----------|--------|
| `feature/*` | CI (build + test) | Ninguno |
| `develop` | CI + CD | Staging |
| `main` | CI + CD | Produccion |

---

## Docs

| Archivo | Descripcion |
|---------|-------------|
| [docs/CORE.md](./docs/CORE.md) | Arquitectura completa + estado de modulos |
| [docs/ANALYTICS.md](./docs/ANALYTICS.md) | 9 motores CTE + SQL + performance |
| [docs/SEED_TEMPLATES.md](./docs/SEED_TEMPLATES.md) | Plantillas por industria |
| [docs/FUTURE_MODULES.md](./docs/FUTURE_MODULES.md) | Blueprints originales (reportes pendiente) |
| [docs/DAILY_REPORTS_CORE_SOLUTIONS.md](./docs/DAILY_REPORTS_CORE_SOLUTIONS.md) | Historial de desarrollo |

---

## License

Proprietary. Todos los derechos reservados.
