<template>
  <q-page class="core-page">
    <div class="q-mb-md fade-in-up">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Facturas</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Registro de facturas de proveedores</p>
    </div>

    <div class="facturas-toolbar">
      <q-input dark dense filled v-model="filter" placeholder="Buscar por número o proveedor..." class="facturas-toolbar__search">
        <template v-slot:prepend><q-icon name="search" /></template>
      </q-input>
      <q-space />
      <q-btn color="primary" icon="add" label="Nueva" @click="openCreate" />
    </div>

    <div v-if="!loading && !filteredRows.length" class="q-my-lg">
      <EmptyState
        icon="receipt_long"
        title="Sin facturas"
        message="Registra tu primera factura de proveedor para comenzar."
      />
    </div>

    <div v-if="loading" class="q-gutter-y-md">
      <div v-for="n in 3" :key="n">
        <q-skeleton type="text" dark animation="pulse" class="q-mb-sm" width="30%" height="16px" />
        <q-skeleton type="rect" dark animation="pulse" class="q-mb-xs" height="40px" />
        <q-skeleton type="rect" dark animation="pulse" class="q-mb-xs" height="40px" />
      </div>
    </div>

    <div v-for="group in monthGroups" :key="group.label" class="q-mb-lg">
      <div class="month-group-header">
        <span class="month-group-label">{{ group.label }}</span>
        <span class="month-group-count">{{ group.items.length }} factura{{ group.items.length !== 1 ? 's' : '' }}</span>
      </div>

      <div
        v-for="inv in group.items" :key="inv.id"
        class="invoice-row"
        @mouseenter="($event.currentTarget as HTMLElement).style.background = 'color-mix(in srgb, var(--pq-surface) 60%, transparent)'"
        @mouseleave="($event.currentTarget as HTMLElement).style.background = ''"
      >
        <div class="invoice-row__info">
          <div class="invoice-row__number">{{ inv.invoiceNumber }}</div>
          <div class="invoice-row__provider">{{ inv.providerName || '—' }}</div>
        </div>
        <div class="invoice-row__date">{{ inv.issueDate }}</div>
        <div class="invoice-row__total">{{ formatCurrency(inv.total) }}</div>
        <div class="invoice-row__status">
          <q-badge :color="statusColor(inv.status)" class="q-px-sm q-py-xs">{{ inv.status }}</q-badge>
        </div>
        <div class="invoice-row__actions">
          <q-btn flat dense round icon="visibility" color="accent" size="sm" @click="openDetail(inv)" aria-label="Ver detalles" />
          <q-btn v-if="inv.status === 'REGISTRADA'" flat dense round icon="edit" color="primary" size="sm" @click="openEdit(inv)" aria-label="Editar" />
          <q-btn v-if="inv.status === 'REGISTRADA'" flat dense round icon="paid" color="positive" size="sm" @click="confirmPay(inv)" aria-label="Marcar como pagada" />
          <q-btn v-if="inv.status === 'REGISTRADA'" flat dense round icon="delete" color="negative" size="sm" @click="confirmDelete(inv)" aria-label="Eliminar" />
        </div>
      </div>
    </div>

    <!-- Create Invoice Dialog -->
    <q-dialog v-model="dialogOpen" dark maximized transition-show="scale" transition-hide="fade">
      <q-card dark class="bg-surface-pine invoice-dialog">
        <q-form @submit.prevent="save" class="fit column no-wrap">
          <!-- Header -->
          <div class="invoice-dialog__header">
            <div class="invoice-dialog__title">
              <q-icon name="receipt_long" size="1.2rem" class="text-primary" />
              <span class="text-h6 text-primary q-ml-sm">{{ editingId ? 'Editar Factura' : 'Nueva Factura' }}</span>
            </div>
            <q-btn flat round icon="close" color="accent" v-close-popup size="sm" />
          </div>

          <q-separator dark class="opacity-20" />

          <!-- Scrollable body -->
          <q-card-section class="invoice-dialog__body col">
            <!-- Form header: 2x2 grid -->
            <div class="row q-col-gutter-x-sm q-col-gutter-y-sm q-mb-md">
              <div v-if="form.tipo !== 'GASTO_OPERATIVO'" class="col-12 col-sm-6">
                <q-select dark dense v-model="form.proveedorId" :options="providerFilteredOptions" label="Proveedor" :rules="[v=>!!v||'Requerido']" map-options emit-value use-input @filter="providerFilter" popup-content-class="product-dropdown">
                  <template v-slot:no-option><q-item><q-item-section class="text-accent text-caption">Escribe el nombre para crearlo</q-item-section></q-item></template>
                </q-select>
              </div>
              <div class="col-12 col-sm-6">
                <q-input dark dense v-model="form.fecha" label="Fecha" type="date" :rules="[v=>!!v||'Requerido']" />
              </div>
              <div class="col-12 col-sm-6">
                <q-select dark dense v-model="form.tipo" :options="['FACTURA','GASTO_OPERATIVO']" label="Tipo" :rules="[v=>!!v||'Requerido']" />
              </div>
              <div class="col-12 col-sm-6">
                <q-select dark dense v-model="form.metodoPago" :options="['EFECTIVO','TRANSFERENCIA','TARJETA','CHEQUE']" label="Método de pago" clearable />
              </div>
            </div>

            <!-- Items header -->
            <div v-if="form.tipo !== 'GASTO_OPERATIVO'" class="invoice-dialog__items-header">
              <div class="invoice-dialog__items-title">
                <q-icon name="list" size="0.85rem" class="text-primary" />
                <span>Items</span>
                <q-badge v-if="form.items.length" :label="form.items.length" color="accent" text-color="dark" class="q-ml-xs" />
              </div>
              <span v-if="form.items.length" class="invoice-dialog__items-total">{{ formatCurrency(computedTotal) }}</span>
            </div>

            <CategoryTabs v-if="form.tipo !== 'GASTO_OPERATIVO' && allProducts.length" v-model="activeCategory" :categories="setupCategories" />

            <div v-if="form.tipo !== 'GASTO_OPERATIVO'" class="invoice-dialog__items q-gutter-y-sm">
              <InvoiceItemCard
                v-for="(item, i) in form.items" :key="item._key"
                :item="item" :index="i"
                :product-options="filteredByCategory"
                :unit-options="unitOptions(item.productoId)"
              :presentation-conversion-map="presentationConversionMap"
              @update:productoId="onProductoChange(item, $event)"
                @update:presentacionId="onPresentacionChange(item, $event)"
                @update:cantidad="item.cantidad = $event"
                @update:valor="item.valor = $event"
                @update:descuento="item.descuento = $event"
                @remove="removeItem(i)"
              />
            </div>

            <div v-if="form.tipo !== 'GASTO_OPERATIVO'" class="invoice-dialog__add">
              <q-btn outline color="primary" icon="add" label="Agregar item" @click="addItem" no-caps size="sm" />
            </div>

            <!-- Gasto operativo: categoría desde CostosPage + monto -->
            <div v-if="form.tipo === 'GASTO_OPERATIVO'" class="q-gutter-y-sm q-mt-sm">
              <div class="invoice-dialog__items-title">
                <q-icon name="payments" size="0.85rem" class="text-primary" />
                <span>Categoría</span>
              </div>
              <q-select
                dark dense v-model="form.categoria"
                :options="categoriaOptions"
                label="Categoría"
                map-options emit-value
                :rules="[v => !!v || 'Requerido']"
              />
              <q-select
                v-if="form.categoria === 'SALARIOS'"
                dark dense v-model="form.colaboradorId"
                :options="colaboradorOptions"
                label="Colaborador"
                map-options emit-value
                clearable
              />
              <div v-if="form.categoria === 'SALARIOS' && selectedColaborador?.tipoPago === 'DIARIO'" class="row q-col-gutter-x-sm">
                <div class="col-6">
                  <q-input dark dense v-model="form.fechaDesde" label="Desde" type="date" />
                </div>
                <div class="col-6">
                  <q-input dark dense v-model="form.fechaHasta" label="Hasta" type="date" />
                </div>
              </div>
              <div v-if="form.categoria === 'SALARIOS' && selectedColaborador?.tipoPago === 'DIARIO'" class="text-caption text-accent q-mt-xs">
                {{ diasEnRango }} días × ${{ selectedColaborador.monto.toLocaleString('en-US') }} = {{ formatCurrency(diasEnRango * selectedColaborador.monto) }}
              </div>
              <q-input
                dark dense filled :model-value="totalStr"
                label="Total / Monto"
                placeholder="0.00"
                prefix="$"
                :rules="[() => !!form.total || 'Requerido']"
                @update:model-value="onTotalInput"
                @blur="formatTotal"
              />
            </div>

            <q-separator dark class="opacity-10 q-mt-sm" />
            <div v-if="form.items.length || form.tipo === 'GASTO_OPERATIVO'" class="invoice-dialog__total">
              <span class="invoice-dialog__total-label">Total factura</span>
              <span class="invoice-dialog__total-val">{{ formatCurrency(computedTotal) }}</span>
            </div>
          </q-card-section>

          <q-separator dark class="opacity-20" />

          <!-- Sticky footer -->
          <div class="invoice-dialog__footer">
            <q-btn flat label="Cancelar" color="accent" v-close-popup no-caps />
            <q-btn type="submit" label="Guardar" color="primary" :loading="saving" no-caps icon="save" />
          </div>
        </q-form>
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

    <InvoiceDetailDialog :factura="detailItem" v-model="detailDialog" :presentation-name-map="presentationNameMap" />
  </q-page>
</template>

<script setup lang="ts">
import { ref, shallowRef, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { formatCurrency } from 'src/utils/format'
import { facturaService } from '../services/factura.service'
import { productoService } from '../services/producto.service'
import { proveedorService } from '../services/proveedor.service'
import { costoService } from '../services/costo.service'
import type { Collaborador, Factura, FacturaRequest, GastoFijoRecurrente, SetupInfo, SetupCategory, ProductOption, Producto } from '../types'
import EmptyState from 'src/components/ui/EmptyState.vue'
import { api } from 'src/boot/axios'
import CategoryTabs from '../components/facturas/CategoryTabs.vue'
import InvoiceItemCard from '../components/facturas/InvoiceItemCard.vue'
import InvoiceDetailDialog from '../components/facturas/InvoiceDetailDialog.vue'
import ConfirmDialog from '../components/facturas/ConfirmDialog.vue'

useMeta({ title: 'Facturas — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId

interface OptionItem { label: string; value: string; __isCreate?: boolean }

const rows = ref<Factura[]>([])
const loading = shallowRef(false)
const filter = shallowRef('')
const editingId = shallowRef<string | null>(null)

const statusColor = (s: string) =>
  s === 'PAGADA' ? 'positive' : s === 'REGISTRADA' ? 'warning' : 'grey'

const filteredRows = computed(() => {
  if (!filter.value) return rows.value
  const q = filter.value.toLowerCase()
  return rows.value.filter(r =>
    r.invoiceNumber.toLowerCase().includes(q) ||
    (r.providerName ?? '').toLowerCase().includes(q)
  )
})

interface MonthGroup {
  label: string
  items: Factura[]
}

const monthGroups = computed(() => {
  const groups = new Map<string, Factura[]>()
  for (const inv of filteredRows.value) {
    const date = new Date(inv.issueDate)
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(inv)
  }
  const result: MonthGroup[] = []
  for (const list of groups.values()) {
    const date = new Date(list[0]!.issueDate)
    const label = date.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
    result.push({ label, items: list })
  }
  result.sort((a, b) => b.label.localeCompare(a.label))
  return result
})

const allProducts = ref<ProductOption[]>([])
const prodsData = ref<Producto[]>([])
const productPresentationsMap = ref<Map<string, { label: string; value: string }[]>>(new Map())
const activeCategory = shallowRef('')
const providerOptions = ref<{ label: string; value: string }[]>([])
const providerFilteredOptions = ref<OptionItem[]>([])
const setupCategories = ref<SetupCategory[]>([])
const setupUnits = ref<{ code: string; name: string }[]>([])
const detailDialog = shallowRef(false)
const detailItem = shallowRef<Factura | null>(null)
const presentationNameMap = ref<Map<string, string>>(new Map())
const presentationConversionMap = ref<Map<string, number>>(new Map())
const colaboradores = ref<Collaborador[]>([])
const gastosFijos = ref<GastoFijoRecurrente[]>([])

function openDetail(f: Factura) {
  detailItem.value = f
  detailDialog.value = true
}

function collectCategoryCodes(cats: SetupCategory[], codes: Set<string>) {
  for (const c of cats) {
    codes.add(c.code)
    if (c.children) collectCategoryCodes(c.children, codes)
  }
}

function findCategoryInTree(cats: SetupCategory[], code: string): Set<string> {
  const codes = new Set<string>()
  function walk(nodes: SetupCategory[]): boolean {
    for (const c of nodes) {
      if (c.code === code) {
        codes.add(c.code)
        if (c.children) collectCategoryCodes(c.children, codes)
        return true
      }
      if (c.children && walk(c.children)) return true
    }
    return false
  }
  walk(cats)
  return codes
}

const unitNameMap = computed(() => {
  const map = new Map<string, string>()
  for (const u of setupUnits.value) map.set(u.code, u.name)
  return map
})

const filteredByProvider = computed(() => {
  const providerId = form.value.proveedorId
  if (!providerId) return allProducts.value
  return allProducts.value.filter(p => p.proveedorId === providerId)
})

const filteredByCategory = computed(() => {
  if (!activeCategory.value) return filteredByProvider.value
  const codes = findCategoryInTree(setupCategories.value, activeCategory.value)
  return codes.size
    ? filteredByProvider.value.filter(p => codes.has(p.category))
    : filteredByProvider.value
})


function unitOptions(productId: string | null): { label: string; value: string }[] {
  if (!productId) return []
  return productPresentationsMap.value.get(productId) || []
}

let keyCounter = 0

interface ItemForm {
  _key: number
  productoId: string | null
  presentacionId: string | null
  cantidad: number | null
  valor: number | null
  descuento: number
}

const CATEGORIA_SALARIOS = 'SALARIOS'
const CATEGORIA_OTRO = 'OTRO'

const dialogOpen = shallowRef(false)
const saving = shallowRef(false)
const form = ref<{
  proveedorId: string | null
  fecha: string
  tipo: string
  metodoPago: string | null
  descuentoGlobal: number
  items: ItemForm[]
  total: number | null
  categoria: string | null
  colaboradorId: string | null
  fechaDesde: string | null
  fechaHasta: string | null
}>({
  proveedorId: null,
  fecha: new Date().toISOString().slice(0, 10),
  tipo: 'FACTURA',
  metodoPago: null,
  descuentoGlobal: 0,
  items: [],
  total: null,
  categoria: null,
  colaboradorId: null,
  fechaDesde: null,
  fechaHasta: null,
})

function addItem() {
  form.value.items.push({
    _key: ++keyCounter,
    productoId: null,
    presentacionId: null,
    cantidad: null,
    valor: null,
    descuento: 0,
  })
}

function onProductoChange(item: ItemForm, productoId: string | null) {
  item.productoId = productoId
  item.presentacionId = null
  const prod = allProducts.value.find(p => p.value === productoId)
  item.valor = prod?.lastUnitPrice ?? null
}

function onPresentacionChange(item: ItemForm, presId: string | null) {
  item.presentacionId = presId
}

function removeItem(i: number) {
  form.value.items.splice(i, 1)
}

const computedTotal = computed(() => {
  if (form.value.tipo === 'GASTO_OPERATIVO') return form.value.total || 0
  return form.value.items.reduce((sum, item) => {
    const qty = item.cantidad || 0
    const val = item.valor || 0
    const disc = item.descuento || 0
    return sum + (qty * val * (1 - disc / 100))
  }, 0)
})

const totalStr = ref('')

function onTotalInput(val: string | number | null) {
  totalStr.value = String(val ?? '').replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1')
}

function formatTotal() {
  const n = parseFloat(totalStr.value.replace(/,/g, ''))
  if (!isNaN(n) && totalStr.value) {
    totalStr.value = n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    form.value.total = n
  } else {
    form.value.total = null
  }
}

function rawTotal(val: number | null) {
  return val
    ? val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : ''
}

const colaboradorOptions = computed(() =>
  colaboradores.value.filter(c => c.activo).map(c => ({
    label: `${c.nombre} · ${c.tipoPago} $${c.monto.toLocaleString('en-US')}`,
    value: c.id,
  }))
)

const gastoFijoCategorias = computed(() => {
  const seen = new Set<string>()
  const out: { label: string; value: string; monto: number }[] = []
  for (const g of gastosFijos.value) {
    if (!g.activo || seen.has(g.categoria)) continue
    seen.add(g.categoria)
    out.push({ label: `${g.categoria} · $${g.monto.toLocaleString('en-US')}`, value: g.categoria, monto: g.monto })
  }
  return out
})

const categoriaOptions = computed(() => [
  { label: 'Salarios', value: CATEGORIA_SALARIOS },
  ...gastoFijoCategorias.value,
  { label: 'Otro', value: CATEGORIA_OTRO },
])

const selectedColaborador = computed(() =>
  colaboradores.value.find(c => c.id === form.value.colaboradorId) || null
)

const diasEnRango = computed(() => {
  if (!form.value.fechaDesde || !form.value.fechaHasta) return 0
  const ms = new Date(form.value.fechaHasta).getTime() - new Date(form.value.fechaDesde).getTime()
  // ponytail: cuenta días calendario (inclusive); días laborables vía config_laboral si hace falta
  return Math.max(1, Math.round(ms / 86400000) + 1)
})

function applyCategoria() {
  if (form.value.tipo !== 'GASTO_OPERATIVO') return
  form.value.colaboradorId = null
  const gf = gastoFijoCategorias.value.find(c => c.value === form.value.categoria)
  form.value.total = gf?.monto ?? null
  totalStr.value = rawTotal(form.value.total)
}

function applySalario() {
  if (form.value.tipo !== 'GASTO_OPERATIVO' || form.value.categoria !== CATEGORIA_SALARIOS) return
  const col = selectedColaborador.value
  form.value.total = col ? (col.tipoPago === 'DIARIO' ? col.monto * diasEnRango.value : col.monto) : null
  totalStr.value = rawTotal(form.value.total)
}

watch(() => form.value.categoria, applyCategoria)
watch(() => [form.value.colaboradorId, form.value.fechaDesde, form.value.fechaHasta], applySalario)

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
  proveedorService.create({ tenantId: tenantId as string, name }).then(res => {
    const newOpt = { label: res.data.name, value: res.data.id }
    providerOptions.value.push(newOpt)
    providerFilteredOptions.value = [...providerOptions.value]
    form.value.proveedorId = res.data.id
    $q.notify({ type: 'positive', message: `Proveedor "${name}" creado` })
  }).catch((err: unknown) => {
    form.value.proveedorId = null
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al crear proveedor' })
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
    total: null,
    categoria: null,
    colaboradorId: null,
    fechaDesde: null,
    fechaHasta: null,
  }
  totalStr.value = ''
  editingId.value = null
  keyCounter = 0
  providerFilteredOptions.value = [...providerOptions.value]
  dialogOpen.value = true
  await nextTick()
  addItem()
}

async function openEdit(factura: Factura) {
  if (!tenantId) return
  editingId.value = factura.id
  try {
    const res = await facturaService.getById(factura.id, tenantId)
    const f = res.data
    const gastoOperativo = f.type === 'GASTO_OPERATIVO'
    form.value = {
      proveedorId: f.providerId,
      fecha: f.issueDate,
      tipo: f.type,
      metodoPago: f.paymentMethod || null,
      descuentoGlobal: Number(f.globalDiscount || 0),
      items: gastoOperativo ? [] : f.items.map(item => ({
        _key: ++keyCounter,
        productoId: item.productId,
        presentacionId: item.presentacionId,
        cantidad: item.cantidadPresentacion ? Number(item.cantidadPresentacion) : (item.conversionFactor && item.conversionFactor > 1 ? Number(item.quantity) / item.conversionFactor : Number(item.quantity)),
        valor: item.valorPresentacion ? Number(item.valorPresentacion) : (item.conversionFactor && item.conversionFactor > 1 ? Number(item.unitPrice) * item.conversionFactor : Number(item.unitPrice)),
        descuento: item.descuentoEsPorcentaje && item.descuentoInput ? Number(item.descuentoInput) : (item.discount && item.quantity ? Number(item.discount) / Number(item.quantity) * 100 : 0),
      })),
      total: gastoOperativo ? Number(f.total || 0) : null,
      categoria: gastoOperativo ? CATEGORIA_OTRO : null,
      colaboradorId: null,
      fechaDesde: null,
      fechaHasta: null,
    }
    totalStr.value = gastoOperativo ? rawTotal(Number(f.total || 0)) : ''
    providerFilteredOptions.value = [...providerOptions.value]
    dialogOpen.value = true
    await nextTick()
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar factura para editar' })
  }
}

watch(() => form.value.proveedorId, onProviderSelected)

function mapProductsToOptions(prods: Producto[]): ProductOption[] {
  const catMap = new Map<string, string>()
  function walkCats(cats: SetupCategory[]) {
    for (const c of cats) {
      catMap.set(c.code, c.name)
      if (c.children?.length) walkCats(c.children)
    }
  }
  walkCats(setupCategories.value)

  return prods.map(p => ({
    label: `${p.name}${p.proveedorName ? ` · ${p.proveedorName}` : ''}`,
    value: p.id,
    productName: p.name,
    sku: p.sku,
    category: p.category,
    categoryName: catMap.get(p.category) || p.category,
    proveedorId: p.proveedorId,
    proveedorName: p.proveedorName,
    lastUnitPrice: p.lastUnitPrice,
  }))
}

async function loadDependencies() {
  if (!tenantId) return
  try {
    const [provs, setupRes, prodsRes, colRes, gfRes] = await Promise.all([
      proveedorService.getAll(tenantId),
      api.get<SetupInfo>(`/core/setup/${tenantId}`),
      productoService.search(tenantId, { page: 0, size: 100 }),
      costoService.getAllCollaboradores(tenantId),
      costoService.getAllGastosFijos(tenantId),
    ])
    colaboradores.value = colRes.data
    gastosFijos.value = gfRes.data
    setupCategories.value = setupRes.data.categories || []
    setupUnits.value = setupRes.data.units || []
    const provOpts = provs.data.map(p => ({ label: p.name, value: p.id }))
    providerOptions.value = provOpts
    providerFilteredOptions.value = [...provOpts]
    prodsData.value = prodsRes.data.content
    allProducts.value = mapProductsToOptions(prodsRes.data.content)
    const presMap = new Map<string, { label: string; value: string }[]>()
    const presNameMap = new Map<string, string>()
    const convMap = new Map<string, number>()
    for (const p of prodsRes.data.content) {
      const baseUnitName = unitNameMap.value.get(p.baseUnit) || p.baseUnit
      const unitOpts: { label: string; value: string }[] = [{ label: baseUnitName, value: '' }]
      for (const pres of (p.presentaciones || [])) {
        unitOpts.push({ label: pres.name, value: pres.id })
        presNameMap.set(pres.id, pres.name)
        convMap.set(pres.id, pres.conversion)
      }
      presMap.set(p.id, unitOpts)
    }
    presentationNameMap.value = presNameMap
    presentationConversionMap.value = convMap
    productPresentationsMap.value = presMap
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Error al cargar datos del formulario'
    $q.notify({ type: 'negative', message: msg })
  }
}

async function save() {
  if (!tenantId) return
  const gastoOperativo = form.value.tipo === 'GASTO_OPERATIVO'
  if (gastoOperativo && !(form.value.total && form.value.total > 0)) {
    $q.notify({ type: 'negative', message: 'Ingresa un monto total mayor a cero' })
    return
  }
  saving.value = true
  try {
    const payload: FacturaRequest = {
      tenantId,
      proveedorId: gastoOperativo ? null : form.value.proveedorId,
      fecha: form.value.fecha,
      tipo: form.value.tipo,
      metodoPago: form.value.metodoPago,
      descuentoGlobal: form.value.descuentoGlobal || 0,
      total: gastoOperativo ? form.value.total : null,
      items: gastoOperativo ? [] : form.value.items.map(item => {
        const conv = item.presentacionId
          ? (presentationConversionMap.value.get(item.presentacionId) || 1)
          : 1
        const val = item.valor || 0
        return {
          productoId: item.productoId!,
          presentacionId: item.presentacionId || null,
          cantidadPresentacion: item.cantidad || 0,
          valorPresentacion: val,
          precioUnitario: conv > 0 ? val / conv : val,
          descuento: (item.cantidad || 0) * val * ((item.descuento || 0) / 100),
          descuentoInput: item.descuento || 0,
          descuentoEsPorcentaje: true,
        }
      }),
    }
    let res
    if (editingId.value) {
      res = await facturaService.update(editingId.value, payload)
      const idx = rows.value.findIndex(r => r.id === editingId.value)
      if (idx >= 0) rows.value[idx] = res.data
      $q.notify({ type: 'positive', message: 'Factura actualizada: ' + res.data.invoiceNumber })
    } else {
      res = await facturaService.create(payload)
      rows.value.unshift(res.data)
      $q.notify({ type: 'positive', message: 'Factura creada: ' + res.data.invoiceNumber })
    }
    dialogOpen.value = false
    editingId.value = null
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Error al guardar factura'
    $q.notify({ type: 'negative', message: msg })
  } finally { saving.value = false }
}

const payDialog = shallowRef(false)
const payingItem = shallowRef<Factura | null>(null)
const paying = shallowRef(false)

function confirmPay(f: Factura) {
  payingItem.value = f
  payDialog.value = true
}

async function pay() {
  if (!payingItem.value || !tenantId) return
  paying.value = true
  try {
    const res = await facturaService.pay(payingItem.value.id, tenantId)
    const idx = rows.value.findIndex(r => r.id === payingItem.value!.id)
    if (idx >= 0) rows.value[idx] = res.data
    payDialog.value = false
    $q.notify({ type: 'positive', message: 'Factura pagada' })
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al pagar factura' })
  } finally {
    paying.value = false
    payingItem.value = null
  }
}

const deleteDialog = shallowRef(false)
const deletingItem = shallowRef<Factura | null>(null)
const deleting = shallowRef(false)

function confirmDelete(f: Factura) {
  deletingItem.value = f
  deleteDialog.value = true
}

async function remove() {
  if (!deletingItem.value || !tenantId) return
  deleting.value = true
  try {
    await facturaService.remove(deletingItem.value.id, tenantId)
    rows.value = rows.value.filter(r => r.id !== deletingItem.value!.id)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Factura eliminada' })
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al eliminar factura' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
}

async function load() {
  if (!tenantId) return
  loading.value = true
  try {
    const res = await facturaService.getAll(tenantId)
    rows.value = res.data
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar facturas' })
  } finally { loading.value = false }
}

onMounted(async () => {
  if (!tenantId) return;
  await Promise.all([load(), loadDependencies()])
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

watch(() => dialogOpen.value, (open) => {
  if (!open) editingId.value = null
})

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    void openCreate()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 's' && dialogOpen.value) {
    e.preventDefault()
    void save()
  }
}
</script>

<style scoped>
.invoice-dialog {
  max-width: 640px;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.invoice-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
}

.invoice-dialog__title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.invoice-dialog__body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 10px 16px;
}

.invoice-dialog__items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0 4px;
}

.invoice-dialog__items-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--pq-text-muted);
}

.invoice-dialog__items-total {
  font-size: 0.85rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-accent);
}

.invoice-dialog__items {
  min-height: 0;
}

.invoice-dialog__add {
  padding: 8px 0 4px;
}

.invoice-dialog__add :deep(.q-btn) {
  width: 100%;
  border-style: dashed;
  opacity: 0.5;
}

.invoice-dialog__add :deep(.q-btn:hover) {
  opacity: 1;
}

.invoice-dialog__total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 10px;
  padding: 8px 0 0;
}

.invoice-dialog__total-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--pq-accent-muted);
}

.invoice-dialog__total-val {
  font-family: var(--pq-font-utility);
  font-size: 1.1rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--pq-accent);
}

.invoice-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 16px;
}

.product-dropdown {
  font-size: 0.82rem;
}

.facturas-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.facturas-toolbar__search {
  max-width: 320px;
}

.month-group-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid color-mix(in srgb, var(--pq-border) 15%, transparent);
  margin-bottom: 8px;
}

.month-group-label {
  font-weight: 700;
  text-transform: capitalize;
  color: var(--pq-text);
  font-size: 0.95rem;
}

.month-group-count {
  color: var(--pq-text-muted);
  font-size: 0.8rem;
}

.invoice-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: background var(--pq-motion-fast);
  border-bottom: 1px solid color-mix(in srgb, var(--pq-border) 6%, transparent);
}

.invoice-row:hover {
  background: color-mix(in srgb, var(--pq-surface) 30%, transparent);
}

.invoice-row__info {
  flex: 1;
  min-width: 0;
}

.invoice-row__number {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invoice-row__provider {
  font-size: 0.8rem;
  color: var(--pq-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invoice-row__date {
  font-size: 0.8rem;
  color: var(--pq-text-muted);
  margin-right: 16px;
  white-space: nowrap;
}

.invoice-row__total {
  font-weight: 700;
  margin-right: 16px;
  font-family: var(--pq-font-utility);
  white-space: nowrap;
  color: var(--pq-accent);
}

.invoice-row__status {
  margin-right: 16px;
}

.invoice-row__actions {
  display: flex;
  gap: 2px;
}
</style>
