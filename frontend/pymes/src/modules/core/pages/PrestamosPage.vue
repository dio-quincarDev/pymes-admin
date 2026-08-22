<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { formatCurrency } from 'src/utils/format';
import { prestamoService } from '../services/prestamo.service';
import { cuotaMensual, totalConInteres } from '../utils/prestamo';
import type { Prestamo, PrestamoRequest, PagoPrestamo, PagoPrestamoRequest } from '../types';
import EmptyState from 'src/components/ui/EmptyState.vue';

useMeta({ title: 'Préstamos — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;
const isOwner = computed(() => authStore.user?.role === 'OWNER');

const rows = ref<Prestamo[]>([]);
const loading = shallowRef(false);

const statusColor = (s: string) => (s === 'ACTIVO' ? 'positive' : s === 'PAGADO' ? 'info' : 'grey');

async function load() {
  if (!tenantId) return;
  loading.value = true;
  try {
    const res = await prestamoService.getAll(tenantId);
    rows.value = res.data;
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar préstamos',
    });
  } finally {
    loading.value = false;
  }
}

const dialogOpen = shallowRef(false);
const editingId = shallowRef<string | null>(null);
const saving = shallowRef(false);
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const form = ref<PrestamoRequest>({
  tenantId: tenantId as string,
  nombre: '',
  monto: 0,
  tasaInteres: 0,
  plazoMeses: 0,
  fechaInicio: new Date().toISOString().slice(0, 10),
});

function openCreate() {
  editingId.value = null;
  form.value = {
    tenantId: tenantId as string,
    nombre: '',
    monto: 0,
    tasaInteres: 0,
    plazoMeses: 0,
    fechaInicio: new Date().toISOString().slice(0, 10),
  };
  dialogOpen.value = true;
}

function openEdit(p: Prestamo) {
  editingId.value = p.id;
  form.value = {
    tenantId: p.tenantId,
    nombre: p.nombre,
    prestamista: p.prestamista,
    monto: p.monto,
    tasaInteres: p.tasaInteres,
    plazoMeses: p.plazoMeses,
    fechaInicio: p.fechaInicio,
    notas: p.notas,
  };
  dialogOpen.value = true;
}

async function save() {
  if (!(await formRef.value?.validate())) return;
  saving.value = true;
  try {
    if (editingId.value) {
      const res = await prestamoService.update(editingId.value, form.value);
      const idx = rows.value.findIndex((r) => r.id === editingId.value);
      if (idx >= 0) rows.value[idx] = res.data;
    } else {
      const res = await prestamoService.create(form.value);
      rows.value.unshift(res.data);
    }
    dialogOpen.value = false;
    $q.notify({
      type: 'positive',
      message: `Préstamo ${editingId.value ? 'actualizado' : 'creado'}`,
    });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al guardar préstamo',
    });
  } finally {
    saving.value = false;
  }
}

const deleteDialog = shallowRef(false);
const deletingItem = shallowRef<Prestamo | null>(null);
const deleting = shallowRef(false);

function confirmDelete(p: Prestamo) {
  deletingItem.value = p;
  deleteDialog.value = true;
}

async function remove() {
  if (!deletingItem.value || !tenantId) return;
  deleting.value = true;
  try {
    await prestamoService.remove(deletingItem.value.id, tenantId);
    rows.value = rows.value.filter((r) => r.id !== deletingItem.value!.id);
    deleteDialog.value = false;
    $q.notify({ type: 'positive', message: 'Préstamo eliminado' });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al eliminar préstamo',
    });
  } finally {
    deleting.value = false;
    deletingItem.value = null;
  }
}

const pagoDialog = shallowRef(false);
const pagoPrestamo = shallowRef<Prestamo | null>(null);
const pagos = ref<PagoPrestamo[]>([]);
const savingPago = shallowRef(false);
const pagoForm = ref<PagoPrestamoRequest>({
  monto: 0,
  fechaPago: new Date().toISOString().slice(0, 10),
});
const pagosLoaded = shallowRef(false);

function openPagos(p: Prestamo) {
  pagoPrestamo.value = p;
  pagoForm.value = { monto: 0, fechaPago: new Date().toISOString().slice(0, 10) };
  pagosLoaded.value = false;
  pagos.value = [];
  pagoDialog.value = true;
  void loadPagos(p.id);
}

async function loadPagos(loanId: string) {
  if (!tenantId) return;
  try {
    const res = await prestamoService.getPagos(loanId, tenantId);
    pagos.value = res.data;
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar pagos',
    });
  } finally {
    pagosLoaded.value = true;
  }
}

async function savePago() {
  if (!pagoPrestamo.value || !tenantId) return;
  savingPago.value = true;
  try {
    await prestamoService.createPago(pagoPrestamo.value.id, pagoForm.value, tenantId);
    $q.notify({ type: 'positive', message: 'Pago registrado' });
    pagoForm.value = { monto: 0, fechaPago: new Date().toISOString().slice(0, 10) };
    await loadPagos(pagoPrestamo.value.id);
    await load();
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al registrar pago',
    });
  } finally {
    savingPago.value = false;
  }
}

const totalPagado = computed(() => pagos.value.reduce((sum, p) => sum + p.monto, 0));

const totalPrestado = computed(() => rows.value.reduce((s, p) => s + p.monto, 0));
const totalSaldo = computed(() => rows.value.reduce((s, p) => s + p.saldoPendiente, 0));
const totalAPagar = computed(() =>
  rows.value.reduce((s, p) => s + totalConInteres(p.monto, p.tasaInteres, p.plazoMeses), 0),
);

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
    <div class="q-mb-md">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Préstamo</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Gestión de préstamos y pagos</p>
    </div>

    <q-card dark class="glass q-pa-sm q-mb-sm">
      <div class="row q-gutter-x-lg q-pa-xs">
        <div>
          <div
            class="text-caption text-accent text-uppercase"
            style="font-size: 0.72rem; letter-spacing: 0.04em"
          >
            Total prestado
          </div>
          <div class="font-mono text-weight-bold text-h6">{{ formatCurrency(totalPrestado) }}</div>
        </div>
        <div>
          <div
            class="text-caption text-accent text-uppercase"
            style="font-size: 0.72rem; letter-spacing: 0.04em"
          >
            Saldo pendiente
          </div>
          <div class="font-mono text-weight-bold text-h6">{{ formatCurrency(totalSaldo) }}</div>
        </div>
        <div>
          <div
            class="text-caption text-accent text-uppercase"
            style="font-size: 0.72rem; letter-spacing: 0.04em"
          >
            Total a pagar (con interés)
          </div>
          <div class="font-mono text-weight-bold text-h6">{{ formatCurrency(totalAPagar) }}</div>
        </div>
      </div>
    </q-card>

    <div class="toolbar">
      <q-space />
      <q-btn v-if="isOwner" color="primary" icon="sym_r_add" label="Nuevo" @click="openCreate" />
    </div>

    <div v-if="!loading && !rows.length" class="q-mt-lg">
      <EmptyState
        icon="sym_r_account_balance"
        title="Sin préstamos registrados"
        message="Registra un préstamo para hacer seguimiento de tus deudas."
      >
        <q-btn
          v-if="isOwner"
          color="primary"
          icon="sym_r_add"
          label="Nuevo Préstamo"
          @click="openCreate"
          class="q-mt-sm"
        />
      </EmptyState>
    </div>

    <div v-if="loading" class="row q-col-gutter-x-sm q-col-gutter-y-sm q-mt-md">
      <div v-for="n in 3" :key="n" class="col-12 col-sm-6 col-md-4">
        <q-skeleton type="rect" dark animation="pulse" height="160px" />
      </div>
    </div>

    <div v-if="!loading && rows.length" class="loan-grid">
      <div v-for="p in rows" :key="p.id" class="loan-card">
        <div class="loan-card__top">
          <div class="loan-card__name">{{ p.nombre }}</div>
          <q-badge :color="statusColor(p.estado)" class="q-px-sm q-py-xs">{{ p.estado }}</q-badge>
        </div>

        <div class="loan-card__amounts">
          <div class="loan-card__amount">
            <span class="loan-card__amount-label">Monto</span>
            <span class="loan-card__amount-value">{{ formatCurrency(p.monto) }}</span>
          </div>
          <div class="loan-card__amount">
            <span class="loan-card__amount-label">Saldo</span>
            <span class="loan-card__amount-value loan-card__amount-value--balance">{{
              formatCurrency(p.saldoPendiente)
            }}</span>
          </div>
          <div class="loan-card__amount">
            <span class="loan-card__amount-label">Tasa</span>
            <span class="loan-card__amount-value">{{ p.tasaInteres }}%</span>
          </div>
        </div>

        <q-linear-progress
          :value="p.monto > 0 ? (p.monto - p.saldoPendiente) / p.monto : 0"
          :color="p.saldoPendiente === 0 ? 'positive' : 'accent'"
          class="q-mt-sm q-mb-sm"
          size="4px"
          rounded
        />

        <div v-if="p.prestamista" class="loan-card__lender">
          <q-icon name="person" size="0.8rem" class="text-accent" />
          {{ p.prestamista }}
        </div>

        <div v-if="p.plazoMeses" class="loan-card__term">
          {{ p.plazoMeses }} meses · Cuota estimada
          {{ formatCurrency(cuotaMensual(p.monto, p.tasaInteres, p.plazoMeses)) }}/mes
        </div>

        <div class="loan-card__actions">
          <q-btn
            flat
            dense
            round
            icon="sym_r_payments"
            color="positive"
            size="sm"
            @click="openPagos(p)"
            aria-label="Pagos"
          />
          <q-btn
            v-if="isOwner && p.estado === 'ACTIVO'"
            flat
            dense
            round
            icon="sym_r_edit"
            color="primary"
            size="sm"
            @click="openEdit(p)"
            aria-label="Editar"
          />
          <q-btn
            v-if="isOwner"
            flat
            dense
            round
            icon="sym_r_delete"
            color="negative"
            size="sm"
            @click="confirmDelete(p)"
            aria-label="Eliminar"
          />
        </div>
      </div>
    </div>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 520px">
        <q-card-section
          ><div class="text-h6 text-primary">
            {{ editingId ? 'Editar' : 'Nuevo' }} Préstamo
          </div></q-card-section
        >
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <q-input
              dark
              filled
              v-model="form.nombre"
              label="Nombre"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <div class="row q-col-gutter-md">
              <div class="col-6">
                <q-input
                  dark
                  filled
                  v-model.number="form.monto"
                  label="Monto"
                  type="number"
                  min="0"
                  step="0.01"
                  prefix="$"
                  :rules="[(v) => !!v || 'Requerido']"
                />
              </div>
              <div class="col-6">
                <q-input
                  dark
                  filled
                  v-model="form.fechaInicio"
                  label="Fecha inicio"
                  type="date"
                  :rules="[(v) => !!v || 'Requerido']"
                />
              </div>
            </div>
            <div class="row q-col-gutter-md">
              <div class="col-6">
                <q-input
                  dark
                  filled
                  v-model.number="form.tasaInteres"
                  label="Tasa % mensual"
                  type="number"
                  min="0"
                  step="0.01"
                  suffix="%"
                  :rules="[(v) => !!v || 'Requerido']"
                />
              </div>
              <div class="col-6">
                <q-input
                  dark
                  filled
                  v-model.number="form.plazoMeses"
                  label="Plazo (meses)"
                  type="number"
                  min="1"
                  step="1"
                  :rules="[(v) => !!v || 'Requerido']"
                />
              </div>
            </div>
            <q-input dark filled v-model="form.prestamista" label="Prestamista" />
            <q-input dark filled v-model="form.notas" label="Notas" type="textarea" />
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
          <div class="text-h6 text-primary">
            Pagos: <strong>{{ pagoPrestamo?.nombre }}</strong>
          </div>
          <div class="text-caption text-accent q-mt-xs">
            Monto: {{ formatCurrency(pagoPrestamo?.monto || 0) }} | Saldo:
            {{ formatCurrency(pagoPrestamo?.saldoPendiente || 0) }} | Pagado:
            {{ formatCurrency(totalPagado) }}
          </div>
        </q-card-section>
        <q-separator dark />
        <q-card-section class="q-gutter-y-sm">
          <div v-if="!pagos.length && pagosLoaded" class="text-accent text-center q-py-md">
            Sin pagos registrados
          </div>
          <div
            v-for="p in pagos"
            :key="p.id"
            class="row items-center justify-between bg-dark q-px-md q-py-sm"
            style="border-radius: 4px"
          >
            <div>
              <span class="text-weight-bold">{{ formatCurrency(p.monto) }}</span>
              <span class="text-accent text-caption q-ml-sm">{{ p.fechaPago }}</span>
            </div>
            <span class="text-accent text-caption">{{ p.metodoPago || '—' }}</span>
          </div>
        </q-card-section>
        <template v-if="isOwner">
          <q-separator dark />
          <q-card-section>
            <div class="text-subtitle2 text-primary q-mb-sm">Registrar pago</div>
            <div class="row q-col-gutter-sm items-end">
              <div class="col-4">
                <q-input
                  dark
                  dense
                  filled
                  v-model.number="pagoForm.monto"
                  label="Monto"
                  type="number"
                  min="0"
                  step="0.01"
                  prefix="$"
                />
              </div>
              <div class="col-4">
                <q-input dark dense filled v-model="pagoForm.fechaPago" label="Fecha" type="date" />
              </div>
              <div class="col-4">
                <q-btn
                  label="Registrar"
                  color="positive"
                  :loading="savingPago"
                  @click="savePago"
                  class="full-width"
                />
              </div>
            </div>
          </q-card-section>
        </template>
      </q-card>
    </q-dialog>

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span
            >Eliminar préstamo <strong>{{ deletingItem?.nombre }}</strong
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
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.loan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.loan-card {
  background: rgba(27, 38, 36, 0.6);
  border: 1px solid rgba(113, 131, 127, 0.08);
  border-radius: 8px;
  padding: 16px;
  transition: border-color 0.15s ease;
}

.loan-card:hover {
  border-color: rgba(163, 120, 94, 0.2);
}

.loan-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.loan-card__name {
  font-size: 0.95rem;
  font-weight: 600;
}

.loan-card__amounts {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

.loan-card__amount-value {
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 0.85rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.loan-card__amount-value--balance {
  color: rgba(163, 120, 94, 0.8);
}

.loan-card__lender {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8rem;
  color: rgba(163, 120, 94, 0.5);
  margin-top: 8px;
}

.loan-card__term {
  font-size: 0.78rem;
  color: rgba(163, 120, 94, 0.4);
  margin-top: 2px;
}

.loan-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 2px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid rgba(113, 131, 127, 0.06);
}
</style>
