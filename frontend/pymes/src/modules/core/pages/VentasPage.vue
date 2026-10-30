<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, shallowRef } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { toLocalISODate } from 'src/utils/format';
import type { VentaDiaria, VentaRequest } from '../types';
import { useVentas } from '../composables/useVentas';
import VentasMonthPager from '../components/ventas/VentasMonthPager.vue';
import VentasStatStrip from '../components/ventas/VentasStatStrip.vue';
import VentasDayList from '../components/ventas/VentasDayList.vue';
import VentaFormDialog from '../components/ventas/VentaFormDialog.vue';
import EmptyState from 'src/components/ui/EmptyState.vue';

useMeta({ title: 'Ventas — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;

const canEdit = computed(() => authStore.user?.role === 'OWNER' || authStore.user?.role === 'ADMIN');

const {
  loading,
  error,
  monthLabel,
  dayGroups,
  totalMes,
  registroCount,
  ticketPromedio,
  prevMonth,
  nextMonth,
  goToday,
  load,
  create,
  update,
  remove,
} = useVentas(tenantId);

const dialogOpen = shallowRef(false);
const editingId = shallowRef<string | null>(null);
const formInitial = reactive<VentaRequest>({
  tenantId: tenantId as string,
  fecha: toLocalISODate(new Date()),
  montoBruto: 0,
  descripcion: null,
});

const deleteDialog = shallowRef(false);
const deletingItem = shallowRef<VentaDiaria | null>(null);
const deleting = shallowRef(false);

function openCreate() {
  if (!canEdit.value) return;
  editingId.value = null;
  Object.assign(formInitial, {
    tenantId: tenantId as string,
    fecha: toLocalISODate(new Date()),
    montoBruto: 0,
    descripcion: null,
    desglose: undefined,
  });
  dialogOpen.value = true;
}

function openEdit(v: VentaDiaria) {
  if (!canEdit.value) return;
  editingId.value = v.id;
  Object.assign(formInitial, {
    tenantId: v.tenantId,
    fecha: v.fecha,
    montoBruto: v.montoBruto,
    descripcion: v.descripcion,
    desglose: v.desglose,
  });
  dialogOpen.value = true;
}

async function onSave(payload: VentaRequest) {
  try {
    if (editingId.value) {
      await update(editingId.value, payload);
    } else {
      await create(payload);
    }
    dialogOpen.value = false;
    $q.notify({
      type: 'positive',
      message: `Venta ${editingId.value ? 'actualizada' : 'registrada'}`,
    });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al guardar venta',
    });
  }
}

function confirmDelete(v: VentaDiaria) {
  deletingItem.value = v;
  deleteDialog.value = true;
}

async function onRemove() {
  if (!deletingItem.value || !tenantId) return;
  deleting.value = true;
  try {
    await remove(deletingItem.value.id, tenantId);
    deleteDialog.value = false;
    $q.notify({ type: 'positive', message: 'Venta eliminada' });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al eliminar venta',
    });
  } finally {
    deleting.value = false;
    deletingItem.value = null;
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault();
    openCreate();
  }
}

async function onRefresh(done: () => void) {
  await load();
  done();
}

onMounted(() => {
  if (!tenantId) return;
  void load();
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => window.removeEventListener('keydown', handleKeydown));
</script>

<template>
  <q-page class="core-page ventas-page">
    <!-- Header editorial con firma cobre (sin glow — brief Mayor) -->
    <div class="page-header">
      <div class="page-header__main">
        <h1 class="page-title">Ventas</h1>
        <p class="page-subtitle">Registro diario de ventas</p>
      </div>
      <div class="page-header__signature" aria-hidden="true">
        <span class="signature-dot" />
        <span class="signature-line" />
      </div>
    </div>

    <div class="stats-row">
      <span class="stat"
        >{{ dayGroups.length }} {{ dayGroups.length === 1 ? 'día con registro' : 'días con registro' }}</span
      >
      <q-btn
        v-if="canEdit && dayGroups.length"
        color="primary"
        label="Nuevo"
        no-caps
        class="toolbar-new toolbar-new--top"
        @click="openCreate"
      />
    </div>
    <div class="rule" />

    <q-pull-to-refresh @refresh="onRefresh">
      <VentasMonthPager :label="monthLabel" @prev="prevMonth" @next="nextMonth" @today="goToday" />

      <VentasStatStrip :total="totalMes" :count="registroCount" :average="ticketPromedio" />

      <!-- CTA entre la franja de stats y la lista de días -->
      <div v-if="canEdit && dayGroups.length" class="stats-cta">
        <q-btn color="primary" label="Nuevo" no-caps class="toolbar-new" @click="openCreate" />
      </div>

      <div v-if="error" class="error-banner" role="alert">
        <q-icon name="error" size="1.25rem" class="q-mr-sm" />
        <span>{{ error }}</span>
        <q-btn flat dense no-caps label="Reintentar" color="accent" class="q-ml-sm" @click="load" />
      </div>

      <div v-if="loading" class="q-gutter-y-md">
        <q-skeleton v-for="n in 4" :key="n" type="rect" dark animation="pulse" height="48px" />
      </div>

      <div v-else-if="!dayGroups.length" class="q-mt-lg">
        <EmptyState
          icon="sym_r_point_of_sale"
          title="Sin ventas este mes"
          :message="`No hay ventas registradas en ${monthLabel}.`"
        >
          <q-btn
            v-if="canEdit"
            color="primary"
            label="Nueva Venta"
            no-caps
            class="q-mt-sm"
            @click="openCreate"
          />
        </EmptyState>
      </div>

      <VentasDayList
        v-else
        :groups="dayGroups"
        :can-edit="canEdit"
        @edit="openEdit"
        @delete="confirmDelete"
      />
    </q-pull-to-refresh>

    <VentaFormDialog
      v-model="dialogOpen"
      :editing="!!editingId"
      :initial="formInitial"
      @save="onSave"
    />

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="dialog-surface">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span
            >Eliminar venta del <strong>{{ deletingItem?.fecha }}</strong
            >?</span
          >
        </q-card-section>
        <q-card-actions align="right">
          <q-btn v-close-popup flat label="Cancelar" color="accent" />
          <q-btn label="Eliminar" color="negative" :loading="deleting" @click="onRemove" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<style scoped>
.ventas-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 64px);
}

/* Header con firma cobre (sin glow — brief Mayor) */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 8px;
}

.page-title {
  font-family: var(--pq-font-display);
  font-weight: 700;
  font-size: clamp(22px, 4vw, 28px);
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--pq-text);
  margin: 0;
}

.page-subtitle {
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

.signature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--pq-accent);
}

.signature-line {
  width: 40px;
  height: 1px;
  background: var(--pq-accent);
  opacity: 0.4;
}

/* Franja de contexto: dato a la izquierda, CTA al borde de salida (desktop) */
.stats-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px 12px;
  margin: 16px 0 8px;
}

/* CTA superior: solo desktop; en mobile va entre stats y lista */
.toolbar-new--top {
  display: none;
}

@media (min-width: 640px) {
  .toolbar-new--top {
    display: inline-flex;
  }
}

.stat {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
  text-transform: uppercase;
}

.rule {
  height: 1px;
  background: var(--pq-border);
  margin: 24px 0;
  opacity: 0.4;
}

/* CTA entre stats y lista — solo mobile; en desktop va en la franja superior */
.stats-cta {
  display: flex;
  justify-content: flex-end;
  margin: 16px 0 8px;
}

@media (min-width: 640px) {
  .stats-cta {
    display: none;
  }
}

/* Nuevo button — filled cobre (mismo patrón que Productos) */
.toolbar-new {
  min-height: 36px;
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

/* tablet táctil: escala táctil 44px */
@media (pointer: coarse) {
  .toolbar-new {
    min-height: 44px;
  }
}

/* Banner de error — franja fina, sin bloque de color completo */
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

.dialog-surface {
  background: var(--pq-elevated) !important;
  border-radius: var(--pq-radius-xl) !important;
  box-shadow: var(--pq-shadow-lg) !important;
  border: 1px solid rgba(77, 112, 99, 0.3);
}

.dialog-title {
  font-family: var(--pq-font-display);
  font-weight: 600;
  font-size: 18px;
  color: var(--pq-text);
}

@media (max-width: 639px) {
  /* mobile: CTA full-width, mismo patrón que antes */
  .stats-cta .toolbar-new {
    flex-basis: 100%;
    width: 100%;
    white-space: nowrap;
  }
}
</style>
