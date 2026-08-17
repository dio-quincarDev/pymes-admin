<script setup lang="ts">
import { computed } from 'vue';
import type { GastoPorCategoria } from 'src/modules/core/composables/useFinancialDashboard';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import { useChartTheme } from 'src/modules/core/composables/useChartTheme';
import BaseChart from 'src/modules/core/components/charts/BaseChart.vue';

interface Props {
  gastos: GastoPorCategoria[];
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), { loading: false });

const { formatCurrency } = useNumberFormat();
const { colors } = useChartTheme();

const MAX_ITEMS = 6;

const chartData = computed(() => {
  const items = props.gastos.slice(0, MAX_ITEMS);
  const othersTotal = props.gastos
    .slice(MAX_ITEMS)
    .reduce((sum, item) => sum + item.total, 0);
  
  const labels = items.map(i => i.categoria);
  const data = items.map(i => i.total);
  
  if (othersTotal > 0) {
    labels.push('Otros');
    data.push(othersTotal);
  }

  return {
    labels,
    datasets: [
      {
        data,
        backgroundColor: [
          colors.value.abcA,
          colors.value.abcB,
          colors.value.abcC,
          colors.value.positive,
          colors.value.negative,
          colors.value.info,
          colors.value.text,
        ],
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
  cutout: '60%',
  plugins: {
    legend: {
      display: true,
      position: 'right' as const,
      labels: {
        boxWidth: 10,
        boxHeight: 10,
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 12,
      },
    },
    tooltip: {
      callbacks: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        label: (context: any) => {
          const value = context.parsed ?? 0;
          const total = (context.dataset.data as number[]).reduce((a: number, b: number) => a + b, 0);
          const pct = total > 0 ? ((value / total) * 100).toFixed(1) : '0';
          return `${context.label}: ${formatCurrency(value)} (${pct}%)`;
        },
      },
    },
  },
}));
</script>

<template>
  <div class="expense-breakdown">
    <h3 class="expense-breakdown__title">Desglose de Gastos</h3>

    <template v-if="loading">
      <div class="expense-breakdown__skeleton">
        <div class="skeleton" style="width: 120px; height: 120px; border-radius: 50%" />
        <div class="expense-breakdown__skeleton-legend">
          <div v-for="i in 4" :key="i" class="skeleton" style="width: 80px; height: 12px" />
        </div>
      </div>
    </template>

    <template v-else-if="gastos.length === 0">
      <div class="expense-breakdown__empty">
        <q-icon name="receipt_long" size="32px" color="positive" aria-hidden="true" />
        <p>Sin gastos registrados en este período</p>
      </div>
    </template>

    <template v-else>
      <BaseChart
        type="doughnut"
        :data="chartData"
        :options="chartOptions"
        :height="200"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.expense-breakdown {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin: 0 0 4px;
  }

  &__skeleton {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 16px 0;
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
    padding: 24px 0;
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
