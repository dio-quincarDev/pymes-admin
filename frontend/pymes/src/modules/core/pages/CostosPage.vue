<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { formatCurrency } from 'src/utils/format';
import { costoService } from '../services/costo.service';
import { proveedorService } from '../services/proveedor.service';
import type {
  Collaborador,
  CollaboradorRequest,
  ConfigLaboral,
  CostoDiario,
  GastoFijoRecurrente,
  GastoFijoRequest,
  Proveedor,
} from '../types';
import EmptyState from 'src/components/ui/EmptyState.vue';

useMeta({ title: 'Costos — PYMEQ' });

const $q = useQuasar();
const route = useRoute();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;

const categoriaOptions = [
  'SALARIOS',
  'AGUA',
  'LUZ',
  'GAS',
  'INTERNET',
  'ALQUILER',
  'MANTENIMIENTO',
  'PUBLICIDAD',
  'OTROS',
];
const tipoPagoOptions = ['DIARIO', 'SEMANAL', 'QUINCENAL', 'MENSUAL'];
const metodoPagoOptions = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'CHEQUE'];

const tab = shallowRef<'colaboradores' | 'gastosFijos' | 'configuracion'>('colaboradores');
const loading = shallowRef(false);

const colaboradores = ref<Collaborador[]>([]);
const gastosFijos = ref<GastoFijoRecurrente[]>([]);
const proveedores = ref<Proveedor[]>([]);
const diario = ref<CostoDiario | null>(null);
const configuracion = ref<ConfigLaboral | null>(null);

const proveedorOptions = computed(() =>
  proveedores.value.map((p) => ({ label: p.name, value: p.id })),
);

async function loadDiario() {
  if (!tenantId) return;
  try {
    const res = await costoService.getDiario(tenantId);
    diario.value = res.data;
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al calcular costo diario',
    });
  }
}

async function loadAll() {
  if (!tenantId) return;
  loading.value = true;
  try {
    const [colabRes, gastosRes, configRes, provRes] = await Promise.all([
      costoService.getAllCollaboradores(tenantId),
      costoService.getAllGastosFijos(tenantId),
      costoService.getConfiguracion(tenantId),
      proveedorService.getAll(tenantId),
    ]);
    colaboradores.value = colabRes.data;
    gastosFijos.value = gastosRes.data;
    proveedores.value = provRes.data;
    configuracion.value = configRes.data;
    configForm.value = { tenantId, diasLaborales: configRes.data.diasLaborales };
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar costos',
    });
  } finally {
    loading.value = false;
  }
  void loadDiario();
}

function refresh() {
  void loadAll();
}

interface CatGroup {
  categoria: string;
  items: GastoFijoRecurrente[];
  total: number;
}

const catGroups = computed(() => {
  const groups = new Map<string, GastoFijoRecurrente[]>();
  for (const g of gastosFijos.value) {
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

const amounts = {
  amountStr: ref(''),
  amountStrGasto: ref(''),
};

function onAmountInput(field: 'amountStr' | 'amountStrGasto', val: string | number | null) {
  amounts[field].value = String(val ?? '')
    .replace(/[^0-9.]/g, '')
    .replace(/(\..*)\./g, '$1');
}

function formatAmount(target: { monto: number }, field: 'amountStr' | 'amountStrGasto') {
  const n = parseFloat(amounts[field].value.replace(/,/g, ''));
  if (!isNaN(n) && amounts[field].value) {
    amounts[field].value = n.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    target.monto = n;
  }
}

function rawAmount(val: number) {
  return val
    ? val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '';
}

// --- Collaboradores CRUD ---
const colabDialog = shallowRef(false);
const editingColabId = shallowRef<string | null>(null);
const saving = shallowRef(false);
const colabFormRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const colabForm = ref<CollaboradorRequest>({
  tenantId: tenantId as string,
  nombre: '',
  tipoPago: 'MENSUAL',
  monto: 0,
});

function openCreateColab() {
  editingColabId.value = null;
  colabForm.value = { tenantId: tenantId as string, nombre: '', tipoPago: 'MENSUAL', monto: 0 };
  amounts.amountStr.value = '';
  colabDialog.value = true;
}

function openEditColab(c: Collaborador) {
  editingColabId.value = c.id;
  colabForm.value = {
    tenantId: c.tenantId,
    nombre: c.nombre,
    tipoPago: c.tipoPago,
    monto: c.monto,
  };
  amounts.amountStr.value = rawAmount(c.monto);
  colabDialog.value = true;
}

async function saveColab() {
  formatAmount(colabForm.value, 'amountStr');
  if (!(await colabFormRef.value?.validate())) return;
  saving.value = true;
  try {
    if (editingColabId.value) {
      const res = await costoService.updateCollaborador(editingColabId.value, colabForm.value);
      const idx = colaboradores.value.findIndex((r) => r.id === editingColabId.value);
      if (idx >= 0) colaboradores.value[idx] = res.data;
    } else {
      const res = await costoService.createCollaborador(colabForm.value);
      colaboradores.value.unshift(res.data);
    }
    colabDialog.value = false;
    $q.notify({ type: 'positive', message: `Colaborador ${editingColabId.value ? 'actualizado' : 'creado'}` });
    refresh();
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al guardar colaborador',
    });
  } finally {
    saving.value = false;
  }
}

const colabDelete = shallowRef(false);
const deletingColab = shallowRef<Collaborador | null>(null);
const deleting = shallowRef(false);

async function removeColab() {
  if (!deletingColab.value || !tenantId) return;
  deleting.value = true;
  try {
    await costoService.removeCollaborador(deletingColab.value.id, tenantId);
    colaboradores.value = colaboradores.value.filter((r) => r.id !== deletingColab.value!.id);
    colabDelete.value = false;
    $q.notify({ type: 'positive', message: 'Colaborador eliminado' });
    refresh();
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al eliminar colaborador',
    });
  } finally {
    deleting.value = false;
    deletingColab.value = null;
  }
}

// --- Gastos Fijos CRUD ---
const gastoDialog = shallowRef(false);
const editingGastoId = shallowRef<string | null>(null);
const gastoFormRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const gastoForm = ref<GastoFijoRequest>({
  tenantId: tenantId as string,
  categoria: '',
  monto: 0,
  descripcion: '',
  diaEjecucion: 1,
  metodoPago: null,
  proveedorId: null,
});

const newProveedorDialog = shallowRef(false);
const newProveedorName = ref('');
const savingNewProveedor = shallowRef(false);

function openNewProveedor() {
  newProveedorName.value = '';
  newProveedorDialog.value = true;
}

async function saveNewProveedor() {
  if (!newProveedorName.value.trim() || !tenantId) return;
  savingNewProveedor.value = true;
  try {
    const res = await proveedorService.create({ tenantId, name: newProveedorName.value.trim() });
    proveedores.value = [...proveedores.value, res.data];
    gastoForm.value.proveedorId = res.data.id;
    newProveedorDialog.value = false;
    $q.notify({ type: 'positive', message: `Proveedor "${res.data.name}" creado` });
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al crear proveedor' });
  } finally {
    savingNewProveedor.value = false;
  }
}

function openCreateGasto() {
  editingGastoId.value = null;
  gastoForm.value = {
    tenantId: tenantId as string,
    categoria: '',
    monto: 0,
    descripcion: '',
    diaEjecucion: 1,
    metodoPago: null,
    proveedorId: null,
  };
  amounts.amountStrGasto.value = '';
  gastoDialog.value = true;
}

function openEditGasto(g: GastoFijoRecurrente) {
  editingGastoId.value = g.id;
  gastoForm.value = {
    tenantId: g.tenantId,
    categoria: g.categoria,
    monto: g.monto,
    descripcion: g.descripcion,
    diaEjecucion: g.diaEjecucion,
    metodoPago: g.metodoPago,
    proveedorId: g.proveedorId,
  };
  amounts.amountStrGasto.value = rawAmount(g.monto);
  gastoDialog.value = true;
}

async function saveGasto() {
  formatAmount(gastoForm.value, 'amountStrGasto');
  if (!(await gastoFormRef.value?.validate())) return;
  saving.value = true;
  try {
    if (editingGastoId.value) {
      const res = await costoService.updateGastoFijo(editingGastoId.value, gastoForm.value);
      const idx = gastosFijos.value.findIndex((r) => r.id === editingGastoId.value);
      if (idx >= 0) gastosFijos.value[idx] = res.data;
    } else {
      const res = await costoService.createGastoFijo(gastoForm.value);
      gastosFijos.value.unshift(res.data);
    }
    gastoDialog.value = false;
    $q.notify({ type: 'positive', message: `Gasto fijo ${editingGastoId.value ? 'actualizado' : 'creado'}` });
    refresh();
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al guardar gasto fijo',
    });
  } finally {
    saving.value = false;
  }
}

const gastoDelete = shallowRef(false);
const deletingGasto = shallowRef<GastoFijoRecurrente | null>(null);

async function removeGasto() {
  if (!deletingGasto.value || !tenantId) return;
  deleting.value = true;
  try {
    await costoService.removeGastoFijo(deletingGasto.value.id, tenantId);
    gastosFijos.value = gastosFijos.value.filter((r) => r.id !== deletingGasto.value!.id);
    gastoDelete.value = false;
    $q.notify({ type: 'positive', message: 'Gasto fijo eliminado' });
    refresh();
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al eliminar gasto fijo',
    });
  } finally {
    deleting.value = false;
    deletingGasto.value = null;
  }
}

// --- Configuración ---
const configSaving = shallowRef(false);
const configForm = ref<ConfigLaboral>({ tenantId: tenantId as string, diasLaborales: 26 });

async function saveConfig() {
  if (!tenantId) return;
  const dias = Number(configForm.value.diasLaborales);
  if (!Number.isInteger(dias) || dias < 1 || dias > 31) {
    $q.notify({ type: 'negative', message: 'Los días laborales deben estar entre 1 y 31' });
    return;
  }
  configSaving.value = true;
  try {
    const res = await costoService.updateConfiguracion(tenantId, { diasLaborales: dias });
    configuracion.value = res.data;
    configForm.value = res.data;
    $q.notify({ type: 'positive', message: 'Configuración actualizada' });
    refresh();
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al guardar configuración',
    });
  } finally {
    configSaving.value = false;
  }
}

const gananciaPositiva = computed(() => (diario.value?.gananciaRealEstimada ?? 0) >= 0);

onMounted(() => {
  const t = route.query.tab;
  if (t === 'gastosFijos' || t === 'configuracion' || t === 'colaboradores') tab.value = t;
  if (!tenantId) return;
  void loadAll();
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => window.removeEventListener('keydown', handleKeydown));

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault();
    if (tab.value === 'colaboradores') openCreateColab();
    else if (tab.value === 'gastosFijos') openCreateGasto();
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 's' && (colabDialog.value || gastoDialog.value)) {
    e.preventDefault();
    if (colabDialog.value) void saveColab();
    else void saveGasto();
  }
}
</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-md">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Estructura de Costos</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">
        Colaboradores, gastos fijos recurrentes y configuración laboral
      </p>
    </div>

    <q-card dark class="glass cost-summary q-mb-md" role="status" aria-live="polite">
      <div class="row items-center q-gutter-x-md q-pa-sm wrap">
        <div class="col-auto summary-item">
          <span class="summary-label">Costo / día</span>
          <span class="summary-value font-mono">{{
            diario ? formatCurrency(diario.costoOperativoDiario) : '—'
          }}</span>
        </div>
        <div class="col-auto summary-arrow" aria-hidden="true">
          <q-icon name="arrow_forward" />
        </div>
        <div class="col-auto summary-item">
          <span class="summary-label">Ventas hoy</span>
          <span class="summary-value font-mono">{{
            diario ? formatCurrency(diario.ventasHoy) : '—'
          }}</span>
        </div>
        <div class="col-auto summary-arrow" aria-hidden="true">
          <q-icon name="arrow_forward" />
        </div>
        <div class="col-auto summary-item">
          <span class="summary-label">Ganancia real estimada</span>
          <span
            class="summary-value font-mono"
            :class="gananciaPositiva ? 'text-positive' : 'text-negative'"
            >{{ diario ? formatCurrency(diario.gananciaRealEstimada) : '—' }}</span
          >
        </div>
      </div>
    </q-card>

    <q-tabs
      v-model="tab"
      dark
      align="left"
      narrow-indicator
      active-color="primary"
      class="cost-tabs q-mb-md"
    >
      <q-tab name="colaboradores" label="Colaboradores" icon="groups" />
      <q-tab name="gastosFijos" label="Gastos Fijos" icon="receipt" />
      <q-tab name="configuracion" label="Configuración" icon="settings" />
    </q-tabs>

    <!-- Colaboradores -->
    <div v-show="tab === 'colaboradores'">
      <div class="toolbar">
        <q-space />
        <q-btn v-if="colaboradores.length" color="primary" icon="add" label="Nuevo" @click="openCreateColab" />
      </div>

      <div v-if="!loading && !colaboradores.length" class="q-mt-lg">
        <EmptyState
          icon="groups"
          title="Sin colaboradores"
          message="Registra a tu equipo para calcular el costo de salarios."
        >
          <q-btn color="primary" icon="add" label="Nuevo Colaborador" @click="openCreateColab" class="q-mt-sm" />
        </EmptyState>
      </div>

      <div v-if="loading" class="q-gutter-y-md q-mt-md">
        <div v-for="n in 3" :key="n">
          <q-skeleton type="rect" dark animation="pulse" height="72px" />
        </div>
      </div>

      <div v-for="c in colaboradores" :key="c.id" class="row-item">
        <div class="row-item__main">
          <div class="row-item__title">{{ c.nombre }}</div>
          <div class="row-item__meta">
            <span>{{ c.tipoPago }}</span>
          </div>
        </div>
        <div class="row-item__amount">{{ formatCurrency(c.monto) }}</div>
        <div class="row-item__actions">
          <q-btn flat dense round icon="edit" color="primary" size="sm" aria-label="Editar" @click="openEditColab(c)" />
          <q-btn flat dense round icon="delete" color="negative" size="sm" aria-label="Eliminar" @click="colabDelete = true; deletingColab = c" />
        </div>
      </div>
    </div>

    <!-- Gastos Fijos -->
    <div v-show="tab === 'gastosFijos'">
      <div class="toolbar">
        <q-space />
        <q-btn v-if="gastosFijos.length" color="primary" icon="add" label="Nuevo" @click="openCreateGasto" />
      </div>

      <div v-if="!loading && !gastosFijos.length" class="q-mt-lg">
        <EmptyState
          icon="receipt"
          title="Sin gastos fijos"
          message="Agrega alquiler, internet, luz y otros gastos recurrentes."
        >
          <q-btn color="primary" icon="add" label="Nuevo Gasto Fijo" @click="openCreateGasto" class="q-mt-sm" />
        </EmptyState>
      </div>

      <div v-if="loading" class="q-gutter-y-md q-mt-md">
        <div v-for="n in 3" :key="n">
          <q-skeleton type="rect" dark animation="pulse" height="72px" />
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

        <div v-for="g in group.items" :key="g.id" class="row-item">
          <div class="row-item__main">
            <div class="row-item__title">{{ g.descripcion || g.categoria }}</div>
            <div class="row-item__meta">
              <span>Día {{ g.diaEjecucion }}</span>
              <span v-if="g.metodoPago" class="row-item__sep">·</span>
              <span v-if="g.metodoPago">{{ g.metodoPago }}</span>
              <span v-if="g.proveedorName" class="row-item__sep">·</span>
              <span v-if="g.proveedorName">{{ g.proveedorName }}</span>
            </div>
          </div>
          <div class="row-item__amount">{{ formatCurrency(g.monto) }}</div>
          <div class="row-item__actions">
            <q-btn flat dense round icon="edit" color="primary" size="sm" aria-label="Editar" @click="openEditGasto(g)" />
            <q-btn flat dense round icon="delete" color="negative" size="sm" aria-label="Eliminar" @click="gastoDelete = true; deletingGasto = g" />
          </div>
        </div>
      </div>
    </div>

    <!-- Configuración -->
    <div v-show="tab === 'configuracion'" class="config-panel">
      <q-card dark class="bg-surface-pine" style="max-width: 420px">
        <q-card-section>
          <div class="text-h6 text-primary">Configuración Laboral</div>
          <p class="text-subtitle2 text-accent q-mt-xs q-mb-md">
            Días laborables al mes. Se usan para convertir el costo mensual en costo diario.
          </p>
          <q-input
            dark
            filled
            type="number"
            min="1"
            max="31"
            v-model.number="configForm.diasLaborales"
            label="Días laborales"
            :rules="[(v) => (v >= 1 && v <= 31) || 'Entre 1 y 31']"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn label="Guardar" color="primary" :loading="configSaving" @click="saveConfig" />
        </q-card-actions>
      </q-card>
    </div>

    <!-- Collaborador dialog -->
    <q-dialog v-model="colabDialog" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section
          ><div class="text-h6 text-primary">
            {{ editingColabId ? 'Editar' : 'Nuevo' }} Colaborador
          </div></q-card-section
        >
        <q-separator dark />
        <q-card-section>
          <q-form ref="colabFormRef" @submit.prevent="saveColab" class="q-gutter-y-md">
            <q-input
              dark
              filled
              v-model="colabForm.nombre"
              label="Nombre"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <q-select
              dark
              filled
              v-model="colabForm.tipoPago"
              :options="tipoPagoOptions"
              label="Frecuencia de pago"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <q-input
              dark
              filled
              :model-value="amounts.amountStr.value"
              @update:model-value="(val) => onAmountInput('amountStr', val)"
              @blur="formatAmount(colabForm, 'amountStr')"
              label="Monto"
              type="text"
              inputmode="decimal"
              prefix="$"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Gasto fijo dialog -->
    <q-dialog v-model="gastoDialog" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section
          ><div class="text-h6 text-primary">
            {{ editingGastoId ? 'Editar' : 'Nuevo' }} Gasto Fijo
          </div></q-card-section
        >
        <q-separator dark />
        <q-card-section>
          <q-form ref="gastoFormRef" @submit.prevent="saveGasto" class="q-gutter-y-md">
            <div class="row q-col-gutter-md">
              <div class="col-6">
                <q-select
                  dark
                  filled
                  v-model="gastoForm.categoria"
                  :options="categoriaOptions"
                  label="Categoría"
                  :rules="[(v) => !!v || 'Requerido']"
                />
              </div>
              <div class="col-6">
                <q-input
                  dark
                  filled
                  type="number"
                  min="1"
                  max="31"
                  v-model.number="gastoForm.diaEjecucion"
                  label="Día de ejecución"
                  :rules="[(v) => (v >= 1 && v <= 31) || 'Entre 1 y 31']"
                />
              </div>
            </div>
            <q-input
              dark
              filled
              v-model="gastoForm.descripcion"
              label="Descripción"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <q-input
              dark
              filled
              :model-value="amounts.amountStrGasto.value"
              @update:model-value="(val) => onAmountInput('amountStrGasto', val)"
              @blur="formatAmount(gastoForm, 'amountStrGasto')"
              label="Monto"
              type="text"
              inputmode="decimal"
              prefix="$"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <q-select
              dark
              filled
              v-model="gastoForm.metodoPago"
              :options="metodoPagoOptions"
              label="Método de pago"
              clearable
            />
            <div class="row items-center q-gutter-x-sm">
              <q-select
                dark
                filled
                v-model="gastoForm.proveedorId"
                :options="proveedorOptions"
                label="Proveedor"
                clearable
                map-options
                emit-value
                class="col"
              />
              <q-btn flat round icon="add" color="primary" type="button" @click="openNewProveedor" />
            </div>
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- New proveedor dialog -->
    <q-dialog v-model="newProveedorDialog" dark>
      <q-card dark class="bg-surface-pine" style="min-width: 320px">
        <q-card-section>
          <div class="text-h6 text-primary">Nuevo Proveedor</div>
        </q-card-section>
        <q-card-section>
          <q-input
            dark
            filled
            v-model="newProveedorName"
            label="Nombre"
            autofocus
            :rules="[(v) => !!v || 'Requerido']"
            @keyup.enter="saveNewProveedor"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Crear" color="primary" :loading="savingNewProveedor" @click="saveNewProveedor" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Delete confirm dialogs -->
    <q-dialog v-model="colabDelete" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span>Eliminar colaborador <strong>{{ deletingColab?.nombre }}</strong>?</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Eliminar" color="negative" :loading="deleting" @click="removeColab" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <q-dialog v-model="gastoDelete" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span
            >Eliminar gasto fijo <strong>{{ deletingGasto?.descripcion || deletingGasto?.categoria }}</strong
            >?</span
          >
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Eliminar" color="negative" :loading="deleting" @click="removeGasto" />
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

.cost-summary {
  position: sticky;
  top: 0;
  z-index: 10;
  border-radius: 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 8px;
}

.summary-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: rgba(163, 120, 94, 0.55);
}

.summary-value {
  font-size: 1.1rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.summary-arrow {
  color: rgba(163, 120, 94, 0.35);
}

.cost-tabs {
  border-bottom: 1px solid rgba(113, 131, 127, 0.12);
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

.row-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(113, 131, 127, 0.04);
}

.row-item:hover {
  background: rgba(27, 38, 36, 0.3);
}

.row-item__title {
  font-size: 0.85rem;
}

.row-item__meta {
  font-size: 0.75rem;
  color: rgba(163, 120, 94, 0.5);
  margin-top: 2px;
}

.row-item__sep {
  margin: 0 4px;
}

.row-item__amount {
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 0.9rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.config-panel {
  padding-top: 8px;
}
</style>
