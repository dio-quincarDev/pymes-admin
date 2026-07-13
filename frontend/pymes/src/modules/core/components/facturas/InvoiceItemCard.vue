<script setup lang="ts">
import { ref, computed } from 'vue'

export interface ProductOption {
  label: string
  value: string
  productName: string
  sku: string
  category: string
  proveedorId: string | null
  proveedorName: string | null
  lastUnitPrice: number | null
}

interface ItemForm {
  _key: number
  productoId: string | null
  presentacionId: string | null
  cantidad: number | null
  precioUnitario: number | null
  descuento: number
}

const props = defineProps<{
  item: ItemForm
  index: number
  productOptions: ProductOption[]
  unitOptions: { label: string; value: string }[]
  presentationConversionMap: Map<string, number>
  baseUnitName: string
}>()

const emit = defineEmits<{
  'update:productoId': [value: string | null]
  'update:presentacionId': [value: string | null]
  'update:cantidad': [value: number | null]
  'update:precioUnitario': [value: number | null]
  'update:descuento': [value: number]
  remove: []
}>()

const search = ref('')

const filteredProducts = computed(() => {
  const needle = search.value.toLowerCase()
  return props.productOptions.filter(p =>
    !needle || p.label.toLowerCase().includes(needle) || p.sku.toLowerCase().includes(needle)
  )
})

const subtotal = computed(() => {
  const qty = props.item.cantidad || 0
  const price = props.item.precioUnitario || 0
  const disc = props.item.descuento || 0
  return formatCurrency(qty * price * (1 - disc / 100))
})

function formatCurrency(n: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(n)
}

function filterProducts(val: string, update: (fn: () => void) => void) {
  search.value = val
  update(() => { /* filteredProducts computed re-evaluates automatically */ })
}

const conversionBadge = computed(() => {
  if (!props.item.presentacionId) return null
  const conv = props.presentationConversionMap.get(props.item.presentacionId) || 1
  if (conv <= 1) return null
  const presOpt = props.unitOptions.find(o => o.value === props.item.presentacionId)
  const presName = presOpt?.label || ''
  return `${presName} = ${conv} ${props.baseUnitName}`
})
</script>

<template>
  <div class="invoice-item-card" role="group" :aria-label="`Item ${index + 1}`">
    <!-- Header row: item number + product + remove -->
    <div class="item-header">
      <div class="item-number">{{ String(index + 1).padStart(2, '0') }}</div>
      <div class="item-product">
        <q-select
          dark dense filled standout
          :model-value="item.productoId"
          @update:model-value="emit('update:productoId', $event)"
          :options="filteredProducts"
          label="Producto"
          map-options emit-value use-input @filter="filterProducts"
          popup-content-class="product-dropdown"
        >
          <template v-slot:prepend>
            <q-icon name="inventory_2" size="1rem" class="text-primary" />
          </template>
          <template v-slot:option="{ itemProps, opt }">
            <q-item v-bind="itemProps" class="product-option">
              <q-item-section>
                <q-item-label class="product-option__name">{{ opt.productName }}</q-item-label>
                <q-item-label caption class="product-option__meta">
                  <span v-if="opt.sku" class="product-option__sku">{{ opt.sku }}</span>
                  <span v-if="opt.category" class="product-option__cat">{{ opt.category }}</span>
                </q-item-label>
              </q-item-section>
              <q-item-section side v-if="opt.proveedorName">
                <q-chip dense size="xs" color="primary" text-color="dark" class="q-mr-none">
                  {{ opt.proveedorName }}
                </q-chip>
              </q-item-section>
            </q-item>
          </template>
        </q-select>
      </div>
      <q-btn
        flat dense round icon="close" size="sm"
        color="negative"
        @click="emit('remove')"
        aria-label="Eliminar item"
        class="remove-btn"
      />
    </div>

    <!-- Input grid -->
    <div class="item-inputs">
      <div class="item-input-group">
        <label class="item-input-label">Cant.</label>
        <q-input
          dark dense outlined
          :model-value="item.cantidad"
          @update:model-value="emit('update:cantidad', Number($event))"
          type="text" inputmode="decimal"
          class="item-input"
        />
      </div>

      <div class="item-input-group">
        <label class="item-input-label">Unidad</label>
        <q-select
          dark dense outlined
          :model-value="item.presentacionId"
          @update:model-value="emit('update:presentacionId', $event)"
          :options="unitOptions"
          map-options emit-value
          :disable="!item.productoId"
          class="item-input"
        />
      </div>

      <div class="item-input-group">
        <label class="item-input-label">P. Unit.</label>
        <q-input
          dark dense outlined
          :model-value="item.precioUnitario"
          @update:model-value="emit('update:precioUnitario', Number($event))"
          type="text" inputmode="decimal"
          class="item-input price-input"
        >
          <template v-slot:prepend><span class="currency-symbol">$</span></template>
        </q-input>
      </div>

      <div class="item-input-group">
        <label class="item-input-label">Dto. %</label>
        <q-input
          dark dense outlined
          :model-value="item.descuento"
          @update:model-value="emit('update:descuento', Number($event))"
          type="text" inputmode="decimal"
          class="item-input discount-input"
        >
          <template v-slot:append><span class="pct-symbol">%</span></template>
        </q-input>
      </div>
    </div>

    <!-- Footer: conversion + subtotal -->
    <div class="item-footer">
      <div class="item-footer__left">
        <Transition name="badge-slide">
          <div v-if="conversionBadge" class="conversion-badge">
            <q-icon name="swap_vert" size="0.85rem" />
            <span>{{ conversionBadge }}</span>
          </div>
        </Transition>
      </div>
      <div class="item-footer__right">
        <span class="subtotal-label">Subtotal</span>
        <span class="subtotal-value" :class="{ 'has-discount': item.descuento > 0 }">{{ subtotal }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.invoice-item-card {
  background:
    linear-gradient(135deg, rgba(163, 120, 94, 0.03) 0%, rgba(27, 38, 36, 0.45) 100%);
  border: 1px solid rgba(113, 131, 127, 0.12);
  border-radius: 12px;
  padding: 14px 16px 12px;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
  position: relative;
  overflow: hidden;
}

.invoice-item-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, rgba(163, 120, 94, 0.15) 50%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.invoice-item-card:hover {
  border-color: rgba(163, 120, 94, 0.25);
}

.invoice-item-card:hover::before {
  opacity: 1;
}

.invoice-item-card:focus-within {
  border-color: rgba(163, 120, 94, 0.4);
  box-shadow: 0 0 0 1px rgba(163, 120, 94, 0.08), 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* ─── Header ─── */
.item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.item-number {
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 0.7rem;
  font-weight: 700;
  color: rgba(163, 120, 94, 0.55);
  background: rgba(163, 120, 94, 0.08);
  border: 1px solid rgba(163, 120, 94, 0.12);
  border-radius: 6px;
  padding: 4px 7px;
  letter-spacing: 0.04em;
  min-width: 30px;
  text-align: center;
  flex-shrink: 0;
}

.item-product {
  flex: 1;
  min-width: 0;
}

.remove-btn {
  opacity: 0.3;
  transition: opacity 0.15s ease, transform 0.15s ease, background-color 0.15s ease;
  flex-shrink: 0;
}

.remove-btn:hover {
  opacity: 1;
  transform: scale(1.15);
  background: rgba(239, 68, 68, 0.12);
}

/* ─── Input grid ─── */
.item-inputs {
  display: grid;
  grid-template-columns: 1fr 1.1fr 1fr 0.8fr;
  gap: 10px;
  margin-bottom: 10px;
}

.item-input-group {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.item-input-label {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: rgba(163, 120, 94, 0.5);
  padding-left: 2px;
}

.item-input :deep(.q-field__control) {
  border-radius: 8px !important;
  min-height: 34px !important;
  font-size: 0.85rem;
  font-variant-numeric: tabular-nums;
}

.currency-symbol {
  font-size: 0.85rem;
  font-weight: 600;
  color: rgba(163, 120, 94, 0.5);
}

.pct-symbol {
  font-size: 0.8rem;
  font-weight: 600;
  color: rgba(163, 120, 94, 0.4);
}

/* ─── Footer ─── */
.item-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 24px;
}

.item-footer__left {
  flex: 1;
}

.conversion-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.72rem;
  font-weight: 500;
  color: rgba(34, 211, 238, 0.8);
  background: rgba(34, 211, 238, 0.08);
  border: 1px solid rgba(34, 211, 238, 0.15);
  border-radius: 6px;
  padding: 3px 8px;
  letter-spacing: 0.02em;
}

.item-footer__right {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.subtotal-label {
  font-size: 0.7rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgba(163, 120, 94, 0.45);
}

.subtotal-value {
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 0.9rem;
  font-weight: 700;
  color: rgba(163, 120, 94, 0.85);
  font-variant-numeric: tabular-nums;
}

.subtotal-value.has-discount {
  color: rgba(34, 197, 94, 0.85);
}

/* ─── Badge transition ─── */
.badge-slide-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.badge-slide-leave-active {
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.badge-slide-enter-from {
  opacity: 0;
  transform: translateX(-8px);
}

.badge-slide-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

/* ─── Dropdown styling ─── */
.product-dropdown {
  font-size: 0.85rem;
}

.product-option {
  padding: 6px 12px;
}

.product-option__name {
  font-weight: 500;
}

.product-option__meta {
  display: flex;
  gap: 6px;
  font-size: 0.75rem;
  color: rgba(163, 120, 94, 0.5);
}

.product-option__sku {
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 0.72rem;
  background: rgba(163, 120, 94, 0.08);
  padding: 1px 5px;
  border-radius: 3px;
}

.product-option__cat {
  font-size: 0.72rem;
}
</style>
