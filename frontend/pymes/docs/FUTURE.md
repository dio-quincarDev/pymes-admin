# FUTURE.md — Frontend: Modulos Core

> **Fecha:** 2026-07-09
> **Objetivo:** Documentar los modulos backend implementados y las pantallas frontend que faltan para conectar con ellos.

---

## Estado Actual

Backend tiene 5 modulos nuevos funcionando. Frontend no tiene pantallas para ninguno.

| Modulo backend | Ruta API | Pagina frontend | Service | Composable |
|----------------|----------|-----------------|---------|------------|
| Gastos | `/api/v1/core/gastos` | ❌ | ❌ | ❌ |
| Ventas | `/api/v1/core/ventas` | ❌ | ❌ | ❌ |
| Prestamos | `/api/v1/core/prestamos` | ❌ | ❌ | ❌ |
| Patrimonio | `/api/v1/core/patrimonio/{tenantId}` | ❌ | ❌ | ❌ |
| Accounting | `/api/v1/core/accounting` | ❌ | ❌ | ❌ |

---

## Gastos Operativos

### Backend

- **Tabla:** `core.operating_expenses`
- **Entidad:** `GastoOperativo.java`
- **Enum categorias:** `SALARIOS`, `AGUA`, `LUZ`, `INTERNET`, `ALQUILER`, `MANTENIMIENTO`, `PUBLICIDAD`, `OTROS`
- **Endpoints:**
  ```
  POST   /api/v1/core/gastos           # Crear
  GET    /api/v1/core/gastos           # Listar (filtros: fecha, categoria)
  GET    /api/v1/core/gastos/{id}      # Obtener
  PUT    /api/v1/core/gastos/{id}      # Actualizar
  DELETE /api/v1/core/gastos/{id}      # Soft delete
  ```
- **Campos:** id, tenantId, category, description, amount, expenseDate, paymentMethod, isActive
- **Evento:** `GastoCreadoEvent` → debounce → recalcular metricas

### Frontend a crear

```
pages/GastosPage.vue              # CRUD tabla + filtros
services/gasto.service.ts         # API calls
composables/useGastos.ts          # logica
components/gastos/
├── GastoFormDialog.vue           # Dialog crear/editar
└── GastoFilters.vue              # Filtros fecha/categoria
```

### Ruta

```
/dashboard/gastos → GastosPage.vue
```

---

## Ventas Diarias

### Backend

- **Tabla:** `core.daily_sales`
- **Entidad:** `VentaDiaria.java`
- **Endpoints:**
  ```
  POST   /api/v1/core/ventas           # Crear
  GET    /api/v1/core/ventas           # Listar (filtros: fecha)
  GET    /api/v1/core/ventas/{id}      # Obtener
  PUT    /api/v1/core/ventas/{id}      # Actualizar
  DELETE /api/v1/core/ventas/{id}      # Soft delete
  ```
- **Campos:** id, tenantId, saleDate, grossAmount, description, isActive
- **Evento:** `VentaCreadaEvent` → debounce → recalcular metricas

### Frontend a crear

```
pages/VentasPage.vue              # CRUD tabla + filtros
services/venta.service.ts         # API calls
composables/useVentas.ts          # logica
components/ventas/
├── VentaFormDialog.vue           # Dialog crear/editar
└── VentaResumenCard.vue          # Card resumen ventas del dia
```

### Ruta

```
/dashboard/ventas → VentasPage.vue
```

---

## Prestamos + Pagos

### Backend

- **Tablas:** `core.loans`, `core.loan_payments`
- **Entidades:** `Prestamo.java`, `PagoPrestamo.java`
- **Enum estado:** `ACTIVO`, `PAGADO`, `CANCELADO`
- **Endpoints:**
  ```
  POST   /api/v1/core/prestamos             # Crear prestamo
  GET    /api/v1/core/prestamos             # Listar
  GET    /api/v1/core/prestamos/{id}        # Obtener
  PUT    /api/v1/core/prestamos/{id}        # Actualizar
  DELETE /api/v1/core/prestamos/{id}        # Soft delete
  POST   /api/v1/core/prestamos/{id}/pagos  # Registrar pago
  GET    /api/v1/core/prestamos/{id}/pagos  # Historial pagos
  ```
- **Campos prestamo:** id, tenantId, name, lender, amount, interestRate, termMonths, startDate, remainingBalance, status, notes, isActive
- **Campos pago:** id, loanId, amount, interestPaid, principalPaid, paymentDate, paymentMethod

### Frontend a crear

```
pages/PrestamosPage.vue           # Tabla prestamos + pagos
services/prestamo.service.ts      # API calls
composables/usePrestamos.ts       # logica
components/prestamos/
├── PrestamoFormDialog.vue        # Dialog crear/editar prestamo
├── PagoFormDialog.vue            # Dialog registrar pago
├── AmortizationTable.vue         # Tabla amortizacion mes a mes
└── PrestamoStatusBadge.vue       # Badge estado (activo/pagado/cancelado)
```

### Ruta

```
/dashboard/prestamos → PrestamosPage.vue
```

---

## Patrimonio

### Backend

- **Tabla:** `core.patrimony`
- **Entidad:** `Patrimonio.java` (PK = tenant_id, una fila por tenant)
- **Endpoints:**
  ```
  GET    /api/v1/core/patrimonio/{tenantId}   # Obtener (get-or-create)
  PUT    /api/v1/core/patrimonio/{tenantId}   # Actualizar
  ```
- **Campos:** tenantId, initialCapital, startDate, notes
- **NOTA:** No hay POST. Se crea automaticamente si no existe (get-or-create).

### Frontend a crear

```
pages/PatrimonioPage.vue          # Capital actual + dashboard ROI
services/patrimonio.service.ts    # API calls
composables/usePatrimonio.ts      # logica
components/patrimonio/
├── PatrimonioForm.vue            # Form capital inicial (solo si no existe)
├── RoiDashboard.vue              # Dashboard ROI (capital, deuda, proyeccion)
└── DeudaGananciaChart.vue        # Grafico deuda vs ganancia
```

### Ruta

```
/dashboard/patrimonio → PatrimonioPage.vue
```

---

## Accounting (Metricas Financieras)

### Backend

- **Tabla:** `core.tenant_financial_metrics`
- **Entidad:** `MetricasFinanciera.java`
- **Query:** CTE consolidado que consulta ventas + facturas + gastos + prestamos en 1 round-trip
- **Endpoints:**
  ```
  GET    /api/v1/core/accounting/consultar?tenantId={uuid}&periodo=YYYY-MM
  POST   /api/v1/core/accounting/recalcular?tenantId={uuid}&periodo=YYYY-MM
  ```
- **Campos:** totalIncome, costOfGoods, operatingExpenses, loanPayments, totalExpenses, grossMargin, grossMarginPct, operatingMargin, operatingMarginPct, netMargin, netMarginPct
- **Debounce:** Las metricas se recalculan automaticamente cuando hay cambios en gastos, ventas o facturas (Redis debounce, 30s)

### Frontend a crear

```
pages/AccountingPage.vue          # Dashboard financiero completo
services/accounting.service.ts    # API calls
composables/useAccounting.ts      # logica
components/accounting/
├── MetricasCard.vue              # Card margen bruto/operativo/neto
├── PeriodoSelector.vue           # Selector periodo YYYY-MM
└── RecalcularButton.vue          # Boton recalcular con feedback
```

### Ruta

```
/dashboard/accounting → AccountingPage.vue
```

---

## Sidebar — Items nuevos

```typescript
// Agregar a MainLayout.vue → links[]
{ title: 'Gastos', icon: 'money_off', path: '/dashboard/gastos' },
{ title: 'Ventas', icon: 'point_of_sale', path: '/dashboard/ventas' },
{ title: 'Préstamos', icon: 'account_balance', path: '/dashboard/prestamos' },
{ title: 'Patrimonio', icon: 'savings', path: '/dashboard/patrimonio' },
{ title: 'Contabilidad', icon: 'balance', path: '/dashboard/accounting' },
```

---

## Resumen de archivos

| Tipo | Cantidad | Archivos |
|------|----------|----------|
| Paginas | 5 | Gastos, Ventas, Prestamos, Patrimonio, Accounting |
| Services | 5 | gasto, venta, prestamo, patrimonio, accounting |
| Composables | 4 | useGastos, useVentas, usePrestamos, useAccounting |
| Componentes | 12 | dialogs, cards, charts, tables |
| **Total** | **26** | — |

### Dependencias

- **Chart.js** ya esta en `package.json` (para graficos de patrimonio/accounting)
- **Quasar QTable, QDialog, QSelect, QDate** ya disponibles
- **PeriodoSelector** puede reutilizar logica de `usePeriod.ts` existente
