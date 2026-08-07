# Módulos Futuros — Blueprints

> **Estado (2026-07):** 5 de 6 modulos implementados. Conservar como referencia. Ver `FUTURE.md` en `frontend/pymes/docs/` para el plan de pantallas.

---

## Hito: Pivot de Paradigma (2026-06)

> **ANTES:** Control de stock físico (Kárdex, existencias, movimientos de almacén).
> **AHORA:** Motor de análisis de gastos + normalización matemática de precios.

### Qué cambió
- Eliminado: entidad Inventario, stock mínimo, control de existencias
- Agregado: `conversionFactor` en ItemFactura, normalización `Precio Base = Precio Compra / Factor`
- Catálogo: plantillas por industria (8), productos como `InsumoTemplate` genérico

### Por qué
- Pymes no necesitan kárdex — necesitan saber dónde va su dinero
- Stock físico = CRUD complejo, errores de conteo, sobrecarga operativa
- Normalización matemática = comparación exacta entre proveedores, análisis histórico

### Referencia
- `STRATEGY_REFACTOR_ANALYSIS.md` fue consolidado aquí
- Ya aplicado en código: `ItemFactura` tiene `presentacionId` + `conversionFactor`

### Flag en tabla de decisiones (CORE.md)
```
| 2026-06 | Pivot: Stock físico → Motor contable | Sin inventario. ItemFactura usa conversionFactor |
```

---

## 1. Accounting (Contabilidad)

> **Diseño completo.** IMPLEMENTADO — `MetricasServiceImpl.java` con CTE consolidado. Falta frontend.

### Visión
Motor de cálculo matriz que consume datos de transacciones del Analytics y produce métricas de negocio listos para UI.

### Arquitectura
```
/core_pymes/accounting/
├── controller/
│   ├── MetricasApi (interface)
│   └── impl/MetricasController
├── service/
│   ├── MetricasService (interface)
│   └── impl/MetricasServiceImpl
├── mapper/MetricasMapper
├── dto/MetricasResponse
├── domain/MetricasFinanciera.java
└── repository/MetricasRepository
```

### Schema

```sql
CREATE TABLE core.tenant_period_metrics (
    tenant_id UUID NOT NULL,
    periodo VARCHAR(7) NOT NULL, -- YYYY-MM
    total_ingresos DECIMAL(15,2) DEFAULT 0,
    total_egresos DECIMAL(15,2) DEFAULT 0,
    costo_mercaderia DECIMAL(15,2) DEFAULT 0,
    gastos_operativos DECIMAL(15,2) DEFAULT 0,
    margen_bruto_pct DECIMAL(10,4) DEFAULT 0,
    margen_bruto_usd DECIMAL(15,2) DEFAULT 0,
    margen_operativo_pct DECIMAL(10,4) DEFAULT 0,
    margen_operativo_usd DECIMAL(15,2) DEFAULT 0,
    margen_neto_pct DECIMAL(10,4) DEFAULT 0,
    margen_neto_usd DECIMAL(15,2) DEFAULT 0,
    ebitda DECIMAL(15,2) DEFAULT 0,
    punto_equilibrio INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (tenant_id, periodo),
    CONSTRAINT fk_metrics_tenant FOREIGN KEY (tenant_id) REFERENCES auth.tenants(id) ON DELETE CASCADE
);
```

```sql
CREATE TABLE core.daily_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    evento VARCHAR(100) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    entidad_id UUID,
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### Endpoints
```java
public interface MetricasApi {
    ResponseEntity<MetricasResponse> consultar(UUID tenantId, String periodo);
    ResponseEntity<MetricasResponse> recalcular(UUID tenantId, String periodo);
    ResponseEntity<MetricasResponse> dashboard(UUID tenantId, String periodo);
}
```

### Eventos y Listeners

```mermaid
graph TD
    A[FacturaCreadaEvent] --> B[MetricasService] --> C[TenantPeriodoMetrics]
    A --> D[AnalisisGastoListener]
    B --> E[ContabilidadEventListener]
    B --> F[ReportesEventListener]
    C --> G[MetricasController consultar()]
    C --> H[MetricasController dashboard()]
    I[FacturaPagadaEvent] --> B
    J[VentaRegistradaEvent] --> B
    F --> H
    E --> H
    E --> K[AlertaMargenBajoEvent]
    E --> L[AlertaFlujoCajaNegativoEvent]
    K --> M[ReportesEventListener]
    L --> M
    M --> H
```

**ContabilidadEventListener:** Escucha FacturaCreada, FacturaPagada, VentaRegistrada, GastoOperativoRegistrado → actualiza agregados → publica MetricasCalculadas → evalúa umbrales (margen_neto < 5%)

**ReportesEventListener:** Escucha MetricasCalculadas, AlertaMargenBajo, AlertaFlujoCajaNegativo → actualiza dashboard → publica ReporteActualizado

### SQL Optimización — Upsert
```java
@Override
@Transactional
public MetricasFinanciera ejecutarCompleto(UUID tenantId, String periodo) {
    var existing = repository.findByTenantIdAndPeriodo(tenantId, periodo);
    if (existing != null) {
        existing.setTotalIngresos(nuevosIngresos);
        existing.setTotalEgresos(nuevosEgresos);
        // ... recalcula márgenes
        return repository.save(existing);
    }
    return repository.save(MetricasFinanciera.builder()
        .tenantId(tenantId).periodo(periodo)...build());
}
```

### Tests (diseñados)
- `MetricasServiceImplTest` (12 tests): motores contables + safe lectura + idempotente
- `MetricasRepositoryTest` (10 tests): CRUD + tenant isolation
- `ContabilidadEventListenerTest` (8 tests): FacturaCreada → métricas, idempotencia
- `ReporteDashboardTest` (6 tests): MetricasResponse → DashboardDto

### Configuración
| Propiedad | Valor |
|----------|-------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/core` |
| `spring.flyway.schemas` | `core` |
| `app.metrics.margen_bajo_umbral` | `5.0` |

---

## 2. Ventas (Registro Manual)

> **Opción A (módulo propio) IMPLEMENTADA.** `VentaDiaria.java` + CRUD completo. Falta frontend.

### Diseño provisional (Opción A)

```
POST /api/core/v1/ventas
  ├── Fecha
  ├── Monto bruto
  ├── Notas (opcional)
  └── Publica: VentaRegistrada

GET /api/core/v1/ventas
  └── Lista (por rango fecha, totalizado por día/semana/mes)

PUT /api/core/v1/ventas/{id}
  └── Edita (solo si no está "cerrada")

DELETE /api/core/v1/ventas/{id}
  └── Elimina
```

---

## 3. Reportes (Dashboard)

> **Sin diseño formal.** Pendiente. Ver `frontend/pymes/docs/FUTURE.md` para plan de pantallas.

### Diseño provisional

```
GET /api/core/v1/reportes/dashboard
  └── Total inversión actual
  └── Alertas activas (stock crítico, margen bajo)
  └── Últimas 10 facturas / ventas
  └── Resumen márgenes mes actual

GET /api/core/v1/reportes/contabilidad
  └── Márgenes, flujo caja, KPIs
```

---

## 4. Patrimonio e Inversión

> **Diseño completo.** IMPLEMENTADO — `Patrimonio.java` (get-or-create) + `Prestamo.java` + `PagoPrestamo.java`. ROI pendiente.

### Entidades

**Patrimonio** (una por tenant)
```
├── tenant_id
├── capital_inicial (editable)
├── fecha_inicio
├── notas
└── activo (boolean)
```

**Préstamo** (múltiples por tenant)
```
├── id, tenant_id
├── nombre, monto, tasa_interes_mensual, plazo_meses
├── fecha_inicio, fecha_vencimiento
├── estado (activo, pagado, cancelado)
├── saldo_pendiente, notas
```

**GastoPréstamo** (registro de pagos)
```
├── id, prestamo_id
├── monto_pago, interes_pagado, capital_pagado
├── fecha_pago, metodo_pago
```

**IndicadorROI** (calculada, no editable)
```
├── tenant_id, periodo
├── ganancia_acumulada, roi_porcentaje
├── capital_comprometido, saldo_deuda_pendiente
├── meses_para_recuperar, proyeccion_recuperacion_fecha
```

### Endpoints
```
POST   /api/core/v1/patrimonios
GET    /api/core/v1/patrimonios/{tenant_id}
PUT    /api/core/v1/patrimonios/{tenant_id}

POST   /api/core/v1/prestamos
GET    /api/core/v1/prestamos?tenant_id=...
GET    /api/core/v1/prestamos/{id}
PUT    /api/core/v1/prestamos/{id}
DELETE /api/core/v1/prestamos/{id}
POST   /api/core/v1/prestamos/{id}/pagos
GET    /api/core/v1/prestamos/{id}/pagos

GET    /api/core/v1/roi?tenant_id=...&periodo=mensual
GET    /api/core/v1/roi/timeline
GET    /api/core/v1/roi/comparativa
```

### Flujo de Eventos

```
PatrimonioActualizado → PatrimonioEventListener → recalcula IndicadorROI
PrestamoAgregado → PrestamoEventListener → suma capital comprometido
PagoPrestamoProcesado → PrestamoEventListener → actualiza saldo deuda
MetricasCalculadas → PatrimonioEventListener → lee ganancia + capital + deuda → calcula ROI
```

### Cálculos

**ROI %:**
```
ROI % = (Ganancia Acumulada / Capital Comprometido) × 100
Capital Comprometido = Capital Inicial + Sum(préstamos activos)
```

**Meses para Recuperar:**
```
Velocidad de ganancia = Ganancia Acumulada / Meses Transcurridos
Meses faltantes = Capital Comprometido / Velocidad
Total estimado = Transcurridos + faltantes
```

**Cuota Mensual (Amortización):**
```
Cuota = P × [i(1+i)^n] / [(1+i)^n - 1]
P = Monto préstamo
i = Tasa mensual
n = Plazo en meses
```

### Validaciones

| Acción | Validación |
|--------|-----------|
| Crear Patrimonio | Capital > 0 |
| Crear Préstamo | Monto > 0, Tasa > 0, Plazo > 0 |
| Editar Préstamo | Solo si no tiene pagos registrados |
| Pagar Préstamo | Monto <= saldo + interés del mes |
| Eliminar Préstamo | Solo si saldo = monto inicial |
| Eliminar Patrimonio | Solo si no hay préstamos activos |

### Visualización
- Dashboard Patrimonio: Capital, Apalancamiento, ROI actual, Saldo deuda, Proyección
- Gráfico Timeline ROI: línea ROI % vs 100% (recuperación)
- Gráfico Deuda vs Ganancia: áreas que se cruzan en punto de equilibrio
- Tabla Amortización: mes a mes con desglose interés/capital

---

## 5. Refactor: Motor de Inteligencia de Compras

> **Parcialmente aplicado.** `ItemFactura` tiene `presentacionId` + `conversionFactor`. Pendiente renombrar Producto → InsumoTemplate.

### Cambio de Paradigma

**ANTES (descartado):**
- Controlar cantidades físicas en almacenes (Inventario)
- Registro estricto de entradas, salidas, mermas y ajustes
- CRUD manual y complejo de productos específicos por cliente

**AHORA (validado):**
- **Cero Stock:** No se calculan existencias disponibles
- **Catálogo Precargado:** Cliente selecciona insumo base de plantilla de su industria
- **Normalización Matemática:** Unidades/presentaciones = factores de conversión para unificar precios históricos

### Algoritmo Core

```
Precio Unitario Base = Precio Unitario de Compra / Factor de Conversión
```

**Ejemplo:**
- Proveedor A: 1 Caja de 25 Lb a $48.75, Factor = 25 → $1.95/Lb
- Proveedor B: Libras sueltas a $1.90, Factor = 1 → $1.90/Lb
- Resultado: Proveedor B es más barato ($0.05/Lb de ahorro)

### Los 6 Motores Analíticos

1. **ABC de Gastos (Pareto):** Insumos categoría A = 80% del presupuesto
2. **Tendencia de Precios:** Precio actual vs promedio móvil 90 días
3. **Impacto en Márgenes:** Fluctuación de precios de compra sobre márgenes
4. **Costo Operativo % Ventas:** Facturas de compra vs ingresos por ventas
5. **Proyección de Gastos:** Simulación lineal 30/60/90 días
6. **Alertas de Anomalías:** Variación > 15%, primer registro de proveedor

### Ruta de Refactorización

**Fase 1: Limpieza (`inventario`)**
- Retirar lógica de existencias, stocks mínimos, almacenes, kárdex
- Renombrar Producto → InsumoTemplate

**Fase 2: Adaptación (`facturas`)**
- ItemFactura vincula a InsumoTemplate (no inventario físico)
- DTO recibe unidad de compra + factor de conversión

**Fase 3: Motor Analítico (`contabilidad`)**
- Persistencia consolidada por tenant/periodo
- Queries nativos de agregación en PostgreSQL

---

## 6. Decisiones Pendientes

- [x] Ventas: Opción A (módulo propio) — implementada
- [x] Accounting: upsert por período — implementado (UNIQUE constraint)
- [ ] Reportes: ¿endpoint dashboard o frontend calcula desde data cruda?
- [ ] Renombrar formalmente Producto → InsumoTemplate
