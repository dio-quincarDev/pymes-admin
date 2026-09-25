<script setup lang="ts">
import { computed } from 'vue';
import { useNumberFormat } from '../../composables/useNumberFormat';
import KpiCard from './KpiCard.vue';

interface Breakdown {
  insumos: number;
  variable: number;
  fijo: number;
  total: number;
}

interface Props {
  amount: number;
  breakdown: Breakdown;
  periodo: string;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), { loading: false });
const { formatCurrency } = useNumberFormat();

const formatted = computed(() => (props.loading ? '—' : formatCurrency(props.amount)));

const detail = computed(() => {
  if (props.breakdown.total === 0) return `Sin movimientos en ${props.periodo}`;
  return `Insumos ${formatCurrency(props.breakdown.insumos)} + variable ${formatCurrency(props.breakdown.variable)} pagado · ${props.periodo}`;
});
</script>

<template>
  <div :title="detail">
    <KpiCard label="Inversión mensual" :value="formatted" icon="payments" accent="gold" />
    <q-tooltip anchor="top middle" self="bottom middle" class="bg-dark text-white text-caption">
      {{ detail }}
    </q-tooltip>
  </div>
</template>
