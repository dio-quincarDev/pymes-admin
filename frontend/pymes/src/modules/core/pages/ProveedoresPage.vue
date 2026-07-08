<script setup lang="ts">
import { ref, shallowRef, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { proveedorService } from '../services/proveedor.service'
import type { Proveedor, ProveedorRequest } from '../types'

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const rows = ref<Proveedor[]>([])
const loading = shallowRef(false)
const filter = shallowRef('')
const pagination = shallowRef({ sortBy: 'name', descending: false, page: 1, rowsPerPage: 15 })

const columns = [
  { name: 'name', label: 'Nombre', field: 'name', align: 'left' as const, sortable: true },
  { name: 'contactName', label: 'Contacto', field: 'contactName', align: 'left' as const, sortable: false },
  { name: 'contactPhone', label: 'Teléfono', field: 'contactPhone', align: 'left' as const, sortable: false },
  { name: 'contactEmail', label: 'Email', field: 'contactEmail', align: 'left' as const, sortable: false },
  { name: 'actions', label: 'Acciones', field: 'id', align: 'right' as const, sortable: false },
]

async function load() {
  loading.value = true
  try {
    const res = await proveedorService.getAll(tenantId)
    rows.value = res.data
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar proveedores' })
  } finally { loading.value = false }
}

const dialogOpen = shallowRef(false)
const editingId = shallowRef<string | null>(null)
const saving = shallowRef(false)
const form = ref<ProveedorRequest>({ tenantId, name: '', contactName: '', contactPhone: '', contactEmail: '' })

const deleteDialog = shallowRef(false)
const deletingItem = shallowRef<Proveedor | null>(null)
const deleting = shallowRef(false)

function openCreate() {
  editingId.value = null
  form.value = { tenantId, name: '', contactName: '', contactPhone: '', contactEmail: '' }
  dialogOpen.value = true
}

function openEdit(p: Proveedor) {
  editingId.value = p.id
  form.value = { tenantId: p.tenantId, name: p.name, contactName: p.contactName, contactPhone: p.contactPhone, contactEmail: p.contactEmail }
  dialogOpen.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      const res = await proveedorService.update(editingId.value, form.value)
      const idx = rows.value.findIndex(r => r.id === editingId.value)
      if (idx >= 0) rows.value[idx] = res.data
    } else {
      const res = await proveedorService.create(form.value)
      rows.value.unshift(res.data)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: `Proveedor ${editingId.value ? 'actualizado' : 'creado'}` })
  } catch {
    $q.notify({ type: 'negative', message: 'Error al guardar proveedor' })
  } finally { saving.value = false }
}

function confirmDelete(p: Proveedor) {
  deletingItem.value = p
  deleteDialog.value = true
}

async function remove() {
  if (!deletingItem.value) return
  deleting.value = true
  try {
    await proveedorService.remove(deletingItem.value.id, tenantId)
    rows.value = rows.value.filter(r => r.id !== deletingItem.value!.id)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Proveedor eliminado' })
  } catch {
    $q.notify({ type: 'negative', message: 'Error al eliminar proveedor' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
}

onMounted(load)
</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-md fade-in-up">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Proveedores</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Gestión de proveedores</p>
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

        <template v-slot:body-cell-contactName="{ row }">
          <td><span class="text-accent">{{ row.contactName || '—' }}</span></td>
        </template>
        <template v-slot:body-cell-contactPhone="{ row }">
          <td><span class="text-accent">{{ row.contactPhone || '—' }}</span></td>
        </template>
        <template v-slot:body-cell-contactEmail="{ row }">
          <td><span class="text-accent">{{ row.contactEmail || '—' }}</span></td>
        </template>

        <template v-slot:body-cell-actions="{ row }">
          <td class="text-right">
            <q-btn flat dense round icon="edit" color="primary" @click="openEdit(row)" aria-label="Editar proveedor" />
            <q-btn flat dense round icon="delete" color="negative" @click="confirmDelete(row)" aria-label="Eliminar proveedor" />
          </td>
        </template>
      </q-table>
    </q-card>

    <!-- Dialog: Create/Edit -->
    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="min-width: 400px">
        <q-card-section>
          <div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nuevo' }} Proveedor</div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form @submit.prevent="save" class="q-gutter-y-md">
            <q-input dark filled v-model="form.name" label="Nombre" :rules="[v => !!v || 'Requerido']" />
            <q-input dark filled v-model="form.contactName" label="Nombre de contacto" />
            <q-input dark filled v-model="form.contactPhone" label="Teléfono" type="tel" />
            <q-input dark filled v-model="form.contactEmail" label="Email" type="email" />
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Dialog: Delete Confirmation -->
    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span>¿Eliminar proveedor <strong>{{ deletingItem?.name }}</strong>? Esta acción no se puede deshacer.</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Eliminar" color="negative" :loading="deleting" @click="remove" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>
