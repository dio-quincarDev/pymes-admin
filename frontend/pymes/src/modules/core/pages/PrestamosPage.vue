<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { formatCurrency } from 'src/utils/format'
import { prestamoService } from '../services/prestamo.service'
import type { Prestamo, PrestamoRequest, PagoPrestamo, PagoPrestamoRequest } from '../types'
import EmptyState from 'src/components/ui/EmptyState.vue'

useMeta({ title: 'Préstamos — PYMEQ' });

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const rows = ref<Prestamo[]>([])
const loading = shallowRef(false)
const filter = shallowRef('')
const pagination = shallowRef({ sortBy: 'startDate', descending: true, page: 1, rowsPerPage: 15 })

const columns = [
  { name: 'name', label: 'Nombre', field: 'name', align: 'left' as const, sortable: true },
  { name: 'lender', label: 'Prestamista', field: 'lender', align: 'left' as const, sortable: false },
  { name: 'amount', label: 'Monto', field: 'amount', align: 'right' as const, sortable: true },
  { name: 'remainingBalance', label: 'Saldo', field: 'remainingBalance', align: 'right' as const, sortable: true },
  { name: 'interestRate', label: 'Tasa %', field: 'interestRate', align: 'center' as const, sortable: false },
  { name: 'status', label: 'Estado', field: 'status', align: 'center' as const, sortable: true },
  { name: 'actions', label: 'Acciones', field: 'id', align: 'right' as const, sortable: false },
]

const statusColor = (s: string) => s === 'ACTIVO' ? 'positive' : s === 'PAGADO' ? 'info' : 'grey'

async function load() {
  loading.value = true
  try {
    const res = await prestamoService.getAll(tenantId)
    rows.value = res.data
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar préstamos' })
  } finally { loading.value = false }
}

// Create/Edit dialog
const dialogOpen = shallowRef(false)
const editingId = shallowRef<string | null>(null)
const saving = shallowRef(false)
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null)
const form = ref<PrestamoRequest>({ tenantId, name: '', amount: 0, interestRate: 0, termMonths: 0, startDate: new Date().toISOString().slice(0, 10) })

function openCreate() {
  editingId.value = null
  form.value = { tenantId, name: '', amount: 0, interestRate: 0, termMonths: 0, startDate: new Date().toISOString().slice(0, 10) }
  dialogOpen.value = true
}

function openEdit(p: Prestamo) {
  editingId.value = p.id
  form.value = { tenantId: p.tenantId, name: p.name, lender: p.lender, amount: p.amount, interestRate: p.interestRate, termMonths: p.termMonths, startDate: p.startDate, notes: p.notes }
  dialogOpen.value = true
}

async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    if (editingId.value) {
      const res = await prestamoService.update(editingId.value, form.value)
      const idx = rows.value.findIndex(r => r.id === editingId.value)
      if (idx >= 0) rows.value[idx] = res.data
    } else {
      const res = await prestamoService.create(form.value)
      rows.value.unshift(res.data)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: `Préstamo ${editingId.value ? 'actualizado' : 'creado'}` })
  } catch { $q.notify({ type: 'negative', message: 'Error al guardar préstamo' })
  } finally { saving.value = false }
}

// Delete dialog
const deleteDialog = shallowRef(false)
const deletingItem = shallowRef<Prestamo | null>(null)
const deleting = shallowRef(false)

function confirmDelete(p: Prestamo) {
  deletingItem.value = p
  deleteDialog.value = true
}

async function remove() {
  if (!deletingItem.value) return
  deleting.value = true
  try {
    await prestamoService.remove(deletingItem.value.id, tenantId)
    rows.value = rows.value.filter(r => r.id !== deletingItem.value!.id)
    deleteDialog.value = false
    $q.notify({ type: 'positive', message: 'Préstamo eliminado' })
  } catch { $q.notify({ type: 'negative', message: 'Error al eliminar préstamo' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
}

// Pago dialog
const pagoDialog = shallowRef(false)
const pagoPrestamo = shallowRef<Prestamo | null>(null)
const pagos = ref<PagoPrestamo[]>([])
const savingPago = shallowRef(false)
const pagoForm = ref<PagoPrestamoRequest>({ amount: 0, paymentDate: new Date().toISOString().slice(0, 10) })
const pagosLoaded = shallowRef(false)

function openPagos(p: Prestamo) {
  pagoPrestamo.value = p
  pagoForm.value = { amount: 0, paymentDate: new Date().toISOString().slice(0, 10) }
  pagosLoaded.value = false
  pagos.value = []
  pagoDialog.value = true
  void loadPagos(p.id)
}

async function loadPagos(loanId: string) {
  try {
    const res = await prestamoService.getPagos(loanId, tenantId)
    pagos.value = res.data
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar pagos' })
  } finally { pagosLoaded.value = true }
}

async function savePago() {
  if (!pagoPrestamo.value) return
  savingPago.value = true
  try {
    await prestamoService.createPago(pagoPrestamo.value.id, pagoForm.value, tenantId)
    $q.notify({ type: 'positive', message: 'Pago registrado' })
    pagoForm.value = { amount: 0, paymentDate: new Date().toISOString().slice(0, 10) }
    await loadPagos(pagoPrestamo.value.id)
    await load()
  } catch { $q.notify({ type: 'negative', message: 'Error al registrar pago' })
  } finally { savingPago.value = false }
}

const totalPagado = computed(() => pagos.value.reduce((sum, p) => sum + p.amount, 0))

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
      <h1 class="text-h4 text-primary font-bold q-ma-none">Préstamos</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Gestión de préstamos y pagos</p>
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
        <template v-slot:body-cell-remainingBalance="{ row }">
          <td class="text-right">{{ formatCurrency(row.remainingBalance) }}</td>
        </template>
        <template v-slot:body-cell-interestRate="{ row }">
          <td class="text-center">{{ row.interestRate }}%</td>
        </template>
        <template v-slot:body-cell-status="{ row }">
          <td class="text-center"><q-badge :color="statusColor(row.status)" class="q-px-sm q-py-xs">{{ row.status }}</q-badge></td>
        </template>
        <template v-slot:body-cell-actions="{ row }">
          <td class="text-right">
            <q-btn flat dense round icon="payments" color="positive" @click="openPagos(row)" aria-label="Pagos">
              <q-tooltip>Ver pagos</q-tooltip>
            </q-btn>
            <q-btn v-if="row.status === 'ACTIVO'" flat dense round icon="edit" color="primary" @click="openEdit(row)" aria-label="Editar préstamo" />
            <q-btn flat dense round icon="delete" color="negative" @click="confirmDelete(row)" aria-label="Eliminar préstamo" />
          </td>
        </template>
        <template v-slot:no-data>
          <EmptyState
            v-if="!loading"
            icon="account_balance"
            title="Sin préstamos"
            message="Registra un préstamo para hacer seguimiento de tus deudas."
          >
            <q-btn color="primary" icon="add" label="Nuevo Préstamo" @click="openCreate" class="q-mt-sm" />
          </EmptyState>
        </template>
      </q-table>
    </q-card>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 520px">
        <q-card-section><div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nuevo' }} Préstamo</div></q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <q-input dark filled v-model="form.name" label="Nombre" :rules="[v => !!v || 'Requerido']" />
            <div class="row q-col-gutter-md">
              <div class="col-6">
                <q-input dark filled v-model.number="form.amount" label="Monto" type="number" min="0" step="0.01" prefix="$" :rules="[v => !!v || 'Requerido']" />
              </div>
              <div class="col-6">
                <q-input dark filled v-model="form.startDate" label="Fecha inicio" type="date" :rules="[v => !!v || 'Requerido']" />
              </div>
            </div>
            <div class="row q-col-gutter-md">
              <div class="col-6">
                <q-input dark filled v-model.number="form.interestRate" label="Tasa % mensual" type="number" min="0" step="0.01" suffix="%" :rules="[v => !!v || 'Requerido']" />
              </div>
              <div class="col-6">
                <q-input dark filled v-model.number="form.termMonths" label="Plazo (meses)" type="number" min="1" step="1" :rules="[v => !!v || 'Requerido']" />
              </div>
            </div>
            <q-input dark filled v-model="form.lender" label="Prestamista" />
            <q-input dark filled v-model="form.notes" label="Notas" type="textarea" />
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="pagoDialog" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 600px">
        <q-card-section>
          <div class="text-h6 text-primary">Pagos: <strong>{{ pagoPrestamo?.name }}</strong></div>
          <div class="text-caption text-accent q-mt-xs">
            Monto: {{ formatCurrency(pagoPrestamo?.amount || 0) }} |
            Saldo: {{ formatCurrency(pagoPrestamo?.remainingBalance || 0) }} |
            Pagado: {{ formatCurrency(totalPagado) }}
          </div>
        </q-card-section>
        <q-separator dark />
        <q-card-section class="q-gutter-y-sm">
          <div v-if="!pagos.length && pagosLoaded" class="text-accent text-center q-py-md">Sin pagos registrados</div>
          <div v-for="p in pagos" :key="p.id" class="row items-center justify-between bg-dark q-px-md q-py-sm" style="border-radius: 4px">
            <div>
              <span class="text-weight-bold">{{ formatCurrency(p.amount) }}</span>
              <span class="text-accent text-caption q-ml-sm">{{ p.paymentDate }}</span>
            </div>
            <span class="text-accent text-caption">{{ p.paymentMethod || '—' }}</span>
          </div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <div class="text-subtitle2 text-primary q-mb-sm">Registrar pago</div>
          <div class="row q-col-gutter-sm items-end">
            <div class="col-4">
              <q-input dark dense filled v-model.number="pagoForm.amount" label="Monto" type="number" min="0" step="0.01" prefix="$" />
            </div>
            <div class="col-4">
              <q-input dark dense filled v-model="pagoForm.paymentDate" label="Fecha" type="date" />
            </div>
            <div class="col-4">
              <q-btn label="Registrar" color="positive" :loading="savingPago" @click="savePago" class="full-width" />
            </div>
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span>¿Eliminar préstamo <strong>{{ deletingItem?.name }}</strong>?</span>
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
