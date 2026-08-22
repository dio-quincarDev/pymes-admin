<template>
  <div ref="container" class="base-chart">
    <canvas ref="canvas" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef, computed } from 'vue';
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

const canvas = shallowRef<HTMLCanvasElement>();
const container = shallowRef<HTMLDivElement>();
const chart = shallowRef<Chart>();
const { colors } = useChartTheme();

// ponytail: reactive — responds if user toggles prefers-reduced-motion mid-session
const reducedMotion = ref(window.matchMedia('(prefers-reduced-motion: reduce)').matches);

// ponytail: per-instance observer — disconnect() on unmount only affects this chart
const resizeObserver = new ResizeObserver(() => {
  chart.value?.resize();
});

const animationConfig = computed(() =>
  reducedMotion.value
    ? false
    : {
        duration: 800,
        easing: 'easeOutQuart' as const,
        // Stagger entrance: bars/arcs arrive left→right, 60ms apart, capped at 600ms
        // (data materializing, not decoration). Cap keeps sparklines snappy.
        delay: (ctx: { type: string; dataIndex: number }) =>
          ctx.type === 'data' ? Math.min(ctx.dataIndex * 60, 600) : 0,
      },
);

function createChart() {
  if (!canvas.value) return;
  chart.value?.destroy();

  const textColor = colors.value.text;
  const gridColor = colors.value.grid;
  const tooltipBg = colors.value.tooltipBg;
  const tooltipBorder = colors.value.tooltipBorder;
  const cssText = getComputedStyle(document.documentElement).getPropertyValue('--pq-text').trim();

  const defaultScales = props.type !== 'doughnut'
    ? {
        x: {
          ticks: { color: textColor, font: { family: "'Geist Mono', monospace", size: 10 } },
          grid: { color: gridColor },
        },
        y: {
          ticks: { color: textColor, font: { family: "'Geist Mono', monospace", size: 10 } },
          grid: { color: gridColor },
        },
      }
    : {};

  chart.value = new Chart(canvas.value, {
    type: props.type,
    data: props.data,
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: animationConfig.value,
      plugins: {
        legend: {
          labels: { color: textColor, font: { family: "'Geist', sans-serif", size: 11 } },
        },
        tooltip: {
          backgroundColor: tooltipBg,
          titleColor: cssText,
          bodyColor: cssText,
          borderColor: tooltipBorder,
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

// Data changes animate in place (Chart.js morphs to new values); options changes recreate.
watch(() => props.data, (data) => {
  if (chart.value) {
    chart.value.data = data;
    chart.value.update();
    return;
  }
  createChart();
}, { deep: true });

watch(() => props.options, createChart, { deep: true });

onUnmounted(() => {
  chart.value?.destroy();
  resizeObserver.disconnect();
});
</script>

<style scoped>
.base-chart {
  position: relative;
  width: 100%;
  /* Container entrance mirrors chart draw-in; reduced-motion block in app.scss kills it */
  animation: baseChartIn 400ms cubic-bezier(0.4, 0, 0.2, 1) both;
}

@keyframes baseChartIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
