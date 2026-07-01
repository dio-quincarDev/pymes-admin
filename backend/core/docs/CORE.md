# Core Service — Estado Actual

> **REALITY CHECK (2026-06):** Módulos implementados: `setup/`, `product/`, `invoice/`, `analytics/`. Pendientes: `accounting/`, `ventas/`, `reportes/`.
> Ver `FUTURE_MODULES.md` para blueprints de módulos pendientes.

---

## 1. Visión General

```
Frontend (PWA)
    ↓ JWT
Gateway (8080)
    ↓
Core Service (8082)
├── setup/   ✅ configuración/inventario inicial
├── product/ ✅ catálogo productos y presentaciones
├── invoice/ ✅ facturas de compra y proveedores
├── analytics/ ✅ 6 motores CTE de análisis de gastos
├── ⬜ accounting/  (ver FUTURE_MODULES.md)
├── ⬜ ventas/      (ver FUTURE_MODULES.md)
└── ⬜ reportes/    (ver FUTURE_MODULES.md)
```

Todos comunican vía Spring Events (no bloqueantes). Paquete base: `core_pymes.*`.

---

## 2. Módulos Implementados

### Setup (`core_pymes/setup/`)
- Plantillas precargadas por industria (8 industrias)
- Categorías jerárquicas (3 niveles, preview con árbol)
- Unidades, Ubicaciones, Productos (template tables)
- Onboarding lazy (primer GET) + POST para completar
- `TenantSetup` entity, `SetupService`, `SetupApi`/`SetupController`
- Flyway V1: tabla `core.tenant_setup`
- **`template_products` + `template_product_presentations`** — creadas via SeedDataRunner DDL, copia productos genéricos al completar onboarding con SKU auto `P-0001` secuencial (ver SEED_TEMPLATES.md §Plantillas de Productos)

### Product (`core_pymes/product/`)
- `Producto` (UUID, tenantId, soft-delete con `@SQLDelete`+`@Where`, timestamps)
- `Presentacion` (FK → producto, factor conversión, soft-delete, dual-field pattern)
- `ProductoCreadoEvent`, `PresentacionCreadaEvent`
- Flyway V3: `core.products`, `core.product_presentations`

### Invoice (`core_pymes/invoice/`)
- `Factura` (UUID, items cascade ALL, FK → proveedor + productos)
- `ItemFactura` (productName snapshot, subtotal calculado, `presentacionId`+`conversionFactor`)
- `Proveedor` (UUID, tenantId, soft-delete)
- `FacturaCreadaEvent` → escuchado por AnalyticsListener
- `FacturaPagadaEvent` → eliminado (dead code, sin listeners)
- Invoice number auto-generado: `F-PROV-{year}-{sequential:04d}`
- `presentacionId` opcional en `ItemFacturaRequest` — cuando null, `conversionFactor = 1` (base unit)
- `@Valid` enforced en `FacturaController.create()` para 400 en vez de 500
- Flyway V4: `core.providers`, `core.invoices`, `core.invoice_items`

### Analytics (`core_pymes/analytics/`)
- `AnalisisGasto` (JSONB por tenant/periodo — expense_analysis table)
- 6 motores CTE: ABC, tendencias, márgenes, opex, proyección, alertas
- Listener conectado a FacturaCreadaEvent → ejecuta análisis async
- Ver `ANALYTICS.md` para detalles
- Tests: 5 unitarios + 4 JPA

---

## 3. Estructura de Paquetes

```
core_pymes/
├── CoreApplication.java
├── common/
│   ├── config/
│   │   └── EventConfig.java (virtual threads + @Async)
│   ├── constant/
│   │   └── CorePath.java (rutas API)
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   └── seed/
│       └── SeedDataRunner.java (8 industrias, 6 tablas template)
│
├── setup/       ✅ controller/domain/dto/mapper/repository/service
├── product/     ✅ controller/domain/dto/event/mapper/repository/service
├── invoice/     ✅ controller/domain/dto/event/listener/mapper/repository/service
├── analytics/   ✅ controller/domain/dto/mapper/repository/service

⬜ accounting/   (ver FUTURE_MODULES.md)
⬜ reportes/
⬜ ventas/
```

Controller pattern: interface (`XxxApi`) + impl (`XxxController`) dentro del módulo.
DTOs: Java records. Mapper: MapStruct.

---

## 4. Flujo de Eventos — Estado Actual

### Tenant Se Registra
```
Auth Service crea Tenant
  └── Setup escucha TenantCreated
      └── Copia plantilla por industria → ConfiguracionTenant
      └── Publica: ConfiguracionCargada
```

### Tenant Registra Factura (Compra) — FLUJO REAL ACTUAL
```
1. Invoice: POST /api/v1/core/facturas
   └── Valida datos
   └── Persiste Factura + Items (incluye presentacionId, conversionFactor)
   └── Publica: FacturaCreadaEvent

2. [Async] FacturaCreadaListener → AnalyticsService.ejecutarCompleto()
   └── Ejecuta 6 motores CTE en PostgreSQL
   └── Persiste resultado en expense_analysis (JSONB)

3. ⬜ Accounting: NO IMPLEMENTADO
4. ⬜ Reportes: NO IMPLEMENTADO
```

### Registrar Gasto Operativo
- Opción A: `Factura.type` libre (`"FACTURA"`, `"GASTO_OPERATIVO"`, etc.)
- Ya implícita en código — frontend decide el tipo

### Registrar Venta — PENDIENTE
- No implementado. No hay endpoint `/ventas`, no hay entidad Venta.

---

## 5. Arquitectura Técnica

### Spring Events
- Simple, sin dependencias externas, suficiente para MVP
- Migrar a RabbitMQ en Fase 3+ sin cambiar código de eventos

### Virtual Threads
- `@EventListener` + `@Async` → Virtual Thread automático
- Concurrencia trivial con recursos mínimos (Java 21)

### Transaccionalidad
1. Operación persiste en BD (COMMIT)
2. Evento se publica DESPUÉS del COMMIT
3. Listeners procesan async (pueden fallar sin afectar persistencia)
4. Idempotencia en listeners: si falla, reintentar sin duplicar

### Cache
- `CacheConfig.java`: `@EnableCaching` + `RedisCacheManager` (TTL 5min, JSON serializer)
- `@Cacheable` en findAll/findById de productos, proveedores, facturas
- `@CacheEvict(allEntries=true)` en writes
- `@ConditionalOnBean(RedisConnectionFactory.class)` — solo con Redis disponible

---

## 6. Endpoints

### Configuración
```
GET  /api/v1/core/setup/{tenantId}
POST /api/v1/core/setup/{tenantId}/onboarding
GET  /api/v1/core/setup/preview/{industry}
```

### Productos
```
POST   /api/v1/core/productos
GET    /api/v1/core/productos
GET    /api/v1/core/productos/{id}
PUT    /api/v1/core/productos/{id}
DELETE /api/v1/core/productos/{id}

POST   /api/v1/core/productos/{id}/presentaciones
GET    /api/v1/core/presentaciones
PUT    /api/v1/core/presentaciones/{id}
DELETE /api/v1/core/presentaciones/{id}
```

### Proveedores
```
POST   /api/v1/core/proveedores
GET    /api/v1/core/proveedores
PUT    /api/v1/core/proveedores/{id}
DELETE /api/v1/core/proveedores/{id}
```

### Facturas
```
POST   /api/v1/core/facturas
GET    /api/v1/core/facturas
GET    /api/v1/core/facturas/{id}
PUT    /api/v1/core/facturas/{id}
DELETE /api/v1/core/facturas/{id}
POST   /api/v1/core/facturas/{id}/pagar
```

### Analytics
```
GET  /api/v1/core/analytics/consultar?tenantId={uuid}&periodo=YYYY-MM
POST /api/v1/core/analytics/recalcular?tenantId={uuid}&periodo=YYYY-MM
```

---

## 7. Seed Data

- Flyway V2: tablas `industries`, `template_categories`, `template_locations`
- `SeedDataRunner`: `@Component` idempotente que inserta seed al startup vía JdbcTemplate
- Crea via DDL: `template_units`, `template_movement_reasons`, `template_payment_methods`, `template_products`, `template_product_presentations`
- 8 industrias: restaurante, bares, salon_belleza, ferreteria, mini_super, taller_mecanico, farmacia, default
- 8 tablas template: industries, categories, locations, units, movement_reasons, payment_methods, products, product_presentations

Ver `SEED_TEMPLATES.md` para detalle completo de plantillas.

---

## 8. Decisiones Técnicas

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
| Categorías jerárquicas | En nested DTO (`children` list) en el mismo `ItemDTO` | DTO separado por nivel, adjancency list con query recursiva |
| Template products | Genéricos por industria (sin marca), copiados al onboarding | Productos específicos con marca, catálogo estático |
| Onboarding productos | Copia en `completeOnboarding()` (transaccional) | Webhook async, job diferido |
| Gastos Operativos | Opción A (type libre en Factura) — ya en código | Enum cerrado |

---

## 9. Pendientes

### Inmediato
- [x] **Template Products** — SeedDataRunner DDL + seed, SetupServiceImpl copy on completeOnboarding con SKU auto `P-0001`, preview en frontend (ver SEED_TEMPLATES.md §Plantillas de Productos)
- [ ] **Accounting** — `core_pymes/accounting/`: MetricaFinanciera + listener → recalcula márgenes (ver FUTURE_MODULES.md)
- [ ] Ejecutar integration tests (requiere Docker)

### Mediate
- [ ] **Reports** — dashboard consolidado con KPIs, alertas
- [ ] CRUD de configuración (edición por tenant)
- [ ] Módulo Ventas (si se opta por Opción A)
- [ ] Movimientos de stock (asociados a facturas+ventas)

### Infraestructura
- [ ] Spring Security (JWT validation local)
- [ ] FeignClient para Auth
- [x] Cache con Redis
- [x] Sistema de eventos cross-module (Spring Events)

### Frontend
- [x] Onboarding post-login
- [x] Módulo Core frontend — Productos, Proveedores, Facturas
- [x] Onboarding 2 pasos con preview de categorías
- [x] Onboarding con preview de productos precargados
