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
const { formatCurrency, formatCompactCurrency } = useNumberFormat();
const { colors } = useChartTheme();

const total = computed(() => props.items.reduce((s, i) => s + (i.spend ?? 0), 0));

// paleta compartida por arcos y leyenda (mismo orden, datos mismos índices)
const palette = computed(() => [
  colors.value.abcA, colors.value.abcB, colors.value.abcC, colors.value.info, colors.value.positive, colors.value.bar,
]);

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
      backgroundColor: palette.value.slice(0, data.length),
      borderColor: 'transparent',
      borderWidth: 0,
      hoverOffset: 4,
    }],
  };
});

// leyenda en HTML (fuera del canvas): el donut queda centrado en vertical y el "Total" cae en el hueco
const legendItems = computed(() => {
  const sorted = [...props.items].sort((a, b) => b.spend - a.spend);
  const items = sorted.slice(0, 5).map((i, idx) => ({ label: i.productName, color: palette.value[idx] }));
  if (sorted.length > 5) items.push({ label: 'Otros', color: palette.value[5] });
  return items;
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '62%',
  plugins: {
    legend: { display: false },
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
    <span class="donut-center__value donut-center__value--full">{{ formatCurrency(total) }}</span>
    <span class="donut-center__value donut-center__value--compact">{{ formatCompactCurrency(total) }}</span>
  </div>
</div>
  <ul v-if="legendItems.length" class="donut-legend">
    <li v-for="item in legendItems" :key="item.label" class="donut-legend__item" :title="item.label">
      <span class="donut-legend__dot" :style="{ background: item.color }" />
      <span class="donut-legend__name">{{ item.label }}</span>
    </li>
  </ul>
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

.donut-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  justify-content: center;
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  min-width: 0;
}

.donut-legend__item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.donut-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.donut-legend__name {
  font-family: 'Satoshi', sans-serif;
  font-size: 11px;
  color: var(--pq-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}

@media (max-width: 600px) {
  .donut-wrap :deep(.base-chart) {
    height: 200px !important;
  }
  // en pantalla chica el hueco es de ~70px: monto completo fuera, corto dentro
  .donut-center__value--full { display: none; }
  .donut-center__value--compact {
    display: block;
    font-size: 0.95rem;
  }
}
</style>
