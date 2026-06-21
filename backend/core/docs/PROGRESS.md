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
- 3 industrias seedadas: restaurante (59 cats, 5 locs), bares (61, 5), salón belleza (68, 4)

### Testing
- `SetupServiceImplTest`: 4 tests unitarios con Mockito
- `AbstractIntegrationTest`: base class con Testcontainers PostgreSQL
- `SetupSeedIntegrationTest`: 5 tests de integración (seed data + API setup)
- `CoreApplicationTests`: smoke test de contexto con Testcontainers
- Testcontainers 1.21.4 agregado a pom.xml

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
