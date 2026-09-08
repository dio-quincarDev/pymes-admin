<script setup lang="ts">
import { computed } from 'vue';
import { useNumberFormat } from '../../composables/useNumberFormat';
import { useChartTheme } from '../../composables/useChartTheme';
import BaseChart from '../charts/BaseChart.vue';

interface DayData {
  label: string;
  ventas: number;
  costos: number;
}

const props = defineProps<{ data: DayData[]; loading?: boolean }>();
const { formatCurrency } = useNumberFormat();
const { colors } = useChartTheme();

const chartData = computed(() => ({
  labels: props.data.map(d => d.label),
  datasets: [
    {
      label: 'Ventas',
      data: props.data.map(d => d.ventas),
      backgroundColor: colors.value.bar,
      borderColor: colors.value.bar,
      borderWidth: 0,
      borderRadius: 3,
      barPercentage: 0.8,
      categoryPercentage: 0.7,
    },
    {
      label: 'Costos',
      data: props.data.map(d => d.costos),
      backgroundColor: colors.value.negative,
      borderColor: colors.value.negative,
      borderWidth: 0,
      borderRadius: 3,
      barPercentage: 0.8,
      categoryPercentage: 0.7,
    },
  ],
}));

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top' as const,
      align: 'end' as const,
      labels: {
        boxWidth: 10,
        boxHeight: 10,
        usePointStyle: true,
        pointStyle: 'rectRounded',
        padding: 16,
      },
    },
    tooltip: {
      callbacks: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        label: (context: any) => {
          const label = context.dataset.label || '';
          const value = context.parsed?.y ?? 0;
          return `${label}: ${formatCurrency(value)}`;
        },
      },
    },
  },
  scales: {
    x: {
      grid: { display: false },
    },
    y: {
      beginAtZero: true,
      ticks: {
        callback: (value: number | string) => formatCurrency(Number(value)),
      },
    },
  },
}));
</script>

<template>
  <div class="ventas-costos-chart">
    <div class="ventas-costos-chart__header">
      <span class="ventas-costos-chart__title">Ventas vs Costos (7 días)</span>
    </div>

    <template v-if="loading">
      <div class="ventas-costos-chart__skeleton">
        <div v-for="n in 7" :key="n" class="skeleton" style="height: 80px; border-radius: 3px" />
      </div>
    </template>

    <template v-else-if="data.length === 0">
      <div class="ventas-costos-chart__empty">
        Sin datos de los últimos 7 días
      </div>
    </template>

    <template v-else>
      <BaseChart
        type="bar"
        :data="chartData"
        :options="chartOptions"
        :height="200"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.ventas-costos-chart {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 8px;
  padding: 16px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 13px;
    font-weight: 600;
    color: var(--pq-text);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  &__skeleton {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
    height: 200px;
  }

  &__empty {
    text-align: center;
    padding: 32px 16px;
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    color: var(--pq-text-muted);
  }
}
</style>
