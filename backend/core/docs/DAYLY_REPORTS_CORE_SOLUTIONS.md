# PROGRESS — Core Service

Registro de lo implementado y lo pendiente.

> Ver también: `CORE.md` (arquitectura + estado), `ANALYTICS.md`, `FUTURE_MODULES.md` (blueprints), `SEED_TEMPLATES.md`.

---

## Estado Rápido

| Módulo | Estado | Tests |
|--------|--------|-------|
| Setup | ✅ Implementado | 5 unit + 10 integration |
| Product | ✅ Implementado | 6 unit + 4 integration (pendiente Docker) |
| Invoice | ✅ Implementado | 7 unit + 4 integration (pendiente Docker) |
| Analytics | ✅ Implementado | 5 unit + 4 JPA |
| Accounting | ⬜ Pendiente | Ver FUTURE_MODULES.md |
| Ventas | ⬜ Pendiente | Ver FUTURE_MODULES.md |
| Reportes | ⬜ Pendiente | Ver FUTURE_MODULES.md |

---

## 2026-07-06 — Fix: productos guardan category_id (UUID) en vez de category_name

### Backend

- `SetupServiceImpl.completeOnboarding()`: Query cambió de `tc.name AS category_name` a `COALESCE(tp.category_id::text, tc.id::text) AS category_code`. `TemplateProductRow.categoryName` renombrado a `categoryCode`.
- `V8__fix_product_category.sql`: Migration que actualiza productos existentes con nombre de categoría → UUID vía JOIN con `template_categories` + `core.tenant_setup`.
- Tests: 60 unit tests core siguen pasando.

### Archivos modificados

```
core/setup/service/impl/SetupServiceImpl.java
resources/db/migration/V8__fix_product_category.sql
```

---

## 2026-07-01 — presentacionId opcional + SKU auto-generado + @Valid enforcement

### Backend

- `ItemFacturaRequest.presentacionId` → `@NotNull` removido, ahora es `UUID` nullable
- `FacturaServiceImpl.createFactura()`: cuando `presentacionId` es null, usa `conversionFactor = 1` y no busca presentación
- `FacturaController.create()` → `@Valid` agregado para validación 400 en vez de 500
- `ProductoServiceImpl.create()`: genera SKU `P-XXXX` secuencial cuando `sku` es null/blank
- `ProductoRepository` → +`countByTenantId(UUID)` para secuencia de SKU

### Archivos modificados

```
core/invoice/dto/ItemFacturaRequest.java       # presentacionId @NotNull removido
core/invoice/service/impl/FacturaServiceImpl.java  # handle null presentacionId
core/invoice/controller/impl/FacturaController.java # +@Valid on create()
core/product/service/impl/ProductoServiceImpl.java  # auto SKU generation
core/product/repository/ProductoRepository.java     # +countByTenantId()
```

---

## 2026-06-30 — Template Products: seed, copia al onboarding, frontend

### Implementado

- DDL `template_products` + `template_product_presentations` en `SeedDataRunner.createTables()` (CREATE TABLE IF NOT EXISTS, sin FK)
- ~20-25 productos por industria con 1-2 presentaciones c/u (~160 productos + ~280 presentaciones total)
- `addProd()` helper para batch arrays de producto + presentaciones en una línea
- `SetupResponse.ProductTemplateDTO(id, name, baseUnit, categoryName)` + campo `products` + factory `preview()`
- `SetupMapper` firma actualizada con 5to parámetro `products`
- `SetupServiceImpl.completeOnboarding()` — copia template_products → `core.products` con SKU auto `P-0001` secuencial + `core.product_presentations` batch insert
- `SetupServiceImpl.loadIndustryData()` — query con JOIN a template_categories para categoryName
- Frontend: `ProductTemplateDTO` type, sección "Productos precargados (N)" en OnboardingPage step 2
- 5 tests nuevos en `SetupSeedIntegrationTest`: seed counts, preview products, onboarding copy con SKU, tenant isolation
- Los tests verifican: 25 productos restaurante, SKU P-0001..P-0025, presentaciones > productos, 2 tenants aislados

### Decisiones

- **Opción A (SeedDataRunner DDL)** sobre Flyway V7/V8 — menos archivos, mismo patrón que template_units existente
- **Sin SKU en template** — se genera `P-%04d` al copiar al tenant
- **Sin FK** en template_product_presentations — datos readonly, orphan aceptable
- **JdbcTemplate batch** en vez de JPA para la copia — más directo para copia plana sin lógica de negocio

---

## Implementado

### Setup (`core_pymes/setup/`)
- `TenantSetup` entity JPA (tenantId, industry, onboardingCompleted)
- `TenantSetupRepository` (findByTenantId, existsByTenantId)
- `SetupService` interface + `SetupServiceImpl` (getOrInitialize lazy, completeOnboarding)
- `SetupApi` / `SetupController`
  - `GET /api/v1/core/setup/{tenantId}` — lazy init + template data (incl. products)
  - `POST /api/v1/core/setup/{tenantId}/onboarding` — completa onboarding + copia productos con SKU auto
  - `GET /api/v1/core/setup/preview/{industry}` — preview categorías + productos
- `SetupResponse` record DTO (id, tenantId, industry, onboardingCompleted, categories[], units[], locations[], products[])
- `SetupMapper` (MapStruct, 5 parámetros)
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
- `SeedDataRunner`: idempotente, 8 industrias, 8 tablas template
- Crea via DDL: `template_units`, `template_movement_reasons`, `template_payment_methods`, `template_products`, `template_product_presentations`
- ~160 productos + ~280 presentaciones en seed

### Testing
- `SetupServiceImplTest`: 5 unit tests (Mockito)
- `AbstractIntegrationTest`: base class Testcontainers PostgreSQL
- `SetupSeedIntegrationTest`: 10 integration tests (6 original + 4 nuevos: preview products, onboarding copy SKU, tenant isolation)
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
- [x] Onboarding 2 pasos con preview de categorías + productos precargados
