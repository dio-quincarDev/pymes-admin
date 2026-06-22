# PROGRESS — Core Service

Registro de lo implementado y lo pendiente

---

## Implementado ✅

### Módulo Setup
- `TenantSetup` entidad JPA (tenantId, industry, onboardingCompleted)
- `TenantSetupRepository` (findByTenantId, existsByTenantId)
- `SetupService` interface + `SetupServiceImpl` (getOrInitialize lazy, completeOnboarding)
- `SetupApi` controller interface + `SetupController` impl
  - `GET /api/v1/core/setup/{tenantId}` — lazy init si no existe
  - `POST /api/v1/core/setup/{tenantId}/onboarding` — completa onboarding con industry
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

### Arquitectura
- Estructura modular: `setup/` contiene controller/domain/service/repository
- Paquete base: `core_pymes`
- Controller interface+impl dentro del módulo (no plano)

---

## Pendiente 🚧

### Inmediato
- [ ] CRUD de categorías, ubicaciones, unidades para Configuración

### Implementado en esta sesión
- [x] GlobalExceptionHandler (@RestControllerAdvice — 404/400/500)
- [x] Industry validation en completeOnboarding (JdbcTemplate + industries table)
- [x] Seeds se usan globales (sin copia a tenant-specific tables)
- [x] Manejo de errores global (RestControllerAdvice)
- [x] Expansión seed: 3 → 8 industrias (ferreteria, mini_super, taller_mecanico, farmacia, default)
- [x] Nuevas tablas: `template_units`, `template_movement_reasons`, `template_payment_methods` (creación DDL + datos)
- [x] SQL review: rename `tipo` → `movement_type`, índices en `industry_code` para las 3 tablas nuevas

### Próximos módulos (orden sugerido)
- [ ] **Inventory** — productos, presentaciones, stock, movimientos
- [ ] **Invoices** — facturas, items, proveedores
- [ ] **Accounting** — márgenes, COGS, flujo de caja
- [ ] **Reports** — dashboards, KPIs, alertas

### Infraestructura pendiente
- [ ] Spring Security (JWT validation local si se requiere)
- [ ] FeignClient para Auth (solo cuando core consuma endpoints de auth)
- [ ] Cache con Redis
- [ ] Systema de eventos cross-module (Spring Events)

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
