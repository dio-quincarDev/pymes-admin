# Motor de Estructura de Costos

> **Estado (2026-07-31):** Implementado (backend + frontend). V2/V3 migraciones, módulo `costos/`, GET /costos/diario, CTE en MetricasServiceImpl, CostosPage.vue + KPI dashboard. Pendiente: señal `DAILY_COST_CONTROL` (Financial Health motor #10 no existe).
>
> **Diferenciador del producto:** Mientras otros sistemas ven gastos planos, este motor razona sobre estructura de costos real — colaboradores con distintas frecuencias de pago, gastos fijos recurrentes con día de ejecución, y un cálculo diario que se compara contra ventas del día para dar ganancia real.

---

## Visión General

El sistema actual trata los gastos operativos como entradas planas (`GastoOperativo` en `core.operating_expenses`). Un gasto de $500 en alquiler es indistinguible de uno de $50 en internet — ambos son solo "gastos". No hay noción de:

- Recurrencia (alquiler paga el día 1, internet el día 15)
- Colaboradores/salarios con distintas frecuencias (diario, semanal, quincenal, mensual)
- Costo operativo diario calculado

Este módulo agrega un **modelo de estructura de costos** que alimenta a los motores analíticos existentes (Financial Health Engine, CTE de Accounting, análisis de margen operativo) y expone el costo diario como métrica central.

---

## Arquitectura

```
costos/
├── controller/
│   ├── CostoApi.java                # Interface con @RequestMapping
│   └── impl/CostoController.java    # @RestController
├── service/
│   ├── CostoService.java            # Interface
│   └── impl/CostoServiceImpl.java   # Lógica + cálculos
├── domain/
│   ├── Collaborador.java            # Entity
│   ├── GastoFijoRecurrente.java     # Entity
│   └── ConfigLaboral.java           # Entity
├── dto/
│   ├── CollaboradorRequest.java     # Record
│   ├── CollaboradorResponse.java    # Record
│   ├── GastoFijoRequest.java        # Record
│   ├── GastoFijoResponse.java       # Record
│   ├── ConfigLaboralRequest.java    # Record
│   ├── ConfigLaboralResponse.java   # Record
│   └── CostoDiarioResponse.java     # Record
├── event/
│   └── CostoStructureChangedEvent.java
├── listener/
│   └── CostoStructureChangedListener.java
└── repository/
    ├── CollaboradorRepository.java
    ├── GastoFijoRepository.java
    └── ConfigLaboralRepository.java
```

### Reciclaje de código existente

| Componente | Referencia | Qué se copia |
|-----------|------------|-------------|
| Entity | `GastoOperativo.java` | `@SQLDelete`, `@Where`, `@Builder.Default isActive`, `UUID tenantId`, `@CreationTimestamp`/`@UpdateTimestamp`, soft-delete |
| Controller | `GastoApi.java` + `GastoController.java` | Interface con `@RequestMapping` + impl con `@RestController implements` + `@PreAuthorize` en writes |
| Mapper | `ProductoMapper.java` | `@Mapper(componentModel = SPRING)` — **MapStruct**, no manual |
| Service | `GastoServiceImpl.java` | `@Transactional`, `@Cacheable`/`@CacheEvict`, tenant guard, event publishing, `@RequiredArgsConstructor` |
| Event | `VentaCreadaEvent.java` | Record con `UUID tenantId`, `String periodo` |
| Listener | `GastoCreadaListener.java` | `@Async @TransactionalEventListener(AFTER_COMMIT)` → `recomputeService.markMetricsDirty()` |
| Ruta | `CorePath.java` | `+COSTOS_ROUTE = "/costos"` |
| Migration | `V12` pattern | `V2` con 3 tablas |
| CTE | `MetricasServiceImpl.java` | Extender el CTE consolidado con fuente `costos` |
| Salud Financiera | `analisisSaludFinanciera()` | Agregar señal `DAILY_COST_CONTROL` |

> **Nota de implementación (2026-07-31):** Se usó `toResponse` manual (patrón `GastoServiceImpl`) en vez de mappers MapStruct — 1:1 simple, menos procesador de anotaciones. Los 3 mappers del plan original se omitieron (ponytail).

### Lo que NO se hace (ponytail)

| No hacer | Razón |
|----------|-------|
| Payroll (SS, aguinaldo, vacaciones, horas extra) | Solo nombre + monto + periodicidad |
| Scheduler que genere gastos automáticos | El costo se calcula sin generar entradas contables |
| Categorías dinámicas | Las 8 existentes alcanzan |
| Relación colaborador → GastoOperativo individual | El motor calcula sin persistir gastos por colaborador |
| Pricing automático en productos | Se integra después, no en este MVP |
| Módulos separados por entidad | 1 módulo, 1 controller, 1 service |

---

## Entidades

### Collaborador

```sql
CREATE TABLE core.collaboradores (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo_pago VARCHAR(20) NOT NULL,   -- DIARIO, SEMANAL, QUINCENAL, MENSUAL
    monto DECIMAL(12,2) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

- `tipo_pago` es enum string (no tabla separada ni JSONB)
- `monto` es el valor del pago en la frecuencia correspondiente (si paga $50/día → monto=50, tipo_pago=DIARIO)
- Soft-delete: `activo = false` en vez de eliminar (preserva histórico)

### GastoFijoRecurrente

```sql
CREATE TABLE core.gastos_fijos_recurrentes (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    descripcion VARCHAR(255),
    dia_ejecucion INT NOT NULL,        -- 1-31
    metodo_pago VARCHAR(50),
    activo BOOLEAN DEFAULT TRUE
);
```

- `categoria` usa los mismos valores que `CategoriaGasto` (ALQUILER, AGUA, LUZ, INTERNET, etc.)
- `dia_ejecucion`: día del mes en que se paga. Motor lo usa para distribuir costo diario si se requiere precisión
- Sin timestamps — es una regla de negocio, no un evento contable

### ConfigLaboral

```sql
CREATE TABLE core.config_laboral (
    tenant_id UUID PRIMARY KEY,
    dias_laborales INT NOT NULL DEFAULT 26
);
```

- 1 fila por tenant (PK = tenant_id)
- `dias_laborales`: divisor del costo mensual para obtener costo diario. Default 26 (lun-sáb)

---

## Motor de Cálculo

### Fórmula

```
costo_fijo_mensual     = SUM(monto de gastos_fijos_recurrentes WHERE activo = true)
costo_salarios_mensual = SUM(
    CASE tipo_pago
        WHEN 'DIARIO'    THEN monto × dias_laborales
        WHEN 'SEMANAL'   THEN monto × 4.33
        WHEN 'QUINCENAL' THEN monto × 2
        WHEN 'MENSUAL'   THEN monto
    END
) WHERE activo = true
costo_operativo_mensual = costo_fijo_mensual + costo_salarios_mensual
costo_operativo_diario  = costo_operativo_mensual / dias_laborales
```

### Factores de periodicidad

| Tipo | Factor | Lógica |
|------|--------|--------|
| DIARIO | `× dias_laborales` | Paga cada día trabajado |
| SEMANAL | `× 4.33` | 52 semanas / 12 meses |
| QUINCENAL | `× 2` | 2 veces al mes |
| MENSUAL | `× 1` | 1 vez al mes |

### CostoDiarioResponse

```json
{
  "costoFijoMensual": 1850.00,
  "costoSalariosMensual": 4200.00,
  "costoSemiFijoMensual": 0,
  "costoOperativoMensual": 6050.00,
  "diasLaborales": 26,
  "costoOperativoDiario": 232.69,
  "ventasHoy": 580.00,
  "gananciaRealEstimada": 347.31
}
```

- `ventasHoy` y `gananciaRealEstimada` vienen de cruce con `VentaDiaria` del día actual
- `gananciaRealEstimada` = `ventasHoy - (ventasHoy × COGS_pct_promedio) - costoOperativoDiario` — aprox rápida, no contable

---

## Endpoints

### CRUD Collaboradores

```
POST   /api/v1/core/costos/collaboradores       # Crear
GET    /api/v1/core/costos/collaboradores        # Listar todos (por tenant)
GET    /api/v1/core/costos/collaboradores/{id}   # Obtener uno
PUT    /api/v1/core/costos/collaboradores/{id}   # Actualizar
DELETE /api/v1/core/costos/collaboradores/{id}   # Soft-delete (activo=false)
```

### CRUD Gastos Fijos Recurrentes

```
POST   /api/v1/core/costos/gastos-fijos          # Crear
GET    /api/v1/core/costos/gastos-fijos           # Listar todos (por tenant)
GET    /api/v1/core/costos/gastos-fijos/{id}      # Obtener uno
PUT    /api/v1/core/costos/gastos-fijos/{id}      # Actualizar
DELETE /api/v1/core/costos/gastos-fijos/{id}      # Soft-delete
```

### Config Laboral

```
GET    /api/v1/core/costos/configuracion         # Obtener (get-or-create con default 26)
PUT    /api/v1/core/costos/configuracion         # Actualizar días laborales
```

### Costo Diario

```
GET    /api/v1/core/costos/diario                # Calcular y devolver breakdown
```

---

## Integración con Motores Existentes

### MetricasServiceImpl (CTE consolidado)

El CTE existente en `MetricasServiceImpl.ejecutarCompleto()` actualmente hace:

```sql
WITH sales AS (...), invoices_cost AS (...), invoices_opex AS (...), opex AS (...), loan_pay AS (...)
```

Agregar:

```sql
costos AS (
    SELECT
        COALESCE((SELECT SUM(monto) FROM core.gastos_fijos_recurrentes WHERE tenant_id = ? AND activo = true), 0) AS costo_fijo_mensual,
        COALESCE((SELECT SUM(
            CASE tipo_pago
                WHEN 'DIARIO' THEN monto * (SELECT dias_laborales FROM core.config_laboral WHERE tenant_id = ?)
                WHEN 'SEMANAL' THEN monto * 4.33
                WHEN 'QUINCENAL' THEN monto * 2
                WHEN 'MENSUAL' THEN monto
                ELSE monto
            END
        ) FROM core.collaboradores WHERE tenant_id = ? AND activo = true), 0) AS costo_salarios_mensual
)
```

El `costo_operativo_diario` se calcula en Java post-CTE y se persiste en `MetricasFinanciera` como nuevo campo `costoOperativoDiario`.

### Financial Health Engine (Motor #10)

Agregar señal:

| Señal | Tipo | Criterio |
|-------|------|----------|
| `DAILY_COST_CONTROL` | 🟢 Inversión | `ventasHoy > costoOperativoDiario × 1.2` en 3+ de los últimos 7 días |

Indica que el negocio genera suficiente ingreso diario para cubrir sus costos operativos diarios con un margen del 20%.

### OPEX_CREEP

La señal `OPEX_CREEP` existente (🔴 Crítica) se vuelve más precisa: ahora cruza gastos fijos reales + salarios vs ingresos, no gastos operativos puntuales que pueden tener ruido mensual.

---

## Frontend

### CostosPage.vue

Nueva página con 3 secciones en tabs:

1. **Colaboradores** — Tabla: nombre, tipo pago, monto. CRUD inline o dialog.
2. **Gastos Fijos** — Tabla: categoría, descripción, monto, día ejecución. Agrupados por categoría.
3. **Configuración** — Input: días laborales (default 26).

Header sticky con resumen: **"Costo operativo diario: $232.69 → Ventas hoy $580 → Ganancia real estimada $347"**

### Dashboard

Nuevo stat strip item: **"Costo/Día $232"** con color basado en ratio ventas/costo. Integrado al layout existente de KpiCards.

---

## Eventos

Un solo evento para toda la estructura de costos:

```java
public record CostoStructureChangedEvent(UUID tenantId, String periodo) {}
```

Publicado por `CostoServiceImpl` cuando cualquier entidad del módulo se crea, actualiza o elimina. Un solo listener:

```java
@Component
@RequiredArgsConstructor
public class CostoStructureChangedListener {
    private final RecomputeDebounceService recomputeService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCostoStructureChanged(CostoStructureChangedEvent event) {
        recomputeService.markMetricsDirty(event.tenantId(), event.periodo());
    }
}
```

El debounce Redis existente (30s, SETNX) maneja la deduplicación.

---

## Migración Flyway — V2

```sql
-- V2__costos_engine.sql

CREATE TABLE IF NOT EXISTS core.collaboradores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo_pago VARCHAR(20) NOT NULL CHECK (tipo_pago IN ('DIARIO', 'SEMANAL', 'QUINCENAL', 'MENSUAL')),
    monto DECIMAL(12,2) NOT NULL CHECK (monto > 0),
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS core.gastos_fijos_recurrentes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    monto DECIMAL(12,2) NOT NULL CHECK (monto > 0),
    descripcion VARCHAR(255),
    dia_ejecucion INT NOT NULL CHECK (dia_ejecucion BETWEEN 1 AND 31),
    metodo_pago VARCHAR(50),
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS core.config_laboral (
    tenant_id UUID PRIMARY KEY,
    dias_laborales INT NOT NULL DEFAULT 26 CHECK (dias_laborales BETWEEN 1 AND 31)
);

CREATE INDEX IF NOT EXISTS idx_collaboradores_tenant ON core.collaboradores(tenant_id);
CREATE INDEX IF NOT EXISTS idx_gastos_fijos_tenant ON core.gastos_fijos_recurrentes(tenant_id);
```

### MetricasFinanciera — nuevo campo

V3: agregar columna a `tenant_financial_metrics`:

```sql
ALTER TABLE core.tenant_financial_metrics
ADD COLUMN IF NOT EXISTS costo_operativo_diario DECIMAL(12,2);
```

---

## Dependencias

Ninguna nueva. Todo usa:
- Spring Boot Starter Data JPA (repositorios)
- Spring Boot Starter Web (endpoints)
- Lombok + MapStruct (ya en pom.xml)
- PostgreSQL JDBC (ya en pom.xml)

---

## Orden de Implementación

| Paso | Descripción | Archivos |
|------|------------|----------|
| 1 | V2 + V3 migration | 2 SQL |
| 2 | 3 entities + TipoPago enum | 4 Java |
| 3 | 3 repositories | 3 Java |
| 4 | 7 DTO records | 7 Java |
| 5 | Event + Listener | 2 Java |
| 6 | CostoService + impl | 2 Java |
| 7 | CostoApi + CostoController | 2 Java |
| 8 | CorePath constant | 1 edit |
| 9 | Extender MetricasServiceImpl CTE | 1 edit |
| 10 | Agregar DAILY_COST_CONTROL a Financial Health | ⏳ Pendiente (motor #10 no existe) |
| 11 | Tests: JPA + unit | 2 Java |
| 12 | Frontend: CostosPage.vue | 1 Vue |
| 13 | Dashboard: KPI Costo/Día | 2 edits |

---

## Registro

| Fecha | Evento |
|-------|--------|
| 2026-07-30 | Estrategia definida y documentada. |
| 2026-07-31 | Implementado backend (V2/V3, módulo `costos/`, CTE) + frontend (CostosPage.vue, KPI dashboard). 160 unit + 22 integration verdes. Pendiente: `DAILY_COST_CONTROL`. |
