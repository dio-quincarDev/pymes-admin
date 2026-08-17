<template>
  <div ref="container" class="base-chart">
    <canvas ref="canvas" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef } from 'vue';
import {
  Chart,
  BarController,
  LineController,
  DoughnutController,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { useChartTheme } from '../../composables/useChartTheme';

Chart.register(
  BarController,
  LineController,
  DoughnutController,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
  Filler,
);

interface Props {
  type: 'bar' | 'line' | 'doughnut';
  data: Chart['data'];
  options?: Chart['options'];
  height?: number;
}

const props = withDefaults(defineProps<Props>(), {
  options: () => ({}),
  height: 300,
});

const canvas = ref<HTMLCanvasElement>();
const container = ref<HTMLDivElement>();
const chart = shallowRef<Chart>();
const { colors } = useChartTheme();

function createChart() {
  if (!canvas.value) return;
  chart.value?.destroy();

  const defaultScales = props.type !== 'doughnut'
    ? {
        x: {
          ticks: { color: colors.value.text, font: { family: "'Geist Mono', monospace", size: 10 } },
          grid: { color: colors.value.grid },
        },
        y: {
          ticks: { color: colors.value.text, font: { family: "'Geist Mono', monospace", size: 10 } },
          grid: { color: colors.value.grid },
        },
      }
    : {};

  chart.value = new Chart(canvas.value, {
    type: props.type,
    data: props.data,
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          labels: { color: colors.value.text, font: { family: "'Geist', sans-serif", size: 11 } },
        },
        tooltip: {
          backgroundColor: colors.value.tooltipBg,
          titleColor: getComputedStyle(document.documentElement).getPropertyValue('--pq-text').trim(),
          bodyColor: getComputedStyle(document.documentElement).getPropertyValue('--pq-text').trim(),
          borderColor: colors.value.tooltipBorder,
          borderWidth: 1,
          titleFont: { family: "'Geist', sans-serif", weight: 600 },
          bodyFont: { family: "'Satoshi', sans-serif" },
          padding: 12,
          cornerRadius: 6,
        },
      },
      scales: defaultScales,
      ...(props.options as Record<string, unknown>),
    },
  });
}

onMounted(() => {
  createChart();
  if (container.value) {
    resizeObserver.observe(container.value);
  }
});

watch(() => [props.data, props.options], createChart, { deep: true });

onUnmounted(() => {
  chart.value?.destroy();
  resizeObserver.disconnect();
});

// ponytail: ResizeObserver for canvas redraw on container resize
const resizeObserver = new ResizeObserver(() => {
  chart.value?.resize();
});
</script>

<style scoped>
.base-chart {
  position: relative;
  width: 100%;
}
</style>
