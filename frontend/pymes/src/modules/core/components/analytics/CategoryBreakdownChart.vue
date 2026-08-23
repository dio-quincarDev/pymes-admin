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
  maxItems: 8,
});

const { formatCurrency } = useNumberFormat();
const { colors } = useChartTheme();

const chartData = computed(() => {
  const items = props.items.slice(0, props.maxItems);
  return {
    labels: items.map(i => i.category),
    datasets: [
      {
        data: items.map(i => i.currentAmount),
        backgroundColor: colors.value.bar,
        borderColor: colors.value.bar,
        borderWidth: 0,
        borderRadius: 3,
        barThickness: 16,
      },
    ],
  };
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: 'y' as const,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        label: (context: any) => {
          const value = context.parsed?.x ?? 0;
          const item = props.items[context.dataIndex];
          const pct = item ? `${item.percentage.toFixed(1)}%` : '';
          return `${formatCurrency(value)} (${pct})`;
        },
      },
    },
  },
  scales: {
    x: {
      beginAtZero: true,
      grid: { color: colors.value.grid },
      ticks: {
        callback: (value: number | string) => formatCurrency(Number(value)),
      },
    },
    y: {
      grid: { display: false },
      ticks: {
        font: { family: "'Satoshi', sans-serif", size: 12 },
      },
    },
  },
}));
</script>

<template>
  <div class="cat-chart">
    <template v-if="loading">
      <div class="cat-chart__skeleton">
        <div v-for="i in 5" :key="i" class="cat-chart__skeleton-row">
          <div class="skeleton" style="width: 80px; height: 12px" />
          <div class="skeleton" :style="{ width: `${60 - i * 8}%`, height: '6px' }" />
        </div>
      </div>
    </template>

    <template v-else-if="empty || items.length === 0">
      <div class="cat-chart__empty">
        <q-icon name="bar_chart" size="32px" style="color: var(--pq-text-subtle)" aria-hidden="true" />
        <p>No hay gastos en este período</p>
      </div>
    </template>

    <template v-else>
      <BaseChart
        type="bar"
        :data="chartData"
        :options="chartOptions"
        :height="Math.max(200, items.length * 40)"
      />
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
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__skeleton-row {
    display: flex;
    align-items: center;
    gap: 12px;
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
