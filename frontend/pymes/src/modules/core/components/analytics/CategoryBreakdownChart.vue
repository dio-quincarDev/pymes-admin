<script setup lang="ts">
import { computed } from 'vue';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import { useChartTheme } from 'src/modules/core/composables/useChartTheme';
import BaseChart from 'src/modules/core/components/charts/BaseChart.vue';

interface CategoryBreakdownItem {
  category: string;
  currentAmount: number;
  previousAmount?: number | undefined;
  percentage: number;
}

interface Props {
  items: CategoryBreakdownItem[];
  loading?: boolean;
  empty?: boolean;
  maxItems?: number;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  empty: false,
  maxItems: 5,
});

const { formatCurrency } = useNumberFormat();
const { colors } = useChartTheme();

// ponytail: donut 5+Otros — evita barras horizontales y scroll-x en móvil (Panamá UTC-5)
const MAX_SLICES = 5;

const chartData = computed(() => {
  const sorted = [...props.items].sort((a, b) => b.currentAmount - a.currentAmount);
  const top = sorted.slice(0, MAX_SLICES);
  const rest = sorted.slice(MAX_SLICES);
  const othersTotal = rest.reduce((s, i) => s + i.currentAmount, 0);
  const othersPct = rest.reduce((s, i) => s + i.percentage, 0);

  const labels = top.map(i => i.category);
  const data = top.map(i => i.currentAmount);
  const pcts = top.map(i => i.percentage);

  if (rest.length) {
    labels.push('Otros');
    data.push(othersTotal);
    pcts.push(othersPct);
  }

  // store pcts for tooltip
  ;(chartData as unknown as { _pcts: number[] })._pcts = pcts;

  return {
    labels,
    datasets: [
      {
        data,
        backgroundColor: [
          colors.value.abcA,
          colors.value.abcB,
          colors.value.positive,
          colors.value.negative,
          colors.value.info,
          colors.value.text,
        ].slice(0, data.length),
        borderColor: 'transparent',
        borderWidth: 0,
        hoverOffset: 4,
      },
    ],
  };
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '62%',
  plugins: {
    legend: {
      display: true,
      position: 'bottom' as const,
      labels: {
        boxWidth: 10,
        boxHeight: 10,
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 14,
        font: { family: "'Satoshi', sans-serif", size: 11 },
      },
    },
    tooltip: {
      callbacks: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        label: (context: any) => {
          const value = context.parsed ?? 0;
          const idx = context.dataIndex as number;
          // pct from sorted slice
          const sorted = [...props.items].sort((a, b) => b.currentAmount - a.currentAmount);
          const top = sorted.slice(0, MAX_SLICES);
          const rest = sorted.slice(MAX_SLICES);
          let pct: number;
          if (idx < top.length) pct = top[idx]?.percentage ?? 0;
          else pct = rest.reduce((s, i) => s + i.percentage, 0);
          return `${context.label}: ${formatCurrency(value)} (${pct.toFixed(1)}%)`;
        },
      },
    },
  },
}));
</script>

<template>
  <div class="cat-chart">
    <template v-if="loading">
      <div class="cat-chart__skeleton">
        <div class="cat-chart__skeleton-donut">
          <div class="skeleton" style="width: 120px; height: 120px; border-radius: 50%" />
          <div class="cat-chart__skeleton-legend">
            <div v-for="i in 4" :key="i" class="skeleton" style="width: 80px; height: 12px" />
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="empty || items.length === 0">
      <div class="cat-chart__empty">
        <q-icon name="donut_large" size="32px" style="color: var(--pq-text-subtle)" aria-hidden="true" />
        <p>No hay gastos en este período</p>
      </div>
    </template>

    <template v-else>
      <BaseChart type="doughnut" :data="chartData" :options="chartOptions" :height="260" />
    </template>
  </div>
</template>

<style scoped lang="scss">
.cat-chart {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 8px;
  padding: 16px;

  &__skeleton {
    padding: 8px 0;
  }

  &__skeleton-donut {
    display: flex;
    align-items: center;
    gap: 20px;
    justify-content: center;
    flex-wrap: wrap;
  }

  &__skeleton-legend {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 32px 0;
    text-align: center;

    p {
      font-family: 'Satoshi', sans-serif;
      font-size: 13px;
      color: var(--pq-text-muted);
      margin: 0;
    }
  }
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--pq-surface) 0%,
    var(--pq-elevated) 50%,
    var(--pq-surface) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 2px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
