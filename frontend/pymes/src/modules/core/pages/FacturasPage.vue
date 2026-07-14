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
            <q-btn flat dense round icon="visibility" color="accent" @click="openDetail(row)" aria-label="Ver detalles">
              <q-tooltip>Ver detalles</q-tooltip>
            </q-btn>
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

        <template v-slot:no-data>
          <EmptyState
            v-if="!loading"
            icon="receipt_long"
            title="Sin facturas"
            message="Registra tu primera factura de proveedor para comenzar."
          >
            <q-btn color="primary" icon="add" label="Nueva Factura" @click="openCreate" class="q-mt-sm" />
          </EmptyState>
        </template>
      </q-table>
    </q-card>

    <!-- Create Invoice Dialog -->
    <q-dialog v-model="dialogOpen" dark maximized transition-show="scale" transition-hide="fade">
      <q-card dark class="bg-surface-pine invoice-dialog" @before-hide="onDialogBeforeHide">
        <!-- Dialog header -->
        <div class="invoice-dialog__header">
          <div class="invoice-dialog__header-content">
            <div class="invoice-dialog__title">
              <q-icon name="receipt_long" size="1.4rem" class="text-primary" />
              <span class="text-h6 text-primary q-ml-sm">Nueva Factura</span>
            </div>
            <div class="invoice-dialog__subtitle">Registro de compra a proveedor</div>
          </div>
          <q-btn flat round icon="close" color="accent" v-close-popup size="sm" class="invoice-dialog__close" />
        </div>

        <q-separator dark class="invoice-dialog__separator" />

        <q-card-section class="invoice-dialog__body">
          <q-form @submit.prevent="save" class="q-gutter-y-md">
            <!-- Provider -->
            <q-select
              dark filled standout v-model="form.proveedorId"
              :options="providerFilteredOptions"
              label="Proveedor"
              :rules="[v => !!v || 'Requerido']"
              map-options emit-value
              use-input
              @filter="providerFilter"
              popup-content-class="product-dropdown"
            >
              <template v-slot:prepend>
                <q-icon name="business" size="1rem" class="text-primary" />
              </template>
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

            <!-- Date + Type row -->
            <div class="row q-col-gutter-md">
              <div class="col-xs-12 col-sm-6">
                <q-input dark filled standout v-model="form.fecha" label="Fecha" type="date" :rules="[v => !!v || 'Requerido']">
                  <template v-slot:prepend><q-icon name="calendar_today" size="1rem" class="text-primary" /></template>
                </q-input>
              </div>
              <div class="col-xs-12 col-sm-6">
                <q-select dark filled standout v-model="form.tipo" :options="['FACTURA', 'GASTO_OPERATIVO']" label="Tipo" :rules="[v => !!v || 'Requerido']">
                  <template v-slot:prepend><q-icon name="category" size="1rem" class="text-primary" /></template>
                </q-select>
              </div>
            </div>

            <!-- Payment method -->
            <q-select dark filled standout v-model="form.metodoPago" :options="['EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'CHEQUE']" label="Método de pago" clearable>
              <template v-slot:prepend><q-icon name="payment" size="1rem" class="text-primary" /></template>
            </q-select>

            <q-separator dark class="invoice-dialog__section-sep" />

            <!-- Items header -->
            <div class="items-header">
              <div class="items-header__title">
                <q-icon name="list" size="1rem" class="text-primary" />
                <span class="text-subtitle2 text-primary">Items</span>
                <q-badge v-if="form.items.length" :label="form.items.length" color="accent" text-color="dark" class="q-ml-xs items-count-badge" />
              </div>
              <div class="items-header__total" v-if="form.items.length">
                <span class="items-header__total-label">Total</span>
                <span class="items-header__total-value">{{ formatCurrency(computedTotal) }}</span>
              </div>
            </div>

            <CategoryTabs v-model="activeCategory" :categories="setupCategories" />

            <InvoiceItemCard
              v-for="(item, i) in form.items" :key="item._key"
              :item="item" :index="i"
              :product-options="filteredByCategory"
              :unit-options="unitOptions(item.productoId)"
              :presentation-conversion-map="presentationConversionMap"
              :base-unit-name="baseUnitNameFor(item)"
              @update:productoId="onProductoChange(item, $event)"
              @update:presentacionId="onPresentacionChange(item, $event)"
              @update:cantidad="item.cantidad = $event"
              @update:precioUnitario="item.precioUnitario = $event"
              @update:descuento="item.descuento = $event"
              @remove="removeItem(i)"
              class="invoice-item-enter"
            />

            <q-btn
              outline color="primary" icon="add" label="Agregar item"
              @click="addItem" class="add-item-btn"
              no-caps
            />

            <!-- Total bar -->
            <Transition name="total-slide">
              <div v-if="form.items.length" class="total-bar">
                <div class="total-bar__content">
                  <div class="total-bar__detail">
                    <span class="total-bar__label">Subtotal</span>
                    <span class="total-bar__value">{{ formatCurrency(form.items.reduce((s, i) => s + (i.cantidad || 0) * (i.precioUnitario || 0) * (1 - (i.descuento || 0) / 100), 0)) }}</span>
                  </div>
                  <q-icon name="arrow_right" size="1rem" class="text-accent" />
                  <div class="total-bar__detail total-bar__detail--total">
                    <span class="total-bar__label">Total</span>
                    <span class="total-bar__value total-bar__value--total">{{ formatCurrency(computedTotal) }}</span>
                  </div>
                </div>
              </div>
            </Transition>

            <q-separator dark class="invoice-dialog__footer-sep" />

            <!-- Actions -->
            <div class="dialog-actions">
              <q-btn flat label="Cancelar" color="accent" v-close-popup no-caps class="dialog-actions__cancel" />
              <q-btn
                type="submit" label="Guardar Factura" color="primary"
                :loading="saving" no-caps
                icon="save"
                class="dialog-actions__save"
              />
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
import type { Factura, FacturaRequest, SetupInfo, SetupCategory, ProductOption, Producto } from '../types'
import { api } from 'src/boot/axios'
import CategoryTabs from '../components/facturas/CategoryTabs.vue'
import InvoiceItemCard from '../components/facturas/InvoiceItemCard.vue'
import InvoiceDetailDialog from '../components/facturas/InvoiceDetailDialog.vue'
import ConfirmDialog from '../components/facturas/ConfirmDialog.vue'
import EmptyState from 'src/components/ui/EmptyState.vue'

useMeta({ title: 'Facturas — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

interface OptionItem { label: string; value: string; __isCreate?: boolean }

const rows = ref<Factura[]>([])
const loading = shallowRef(false)
const filter = shallowRef('')
const pagination = shallowRef({ sortBy: 'issueDate', descending: true, page: 1, rowsPerPage: 15 })

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
  precioUnitario: number | null
  descuento: number
}

const dialogOpen = shallowRef(false)
const saving = shallowRef(false)
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

function onProductoChange(item: ItemForm, productoId: string | null) {
  item.productoId = productoId
  item.presentacionId = null
  const prod = allProducts.value.find(p => p.value === productoId)
  item.precioUnitario = prod?.lastUnitPrice ?? null
}

function onPresentacionChange(item: ItemForm, presId: string | null) {
  item.presentacionId = presId
  if (!item.productoId) return
  const prod = allProducts.value.find(p => p.value === item.productoId)
  if (!prod) return
  if (presId) {
    const conv = presentationConversionMap.value.get(presId) || 1
    item.precioUnitario = conv > 1 && prod.lastUnitPrice != null ? prod.lastUnitPrice / conv : prod.lastUnitPrice ?? null
  } else {
    item.precioUnitario = prod.lastUnitPrice ?? null
  }
}

function baseUnitNameFor(item: ItemForm): string {
  if (!item.productoId) return ''
  const prods = prodsData.value.find(p => p.id === item.productoId)
  return prods ? (unitNameMap.value.get(prods.baseUnit) || prods.baseUnit) : ''
}

function removeItem(i: number) {
  form.value.items.splice(i, 1)
}

const computedTotal = computed(() => {
  const itemsTotal = form.value.items.reduce((sum, item) => {
    const qty = item.cantidad || 0
    const price = item.precioUnitario || 0
    const disc = item.descuento || 0
    return sum + (qty * price * (1 - disc / 100))
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
  proveedorService.create({ tenantId, name }).then(res => {
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
  try {
    const [provs, setupRes, prodsRes] = await Promise.all([
      proveedorService.getAll(tenantId),
      api.get<SetupInfo>(`/core/setup/${tenantId}`),
      productoService.search(tenantId, { page: 0, size: 100 }),
    ])
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
        descuento: (item.cantidad || 0) * (item.precioUnitario || 0) * ((item.descuento || 0) / 100),
      })),
    }
    const res = await facturaService.create(payload)
    rows.value.unshift(res.data)
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: 'Factura creada: ' + res.data.invoiceNumber })
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Error al crear factura'
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

const deleteDialog = shallowRef(false)
const deletingItem = shallowRef<Factura | null>(null)
const deleting = shallowRef(false)

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

function onDialogBeforeHide(done?: () => void) {
  const hasData = form.value.items.length > 0 || form.value.proveedorId
  if (!hasData) { done?.(); return }
  $q.dialog({
    title: 'Cambios sin guardar',
    message: 'Tienes datos sin guardar. ¿Cerrar de todas formas?',
    cancel: { label: 'Cancelar', flat: true, color: 'accent' },
    ok: { label: 'Cerrar', color: 'negative' },
    persistent: true,
  }).onOk(() => done?.())
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
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
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
/* ─── Invoice Dialog ─── */
.invoice-dialog {
  max-width: 720px;
  border-radius: 16px;
  overflow: hidden;
}

.invoice-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 10px;
}

.invoice-dialog__header-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.invoice-dialog__title {
  display: flex;
  align-items: center;
}

.invoice-dialog__subtitle {
  font-size: 0.78rem;
  color: rgba(163, 120, 94, 0.5);
  padding-left: 2rem;
}

.invoice-dialog__close {
  opacity: 0.5;
  transition: opacity 0.15s ease;
}

.invoice-dialog__close:hover {
  opacity: 1;
}

.invoice-dialog__separator {
  opacity: 0.3;
}

.invoice-dialog__body {
  padding: 12px 16px 16px;
}

.invoice-dialog__section-sep {
  opacity: 0.15;
  margin: 4px 0;
}

.invoice-dialog__footer-sep {
  opacity: 0.15;
  margin: 4px 0 0;
}

/* ─── Items header ─── */
.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}

.items-header__title {
  display: flex;
  align-items: center;
  gap: 4px;
}

.items-count-badge {
  font-size: 0.65rem;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
}

.items-header__total {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.items-header__total-label {
  font-size: 0.7rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgba(163, 120, 94, 0.45);
}

.items-header__total-value {
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 1rem;
  font-weight: 700;
  color: rgba(212, 175, 55, 0.9);
  font-variant-numeric: tabular-nums;
}

/* ─── Add item button ─── */
.add-item-btn {
  width: 100%;
  border-style: dashed;
  border-width: 1.5px;
  opacity: 0.6;
  transition: opacity 0.2s ease, border-color 0.2s ease;
}

.add-item-btn:hover {
  opacity: 1;
  border-color: rgba(163, 120, 94, 0.4);
}

/* ─── Total bar ─── */
.total-bar {
  background:
    linear-gradient(135deg, rgba(163, 120, 94, 0.06) 0%, rgba(212, 175, 55, 0.04) 100%);
  border: 1px solid rgba(163, 120, 94, 0.12);
  border-radius: 10px;
  padding: 10px 14px;
}

.total-bar__content {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.total-bar__detail {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
}

.total-bar__label {
  font-size: 0.68rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgba(163, 120, 94, 0.45);
}

.total-bar__value {
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 0.85rem;
  font-weight: 600;
  color: rgba(163, 120, 94, 0.7);
  font-variant-numeric: tabular-nums;
}

.total-bar__detail--total {
  align-items: flex-end;
}

.total-bar__value--total {
  font-size: 1.05rem;
  font-weight: 800;
  color: rgba(212, 175, 55, 0.95);
}

/* ─── Total transition ─── */
.total-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.total-slide-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.total-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.total-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ─── Dialog actions ─── */
.dialog-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

.dialog-actions__cancel {
  opacity: 0.7;
  transition: opacity 0.15s ease;
}

.dialog-actions__cancel:hover {
  opacity: 1;
}

.dialog-actions__save {
  font-weight: 600;
  letter-spacing: 0.02em;
  border-radius: 8px;
  padding: 6px 20px;
}

/* ─── Invoice item enter animation ─── */
.invoice-item-enter {
  animation: itemAppear 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes itemAppear {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ─── Dropdown styling ─── */
.product-dropdown {
  font-size: 0.85rem;
}
</style>
