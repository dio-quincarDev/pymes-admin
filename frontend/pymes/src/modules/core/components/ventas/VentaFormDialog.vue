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
  <q-dialog v-model="open">
    <q-card dark class="bg-surface-pine venta-dialog">
      <q-card-section>
        <div class="text-h6 text-primary">{{ editing ? 'Editar' : 'Nueva' }} Venta</div>
      </q-card-section>
      <q-separator dark />
      <q-card-section>
        <q-form ref="formRef" class="q-gutter-y-md" @submit.prevent="onSave">
          <q-input
            v-model="draft.fecha"
            dark
            filled
            label="Fecha"
            type="date"
            :rules="[(v) => !!v || 'Requerido']"
          />
          <q-input
            :model-value="montoBrutoStr"
            dark
            filled
            label="Monto Bruto"
            type="text"
            inputmode="decimal"
            prefix="$"
            :rules="[(v) => !!v || 'Requerido']"
            @update:model-value="onGrossAmountInput"
            @blur="formatGrossAmount"
          />
          <q-input v-model="draft.descripcion" dark filled label="Descripción" />
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
.venta-dialog {
  width: 90vw;
  max-width: 460px;
}

/* quasar-skilld: responsive CSS en vez de Screen plugin en JS */
@media (max-width: 599px) {
  .venta-dialog {
    width: 100vw;
    max-width: 100vw;
    height: 100dvh;
    max-height: 100dvh;
    border-radius: 0;
  }
}
</style>
