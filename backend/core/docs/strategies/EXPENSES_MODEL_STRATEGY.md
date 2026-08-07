# Modelo de Gastos Operativos — Estrategia (Core)

Estado: **backend implementado** (2026-08-02). Frontend (pasos 4-6) pendiente — movido a TO_DO. Aplicada revisión SQL (2026-08-02): `analisisGastoVariable` alineado (solo gastos PAGADA + facturas sin items) + índice covering con `status` (V5).
Objetivo: eliminar el doble conteo de gastos, convertir los gastos operativos en config flexible (presupuesto) y cerrar el hueco del `costo_operativo_diario` dentro del motor de salud financiera.

## Problema

1. **Costo operativo diario desconectado** — se calcula y muestra (`GET /costos/diario`, `tenant_financial_metrics.costo_operativo_diario`), pero `expansionReadiness` y el scoring de salud financiera no lo usan.
2. **Riesgo de doble conteo** — registrar "Luz" en gastos puntuales (`operating_expenses`) y también en gastos fijos recurrentes (`gastos_fijos_recurrentes`) la cuenta 2 veces: una en `operatingExpenses` y otra en el costo diario.
3. **Gastos no pagados cuentan como reales** — el CTE `invoices_opex` suma facturas `GASTO_OPERATIVO` sin importar su status (`REGISTRADA` cuenta igual que `PAGADA`).
4. **Fricción de registro** — una factura `GASTO_OPERATIVO` exige items con producto real, obligando a inventar productos para registrar "pagué la luz" o un salario.

## Modelo de 2 capas (confirmado con el usuario)

| Capa | Qué contiene | Para qué sirve | Fuente |
|------|--------------|----------------|--------|
| **1. Config flexible (presupuesto)** | Colaboradores, gastos fijos recurrentes, días laborables, gastos puntuales | Calcular `costo_operativo_diario` presupuestal, editable, **nunca se ajusta por facturas** | `gastos_fijos_recurrentes`, `collaboradores`, `config_laboral` |
| **2. Registro real (permanente)** | Facturas de proveedores | Gasto real del negocio — **solo cuenta si PAGADA** | `invoices` tipo `GASTO_OPERATIVO` con `status='PAGADA'` |

La realidad (capa 2) puede ser menor o mayor que el presupuesto (capa 1). **No se deduplican**: son números con propósito distinto. Compararlos es exactamente lo que la salud financiera debe hacer.

## Decisiones de diseño (ponytail)

1. **No hay tipo de factura `SALARIO`** — se reutiliza `GASTO_OPERATIVO`. La factura solo guarda monto + descripción ("Salarios — Iván, Mar-Vie, $80"). El cálculo `días × tarifa` es un helper del frontend que lee `GET /costos/collaboradores`. Sin campo de colaborador en DB.
2. **La página Gastos se depreca** — `operating_expenses` deja de contar como gasto real; la página apunta a CostosPage → Gastos Fijos. Sin migración de datos: las filas viejas simplemente dejan de afectar métricas.
3. **Helper de salario solo para colaboradores `DIARIO`** — `días × monto` aplica limpio. Los demás crean la factura con monto manual.
4. **La config nunca se auto-modifica** — pagar una factura no toca `gastos_fijos_recurrentes` ni `collaboradores`. Solo se edita manualmente.
5. **El doble conteo se elimina por diseño** — cada número sale de una fuente distinta y no se suman entre sí. Sin lógica anti-dedup.

## Impacto en métricas

`operatingExpenses` pasa de:
```sql
-- ANTES: gastos puntuales + facturas GASTO_OPERATIVO (sin status)
io.total + o.total
```
a:
```sql
-- DESPUÉS: solo facturas GASTO_OPERATIVO pagadas
io.total   -- WHERE ... AND type = 'GASTO_OPERATIVO' AND status = 'PAGADA'
```
- CTE `opex` (`operating_expenses`) se elimina.
- CTE `costos` (gasto diario) **no se toca** — ya sale solo de la config.
- Las métricas derivadas (margen operativo, neto, scores) se recalculan solas vía el flujo existente (`markMetricsDirty` → debounce 30s → `recalcular`).

## Plan de implementación

> Estado: pasos 1-3, 7a, 7b, 8 ✅ (2026-08-02). Pasos 4-6 (frontend) pendientes — en TO_DO.md.

### Backend (3 cambios)

1. **`MetricasServiceImpl.computeMetrics`** — eliminar CTE `opex` + su cross join; agregar `AND status = 'PAGADA'` a `invoices_opex`.
2. **Pago de factura dispara recálculo de métricas** — `FacturaPagadaEvent` + `FacturaPagadaListener` → `markMetricsDirty` (hoy `FacturaCreadaListener` solo marca analytics en create; el pago no emite evento y las métricas quedarían obsoletas).
3. **`FacturaServiceImpl`** — permitir facturas `GASTO_OPERATIVO` sin items con campo `total` en el request (la validación de productos L136-143 no aplica). El CTE `invoices_opex` ya usa `i.total`.

### Frontend (3 cambios)

4. **`FacturasPage.vue`** — helper "Pago de salario": con tipo `GASTO_OPERATIVO`, select de colaborador (solo DIARIO) + rango de días → precarga `días × tarifa` en el total (editable) y descripción "Salarios — {nombre}, {rango}".
5. **`GastosPage.vue`** — deprecar: banner + enlace a CostosPage → Gastos Fijos.
6. **`useFinancialDashboard.ts`** — la vista de gastos se alimenta de facturas `GASTO_OPERATIVO` pagadas (agrupadas por proveedor) en vez de `gastoService.getAll`, para que coincida con lo que cuenta el motor.

### Salud financiera (2 cambios)

7a. **`AnalyticsServiceImpl.analisisSaludFinanciera`** — leer `getCostoOperativoDiario()` (ya persistido) y venta diaria promedio (`totalIncome / días del período`):
   - `costoDiario > ventaDiaria × 1.2` → crítica `DAILY_COST_CONTROL` "el negocio no cubre su costo diario".
   - `costoDiario < ventaDiaria × 0.8` → señal de expansión `DAILY_COST_COVERED` (alimenta `expansionReadiness`).
   - Cierra el hueco del detalle 1 sin tocar el CTE `costos`.

7b. **Patrimonio conectado al motor** — hoy `patrimony` (`initial_capital`, `start_date`) no lo consume ningún módulo. Se lee vía `PatrimonioRepository` (ya existe) y se combina con `costoOperativoDiario`:
   - `mesesRespaldo = initialCapital / (costoOperativoDiario × 30)`.
   - Crítica `CAPITAL_BURN` si `mesesRespaldo < 1` **y** el negocio pierde dinero (`netMarginPct < 0`): "el capital inicial no cubre ni un mes de costo operativo y estás perdiendo".
   - Señal de expansión `CAPITAL_READINESS` si `mesesRespaldo ≥ 3` **y** `netMarginPct > 0`: "capital suficiente para 3+ meses de operación" (alimenta `expansionReadiness`).
   - Sin tablas nuevas; solo lectura del repositorio existente.
   - ⚠️ **REEMPLAZADO (2026-08-02)** por tiempo de recuperación de inversión (`PAYBACK_RECOVERY`): el concepto "meses que el capital cubre los costos" no refleja la realidad (el capital inicial ya está gastado). Ver [`INVESTMENT_RECOVERY_STRATEGY.md`](INVESTMENT_RECOVERY_STRATEGY.md).

### Tests + docs

8. IT: `GASTO_OPERATIVO` en `REGISTRADA` no cuenta en `operatingExpenses`; en `PAGADA` sí. IT de la señal `DAILY_COST_CONTROL`. Ajustar tests existentes si el cambio de composición los rompe.
9. Actualizar `docs/GAPS.md`, `docs/TO_DO.md` y el daily report.

## Fuera de alcance

- Migración/borrado de `operating_expenses` (queda como tabla muerta; ver `CORE_MIGRATIONS_STRATEGY.md`).
- Tipo de factura `SALARIO` con reporte por empleado — add cuando se necesite nómina.
- Colaboradores SEMANAL/QUINCENAL/MENSUAL con rango de días en el helper.
- `calcularDiario` respetando el período analizado en vez de `LocalDate.now()` — post-MVP.

## Archivos afectados

- `backend/core/src/main/java/core_pymes/accounting/service/impl/MetricasServiceImpl.java`
- `backend/core/src/main/java/core_pymes/invoice/service/impl/FacturaServiceImpl.java`
- `backend/core/src/main/java/core_pymes/invoice/event/FacturaPagadaEvent.java` (nuevo)
- `backend/core/src/main/java/core_pymes/invoice/listener/FacturaPagadaListener.java` (nuevo)
- `backend/core/src/main/java/core_pymes/analytics/service/impl/AnalyticsServiceImpl.java`
- `backend/core/src/main/java/core_pymes/inversion/service/impl/PatrimonioServiceImpl.java` (lectura para 7b)
- `frontend/pymes/src/modules/core/pages/FacturasPage.vue`
- `frontend/pymes/src/modules/core/pages/GastosPage.vue`
- `frontend/pymes/src/modules/core/composables/useFinancialDashboard.ts`
- Tests: `backend/core/src/test/java/core_pymes/integration/`
