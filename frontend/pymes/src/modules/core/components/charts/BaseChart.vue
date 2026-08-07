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

function createChart() {
  if (!canvas.value) return;
  chart.value?.destroy();

  const defaultScales = props.type !== 'doughnut'
    ? {
        x: {
          ticks: { color: '#8A9E99', font: { size: 10 } },
          grid: { color: 'rgba(138, 158, 153, 0.08)' },
        },
        y: {
          ticks: { color: '#8A9E99', font: { size: 10 } },
          grid: { color: 'rgba(138, 158, 153, 0.08)' },
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
          labels: { color: '#8A9E99', font: { size: 11 } },
        },
        tooltip: {
          backgroundColor: '#1B2624',
          titleColor: '#E2E8E4',
          bodyColor: '#E2E8E4',
          borderColor: 'rgba(163, 120, 94, 0.3)',
          borderWidth: 1,
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
