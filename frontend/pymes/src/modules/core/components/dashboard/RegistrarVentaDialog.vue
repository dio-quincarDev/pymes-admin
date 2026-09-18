<script setup lang="ts">
import { useTemplateRef } from 'vue';
import { useRegistrarVenta } from '../../composables/useRegistrarVenta';

defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'created': [];
}>();

// ponytail: delegate state/side-effects to composable — dialog stays presentational (props down, events up)
const { montoStr, descripcion, fecha, saving, error, isValid, save } = useRegistrarVenta();

// ponytail: useTemplateRef per sfc.md (Vue 3.5+) — typed as component instance with $el
const fechaDialogRef = useTemplateRef<{ $el: HTMLElement }>('fechaDialogRef');

function openPickerDialog() {
  const input = fechaDialogRef.value?.$el.querySelector<HTMLInputElement>('input[type="date"]');
  if (input?.showPicker) {
    try {
      input.showPicker();
    } catch {
      input.focus();
    }
  } else {
    input?.focus();
    input?.click();
  }
}

async function onSave() {
  const ok = await save();
  if (ok) {
    emit('created');
    emit('update:modelValue', false);
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
          v-model="montoStr"
          label="Monto"
          prefix="B/."
          placeholder="0.00"
          outlined
          dense
          autofocus
          input-class="venta-dialog__mono"
          class="venta-dialog__input"
          :error="!!error"
          :error-message="error"
          @keyup.enter="onSave"
        />
        <q-input
          ref="fechaDialogRef"
          v-model="fecha"
          label="Fecha"
          type="date"
          outlined
          dense
          class="venta-dialog__input"
          @click="openPickerDialog"
        />
        <q-input
          v-model="descripcion"
          label="Descripción (opcional)"
          outlined
          dense
          class="venta-dialog__input"
          @keyup.enter="onSave"
        />
      </q-card-section>

      <q-card-actions align="right" class="venta-dialog__actions">
        <q-btn flat no-caps label="Cancelar" @click="emit('update:modelValue', false)" />
        <q-btn
          no-caps
          label="Guardar"
          color="primary"
          text-color="dark"
          :disable="!isValid"
          :loading="saving"
          @click="onSave"
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

  &__mono {
    font-family: 'Geist Mono', monospace;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
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
