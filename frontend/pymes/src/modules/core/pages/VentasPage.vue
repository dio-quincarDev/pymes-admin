<script setup lang="ts">
import { ref, shallowRef, onMounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { formatCurrency } from 'src/utils/format'
import { ventaService } from '../services/venta.service'
import type { VentaDiaria, VentaRequest } from '../types'

useMeta({ title: 'Ventas — PYMEQ' });

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const rows = ref<VentaDiaria[]>([])
const loading = shallowRef(false)
const filter = shallowRef('')
const pagination = shallowRef({ sortBy: 'saleDate', descending: true, page: 1, rowsPerPage: 15 })

const columns = [
  { name: 'saleDate', label: 'Fecha', field: 'saleDate', align: 'left' as const, sortable: true },
  { name: 'description', label: 'Descripción', field: 'description', align: 'left' as const, sortable: false },
  { name: 'grossAmount', label: 'Monto Bruto', field: 'grossAmount', align: 'right' as const, sortable: true },
  { name: 'actions', label: 'Acciones', field: 'id', align: 'right' as const, sortable: false },
]

async function load() {
  loading.value = true
  try {
    const res = await ventaService.getAll(tenantId)
    rows.value = res.data
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar ventas' })
  } finally { loading.value = false }
}

const dialogOpen = shallowRef(false)
const editingId = shallowRef<string | null>(null)
const saving = shallowRef(false)
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null)
const form = ref<VentaRequest>({ tenantId, saleDate: new Date().toISOString().slice(0, 10), grossAmount: 0 })

function openCreate() {
  editingId.value = null
  form.value = { tenantId, saleDate: new Date().toISOString().slice(0, 10), grossAmount: 0 }
  dialogOpen.value = true
}

function openEdit(v: VentaDiaria) {
  editingId.value = v.id
  form.value = { tenantId: v.tenantId, saleDate: v.saleDate, grossAmount: v.grossAmount, description: v.description }
  dialogOpen.value = true
}

async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    if (editingId.value) {
      const res = await ventaService.update(editingId.value, form.value)
      const idx = rows.value.findIndex(r => r.id === editingId.value)
      if (idx >= 0) rows.value[idx] = res.data
    } else {
      const res = await ventaService.create(form.value)
      rows.value.unshift(res.data)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: `Venta ${editingId.value ? 'actualizada' : 'registrada'}` })
  } catch { $q.notify({ type: 'negative', message: 'Error al guardar venta' })
  } finally { saving.value = false }
}

const deleteDialog = shallowRef(false)
const deletingItem = shallowRef<VentaDiaria | null>(null)
const deleting = shallowRef(false)

function confirmDelete(v: VentaDiaria) {
  deletingItem.value = v
  deleteDialog.value = true
}

async function remove() {
  if (!deletingItem.value) return
  deleting.value = true
  try {
    await ventaService.remove(deletingItem.value.id, tenantId)
    rows.value = rows.value.filter(r => r.id !== deletingItem.value!.id)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Venta eliminada' })
  } catch { $q.notify({ type: 'negative', message: 'Error al eliminar venta' })
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
      <h1 class="text-h4 text-primary font-bold q-ma-none">Ventas</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Registro diario de ventas</p>
    </div>

    <q-card dark class="bg-surface-pine">
      <q-table dark flat :rows="rows" :columns="columns" row-key="id" :loading="loading" :filter="filter" v-model:pagination="pagination" :rows-per-page-options="[10, 20, 50]">
        <template v-slot:top>
          <q-input dark dense filled v-model="filter" placeholder="Buscar..." class="q-mr-sm" style="max-width: 250px">
            <template v-slot:prepend><q-icon name="search" /></template>
          </q-input>
          <q-space />
          <q-btn color="primary" icon="add" label="Nueva" @click="openCreate" />
        </template>
        <template v-slot:body-cell-grossAmount="{ row }">
          <td class="text-right text-weight-bold">{{ formatCurrency(row.grossAmount) }}</td>
        </template>
        <template v-slot:body-cell-description="{ row }">
          <td><span class="text-accent">{{ row.description || '—' }}</span></td>
        </template>
        <template v-slot:body-cell-actions="{ row }">
          <td class="text-right">
            <q-btn flat dense round icon="edit" color="primary" @click="openEdit(row)" aria-label="Editar venta" />
            <q-btn flat dense round icon="delete" color="negative" @click="confirmDelete(row)" aria-label="Eliminar venta" />
          </td>
        </template>
      </q-table>
    </q-card>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 460px">
        <q-card-section><div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nueva' }} Venta</div></q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <q-input dark filled v-model="form.saleDate" label="Fecha" type="date" :rules="[v => !!v || 'Requerido']" />
            <q-input dark filled v-model.number="form.grossAmount" label="Monto Bruto" type="number" min="0" step="0.01" prefix="$" :rules="[v => !!v || 'Requerido']" />
            <q-input dark filled v-model="form.description" label="Descripción" />
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span>¿Eliminar venta del <strong>{{ deletingItem?.saleDate }}</strong>?</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Eliminar" color="negative" :loading="deleting" @click="remove" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<style scoped lang="scss">
:deep(.q-dialog__inner > .q-card) {
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(113, 131, 127, 0.08);
}
</style>
