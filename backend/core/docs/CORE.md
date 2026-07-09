# Core Service — Estado Actual

> **Estado (2026-07-09):** Todos los modulos de negocio implementados: `setup/`, `product/`, `invoice/`, `analytics/`, `gasto/`, `prestamo/`, `inversion/`, `venta/`, `accounting/`. Pendiente: `reportes/`.
> Ver `FUTURE_MODULES.md` para blueprints originales y `DAYLY_REPORTS_CORE_SOLUTIONS.md` para historial.

---

## Vision General

```
Frontend (PWA)
    | JWT
Gateway (8080)
    |
Core Service (8082)
├── setup/      configuracion/inventario inicial
├── product/    catalogo productos y presentaciones
├── invoice/    facturas de compra y proveedores
├── analytics/  6 motores CTE de analisis de gastos
├── gasto/      gastos operativos
├── prestamo/   prestamos y pagos
├── inversion/  patrimonio
├── venta/      ventas diarias
├── accounting/ metricas financieras consolidadas
└── reportes/   (pendiente)
```

Todos comunican via Spring Events (no bloqueantes). Paquete base: `core_pymes.*`.

---

## Modulos Implementados

### Setup (`core_pymes/setup/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `TenantSetup` (tenantId, industry, onboardingCompleted) |
| Service | `SetupService` / `SetupServiceImpl` (lazy init + completeOnboarding) |
| Endpoints | `GET /setup/{tenantId}`, `POST /setup/{tenantId}/onboarding`, `GET /setup/preview/{industry}` |
| Flyway | V1: `core.tenant_setup` |
| Templates | Categorias jerarquicas (3 niveles), unidades, ubicaciones, productos precargados por industria |
| SKU auto | `P-0001` secuencial al copiar template_products en onboarding |

### Product (`core_pymes/product/`)

| Aspecto | Detalle |
|---------|---------|
| Entidades | `Producto` (UUID, soft-delete), `Presentacion` (FK, conversionFactor, soft-delete) |
| Endpoints | CRUD `/productos`, CRUD `/productos/{id}/presentaciones`, `DELETE /presentaciones/{id}` |
| Eventos | `ProductoCreadoEvent`, `PresentacionCreadaEvent` |
| Flyway | V3: `core.products`, `core.product_presentations` |

### Invoice (`core_pymes/invoice/`)

| Aspecto | Detalle |
|---------|---------|
| Entidades | `Factura` (UUID, items cascade ALL), `ItemFactura` (snapshot, subtotal), `Proveedor` (soft-delete) |
| Endpoints | CRUD `/facturas`, `POST /facturas/{id}/pagar`, CRUD `/proveedores` |
| Eventos | `FacturaCreadaEvent` (escuchado por Analytics + debounce) |
| Invoice number | `F-PROV-{year}-{sequential:04d}` por tenant |
| Flyway | V4: `core.providers`, `core.invoices`, `core.invoice_items` |

### Analytics (`core_pymes/analytics/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `AnalisisGasto` (JSONB por tenant/periodo) |
| Endpoints | `GET /analytics/consultar`, `POST /analytics/recalcular` |
| Motores | ABC, tendencias, margenes, opex, proyeccion, alertas, comparativa proveedores, recomendaciones, predicciones |
| Flyway | V6 (analytics), V11 (supplier fields) |

### Gasto (`core_pymes/gasto/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `GastoOperativo` (soft-delete) |
| Enum | `CategoriaGasto`: SALARIOS, AGUA, LUZ, INTERNET, ALQUILER, MANTENIMIENTO, PUBLICIDAD, OTROS |
| Endpoints | CRUD `/gastos` |
| Eventos | `GastoCreadoEvent` -> debounce Redis |

### Prestamo (`core_pymes/prestamo/`)

| Aspecto | Detalle |
|---------|---------|
| Entidades | `Prestamo` (soft-delete), `PagoPrestamo` |
| Enum | `EstadoPrestamo`: ACTIVO, PAGADO, CANCELADO |
| Endpoints | CRUD `/prestamos`, `POST /prestamos/{id}/pagos`, `GET /prestamos/{id}/pagos` |

### Inversion (`core_pymes/inversion/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `Patrimonio` (PK = tenant_id, 1 fila por tenant) |
| Endpoints | `GET /patrimonio/{tenantId}` (get-or-create), `PUT /patrimonio/{tenantId}` |

### Venta (`core_pymes/venta/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `VentaDiaria` (soft-delete) |
| Endpoints | CRUD `/ventas` |
| Eventos | `VentaCreadaEvent` -> debounce Redis |

### Accounting (`core_pymes/accounting/`)

| Aspecto | Detalle |
|---------|---------|
| Entidad | `MetricasFinanciera` (tenant_id + period, UNIQUE) |
| Endpoints | `GET /accounting/consultar`, `POST /accounting/recalcular` |
| Query | CTE consolidado (5 fuentes: ventas, facturas, gastos, prestamos, patrimonio) en 1 round-trip |

---

## Estructura de Paquetes

```
core_pymes/
├── CoreApplication.java
├── common/
│   ├── config/
│   │   ├── EventConfig.java        # @EnableAsync + @EnableScheduling
│   │   └── CacheConfig.java        # @EnableCaching + RedisCacheManager
│   ├── constant/
│   │   └── CorePath.java           # rutas API
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   ├── seed/
│   │   └── SeedDataRunner.java     # 8 industrias, 6 tablas template
│   └── service/
│       └── RecomputeDebounceService.java  # Redis debounce para metricas/analytics
│
├── setup/       controller/domain/dto/mapper/repository/service
├── product/     controller/domain/dto/event/mapper/repository/service
├── invoice/     controller/domain/dto/event/listener/mapper/repository/service
├── analytics/   controller/domain/dto/mapper/repository/service
├── gasto/       controller/domain/dto/event/listener/mapper/repository/service
├── prestamo/    controller/domain/dto/mapper/repository/service
├── inversion/   controller/domain/dto/mapper/repository/service
├── venta/       controller/domain/dto/event/listener/mapper/repository/service
└── accounting/  controller/domain/dto/mapper/repository/service
```

Controller pattern: interface (`XxxApi`) + impl (`XxxController`) dentro del modulo.
DTOs: Java records. Mapper: MapStruct.

---

## Flujo de Eventos

### Tenant Se Registra

```
Auth Service crea Tenant
  └── Setup escucha TenantCreated
      └── Copia plantilla por industria -> ConfiguracionTenant
      └── Publica: ConfiguracionCargada
```

### Tenant Registra Factura

```
1. Invoice: POST /api/v1/core/facturas
   └── Valida datos
   └── Persiste Factura + Items
   └── Publica: FacturaCreadaEvent

2. [Async] FacturaCreadaListener -> RecomputeDebounceService.markAnalyticsDirty()
   └── SETNX recompute:analytics:{tenantId}:{period} en Redis (1ms)

3. [Scheduled @30s] RecomputeDebounceService.processPending()
   └── Barre keys pendientes
   └── 1 recompute por (tipo, tenant, periodo) unico
   └── AnalyticsService.ejecutarCompleto() -> 7 motores CTE
   └── MetricasService.recalcular() -> 1 CTE consolidado
```

### Registrar Gasto / Venta

```
POST /gastos o /ventas
  └── Persiste entidad
  └── Publica: GastoCreadoEvent / VentaCreadaEvent

[Async] GastoCreadaListener / VentaCreadaListener
  └── RecomputeDebounceService.markMetricsDirty()
  └── Redis SETNX -> 1ms, scheduler recalc cada 30s
```

### Debounce (Redis)

| Key pattern | Tipo | Service llamado |
|-------------|------|-----------------|
| `recompute:metrics:{tenantId}:{period}` | gasto/venta | `MetricasService.recalcular()` |
| `recompute:analytics:{tenantId}:{period}` | factura | `AnalyticsService.ejecutarCompleto()` |

TTL: 1 hora. Retry: key se conserva si falla (reintenta en proximo ciclo).

---

## Arquitectura Tecnica

### Spring Events

- Simple, sin dependencias externas, suficiente para MVP
- Migrar a RabbitMQ en Fase 3+ sin cambiar codigo de eventos

### Virtual Threads

- `@EventListener` + `@Async` -> Virtual Thread automatico
- Concurrencia trivial con recursos minimos (Java 21)

### Transaccionalidad

1. Operacion persiste en BD (COMMIT)
2. Evento se publica DESPUES del COMMIT
3. Listeners procesan async (pueden fallar sin afectar persistencia)
4. Idempotencia: debounce Redis deduplica por (tipo, tenant, periodo)

### Cache

| Aspecto | Detalle |
|---------|---------|
| Config | `CacheConfig.java`: `@EnableCaching` + `RedisCacheManager` (TTL 5min) |
| Lecturas | `@Cacheable` en findAll/findById de productos, proveedores, facturas |
| Escrituras | `@CacheEvict(allEntries=true)` en writes |
| Condicion | `@ConditionalOnBean(RedisConnectionFactory.class)` |

---

## Endpoints

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
GET    /api/v1/core/presentaciones
PUT    /api/v1/core/presentaciones/{id}
DELETE /api/v1/core/presentaciones/{id}
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

### Gastos Operativos

```
POST   /api/v1/core/gastos
GET    /api/v1/core/gastos
GET    /api/v1/core/gastos/{id}
PUT    /api/v1/core/gastos/{id}
DELETE /api/v1/core/gastos/{id}
```

### Prestamos

```
POST   /api/v1/core/prestamos
GET    /api/v1/core/prestamos
GET    /api/v1/core/prestamos/{id}
PUT    /api/v1/core/prestamos/{id}
DELETE /api/v1/core/prestamos/{id}
POST   /api/v1/core/prestamos/{id}/pagos
GET    /api/v1/core/prestamos/{id}/pagos
```

### Patrimonio

```
GET    /api/v1/core/patrimonio/{tenantId}
PUT    /api/v1/core/patrimonio/{tenantId}
```

### Ventas

```
POST   /api/v1/core/ventas
GET    /api/v1/core/ventas
GET    /api/v1/core/ventas/{id}
PUT    /api/v1/core/ventas/{id}
DELETE /api/v1/core/ventas/{id}
```

### Accounting

```
GET    /api/v1/core/accounting/consultar?tenantId={uuid}&periodo=YYYY-MM
POST   /api/v1/core/accounting/recalcular?tenantId={uuid}&periodo=YYYY-MM
```

### Analytics

```
GET    /api/v1/core/analytics/consultar?tenantId={uuid}&periodo=YYYY-MM
POST   /api/v1/core/analytics/recalcular?tenantId={uuid}&periodo=YYYY-MM
```

---

## Seed Data

| Aspecto | Detalle |
|---------|---------|
| Flyway V2 | `industries`, `template_categories`, `template_locations` |
| SeedDataRunner | `@Component` idempotente, inserta via JdbcTemplate al startup |
| DDL | `template_units`, `template_movement_reasons`, `template_payment_methods`, `template_products`, `template_product_presentations` |
| Industrias | 8: restaurante, bares, salon_belleza, ferreteria, mini_super, taller_mecanico, farmacia, default |
| Productos | ~160 productos + ~280 presentaciones en seed |

Ver `SEED_TEMPLATES.md` para detalle completo.

---

## Decisiones Tecnicas

| Decision | Opcion elegida | Alternativa descartada |
|----------|---------------|----------------------|
| Inicializacion | Lazy (primer GET) | Webhook/evento TenantCreated |
| Estructura | Modular por modulo (`setup/`) | Layer-based plana |
| Docker | `COPY . .` + `mvn package` | `dependency:go-offline` separado |
| Comunicacion | Spring Events | RabbitMQ (post-MVP) |
| Concurrencia | Virtual Threads + @Async | Thread pool tradicional |
| Seed data | Java `ApplicationRunner` + JdbcTemplate | Flyway SQL, JPA entities |
| Test infra | Testcontainers PostgreSQL + Redis | Docker Compose externo |
| Mapper | MapStruct con interface + @Mapping | Manual setters, Lombok Builder |
| Relaciones FK | Dual-field pattern (UUID field + @ManyToOne read-only) | @ManyToOne con cascade completo |
| Entidades | Espanol (Producto, Factura, Proveedor) | Ingles (Product, Invoice) |
| Invoice numbering | Native query secuencial por tenant/año | UUID, secuencia global, logica en app |
| Soft-delete | @SQLDelete + @Where(clause = "is_active = true") | @ManyToMany, tabla aparte |
| Response wrapper | Sin wrapper (response directo) | ApiResponse envelope generico |
| Categorias jerarquicas | Nested DTO (`children` list) en el mismo `ItemDTO` | DTO separado por nivel |
| Template products | Genericos por industria (sin marca), copiados al onboarding | Productos especificos con marca |
| Gastos Operativos | Type libre en Factura (ya en codigo) | Enum cerrado |
| Recompute debounce | Redis SETNX + @Scheduled(fixedDelay=30s) | Kafka, in-memory debounce |
| SQL metrics | CTE consolidado (1 round-trip) | 5 queries separadas |

---

## Pendientes

### Inmediato

- [x] Accounting — `core_pymes/accounting/`: MetricaFinanciera + CTE consolidado
- [x] Gasto — `core_pymes/gasto/`: GastoOperativo + CategoriaGasto enum
- [x] Prestamo — `core_pymes/prestamo/`: Prestamo + PagoPrestamo + EstadoPrestamo
- [x] Inversion — `core_pymes/inversion/`: Patrimonio (1 por tenant)
- [x] Venta — `core_pymes/venta/`: VentaDiaria
- [x] Redis debounce — RecomputeDebounceService + @Scheduled
- [x] Testcontainers Redis en AbstractIntegrationTest
- [x] SQL review — division por cero en analisisABC, indices redundantes removidos

### Mediate

- [ ] Reports — dashboard consolidado con KPIs, alertas
- [ ] CRUD de configuracion (edicion por tenant)
- [ ] Movimientos de stock (asociados a facturas+ventas)

### Infraestructura

- [ ] Spring Security (JWT validation local en core)
- [ ] FeignClient para Auth
- [x] Cache con Redis
- [x] Sistema de eventos cross-module (Spring Events)
- [x] Redis debounce para recomputo de metricas/analytics

### Frontend

- [x] Onboarding post-login
- [x] Modulo Core frontend — Productos, Proveedores, Facturas
- [x] Onboarding 2 pasos con preview de categorias
- [x] Onboarding con preview de productos precargados
