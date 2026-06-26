# Estrategia Arquitectónica: Core Service Event-Driven

> **REALITY CHECK (2026-06):** Este documento describe la visión completa. Lo implementado difiere en:
> - **Paquete base**: `core_pymes.*`, no `com.pymes.core`
> - **Módulos existentes**: `setup/`, `product/`, `invoice/`, `analytics/` (no `configuracion/inventario/facturas/contabilidad/reportes`)
> - **Analytics**: Implementado con 6 motores CTE + listener conectado a `FacturaCreadaEvent`
> - **Accounting (contabilidad)**: NO implementado — diseño pendiente
> - **Ventas**: NO implementado
> - **Reportes**: NO implementado
> - **Estándares de código**: `@SQLDelete` + `@Where` para soft-delete, MapStruct, Java records para DTOs
> - **Decisión gastos**: Ya implícita en código — `Factura.type` es libre (Opción A)
> - **Refactor precios**: Ya aplicado — `ItemFactura` tiene `presentacionId` + `conversionFactor`
>
> ---

## 1. Visión General

Core Service es el corazón del SaaS. Maneja operaciones del negocio con arquitectura event-driven, aprovechando Virtual Threads de Java 21 para concurrencia eficiente.

```
Frontend (PWA)
    ↓ JWT
Gateway (8080)
    ↓
Core Service (8082)
├── Módulo: Setup (configuración/inventario → setup/product)
├── Módulo: Invoice (facturas + proveedores)
├── Módulo: Analytics (6 motores de análisis de gastos)
├── ⬜ Módulo: Accounting (contabilidad — NO IMPLEMENTADO)
├── ⬜ Módulo: Reportes (NO IMPLEMENTADO)
└── ⬜ Módulo: Ventas (NO IMPLEMENTADO)

(todos comunican vía Spring Events - no bloqueantes)
```

---

## 2. Módulos del Core Service

### Módulo: Setup (`core_pymes/setup/`)
**Responsabilidad:** Gestionar setup inicial del tenant ✅ IMPLEMENTADO

Contiene:
- Plantillas precargadas por industria (8 industrias)
- Categorías jerárquicas (3 niveles, preview con árbol)
- Unidades, Ubicaciones (template tables)
- Onboarding lazy (primer GET) + POST para completar

Eventos que genera:
- (ninguno aún — no hay cross-service events)

---

### Módulo: Product (`core_pymes/product/`)
**Responsabilidad:** Catálogo de productos y presentaciones ✅ IMPLEMENTADO

Contiene:
- Producto (nombre, SKU, categoría, unidad base, soft-delete)
- Presentación (factor de conversión: Caja=24, etc)

Eventos que genera:
- ProductoCreadoEvent
- PresentacionCreadaEvent

---

### Módulo: Invoice (`core_pymes/invoice/`)
**Responsabilidad:** Facturas de compra y proveedores ✅ IMPLEMENTADO

Contiene:
- Factura (número auto-generado, items, tipo, estado)
- ItemFactura (producto, presentación, factor conversión, precio)
- Proveedor

Eventos que genera:
- FacturaCreadaEvent → escuchado por AnalyticsListener
- FacturaPagadaEvent → **huérfano (nadie escucha)**

---

### Módulo: Analytics (`core_pymes/analytics/`)
**Responsabilidad:** 6 motores de análisis de gastos ✅ IMPLEMENTADO

Contiene:
- AnalisisGasto (JSONB por tenant/periodo — expense_analysis table)
- 6 motores CTE: ABC, tendencias, márgenes, opex, proyección, alertas
- Listener conectado a FacturaCreadaEvent → ejecuta análisis async

Eventos que genera:
- (ninguno — es sink)

---

### ⬜ Módulo: Accounting (NO IMPLEMENTADO)
Diseñado en ACCOUNTING_METRICS_IMPLEMENTATION.md pero sin código:
- MetricasFinanciera, tenant_period_metrics, listeners, alertas

### ⬜ Módulo: Reportes (NO IMPLEMENTADO)
### ⬜ Módulo: Ventas (NO IMPLEMENTADO)
### ⬜ Módulo: Patrimonio/Préstamos (NO IMPLEMENTADO)

---

## 3. Flujo de Eventos Completo

### Escenario 1: Tenant Se Registra

```
1. Auth Service crea Tenant
   └── Publica: TenantCreated (tenant_id, industria)

2. Módulo: Configuración escucha TenantCreated
   └── Busca plantilla por industria
   └── Copia categorías a ConfiguracionTenant
   └── Copia unidades a ConfiguracionTenant
   └── Copia ubicaciones a ConfiguracionTenant
   └── Publica: ConfiguracionCargada (tenant_id, categorias, unidades, ubicaciones)
   
3. Módulo: Reportes escucha ConfiguracionCargada
   └── Inicializa dashboard para tenant
   └── Prepara template de reportes
```

**Timeline:** Auth espera respuesta de onboarding, pero Configuración carga async en background.

---

### Escenario 2: Tenant Registra Factura (Compra) — FLUJO REAL ACTUAL

```
1. Invoice: POST /api/v1/core/facturas
   └── Valida datos
   └── Persiste Factura + Items (incluye presentacionId, conversionFactor)
   └── Publica: FacturaCreadaEvent

2. [Async] FacturaCreadaListener → AnalyticsService.ejecutarCompleto()
   └── Ejecuta 6 motores CTE en PostgreSQL:
       - ABC de Gastos (Pareto)
       - Tendencia de Precios (media móvil 90d)
       - Impacto en Márgenes
       - Costo Operativo como % de Ventas
       - Proyección de Gastos (30/60/90d)
       - Alertas de Variación >15%
   └── Persiste resultado en expense_analysis (JSONB)

3. ⬜ Contabilidad: NO IMPLEMENTADO — no recalcula márgenes
4. ⬜ Reportes: NO IMPLEMENTADO — no actualiza dashboard
5. ⬜ Inventario/Stock: NO IMPLEMENTADO — no hay control de existencias
```

**Timeline:** Async vía Virtual Threads. Solo analytics se ejecuta.

---

### Escenario 3: Registrar Gasto Operativo — YA DECIDIDO

**Decisión implícita en código:** Opción A.
- `Factura.type` es String: puede ser `"FACTURA"`, `"GASTO_OPERATIVO"`, etc.
- No hay validación enum — el frontend decide el tipo.
- ⬜ Pendiente: listener que diferencie tipos para accounting.

---

### Escenario 4: Registrar Venta — PENDIENTE

⬜ No implementado. No hay endpoint `/ventas`, no hay entidad Venta.
A esperar decisión: ¿Opción A (módulo Ventas) o B (movimientos)?

---

## 4. Arquitectura Técnica

### Spring Events (Broker)

```
ApplicationEventPublisher (publicador)
  └── Publica eventos de dominio

@EventListener (suscriptores)
  └── Escuchan eventos

Virtual Threads (ejecutores)
  └── Cada listener corre en thread virtual
  └── No bloqueante, eficiente
```

**Por qué Spring Events:**
- Simple, sin dependencias externas
- Suficiente para MVP
- Luego migrar a RabbitMQ sin cambiar código (eventos son agnósticos)

---

### Virtual Threads Strategy

**Dónde se usan:**

1. **Event Listeners:**
   - Automático en Spring: `@EventListener` + `@Async` → Virtual Thread
   - Cada listener es independiente, no bloquea otros

2. **Procesamiento Pesado:**
   - Cálculos de márgenes (loops, agregaciones)
   - Generación de reportes
   - Validaciones complejas
   - Delegados a `ExecutorService` con virtual threads

3. **I/O:**
   - Queries a BD
   - Feign calls a Auth Service
   - Se benefician automáticamente de virtual threads

**Ventaja:** Con Java 21, virtual threads hacen que concurrencia sea trivial. 1000 eventos simultáneos = trivial.

---

### Transaccionalidad

**Problema:** Eventos async pueden fallar, ¿qué pasa?

**Estrategia:**

1. **Publicador es transaccional:**
   ```
   Factura persiste en BD (COMMIT)
   └── Luego publica evento FacturaCreada
   └── Si evento falla, Factura ya está guardada
   ```

2. **Listeners son idempotentes:**
   ```
   Inventario recibe FacturaCreada(factura_id)
   └── Busca factura por ID
   └── Si ya procesó este ID, ignora (idempotencia)
   └── Si falla, puede reintentarse sin duplicar
   ```

3. **Dead Letter Queue (futuro):**
   ```
   Si listener falla, evento no se pierde
   └── Se guarda en table fallidos
   └── Admin puede reintentarlo manualmente
   ```

---

## 5. Estructura de Paquetes (Actual)

```
backend/core/src/main/java/core_pymes/
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
├── setup/                          ✅ Implementado
│   ├── controller/ → SetupApi.java + impl/SetupController.java
│   ├── domain/ → TenantSetup.java
│   ├── dto/ → SetupResponse.java (ItemDTO con jerarquía)
│   ├── mapper/ → SetupMapper.java
│   ├── repository/ → TenantSetupRepository.java
│   └── service/ → SetupService.java + impl/SetupServiceImpl.java
│
├── product/                        ✅ Implementado
│   ├── controller/ → ProductoApi.java + impl/ProductoController.java
│   ├── domain/ → Producto.java, Presentacion.java
│   ├── dto/ → ProductoRequest/Response, PresentacionRequest/Response
│   ├── event/ → ProductoCreadoEvent, PresentacionCreadaEvent
│   ├── mapper/ → ProductoMapper.java
│   ├── repository/ → ProductoRepository, PresentacionRepository
│   └── service/ → ProductoService + impl/ProductoServiceImpl
│
├── invoice/                        ✅ Implementado
│   ├── controller/ → FacturaApi, ProveedorApi + impl
│   ├── domain/ → Factura, ItemFactura, Proveedor
│   ├── dto/ → FacturaRequest/Response, ProveedorRequest/Response
│   ├── event/ → FacturaCreadaEvent, FacturaPagadaEvent
│   ├── listener/ → FacturaCreadaListener (→ AnalyticsService)
│   ├── mapper/ → FacturaMapper.java
│   ├── repository/ → FacturaRepository, ProveedorRepository
│   └── service/ → FacturaService + impl/FacturaServiceImpl
│
└── analytics/                      ✅ Implementado
    ├── controller/ → AnalyticsApi + impl/AnalyticsController
    ├── domain/ → AnalisisGasto.java (JSONB)
    ├── dto/ → AnalyticsResponse.java
    ├── mapper/ → AnalyticsMapper.java
    ├── repository/ → AnalisisGastoRepository.java
    └── service/ → AnalyticsService + impl/AnalyticsServiceImpl
                    (6 motores CTE: ABC, tendencia, margen, opex, proyección, alertas)

⬜ accounting/  — NO IMPLEMENTADO (ver ACCOUNTING_METRICS_IMPLEMENTATION.md)
⬜ reportes/    — NO IMPLEMENTADO
⬜ ventas/      — NO IMPLEMENTADO
```

---

## 6. Decisiones Técnicas Justificadas

### 1. Spring Events vs RabbitMQ

| Aspecto | Spring Events | RabbitMQ |
|--------|---------------|----------|
| Infraestructura | Embebido en app | Externa |
| MVP | OK | Overkill |
| Persistencia | No | Sí |
| Escalabilidad | Limitada (1 instancia) | Ilimitada |
| Latencia | ms | ms+ |

**Decisión:** Spring Events ahora, RabbitMQ en Fase 3+ cuando tengas múltiples instancias de Core.

---

### 2. Virtual Threads

**Decisión:** Usar `@Async` con virtual threads automáticamente.

```
Spring 3.2+ detecta virtual threads disponibles
└── ApplicationEventPublisher.publishEvent() es non-blocking
└── Listeners procesan en paralelo, sin threads costosos
```

**Impacto:** Concurrencia trivial con recursos mínimos.

---

### 3. Transaccionalidad

**Decisión:** Event Sourcing básico (no complejo).

```
1. Operación persiste en BD (COMMIT)
2. Evento se publica DESPUÉS del COMMIT
3. Listeners procesan evento (pueden fallar sin afectar persistencia)
4. Si listener falla, se reintenta (idempotencia en listener)
```

---

### 4. Ubicación de Gastos Operativos

**Pendiente tu respuesta:** ¿Opción A o B del Escenario 3?

---

### 5. Ubicación de Ventas

**Pendiente tu respuesta:** ¿Opción A o B del Escenario 4?

---

## 7. Flujo de Cumplimiento de Requerimientos

### Márgenes Financieros

```
FacturaCreada (evento)
  ↓
Contabilidad suma a CostoMercancia
  ↓
Contabilidad suma GastosOperativos (de Facturas o módulo)
  ↓
Contabilidad suma Ingresos (de Ventas)
  ↓
Contabilidad calcula:
  - Margen Bruto % = (Ingresos - CostoMercancia) / Ingresos × 100
  - Margen Bruto USD = Ingresos - CostoMercancia
  - Margen Operativo % = (Ingresos - CostoMercancia - GastosOp) / Ingresos × 100
  - Margen Operativo USD = ...
  - Margen Neto % = ...
  - EBITDA
  - Punto Equilibrio
  ↓
Contabilidad publica: MetricasCalculadas
  ↓
Reportes escucha y muestra en dashboard
```

---

### Alertas Informativas

```
MetricasCalculadas
  ↓
Contabilidad valida:
  - Si Margen Neto < 5% → AlertaMargenBajo
  - Si FlujoCaja próx 30 días es negativo → AlertaFlujoCajaNegativo
  - Si Stock < Mínimo → StockCrítico (desde Inventario)
  ↓
Reportes escucha alertas
  ↓
Dashboard muestra alertas activas (lista, colores, acciones sugeridas)
```

---

## 8. Decisiones Pendientes (Reales)

- [x] Gastos Operativos → Opción A (type libre en Factura) — ya en código
- [ ] Ventas: ¿Opción A (módulo Ventas) o B (movimientos)?
- [ ] Accounting: ¿implementar MetricasFinanciera con upsert por período?
- [ ] Reportes: ¿endpoint dashboard o frontend calcula desde data cruda?
- [ ] `FacturaPagadaEvent`: está publicado pero huérfano — ¿quién debe escucharlo?
