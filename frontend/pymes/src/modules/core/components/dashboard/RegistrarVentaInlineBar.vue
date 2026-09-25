<script setup lang="ts">
import { useTemplateRef } from 'vue';
import { useRegistrarVenta } from '../../composables/useRegistrarVenta';

interface Props {
  modelValue: boolean;
}

defineProps<Props>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  created: [];
}>();

const { montoStr, descripcion, fecha, saving, error, isValid, save } = useRegistrarVenta();

// ponytail: native date picker only opens on indicator; expand hit-area via showPicker() — useTemplateRef per sfc.md (Vue 3.5+)
const fechaInputRef = useTemplateRef<{ $el: HTMLElement }>('fechaInputRef');

function openPicker() {
  const input = fechaInputRef.value?.$el.querySelector<HTMLInputElement>('input[type="date"]');
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
  <transition name="venta-inline">
    <div v-if="modelValue" class="venta-inline-bar" role="region" aria-label="Registrar venta rápida">
      <q-input
        v-model="montoStr"
        placeholder="B/. 0.00"
        prefix="B/."
        outlined
        dense
        input-class="venta-inline-bar__mono"
        class="venta-inline-bar__field venta-inline-bar__field--monto"
        :error="!!error"
        :error-message="error"
        @keyup.enter="onSave"
      />
      <q-input
        ref="fechaInputRef"
        v-model="fecha"
        type="date"
        outlined
        dense
        class="venta-inline-bar__field venta-inline-bar__field--fecha"
        @click="openPicker"
      />
      <q-input
        v-model="descripcion"
        placeholder="Descripción (opcional)"
        outlined
        dense
        class="venta-inline-bar__field venta-inline-bar__field--desc"
        @keyup.enter="onSave"
      />
      <q-btn
        no-caps
        label="Guardar"
        color="primary"
        text-color="dark"
        class="venta-inline-bar__save"
        :disable="!isValid"
        :loading="saving"
        @click="onSave"
      />
      <q-btn flat dense round icon="sym_r_close" aria-label="Cerrar" @click="emit('update:modelValue', false)" />
      <div class="venta-inline-bar__hint">Se refleja en gráfico 7 días al guardar (Costo es fijo)</div>
    </div>
  </transition>
</template>

<style scoped lang="scss">
.venta-inline-bar {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px;
  margin-bottom: 20px;
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-md);
  box-shadow: var(--pq-shadow-md, 0 8px 24px rgba(0, 0, 0, 0.25));

  &__field {
    flex: 1 1 140px;
    min-width: 0;

    &--monto {
      flex: 1 1 140px;
    }

    &--fecha {
      flex: 1 1 160px;
    }

    &--desc {
      flex: 1 1 180px;
    }

    :deep(.q-field__control) {
      background: var(--pq-background);
    }
  }

  &__mono {
    font-family: 'Geist Mono', monospace;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }

  &__save {
    font-family: 'Satoshi', sans-serif;
    font-weight: 700;
    align-self: center;
  }

  &__hint {
    flex: 1 0 100%;
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    color: var(--pq-text-subtle);
    margin-top: 2px;
  }
}

.venta-inline-enter-active,
.venta-inline-leave-active {
  transition: opacity var(--pq-motion-base, 160ms) cubic-bezier(0.4, 0, 0.2, 1),
    transform var(--pq-motion-base, 160ms) cubic-bezier(0.16, 1, 0.3, 1);
}

.venta-inline-enter-from,
.venta-inline-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 600px) {
  .venta-inline-bar {
    &__field {
      flex: 1 1 100%;

      &--monto,
      &--fecha,
      &--desc {
        flex: 1 1 100%;
      }
    }

    &__save {
      width: 100%;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .venta-inline-enter-active,
  .venta-inline-leave-active {
    transition: none;
  }
}
</style>
