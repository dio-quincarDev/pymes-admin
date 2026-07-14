## KPI + Analytics

> **Estado (2026-07-14):** 6 KPIs implementados (3 cálculos únicos + 3 aliases). Outputs del Motor #10 (Financial Health Engine) en `ANALYTICS.md`.

---

### 1. Margen Bruto

**Fórmula:**
```
= (Ventas - COGS) / Ventas × 100
```

**Propósito:** Mide la rentabilidad directa de la mercancía vendida antes de considerar gastos operativos.

**Interpretación:** Indica el porcentaje de ingresos que queda para cubrir gastos operativos y generar beneficio. Un margen bajo sugiere problemas en precios o costos de adquisición.

**Dependencias:** Ventas (registros VENTA), COGS (registros FACTURA). Implementable al 100%.

---

### 2. Margen Operativo

**Fórmula:**
```
= (Ventas - COGS - Gastos Operativos) / Ventas × 100
```

**Propósito:** Evalúa la eficiencia operativa del negocio, excluyendo impuestos e intereses.

**Interpretación:** Refleja la capacidad de generar beneficio de las operaciones principales. Un margen operativo negativo indica que el negocio no cubre sus costos fijos.

**Dependencias:** Ventas, COGS, Gastos Operativos (registros GASTO_OPERATIVO). Sin impuestos, válido para PyMEs.

---

### 3. Margen Neto

**Fórmula:**
```
= (Ventas - COGS - Gastos Operativos) / Ventas × 100
```

**Propósito:** Mide la rentabilidad final después de todos los gastos del negocio.

**Interpretación:** En nuestro contexto (sin intereses ni impuestos), es equivalente al Margen Operativo. Representa el beneficio real disponible.

**Dependencias:** Mismos datos que Margen Operativo. Implementable, aunque conceptualmente simplificado.

---

### 4. Margen EBITDA (Adaptado)

**Fórmula:**
```
= (Ventas - COGS - Gastos Operativos) / Ventas × 100
```

**Propósito:** Estimar el flujo de caja operativo antes de depreciaciones, amortizaciones, impuestos e intereses.

**Interpretación:** Dado que no se registran depreciaciones, este cálculo es una aproximación válida al EBITDA real. Renombrar como "Margen Disponible para Deudas/Inversión" para evitar confusiones.

**Dependencias:** Datos de Ventas, COGS y Gastos Operativos.

---

### 5. Margen de Contribución (Adaptado)

**Fórmula:**
```
= (Ventas - COGS) / Ventas × 100
```

**Propósito:** Mide la contribución de cada unidad vendida para cubrir costos fijos y generar ganancia.

**Interpretación:** Dado que no se distinguen costos fijos de variables, asumimos que COGS son costos variables y Gastos Operativos son fijos. Así, este margen es idéntico al Margen Bruto en nuestra implementación.

**Nota:** Avisar al usuario que se asume esta simplificación.

---

### 6. Margen de Flujo de Caja (Adaptado)

**Fórmula:**
```
= (Ventas - COGS - Gastos Operativos) / Ventas × 100
```

**Propósito:** Aproximar la liquidez generada por el negocio, asumiendo que todos los ingresos y gastos registrados son efectivamente cobrados y pagados.

**Interpretación:** Renombrar como "Margen de Liquidez Bruta" para reflejar que no se consideran plazos de cobro/pago. Es una estimación rápida de capacidad de caja.

**Dependencias:** Mismos datos que los márgenes anteriores.

---

## Resumen de Implementación

| KPI | Fórmula (simplificada) | Estado | Implementado en | Observación |
|-----|------------------------|--------|-----------------|-------------|
| Margen Bruto | (V - COGS) / V | ✅ Implementado | `MetricasServiceImpl` + `MetricasFinanciera.grossMarginPct` | CTE consolidado, 1 round-trip |
| Margen Operativo | (V - COGS - GastosOp) / V | ✅ Implementado | `MetricasServiceImpl` + `MetricasFinanciera.operatingMarginPct` | Sin impuestos (PyMEs) |
| Margen Neto | (V - COGS - GastosOp - Loans) / V | ✅ Implementado | `MetricasServiceImpl` + `MetricasFinanciera.netMarginPct` | Incluye pagos de préstamos |
| EBITDA Adaptado | = Margen Operativo | ✅ Alias | `MetricasResponse.ebitdaAdaptado` | Renombrar a "Margen Disponible" en UI |
| Contribución | = Margen Bruto | ✅ Alias | `MetricasResponse.margenContribucion` | COGS = variable, gastosOp = fijos |
| Flujo Caja | = Margen Neto | ✅ Alias | `MetricasResponse.margenLiquidezBruta` | Sin considerar plazos cobro/pago |

**Endpoints:** `GET /accounting/consultar`, `POST /accounting/recalcular`

**Financial Health Engine (Motor #10):** Los 6 márgenes son inputs del scoring compuesto que produce alertas, señales de inversión y readiness de expansión. Ver `ANALYTICS.md` y `CORE.md` §Motor de Salud Financiera.

---

## Consideraciones Técnicas Adicionales

- **Consistencia de Datos:** Todos los márgenes utilizan los mismos campos base, lo que garantiza coherencia en los cálculos y evita discrepancias por fuentes de datos diferentes.
- **Frecuencia de Cálculo:** Se actualizan con debounce Redis (30s) al registrar facturas, gastos o ventas. Recálculo manual vía `POST /accounting/recalcular`.
- **Métricas Complementarias:** Punto de Equilibrio (Gastos Fijos / Margen de Contribución) y Proyección a 30/60/90 días están implementados en el Motor #10 del analytics (Financial Health Engine) como señales de expansión.
