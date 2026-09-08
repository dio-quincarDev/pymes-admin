<script setup lang="ts">
import { ref, computed } from 'vue';
import { useQuasar } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { ventaService } from '../../services/venta.service';

defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'created': [];
}>();

const $q = useQuasar();
const authStore = useAuthStore();
const saving = ref(false);

const amount = ref<number | null>(null);
const descripcion = ref('');
const fecha = ref(new Date().toISOString().slice(0, 10));

const isValid = computed(() => amount.value !== null && amount.value > 0);

async function save() {
  const tenantId = authStore.user?.tenantId;
  if (!tenantId || !isValid.value) return;

  saving.value = true;
  try {
    await ventaService.create({
      tenantId,
      montoBruto: amount.value!,
      fecha: fecha.value,
      descripcion: descripcion.value || null,
    });
    $q.notify({ type: 'positive', message: 'Venta registrada' });
    emit('created');
    emit('update:modelValue', false);
    amount.value = null;
    descripcion.value = '';
    fecha.value = new Date().toISOString().slice(0, 10);
  } catch {
    $q.notify({ type: 'negative', message: 'Error al registrar venta' });
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <q-dialog :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)">
    <q-card class="venta-dialog">
      <q-card-section class="venta-dialog__header">
        <span class="venta-dialog__title">Registrar venta</span>
        <q-btn flat dense round icon="sym_r_close" size="sm" @click="emit('update:modelValue', false)" />
      </q-card-section>

      <q-card-section class="venta-dialog__body">
        <q-input
          v-model.number="amount"
          label="Monto"
          type="number"
          step="0.01"
          min="0"
          outlined
          dense
          autofocus
          class="venta-dialog__input"
        />
        <q-input
          v-model="fecha"
          label="Fecha"
          type="date"
          outlined
          dense
          class="venta-dialog__input"
        />
        <q-input
          v-model="descripcion"
          label="Descripción (opcional)"
          outlined
          dense
          class="venta-dialog__input"
        />
      </q-card-section>

      <q-card-actions align="right" class="venta-dialog__actions">
        <q-btn flat no-caps label="Cancelar" @click="emit('update:modelValue', false)" />
        <q-btn
          no-caps
          label="Guardar"
          color="positive"
          :disable="!isValid"
          :loading="saving"
          @click="save"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<style scoped lang="scss">
.venta-dialog {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  min-width: 320px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-bottom: 1px solid var(--pq-border);
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 14px;
    font-weight: 600;
    color: var(--pq-text);
  }

  &__body {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__input {
    :deep(.q-field__control) {
      background: var(--pq-background);
    }

    :deep(.q-field__label) {
      color: var(--pq-text-muted);
    }
  }

  &__actions {
    padding: 8px 16px 14px;
  }
}
</style>
