<script setup lang="ts">
import { computed } from 'vue';
import { useNumberFormat } from '../../composables/useNumberFormat';
import KpiCard from './KpiCard.vue';

interface Breakdown {
  fijo: number;
  variable: number;
  total: number;
}

interface Props {
  amount: number;
  breakdown: Breakdown;
  periodo: string;
  bajaConfianza?: boolean;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), { bajaConfianza: false, loading: false });
const { formatCurrency } = useNumberFormat();

const formatted = computed(() => (props.loading ? '—' : formatCurrency(props.amount)));

const detail = computed(() => {
  if (props.breakdown.total === 0) return `Sin proyección en ${props.periodo}`;
  const base = `Fijo ${formatCurrency(props.breakdown.fijo)} + variable proyectado ${formatCurrency(props.breakdown.variable)} · ${props.periodo}`;
  return props.bajaConfianza ? `${base} · baja confianza (pocos datos)` : base;
});
</script>

<template>
  <div :title="detail">
    <KpiCard label="Proyección mensual" :value="formatted" icon="trending_up" accent="copper" />
    <q-tooltip anchor="top middle" self="bottom middle" class="bg-dark text-white text-caption">
      {{ detail }}
    </q-tooltip>
  </div>
</template>
