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
      type: 'bar' as const,
      data: props.data.map(d => d.ventas),
      backgroundColor: colors.value.bar,
      borderColor: 'transparent',
      borderWidth: 0,
      borderRadius: 2,
      barPercentage: 0.55,
      categoryPercentage: 0.7,
    },
    {
      label: 'Costos',
      type: 'line' as const,
      data: props.data.map(d => d.costos),
      borderColor: colors.value.line,
      backgroundColor: 'transparent',
      borderWidth: 1.5,
      tension: 0.4,
      pointRadius: 0,
      pointHoverRadius: 4,
      pointBackgroundColor: colors.value.line,
      pointBorderColor: colors.value.tooltipBg,
      pointBorderWidth: 2,
      fill: false,
    },
  ],
}));

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    mode: 'index' as const,
    intersect: false,
  },
  plugins: {
    legend: {
      display: true,
      position: 'bottom' as const,
      align: 'start' as const,
      labels: {
        boxWidth: 12,
        boxHeight: 2,
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 16,
        font: { family: "'Satoshi', sans-serif", size: 11, weight: 500 },
        color: colors.value.text,
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
      border: { display: false },
      ticks: {
        color: colors.value.text,
        font: { family: "'Geist Mono', monospace", size: 10 },
      },
    },
    y: {
      beginAtZero: true,
      border: { display: false },
      grid: {
        color: colors.value.grid,
        lineWidth: 1,
      },
      ticks: {
        color: colors.value.text,
        font: { family: "'Geist Mono', monospace", size: 10 },
        callback: (value: number | string) => formatCurrency(Number(value)),
        maxTicksLimit: 5,
      },
    },
  },
}));
</script>

<template>
  <div class="ventas-costos-chart">
    <div class="ventas-costos-chart__header">
      <span class="ventas-costos-chart__title">Ventas vs Costos</span>
      <span class="ventas-costos-chart__subtitle">Últimos 7 días</span>
    </div>

    <template v-if="loading">
      <div class="ventas-costos-chart__skeleton">
        <div v-for="n in 7" :key="n" class="skeleton" style="height: 80px; border-radius: 2px" />
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
        :height="220"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.ventas-costos-chart {
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-md);
  padding: 20px 24px;

  &__header {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin-bottom: 20px;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--pq-text);
    letter-spacing: -0.01em;
  }

  &__subtitle {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    color: var(--pq-text-subtle);
  }

  &__skeleton {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
    height: 220px;
    align-items: end;
  }

  &__empty {
    text-align: center;
    padding: 48px 16px;
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    color: var(--pq-text-muted);
  }
}
</style>
