<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { formatCurrency } from 'src/utils/format';
import { gastoService } from '../services/gasto.service';
import type { GastoOperativo, GastoRequest } from '../types';
import EmptyState from 'src/components/ui/EmptyState.vue';

useMeta({ title: 'Gastos — PYMEQ' });

const $q = useQuasar();
const router = useRouter();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;

const categoriaOptions = [
  'SALARIOS',
  'AGUA',
  'LUZ',
  'INTERNET',
  'ALQUILER',
  'MANTENIMIENTO',
  'PUBLICIDAD',
  'OTROS',
];
const metodoPagoOptions = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'CHEQUE'];

const rows = ref<GastoOperativo[]>([]);
const loading = shallowRef(false);

interface CatGroup {
  categoria: string;
  items: GastoOperativo[];
  total: number;
}

const catGroups = computed(() => {
  const groups = new Map<string, GastoOperativo[]>();
  for (const g of rows.value) {
    const cat = g.categoria || 'OTROS';
    if (!groups.has(cat)) groups.set(cat, []);
    groups.get(cat)!.push(g);
  }
  const result: CatGroup[] = [];
  for (const [categoria, list] of groups) {
    result.push({
      categoria,
      items: list,
      total: list.reduce((s, g) => s + g.monto, 0),
    });
  }
  result.sort((a, b) => b.total - a.total);
  return result;
});

const totalGeneral = computed(() => rows.value.reduce((s, g) => s + g.monto, 0));

async function load() {
  if (!tenantId) return;
  loading.value = true;
  try {
    const res = await gastoService.getAll(tenantId);
    rows.value = res.data;
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar gastos',
    });
  } finally {
    loading.value = false;
  }
}

const dialogOpen = shallowRef(false);
const editingId = shallowRef<string | null>(null);
const saving = shallowRef(false);
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const form = ref<GastoRequest>({
  tenantId: tenantId as string,
  categoria: '',
  descripcion: '',
  monto: 0,
  fecha: new Date().toISOString().slice(0, 10),
});

const amountStr = ref('');
function onAmountInput(val: string | number | null) {
  amountStr.value = String(val ?? '')
    .replace(/[^0-9.]/g, '')
    .replace(/(\..*)\./g, '$1');
}
function formatAmount() {
  const n = parseFloat(amountStr.value);
  if (!isNaN(n) && amountStr.value) {
    amountStr.value = n.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    form.value.monto = n;
  }
}
function rawAmount(val: number) {
  return val
    ? val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '';
}

function openCreate() {
  editingId.value = null;
  form.value = {
    tenantId: tenantId as string,
    categoria: '',
    descripcion: '',
    monto: 0,
    fecha: new Date().toISOString().slice(0, 10),
  };
  amountStr.value = '';
  dialogOpen.value = true;
}

function openEdit(g: GastoOperativo) {
  editingId.value = g.id;
  form.value = {
    tenantId: g.tenantId,
    categoria: g.categoria,
    descripcion: g.descripcion,
    monto: g.monto,
    fecha: g.fecha,
    metodoPago: g.metodoPago,
  };
  amountStr.value = rawAmount(g.monto);
  dialogOpen.value = true;
}

async function save() {
  formatAmount();
  if (!(await formRef.value?.validate())) return;
  saving.value = true;
  try {
    if (editingId.value) {
      const res = await gastoService.update(editingId.value, form.value);
      const idx = rows.value.findIndex((r) => r.id === editingId.value);
      if (idx >= 0) rows.value[idx] = res.data;
    } else {
      const res = await gastoService.create(form.value);
      rows.value.unshift(res.data);
    }
    dialogOpen.value = false;
    $q.notify({ type: 'positive', message: `Gasto ${editingId.value ? 'actualizado' : 'creado'}` });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al guardar gasto',
    });
  } finally {
    saving.value = false;
  }
}

const deleteDialog = shallowRef(false);
const deletingItem = shallowRef<GastoOperativo | null>(null);
const deleting = shallowRef(false);

function confirmDelete(g: GastoOperativo) {
  deletingItem.value = g;
  deleteDialog.value = true;
}

async function remove() {
  if (!deletingItem.value || !tenantId) return;
  deleting.value = true;
  try {
    await gastoService.remove(deletingItem.value.id, tenantId);
    rows.value = rows.value.filter((r) => r.id !== deletingItem.value!.id);
    deleteDialog.value = false;
    $q.notify({ type: 'positive', message: 'Gasto eliminado' });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al eliminar gasto',
    });
  } finally {
    deleting.value = false;
    deletingItem.value = null;
  }
}

onMounted(() => {
  if (!tenantId) return;
  void load();
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => window.removeEventListener('keydown', handleKeydown));

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault();
    openCreate();
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 's' && dialogOpen.value) {
    e.preventDefault();
    void save();
  }
}
</script>

<template>
  <q-page class="core-page">
    <q-banner
      class="deprecation-banner q-mb-md"
      role="note"
    >
      <template v-slot:avatar>
        <q-icon name="info" />
      </template>
      <div>
        <strong>Esta sección quedó en desuso.</strong>
        Los gastos fijos y colaboradores ahora se administran en Estructura de Costos.
      </div>
      <template v-slot:action>
        <q-btn
          flat
          dense
          no-caps
          label="Ir a Costos → Gastos Fijos"
          color="accent"
          @click="router.push('/dashboard/costos?tab=gastosFijos')"
        />
      </template>
    </q-banner>

    <div class="q-mb-md">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Gastos Operativos</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Registro de gastos operativos del negocio</p>
    </div>

    <q-card dark class="glass q-pa-sm q-mb-sm">
      <div class="row items-baseline q-gutter-x-sm q-pa-xs">
        <span
          class="text-caption text-accent text-uppercase"
          style="font-size: 0.72rem; letter-spacing: 0.04em"
          >Total general</span
        >
        <span class="font-mono text-weight-bold text-h6">{{ formatCurrency(totalGeneral) }}</span>
      </div>
    </q-card>

    <div class="toolbar">
      <q-space />
      <q-btn v-if="rows.length" color="primary" icon="sym_r_add" label="Nuevo" @click="openCreate" />
    </div>

    <div v-if="!loading && !rows.length" class="q-mt-lg">
      <EmptyState
        icon="sym_r_money_off"
        title="Sin gastos registrados"
        message="Registra tu primer gasto operativo del negocio."
      >
        <q-btn color="primary" icon="sym_r_add" label="Nuevo Gasto" @click="openCreate" class="q-mt-sm" />
      </EmptyState>
    </div>

    <div v-if="loading" class="q-gutter-y-md q-mt-md">
      <div v-for="n in 4" :key="n">
        <q-skeleton type="rect" dark animation="pulse" height="80px" />
      </div>
    </div>

    <div v-for="group in catGroups" :key="group.categoria" class="cat-group">
      <div class="cat-group__header">
        <span class="cat-group__title">{{ group.categoria }}</span>
        <span class="cat-group__count"
          >{{ group.items.length }} gasto{{ group.items.length !== 1 ? 's' : '' }}</span
        >
        <q-space />
        <span class="cat-group__total">{{ formatCurrency(group.total) }}</span>
      </div>

      <div v-for="g in group.items" :key="g.id" class="expense-row">
        <div class="expense-row__main">
          <div class="expense-row__desc">{{ g.descripcion }}</div>
          <div class="expense-row__meta">
            <span>{{ g.fecha }}</span>
            <span v-if="g.metodoPago" class="expense-row__sep">·</span>
            <span v-if="g.metodoPago">{{ g.metodoPago }}</span>
          </div>
        </div>
        <div class="expense-row__amount">{{ formatCurrency(g.monto) }}</div>
        <div class="expense-row__actions">
          <q-btn
            flat
            dense
            round
            icon="sym_r_edit"
            color="primary"
            size="sm"
            @click="openEdit(g)"
            aria-label="Editar"
          />
          <q-btn
            flat
            dense
            round
            icon="sym_r_delete"
            color="negative"
            size="sm"
            @click="confirmDelete(g)"
            aria-label="Eliminar"
          />
        </div>
      </div>
    </div>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section
          ><div class="text-h6 text-primary">
            {{ editingId ? 'Editar' : 'Nuevo' }} Gasto
          </div></q-card-section
        >
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <div class="row q-col-gutter-md">
              <div class="col-6">
                <q-input
                  dark
                  filled
                  v-model="form.fecha"
                  label="Fecha"
                  type="date"
                  :rules="[(v) => !!v || 'Requerido']"
                />
              </div>
              <div class="col-6">
                <q-select
                  dark
                  filled
                  v-model="form.categoria"
                  :options="categoriaOptions"
                  label="Categoría"
                  :rules="[(v) => !!v || 'Requerido']"
                />
              </div>
            </div>
            <q-input
              dark
              filled
              v-model="form.descripcion"
              label="Descripción"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <q-input
              dark
              filled
              :model-value="amountStr"
              @update:model-value="onAmountInput"
              @blur="formatAmount"
              label="Monto"
              type="text"
              inputmode="decimal"
              prefix="$"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <q-select
              dark
              filled
              v-model="form.metodoPago"
              :options="metodoPagoOptions"
              label="Método de pago"
              clearable
            />
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
          <span
            >Eliminar gasto <strong>{{ deletingItem?.descripcion }}</strong
            >?</span
          >
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
.deprecation-banner {
  background: rgba(200, 150, 62, 0.06);
  border: 1px solid rgba(200, 150, 62, 0.3);
  border-radius: 6px;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.cat-group {
  margin-bottom: 20px;
}

.cat-group:last-child {
  margin-bottom: 0;
}

.cat-group__header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(27, 38, 36, 0.3);
  border-radius: 6px;
  margin-bottom: 4px;
}

.cat-group__title {
  font-size: 0.85rem;
  font-weight: 600;
}

.cat-group__count {
  font-size: 0.72rem;
  color: rgba(163, 120, 94, 0.45);
}

.cat-group__total {
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 0.9rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: rgba(163, 120, 94, 0.7);
}

.expense-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(113, 131, 127, 0.04);
}

.expense-row:hover {
  background: rgba(27, 38, 36, 0.3);
}

.expense-row__desc {
  font-size: 0.85rem;
}

.expense-row__meta {
  font-size: 0.75rem;
  color: rgba(163, 120, 94, 0.5);
  margin-top: 2px;
}

.expense-row__sep {
  margin: 0 4px;
}
</style>
