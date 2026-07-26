<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { formatCurrency } from 'src/utils/format'
import { ventaService } from '../services/venta.service'
import type { VentaDiaria, VentaRequest } from '../types'
import EmptyState from 'src/components/ui/EmptyState.vue'

useMeta({ title: 'Ventas — PYMEQ' });

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId

const rows = ref<VentaDiaria[]>([])
const loading = shallowRef(false)

const totalSemana = computed(() => {
  const now = new Date()
  const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
  return rows.value
    .filter(r => new Date(r.saleDate) >= weekAgo)
    .reduce((s, r) => s + r.grossAmount, 0)
})

const totalMes = computed(() => {
  const now = new Date()
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
  return rows.value
    .filter(r => new Date(r.saleDate) >= monthStart)
    .reduce((s, r) => s + r.grossAmount, 0)
})

interface DayGroup {
  date: string
  label: string
  items: VentaDiaria[]
  total: number
}

const dayGroups = computed(() => {
  const groups = new Map<string, VentaDiaria[]>()
  for (const v of rows.value) {
    if (!groups.has(v.saleDate)) groups.set(v.saleDate, [])
    groups.get(v.saleDate)!.push(v)
  }
  const result: DayGroup[] = []
  for (const [date, list] of groups) {
    const d = new Date(date)
    const label = d.toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short' })
    result.push({
      date,
      label,
      items: list,
      total: list.reduce((s, v) => s + v.grossAmount, 0),
    })
  }
  result.sort((a, b) => b.date.localeCompare(a.date))
  return result
})

async function load() {
  loading.value = true
  try {
    const res = await ventaService.getAll(tenantId)
    rows.value = res.data
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar ventas' })
  } finally { loading.value = false }
}

const dialogOpen = shallowRef(false)
const editingId = shallowRef<string | null>(null)
const saving = shallowRef(false)
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null)
const form = ref<VentaRequest>({ tenantId: tenantId as string, saleDate: new Date().toISOString().slice(0, 10), grossAmount: 0 })

function openCreate() {
  editingId.value = null
  form.value = { tenantId: tenantId as string, saleDate: new Date().toISOString().slice(0, 10), grossAmount: 0 }
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
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al guardar venta' })
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
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al eliminar venta' })
  } finally {
    deleting.value = false
    deletingItem.value = null
  }
}

onMounted(() => {
  if (!tenantId) return;
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
    <div class="q-mb-md">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Ventas</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Registro diario de ventas</p>
    </div>

    <q-card dark class="glass q-pa-sm q-mb-sm">
      <div class="row q-gutter-x-lg q-pa-xs">
        <div>
          <div class="text-caption text-accent text-uppercase" style="font-size: 0.72rem; letter-spacing: 0.04em">Esta semana</div>
          <div class="font-mono text-weight-bold text-h6">{{ formatCurrency(totalSemana) }}</div>
        </div>
        <div>
          <div class="text-caption text-accent text-uppercase" style="font-size: 0.72rem; letter-spacing: 0.04em">Este mes</div>
          <div class="font-mono text-weight-bold text-h6">{{ formatCurrency(totalMes) }}</div>
        </div>
      </div>
    </q-card>

    <div class="toolbar">
      <q-space />
      <q-btn color="primary" icon="add" label="Nueva" @click="openCreate" />
    </div>

    <div v-if="!loading && !rows.length" class="q-mt-lg">
      <EmptyState
        icon="point_of_sale"
        title="Sin ventas registradas"
        message="Registra tu primera venta del d\u00EDa para llevar el control."
      >
        <q-btn color="primary" icon="add" label="Nueva Venta" @click="openCreate" class="q-mt-sm" />
      </EmptyState>
    </div>

    <div v-if="loading" class="q-gutter-y-md q-mt-md">
      <div v-for="n in 4" :key="n">
        <q-skeleton type="rect" dark animation="pulse" height="48px" />
      </div>
    </div>

    <div v-for="group in dayGroups" :key="group.date" class="day-group">
      <div class="day-group__header">
        <span class="day-group__label">{{ group.label }}</span>
        <span class="day-group__total">{{ formatCurrency(group.total) }}</span>
      </div>

      <div v-for="v in group.items" :key="v.id" class="sale-row">
        <div class="sale-row__desc">{{ v.description || 'Sin descripci\u00F3n' }}</div>
        <div class="sale-row__amount">{{ formatCurrency(v.grossAmount) }}</div>
        <div class="sale-row__actions">
          <q-btn flat dense round icon="edit" color="primary" size="sm" @click="openEdit(v)" aria-label="Editar" />
          <q-btn flat dense round icon="delete" color="negative" size="sm" @click="confirmDelete(v)" aria-label="Eliminar" />
        </div>
      </div>
    </div>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 460px">
        <q-card-section><div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nueva' }} Venta</div></q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <q-input dark filled v-model="form.saleDate" label="Fecha" type="date" :rules="[v => !!v || 'Requerido']" />
            <q-input dark filled v-model.number="form.grossAmount" label="Monto Bruto" type="number" min="0" step="0.01" prefix="$" :rules="[v => !!v || 'Requerido']" />
            <q-input dark filled v-model="form.description" label="Descripci\u00F3n" />
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
          <span>Eliminar venta del <strong>{{ deletingItem?.saleDate }}</strong>?</span>
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
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.day-group {
  margin-bottom: 20px;
}

.day-group:last-child {
  margin-bottom: 0;
}

.day-group__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(27, 38, 36, 0.3);
  border-radius: 6px;
  margin-bottom: 4px;
}

.day-group__label {
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: capitalize;
}

.sale-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(113, 131, 127, 0.04);
}

.sale-row:hover {
  background: rgba(27, 38, 36, 0.3);
}

.sale-row__desc {
  font-size: 0.85rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
