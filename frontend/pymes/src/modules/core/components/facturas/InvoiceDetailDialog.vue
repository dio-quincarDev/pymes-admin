<script setup lang="ts">
import { computed } from 'vue'
import type { Factura, ItemFactura } from 'src/modules/core/types'
import { formatDate, formatCurrency } from 'src/utils/format'

const props = defineProps<{
  modelValue: boolean
  factura: Factura | null
  presentationNameMap: Map<string, string>
  categoriaMap: Map<string, string>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const tipoLabel: Record<string, string> = { FACTURA: 'Factura', GASTO_OPERATIVO: 'Gasto' }

const statusColor: Record<string, string> = { PAGADA: 'positive', REGISTRADA: 'warning' }
const statusLabel: Record<string, string> = { PAGADA: 'Pagada', REGISTRADA: 'Pendiente' }

// ponytail: columnas derivadas como computed — evita recrear array + closures en cada render
// precio muestra lo typeado (valorPresentacion) con fallback a unitPrice base
const detailColumns = computed(() => [
  { name: 'product', label: 'Producto', field: 'productName', align: 'left' as const },
  {
    name: 'unidad',
    label: 'Unidad',
    field: (row: ItemFactura) => (row.presentacionId ? props.presentationNameMap.get(row.presentacionId) || '—' : '—'),
    align: 'left' as const,
  },
  {
    name: 'cantidad',
    label: 'Cant.',
    field: (row: ItemFactura) => row.cantidadPresentacion ?? row.quantity,
    align: 'right' as const,
  },
  {
    name: 'precio',
    label: 'Precio',
    field: (row: ItemFactura) => row.valorPresentacion ?? row.unitPrice,
    align: 'right' as const,
    format: (v: number) => formatCurrency(v),
  },
  {
    name: 'descuento',
    label: 'Desc.',
    field: 'discount' as const,
    align: 'right' as const,
    format: (v: number | null) => (v ? formatCurrency(v) : '—'),
  },
  {
    name: 'subtotal',
    label: 'Subtotal',
    field: 'subtotal' as const,
    align: 'right' as const,
    format: (v: number) => formatCurrency(v),
  },
])
</script>

<template>
  <q-dialog :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" dark maximized>
    <q-card v-if="factura" dark class="bg-surface-pine detail-dialog">
      <q-card-section class="row items-center justify-between">
        <div>
          <div class="text-h6 text-primary">{{ factura.invoiceNumber }}</div>
          <div class="text-caption text-accent">{{ formatDate(factura.issueDate, true) }}</div>
        </div>
        <q-btn flat round dense icon="sym_r_close" color="accent" v-close-popup />
      </q-card-section>
      <q-separator dark />
      <q-card-section class="q-gutter-y-sm">
        <div class="row q-col-gutter-md">
          <div class="col-6">
            <div class="text-caption text-accent">{{ factura.category === 'SALARIOS' ? 'Equipo' : 'Proveedor' }}</div>
            <div class="text-secondary text-weight-medium">{{ factura.category === 'SALARIOS' ? (factura.collaboradorName || '—') : (factura.providerName || '—') }}</div>
          </div>
          <div class="col-3">
            <div class="text-caption text-accent">Tipo</div>
            <div class="text-secondary">{{ tipoLabel[factura.type] || factura.type }}</div>
          </div>
          <div class="col-3">
            <div class="text-caption text-accent">Estado</div>
            <q-badge :color="statusColor[factura.status] || 'grey'" class="q-px-sm q-py-xs">{{ statusLabel[factura.status] || factura.status }}</q-badge>
          </div>
        </div>
        <div v-if="factura.type === 'GASTO_OPERATIVO' && factura.category" class="row">
          <div class="col-6">
            <div class="text-caption text-accent">Categoría</div>
            <div class="text-secondary">{{ factura.category === 'SALARIOS' ? 'Salarios' : factura.category === 'OTRO' ? 'Otro' : categoriaMap.get(factura.category) || factura.category }}</div>
          </div>
        </div>
        <div v-if="factura.paymentMethod" class="row">
          <div class="col-6">
            <div class="text-caption text-accent">Método de pago</div>
            <div class="text-secondary">{{ factura.paymentMethod }}</div>
          </div>
        </div>
      </q-card-section>
      <q-separator dark />
      <q-card-section>
        <div class="text-subtitle2 text-primary q-mb-sm">Items</div>
        <div v-if="factura.type === 'GASTO_OPERATIVO' && !factura.items.length" class="text-caption text-accent q-py-sm">
          Monto directo (sin items)
        </div>
        <q-table
          v-else
          dark flat dense
          :rows="factura.items"
          :columns="detailColumns"
          row-key="id"
          hide-pagination
          hide-bottom
        />
      </q-card-section>
      <q-separator dark />
      <q-card-section class="row items-center justify-between">
        <div class="text-caption text-accent" style="letter-spacing:0.08em">TOTAL</div>
        <div class="text-h6 text-primary" style="font-variant-numeric: tabular-nums">{{ formatCurrency(factura.total) }}</div>
      </q-card-section>
      <div v-if="factura.globalDiscount" class="text-caption text-accent text-right q-px-md q-pb-sm">Desc. global: -{{ formatCurrency(factura.globalDiscount) }}</div>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.detail-dialog {
  max-width: 700px;
}

.detail-dialog :deep(.q-table) {
  font-family: var(--pq-font-body);
  font-size: 0.82rem;
}

.detail-dialog :deep(.q-table tbody td) {
  font-family: var(--pq-font-utility);
  font-variant-numeric: tabular-nums;
}
</style>
