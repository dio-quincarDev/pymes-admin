<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted, watch } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { api } from 'src/boot/axios'
import { productoService } from '../services/producto.service'
import { proveedorService } from '../services/proveedor.service'
import type { Producto, ProductoRequest, Presentacion, PresentacionRequest, SetupInfo, SetupCategory } from '../types'
import EmptyState from 'src/components/ui/EmptyState.vue'

useMeta({ title: 'Productos — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const rows = ref<Producto[]>([])
const loading = shallowRef(false)
const filter = shallowRef('')
const pagination = shallowRef({ sortBy: 'name', descending: false, page: 0, rowsPerPage: 15, rowsNumber: 0 })
const categoryFilter = ref('')

const catOptions = ref<{ label: string; value: string }[]>([])
const setupCategories = ref<SetupCategory[]>([])
const unitOptions = ref<{ label: string; value: string }[]>([])
const providerOptions = ref<{ label: string; value: string }[]>([])

const columns = [
  { name: 'name', label: 'Nombre', field: 'name', align: 'left' as const, sortable: true },
  { name: 'sku', label: 'SKU', field: 'sku', align: 'left' as const, sortable: true },
  { name: 'proveedorName', label: 'Proveedor', field: 'proveedorName', align: 'left' as const, sortable: false },
  { name: 'category', label: 'Categoría', field: 'category', align: 'left' as const, sortable: false },
  { name: 'baseUnit', label: 'Unidad', field: 'baseUnit', align: 'left' as const, sortable: false },
  { name: 'presentaciones', label: 'Presentaciones', field: 'id', align: 'left' as const, sortable: false },
  { name: 'actions', label: 'Acciones', field: 'id', align: 'right' as const, sortable: false },
]

function flattenCategories(cats: SetupCategory[], prefix = ''): { label: string; value: string }[] {
  const result: { label: string; value: string }[] = []
  for (const c of cats) {
    const label = prefix ? `${prefix} › ${c.name}` : c.name
    result.push({ label, value: c.code })
    if (c.children?.length) result.push(...flattenCategories(c.children, label))
  }
  return result
}

const categoryNameMap = computed(() => {
  const map = new Map<string, string>()
  function walk(cats: SetupCategory[]) {
    for (const c of cats) {
      map.set(c.code, c.name)
      if (c.children?.length) walk(c.children)
    }
  }
  walk(setupCategories.value)
  return map
})

const unitNameMap = computed(() => {
  const map = new Map<string, string>()
  for (const o of unitOptions.value) map.set(o.value, o.label)
  return map
})

async function loadSetup() {
  if (!tenantId) return
  try {
    const [setupRes, provRes] = await Promise.all([
      api.get<SetupInfo>(`/core/setup/${tenantId}`),
      proveedorService.getAll(tenantId),
    ])
    setupCategories.value = setupRes.data.categories || []
    catOptions.value = flattenCategories(setupCategories.value)
    unitOptions.value = (setupRes.data.units || []).map(u => ({ label: u.name, value: u.code }))
    providerOptions.value = provRes.data.map(p => ({ label: p.name, value: p.id }))
  } catch { /* template data non-critical */ }
}

async function load(page = 0, size = 15) {
  loading.value = true
  try {
    const params: { category?: string; page: number; size: number } = { page, size }
    if (categoryFilter.value) params.category = categoryFilter.value
    const res = await productoService.search(tenantId, params)
    rows.value = res.data.content
    pagination.value.rowsNumber = res.data.totalElements
    pagination.value.page = page
    pagination.value.rowsPerPage = size
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar productos' })
  } finally { loading.value = false }
}

function onRequest(props: { pagination: { page: number; rowsPerPage: number; sortBy: string; descending: boolean } }) {
  void load(props.pagination.page, props.pagination.rowsPerPage)
}

watch(categoryFilter, () => { void load(0, pagination.value.rowsPerPage) })

const dialogOpen = shallowRef(false)
const editingId = shallowRef<string | null>(null)
const saving = shallowRef(false)
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null)
const form = ref<ProductoRequest>({ tenantId, name: '', category: '', baseUnit: '', proveedorId: null })

function openCreate() {
  editingId.value = null
  form.value = { tenantId, name: '', category: '', baseUnit: '', proveedorId: null }
  dialogOpen.value = true
}

function openEdit(p: Producto) {
  editingId.value = p.id
  form.value = { tenantId: p.tenantId, name: p.name, sku: p.sku, category: p.category, baseUnit: p.baseUnit, proveedorId: p.proveedorId }
  dialogOpen.value = true
}

onMounted(async () => {
  await loadSetup()
  await load()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => window.removeEventListener('keydown', handleKeydown))

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    openCreate()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 's' && dialogOpen.value) {
    e.preventDefault()
    void save()
  }
}

async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    if (editingId.value) {
      const res = await productoService.update(editingId.value, form.value)
      const idx = rows.value.findIndex(r => r.id === editingId.value)
      if (idx >= 0) rows.value[idx] = res.data
    } else {
      const res = await productoService.create(form.value)
      rows.value.unshift(res.data)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: `Producto ${editingId.value ? 'actualizado' : 'creado'}` })
  } catch { $q.notify({ type: 'negative', message: 'Error al guardar producto' })
  } finally { saving.value = false }
}

const presDialog = shallowRef(false)
const presProduct = ref<Producto | null>(null)
const presItems = ref<Presentacion[]>([])
const presForm = ref<PresentacionRequest>({ name: '', conversion: 1 })
const baseUnitLabel = computed(() => {
  if (!presProduct.value) return 'unidades'
  const opt = unitOptions.value.find(o => o.value === presProduct.value!.baseUnit)
  return opt?.label || 'unidades'
})
const conversionPreview = computed(() => {
  const name = presForm.value.name || '—'
  const conv = presForm.value.conversion
  const unit = baseUnitLabel.value
  return conv > 1 ? `1 ${name} = ${conv} ${unit}` : ''
})
const addingPres = shallowRef(false)
const removingPres = shallowRef(false)

function openPresentations(p: Producto) {
  presProduct.value = p
  presItems.value = [...(p.presentaciones || [])]
  presForm.value = { name: '', conversion: 1 }
  presDialog.value = true
}

async function addPresentation() {
  if (!presForm.value.name || !presProduct.value) return
  addingPres.value = true
  try {
    const res = await productoService.addPresentation(presProduct.value.id, presForm.value, tenantId)
    presItems.value.push(res.data)
    const prod = rows.value.find(r => r.id === presProduct.value!.id)
    if (prod) prod.presentaciones = [...presItems.value]
    presForm.value = { name: '', conversion: 1 }
    $q.notify({ type: 'positive', message: 'Presentación agregada' })
  } catch { $q.notify({ type: 'negative', message: 'Error al agregar presentación' })
  } finally { addingPres.value = false }
}

async function removePresentation(p: Presentacion) {
  removingPres.value = true
  try {
    await productoService.removePresentation(p.id, tenantId)
    presItems.value = presItems.value.filter(x => x.id !== p.id)
    const prod = rows.value.find(r => r.id === presProduct.value!.id)
    if (prod) prod.presentaciones = [...presItems.value]
  } catch { $q.notify({ type: 'negative', message: 'Error al eliminar presentación' })
  } finally { removingPres.value = false }
}

const deleteDialog = shallowRef(false)
const deletingItem = ref<Producto | null>(null)
const deleting = shallowRef(false)

function confirmDelete(p: Producto) {
  deletingItem.value = p
  deleteDialog.value = true
}

async function remove() {
  if (!deletingItem.value) return
  deleting.value = true
  try {
    await productoService.remove(deletingItem.value.id, tenantId)
    rows.value = rows.value.filter(r => r.id !== deletingItem.value!.id)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Producto eliminado' })
  } catch { $q.notify({ type: 'negative', message: 'Error al eliminar producto' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
}

</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-md fade-in-up">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Productos</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Catálogo de productos y presentaciones</p>
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
        @request="onRequest"
      >
        <template v-slot:top>
          <q-select dark dense filled v-model="categoryFilter" :options="catOptions" label="Categoría" clearable emit-value map-options class="q-mr-sm" style="min-width: 180px" />
          <q-input dark dense filled v-model="filter" placeholder="Buscar..." class="q-mr-sm" style="max-width: 250px">
            <template v-slot:prepend><q-icon name="search" /></template>
          </q-input>
          <q-space />
          <q-btn color="primary" icon="add" label="Nuevo" @click="openCreate" />
        </template>

        <template v-slot:body-cell-proveedorName="{ row }">
          <td><span class="text-accent">{{ row.proveedorName || '—' }}</span></td>
        </template>
        <template v-slot:body-cell-category="{ row }">
          <td>{{ categoryNameMap.get(row.category) || row.category }}</td>
        </template>

        <template v-slot:body-cell-baseUnit="{ row }">
          <td>{{ unitNameMap.get(row.baseUnit) || row.baseUnit }}</td>
        </template>

        <template v-slot:body-cell-presentaciones="{ row }">
          <td>
            <q-chip v-if="row.presentaciones?.length" dense dark color="accent" text-color="dark">
              {{ row.presentaciones.length }} {{ row.presentaciones.length === 1 ? 'presentación' : 'presentaciones' }}
            </q-chip>
            <span v-else class="text-accent text-caption">—</span>
          </td>
        </template>

        <template v-slot:body-cell-actions="{ row }">
          <td class="text-right">
            <q-btn flat dense round icon="layers" color="info" @click="openPresentations(row)" aria-label="Presentaciones">
              <q-tooltip>Presentaciones</q-tooltip>
            </q-btn>
            <q-btn flat dense round icon="edit" color="primary" @click="openEdit(row)" aria-label="Editar producto" />
            <q-btn flat dense round icon="delete" color="negative" @click="confirmDelete(row)" aria-label="Eliminar producto" />
          </td>
        </template>
        <template v-slot:no-data>
          <EmptyState
            v-if="!loading"
            icon="inventory_2"
            title="Sin productos"
            message="Agrega tu primer producto al catálogo para comenzar a facturar."
          >
            <q-btn color="primary" icon="add" label="Nuevo Producto" @click="openCreate" class="q-mt-sm" />
          </EmptyState>
        </template>
      </q-table>
    </q-card>

    <!-- Dialog: Create/Edit Product -->
    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section>
          <div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nuevo' }} Producto</div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <q-input dark filled v-model="form.name" label="Nombre" :rules="[v => !!v || 'Requerido']" />
            <q-select dark filled v-model="form.category" label="Categoría" :options="catOptions" option-value="value" option-label="label" emit-value map-options use-input input-debounce="0" @filter="(val, update) => { update(() => catOptions.filter((o: { label: string; value: string }) => !val || o.label.toLowerCase().includes(val.toLowerCase()))) }" :rules="[v => !!v || 'Requerido']" />
            <q-select dark filled v-model="form.baseUnit" label="Unidad base" :options="unitOptions" option-value="value" option-label="label" emit-value map-options use-input input-debounce="0" @filter="(val, update) => { update(() => unitOptions.filter((o: { label: string; value: string }) => !val || o.label.toLowerCase().includes(val.toLowerCase()))) }" />
            <q-select dark filled v-model="form.proveedorId" label="Proveedor" :options="providerOptions" option-value="value" option-label="label" emit-value map-options clearable />
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Dialog: Manage Presentations -->
    <q-dialog v-model="presDialog" dark>
      <q-card dark class="bg-surface-pine pres-dialog">
        <div class="pres-dialog__header">
          <q-icon name="inventory_2" size="1.3rem" class="text-primary" />
          <div class="pres-dialog__header-text">
            <div class="text-h6 text-primary q-ma-none">Presentaciones</div>
            <div class="pres-dialog__product-name">{{ presProduct?.name }}</div>
          </div>
        </div>
        <q-separator dark class="pres-dialog__sep" />

        <q-card-section class="pres-dialog__list">
          <TransitionGroup name="pres-list" tag="div" class="pres-list">
            <div v-if="!presItems.length" key="empty" class="pres-empty">
              <q-icon name="inventory" size="2rem" class="pres-empty__icon" />
              <span>Sin presentaciones</span>
            </div>
            <div v-for="p in presItems" :key="p.id" class="pres-row">
              <div class="pres-row__info">
                <span class="pres-row__name">{{ p.name }}</span>
                <span class="pres-row__conv">
                  <q-icon name="close" size="0.7rem" />
                  {{ p.conversion }}
                </span>
              </div>
              <q-btn
                flat dense round icon="delete_outline" color="negative" size="sm"
                @click="removePresentation(p)" :disable="removingPres"
                aria-label="Eliminar presentación"
                class="pres-row__remove"
              />
            </div>
          </TransitionGroup>
        </q-card-section>

        <q-separator dark class="pres-dialog__sep" />

        <q-card-section class="pres-dialog__form">
          <div class="pres-form-title">Agregar presentación</div>
          <div class="row q-col-gutter-sm items-start">
            <div class="col-xs-12 col-sm-5">
              <q-input dark dense outlined v-model="presForm.name" label="Nombre" placeholder="Ej: Caja x24" class="pres-input" />
            </div>
            <div class="col-xs-12 col-sm-4">
              <q-input dark dense outlined v-model.number="presForm.conversion" label="Conversión" type="text" inputmode="numeric" class="pres-input" />
              <div class="pres-hint">
                ¿Cuántas unidades base caben aquí?
              </div>
            </div>
            <div class="col-xs-12 col-sm-3">
              <q-btn
                label="Agregar" color="primary" no-caps
                :loading="addingPres" @click="addPresentation"
                icon="add"
                class="pres-add-btn"
              />
            </div>
          </div>
          <Transition name="preview-fade">
            <div v-if="conversionPreview" class="pres-preview">
              <q-icon name="swap_vert" size="0.9rem" />
              <span>{{ conversionPreview }}</span>
            </div>
          </Transition>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Dialog: Delete Confirmation -->
    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span>¿Eliminar producto <strong>{{ deletingItem?.name }}</strong>?</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Eliminar" color="negative" :loading="deleting" @click="remove" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<style scoped>
/* ─── Presentation Dialog ─── */
.pres-dialog {
  width: 90vw;
  max-width: 480px;
  border-radius: 14px;
  overflow: hidden;
}

.pres-dialog__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px 12px;
}

.pres-dialog__header-text {
  display: flex;
  flex-direction: column;
}

.pres-dialog__product-name {
  font-size: 0.78rem;
  color: rgba(163, 120, 94, 0.5);
}

.pres-dialog__sep {
  opacity: 0.2;
}

.pres-dialog__list {
  padding: 12px 20px;
}

.pres-dialog__form {
  padding: 12px 20px 16px;
}

.pres-form-title {
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgba(163, 120, 94, 0.6);
  margin-bottom: 10px;
}

/* ─── Presentation list ─── */
.pres-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pres-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 0;
  color: rgba(163, 120, 94, 0.35);
  font-size: 0.8rem;
}

.pres-empty__icon {
  opacity: 0.4;
}

.pres-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(27, 38, 36, 0.5);
  border: 1px solid rgba(113, 131, 127, 0.08);
  border-radius: 8px;
  transition: border-color 0.15s ease;
}

.pres-row:hover {
  border-color: rgba(163, 120, 94, 0.15);
}

.pres-row__info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pres-row__name {
  font-size: 0.85rem;
  font-weight: 500;
}

.pres-row__conv {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.72rem;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  color: rgba(163, 120, 94, 0.5);
  background: rgba(163, 120, 94, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
}

.pres-row__remove {
  opacity: 0.3;
  transition: opacity 0.15s ease;
}

.pres-row__remove:hover {
  opacity: 1;
}

/* ─── List transitions ─── */
.pres-list-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.pres-list-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.pres-list-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.pres-list-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* ─── Conversion preview ─── */
.pres-preview {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  padding: 5px 10px;
  font-size: 0.75rem;
  font-weight: 500;
  color: rgba(34, 211, 238, 0.8);
  background: rgba(34, 211, 238, 0.08);
  border: 1px solid rgba(34, 211, 238, 0.15);
  border-radius: 6px;
}

.pres-hint {
  font-size: 0.7rem;
  color: rgba(163, 120, 94, 0.45);
  line-height: 1.4;
  margin-top: 4px;
}

.pres-input :deep(.q-field__control) {
  border-radius: 8px !important;
}

.pres-add-btn {
  border-radius: 8px;
  font-weight: 600;
}

/* ─── Preview transition ─── */
.preview-fade-enter-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.preview-fade-leave-active {
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.preview-fade-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}

.preview-fade-leave-to {
  opacity: 0;
}
</style>
