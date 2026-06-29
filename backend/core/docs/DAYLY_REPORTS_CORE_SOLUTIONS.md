# PROGRESS — Core Service

Registro de lo implementado y lo pendiente.

> Ver también: `CORE.md` (arquitectura + estado), `ANALYTICS.md`, `FUTURE_MODULES.md` (blueprints), `SEED_TEMPLATES.md`.

---

## Estado Rápido

| Módulo | Estado | Tests |
|--------|--------|-------|
| Setup | ✅ Implementado | 5 unit + 6 integration |
| Product | ✅ Implementado | 6 unit + 4 integration (pendiente Docker) |
| Invoice | ✅ Implementado | 7 unit + 4 integration (pendiente Docker) |
| Analytics | ✅ Implementado | 5 unit + 4 JPA |
| Accounting | ⬜ Pendiente | Ver FUTURE_MODULES.md |
| Ventas | ⬜ Pendiente | Ver FUTURE_MODULES.md |
| Reportes | ⬜ Pendiente | Ver FUTURE_MODULES.md |

---

## Implementado

### Setup (`core_pymes/setup/`)
- `TenantSetup` entity JPA (tenantId, industry, onboardingCompleted)
- `TenantSetupRepository` (findByTenantId, existsByTenantId)
- `SetupService` interface + `SetupServiceImpl` (getOrInitialize lazy, completeOnboarding)
- `SetupApi` / `SetupController`
  - `GET /api/v1/core/setup/{tenantId}` — lazy init + template data
  - `POST /api/v1/core/setup/{tenantId}/onboarding` — completa onboarding
  - `GET /api/v1/core/setup/preview/{industry}` — preview categorías jerárquicas
- `SetupResponse` record DTO (id, tenantId, industry, onboardingCompleted, categories[], units[], locations[])
- `SetupMapper` (MapStruct)
- Flyway V1: `core.tenant_setup`
- `buildCategoryTree()` con mapa O(n)

### Product (`core_pymes/product/`)
- `Producto` entity JPA (UUID, tenantId, soft-delete `@SQLDelete`+`@Where`, timestamps)
- `Presentacion` entity JPA (FK → producto, soft-delete, dual-field pattern)
- `ProductoRepository` + `PresentacionRepository`
- `ProductoService` / `ProductoServiceImpl` (CRUD + tenant isolation)
- `ProductoApi` / `ProductoController`
  - `GET/POST/PUT/DELETE /api/v1/core/productos`
  - `GET/POST /api/v1/core/productos/{id}/presentaciones`
  - `DELETE /api/v1/core/productos/presentaciones/{id}`
- DTOs: `ProductoRequest/Response`, `PresentacionRequest/Response` (Java records)
- `ProductoMapper` (MapStruct)
- Eventos: `ProductoCreadoEvent`, `PresentacionCreadaEvent`
- Flyway V3: `core.products`, `core.product_presentations`

### Invoice (`core_pymes/invoice/`)
- `Proveedor` entity JPA (UUID, tenantId, soft-delete)
- `Factura` entity JPA (UUID, items cascade ALL, FK → proveedor + productos)
- `ItemFactura` entity JPA (productName snapshot, subtotal, `presentacionId`+`conversionFactor`)
- `ProveedorRepository` + `FacturaRepository`
- `FacturaService` / `FacturaServiceImpl` (CRUD + total auto-calc + estado workflow)
- `ProveedorApi` / `ProveedorController`
- `FacturaApi` / `FacturaController`
  - `GET/POST/PUT/DELETE /api/v1/core/proveedores`
  - `GET/POST /api/v1/core/facturas`
  - `POST /api/v1/core/facturas/{id}/pagar`
  - `DELETE /api/v1/core/facturas/{id}`
- DTOs: `ProveedorRequest/Response`, `FacturaRequest/Response`, `ItemFacturaRequest/Response`
- `FacturaMapper` (MapStruct)
- Eventos: `FacturaCreadaEvent`
- Flyway V4: `core.providers`, `core.invoices`, `core.invoice_items`
- Invoice number: `F-PROV-{year}-{sequential:04d}` por tenant

### Analytics (`core_pymes/analytics/`)
- `AnalisisGasto` entity (JSONB por tenant/periodo)
- 6 motores CTE: ABC, tendencias, márgenes, opex, proyección, alertas
- Listener conectado a FacturaCreadaEvent
- Ver `ANALYTICS.md` para detalles

### Infraestructura
- Dockerfile multi-stage (sin `dependency:go-offline`)
- docker-compose.yml: core-service con healthcheck, depende de postgres+redis+auth
- Gateway route `/api/v1/core/**` → `pymes-core-service:8082`
- EventConfig: `@EnableAsync` + virtual threads
- CorePath.java con constantes de ruta
- Perfiles dev/stg/prod
- MapStruct + Lombok + annotation processor
- OpenFeign configurado (sin clientes aún)
- Actuator habilitado (health)
- `CacheConfig.java`: `@EnableCaching` + `RedisCacheManager` (TTL 5min, JSON serializer)
- `@Cacheable` en findAll/findById de productos, proveedores, facturas
- `@CacheEvict(allEntries=true)` en writes
- `@ConditionalOnBean(RedisConnectionFactory.class)` — solo con Redis

### PWA / Offline
- `StaleWhileRevalidate` para `/api/v1/core/*` GETs
- Banner offline en `MainLayout.vue`
- Diálogo de actualización con `SKIP_WAITING`

### Seed Data
- Flyway V2: `industries`, `template_categories`, `template_locations`
- `SeedDataRunner`: idempotente, 8 industrias, 6 tablas template
- Crea via DDL: `template_units`, `template_movement_reasons`, `template_payment_methods`

### Testing
- `SetupServiceImplTest`: 5 unit tests (Mockito)
- `AbstractIntegrationTest`: base class Testcontainers PostgreSQL
- `SetupSeedIntegrationTest`: 6 integration tests
- `CoreApplicationTests`: smoke test contexto
- `ProductoServiceImplTest`: 6 unit tests (CRUD + eventos + tenant isolation)
- `FacturaServiceImplTest`: 7 unit tests (total calc + descuento + estados + tenant isolation)
- `AnalyticsServiceImplTest`: 5 unit tests (6 motores + upsert + consulta)
- `AnalyticsRepositoryTest`: 4 JPA tests (CRUD JSONB + tenant isolation)
- `ProductoIntegrationTest`: 4 integration tests (pendiente Docker)
- `FacturaIntegrationTest`: 4 integration tests (pendiente Docker)

### Refactoring (2026-06)
- Eliminado `FacturaPagadaEvent` — evento sin listener
- JSONB: `AnalisisGasto` actualizado a `@JdbcTypeCode(SqlTypes.JSON)`

---

## Pendiente

### Inmediato
- [ ] Accounting — `core_pymes/accounting/`: MetricaFinanciera + listener (ver FUTURE_MODULES.md)
- [ ] Ejecutar integration tests (requiere Docker)

### Mediate
- [ ] Reports — dashboard consolidado con KPIs, alertas
- [ ] CRUD configuración (edición por tenant)
- [ ] Módulo Ventas
- [ ] Movimientos de stock

### Infraestructura
- [ ] Spring Security (JWT validation local)
- [ ] FeignClient para Auth

### Frontend
- [x] Onboarding post-login
- [x] Módulo Core frontend (Productos, Proveedores, Facturas)
- [ ] Onboarding 2 pasos con preview de categorías
  - [ ] `CategoryTree.vue`
  - [ ] `OnboardingPage.vue`
  - [ ] `setup.service.ts` — agregar `preview()`
  - [ ] `SetupInfo` type — categorías jerárquicas
