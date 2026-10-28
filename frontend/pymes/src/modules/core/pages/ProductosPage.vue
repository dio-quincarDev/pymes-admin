<script setup lang="ts">
import { ref, shallowRef, computed, watch, onMounted, onUnmounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { useTutorial } from 'src/composables/useTutorial'
import { api } from 'src/boot/axios'
import { productoService } from '../services/producto.service'
import { proveedorService } from '../services/proveedor.service'
import type { Producto, ProductoRequest, SetupInfo, SetupCategory } from '../types'
import { isUuidLike } from 'src/utils/format'
import PresentacionesDialog from '../components/PresentacionesDialog.vue'
import EmptyState from 'src/components/ui/EmptyState.vue'

useMeta({ title: 'Productos — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId
const { showForCurrentRoute } = useTutorial()

const PAGE_SIZE = 12
const rows = ref<Producto[]>([])
const loading = shallowRef(false)
const loadError = shallowRef(false)
const search = shallowRef('')
const page = shallowRef(1)
const totalElements = shallowRef(0)
const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / PAGE_SIZE)))
const categoryFilter = shallowRef('')

const catOptions = shallowRef<{ label: string; value: string }[]>([])
const setupCategories = shallowRef<SetupCategory[]>([])
const unitOptions = shallowRef<{ label: string; value: string }[]>([])
const allUnits = shallowRef<{ label: string; value: string }[]>([])
const providerOptions = shallowRef<{ label: string; value: string }[]>([])
const setupReady = shallowRef(false)

function flattenCategories(cats: SetupCategory[], prefix = ''): { label: string; value: string }[] {
  const result: { label: string; value: string }[] = []
  for (const c of cats) {
    const label = prefix ? `${prefix} \u203A ${c.name}` : c.name
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
  for (const o of allUnits.value) map.set(o.value, o.label)
  return map
})

function baseUnitLabel(code: string): string {
  const named = unitNameMap.value.get(code)
  if (named) return named
  return isUuidLike(code) ? 'Unidad' : code
}

const totalCategories = computed(() => {
  const seen = new Set(rows.value.map(p => p.category))
  return seen.size
})

const emptyMessage = computed(() =>
  search.value || categoryFilter.value
    ? 'Sin resultados para tu búsqueda o filtro.'
    : 'Agrega tu primer producto al catálogo para comenzar a facturar.',
)

const needsOnboarding = computed(() => setupReady.value && !unitOptions.value.length)

async function loadSetup() {
  if (!tenantId) return
  try {
    const [setupRes, provRes] = await Promise.all([
      api.get<SetupInfo>(`/core/setup/${tenantId}`),
      proveedorService.getAll(tenantId),
    ])
    setupCategories.value = setupRes.data.categories || []
    catOptions.value = flattenCategories(setupCategories.value)
    const seenUnits = new Set<string>()
    const rawUnits = (setupRes.data.units || [])
      .map(u => ({ label: u.name, value: u.code }))
    allUnits.value = rawUnits
    unitOptions.value = rawUnits.filter(o => {
        const k = o.label.toLowerCase()
        if (seenUnits.has(k)) return false
        seenUnits.add(k)
        return true
      })
    providerOptions.value = provRes.data.map(p => ({ label: p.name, value: p.id }))
  } catch { /* non-critical */ } finally {
    setupReady.value = true
  }
}

async function load(p = 1) {
  if (!tenantId) return
  loading.value = true
  loadError.value = false
  try {
    const params: { category?: string; search?: string; page: number; size: number; sort: string } = { page: p - 1, size: PAGE_SIZE, sort: 'name,asc' }
    if (categoryFilter.value) params.category = categoryFilter.value
    if (search.value.trim()) params.search = search.value.trim()
    const res = await productoService.search(tenantId, params)
    rows.value = res.data.content
    totalElements.value = res.data.totalElements
    page.value = p
  } catch (err) {
    loadError.value = true
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar productos' })
  } finally {
    loading.value = false
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(search, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => void load(1), 300)
})
watch(categoryFilter, () => void load(1))

const dialogOpen = shallowRef(false)
const editingId = shallowRef<string | null>(null)
const saving = shallowRef(false)
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null)
const form = ref<ProductoRequest>({ tenantId: tenantId as string, name: '', category: '', baseUnit: '', proveedorId: null })

const formUnitOptions = computed(() => {
  const cur = form.value.baseUnit
  if (!cur || unitOptions.value.some(o => o.value === cur)) return unitOptions.value
  const legacy = allUnits.value.find(o => o.value === cur)
  return [...unitOptions.value, { label: legacy?.label || 'Unidad anterior', value: cur }]
})

const presDialog = shallowRef(false)
const presProduct = ref<Producto | null>(null)
const unitLabel = computed(() => {
  if (!presProduct.value) return 'unidades'
  const opt = allUnits.value.find(o => o.value === presProduct.value!.baseUnit)
  return opt?.label || 'unidades'
})

function openCreate() {
  editingId.value = null
  form.value = { tenantId: tenantId as string, name: '', category: '', baseUnit: '', proveedorId: null }
  dialogOpen.value = true
}

function openEdit(p: Producto) {
  editingId.value = p.id
  form.value = {
    tenantId: p.tenantId, name: p.name, sku: p.sku,
    category: p.category, baseUnit: p.baseUnit, proveedorId: p.proveedorId,
  }
  dialogOpen.value = true
}

function openPresentaciones(p: Producto) {
  presProduct.value = p
  presDialog.value = true
}

async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    if (editingId.value) {
      await productoService.update(editingId.value, form.value)
    } else {
      await productoService.create(form.value)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: `Producto ${editingId.value ? 'actualizado' : 'creado'}` })
    await load(page.value)
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al guardar producto' })
  } finally {
    saving.value = false
  }
}

const deleteDialog = shallowRef(false)
const deletingItem = ref<Producto | null>(null)
const deleting = shallowRef(false)

function confirmDelete(p: Producto) {
  deletingItem.value = p
  deleteDialog.value = true
}

async function remove() {
  if (!deletingItem.value || !tenantId) return
  deleting.value = true
  try {
    await productoService.remove(deletingItem.value.id, tenantId)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Producto eliminado' })
    const isLastOnPage = rows.value.length === 1 && page.value > 1
    await load(isLastOnPage ? page.value - 1 : page.value)
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al eliminar producto' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
}

onMounted(async () => {
  await loadSetup()
  await load(1)
  void showForCurrentRoute()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (searchTimer) clearTimeout(searchTimer)
  window.removeEventListener('keydown', handleKeydown)
})

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
</script>

<template>
  <q-page class="core-page productos-page">
    <!-- Header con firma cobre -->
    <div class="page-header" data-tour="productos">
      <div class="page-header__main">
        <h1 class="page-title">Productos</h1>
        <p class="page-subtitle">Catálogo de productos y presentaciones</p>
      </div>
      <div class="page-header__signature" aria-hidden="true">
        <span class="signature-dot" />
        <span class="signature-line" />
      </div>
    </div>

    <!-- Stats row -->
    <div class="stats-row">
      <span class="stat">{{ totalElements }} {{ totalElements === 1 ? 'producto' : 'productos' }}</span>
      <span class="stat-sep" aria-hidden="true">·</span>
      <span class="stat">{{ totalCategories }} {{ totalCategories === 1 ? 'categoría' : 'categorías' }}</span>
    </div>
    <div class="rule" />

    <!-- Toolbar -->
    <div class="productos-toolbar">
      <q-select
        dark dense filled
        v-model="categoryFilter"
        :options="catOptions"
        label="Categoría"
        clearable emit-value map-options
        class="toolbar-field"
      />
      <q-input
        dark dense filled v-model="search"
        label="Buscar"
        class="toolbar-field toolbar-search"
        clearable
      >
        <template v-slot:prepend><q-icon name="search" /></template>
      </q-input>
      <q-space class="toolbar-spacer" />
      <q-btn
        color="primary" label="Nuevo" no-caps
        @click="openCreate" class="toolbar-new"
      />
    </div>

    <!-- Empty -->
    <div v-if="!loading && !rows.length">
      <EmptyState
        icon="sym_r_inventory_2"
        title="Sin productos"
        :message="emptyMessage"
      >
        <q-btn color="primary" label="Nuevo Producto" no-caps @click="openCreate" class="q-mt-sm" />
        <q-btn v-if="search || categoryFilter" flat dense no-caps label="Limpiar filtros" color="accent" class="q-mt-sm q-ml-sm" @click="search = ''; categoryFilter = ''" />
      </EmptyState>
    </div>

    <!-- Error loading -->
    <div v-if="!loading && loadError" class="error-banner">
      <q-icon name="error" size="1.25rem" class="q-mr-sm" />
      <span>No se pudo cargar el catálogo.</span>
      <q-btn flat dense no-caps label="Reintentar" color="accent" class="q-ml-sm" @click="load(page)" />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="card-grid">
      <div v-for="n in 6" :key="n" class="skeleton-card">
        <div class="skeleton-card__body">
          <q-skeleton type="text" dark animation="pulse" width="60%" height="18px" class="q-mb-xs" />
          <q-skeleton type="text" dark animation="pulse" width="30%" height="12px" class="q-mb-sm" />
          <div class="skeleton-card__badges">
            <q-skeleton type="rect" dark animation="pulse" width="72px" height="18px" />
            <q-skeleton type="rect" dark animation="pulse" width="60px" height="18px" />
          </div>
          <q-skeleton type="text" dark animation="pulse" width="45%" height="12px" class="q-mt-sm" />
        </div>
      </div>
    </div>

    <!-- Data -->
    <div v-if="!loading && rows.length" class="card-grid stagger-children">
      <q-card
        v-for="item in rows" :key="item.id"
        dark class="product-card hover-lift"
        tabindex="0"
      >
        <div class="product-card__accent" aria-hidden="true" />
        <q-card-section class="product-card__body">
          <div class="product-card__name">{{ item.name }}</div>
          <div v-if="item.sku" class="product-card__sku">{{ item.sku }}</div>
          <div class="product-card__badges">
            <q-chip v-if="item.category" dense dark size="sm" color="primary" text-color="dark" class="badge badge--filled">
              {{ categoryNameMap.get(item.category) || item.category }}
            </q-chip>
            <q-chip v-if="item.baseUnit" dense dark size="sm" outline color="primary" class="badge badge--outlined">
              {{ baseUnitLabel(item.baseUnit) }}
            </q-chip>
          </div>
          <div v-if="item.proveedorName" class="product-card__meta">
            <q-icon name="store" size="0.85rem" class="q-mr-xs" />
            {{ item.proveedorName }}
          </div>
          <div v-if="item.presentaciones?.length" class="product-card__meta">
            <q-icon name="layers" size="0.85rem" class="q-mr-xs" />
            {{ item.presentaciones.length }} {{ item.presentaciones.length === 1 ? 'presentación' : 'presentaciones' }}
          </div>
        </q-card-section>
        <q-separator dark class="product-card__sep" />
        <q-card-actions align="right" class="product-card__actions">
          <q-btn flat dense round icon="sym_r_layers" color="info" size="sm" @click="openPresentaciones(item)" aria-label="Presentaciones" />
          <q-btn flat dense round icon="sym_r_edit" color="primary" size="sm" @click="openEdit(item)" aria-label="Editar" />
          <q-btn flat dense round icon="sym_r_delete" color="negative" size="sm" @click="confirmDelete(item)" aria-label="Eliminar" />
        </q-card-actions>
      </q-card>
    </div>

    <!-- Pagination -->
    <div class="q-mt-md flex justify-center" v-if="totalPages > 1">
      <q-pagination
        v-model="page"
        :max="totalPages"
        :max-pages="5"
        boundary-numbers
        direction-links
        color="primary"
        text-color="accent"
        active-color="primary"
        @update:model-value="load"
      />
    </div>

    <!-- Dialog create/edit -->
    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="dialog-surface" style="width: 90vw; max-width: 480px">
        <q-card-section>
          <div class="dialog-title">{{ editingId ? 'Editar' : 'Nuevo' }} Producto</div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <q-banner v-if="needsOnboarding" dense class="bg-warning text-dark">
              Completa tu configuración primero para tener unidades.
              <template v-slot:action><q-btn flat dense label="Ir" to="/onboarding" /></template>
            </q-banner>
            <q-input dark outlined v-model="form.name" label="Nombre" :rules="[v => !!v || 'Requerido']" />
            <q-select dark outlined v-model="form.category" label="Categoría" :options="catOptions" option-value="value" option-label="label" emit-value map-options use-input input-debounce="0" @filter="(val, update) => { update(() => catOptions.filter(o => !val || o.label.toLowerCase().includes(val.toLowerCase()))) }" :rules="[v => !!v || 'Requerido']" />
            <q-select dark outlined v-model="form.baseUnit" label="Unidad base" :options="formUnitOptions" option-value="value" option-label="label" emit-value map-options use-input input-debounce="0" @filter="(val, update) => { update(() => formUnitOptions.filter(o => !val || o.label.toLowerCase().includes(val.toLowerCase()))) }" :rules="[v => !!v || 'Elige la unidad base del catálogo']" />
            <q-select dark outlined v-model="form.proveedorId" label="Proveedor" :options="providerOptions" option-value="value" option-label="label" emit-value map-options clearable />
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" :disable="needsOnboarding" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <PresentacionesDialog
      v-model="presDialog"
      :product="presProduct"
      :unit-label="unitLabel"
      @updated="load(page)"
    />

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="dialog-surface">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span>Eliminar producto <strong>{{ deletingItem?.name }}</strong>?</span>
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
.productos-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 64px);
}

/* Header con firma cobre */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 8px;
}

.page-title {
  font-family: var(--pq-font-display);
  font-weight: 700;
  font-size: clamp(22px, 4vw, 28px);
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--pq-text);
  margin: 0;
}

.page-subtitle {
  font-family: var(--pq-font-body);
  font-weight: 500;
  font-size: 13px;
  line-height: 1.5;
  color: var(--pq-text-muted);
  margin: 4px 0 0;
}

.page-header__signature {
  display: none;
  align-items: center;
  gap: 8px;
}

@media (min-width: 1024px) {
  .page-header__signature {
    display: flex;
  }
}

.signature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--pq-accent);
  box-shadow: 0 0 12px rgba(163, 120, 94, 0.4);
}

.signature-line {
  width: 48px;
  height: 2px;
  background: linear-gradient(90deg, var(--pq-accent), transparent);
}

.stats-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 8px;
}

.stat {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
  text-transform: uppercase;
}

.stat-sep {
  color: var(--pq-text-muted);
}

.rule {
  height: 1px;
  background: var(--pq-border);
  margin: 24px 0;
  opacity: 0.4;
}

.productos-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.toolbar-field {
  min-width: 180px;
}

.toolbar-search {
  flex: 1 1 220px;
  min-width: 200px;
}

.toolbar-spacer {
  display: none;
}

@media (min-width: 640px) {
  .toolbar-spacer {
    display: block;
    flex: 1;
  }
}

/* Nuevo button — filled cobre con shadow + lift */
.toolbar-new {
  min-height: 36px;
  background: var(--pq-accent) !important;
  color: var(--pq-background) !important;
  font-weight: 600;
  border-radius: var(--pq-radius-sm);
  box-shadow: 0 2px 8px rgba(163, 120, 94, 0.25);
  transition: background-color 0.16s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.16s ease, transform 0.1s ease;
}

.toolbar-new:hover {
  background: var(--pq-accent-hover) !important;
  box-shadow: 0 4px 16px rgba(163, 120, 94, 0.35);
  transform: translateY(-1px);
}

.toolbar-new:active {
  transform: scale(0.985) translateY(0);
}

.toolbar-new:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}

@media (max-width: 639px) {
  .productos-toolbar {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .toolbar-field,
  .toolbar-search,
  .toolbar-new {
    width: 100%;
    min-width: 0;
  }
  .toolbar-spacer {
    display: none;
  }
}

.card-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

@media (min-width: 640px) {
  .card-grid { grid-template-columns: repeat(2, 1fr); gap: 20px; }
}
@media (min-width: 1024px) {
  .card-grid { grid-template-columns: repeat(3, 1fr); gap: 24px; }
}

/* Product card — creative Mayor */
.product-card {
  position: relative;
  background: var(--pq-surface);
  border: 1px solid rgba(77, 112, 99, 0.4);
  border-left: 3px solid var(--pq-accent-muted);
  border-radius: var(--pq-radius-md);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: border-color 0.2s cubic-bezier(0.22, 1, 0.36, 1), border-left-color 0.2s ease, transform 0.2s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.2s ease;
  outline: none;
}

.product-card:hover,
.product-card:focus-visible {
  border-color: rgba(77, 112, 99, 0.6);
  border-left-color: var(--pq-accent);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(5, 12, 10, 0.35), 0 0 0 1px rgba(163, 120, 94, 0.08);
}

.product-card:focus-visible {
  box-shadow: 0 0 0 2px var(--pq-accent);
}

.product-card__accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--pq-accent), transparent 60%);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.product-card:hover .product-card__accent {
  opacity: 1;
}

.product-card__body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.product-card__name {
  font-family: var(--pq-font-display);
  font-weight: 600;
  font-size: clamp(15px, 2.5vw, 17px);
  line-height: 1.3;
  color: var(--pq-text);
}

.product-card__sku {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  color: var(--pq-text-subtle);
  font-variant-numeric: tabular-nums;
  margin-top: 2px;
}

.product-card__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.badge {
  font-family: var(--pq-font-utility);
  font-size: 11px;
  letter-spacing: 0.04em;
  font-weight: 500;
}

.badge--filled {
  background: var(--pq-accent) !important;
  color: var(--pq-background) !important;
}

.badge--outlined {
  border-color: var(--pq-accent) !important;
  color: var(--pq-accent) !important;
}

.product-card__meta {
  font-family: var(--pq-font-body);
  font-size: 13px;
  color: var(--pq-text-muted);
  display: flex;
  align-items: center;
  margin-top: 2px;
}

.product-card__sep {
  background: rgba(77, 112, 99, 0.25);
  margin: 0;
}

.product-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.15);
  transition: background 0.2s ease;
}

.product-card:hover .product-card__actions {
  background: rgba(163, 120, 94, 0.04);
}

.product-card__actions .q-btn {
  transition: background-color 0.16s cubic-bezier(0.22, 1, 0.36, 1), color 0.16s ease, transform 0.1s ease;
  border-radius: var(--pq-radius-sm);
}

.product-card__actions .q-btn:hover {
  background-color: rgba(163, 120, 94, 0.1);
  transform: scale(1.05);
}

.product-card__actions .q-btn:active {
  transform: scale(0.95);
}

.product-card__actions .q-btn:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}

.dialog-surface {
  background: var(--pq-elevated) !important;
  border-radius: var(--pq-radius-xl) !important;
  box-shadow: var(--pq-shadow-lg) !important;
  border: 1px solid rgba(77, 112, 99, 0.3);
}

.dialog-title {
  font-family: var(--pq-font-display);
  font-weight: 600;
  font-size: 16px;
  color: var(--pq-text);
}

.error-banner {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 16px;
  border: 1px solid rgba(192, 106, 96, 0.4);
  border-left: 3px solid var(--pq-danger);
  border-radius: var(--pq-radius-md);
  color: var(--pq-danger);
  font-family: var(--pq-font-body);
  font-size: 13px;
  margin-bottom: 24px;
  background: rgba(192, 106, 96, 0.04);
}

.skeleton-card {
  border: 1px solid rgba(77, 112, 99, 0.3);
  border-left: 3px solid var(--pq-accent-muted);
  border-radius: var(--pq-radius-md);
  padding: 20px;
  min-height: 168px;
  background: var(--pq-surface);
}

.skeleton-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-card__badges {
  display: flex;
  gap: 6px;
}

@media (max-width: 639px) {
  .product-card__actions .q-btn {
    min-width: 44px;
    min-height: 44px;
  }
  .card-grid {
    gap: 12px;
  }
}
</style>