# Rediseño de FacturasPage — Plan de Implementación

**Estado:** Plan definido / Pendiente de implementación  
**Ámbito:** 1 solo archivo — [`frontend/pymes/src/modules/core/pages/FacturasPage.vue`](file:///home/dio/desarrollo/side-projects/pymes-admin/frontend/pymes/src/modules/core/pages/FacturasPage.vue)  
**Objetivo:** Eliminar el scroll infinito de facturas reemplazándolo por una navegación mensual con resumen de KPIs y filtros de estado, sin añadir archivos ni abstracciones extras.

---

## 1. Diagnóstico del Estado Actual

En `FacturasPage.vue`, todas las facturas del tenant se renderizan simultáneamente en el DOM dentro de un bucle continuo de meses:
```html
<div v-for="group in monthGroups" :key="group.label" class="q-mb-lg">
  <div class="month-group-header">...</div>
  <div v-for="inv in group.items" :key="inv.id" class="invoice-row">...</div>
</div>
```

### Problemas:
1. **Scroll Infinito Acumulativo**: Con decenas o cientos de facturas a lo largo del tiempo, la página se vuelve interminable.
2. **Dificultad para Auditar Pendientes**: Las facturas pendientes (`REGISTRADA`) y pagadas (`PAGADA`) se mezclan en la misma lista sin posibilidad de filtrar rápido qué cuentas faltan por saldar.
3. **Falta de Métricas del Período**: No hay un resumen financiero visible que indique cuánto se ha facturado en el mes o cuánto dinero está pendiente de pago.

---

## 2. Decisión de Diseño

Siguiendo la regla de diseño y coherencia con [`VentasPage.vue`](file:///home/dio/desarrollo/side-projects/pymes-admin/frontend/pymes/src/modules/core/pages/VentasPage.vue), se adopta el patrón de **Navegación Mensual + Stat Strip + Tabs de Estado**:

- **Cero archivos nuevos**: Todo se integra inline en `FacturasPage.vue`.
- **Navegación por Mes**: Selector `< Mes Año >` con botón "Hoy" para centrarse siempre en el período relevante.
- **Stat Strip de KPIs del Mes** (actualizado 2026-11-09 — el "Factura Promedio" viejo mezclaba insumos con gasto rápido en un promedio sin significado; se separó por tipo):
  - **Insumos pagados**: monto + cantidad de `PAGADA` de tipo `FACTURA`.
  - **Gasto rápido pagado**: monto + cantidad de `PAGADA` de tipo `GASTO_OPERATIVO` (cuadrado con `costoMercaderia`/`gastosOperativos` de Análisis: misma receta PAGADA + fecha de emisión).
  - **Insumo diario**: insumos pagados ÷ días del período (mes en curso: días transcurridos; mes cerrado: días del mes) — subtexto "en N días" muestra el divisor.
  - **Pendiente de Pago**: monto y cantidad de facturas en estado `REGISTRADA` (alerta).
  - "Total Pagado" como tarjeta murió: es la suma visible de las dos primeras. Grid mobile-first: 2×2 base, 4 columnas desde 900px.
- **Filtros Rápidos de Estado**: Pestañas o chips compactos:
  - `Todas (N)`
  - `Pendientes (N)`
  - `Pagadas (N)`
- **Listado del Mes**: Muestra únicamente las facturas del mes y estado seleccionados, manteniendo el buscador reactivo por número o proveedor.

---

## 3. Especificación Técnica (`FacturasPage.vue`)

### 3.1 Script Setup
```typescript
// 1. Período reactivo
const selectedDate = ref(new Date());

const selectedMonthKey = computed(() => {
  const d = selectedDate.value;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
});

const monthLabel = computed(() => {
  return selectedDate.value.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });
});

function prevMonth() {
  const d = new Date(selectedDate.value);
  d.setMonth(d.getMonth() - 1);
  selectedDate.value = d;
}

function nextMonth() {
  const d = new Date(selectedDate.value);
  d.setMonth(d.getMonth() + 1);
  selectedDate.value = d;
}

function goToday() {
  selectedDate.value = new Date();
}

// 2. Filtro por estado
type StatusFilter = 'TODAS' | 'REGISTRADA' | 'PAGADA';
const statusFilter = ref<StatusFilter>('TODAS');

// 3. Facturas del mes actual
const currentMonthInvoices = computed(() => {
  return rows.value.filter((r) => r.issueDate.startsWith(selectedMonthKey.value));
});

// 4. Métricas reactivas del mes
const totalMes = computed(() =>
  currentMonthInvoices.value.reduce((acc, r) => acc + (r.total || 0), 0)
);

const pendientesList = computed(() =>
  currentMonthInvoices.value.filter((r) => r.status === 'REGISTRADA')
);
const totalPendiente = computed(() =>
  pendientesList.value.reduce((acc, r) => acc + (r.total || 0), 0)
);

const pagadasList = computed(() =>
  currentMonthInvoices.value.filter((r) => r.status === 'PAGADA')
);
// 2026-11-09: el strip separa por tipo — insumos = PAGADA type 'FACTURA', gasto rápido = PAGADA type 'GASTO_OPERATIVO'
const totalInsumos = computed(() =>
  pagadasList.value.filter((r) => r.type === 'FACTURA').reduce((acc, r) => acc + (r.total || 0), 0)
);

// 5. Facturas filtradas para la vista
const displayedInvoices = computed(() => {
  let list = currentMonthInvoices.value;
  if (statusFilter.value !== 'TODAS') {
    list = list.filter((r) => r.status === statusFilter.value);
  }
  if (filter.value.trim()) {
    const q = filter.value.toLowerCase().trim();
    list = list.filter(
      (r) =>
        r.invoiceNumber.toLowerCase().includes(q) ||
        (r.providerName ?? '').toLowerCase().includes(q)
    );
  }
  return list;
});
```

### 3.2 Template
Se reemplaza la sección del bucle `v-for="group in monthGroups"` por:
1. **Selector de Período**:
   - Botón `<` (`sym_r_chevron_left`), Botón del Mes que al hacer click regresa a "Hoy", y Botón `>` (`sym_r_chevron_right`).
2. **Stat Strip**:
   - Tarjetas sutiles con bordes `var(--pq-border)` y números en `var(--pq-font-utility)`.
3. **Chips de Estado**:
   - `Todas` (`currentMonthInvoices.length`)
   - `Pendientes` (`pendientesList.length`)
   - `Pagadas` (`pagadasList.length`)
4. **Listado**:
   - Mapeo directo de `displayedInvoices` con las filas de factura actuales (`.invoice-row`).
   - Mantiene botones: Ver Detalle (`openDetail`), Editar (`openEdit`), Pagar (`confirmPay`), Eliminar/Anular (`confirmDelete`).
   - Si `displayedInvoices.length === 0`, muestra `EmptyState` indicando que no hay facturas con ese filtro o mes.

### 3.3 Preservación Total
Se conservan intactos en el mismo archivo:
- Modal y formulario de creación/edición de facturas y gastos operativos (`InvoiceItemCard`, conversiones, impuestos ITBMS, sueltos).
- `InvoiceDetailDialog`.
- Diálogos de confirmación de pago y eliminación/anulación.
- Atajos de teclado (`Ctrl+N`, `Ctrl+S`).

---

## 4. Plan de Verificación

```bash
# Validar linting
cd frontend/pymes && npm run lint

# Validar type checking y build
cd frontend/pymes && npm run build
```

### Casos de prueba manuales:
1. **Navegación**: Cambiar de mes y comprobar que solo se muestran las facturas de ese período.
2. **Filtros de estado**:
   - Click en `Pendientes`: se ocultan las facturas pagadas.
   - Click en `Pagadas`: se ocultan las pendientes.
3. **Pagar factura**: Al presionar "Pagar", el estado cambia inmediatamente a `PAGADA` y el Stat Strip descuenta el monto de Pendiente y lo suma a "Insumos pagados" o "Gasto rápido pagado" según el tipo, en tiempo real.
4. **Buscador**: Filtrar por nombre de proveedor o número de factura dentro del período.
