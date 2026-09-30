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

> Requiere PostgreSQL y Redis en local. Copia `.env.example` a `.env` (igual que auth).

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
│   │   ├── EventConfig.java              # @EnableAsync + @EnableScheduling
│   │   ├── CacheConfig.java              # @EnableCaching + RedisCacheManager (facturas+productos)
│   │   ├── IdempotencyFilter.java        # POST Idempotency-Key 6h SET NX + replay
│   │   ├── TenantValidationFilter.java   # 403 si X-Tenant-Id != ?tenantId
│   │   ├── RoleHeaderFilter.java         # X-User-Role → SecurityContext
│   │   └── SecurityConfig.java           # @EnableMethodSecurity, 18 WRITE @PreAuthorize OWNER|ADMIN
│   ├── constant/CorePath.java            # rutas API
│   ├── exception/                        # GlobalExceptionHandler 12 handlers + CodigoError
│   ├── seed/SeedDataRunner.java          # 8 industrias, 6 tablas template
│   └── service/
│       └── RecomputeDebounceService.java  # Redis debounce
│
├── setup/       configuracion/inventario inicial
├── product/     catalogo productos y presentaciones
├── invoice/     facturas de compra y proveedores (V6 ITBMS, EstadoFactura, advisory_lock)
├── analytics/   9 motores CTE de analisis de gastos
├── gasto/       gastos operativos
├── prestamo/    prestamos y pagos
├── inversion/   patrimonio
├── venta/       ventas diarias
├── accounting/  metricas financieras consolidadas
└── report/      reporte mensual PDF (cron + endpoint dryRun)
```

Controller pattern: interface (`XxxApi`) + impl (`XxxController`) dentro del modulo.
DTOs: Java records. Mapper: MapStruct.

Ver [docs/CORE.md](./docs/CORE.md) para arquitectura completa.

---

## Modulos

| Modulo | Endpoints | Descripcion |
|--------|-----------|-------------|
| setup | 3 | Onboarding lazy + plantillas por industria |
| product | 8 | CRUD productos + presentaciones (soft-delete) + SKU 409 fix `findTopSkuByTenantId` + paginación A-Z `V5 idx_products_tenant_name` |
| invoice (facturas) | 5 | CRUD facturas + pagar + `PUT` editar REGISTRADA — ITBMS 0/7/10 por ítem `V6 itbms_tasa/itbms_monto` + `subtotalExento/Gravado/itbmsTotal`, delete solo OWNER `ANULADA` conserva items |
| invoice (proveedores) | 5 | CRUD proveedores (soft-delete) |
| gasto | 5 | Gastos operativos con categorias (soft-delete) + `GAS` enum |
| prestamo | 7 | Prestamos + pagos + estados (soft-delete) |
| inversion | 2 | Patrimonio por tenant (1 fila) |
| venta | 5 | Ventas diarias (soft-delete) |
| analytics | 2 | 9 motores CTE (ABC, tendencias, margenes, opex, proyeccion, alertas, supplier analytics) — solo `PAGADA` alimenta métricas |
| accounting | 2 | Metricas financieras consolidadas (CTE 1 round-trip) |
| report | 1 | Reporte mensual a OWNERs: PDF Jasper 7 + mail + idempotencia (`V8 report_log`) — cron día 1 06:00 Panama |

> **Total: 45 endpoints** — Facturas ahora exponen `itbmsTasa/itbmsMonto` por ítem y `subtotalExento/Gravado/itbmsTotal` en header; `Idempotency-Key` header opcional en POST (replay 6h)

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

POST Factura (idempotencia 6h)
  └── IdempotencyFilter @Order(0) Idempotency-Key header
      └── Redis GET idempotency:{tenantId}:{key} → replay 2xx (status|contentType|body)
      └── ContentCachingResponseWrapper + SET NX EX 6h solo 2xx
FacturaServiceImpl.generateInvoiceNumber
  └── SELECT pg_advisory_xact_lock(hashtext(tenantId)) → MAX+1 sin carrera

Cache productos
  └── ProductoServiceImpl @Cacheable("productos") 5min TTL
  └── FacturaServiceImpl @Caching(evict facturas+productos allEntries=true) en create/update/delete

Seguridad
  └── TenantValidationFilter: X-Tenant-Id (gateway) vs ?tenantId → 403 si difieren
  └── RoleHeaderFilter: X-User-Role → ROLE_OWNER|ADMIN + @PreAuthorize en 18 WRITE (CONTABLE/VIEWER solo GET)
```

| Key pattern | Tipo | Service |
|-------------|------|---------|
| `recompute:metrics:{tenantId}:{period}` | gasto/venta | MetricasService |
| `recompute:analytics:{tenantId}:{period}` | factura PAGADA | AnalyticsService |
| `idempotency:{tenantId}:{key}` | POST factura | IdempotencyFilter (TTL 6h) |

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

> Verificado `grep -c @Test` 2026-09-29 — total **291** (`88 unit + 17 analytics + 104 JPA + 15 report + 66 integration + 1 context`).

| Tipo | Tests | Tecnologia |
|------|-------|------------|
| Unit | 88 | Mockito, JUnit 5 (incl. `InvoiceCalculatorItbmsTest` 8, `GlobalExceptionHandlerTest` 14) |
| Analytics unit | 17 | Mockito + JdbcTemplate mock (`AnalyticsServiceImplTest`) |
| JPA | 104 | @DataJpaTest + Testcontainers PostgreSQL (Producto 30, Factura 18, Gasto 10, Prestamo 12, Venta 11, etc.) |
| Report | 15 | @SpringBootTest + Testcontainers PG/Redis — `core_pymes/report/`: `MonthlyReportIntegrationTest` 4 (PDF real) + `MonthlyReportControllerTest` 11 (edge cases) |
| Integration | 66 | @SpringBootTest + Testcontainers PG + Redis — `core_pymes/integration/` (Factura, Itbms, Producto, etc.) |
| Context | 1 | Application context load |
| **Total** | **291** | `./mvnw test -B` = **225** (todo salvo `core_pymes/integration/`) · `verify -Pintegration` = 66 |

> **Los tests de `report/` viven en `core_pymes/report/`, NO en `integration/`** → corren en la fase `test`
> (job "Unit Tests Core" de CI, que arranca Docker) y quedan fuera de `verify -Pintegration`.
> Si algún día se mueven a `integration/`, `./mvnw test -B` baja a 210 y el job de integración sube a 70.

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
POST   /api/v1/core/facturas                          # Header opcional Idempotency-Key (replay 6h, SET NX solo 2xx)
GET    /api/v1/core/facturas                          # filtra ANULADA en DB (WHERE status != 'ANULADA')
GET    /api/v1/core/facturas/{id}
PUT    /api/v1/core/facturas/{id}?tenantId=...        # solo REGISTRADA, revierte product stats + rebuild items
DELETE /api/v1/core/facturas/{id}?tenantId=...        # solo OWNER @PreAuthorize; PAGADA→ANULADA conserva items, REGISTRADA→soft-delete
POST   /api/v1/core/facturas/{id}/pagar               # dispara FacturaPagadaEvent → metrics dirty
```
> Item: `itbmsTasa 0|7|10 (null→0 exento) + itbmsMonto HALF_UP`; header: `subtotalExento/Gravado/itbmsTotal`, `total=subtotalNet+itbmsTotal-globalDiscount`. `GASTO_OPERATIVO` sin items usa `total` directo + `providerId` nullable + `colaboradorId` opcional. Solo `PAGADA` alimenta `analytics`/`metrics` (13 CTEs `WHERE status='PAGADA'`).

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
POST   /api/v1/core/reportes/monthly?period=YYYY-MM&dryRun=true   # OWNER|ADMIN
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
| [docs/strategies/MONTHLY_REPORT_STRATEGY.md](./docs/strategies/MONTHLY_REPORT_STRATEGY.md) | Reporte mensual fase 1 (PDF) + tabla de migración JR6→JR7 |
| [docs/DAILY_REPORTS_CORE_SOLUTIONS.md](./docs/DAILY_REPORTS_CORE_SOLUTIONS.md) | Historial de desarrollo |

---

## License

Proprietary. Todos los derechos reservados.
