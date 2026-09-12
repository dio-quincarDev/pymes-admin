<script setup lang="ts">
import { ref, shallowRef, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { costoService } from '../services/costo.service';
import { useCostos } from '../composables/useCostos';
import type { Collaborador, CollaboradorRequest, GastoFijoRecurrente, GastoFijoRequest } from '../types';
import CostSummaryBar from '../components/costos/CostSummaryBar.vue';
import CollaboratorList from '../components/costos/CollaboratorList.vue';
import GastoFijoGroupedList from '../components/costos/GastoFijoGroupedList.vue';

useMeta({ title: 'Costos — PYMEQ' });

const $q = useQuasar();
const route = useRoute();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId as string;

// composable is single source of truth — page is composition surface (sfc.md + composables.md)
const {
  colaboradores, catGroups, loading, diario, configForm, configSaving,
  proveedorOptions, costoFijoDiario, costoSalariosDiario, costoPrestamosDiario,
  costoOperativoDiario, gananciaPositiva, loadAll, saveConfig, createProveedor,
  upsertLocalColaborador, removeLocalColaborador, upsertLocalGasto, removeLocalGasto,
} = useCostos();

const tab = shallowRef<'colaboradores' | 'gastosFijos'>('colaboradores');
const categoriaOptions = ['SALARIOS','AGUA','LUZ','GAS','INTERNET','ALQUILER','MANTENIMIENTO','PUBLICIDAD','OTROS'];
const tipoPagoOptions = ['DIARIO','SEMANAL','QUINCENAL','MENSUAL'];
const metodoPagoOptions = ['EFECTIVO','TRANSFERENCIA','TARJETA','CHEQUE'];

// dialogs — shallowRef per reactivity.md primitive
const colabDialog = shallowRef(false);
const gastoDialog = shallowRef(false);
const newProveedorDialog = shallowRef(false);
const colabDelete = shallowRef(false);
const gastoDelete = shallowRef(false);
const editingColabId = shallowRef<string | null>(null);
const editingGastoId = shallowRef<string | null>(null);
const saving = shallowRef(false);
const deleting = shallowRef(false);
const savingNewProveedor = shallowRef(false);
const deletingColab = ref<Collaborador | null>(null);
const deletingGasto = ref<GastoFijoRecurrente | null>(null);
const newProveedorName = ref('');
const colabFormRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const gastoFormRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const colabForm = ref<CollaboradorRequest>({ tenantId, nombre: '', tipoPago: 'MENSUAL', monto: 0 });
const gastoForm = ref<GastoFijoRequest>({ tenantId, categoria: '', monto: 0, descripcion: '', diaEjecucion: 1, metodoPago: null, proveedorId: null });
const amounts = { amountStr: ref(''), amountStrGasto: ref('') };

function onAmountInput(field: 'amountStr' | 'amountStrGasto', val: string | number | null) {
  amounts[field].value = String(val ?? '').replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1');
}
function formatAmount(target: { monto: number }, field: 'amountStr' | 'amountStrGasto') {
  const n = parseFloat(amounts[field].value.replace(/,/g, ''));
  if (!isNaN(n) && amounts[field].value) {
    amounts[field].value = n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    target.monto = n;
  }
}
function rawAmount(val: number) { return val ? val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : ''; }

function openCreateColab() {
  editingColabId.value = null;
  colabForm.value = { tenantId, nombre: '', tipoPago: 'MENSUAL', monto: 0 };
  amounts.amountStr.value = '';
  colabDialog.value = true;
}
function openEditColab(c: Collaborador) {
  editingColabId.value = c.id;
  colabForm.value = { tenantId: c.tenantId, nombre: c.nombre, tipoPago: c.tipoPago, monto: c.monto };
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
      upsertLocalColaborador(res.data, editingColabId.value);
    } else {
      const res = await costoService.createCollaborador(colabForm.value);
      upsertLocalColaborador(res.data, null);
    }
    colabDialog.value = false;
    $q.notify({ type: 'positive', message: `Integrante ${editingColabId.value ? 'actualizado' : 'creado'}` });
    void loadAll();
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al guardar colaborador' }); }
  finally { saving.value = false; }
}
async function removeColab() {
  if (!deletingColab.value) return;
  deleting.value = true;
  try {
    await costoService.removeCollaborador(deletingColab.value.id, tenantId);
    removeLocalColaborador(deletingColab.value.id);
    colabDelete.value = false;
    $q.notify({ type: 'positive', message: 'Integrante eliminado' });
    void loadAll();
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al eliminar colaborador' }); }
  finally { deleting.value = false; deletingColab.value = null; }
}

function openNewProveedor() { newProveedorName.value = ''; newProveedorDialog.value = true; }
async function saveNewProveedor() {
  savingNewProveedor.value = true;
  try {
    const p = await createProveedor(newProveedorName.value);
    if (p) gastoForm.value.proveedorId = p.id;
    newProveedorDialog.value = false;
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al crear proveedor' }); }
  finally { savingNewProveedor.value = false; }
}
function openCreateGasto() {
  editingGastoId.value = null;
  gastoForm.value = { tenantId, categoria: '', monto: 0, descripcion: '', diaEjecucion: 1, metodoPago: null, proveedorId: null };
  amounts.amountStrGasto.value = '';
  gastoDialog.value = true;
}
function openEditGasto(g: GastoFijoRecurrente) {
  editingGastoId.value = g.id;
  gastoForm.value = { tenantId: g.tenantId, categoria: g.categoria, monto: g.monto, descripcion: g.descripcion, diaEjecucion: g.diaEjecucion, metodoPago: g.metodoPago, proveedorId: g.proveedorId };
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
      upsertLocalGasto(res.data, editingGastoId.value);
    } else {
      const res = await costoService.createGastoFijo(gastoForm.value);
      upsertLocalGasto(res.data, null);
    }
    gastoDialog.value = false;
    $q.notify({ type: 'positive', message: `Gasto fijo ${editingGastoId.value ? 'actualizado' : 'creado'}` });
    void loadAll();
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al guardar gasto fijo' }); }
  finally { saving.value = false; }
}
async function removeGasto() {
  if (!deletingGasto.value) return;
  deleting.value = true;
  try {
    await costoService.removeGastoFijo(deletingGasto.value.id, tenantId);
    removeLocalGasto(deletingGasto.value.id);
    gastoDelete.value = false;
    $q.notify({ type: 'positive', message: 'Gasto fijo eliminado' });
    void loadAll();
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al eliminar gasto fijo' }); }
  finally { deleting.value = false; deletingGasto.value = null; }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault();
    if (tab.value === 'colaboradores') openCreateColab(); else openCreateGasto();
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 's' && (colabDialog.value || gastoDialog.value)) {
    e.preventDefault();
    if (colabDialog.value) void saveColab(); else void saveGasto();
  }
}

onMounted(() => {
  const t = route.query.tab;
  if (t === 'gastosFijos' || t === 'colaboradores') tab.value = t;
  void loadAll();
  window.addEventListener('keydown', handleKeydown);
});
onUnmounted(() => window.removeEventListener('keydown', handleKeydown));
</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-md">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Estructura de Costos</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Equipo, gastos fijos recurrentes y configuración laboral</p>
    </div>

    <CostSummaryBar
      :diario="diario"
      :costo-fijo-diario="costoFijoDiario"
      :costo-salarios-diario="costoSalariosDiario"
      :costo-prestamos-diario="costoPrestamosDiario"
      :costo-operativo-diario="costoOperativoDiario"
      :ganancia-positiva="gananciaPositiva"
      :config-form="configForm"
      :config-saving="configSaving"
      @update:config="(v) => { configForm.diasLaborales = v; }"
      @save-config="saveConfig"
    />

    <q-tabs v-model="tab" dark align="left" narrow-indicator active-color="primary" class="cost-tabs q-mb-md">
      <q-tab name="colaboradores" label="Equipo" icon="sym_r_groups" />
      <q-tab name="gastosFijos" label="Gastos Fijos" icon="sym_r_receipt" />
    </q-tabs>

    <div v-show="tab === 'colaboradores'">
      <CollaboratorList :items="colaboradores" :loading="loading" @create="openCreateColab" @edit="openEditColab" @delete="(c) => { deletingColab = c; colabDelete = true; }" />
    </div>
    <div v-show="tab === 'gastosFijos'">
      <GastoFijoGroupedList :groups="catGroups" :loading="loading" @create="openCreateGasto" @edit="openEditGasto" @delete="(g) => { deletingGasto = g; gastoDelete = true; }" />
    </div>

    <!-- dialogs — kept thin, emit up via page actions (component-data-flow.md) -->
    <q-dialog v-model="colabDialog" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section><div class="text-h6 text-primary">{{ editingColabId ? 'Editar' : 'Nuevo' }} Integrante</div></q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="colabFormRef" @submit.prevent="saveColab" class="q-gutter-y-md">
            <q-input dark filled v-model="colabForm.nombre" label="Nombre" :rules="[(v) => !!v || 'Requerido']" />
            <q-select dark filled v-model="colabForm.tipoPago" :options="tipoPagoOptions" label="Frecuencia de pago" :rules="[(v) => !!v || 'Requerido']" />
            <q-input dark filled :model-value="amounts.amountStr.value" @update:model-value="(val) => onAmountInput('amountStr', val)" @blur="formatAmount(colabForm, 'amountStr')" label="Monto" type="text" inputmode="decimal" prefix="$" :rules="[(v) => !!v || 'Requerido']" />
            <div class="row justify-end q-gutter-x-sm"><q-btn flat label="Cancelar" color="accent" v-close-popup /><q-btn type="submit" label="Guardar" color="primary" :loading="saving" /></div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="gastoDialog" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section><div class="text-h6 text-primary">{{ editingGastoId ? 'Editar' : 'Nuevo' }} Gasto Fijo</div></q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="gastoFormRef" @submit.prevent="saveGasto" class="q-gutter-y-md">
            <div class="row q-col-gutter-md">
              <div class="col-6"><q-select dark filled v-model="gastoForm.categoria" :options="categoriaOptions" label="Categoría" :rules="[(v) => !!v || 'Requerido']" /></div>
              <div class="col-6"><q-input dark filled type="number" min="1" max="31" v-model.number="gastoForm.diaEjecucion" label="Día de ejecución" :rules="[(v) => (v >= 1 && v <= 31) || 'Entre 1 y 31']" /></div>
            </div>
            <q-input dark filled v-model="gastoForm.descripcion" label="Descripción" :rules="[(v) => !!v || 'Requerido']" />
            <q-input dark filled :model-value="amounts.amountStrGasto.value" @update:model-value="(val) => onAmountInput('amountStrGasto', val)" @blur="formatAmount(gastoForm, 'amountStrGasto')" label="Monto" type="text" inputmode="decimal" prefix="$" :rules="[(v) => !!v || 'Requerido']" />
            <q-select dark filled v-model="gastoForm.metodoPago" :options="metodoPagoOptions" label="Método de pago" clearable />
            <div class="row items-center q-gutter-x-sm">
              <q-select dark filled v-model="gastoForm.proveedorId" :options="proveedorOptions" label="Proveedor" clearable map-options emit-value class="col" />
              <q-btn flat round icon="sym_r_add" color="primary" type="button" @click="openNewProveedor" />
            </div>
            <div class="row justify-end q-gutter-x-sm"><q-btn flat label="Cancelar" color="accent" v-close-popup /><q-btn type="submit" label="Guardar" color="primary" :loading="saving" /></div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="newProveedorDialog" dark>
      <q-card dark class="bg-surface-pine" style="min-width: 320px">
        <q-card-section><div class="text-h6 text-primary">Nuevo Proveedor</div></q-card-section>
        <q-card-section><q-input dark filled v-model="newProveedorName" label="Nombre" autofocus :rules="[(v) => !!v || 'Requerido']" @keyup.enter="saveNewProveedor" /></q-card-section>
        <q-card-actions align="right"><q-btn flat label="Cancelar" color="accent" v-close-popup /><q-btn label="Crear" color="primary" :loading="savingNewProveedor" @click="saveNewProveedor" /></q-card-actions>
      </q-card>
    </q-dialog>

    <q-dialog v-model="colabDelete" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md"><q-icon name="warning" color="negative" size="md" /><span>Eliminar integrante <strong>{{ deletingColab?.nombre }}</strong>?</span></q-card-section>
        <q-card-actions align="right"><q-btn flat label="Cancelar" color="accent" v-close-popup /><q-btn label="Eliminar" color="negative" :loading="deleting" @click="removeColab" /></q-card-actions>
      </q-card>
    </q-dialog>
    <q-dialog v-model="gastoDelete" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md"><q-icon name="warning" color="negative" size="md" /><span>Eliminar gasto fijo <strong>{{ deletingGasto?.descripcion || deletingGasto?.categoria }}</strong>?</span></q-card-section>
        <q-card-actions align="right"><q-btn flat label="Cancelar" color="accent" v-close-popup /><q-btn label="Eliminar" color="negative" :loading="deleting" @click="removeGasto" /></q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<style scoped lang="scss">
.cost-tabs { border-bottom: 1px solid rgba(113,131,127,0.12); overflow-x: auto; -webkit-overflow-scrolling: touch; }
.cost-tabs :deep(.q-tabs__content) { flex-wrap: nowrap; }
</style>
