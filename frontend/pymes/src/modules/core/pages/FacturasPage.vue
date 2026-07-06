<template>
  <q-page class="core-page">
    <div class="q-mb-md fade-in-up">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Facturas</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Registro de facturas de proveedores</p>
    </div>

    <q-card dark class="bg-surface-pine">
      <q-table
        dark flat
        :rows="rows"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :filter="filter"
        v-model:pagination="pagination"
        :rows-per-page-options="[10, 20, 50]"
      >
        <template v-slot:top>
          <q-input dark dense filled v-model="filter" placeholder="Buscar..." class="q-mr-sm" style="max-width: 250px">
            <template v-slot:prepend><q-icon name="search" /></template>
          </q-input>
          <q-space />
          <q-btn color="primary" icon="add" label="Nueva" @click="openCreate" />
        </template>

        <template v-slot:body-cell-status="{ row }">
          <td>
            <q-badge :color="statusColor(row.status)" class="q-px-sm q-py-xs">
              {{ row.status }}
            </q-badge>
          </td>
        </template>

        <template v-slot:body-cell-total="{ row }">
          <td class="text-right text-weight-bold">{{ formatCurrency(row.total) }}</td>
        </template>

        <template v-slot:body-cell-actions="{ row }">
          <td class="text-right">
            <q-btn
              v-if="row.status === 'REGISTRADA'"
              flat dense round icon="paid" color="positive"
              @click="confirmPay(row)"
              aria-label="Marcar como pagada"
            >
              <q-tooltip>Marcar como pagada</q-tooltip>
            </q-btn>
            <q-btn
              v-if="row.status === 'REGISTRADA'"
              flat dense round icon="delete" color="negative"
              @click="confirmDelete(row)"
              aria-label="Eliminar factura"
            />
          </td>
        </template>
      </q-table>
    </q-card>

    <!-- Create Invoice Dialog -->
    <q-dialog v-model="dialogOpen" dark maximized>
      <q-card dark class="bg-surface-pine" style="max-width: 700px">
        <q-card-section>
          <div class="text-h6 text-primary">Nueva Factura</div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form @submit.prevent="save" class="q-gutter-y-md">
            <q-select
              dark filled v-model="form.proveedorId"
              :options="providerFilteredOptions"
              label="Proveedor"
              :rules="[v => !!v || 'Requerido']"
              map-options emit-value
              use-input
              @filter="providerFilter"
            >
              <template v-slot:option="{ itemProps, opt }">
                <q-item v-bind="itemProps">
                  <q-item-section>
                    <span :class="opt.__isCreate ? 'text-primary' : ''">{{ opt.label }}</span>
                  </q-item-section>
                </q-item>
              </template>
              <template v-slot:no-option>
                <q-item><q-item-section class="text-accent text-caption">Escribe el nombre para crearlo</q-item-section></q-item>
              </template>
            </q-select>

            <div class="row q-col-gutter-md">
              <div class="col-6">
                <q-input dark filled v-model="form.fecha" label="Fecha" type="date" :rules="[v => !!v || 'Requerido']" />
              </div>
              <div class="col-6">
                <q-select dark filled v-model="form.tipo" :options="['FACTURA', 'GASTO_OPERATIVO']" label="Tipo" :rules="[v => !!v || 'Requerido']" />
              </div>
            </div>

            <q-select dark filled v-model="form.metodoPago" :options="['EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'CHEQUE']" label="Método de pago" clearable />

            <q-separator dark />
            <div class="text-subtitle2 text-primary q-mb-sm">Items</div>

            <CategoryTabs v-model="activeCategory" :categories="productCategories" />

            <InvoiceItemCard
              v-for="(item, i) in form.items" :key="item._key"
              :item="item" :index="i"
              :product-options="filteredByCategory"
              :unit-options="unitOptions(item.productoId)"
              @update:productoId="item.productoId = $event; item.presentacionId = null"
              @update:presentacionId="item.presentacionId = $event"
              @update:cantidad="item.cantidad = $event"
              @update:precioUnitario="item.precioUnitario = $event"
              @update:descuento="item.descuento = $event"
              @remove="removeItem(i)"
            />

            <q-btn outline color="primary" icon="add" label="Agregar item" @click="addItem" class="q-mt-sm" />

            <div class="row justify-end">
              <div class="text-body1 text-weight-bold text-secondary">
                Total: {{ formatCurrency(computedTotal) }}
              </div>
            </div>

            <q-separator dark />

            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar Factura" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <ConfirmDialog
      v-model="payDialog"
      icon="paid" icon-color="positive"
      :message="`Marcar como pagada la factura <strong>${payingItem?.invoiceNumber}</strong>?`"
      confirm-label="Confirmar Pago" confirm-color="positive"
      :loading="paying" @confirm="pay"
    />

    <ConfirmDialog
      v-model="deleteDialog"
      icon="warning" icon-color="negative"
      :message="`¿Eliminar factura <strong>${deletingItem?.invoiceNumber}</strong>?`"
      confirm-label="Eliminar" confirm-color="negative"
      :loading="deleting" @confirm="remove"
    />
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { facturaService } from '../services/factura.service'
import { productoService } from '../services/producto.service'
import { proveedorService } from '../services/proveedor.service'
import type { Factura, FacturaRequest } from '../types'
import CategoryTabs from '../components/facturas/CategoryTabs.vue'
import InvoiceItemCard from '../components/facturas/InvoiceItemCard.vue'
import ConfirmDialog from '../components/facturas/ConfirmDialog.vue'

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const rows = ref<Factura[]>([])
const loading = ref(false)
const filter = ref('')
const pagination = ref({ sortBy: 'issueDate', descending: true, page: 1, rowsPerPage: 15 })

const allProducts = ref<{ label: string; value: string; productName: string; sku: string; category: string }[]>([])
const productPresentationsMap = ref<Map<string, { label: string; value: string }[]>>(new Map())
const activeCategory = ref('')
const providerOptions = ref<{ label: string; value: string }[]>([])
const providerFilteredOptions = ref<OptionItem[]>([])

const productCategories = computed(() => {
  const cats = new Set(allProducts.value.map(p => p.category).filter(Boolean))
  return Array.from(cats).sort()
})

const filteredByCategory = computed(() =>
  !activeCategory.value
    ? allProducts.value
    : allProducts.value.filter(p => p.category === activeCategory.value)
)

const columns = [
  { name: 'invoiceNumber', label: 'N° Factura', field: 'invoiceNumber', align: 'left' as const, sortable: true },
  { name: 'providerName', label: 'Proveedor', field: 'providerName', align: 'left' as const, sortable: true },
  { name: 'issueDate', label: 'Fecha', field: 'issueDate', align: 'center' as const, sortable: true },
  { name: 'status', label: 'Estado', field: 'status', align: 'center' as const, sortable: false },
  { name: 'total', label: 'Total', field: 'total', align: 'right' as const, sortable: true },
  { name: 'actions', label: 'Acciones', field: 'id', align: 'right' as const, sortable: false },
]

const statusColor = (s: string) =>
  s === 'PAGADA' ? 'positive' : s === 'REGISTRADA' ? 'warning' : 'grey'
const formatCurrency = (n: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(n)

function unitOptions(productId: string | null): { label: string; value: string }[] {
  if (!productId) return []
  return productPresentationsMap.value.get(productId) || []
}

let keyCounter = 0
interface OptionItem { label: string; value: string; __isCreate?: boolean }

interface ItemForm {
  _key: number
  productoId: string | null
  presentacionId: string | null
  cantidad: number | null
  precioUnitario: number | null
  descuento: number
}

const dialogOpen = ref(false)
const saving = ref(false)
const form = ref<{
  proveedorId: string | null
  fecha: string
  tipo: string
  metodoPago: string | null
  descuentoGlobal: number
  items: ItemForm[]
}>({
  proveedorId: null,
  fecha: new Date().toISOString().slice(0, 10),
  tipo: 'FACTURA',
  metodoPago: null,
  descuentoGlobal: 0,
  items: [],
})

function addItem() {
  form.value.items.push({
    _key: ++keyCounter,
    productoId: null,
    presentacionId: null,
    cantidad: null,
    precioUnitario: null,
    descuento: 0,
  })
}

function removeItem(i: number) {
  form.value.items.splice(i, 1)
}

const computedTotal = computed(() => {
  const itemsTotal = form.value.items.reduce((sum, item) => {
    const qty = item.cantidad || 0
    const price = item.precioUnitario || 0
    const disc = item.descuento || 0
    return sum + (qty * price - disc)
  }, 0)
  const gd = form.value.descuentoGlobal || 0
  return Math.max(0, itemsTotal - gd)
})

function providerFilter(val: string, update: (fn: () => void) => void) {
  update(() => {
    if (!val) {
      providerFilteredOptions.value = [...providerOptions.value]
      return
    }
    const needle = val.toLowerCase()
    const filtered: OptionItem[] = providerOptions.value.filter(p => p.label.toLowerCase().includes(needle))
    if (filtered.length === 0 && val.trim()) {
      filtered.push({ label: `+ Crear "${val.trim()}"`, value: `__CREATE__${val.trim()}`, __isCreate: true })
    }
    providerFilteredOptions.value = filtered
  })
}

function onProviderSelected(val: string | null) {
  if (!val || !val.startsWith('__CREATE__')) return
  const name = val.replace('__CREATE__', '')
  proveedorService.create({ tenantId, name, ruc: null }).then(res => {
    const newOpt = { label: res.data.name, value: res.data.id }
    providerOptions.value.push(newOpt)
    providerFilteredOptions.value = [...providerOptions.value]
    form.value.proveedorId = res.data.id
    $q.notify({ type: 'positive', message: `Proveedor "${name}" creado` })
  }).catch(() => {
    form.value.proveedorId = null
    $q.notify({ type: 'negative', message: 'Error al crear proveedor' })
  })
}

async function openCreate() {
  form.value = {
    proveedorId: null,
    fecha: new Date().toISOString().slice(0, 10),
    tipo: 'FACTURA',
    metodoPago: null,
    descuentoGlobal: 0,
    items: [],
  }
  keyCounter = 0
  providerFilteredOptions.value = [...providerOptions.value]
  dialogOpen.value = true
  await nextTick()
  addItem()
}

watch(() => form.value.proveedorId, onProviderSelected)

async function loadDependencies() {
  try {
    const [prods, provs] = await Promise.all([
      productoService.getAll(tenantId),
      proveedorService.getAll(tenantId),
    ])
    allProducts.value = prods.data.map(p => ({
      label: `${p.name} (${p.sku})`,
      value: p.id,
      productName: p.name,
      sku: p.sku,
      category: p.category,
    }))
    const presMap = new Map<string, { label: string; value: string }[]>()
    for (const p of prods.data) {
      const opts: { label: string; value: string }[] = [{ label: p.baseUnit, value: '' }]
      for (const pres of (p.presentaciones || [])) {
        opts.push({ label: pres.name, value: pres.id })
      }
      presMap.set(p.id, opts)
    }
    productPresentationsMap.value = presMap
    const provOpts = provs.data.map(p => ({ label: p.name, value: p.id }))
    providerOptions.value = provOpts
    providerFilteredOptions.value = [...provOpts]
  } catch {
    $q.notify({ type: 'negative', message: 'Error al cargar datos del formulario' })
  }
}

async function save() {
  saving.value = true
  try {
    const payload: FacturaRequest = {
      tenantId,
      proveedorId: form.value.proveedorId!,
      fecha: form.value.fecha,
      tipo: form.value.tipo,
      metodoPago: form.value.metodoPago,
      descuentoGlobal: form.value.descuentoGlobal || 0,
      items: form.value.items.map(item => ({
        productoId: item.productoId!,
        presentacionId: item.presentacionId || null,
        cantidad: item.cantidad || 0,
        precioUnitario: item.precioUnitario || 0,
        descuento: item.descuento || 0,
      })),
    }
    const res = await facturaService.create(payload)
    rows.value.unshift(res.data)
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: 'Factura creada: ' + res.data.invoiceNumber })
  } catch {
    $q.notify({ type: 'negative', message: 'Error al crear factura' })
  } finally { saving.value = false }
}

const payDialog = ref(false)
const payingItem = ref<Factura | null>(null)
const paying = ref(false)

function confirmPay(f: Factura) {
  payingItem.value = f
  payDialog.value = true
}

async function pay() {
  if (!payingItem.value) return
  paying.value = true
  try {
    const res = await facturaService.pay(payingItem.value.id, tenantId)
    const idx = rows.value.findIndex(r => r.id === payingItem.value!.id)
    if (idx >= 0) rows.value[idx] = res.data
    payDialog.value = false
    $q.notify({ type: 'positive', message: 'Factura pagada' })
  } catch {
    $q.notify({ type: 'negative', message: 'Error al pagar factura' })
  } finally {
    paying.value = false
    payingItem.value = null
  }
}

const deleteDialog = ref(false)
const deletingItem = ref<Factura | null>(null)
const deleting = ref(false)

function confirmDelete(f: Factura) {
  deletingItem.value = f
  deleteDialog.value = true
}

async function remove() {
  if (!deletingItem.value) return
  deleting.value = true
  try {
    await facturaService.remove(deletingItem.value.id, tenantId)
    rows.value = rows.value.filter(r => r.id !== deletingItem.value!.id)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Factura eliminada' })
  } catch {
    $q.notify({ type: 'negative', message: 'Error al eliminar factura' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
}

async function load() {
  loading.value = true
  try {
    const res = await facturaService.getAll(tenantId)
    rows.value = res.data
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar facturas' })
  } finally { loading.value = false }
}

onMounted(async () => {
  await Promise.all([load(), loadDependencies()])
})
</script>
