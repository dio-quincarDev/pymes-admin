# PROGRESS — Core Service

Registro de lo implementado y lo pendiente

> Ver también: [`docs/` raíz](/docs/) — estrategias globales (DB, infra, testcontainers) y bitácora diaria en `DAILY_REPORTS_PROJECT.md`.

---

## Implementado ✅

### Módulo Setup
- `TenantSetup` entidad JPA (tenantId, industry, onboardingCompleted)
- `TenantSetupRepository` (findByTenantId, existsByTenantId)
- `SetupService` interface + `SetupServiceImpl` (getOrInitialize lazy, completeOnboarding)
- `SetupApi` controller interface + `SetupController` impl
  - `GET /api/v1/core/setup/{tenantId}` — lazy init si no existe, devuelve `SetupResponse` con template data
  - `POST /api/v1/core/setup/{tenantId}/onboarding` — completa onboarding con industry, devuelve `SetupResponse` con template data
- `SetupResponse` record DTO en `setup.dto` con `id`, `tenantId`, `industry`, `onboardingCompleted`, `categories[]`, `units[]`, `locations[]`
- `SetupMapper` (MapStruct): `toResponse(entity, categories, units, locations)` transforma entity + lists → DTO
- Flyway V1: tabla `core.tenant_setup`

### Infraestructura
- Dockerfile multi-stage simplificado (sin `dependency:go-offline`)
- docker-compose.yml: core-service con healthcheck, depende de postgres+redis+auth
- Gateway route `/api/v1/core/**` → `pymes-core-service:8082` con AuthenticationFilter
- Env var `CORE_SERVICE_HOST` añadida al gateway

### Configuración
- EventConfig con `@EnableAsync` + virtual threads (`spring.threads.virtual.enabled`)
- CorePath.java con constantes de ruta
- Perfiles dev/stg/prod con application.yaml base + perfiles
- MapStruct + Lombok + annotation processor configurado en pom.xml
- OpenFeign configurado (@EnableFeignClients) aunque sin clientes aún
- Actuator habilitado (health endpoint)

### Seed Data
- Flyway V2: tablas `industries`, `template_categories` (3 niveles con padre auto-ref), `template_locations`
- `SeedDataRunner`: `@Component` idempotente que inserta seed al startup vía JdbcTemplate
  - Crea vía DDL: `template_units`, `template_movement_reasons`, `template_payment_methods` (CREATE TABLE IF NOT EXISTS + índices en industry_code)
  - 8 industrias seedadas: restaurante, bares, salon_belleza, ferreteria, mini_super, taller_mecanico, farmacia, default
  - 6 tablas template: industries, categories, locations, units, movement_reasons, payment_methods

### Testing
- `SetupServiceImplTest`: 5 tests unitarios con Mockito
- `AbstractIntegrationTest`: base class con Testcontainers PostgreSQL
- `SetupSeedIntegrationTest`: 6 tests de integración (8 industrias, 6 tablas template)
- `CoreApplicationTests`: smoke test de contexto con Testcontainers

### Módulo Product (`core_pymes/product/`)

- `Producto` entidad JPA (UUID, tenantId, soft-delete con `@SQLDelete`+`@Where`, timestamps)
- `Presentacion` entidad JPA (FK → producto, soft-delete, dual-field con `productId` UUID + `@ManyToOne` read-only)
- `ProductoRepository` + `PresentacionRepository` (Spring Data JPA)
- `ProductoService` interface + `ProductoServiceImpl` (CRUD completo + tenant isolation)
- `ProductoApi` interface + `ProductoController`
  - `GET/POST/PUT/DELETE /api/v1/core/productos`
  - `GET/POST /api/v1/core/productos/{id}/presentaciones`
  - `DELETE /api/v1/core/productos/presentaciones/{id}`
- DTOs: `ProductoRequest/Response`, `PresentacionRequest/Response` (Java records)
- Mapper: `ProductoMapper` (MapStruct, primer mapper real del proyecto)
- Eventos: `ProductoCreadoEvent`, `PresentacionCreadaEvent`
- Flyway V3: tablas `core.products`, `core.product_presentations`

### Módulo Invoice (`core_pymes/invoice/`)

- `Proveedor` entidad JPA (UUID, tenantId, soft-delete)
- `Factura` entidad JPA (UUID, items cascade ALL, FK → proveedor + productos, dual-field relations)
- `ItemFactura` entidad JPA (productName snapshot, subtotal calculado)
- `ProveedorRepository` + `FacturaRepository`
- `FacturaService` interface + `FacturaServiceImpl` (CRUD + total auto-calc + estado workflow)
- `ProveedorApi` interface + `ProveedorController`
- `FacturaApi` interface + `FacturaController`
  - `GET/POST/PUT/DELETE /api/v1/core/proveedores`
  - `GET/POST /api/v1/core/facturas`
  - `POST /api/v1/core/facturas/{id}/pagar`
  - `DELETE /api/v1/core/facturas/{id}`
- DTOs: `ProveedorRequest/Response`, `FacturaRequest/Response`, `ItemFacturaRequest/Response` (Java records)
- Mapper: `FacturaMapper` (MapStruct)
- Eventos: `FacturaCreadaEvent`, `FacturaPagadaEvent`
- Flyway V4: tablas `core.providers`, `core.invoices`, `core.invoice_items`
- Invoice number auto-generado: `F-PROV-{year}-{sequential:04d}` por tenant via native query

### Testing

- `ProductoServiceImplTest`: 6 unit tests (CRUD + eventos + tenant isolation)
- `FacturaServiceImplTest`: 7 unit tests (total calc + descuento + estados + tenant isolation)
- `ProductoIntegrationTest`: 4 integration tests (pendiente — necesita Docker)
- `FacturaIntegrationTest`: 4 integration tests (pendiente — necesita Docker)

### Arquitectura
- Estructura modular: `setup/`, `product/`, `invoice/` cada uno con controller/domain/dto/event/mapper/repository/service
- Paquete base: `core_pymes`
- Controller interface+impl dentro del módulo (no plano)

---

## Pendiente 🚧

### Inmediato
- [x] **Template loading**: `POST /core/setup/{tenantId}/onboarding` y `GET /core/setup/{tenantId}` devuelven `SetupResponse` con `categories[]`, `units[]`, `locations[]` cargados de las tablas template filtradas por `industry_code`. Implementado via `SetupServiceImpl.buildResponse()` (private, reusado en ambos endpoints).
- [x] **Onboarding preview de categorías** — Endpoint `GET /setup/preview/{industry}` + `ItemDTO` con `parentId` + `children` para jerarquía — ✅ **IMPLEMENTADO**
  - `buildCategoryTree()` con mapa O(n), `SetupController.preview()` delegando a `SetupServiceImpl.previewIndustry()`
- [ ] **Accounting** — `core_pymes/accounting/`: MetricaFinanciera entity + listener FacturaCreada → recalcula márgenes
  - [ ] GET `contabilidad/metricas?tenantId=&year=&month=`
  - [ ] EventListener on FacturaCreada / FacturaPagada / (futuro VentaRegistrada)
  - [ ] Cálculos: ingresos, egresos, COGS, gastos operativos, márgenes
- [ ] Ejecutar integration tests (requiere Docker)
- [x] Decisión Gastos Operativos → Opción A (tipo=GASTO en facturas) — ya en código
- [ ] Decisión pendiente: Ventas → Opción A (módulo Ventas propio) o B (desde Movimientos futuro)

### Mediate
- [ ] **Reports** — dashboard consolidado con KPIs, últimas facturas/ventas, alertas
- [ ] CRUD de configuración (categorías, ubicaciones, unidades) para edición por tenant
  - [x] Preview de solo lectura: `GET /setup/preview/{industry}` (jerarquía de categorías)
  - [ ] Edición de configuración (CRUD completo por tenant)
- [ ] Módulo Ventas (si se opta por Opción A)
- [ ] Movimientos de stock (Parte 3 — asociados a facturas+ventas)

### Infraestructura pendiente
- [ ] Spring Security (JWT validation local si se requiere)
- [ ] FeignClient para Auth (solo cuando core consuma endpoints de auth)
- [ ] Cache con Redis
- [x] Sistema de eventos cross-module (Spring Events — implementado en product + invoice; faltan listeners accounting+reports)

### Frontend
- [x] Onboarding post-login — página `/onboarding` + router guard que llama `GET/POST /core/setup/{tenantId}`
- [x] Módulo Core frontend — Productos, Proveedores, Facturas, Configuración (CRUD completo)
- [ ] Onboarding 2 pasos — flujo select industria → preview categorías → confirmar
  - [ ] `CategoryTree.vue` — componente visual de árbol de categorías (solo lectura)
  - [ ] `OnboardingPage.vue` — flujo 2 pasos con preview antes de confirmar
  - [ ] `setup.service.ts` — agregar `preview()` para `GET /setup/preview/{industry}`
  - [ ] `SetupInfo` type — actualizar con categorías jerárquicas (`parentId` + `children`)

---

## Decisiones clave

| Decisión | Opción elegida | Alternativa descartada |
|----------|---------------|----------------------|
| Inicialización | Lazy (primer GET) | Webhook/evento TenantCreated |
| Estructura | Modular por módulo (`setup/`) | Layer-based plana |
| Docker | `COPY . .` + `mvn package` | `dependency:go-offline` separado |
| Comunicación | Spring Events | RabbitMQ (post-MVP) |
| Concurrencia | Virtual Threads + @Async | Thread pool tradicional |
| Seed data | Java `ApplicationRunner` + JdbcTemplate | Flyway SQL, JPA entities |
| Test infra | Testcontainers PostgreSQL, no Redis | Docker Compose externo |
| Mapper | MapStruct con interface + @Mapping | Manual setters, Lombok Builder |
| Relaciones FK | Dual-field pattern (UUID field + @ManyToOne read-only) | @ManyToOne con cascade completo |
| Entidades nomenclatura | Español (Producto, Factura, Proveedor) | Inglés (Product, Invoice) |
| Invoice numbering | Native query secuencial por tenant/año | UUID, secuencia global, lógica en app |
| Soft-delete | @SQLDelete + @Where(clause = "is_active = true") | @ManyToMany, tabla aparte |
| Response wrapper | Sin wrapper (response directo) | ApiResponse envelope genérico |
| Categorías jerarquicas | En nested DTO (`children` list) en el mismo `ItemDTO` | DTO separado por nivel, adjancency list con query recursiva |
