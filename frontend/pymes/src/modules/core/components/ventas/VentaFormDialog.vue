<script setup lang="ts">
import { computed, reactive, shallowRef, useTemplateRef, watch } from 'vue';
import type { VentaRequest, VentaPago } from '../../types';
import { METODOS_PAGO } from '../../utils/metodosPago';
import type { MetodoPago } from '../../types';

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
const montos = shallowRef<Record<MetodoPago, string>>({
  EFECTIVO: '', ACH: '', YAPPY: '', TARJETA: '', CHEQUE: '', CREDITO: '',
});

// rehidratar al abrir el diálogo, no al cambiar la prop props.initial
watch(open, (isOpen) => { if (isOpen) populateFromInitial(props.initial); }, { immediate: true });

function rawAmount(val: number) {
  return val
    ? val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '';
}

function populateFromInitial(val: VentaRequest) {
  Object.assign(draft, val);
  const next: Record<MetodoPago, string> = { EFECTIVO: '', ACH: '', YAPPY: '', TARJETA: '', CHEQUE: '', CREDITO: '' };
  if (val.desglose && val.desglose.length) {
    for (const p of val.desglose) next[p.metodo] = rawAmount(p.monto);
  } else if (val.montoBruto) {
    next.EFECTIVO = rawAmount(val.montoBruto);
  }
  montos.value = next;
}

function onMontoInput(metodo: MetodoPago, val: string | number | null) {
  const s = String(val ?? '').replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1');
  montos.value = { ...montos.value, [metodo]: s };
}

function parseMonto(s: string): number {
  const n = parseFloat(s.replace(',', '.'));
  return Number.isFinite(n) ? n : 0;
}

const total = computed(() =>
  (Object.keys(montos.value) as MetodoPago[]).reduce(
    (s, k) => s + parseMonto(montos.value[k] ?? ''),
    0,
  ),
);

async function onSave() {
  if (!(await formRef.value?.validate())) return;
  saving.value = true;
  try {
    const desglose: VentaPago[] = (Object.keys(montos.value) as MetodoPago[])
      .map((m) => ({ metodo: m, monto: parseMonto(montos.value[m] ?? '') }))
      .filter((p) => p.monto > 0);
    const payload: VentaRequest = {
      tenantId: draft.tenantId,
      fecha: draft.fecha,
      descripcion: draft.descripcion ?? null,
    };
    if (total.value > 0) payload.montoBruto = total.value;
    if (desglose.length) payload.desglose = desglose;
    emit('save', payload);
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
          <div class="desglose-grid">
            <q-input
              v-for="opt in METODOS_PAGO"
              :key="opt.value"
              :model-value="montos[opt.value]"
              dark
              outlined
              dense
              :label="opt.label"
              type="text"
              inputmode="decimal"
              prefix="$"
              @update:model-value="onMontoInput(opt.value, $event)"
            />
          </div>
          <div class="total-row">
            <span>Total</span>
            <span class="total-value">{{ total.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</span>
          </div>
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

.desglose-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

@media (max-width: 639px) {
  .desglose-grid {
    grid-template-columns: 1fr;
  }
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--pq-text);
  border-top: 1px solid var(--pq-border);
  padding-top: 8px;
}

.total-value {
  font-family: var(--pq-font-utility);
  color: var(--pq-accent);
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
