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

const total = computed(() => props.items.reduce((s, i) => s + (i.spend ?? 0), 0));

const chartData = computed(() => {
  const sorted = [...props.items].sort((a, b) => b.spend - a.spend);
  const top = sorted.slice(0, 5);
  const rest = sorted.slice(5);
  const labels = top.map(i => i.productName);
  const data = top.map((i) => i.spend);
  if (rest.length) {
    labels.push('Otros');
    data.push(rest.reduce((s, i) => s + i.spend, 0));
  }
  return {
    labels,
    datasets: [{
      data,
      backgroundColor: [colors.value.abcA, colors.value.abcB, colors.value.abcC, colors.value.info, colors.value.positive, colors.value.bar].slice(0, data.length),
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
    <div v-else class="donut-wrap">
  <BaseChart type="doughnut" :data="chartData" :options="chartOptions" :height="260" />
  <div class="donut-center">
    <span class="donut-center__label">Total</span>
    <span class="donut-center__value">{{ formatCurrency(total) }}</span>
  </div>
</div>
  </div>
</template>

<style scoped lang="scss">
.top-productos-donut {
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.donut-wrap {
  position: relative;
  min-width: 0;
  max-width: 100%;
}

.donut-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
  text-align: center;
}

.donut-center__label {
  font-family: var(--pq-font-utility);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
}

.donut-center__value {
  font-family: var(--pq-font-display);
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--pq-text);
  line-height: 1.1;
}

@media (max-width: 600px) {
  .donut-wrap :deep(.base-chart) {
    height: 200px !important;
  }
  .donut-center__value {
    font-size: 0.95rem;
  }
}
</style>
