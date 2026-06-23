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
            >
              <q-tooltip>Marcar como pagada</q-tooltip>
            </q-btn>
            <q-btn
              v-if="row.status === 'REGISTRADA'"
              flat dense round icon="delete" color="negative"
              @click="confirmDelete(row)"
            />
          </td>
        </template>
      </q-table>
    </q-card>

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
              :options="providerOptions" label="Proveedor"
              :rules="[v => !!v || 'Requerido']"
              map-options emit-value
            >
              <template v-slot:option="{ itemProps, opt }">
                <q-item v-bind="itemProps"><q-item-section>{{ opt.label }}</q-item-section></q-item>
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
            <div class="text-subtitle2 text-primary">Items</div>

            <div v-for="(item, i) in form.items" :key="item._key" class="row q-col-gutter-sm items-end">
              <div class="col-4">
                <q-select dark dense filled v-model="item.productoId" :options="productOptions" label="Producto"
                  map-options emit-value use-input @filter="filterProducts">
                  <template v-slot:option="{ itemProps, opt }">
                    <q-item v-bind="itemProps"><q-item-section>{{ opt.label }}</q-item-section></q-item>
                  </template>
                </q-select>
              </div>
              <div class="col-2">
                <q-input dark dense filled v-model.number="item.cantidad" label="Cant." type="number" min="0.01" step="0.01" />
              </div>
              <div class="col-2">
                <q-input dark dense filled v-model.number="item.precioUnitario" label="P.Unit." type="number" min="0" step="0.01" />
              </div>
              <div class="col-2">
                <q-input dark dense filled v-model.number="item.descuento" label="Desc." type="number" min="0" step="0.01" />
              </div>
              <div class="col-1 text-center text-caption text-accent">
                {{ itemSubtotal(item) }}
              </div>
              <div class="col-1">
                <q-btn flat dense round icon="close" color="negative" size="sm" @click="removeItem(i)" />
              </div>
            </div>

            <q-btn flat dense color="primary" icon="add" label="Agregar item" @click="addItem" />

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

    <q-dialog v-model="payDialog" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="paid" color="positive" size="md" />
          <span>Marcar como pagada la factura <strong>{{ payingItem?.invoiceNumber }}</strong>?</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Confirmar Pago" color="positive" :loading="paying" @click="pay" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span>¿Eliminar factura <strong>{{ deletingItem?.invoiceNumber }}</strong>?</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Eliminar" color="negative" :loading="deleting" @click="remove" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { facturaService } from '../services/factura.service'
import { productoService } from '../services/producto.service'
import { proveedorService } from '../services/proveedor.service'
import type { Factura, FacturaRequest } from '../types'

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const rows = ref<Factura[]>([])
const loading = ref(false)
const filter = ref('')
const pagination = ref({ sortBy: 'issueDate', descending: true, page: 1, rowsPerPage: 15 })
const productOptions = ref<{ label: string; value: string }[]>([])
const allProducts = ref<{ label: string; value: string }[]>([])
const providerOptions = ref<{ label: string; value: string }[]>([])

const columns = [
  { name: 'invoiceNumber', label: 'N° Factura', field: 'invoiceNumber', align: 'left' as const, sortable: true },
  { name: 'providerName', label: 'Proveedor', field: 'providerName', align: 'left' as const, sortable: true },
  { name: 'issueDate', label: 'Fecha', field: 'issueDate', align: 'center' as const, sortable: true },
  { name: 'status', label: 'Estado', field: 'status', align: 'center' as const, sortable: false },
  { name: 'total', label: 'Total', field: 'total', align: 'right' as const, sortable: true },
  { name: 'actions', label: 'Acciones', field: 'id', align: 'right' as const, sortable: false },
]

// ponytail: hardcoded status colors, use config if statuses become dynamic
const statusColor = (s: string) =>
  s === 'PAGADA' ? 'positive' : s === 'REGISTRADA' ? 'warning' : 'grey'
const formatCurrency = (n: number) =>
  new Intl.NumberFormat('es-PY', { style: 'currency', currency: 'PYG', minimumFractionDigits: 0 }).format(n)

let keyCounter = 0
interface ItemForm {
  _key: number
  productoId: string | null
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
    cantidad: null,
    precioUnitario: null,
    descuento: 0,
  })
}

function removeItem(i: number) {
  form.value.items.splice(i, 1)
}

const itemSubtotal = (item: ItemForm) => {
  const qty = item.cantidad || 0
  const price = item.precioUnitario || 0
  const disc = item.descuento || 0
  const st = qty * price - disc
  return formatCurrency(st)
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

function filterProducts(val: string, update: (fn: () => void) => void) {
  update(() => {
    const needle = val.toLowerCase()
    productOptions.value = allProducts.value.filter(
      p => p.label.toLowerCase().includes(needle)
    )
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
  dialogOpen.value = true
  await nextTick()
  addItem()
}

async function loadDependencies() {
  try {
    const [prods, provs] = await Promise.all([
      productoService.getAll(tenantId),
      proveedorService.getAll(tenantId),
    ])
    const opts = prods.data.map(p => ({ label: `${p.name} (${p.sku})`, value: p.id }))
    allProducts.value = opts
    productOptions.value = [...opts]
    providerOptions.value = provs.data.map(p => ({ label: p.name, value: p.id }))
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
