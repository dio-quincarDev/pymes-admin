<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { api } from 'src/boot/axios'
import { productoService } from '../services/producto.service'
import type { Producto, ProductoRequest, Presentacion, PresentacionRequest, SetupInfo, SetupCategory } from '../types'

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const rows = ref<Producto[]>([])
const loading = ref(false)
const filter = ref('')
const pagination = ref({ sortBy: 'name', descending: false, page: 1, rowsPerPage: 15 })

const catOptions = ref<{ label: string; value: string }[]>([])
const unitOptions = ref<{ label: string; value: string }[]>([])

const columns = [
  { name: 'name', label: 'Nombre', field: 'name', align: 'left' as const, sortable: true },
  { name: 'sku', label: 'SKU', field: 'sku', align: 'left' as const, sortable: true },
  { name: 'category', label: 'Categoría', field: 'category', align: 'left' as const, sortable: false },
  { name: 'baseUnit', label: 'Unidad', field: 'baseUnit', align: 'left' as const, sortable: false },
  { name: 'presentaciones', label: 'Presentaciones', field: 'id', align: 'left' as const, sortable: false },
  { name: 'actions', label: 'Acciones', field: 'id', align: 'right' as const, sortable: false },
]

function flattenCategories(cats: SetupCategory[]): { label: string; value: string }[] {
  const result: { label: string; value: string }[] = []
  for (const c of cats) {
    result.push({ label: c.name, value: c.code })
    if (c.children?.length) result.push(...flattenCategories(c.children))
  }
  return result
}

async function loadSetup() {
  if (!tenantId) return
  try {
    const res = await api.get<SetupInfo>(`/core/setup/${tenantId}`)
    catOptions.value = flattenCategories(res.data.categories || [])
    unitOptions.value = (res.data.units || []).map(u => ({ label: u.name, value: u.code }))
  } catch { /* template data non-critical */ }
}

async function load() {
  loading.value = true
  try {
    const res = await productoService.getAll(tenantId)
    rows.value = res.data
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar productos' })
  } finally { loading.value = false }
}

const dialogOpen = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const form = ref<ProductoRequest>({ tenantId, name: '', sku: '', category: '', baseUnit: '' })

function openCreate() {
  editingId.value = null
  form.value = { tenantId, name: '', sku: '', category: '', baseUnit: '' }
  dialogOpen.value = true
}

function openEdit(p: Producto) {
  editingId.value = p.id
  form.value = { tenantId: p.tenantId, name: p.name, sku: p.sku, category: p.category, baseUnit: p.baseUnit }
  dialogOpen.value = true
}

onMounted(async () => { await load(); await loadSetup() })

async function save() {
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

const presDialog = ref(false)
const presProduct = ref<Producto | null>(null)
const presItems = ref<Presentacion[]>([])
const presForm = ref<PresentacionRequest>({ name: '', conversion: 1 })
const addingPres = ref(false)
const removingPres = ref(false)

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

const deleteDialog = ref(false)
const deletingItem = ref<Producto | null>(null)
const deleting = ref(false)

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
      >
        <template v-slot:top>
          <q-input dark dense filled v-model="filter" placeholder="Buscar..." class="q-mr-sm" style="max-width: 250px">
            <template v-slot:prepend><q-icon name="search" /></template>
          </q-input>
          <q-space />
          <q-btn color="primary" icon="add" label="Nuevo" @click="openCreate" />
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
            <q-btn flat dense round icon="layers" color="info" @click="openPresentations(row)">
              <q-tooltip>Presentaciones</q-tooltip>
            </q-btn>
            <q-btn flat dense round icon="edit" color="primary" @click="openEdit(row)" />
            <q-btn flat dense round icon="delete" color="negative" @click="confirmDelete(row)" />
          </td>
        </template>
      </q-table>
    </q-card>

    <!-- Dialog: Create/Edit Product -->
    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="min-width: 480px">
        <q-card-section>
          <div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nuevo' }} Producto</div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form @submit.prevent="save" class="q-gutter-y-md">
            <q-input dark filled v-model="form.name" label="Nombre" :rules="[v => !!v || 'Requerido']" />
            <q-input dark filled v-model="form.sku" label="SKU" :rules="[v => !!v || 'Requerido']" />
            <q-select dark filled v-model="form.category" label="Categoría" :options="catOptions" option-value="value" option-label="label" emit-value map-options use-input input-debounce="0" @filter="(val, update) => { update(() => catOptions.filter((o: { label: string; value: string }) => !val || o.label.toLowerCase().includes(val.toLowerCase()))) }" />
            <q-select dark filled v-model="form.baseUnit" label="Unidad base" :options="unitOptions" option-value="value" option-label="label" emit-value map-options use-input input-debounce="0" @filter="(val, update) => { update(() => unitOptions.filter((o: { label: string; value: string }) => !val || o.label.toLowerCase().includes(val.toLowerCase()))) }" />
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
      <q-card dark class="bg-surface-pine" style="min-width: 450px">
        <q-card-section>
          <div class="text-h6 text-primary">Presentaciones de <strong>{{ presProduct?.name }}</strong></div>
        </q-card-section>
        <q-separator dark />
        <q-card-section class="q-gutter-y-sm">
          <div v-if="!presItems.length" class="text-accent text-center q-py-md">Sin presentaciones</div>
          <div v-for="p in presItems" :key="p.id" class="pres-row row items-center justify-between bg-dark q-px-md q-py-sm">
            <span>{{ p.name }} <span class="text-accent text-caption">(x{{ p.conversion }})</span></span>
            <q-btn flat dense round icon="delete" color="negative" size="sm" @click="removePresentation(p)" :disable="removingPres" />
          </div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <div class="text-subtitle2 text-primary q-mb-sm">Agregar presentación</div>
          <div class="row q-col-gutter-sm items-center">
            <div class="col-5">
              <q-input dark dense filled v-model="presForm.name" label="Nombre" placeholder="Ej: Caja x24" />
            </div>
            <div class="col-4">
              <q-input dark dense filled v-model.number="presForm.conversion" label="Conversión" type="number" min="1" />
            </div>
            <div class="col-3">
              <q-btn label="Agregar" color="primary" :loading="addingPres" @click="addPresentation" />
            </div>
          </div>
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
.pres-row { border-radius: 4px; }
</style>
