<script setup lang="ts">
import { computed } from 'vue';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import { useChartTheme } from 'src/modules/core/composables/useChartTheme';
import BaseChart from 'src/modules/core/components/charts/BaseChart.vue';
import type { AbcItem } from '../../types/analytics';

interface Props {
  items: AbcItem[];
  loading?: boolean;
  empty?: boolean;
}
const props = withDefaults(defineProps<Props>(), { loading: false, empty: false });
const { formatCurrency } = useNumberFormat();
const { colors } = useChartTheme();

// ponytail: frontend tolera backend (totalSpend/pct); 0 si falta — sin tocar servidor
function getSpend(item: AbcItem): number {
  const raw = item as unknown as Record<string, unknown>;
  const v = (raw.spend ?? raw.totalSpend) as unknown;
  if (typeof v === 'number' && Number.isFinite(v)) return v;
  if (typeof v === 'string') {
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  }
  return 0;
}

const chartData = computed(() => {
  const sorted = [...props.items].sort((a, b) => getSpend(b) - getSpend(a));
  const top = sorted.slice(0, 5);
  const rest = sorted.slice(5);
  const labels = top.map(i => i.productName);
  const data = top.map(i => getSpend(i));
  if (rest.length) {
    labels.push('Otros');
    data.push(rest.reduce((s, i) => s + getSpend(i), 0));
  }
  return {
    labels,
    datasets: [{
      data,
      backgroundColor: [colors.value.abcA, colors.value.abcB, colors.value.positive, colors.value.negative, colors.value.info, colors.value.text].slice(0, data.length),
      borderColor: 'transparent',
      borderWidth: 0,
      hoverOffset: 4,
    }],
  };
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '62%',
  plugins: {
    legend: { display: true, position: 'bottom' as const, labels: { boxWidth: 10, boxHeight: 10, usePointStyle: true, pointStyle: 'circle', padding: 14, font: { family: "'Satoshi', sans-serif", size: 11 } } },
    tooltip: {
      callbacks: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        label: (ctx: any) => `${ctx.label}: ${formatCurrency(ctx.parsed ?? 0)}`,
      },
    },
  },
}));
</script>

<template>
  <div class="top-productos-donut">
    <div v-if="loading" class="flex flex-center q-pa-md"><q-spinner color="primary" size="24px" /></div>
    <div v-else-if="empty || items.length===0" class="flex flex-center column q-pa-md" style="color: var(--pq-text-muted)"><q-icon name="donut_large" size="28px" /><span class="text-caption">Sin datos</span></div>
    <BaseChart v-else type="doughnut" :data="chartData" :options="chartOptions" :height="260" />
  </div>
</template>

<style scoped lang="scss">
.top-productos-donut {
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}
</style>
