<script setup lang="ts">
import { ref, computed } from 'vue'

export interface ProductOption { label: string; value: string; productName: string; sku: string; category: string }

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

const subtotal = computed(() => {
  const qty = props.item.cantidad || 0
  const price = props.item.precioUnitario || 0
  const disc = props.item.descuento || 0
  return formatCurrency(qty * price - disc)
})

const filteredProducts = computed(() => {
  const needle = search.value.toLowerCase()
  return props.productOptions.filter(p =>
    !needle || p.label.toLowerCase().includes(needle) || p.sku.toLowerCase().includes(needle)
  )
})

function formatCurrency(n: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(n)
}

function filterProducts(val: string, update: (fn: () => void) => void) {
  search.value = val
  update(() => { /* filteredProducts computed re-evaluates automatically */ })
}
</script>

<template>
  <div class="invoice-item-card q-mb-md q-pa-md" role="group" :aria-label="`Item ${index + 1}`">
    <div class="row q-col-gutter-sm items-center">
      <div class="col-10">
        <q-select dark dense filled :model-value="item.productoId"
          @update:model-value="emit('update:productoId', $event)"
          :options="filteredProducts" label="Producto"
          map-options emit-value use-input @filter="filterProducts">
          <template v-slot:option="{ itemProps, opt }">
            <q-item v-bind="itemProps">
              <q-item-section avatar>
                <q-icon name="inventory_2" size="1.1rem" color="accent" />
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ opt.productName }}</q-item-label>
                <q-item-label caption class="text-accent">{{ opt.sku }}</q-item-label>
              </q-item-section>
              <q-item-section v-if="opt.category" side>
                <q-badge :label="opt.category" color="dark" text-color="accent" class="q-px-sm" />
              </q-item-section>
            </q-item>
          </template>
        </q-select>
      </div>
      <div class="col-2 text-right">
        <q-btn flat dense round icon="close" color="negative" size="sm" @click="emit('remove')"
          aria-label="Eliminar item" class="remove-item-btn" />
      </div>
    </div>
    <div class="row q-col-gutter-sm items-start q-mt-sm">
      <div class="col-3">
        <q-input dark dense filled :model-value="item.cantidad"
          @update:model-value="emit('update:cantidad', Number($event))"
          label="Cantidad" type="number" min="0.01" step="0.01" />
      </div>
      <div class="col-3">
        <q-select dark dense filled :model-value="item.presentacionId"
          @update:model-value="emit('update:presentacionId', $event)"
          :options="unitOptions" label="Unidad" map-options emit-value
          :disable="!item.productoId" />
      </div>
      <div class="col-3">
        <q-input dark dense filled :model-value="item.precioUnitario"
          @update:model-value="emit('update:precioUnitario', Number($event))"
          label="Precio Unit." type="number" min="0" step="0.01" prefix="$" />
      </div>
      <div class="col-3">
        <q-input dark dense filled :model-value="item.descuento"
          @update:model-value="emit('update:descuento', Number($event))"
          label="Descuento" type="number" min="0" step="0.01" prefix="$" />
      </div>
    </div>
    <div class="row q-col-gutter-sm items-center q-mt-xs">
      <div class="col-3"></div>
      <div class="col-9 text-right">
        <div class="text-caption text-accent">Subtotal: <span class="text-weight-bold text-secondary text-body2">{{ subtotal }}</span></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.invoice-item-card {
  background: linear-gradient(
    170deg,
    rgba(163, 120, 94, 0.04) 0%,
    rgba(27, 38, 36, 0.5) 100%
  );
  border: 1px solid rgba(113, 131, 127, 0.1);
  border-radius: 10px;
  transition: border-color 0.2s ease;

  &:hover {
    border-color: rgba(163, 120, 94, 0.2);
  }

  &:focus-within {
    border-color: rgba(163, 120, 94, 0.35);
    box-shadow: 0 0 12px rgba(163, 120, 94, 0.08);
  }
}

.remove-item-btn {
  opacity: 0.4;
  transition: opacity 0.15s ease, transform 0.15s ease;

  &:hover {
    opacity: 1;
    transform: scale(1.15);
  }
}

/* ponytail: hide increment/decrement spin buttons for number inputs */
:deep(input[type="number"]) {
  -moz-appearance: textfield;
}
:deep(input[type="number"]::-webkit-outer-spin-button),
:deep(input[type="number"]::-webkit-inner-spin-button) {
  -webkit-appearance: none;
  margin: 0;
}
</style>
