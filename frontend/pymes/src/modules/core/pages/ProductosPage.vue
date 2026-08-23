<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { api } from 'src/boot/axios'
import { productoService } from '../services/producto.service'
import { proveedorService } from '../services/proveedor.service'
import type { Producto, ProductoRequest, SetupInfo, SetupCategory } from '../types'
import PresentacionesDialog from '../components/PresentacionesDialog.vue'
import EmptyState from 'src/components/ui/EmptyState.vue'

useMeta({ title: 'Productos — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId

const rows = ref<Producto[]>([])
const loading = shallowRef(false)
const search = shallowRef('')
const page = shallowRef(0)
const totalElements = shallowRef(0)
const categoryFilter = shallowRef('')

const catOptions = ref<{ label: string; value: string }[]>([])
const setupCategories = ref<SetupCategory[]>([])
const unitOptions = ref<{ label: string; value: string }[]>([])
const providerOptions = ref<{ label: string; value: string }[]>([])

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
  for (const o of unitOptions.value) map.set(o.value, o.label)
  return map
})

const totalCategories = computed(() => {
  const seen = new Set(filteredRows.value.map(p => p.category))
  return seen.size
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
  } catch { /* non-critical */ }
}

async function load(p = 0) {
  if (!tenantId) return
  loading.value = true
  try {
    const params: { category?: string; page: number; size?: number } = { page: p, size: 30 }
    if (categoryFilter.value) params.category = categoryFilter.value
    const res = await productoService.search(tenantId, params)
    rows.value = p === 0 ? res.data.content : [...rows.value, ...res.data.content]
    totalElements.value = res.data.totalElements
    page.value = p
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar productos' })
  } finally {
    loading.value = false
  }
}

const filteredRows = computed(() => {
  if (!search.value) return rows.value
  const q = search.value.toLowerCase()
  return rows.value.filter(r =>
    r.name.toLowerCase().includes(q) ||
    r.sku?.toLowerCase().includes(q) ||
    r.proveedorName?.toLowerCase().includes(q)
  )
})

const dialogOpen = shallowRef(false)
const editingId = shallowRef<string | null>(null)
const saving = shallowRef(false)
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null)
const form = ref<ProductoRequest>({ tenantId: tenantId as string, name: '', category: '', baseUnit: '', proveedorId: null })

const presDialog = shallowRef(false)
const presProduct = ref<Producto | null>(null)
const unitLabel = computed(() => {
  if (!presProduct.value) return 'unidades'
  const opt = unitOptions.value.find(o => o.value === presProduct.value!.baseUnit)
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
      const res = await productoService.update(editingId.value, form.value)
      const idx = rows.value.findIndex(r => r.id === editingId.value)
      if (idx >= 0) rows.value[idx] = res.data
    } else {
      const res = await productoService.create(form.value)
      rows.value.unshift(res.data)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: `Producto ${editingId.value ? 'actualizado' : 'creado'}` })
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
    rows.value = rows.value.filter(r => r.id !== deletingItem.value!.id)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Producto eliminado' })
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al eliminar producto' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
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
</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-md">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Productos</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Catálogo de productos y presentaciones</p>
    </div>

    <div class="row items-center q-gutter-x-xs q-mb-sm">
      <span class="text-accent text-caption">{{ totalElements }} {{ totalElements === 1 ? 'producto' : 'productos' }}</span>
      <q-icon name="circle" size="0.25rem" color="accent" />
      <span class="text-accent text-caption">{{ totalCategories }} {{ totalCategories === 1 ? 'categoría' : 'categorías' }}</span>
    </div>

    <div class="row items-center q-gutter-x-sm q-mb-md">
      <q-select
        dark dense filled
        v-model="categoryFilter"
        :options="catOptions"
        label="Categoría"
        clearable emit-value map-options
        style="min-width: 180px"
        @update:model-value="load()"
      />
      <q-input
        dark dense filled v-model="search"
        placeholder="Buscar..."
        style="max-width: 240px"
      >
        <template v-slot:prepend><q-icon name="search" /></template>
      </q-input>
      <q-space />
      <q-btn v-if="rows.length" color="primary" icon="sym_r_add" label="Nuevo" @click="openCreate" />
    </div>

    <div v-if="!loading && !filteredRows.length">
      <EmptyState
        icon="sym_r_inventory_2"
        title="Sin productos"
        message="Agrega tu primer producto al catálogo para comenzar a facturar."
      >
        <q-btn color="primary" icon="sym_r_add" label="Nuevo Producto" @click="openCreate" class="q-mt-sm" />
      </EmptyState>
    </div>

    <div v-if="loading" class="row q-col-gutter-x-sm q-col-gutter-y-md">
      <div v-for="n in 6" :key="n" class="col-12 col-sm-6 col-md-4">
        <q-skeleton type="rect" dark animation="pulse" class="full-width" height="140px" />
      </div>
    </div>

    <div v-if="!loading && filteredRows.length" class="row q-col-gutter-x-sm q-col-gutter-y-sm">
      <div v-for="item in filteredRows" :key="item.id" class="col-12 col-sm-6 col-md-4">
        <q-card dark class="glass hover-lift">
          <q-card-section class="q-pa-md">
            <div class="text-weight-bold q-mb-xs">{{ item.name }}</div>
            <div v-if="item.sku" class="text-caption text-accent q-mb-sm">{{ item.sku }}</div>
            <div class="row q-gutter-x-xs">
              <q-chip v-if="item.category" dense dark size="sm" color="accent" text-color="dark">
                {{ categoryNameMap.get(item.category) || item.category }}
              </q-chip>
              <q-chip v-if="item.baseUnit" dense dark size="sm" outline color="accent">
                {{ unitNameMap.get(item.baseUnit) || item.baseUnit }}
              </q-chip>
            </div>
            <div v-if="item.proveedorName" class="text-caption text-accent q-mt-sm">
              <q-icon name="store" size="0.85rem" class="q-mr-xs" />
              {{ item.proveedorName }}
            </div>
            <div v-if="item.presentaciones?.length" class="text-caption text-accent q-mt-xs">
              {{ item.presentaciones.length }} {{ item.presentaciones.length === 1 ? 'presentación' : 'presentaciones' }}
            </div>
          </q-card-section>
          <q-separator dark />
          <q-card-actions align="right" class="q-pa-xs">
            <q-btn flat dense round icon="sym_r_layers" color="info" size="sm" @click="openPresentaciones(item)" aria-label="Presentaciones" />
            <q-btn flat dense round icon="sym_r_edit" color="primary" size="sm" @click="openEdit(item)" aria-label="Editar" />
            <q-btn flat dense round icon="sym_r_delete" color="negative" size="sm" @click="confirmDelete(item)" aria-label="Eliminar" />
          </q-card-actions>
        </q-card>
      </div>
    </div>

    <div class="q-mt-md flex justify-center" v-if="totalElements > rows.length && !search && !categoryFilter">
      <q-btn flat color="primary" label="Cargar más" @click="load(page + 1)" :loading="loading" />
    </div>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section>
          <div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nuevo' }} Producto</div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <q-input dark filled v-model="form.name" label="Nombre" :rules="[v => !!v || 'Requerido']" />
            <q-select dark filled v-model="form.category" label="Categoría" :options="catOptions" option-value="value" option-label="label" emit-value map-options use-input input-debounce="0" @filter="(val, update) => { update(() => catOptions.filter(o => !val || o.label.toLowerCase().includes(val.toLowerCase()))) }" :rules="[v => !!v || 'Requerido']" />
            <q-select dark filled v-model="form.baseUnit" label="Unidad base" :options="unitOptions" option-value="value" option-label="label" emit-value map-options use-input input-debounce="0" @filter="(val, update) => { update(() => unitOptions.filter(o => !val || o.label.toLowerCase().includes(val.toLowerCase()))) }" />
            <q-select dark filled v-model="form.proveedorId" label="Proveedor" :options="providerOptions" option-value="value" option-label="label" emit-value map-options clearable />
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <PresentacionesDialog
      v-model="presDialog"
      :product="presProduct"
      :unit-label="unitLabel"
      @updated="load()"
    />

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
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
