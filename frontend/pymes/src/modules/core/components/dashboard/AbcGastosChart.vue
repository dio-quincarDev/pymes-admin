<template>
  <div class="abc-chart" :style="{ height: height + 'px' }">
    <BaseChart v-if="chartData" type="bar" :data="chartData" :options="chartOptions" :height="height" />
    <div v-else class="abc-chart__empty text-grey-6 text-center q-pa-lg">
      Sin datos de gastos para este período
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { AbcItem } from '../../types/analytics';
import { useChartTheme } from '../../composables/useChartTheme';
import BaseChart from '../charts/BaseChart.vue';

interface Props {
  data: AbcItem[];
  height?: number;
}

const props = withDefaults(defineProps<Props>(), {
  height: 300,
});

const { colors } = useChartTheme();

const chartData = computed(() => {
  if (!props.data.length) return null;
  const sorted = [...props.data].sort((a, b) => b.spend - a.spend);
  return {
    labels: sorted.map((d) => d.productName),
    datasets: [
      {
        label: 'Gasto',
        data: sorted.map((d) => d.spend),
        backgroundColor: sorted.map((d) => {
          if (d.category === 'A') return colors.value.abcA;
          if (d.category === 'B') return colors.value.abcB;
          return colors.value.abcC;
        }),
        borderRadius: 4,
        yAxisID: 'y',
        order: 2,
      },
      {
        label: 'Acumulado %',
        data: sorted.map((d) => d.cumulativePct * 100),
        type: 'line' as const,
        borderColor: colors.value.negative,
        backgroundColor: 'transparent',
        pointRadius: 3,
        pointBackgroundColor: colors.value.negative,
        tension: 0.3,
        yAxisID: 'y1',
        order: 1,
      },
    ],
  };
});

const chartOptions = computed(() => ({
  plugins: {
    legend: { position: 'bottom' as const },
  },
  scales: {
    y: {
      position: 'left' as const,
      title: { display: true, text: 'Gasto (S/)', color: colors.value.text },
    },
    y1: {
      position: 'right' as const,
      min: 0,
      max: 100,
      title: { display: true, text: 'Acumulado %', color: colors.value.text },
      grid: { drawOnChartArea: false },
    },
  },
}));
</script>

<style scoped lang="scss">
.abc-chart {
  width: 100%;

  &__empty {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
