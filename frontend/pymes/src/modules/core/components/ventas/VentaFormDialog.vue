<script setup lang="ts">
import { reactive, shallowRef, useTemplateRef, watch } from 'vue';
import type { VentaRequest } from '../../types';

const props = defineProps<{
  editing: boolean;
  initial: VentaRequest;
}>();

const emit = defineEmits<{
  save: [payload: VentaRequest];
}>();

const open = defineModel<boolean>({ required: true });
const saving = shallowRef(false);
const formRef = useTemplateRef<{ validate: () => Promise<boolean> }>('formRef');

const draft = reactive<VentaRequest>({ ...props.initial });
const montoBrutoStr = shallowRef('');

watch(
  () => props.initial,
  (val) => {
    Object.assign(draft, val);
    montoBrutoStr.value = rawAmount(val.montoBruto);
  },
  { immediate: true, deep: true },
);

function onGrossAmountInput(val: string | number | null) {
  montoBrutoStr.value = String(val ?? '')
    .replace(/[^0-9.]/g, '')
    .replace(/(\..*)\./g, '$1');
}

function formatGrossAmount() {
  const n = parseFloat(montoBrutoStr.value);
  if (!isNaN(n) && montoBrutoStr.value) {
    montoBrutoStr.value = n.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    draft.montoBruto = n;
  }
}

function rawAmount(val: number) {
  return val
    ? val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '';
}

async function onSave() {
  formatGrossAmount();
  if (!(await formRef.value?.validate())) return;
  saving.value = true;
  try {
    emit('save', { ...draft });
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <q-dialog v-model="open" dark>
    <q-card dark class="dialog-surface venta-dialog">
      <q-card-section>
        <div class="dialog-title">{{ editing ? 'Editar' : 'Nueva' }} Venta</div>
      </q-card-section>
      <q-separator dark />
      <q-card-section>
        <q-form ref="formRef" class="q-gutter-y-md" @submit.prevent="onSave">
          <q-input
            v-model="draft.fecha"
            dark
            outlined
            label="Fecha"
            type="date"
            :rules="[(v) => !!v || 'Requerido']"
          />
          <q-input
            :model-value="montoBrutoStr"
            dark
            outlined
            label="Monto Bruto"
            type="text"
            inputmode="decimal"
            prefix="$"
            :rules="[(v) => !!v || 'Requerido']"
            @update:model-value="onGrossAmountInput"
            @blur="formatGrossAmount"
          />
          <q-input v-model="draft.descripcion" dark outlined label="Descripción" />
          <div class="row justify-end q-gutter-x-sm">
            <q-btn v-close-popup flat label="Cancelar" color="accent" />
            <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
          </div>
        </q-form>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<style scoped>
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

.venta-dialog {
  width: 90vw;
  max-width: 460px;
}

/* mobile: full-screen con dvh */
@media (max-width: 639px) {
  .venta-dialog {
    width: 100vw;
    max-width: 100vw;
    height: 100dvh;
    max-height: 100dvh;
    border-radius: 0;
  }
}
</style>
