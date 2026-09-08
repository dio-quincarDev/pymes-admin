# Estrategia: Amortización de Préstamos vía Factura PAGADA

Estado: **plan definido** (2026-08-16). Pendiente de implementación.

## Problema actual

1. `cuotaMensual` (utils/prestamo.ts) calcula interés simple total dividido por meses — no refleja amortización real
2. Pagos de préstamo se registran directamente sin vínculo contable (sin factura)
3. `pagosPrestamos` en métricas cuenta el monto completo como gasto (debería ser solo interés)
4. No hay trazabilidad contable del pago del préstamo

## Solución: Amortización francesa + Factura PAGADA

### Flujo propuesto

1. PrestamosPage "Registrar pago" → crea factura tipo `PAGO_PRESTAMO` ligada al préstamo → la marca `PAGADA` de una
2. Listener en `pagarFactura` detecta `PAGO_PRESTAMO` → calcula amortización:
   - `interesMes = remainingBalance × (tasaInteres/100)`
   - Si `pago > interesMes`: `interestPaid = interesMes`, `principalPaid = pago - interesMes`
   - Si `pago <= interesMes`: `interestPaid = pago`, `principalPaid = 0`
   - `remainingBalance -= principalPaid`; si ≤ 0 → `PAGADO`
3. Métricas: `loan_pay` CTE suma `lp.interest_paid` (solo interés es gasto)

### Cuota francesa constante

Fórmula estándar: `cuota = P × [r(1+r)^n] / [(1+r)^n - 1]`
- P = principal, r = tasa mensual (%), n = meses
- Sin interés: `cuota = P / n`
- La cuota es constante; el interés decrece y el capital crece

## Cambios por capa

### 1. Frontend — `utils/prestamo.ts`

```typescript
cuotaMensual = (monto, tasaMensual, plazoMeses) => {
  if (plazoMeses <= 0 || monto <= 0) return 0;
  const r = tasaMensual / 100;
  if (r === 0) return monto / plazoMeses;
  const factor = Math.pow(1 + r, plazoMeses);
  return monto * (r * factor) / (factor - 1);
}
```

`interesTotal` y `totalConInteres` se mantienen para display en PrestamosPage.

### 2. Backend — `PrestamoServiceImpl.registrarPago`

Cambiar para calcular amortización automáticamente (interest first, rest → principal):

```java
BigDecimal tasaMensual = prestamo.getInterestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
BigDecimal interesMes = prestamo.getRemainingBalance().multiply(tasaMensual);
BigDecimal interesPagado = request.monto().min(interesMes);
BigDecimal capitalPagado = request.monto().subtract(interesPagado);
```

El endpoint `POST /prestamos/{id}/pagos` se mantiene (backward compat + tests). La lógica de amortización vive aquí y también se usa desde el listener de factura.

### 3. Migración V2 — Vínculo préstamo-factura

```sql
ALTER TABLE core.invoices ADD COLUMN loan_id UUID REFERENCES core.loans(id);
CREATE INDEX idx_invoices_loan ON core.invoices(loan_id) WHERE loan_id IS NOT NULL;
```

### 4. Domain — Factura

- `Factura.java`: campo `loanId` (`@Column(name = "loan_id")`)
- `FacturaRequest.java`: último campo `prestamoId`
- `FacturaResponse.java`: último campo `prestamoId`
- `FacturaMapper.java`: mapear `f.getLoanId()`

### 5. Service — `FacturaServiceImpl`

- `isGastoSinItems`: acepta `PAGO_PRESTAMO` además de `GASTO_OPERATIVO`
- `createFactura`: validar `prestamoId` no null, préstamo existe y pertenece al tenant, guardar `loanId`

### 6. Listener — `FacturaPagadaListener`

Extender para detectar `PAGO_PRESTAMO`:

```java
if ("PAGO_PRESTAMO".equals(factura.getType()) && factura.getLoanId() != null) {
    prestamoService.registrarPagoDesdeFactura(
        factura.getLoanId(), factura.getTotal(),
        factura.getIssueDate(), factura.getPaymentMethod()
    );
}
```

Se inyecta `PrestamoService` en el listener. Se crea método `registrarPagoDesdeFactura` que reutiliza la lógica de amortización.

### 7. Métricas — `MetricasServiceImpl`

Cambiar CTE `loan_pay`:
```sql
-- Antes:
SELECT COALESCE(SUM(lp.amount), 0) AS total
-- Después:
SELECT COALESCE(SUM(lp.interest_paid), 0) AS total
```

### 8. Frontend — PrestamosPage

Pago dialog crea factura + la paga:
```typescript
const res = await facturaService.create({
  tenantId, prestamoId: pagoPrestamo.value!.id,
  fecha: pagoForm.value.fechaPago, tipo: 'PAGO_PRESTAMO',
  items: [], total: pagoForm.value.monto,
});
await facturaService.pay(res.data.id, tenantId);
```

### 9. Frontend — FacturasPage

Añadir `PAGO_PRESTAMO` al select de tipo.

### 10. Frontend — types/index.ts

- `Factura.prestamoId: string | null`
- `FacturaRequest.prestamoId?: string | null`

### 11. Tests

- `FacturaServiceImplTest`: 9 llamadas `new FacturaRequest(...)` → añadir `null` al final (prestamoId)
- Sin tests nuevos de amortización (los JPA tests + flujo E2E via factura cubren)

## Skipped deliberadamente

| Qué | Por qué |
|-----|---------|
| Tabla `loan_payments.invoice_id` | El vínculo es `invoices.loan_id` — suficiente |
| Tests unitarios de amortización | Los JPA tests + el flujo E2E via factura cubren el caso |
| Deshabilitar endpoint POST /prestamos/{id}/pagos | Se mantiene por backward compat y tests existentes |
| Doble conteo en recovery time | `gananciaMensual = totalIngresos × margenNetoPct/100`. El margenNeto YA descuenta `loanPayments`. `plataARecuperar = capital + saldoPendiente`. No hay doble conteo |
| Migración de datos | No hay datos existentes que migrar — es funcionalidad nueva |

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `frontend/pymes/src/modules/core/utils/prestamo.ts` | Cuota francesa |
| `backend/core/.../prestamo/service/impl/PrestamoServiceImpl.java` | Amortización automática |
| `backend/core/src/main/resources/db/migration/V2__loan_payment_link.sql` | Nueva migración |
| `backend/core/.../invoice/domain/Factura.java` | Campo loanId |
| `backend/core/.../invoice/dto/FacturaRequest.java` | Campo prestamoId |
| `backend/core/.../invoice/dto/FacturaResponse.java` | Campo prestamoId |
| `backend/core/.../invoice/mapper/FacturaMapper.java` | Mapear loanId |
| `backend/core/.../invoice/service/impl/FacturaServiceImpl.java` | Aceptar PAGO_PRESTAMO + validar préstamo |
| `backend/core/.../invoice/listener/FacturaPagadaListener.java` | Detectar PAGO_PRESTAMO + amortización |
| `backend/core/.../accounting/service/impl/MetricasServiceImpl.java` | loan_pay = interest_paid |
| `frontend/pymes/src/modules/core/pages/PrestamosPage.vue` | Pago vía factura |
| `frontend/pymes/src/modules/core/pages/FacturasPage.vue` | Tipo PAGO_PRESTAMO |
| `frontend/pymes/src/modules/core/types/index.ts` | prestamoId |
| `backend/core/src/test/.../FacturaServiceImplTest.java` | Actualizar FacturaRequest calls |
