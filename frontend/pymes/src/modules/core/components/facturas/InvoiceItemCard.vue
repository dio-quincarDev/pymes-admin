<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { calcItbms, calcNeto } from 'src/modules/core/utils/invoiceMath'

export interface ProductOption {
  label: string
  value: string
  productName: string
  sku: string
  category: string
  categoryName: string
  proveedorId: string | null
  proveedorName: string | null
  lastUnitPrice: number | null
}

interface ItemForm {
  _key: number
  productoId: string | null
  presentacionId: string | null
  cantidad: number | null
  valor: number | null
  descuento: number
  itbmsTasa: number
}

interface Props {
  item: ItemForm
  index: number
  productOptions: ProductOption[]
  /** catálogo completo para resolver label aunque el filtro de categoría oculte el producto */
  allProductOptions?: ProductOption[]
  unitOptions: { label: string; value: string }[]
  presentationConversionMap: Map<string, number>
}

interface Emits {
  'update:productoId': [value: string | null]
  'update:presentacionId': [value: string | null]
  'update:cantidad': [value: number | null]
  'update:valor': [value: number | null]
  'update:descuento': [value: number]
  'update:itbmsTasa': [value: number]
  remove: []
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const selectedLabel = computed(() => {
  const catalog = props.allProductOptions ?? props.productOptions
  return catalog.find(o => o.value === props.item.productoId)?.label ?? ''
})

// ponytail: 3 filtros independientes AND — FacturasPage ya entrega productOptions = proveedor AND categoría; acá texto filtra ENCIMA de esa base y se preserva al cambiar proveedor/categoría
const searchNeedle = shallowRef('')
const filteredOptions = ref<ProductOption[]>([])

function getFiltered(list: ProductOption[], needle: string) {
  if (!needle) return [...list]
  const n = needle.toLowerCase()
  return list.filter(o =>
    o.productName.toLowerCase().includes(n) ||
    (o.sku ?? '').toLowerCase().includes(n) ||
    (o.proveedorName ?? '').toLowerCase().includes(n) ||
    (o.categoryName ?? '').toLowerCase().includes(n)
  )
}

// ponytail: single source — text filters on top of proveedor+categoría base from parent; selected only prepended when needle empty so user sees filter working
function getFilteredWithSelected(list: ProductOption[], needle: string) {
  const base = getFiltered(list, needle)
  const sel = props.item.productoId
  if (!sel || needle) return base
  if (base.some(o => o.value === sel)) return base
  const catalog = props.allProductOptions?.length ? props.allProductOptions : list
  const found = catalog.find(o => o.value === sel)
  return found ? [found, ...base] : base
}

watch(() => props.productOptions, v => { filteredOptions.value = getFilteredWithSelected(v, searchNeedle.value) }, { immediate: true })
watch(() => props.item.productoId, () => { filteredOptions.value = getFilteredWithSelected(props.productOptions, searchNeedle.value) })

function productFilter(val: string, update: (fn: () => void) => void) {
  update(() => {
    searchNeedle.value = val ?? ''
    filteredOptions.value = getFilteredWithSelected(props.productOptions, searchNeedle.value)
  })
}

function onClear() {
  searchNeedle.value = ''
  filteredOptions.value = getFilteredWithSelected(props.productOptions, '')
}

const conversion = computed(() => {
  if (!props.item.presentacionId) return 1
  return props.presentationConversionMap.get(props.item.presentacionId) || 1
})

const precioUnitario = computed(() => {
  const val = props.item.valor
  if (val == null) return null
  const conv = conversion.value
  return conv > 0 ? val / conv : val
})

const subtotal = computed(() => calcNeto(props.item.cantidad, props.item.valor, props.item.descuento))

const itbmsMonto = computed(() => calcItbms(subtotal.value, props.item.itbmsTasa))

const itbmsOptions = [
  { label: 'Sin ITBMS (0%)', value: 0 },
  { label: '7%', value: 7 },
  { label: '10%', value: 10 },
]

function fmt(n: number | null) {
  if (n == null || !Number.isFinite(n)) return '—'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(n)
}
</script>

<template>
  <div class="item-card" role="group" :aria-label="`Item ${index + 1}`">
    <!-- Top: number + product + remove -->
    <div class="item-card__top">
      <span class="item-card__num">{{ String(index + 1).padStart(2, '0') }}</span>
      <q-select
        dark dense
        :model-value="item.productoId"
        @update:model-value="emit('update:productoId', $event)"
        :options="filteredOptions"
        option-value="value"
        option-label="label"
        :placeholder="selectedLabel ? '' : 'Buscar producto...'"
        map-options emit-value use-input input-debounce="0"
        clearable
        @filter="productFilter"
        @clear="onClear"
        class="item-card__product"
        popup-content-class="item-dropdown"
      >
        <template v-slot:no-option>
          <q-item>
            <q-item-section class="text-caption text-accent">Sin productos — limpia el filtro de categoría o cambia de proveedor</q-item-section>
          </q-item>
        </template>
        <template v-slot:selected>
          <span v-if="selectedLabel" class="item-card__selected">{{ selectedLabel }}</span>
        </template>
        <template v-slot:option="{ itemProps, opt }">
          <q-item v-bind="itemProps" class="item-dropdown__opt">
            <q-item-section>
              <q-item-label class="item-dropdown__name">{{ opt.productName }}</q-item-label>
              <q-item-label caption class="item-dropdown__meta">
                <span v-if="opt.sku" class="item-dropdown__sku">{{ opt.sku }}</span>
                <span v-if="opt.categoryName" class="item-dropdown__cat">{{ opt.categoryName }}</span>
              </q-item-label>
            </q-item-section>
            <q-item-section side v-if="opt.proveedorName">
              <span class="item-dropdown__prov">{{ opt.proveedorName }}</span>
            </q-item-section>
          </q-item>
        </template>
      </q-select>
      <q-btn
        flat dense round icon="sym_r_close" size="xs"
        color="accent"
        @click="emit('remove')"
        aria-label="Eliminar item"
        class="item-card__remove"
      />
    </div>

    <!-- Divider -->
    <div class="item-card__divider"></div>

    <!-- Bottom: numeric fields grid -->
    <div class="item-card__fields">
      <div class="item-card__field item-card__field--qty">
        <span class="item-card__label">Cant</span>
        <q-input
          dark dense outlined
          :model-value="item.cantidad"
          @update:model-value="emit('update:cantidad', Number($event) || null)"
          type="text" inputmode="decimal"
          placeholder="0"
        />
      </div>

      <div class="item-card__field item-card__field--unit">
        <span class="item-card__label">Unidad</span>
        <q-select
          dark dense outlined
          :model-value="item.presentacionId"
          @update:model-value="emit('update:presentacionId', $event)"
          :options="unitOptions"
          map-options emit-value
          :disable="!item.productoId"
          placeholder="—"
        />
      </div>

      <div class="item-card__field item-card__field--valor">
        <span class="item-card__label">Valor $</span>
        <q-input
          dark dense outlined
          :model-value="item.valor"
          @update:model-value="emit('update:valor', Number($event) || null)"
          type="text" inputmode="decimal"
          placeholder="0.00"
        />
      </div>

      <div class="item-card__field item-card__field--calc">
        <span class="item-card__label">P.Unit</span>
        <div class="item-card__calc-val">
          <template v-if="precioUnitario != null">{{ fmt(precioUnitario) }}</template>
          <span v-else class="item-card__calc-empty">—</span>
        </div>
      </div>

      <div class="item-card__field item-card__field--disc">
        <span class="item-card__label">Dto%</span>
        <q-input
          dark dense outlined
          :model-value="item.descuento"
          @update:model-value="emit('update:descuento', Number($event) || 0)"
          type="text" inputmode="decimal"
          placeholder="0"
        />
      </div>

      <div class="item-card__field item-card__field--itbms">
        <span class="item-card__label">ITBMS</span>
        <q-select
          dark dense outlined
          :model-value="item.itbmsTasa"
          @update:model-value="emit('update:itbmsTasa', Number($event) ?? 0)"
          :options="itbmsOptions"
          map-options emit-value
          class="item-card__itbms"
        />
      </div>

      <div class="item-card__field item-card__field--subtotal">
        <span class="item-card__label">Subtotal</span>
        <div class="item-card__subtotal-wrap">
          <span class="item-card__subtotal" :class="{ 'item-card__subtotal--disc': item.descuento > 0 }">
            {{ fmt(subtotal) }}
          </span>
          <span v-if="itbmsMonto" class="item-card__itbms-val">+{{ fmt(itbmsMonto) }} ITBMS</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.item-card {
  background: color-mix(in srgb, var(--pq-surface) 85%, transparent);
  border: 1px solid color-mix(in srgb, var(--pq-border) 15%, transparent);
  border-radius: var(--pq-radius-md);
  padding: 8px 10px 10px;
  transition: border-color var(--pq-motion-fast);
}

.item-card:focus-within {
  border-color: color-mix(in srgb, var(--pq-accent) 50%, transparent);
}

.item-card__top {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
}

.item-card__num {
  font-family: var(--pq-font-utility);
  font-size: 0.65rem;
  font-weight: 700;
  color: color-mix(in srgb, var(--pq-accent) 40%, transparent);
  background: color-mix(in srgb, var(--pq-accent) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--pq-accent) 12%, transparent);
  border-radius: var(--pq-radius-xs);
  padding: 2px 5px;
  min-width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.item-card__product {
  flex: 1;
  min-width: 0;
}

.item-card__product :deep(.q-field__control) {
  min-height: 30px !important;
  border-radius: 5px !important;
  font-size: 0.85rem;
  padding: 0 8px !important;
}

.item-card__product :deep(.q-field__marginal) {
  height: 30px;
  min-width: 20px;
}

.item-card__product :deep(.q-field__native) {
  padding: 0 4px !important;
}

.item-card__selected {
  font-size: 0.85rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-card__placeholder {
  color: color-mix(in srgb, var(--pq-accent) 35%, transparent);
  font-size: 0.85rem;
}

.item-card__remove {
  opacity: 0;
  transition: opacity var(--pq-motion-fast);
  flex-shrink: 0;
}

.item-card:hover .item-card__remove {
  opacity: 0.35;
}

.item-card__remove:hover {
  opacity: 1 !important;
  background: color-mix(in srgb, var(--pq-danger) 15%, transparent);
}

.item-card__divider {
  height: 1px;
  background: color-mix(in srgb, var(--pq-border) 10%, transparent);
  margin: 6px 0 5px;
}

.item-card__fields {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.item-card__field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-card__label {
  font-size: 0.6rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: color-mix(in srgb, var(--pq-accent) 50%, transparent);
  padding-left: 2px;
}

.item-card__field--qty { width: 56px; }
.item-card__field--unit { width: 110px; }
.item-card__field--valor { width: 96px; }
.item-card__field--calc { width: 88px; }
.item-card__field--disc { width: 54px; }
.item-card__field--itbms { width: 92px; }
.item-card__field--subtotal { width: 115px; }

.item-card__field :deep(.q-field__control) {
  min-height: 30px !important;
  border-radius: var(--pq-radius-xs) !important;
  font-size: 0.82rem;
  font-family: var(--pq-font-utility);
  font-variant-numeric: tabular-nums;
}

.item-card__field :deep(.q-field__marginal) {
  height: 30px;
  min-width: 18px;
}

.item-card__calc-val {
  height: 30px;
  display: flex;
  align-items: center;
  padding: 0 6px;
  font-family: var(--pq-font-utility);
  font-size: 0.82rem;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: color-mix(in srgb, var(--pq-accent) 60%, transparent);
  background: color-mix(in srgb, var(--pq-surface) 50%, transparent);
  border-radius: var(--pq-radius-xs);
  border: 1px solid transparent;
}

.item-card__calc-empty {
  color: color-mix(in srgb, var(--pq-accent) 25%, transparent);
}

.item-card__itbms :deep(.q-field__control) { font-size: 0.78rem; }

.item-card__subtotal-wrap {
  height: 30px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 0;
  line-height: 1;
}

.item-card__subtotal {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 2px;
  font-family: var(--pq-font-utility);
  font-size: 0.9rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--pq-accent);
}

.item-card__itbms-val {
  font-family: var(--pq-font-utility);
  font-size: 0.62rem;
  font-weight: 600;
  color: color-mix(in srgb, var(--pq-accent) 55%, transparent);
}

.item-card__subtotal--disc {
  color: var(--pq-success);
}

@media (max-width: 599px) {
  .item-card {
    padding: 6px 8px 8px;
  }

  .item-card__fields {
    gap: 4px;
  }

  .item-card__field--qty { width: 48px; }
  .item-card__field--unit { width: 90px; }
  .item-card__field--valor { width: 80px; }
  .item-card__field--calc { width: 76px; }
  .item-card__field--disc { width: 46px; }
  .item-card__field--itbms { width: 80px; }
  .item-card__field--subtotal { width: 88px; }
}

.item-dropdown {
  font-size: 0.82rem;
}

.item-dropdown__opt {
  padding: 4px 10px;
}

.item-dropdown__name {
  font-weight: 500;
  font-size: 0.82rem;
}

.item-dropdown__meta {
  display: flex;
  gap: 4px;
  font-size: 0.72rem;
}

.item-dropdown__sku {
  font-family: var(--pq-font-utility);
  background: color-mix(in srgb, var(--pq-accent) 8%, transparent);
  padding: 1px 4px;
  border-radius: var(--pq-radius-2xs, 2px);
}

.item-dropdown__cat {
  color: color-mix(in srgb, var(--pq-accent) 50%, transparent);
}

.item-dropdown__prov {
  font-size: 0.72rem;
  color: color-mix(in srgb, var(--pq-accent) 60%, transparent);
}
</style>
