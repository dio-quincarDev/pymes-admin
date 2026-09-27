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
  <q-page class="core-page">
    <div class="q-mb-md">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Ventas</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Registro diario de ventas</p>
    </div>

    <q-pull-to-refresh @refresh="onRefresh">
      <VentasMonthPager :label="monthLabel" @prev="prevMonth" @next="nextMonth" @today="goToday" />

      <VentasStatStrip :total="totalMes" :count="registroCount" :average="ticketPromedio" />

      <q-banner v-if="error" dense class="bg-negative text-white q-mb-sm">
        {{ error }}
        <template #action>
          <q-btn flat dense label="Reintentar" @click="load" />
        </template>
      </q-banner>

      <div v-if="loading" class="q-gutter-y-md q-mt-md">
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
            icon="sym_r_add"
            label="Nueva Venta"
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

    <q-page-sticky v-if="canEdit" position="bottom-right" :offset="[16, 88]">
      <q-btn fab icon="sym_r_add" color="primary" aria-label="Nueva venta" @click="openCreate" />
    </q-page-sticky>

    <VentaFormDialog
      v-model="dialogOpen"
      :editing="!!editingId"
      :initial="formInitial"
      @save="onSave"
    />

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
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
