<script setup lang="ts">
import type { Factura, ItemFactura } from 'src/modules/core/types'

defineProps<{
  modelValue: boolean
  factura: Factura | null
  presentationNameMap: Map<string, string>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const tipoLabel: Record<string, string> = { FACTURA: 'Factura', GASTO_OPERATIVO: 'Gasto Operativo' }

function formatCurrency(n: number) {
  return Number.isFinite(n)
    ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(n)
    : '$0.00'
}

function formatDate(s: string) {
  return new Date(s + 'T00:00:00').toLocaleDateString('es-MX', { year: 'numeric', month: 'short', day: 'numeric' })
}

const statusColor: Record<string, string> = { PAGADA: 'positive', REGISTRADA: 'warning' }
</script>

<template>
  <q-dialog :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" dark maximized>
    <q-card v-if="factura" dark class="bg-surface-pine detail-dialog">
      <q-card-section class="row items-center justify-between">
        <div>
          <div class="text-h6 text-primary">{{ factura.invoiceNumber }}</div>
          <div class="text-caption text-accent">{{ formatDate(factura.issueDate) }}</div>
        </div>
        <q-btn flat round dense icon="close" color="accent" v-close-popup />
      </q-card-section>
      <q-separator dark />
      <q-card-section class="q-gutter-y-sm">
        <div class="row q-col-gutter-md">
          <div class="col-6">
            <div class="text-caption text-accent">Proveedor</div>
            <div class="text-secondary text-weight-medium">{{ factura.providerName || '—' }}</div>
          </div>
          <div class="col-3">
            <div class="text-caption text-accent">Tipo</div>
            <div class="text-secondary">{{ tipoLabel[factura.type] || factura.type }}</div>
          </div>
          <div class="col-3">
            <div class="text-caption text-accent">Estado</div>
            <q-badge :color="statusColor[factura.status] || 'grey'" class="q-px-sm q-py-xs">{{ factura.status }}</q-badge>
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
          :columns="[
            { name: 'product', label: 'Producto', field: 'productName', align: 'left' },
            { name: 'unidad', label: 'Unidad', field: (row: ItemFactura) => row.presentacionId ? presentationNameMap.get(row.presentacionId) || '—' : 'Base', align: 'left' },
            { name: 'cantidad', label: 'Cant.', field: 'cantidad', align: 'right' },
            { name: 'precio', label: 'Precio', field: 'precioUnitario', align: 'right', format: (v: number) => formatCurrency(v) },
            { name: 'descuento', label: 'Desc.', field: 'descuento', align: 'right', format: (v: number | null) => v ? formatCurrency(v) : '—' },
            { name: 'subtotal', label: 'Subtotal', field: 'subtotal', align: 'right', format: (v: number) => formatCurrency(v) },
          ]"
          row-key="id"
          hide-pagination
          hide-bottom
        />
      </q-card-section>
      <q-separator dark />
      <q-card-section class="text-right">
        <div v-if="factura.globalDiscount" class="text-caption text-accent">Desc. global: -{{ formatCurrency(factura.globalDiscount) }}</div>
        <div class="text-h6 text-primary">{{ formatCurrency(factura.total) }}</div>
      </q-card-section>
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
