## KPI + Analytics

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

| KPI | Fórmula (simplificada) | Estado | Observación |
|-----|------------------------|--------|-------------|
| Margen Bruto | (V - COGS) / V | Implementable | Cálculo directo |
| Margen Operativo | (V - COGS - GastosOp) / V | Implementable | Sin impuestos |
| Margen Neto | (V - COGS - GastosOp) / V | Implementable | Equivale al Operativo en nuestro contexto |
| EBITDA Adaptado | (V - COGS - GastosOp) / V | Renombrar | Llamar "Margen Disponible" |
| Contribución Adaptado | (V - COGS) / V | Implementable | Igual al Bruto (asumiendo COGS variable) |
| Flujo Caja Adaptado | (V - COGS - GastosOp) / V | Renombrar | Llamar "Liquidez Bruta" |

---

## Consideraciones Técnicas Adicionales

- **Consistencia de Datos:** Todos los márgenes utilizan los mismos campos base, lo que garantiza coherencia en los cálculos y evita discrepancias por fuentes de datos diferentes.
- **Frecuencia de Cálculo:** Se recomienda actualizar estos KPI en tiempo real o con refresco diario, ya que dependen de transacciones diarias (ventas y gastos).
- **Métricas Complementarias:** Añadir Punto de Equilibrio (Gastos Fijos / Margen de Contribución) y Proyección a 30/60/90 días (Ganancia Acumulada / Días × N) para enriquecer el análisis financiero sin modificar los 6 márgenes base.
