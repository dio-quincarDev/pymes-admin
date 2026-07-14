<script setup lang="ts">
import { ref, shallowRef, onMounted, onUnmounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { formatCurrency } from 'src/utils/format'
import { gastoService } from '../services/gasto.service'
import type { GastoOperativo, GastoRequest } from '../types'
import EmptyState from 'src/components/ui/EmptyState.vue'

useMeta({ title: 'Gastos — PYMEQ' });

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const categoriaOptions = ['SALARIOS', 'AGUA', 'LUZ', 'INTERNET', 'ALQUILER', 'MANTENIMIENTO', 'PUBLICIDAD', 'OTROS']
const metodoPagoOptions = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'CHEQUE']

const rows = ref<GastoOperativo[]>([])
const loading = shallowRef(false)
const filter = shallowRef('')
const pagination = shallowRef({ sortBy: 'expenseDate', descending: true, page: 1, rowsPerPage: 15 })

const columns = [
  { name: 'expenseDate', label: 'Fecha', field: 'expenseDate', align: 'left' as const, sortable: true },
  { name: 'category', label: 'Categoría', field: 'category', align: 'left' as const, sortable: true },
  { name: 'description', label: 'Descripción', field: 'description', align: 'left' as const, sortable: false },
  { name: 'amount', label: 'Monto', field: 'amount', align: 'right' as const, sortable: true },
  { name: 'paymentMethod', label: 'Pago', field: 'paymentMethod', align: 'center' as const, sortable: false },
  { name: 'actions', label: 'Acciones', field: 'id', align: 'right' as const, sortable: false },
]

async function load() {
  loading.value = true
  try {
    const res = await gastoService.getAll(tenantId)
    rows.value = res.data
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar gastos' })
  } finally { loading.value = false }
}

const dialogOpen = shallowRef(false)
const editingId = shallowRef<string | null>(null)
const saving = shallowRef(false)
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null)
const form = ref<GastoRequest>({ tenantId, category: '', description: '', amount: 0, expenseDate: new Date().toISOString().slice(0, 10) })

function openCreate() {
  editingId.value = null
  form.value = { tenantId, category: '', description: '', amount: 0, expenseDate: new Date().toISOString().slice(0, 10) }
  dialogOpen.value = true
}

function openEdit(g: GastoOperativo) {
  editingId.value = g.id
  form.value = { tenantId: g.tenantId, category: g.category, description: g.description, amount: g.amount, expenseDate: g.expenseDate, paymentMethod: g.paymentMethod }
  dialogOpen.value = true
}

async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    if (editingId.value) {
      const res = await gastoService.update(editingId.value, form.value)
      const idx = rows.value.findIndex(r => r.id === editingId.value)
      if (idx >= 0) rows.value[idx] = res.data
    } else {
      const res = await gastoService.create(form.value)
      rows.value.unshift(res.data)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: `Gasto ${editingId.value ? 'actualizado' : 'creado'}` })
  } catch { $q.notify({ type: 'negative', message: 'Error al guardar gasto' })
  } finally { saving.value = false }
}

const deleteDialog = shallowRef(false)
const deletingItem = shallowRef<GastoOperativo | null>(null)
const deleting = shallowRef(false)

function confirmDelete(g: GastoOperativo) {
  deletingItem.value = g
  deleteDialog.value = true
}

async function remove() {
  if (!deletingItem.value) return
  deleting.value = true
  try {
    await gastoService.remove(deletingItem.value.id, tenantId)
    rows.value = rows.value.filter(r => r.id !== deletingItem.value!.id)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Gasto eliminado' })
  } catch { $q.notify({ type: 'negative', message: 'Error al eliminar gasto' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
}

onMounted(() => {
  void load()
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
    <div class="q-mb-md fade-in-up">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Gastos Operativos</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Registro de gastos operativos del negocio</p>
    </div>

    <q-card dark class="bg-surface-pine">
      <q-table dark flat :rows="rows" :columns="columns" row-key="id" :loading="loading" :filter="filter" v-model:pagination="pagination" :rows-per-page-options="[10, 20, 50]">
        <template v-slot:top>
          <q-input dark dense filled v-model="filter" placeholder="Buscar..." class="q-mr-sm" style="max-width: 250px">
            <template v-slot:prepend><q-icon name="search" /></template>
          </q-input>
          <q-space />
          <q-btn color="primary" icon="add" label="Nuevo" @click="openCreate" />
        </template>
        <template v-slot:body-cell-amount="{ row }">
          <td class="text-right text-weight-bold">{{ formatCurrency(row.amount) }}</td>
        </template>
        <template v-slot:body-cell-paymentMethod="{ row }">
          <td class="text-center"><span class="text-accent">{{ row.paymentMethod || '—' }}</span></td>
        </template>
        <template v-slot:body-cell-actions="{ row }">
          <td class="text-right">
            <q-btn flat dense round icon="edit" color="primary" @click="openEdit(row)" aria-label="Editar gasto" />
            <q-btn flat dense round icon="delete" color="negative" @click="confirmDelete(row)" aria-label="Eliminar gasto" />
          </td>
        </template>
        <template v-slot:no-data>
          <EmptyState
            v-if="!loading"
            icon="money_off"
            title="Sin gastos registrados"
            message="Registra tu primer gasto operativo del negocio."
          >
            <q-btn color="primary" icon="add" label="Nuevo Gasto" @click="openCreate" class="q-mt-sm" />
          </EmptyState>
        </template>
      </q-table>
    </q-card>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section><div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nuevo' }} Gasto</div></q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <div class="row q-col-gutter-md">
              <div class="col-6">
                <q-input dark filled v-model="form.expenseDate" label="Fecha" type="date" :rules="[v => !!v || 'Requerido']" />
              </div>
              <div class="col-6">
                <q-select dark filled v-model="form.category" :options="categoriaOptions" label="Categoría" :rules="[v => !!v || 'Requerido']" />
              </div>
            </div>
            <q-input dark filled v-model="form.description" label="Descripción" :rules="[v => !!v || 'Requerido']" />
            <q-input dark filled v-model.number="form.amount" label="Monto" type="number" min="0" step="0.01" prefix="$" :rules="[v => !!v || 'Requerido']" />
            <q-select dark filled v-model="form.paymentMethod" :options="metodoPagoOptions" label="Método de pago" clearable />
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
          <span>¿Eliminar gasto <strong>{{ deletingItem?.description }}</strong>?</span>
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
