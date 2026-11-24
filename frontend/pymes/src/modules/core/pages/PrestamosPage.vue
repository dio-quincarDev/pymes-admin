<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { formatCurrency, toLocalISODate } from 'src/utils/format';
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
const loadError = shallowRef(false);

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
    loadError.value = true;
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
  fechaInicio: toLocalISODate(new Date()),
});

function openCreate() {
  editingId.value = null;
  form.value = {
    tenantId: tenantId as string,
    nombre: '',
    monto: 0,
    tasaInteres: 0,
    plazoMeses: 0,
    fechaInicio: toLocalISODate(new Date()),
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
  fechaPago: toLocalISODate(new Date()),
});
const pagosLoaded = shallowRef(false);

function openPagos(p: Prestamo) {
  pagoPrestamo.value = p;
  pagoForm.value = { monto: 0, fechaPago: toLocalISODate(new Date()) };
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
    pagoForm.value = { monto: 0, fechaPago: toLocalISODate(new Date()) };
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
  <!-- Header editorial con firma cobre -->
  <div class="page-header" data-tour="prestamos">
    <div class="page-header__main">
      <h1 class="page-title">Préstamos</h1>
      <p class="page-subtitle">Gestión de préstamos y pagos</p>
    </div>
    <div class="page-header__signature" aria-hidden="true">
      <span class="signature-dot" />
      <span class="signature-line" />
    </div>
  </div>

  <!-- Stats row + rule -->
  <div class="stats-row">
    <span class="stat">{{ formatCurrency(totalPrestado) }} {{ totalPrestado === 1 ? 'total prestado' : 'totales prestados' }}</span>
    <span class="stat-sep" aria-hidden="true">·</span>
    <span class="stat">{{ formatCurrency(totalSaldo) }} {{ totalSaldo === 1 ? 'saldo pendiente' : 'saldos pendientes' }}</span>
    <span class="stat-sep" aria-hidden="true">·</span>
    <span class="stat">{{ formatCurrency(totalAPagar) }} {{ totalAPagar === 1 ? 'total a pagar' : 'totales a pagar' }}</span>
  </div>
  <div class="rule" />

  <!-- Toolbar -->
  <div class="prestamos-toolbar">
    <q-space />
    <q-btn v-if="isOwner && rows.length" color="primary" label="Nuevo" no-caps @click="openCreate" class="toolbar-new" />
  </div>

  <!-- Empty -->
  <div v-if="!loading && !rows.length">
    <EmptyState
      icon="sym_r_account_balance"
      title="Sin préstamos registrados"
      message="Registra un préstamo para hacer seguimiento de tus deudas."
    >
      <q-btn v-if="isOwner" color="primary" no-caps label="Nuevo Préstamo" @click="openCreate" class="q-mt-sm" />
    </EmptyState>
  </div>

  <!-- Error -->
  <div v-if="!loading && loadError" class="error-banner" role="alert">
    <q-icon name="error" size="1.25rem" class="q-mr-sm" />
    <span>No se pudo cargar el catálogo de préstamos.</span>
    <q-btn flat dense no-caps label="Reintentar" color="accent" class="q-ml-sm" @click="load" />
  </div>

  <!-- Loading -->
  <div v-if="loading" class="card-grid">
    <div v-for="n in 3" :key="n" class="skeleton-card" />
  </div>

  <!-- Loan list -->
  <div v-if="!loading && rows.length" class="card-grid">
    <div v-for="p in rows" :key="p.id" class="prestamo-card">
      <div class="prestamo-card__header">
        <div class="prestamo-card__name">{{ p.nombre }}</div>
        <q-badge :color="statusColor(p.estado)" class="q-px-sm q-py-xs">{{ p.estado }}</q-badge>
      </div>

      <div class="prestamo-card__meta">
        <span class="prestamo-card__amount">
          <span class="prestamo-card__amount-label">Monto</span>
          <span class="prestamo-card__amount-value">{{ formatCurrency(p.monto) }}</span>
        </span>
        <span class="prestamo-card__separator">·</span>
        <span class="prestamo-card__amount">
          <span class="prestamo-card__amount-label">Saldo</span>
          <span class="prestamo-card__amount-value loan-card__amount-value--balance">{{ formatCurrency(p.saldoPendiente) }}</span>
        </span>
        <span class="prestamo-card__separator">·</span>
        <span class="prestamo-card__amount">
          <span class="prestamo-card__amount-label">Tasa</span>
          <span class="prestamo-card__amount-value">{{ p.tasaInteres }}%</span>
        </span>
      </div>

      <q-linear-progress
        :value="p.monto > 0 ? (p.monto - p.saldoPendiente) / p.monto : 0"
        :color="p.saldoPendiente === 0 ? 'positive' : 'accent'"
        class="prestamo-card__progress"
        size="4px"
        rounded
      />

      <div v-if="p.prestamista" class="prestamo-card__lender">
        <q-icon name="person" size="0.8rem" class="text-accent" />
        {{ p.prestamista }}
      </div>

      <div v-if="p.plazoMeses" class="prestamo-card__term">
        {{ p.plazoMeses }} meses · Cuota estimada
        {{ formatCurrency(cuotaMensual(p.monto, p.tasaInteres, p.plazoMeses)) }}/mes
      </div>

      <div class="prestamo-card__actions">
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

  <!-- Create/Edit Dialog -->
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

  <!-- Payment Dialog -->
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

  <!-- Delete Dialog -->
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
</template>

<style scoped>
/* Signature cobre — igual que Productos/Ventas */
.page-header {
  margin-bottom: 24px;
}
.page-header__main {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}
.page-header__main h1 {
  font-family: var(--pq-font-display);
  font-weight: 700;
  font-size: clamp(22px, 4vw, 28px);
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--pq-text);
  margin: 0;
}
.page-header__main p {
  font-family: var(--pq-font-body);
  font-weight: 500;
  font-size: 13px;
  line-height: 1.5;
  color: var(--pq-text-muted);
  margin: 4px 0 0;
}
.page-header__signature {
  display: none;
  align-items: center;
  gap: 8px;
}
@media (min-width: 1024px) {
  .page-header__signature {
    display: flex;
  }
}
.page-header__signature .signature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--pq-accent);
}
.page-header__signature .signature-line {
  width: 40px;
  height: 1px;
  background: var(--pq-accent);
  opacity: 0.4;
}

/* Stats row + rule */
.stats-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 8px;
}
.stats-row .stat {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
  text-transform: uppercase;
}
.stats-row .stat-sep {
  color: var(--pq-border);
  opacity: 0.4;
}
.rule {
  height: 1px;
  background: var(--pq-border);
  margin: 24px 0;
  opacity: 0.4;
}

/* Toolbar */
.prestamos-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
@media (max-width: 639px) {
  .prestamos-toolbar {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .toolbar-new {
    width: 100%;
    min-width: 0;
    max-width: none;
  }
}

/* toolbar-new estilo cobre con shadow + lift */
.toolbar-new {
  min-height: 40px;
  background: var(--pq-accent) !important;
  color: var(--pq-background) !important;
  font-weight: 600;
  border-radius: var(--pq-radius-sm);
  box-shadow: 0 2px 8px rgba(163, 120, 94, 0.25);
  transition: background-color 0.16s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.16s ease, transform 0.1s ease;
}
.toolbar-new:hover {
  background: var(--pq-accent-hover) !important;
  box-shadow: 0 4px 16px rgba(163, 120, 94, 0.35);
  transform: translateY(-1px);
}
.toolbar-new:active {
  transform: scale(0.985) translateY(0);
}
.toolbar-new:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}
@media (pointer: coarse) {
  .toolbar-new {
    min-height: 44px;
  }
}

/* EmptyState wrapper — sin q-mt-lg, alineado con Productos */
.empty-wrapper {
  margin: 24px 0;
}

/* Error banner */
.error-banner {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 16px;
  border: 1px solid rgba(192, 106, 96, 0.4);
  border-left: 3px solid var(--pq-danger);
  border-radius: var(--pq-radius-md);
  color: var(--pq-danger);
  font-family: var(--pq-font-body);
  font-size: 13px;
  margin-bottom: 24px;
  background: rgba(192, 106, 96, 0.04);
}

/* Card grid */
.card-grid {
  display: grid;
  gap: 12px;
}
@media (min-width: 640px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (min-width: 1024px) {
  .card-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* Skeletons */
.skeleton-card {
  border: 1px solid rgba(77, 112, 99, 0.3);
  border-left: 3px solid var(--pq-accent-muted);
  border-radius: var(--pq-radius-md);
  min-height: 140px;
  background: var(--pq-surface);
  margin: 8px 0;
}

/* ---------- Tarjeta de préstamo (estilo product-card) ---------- */
.prestamo-card {
  position: relative;
  background: var(--pq-surface);
  border: 1px solid rgba(77, 112, 99, 0.4);
  border-left: 3px solid var(--pq-accent-muted);
  border-radius: var(--pq-radius-md);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: border-color 0.2s cubic-bezier(0.22, 1, 0.36, 1), border-left-color 0.2s ease, transform 0.2s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.2s ease;
  outline: none;
}
.prestamo-card:hover,
.prestamo-card:focus-visible {
  border-color: rgba(77, 112, 99, 0.6);
  border-left-color: var(--pq-accent);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(5, 12, 10, 0.35), 0 0 0 1px rgba(163, 120, 94, 0.08);
}
.prestamo-card:focus-visible {
  box-shadow: 0 0 0 2px var(--pq-accent);
}

/* Header de la card: nombre + badge */
.prestamo-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid rgba(113, 131, 127, 0.08);
}
.prestamo-card__name {
  font-family: var(--pq-font-display);
  font-weight: 600;
  font-size: 0.95rem;
}

/* Meta line: Monto · Saldo · Tasa */
.prestamo-card__meta {
  padding: 0 16px 16px;
  font-size: 0.78rem;
}
.prestamo-card__amount {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.prestamo-card__amount-label {
  font-family: var(--pq-font-utility);
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--pq-text-subtle);
  white-space: nowrap;
}
.prestamo-card__amount-value {
  font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  font-size: 0.85rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-text);
  white-space: nowrap;
}
.prestamo-card__amount-value--balance {
  color: rgba(163, 120, 94, 0.8);
}
.prestamo-card__separator {
  margin: 0 4px;
  color: rgba(163, 120, 94, 0.3);
}

/* Progress bar */
.prestamo-card__progress {
  width: 100%;
  margin: 8px 16px;
}

/* Lender */
.prestamo-card__lender {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8rem;
  color: rgba(163, 120, 94, 0.5);
  padding: 0 16px 16px;
}

/* Term (plazo + cuota) */
.prestamo-card__term {
  font-size: 0.78rem;
  color: rgba(163, 120, 94, 0.4);
  margin: 0 16px 16px;
  padding-bottom: 4px;
  border-bottom: 1px solid rgba(113, 131, 127, 0.04);
}

/* Acciones footer */
.prestamo-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 2px;
  margin: 0 16px 16px;
  padding-top: 8px;
  border-top: 1px solid rgba(113, 131, 127, 0.06);
}
.prestamo-card__actions q-btn {
  transition: background-color 0.16s cubic-bezier(0.22, 1, 0.36, 1), color 0.16s ease, transform 0.1s ease;
  border-radius: var(--pq-radius-sm);
}
.prestamo-card__actions q-btn:hover {
  background-color: rgba(163, 120, 94, 0.04);
  transform: scale(1.05);
}
.prestamo-card__actions q-btn:active {
  transform: scale(0.95);
}
.prestamo-card__actions q-btn:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}

/* Reduce motion */
@media (prefers-reduced-motion: reduce) {
  .prestamo-card:hover,
  .prestamo-card:focus-visible {
    transform: none;
    box-shadow: 0 0 0 1px rgba(163, 120, 94, 0.08);
  }
  .toolbar-new:hover,
  .toolbar-new:active {
    transition: none;
  }
}
</style>