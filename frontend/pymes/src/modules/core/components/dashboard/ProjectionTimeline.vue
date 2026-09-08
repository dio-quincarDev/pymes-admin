<template>
  <div class="projection">
    <BaseChart v-if="items.length" type="line" :data="chartData" :options="chartOptions" :height="200" />
    <div v-else class="text-grey-6 text-center q-pa-md">
      Sin proyecciones para este período
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ProjectionItem } from '../../types/analytics';
import { useNumberFormat } from '../../composables/useNumberFormat';
import { useChartTheme } from '../../composables/useChartTheme';
import BaseChart from '../charts/BaseChart.vue';

const props = defineProps<{ items: ProjectionItem[] }>();
const { formatCurrency } = useNumberFormat();
const { colors } = useChartTheme();

const chartData = computed(() => ({
  labels: props.items.map((d) => d.period),
  datasets: [
    {
      label: 'Proyectado',
      data: props.items.map((d) => d.projectedSpend),
      borderColor: colors.value.bar,
      backgroundColor: colors.value.area,
      fill: true,
      tension: 0.3,
      pointRadius: 5,
      pointBackgroundColor: colors.value.bar,
    },
    {
      label: 'Confianza inferior',
      data: props.items.map((d) => d.projectedSpend * (1 - d.confidence)),
      borderColor: 'transparent',
      backgroundColor: colors.value.area.replace('0.15', '0.05'),
      fill: '+1',
      pointRadius: 0,
    },
    {
      label: 'Confianza superior',
      data: props.items.map((d) => d.projectedSpend * (1 + d.confidence)),
      borderColor: 'transparent',
      backgroundColor: colors.value.area.replace('0.15', '0.05'),
      fill: false,
      pointRadius: 0,
    },
  ],
}));

const chartOptions = {
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx: { parsed: { y: number | null } }) => {
          if (ctx.parsed.y === null) return '';
          return formatCurrency(ctx.parsed.y);
        },
      },
    },
  },
  scales: {
    y: {
      ticks: {
        callback: (val: number | string) => formatCurrency(Number(val)),
      },
    },
  },
};
</script>

<style scoped>
.projection {
  width: 100%;
}
</style>
