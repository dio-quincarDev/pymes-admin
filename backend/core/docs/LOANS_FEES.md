# ⬜ Estrategia: Módulo Patrimonio e Inversión — NO IMPLEMENTADO

> **REALITY CHECK (2026-06):** Diseño futuro. Nada de esto existe en código.
> Sin entidades Patrimonio/Préstamo/ROI, sin endpoints, sin listeners.

---

## 1. Visión General

Dentro del módulo Contabilidad, agregar sub-módulo que rastree:
- Capital inicial invertido
- Préstamos/apalancamiento (múltiples, editables)
- Retorno de inversión (ROI) acumulado
- Tiempo de recuperación
- Proyecciones
- Timeline visual

---

## 2. Entidades Nuevas

### Patrimonio (una por tenant)
```
Patrimonio
├── tenant_id
├── capital_inicial (editable)
├── fecha_inicio
├── notas
└── activo (boolean)
```

Registra una sola vez pero editable. Cuando cambia, recalcula todo.

---

### Préstamo (múltiples por tenant)
```
Préstamo
├── id
├── tenant_id
├── nombre (Préstamo Banco XYZ, Crédito personal, etc)
├── monto
├── tasa_interes_mensual (%)
├── plazo_meses
├── fecha_inicio
├── fecha_vencimiento
├── estado (activo, pagado, cancelado)
├── saldo_pendiente
├── notas
└── fecha_creacion
```

**Editable:**
- Monto (si aún no se desembolsó)
- Tasa de interés
- Plazo
- Saldo pendiente (se actualiza con pagos)

---

### GastoPréstamo (Registro de pagos)
```
GastoPréstamo
├── id
├── prestamo_id
├── monto_pago
├── interes_pagado
├── capital_pagado
├── fecha_pago
└── metodo_pago
```

Se registra cada mes cuando paga cuota. Actualiza saldo_pendiente del Préstamo.

---

### IndicadorROI (Calculada, no editable)
```
IndicadorROI
├── tenant_id
├── periodo (mes/año)
├── ganancia_acumulada
├── roi_porcentaje (%)
├── capital_comprometido (capital + suma préstamos)
├── saldo_deuda_pendiente
├── meses_para_recuperar
├── proyeccion_recuperacion_fecha
├── calculado_at
└── ultima_actualizacion
```

Se recalcula automáticamente vía eventos.

---

## 3. Flujo de Eventos

### Cuando se Crea/Edita Patrimonio

```
Frontend POST /api/core/patrimonios (crear o editar)
  ├── capital_inicial: $5,000
  └── fecha_inicio: 2026-03-01

FacturaService persiste Patrimonio
  ↓
Publica: PatrimonioActualizado(tenant_id, capital_nuevo, capital_anterior)
  ↓
[Async] ContabilidadEventListener escucha
  └── Recalcula IndicadorROI
  └── Publica: MetricasCalculadas
```

---

### Cuando se Agrega Préstamo

```
Frontend POST /api/core/prestamos
  ├── nombre: "Préstamo Banco General"
  ├── monto: $10,000
  ├── tasa_interes_mensual: 5%
  ├── plazo_meses: 24
  └── fecha_inicio: 2026-03-15

PrestamoService persiste Préstamo
  ↓
Publica: PrestamoAgregado(tenant_id, monto, tasa, plazo)
  ↓
[Async] ContabilidadEventListener escucha
  └── Suma monto a Capital Comprometido
  └── Calcula cuota mensual (Amortización)
  └── Recalcula IndicadorROI
  └── Publica: MetricasCalculadas
```

---

### Cuando se Registra Pago de Préstamo

```
Frontend POST /api/core/prestamos/{id}/pagos
  ├── monto: $500
  ├── fecha: 2026-04-01
  └── metodo: "Transferencia"

PrestamoService persiste GastoPréstamo
  ├── Calcula: interes_pagado (saldo × tasa)
  ├── Calcula: capital_pagado (monto - interes)
  ├── Actualiza Préstamo.saldo_pendiente
  ↓
Publica: PagoPrestamoProcesado(tenant_id, prestamo_id, saldo_nuevo)
  ↓
[Async] ContabilidadEventListener escucha
  └── Recalcula saldo_deuda_pendiente en IndicadorROI
  └── Publica: MetricasCalculadas
```

---

### Cuando se Recalculan Métricas Financieras (de Facturas/Ventas)

```
MetricasCalculadas (evento existente)
  ↓
[Async] PatrimonioEventListener escucha (NUEVO)
  ├── Lee: ganancia_acumulada (desde MetricaFinanciera)
  ├── Lee: capital_comprometido (capital_inicial + sum(préstamos activos))
  ├── Lee: saldo_deuda_pendiente (sum(saldo_pendiente de préstamos))
  ├── Calcula:
  │   ├── roi_porcentaje = (ganancia_acumulada / capital_comprometido) × 100
  │   ├── meses_para_recuperar = capital_comprometido / (ganancia_acumulada / meses_transcurridos)
  │   └── proyeccion_recuperacion_fecha = hoy + meses_para_recuperar
  ├── Persiste IndicadorROI
  └── Publica: IndicadorROIActualizado
  
[Async] ReportesEventListener escucha
  └── Actualiza dashboard con ROI y timeline
```

---

## 4. Cálculos Detallados

### ROI Porcentaje (Mensual)

```
ROI % = (Ganancia Acumulada / Capital Comprometido) × 100

Ejemplo:
Capital Inicial: $5,000
Préstamo: $10,000
Capital Comprometido: $15,000

Mes 3: Ganancia acumulada: $1,500
ROI: (1,500 / 15,000) × 100 = 10%

Mes 6: Ganancia acumulada: $3,000
ROI: (3,000 / 15,000) × 100 = 20%

Mes 10: Ganancia acumulada: $5,000
ROI: (5,000 / 15,000) × 100 = 33.3%

Mes 15: Ganancia acumulada: $7,500
ROI: (7,500 / 15,000) × 100 = 50%

Mes 30: Ganancia acumulada: $15,000
ROI: (15,000 / 15,000) × 100 = 100% ← RECUPERADO
```

---

### Meses para Recuperar Inversión

```
Velocidad de ganancia = Ganancia Acumulada / Meses Transcurridos

Ejemplo:
Transcurridos: 6 meses
Ganancia acumulada: $3,000
Velocidad: $3,000 / 6 = $500/mes

Capital Comprometido: $15,000
Meses faltantes: $15,000 / $500 = 30 meses

Total estimado: 6 + 30 = 36 meses hasta recuperar
```

**Cálculo dinámico:** Cada mes se recalcula. Si velocidad aumenta, plazo disminuye.

---

### Comparativa Deuda vs Ganancia Acumulada

```
Mes 1:  Deuda: $14,500 | Ganancia: $200   | Diferencia: -$14,300
Mes 2:  Deuda: $14,000 | Ganancia: $450   | Diferencia: -$13,550
Mes 3:  Deuda: $13,450 | Ganancia: $750   | Diferencia: -$12,700
...
Mes 15: Deuda: $7,500  | Ganancia: $7,500 | Diferencia: $0 ✓ PUNTO DE EQUILIBRIO
Mes 20: Deuda: $3,000  | Ganancia: $10,000| Diferencia: +$7,000
Mes 24: Deuda: $0      | Ganancia: $12,000| Diferencia: +$12,000
```

**Significado:** Fecha en que ganancia > deuda = momento donde negocio es solvente.

---

### Cuota Mensual de Préstamo (Amortización)

```
Fórmula: Cuota = P × [i(1+i)^n] / [(1+i)^n - 1]

Donde:
P = Monto préstamo ($10,000)
i = Tasa mensual (5% = 0.05)
n = Plazo en meses (24)

Cuota = 10,000 × [0.05 × 1.05^24] / [1.05^24 - 1]
Cuota ≈ $644.30/mes

Desglose primera cuota:
├── Interés: $500 (10,000 × 5%)
└── Capital: $144.30
```

**Uso:** Para proyectar flujo de caja con obligaciones de pago.

---

## 5. Endpoints

### Patrimonio
```
POST   /api/core/v1/patrimonios
  ├── capital_inicial
  ├── fecha_inicio
  └── notas

GET    /api/core/v1/patrimonios/{tenant_id}
  └── Retorna patrimonio actual

PUT    /api/core/v1/patrimonios/{tenant_id}
  └── Edita capital, fecha, notas

DELETE /api/core/v1/patrimonios/{tenant_id}
  └── Solo si no hay préstamos ni ganancia
```

---

### Préstamos
```
POST   /api/core/v1/prestamos
  ├── nombre
  ├── monto
  ├── tasa_interes_mensual
  ├── plazo_meses
  └── fecha_inicio

GET    /api/core/v1/prestamos?tenant_id=...
  └── Lista todos los préstamos

GET    /api/core/v1/prestamos/{id}
  └── Detalle con amortización y saldo

PUT    /api/core/v1/prestamos/{id}
  └── Edita (solo si no tiene pagos registrados)

DELETE /api/core/v1/prestamos/{id}
  └── Elimina (solo si saldo = monto inicial)

POST   /api/core/v1/prestamos/{id}/pagos
  ├── monto
  ├── fecha
  └── metodo_pago
  └── Actualiza saldo y calcula desglose

GET    /api/core/v1/prestamos/{id}/pagos
  └── Histórico de pagos
```

---

### Indicadores ROI
```
GET    /api/core/v1/roi?tenant_id=...&periodo=mensual
  └── Retorna:
      - ROI actual (%)
      - Ganancia acumulada
      - Capital comprometido
      - Saldo deuda
      - Meses para recuperar
      - Fecha proyectada recuperación
      - Timeline histórico

GET    /api/core/v1/roi/timeline
  └── Datos para gráfico temporal
      (mes, ganancia, deuda, roi%)

GET    /api/core/v1/roi/comparativa
  └── Deuda vs Ganancia acumulada
```

---

## 6. Visualización/Reportes

### Dashboard - Patrimonio

```
┌─────────────────────────────────────┐
│ PATRIMONIO E INVERSIÓN              │
├─────────────────────────────────────┤
│ Capital Inicial:        $5,000      │
│ Apalancamiento Total:   $10,000     │
│ Capital Comprometido:   $15,000     │
├─────────────────────────────────────┤
│ ROI ACTUAL:             33.3%       │
│ Ganancia Acumulada:     $5,000      │
│ Saldo Deuda:            $5,500      │
├─────────────────────────────────────┤
│ PROYECCIÓN:                         │
│ Meses para Recuperar:   12 meses    │
│ Fecha Estimada:         Mar 2027    │
│ Status:                 En Camino ✓ │
└─────────────────────────────────────┘
```

---

### Gráfico 1: Timeline ROI (Línea)

```
Eje Y: ROI %
Eje X: Meses
Línea verde: ROI mensual
Línea punteada roja: 100% (recuperación)

Ejemplo:
Mes 1:  10%
Mes 3:  15%
Mes 6:  22%
Mes 9:  30%
Mes 12: 45%
Mes 15: 60%
Mes 18: 80%
Mes 20: 100% ← PUNTO DE CRUCE
```

---

### Gráfico 2: Deuda vs Ganancia Acumulada (Áreas)

```
Eje Y: Dinero ($)
Eje X: Meses

Área Roja: Deuda pendiente (disminuye)
Área Verde: Ganancia acumulada (aumenta)
Intersección: Punto de equilibrio

Mes 0:  Deuda $15,000 | Ganancia $0
Mes 10: Deuda $7,500  | Ganancia $3,500
Mes 15: Deuda $5,000  | Ganancia $5,000 ← SE CRUZAN
Mes 20: Deuda $2,000  | Ganancia $8,000
Mes 24: Deuda $0      | Ganancia $10,000
```

---

### Gráfico 3: Desglose Préstamos (Pastel)

```
Si tiene múltiples préstamos:

Préstamo Banco General: $10,000 (67%)
Crédito Personal:        $5,000 (33%)

Total: $15,000
```

---

### Tabla: Amortización Préstamo (Detalladda)

```
Mes | Saldo Inicial | Cuota | Interés | Capital | Saldo Final
 1  | $10,000      | $644 | $500    | $144    | $9,856
 2  | $9,856       | $644 | $493    | $151    | $9,705
 3  | $9,705       | $644 | $485    | $159    | $9,546
...
24  | $644         | $644 | $32     | $612    | $0
```

---

## 7. Validaciones

| Acción | Validación |
|--------|-----------|
| Crear Patrimonio | Capital > 0 |
| Editar Patrimonio | Si ya hay ganancia, aviso al usuario |
| Crear Préstamo | Monto > 0, Tasa > 0, Plazo > 0 |
| Editar Préstamo | Solo si no tiene pagos registrados |
| Pagar Préstamo | Monto <= (saldo + interés del mes) |
| Eliminar Préstamo | Solo si saldo = monto inicial |
| Eliminar Patrimonio | Solo si no hay préstamos activos |

---

## 8. Listeners Nuevos

### PatrimonioEventListener

Escucha:
- PatrimonioActualizado → Recalcula ROI
- MetricasCalculadas → Recalcula indicadores

Publica:
- IndicadorROIActualizado

---

### PrestamoEventListener

Escucha:
- PrestamoAgregado → Agrega a capital comprometido
- PagoPrestamoProcesado → Actualiza saldo deuda

Publica:
- IndicadorROIActualizado

---

## 9. Transaccionalidad

```
PagoPrestamoService.registrarPago():
  1. @Transactional: Persiste GastoPréstamo
  2. Actualiza Préstamo.saldo_pendiente
  3. Commit
  4. publishEvent(new PagoPrestamoProcesado(...))

PatrimonioEventListener.onMetricasCalculadas():
  1. Lee ganancia_acumulada
  2. Lee capital_comprometido
  3. Lee saldo_deuda_pendiente
  4. Calcula indicadores
  5. Persiste IndicadorROI
  6. publishEvent(new IndicadorROIActualizado(...))

Si listener falla:
  - Pago ya está guardado
  - Métricas no se recalculan hasta retry
```

---

## 10. Integración con Módulos Existentes

```
Facturas + Ventas
  ↓ generan
MetricaFinanciera
  ↓ (evento)
PatrimonioEventListener
  ↓ lee
Patrimonio + Préstamos
  ↓ calcula
IndicadorROI
  ↓ (evento)
ReportesEventListener
  ↓ muestra en
Dashboard
```

---

## 11. Checklist Fase 1 - Parte 2 (Actualizado)

- [ ] Entidad Patrimonio (editable)
- [ ] Entidad Préstamo (múltiple, editable)
- [ ] Entidad GastoPréstamo (histórico pagos)
- [ ] Entidad IndicadorROI (calculada)
- [ ] Controllers CRUD (Patrimonio, Préstamo, Pagos)
- [ ] Services (lógica de cálculo)
- [ ] EventListeners (Patrimonio, Préstamo)
- [ ] Cálculos ROI, Amortización, Meses recuperación
- [ ] Endpoints de reportes ROI
- [ ] Gráficos: Timeline ROI, Deuda vs Ganancia, Amortización
- [ ] Transaccionalidad + Retry
- [ ] Tests unitarios

---

** usa .ponytail para optimizar la idea**
