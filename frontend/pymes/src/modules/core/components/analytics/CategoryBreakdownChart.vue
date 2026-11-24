<script setup lang="ts">
import { computed, ref, useTemplateRef, watch } from 'vue';
import type { TooltipItem } from 'chart.js';
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

// total del período = todas las categorías incluida "Otros" (el donut recorta a 5, el monto no)
const total = computed(() => props.items.reduce((s, i) => s + i.currentAmount, 0));

// paleta compartida por arcos y leyenda (mismo orden, datos mismos índices)
const palette = computed(() => [
  colors.value.abcA, colors.value.abcB, colors.value.positive, colors.value.negative, colors.value.info, colors.value.text,
]);

const chartData = computed(() => {
  const sorted = [...props.items].sort((a, b) => b.currentAmount - a.currentAmount);
  const top = sorted.slice(0, MAX_SLICES);
  const rest = sorted.slice(MAX_SLICES);
  const othersTotal = rest.reduce((s, i) => s + i.currentAmount, 0);

  const labels = top.map(i => i.category);
  const data = top.map(i => i.currentAmount);

  if (rest.length) {
    labels.push('Otros');
    data.push(othersTotal);
  }

  return {
    labels,
    datasets: [
      {
        data,
        backgroundColor: palette.value.slice(0, data.length),
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
  cutout: '72%',
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (context: TooltipItem<'doughnut'>) => {
          const value = typeof context.parsed === 'number' ? context.parsed : 0;
          const idx = context.dataIndex;
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

// leyenda en HTML (fuera del canvas): el donut queda centrado en vertical y el "Total" cae en el hueco
const legendItems = computed(() => {
  const sorted = [...props.items].sort((a, b) => b.currentAmount - a.currentAmount);
  const items = sorted.slice(0, MAX_SLICES).map((i, idx) => ({ label: i.category, color: palette.value[idx] }));
  if (sorted.length > MAX_SLICES) items.push({ label: 'Otros', color: palette.value[MAX_SLICES] });
  return items;
});

// leyenda táctil: tocar tacha la tajada (la leyenda Chart.js lo traía gratis; al moverla a HTML hay que conectarla)
const base = useTemplateRef<InstanceType<typeof BaseChart>>('base');
const hidden = ref<Set<number>>(new Set());

function toggleLegend(idx: number) {
  if (!base.value) return;
  base.value.toggleSlice(idx);
  const next = new Set(hidden.value);
  if (next.has(idx)) next.delete(idx);
  else next.add(idx);
  hidden.value = next;
}

// datos nuevos (otro mes): limpiar tachados, si no el índice oculto "persigue" a otra categoría
watch(chartData, () => {
  hidden.value.forEach((i) => base.value?.showSlice(i));
  hidden.value = new Set();
});
</script>

<template>
  <div class="cat-chart">
    <div class="cat-chart__header">
      <span class="cat-chart__title">Gastos por categoría</span>
    </div>

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
      <div class="donut-wrap">
        <BaseChart ref="base" type="doughnut" :data="chartData" :options="chartOptions" :height="260" />
        <div class="donut-center">
          <span class="donut-center__label">Total</span>
          <span class="donut-center__value">{{ formatCurrency(total) }}</span>
        </div>
      </div>
      <ul v-if="legendItems.length" class="donut-legend">
        <li v-for="(item, idx) in legendItems" :key="item.label">
          <button
            type="button"
            class="donut-legend__item"
            :class="{ 'donut-legend__item--off': hidden.has(idx) }"
            :title="item.label"
            :aria-pressed="hidden.has(idx)"
            @click="toggleLegend(idx)"
          >
            <span class="donut-legend__dot" :style="{ background: item.color }" />
            <span class="donut-legend__name">{{ item.label }}</span>
          </button>
        </li>
      </ul>
    </template>
  </div>
</template>

<style scoped lang="scss">
.cat-chart {
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-md);
  padding: 20px 24px;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;

  &__header {
    margin-bottom: 16px;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--pq-text);
    letter-spacing: -0.01em;
  }

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

.donut-wrap {
  position: relative;
  min-width: 0;
  max-width: 100%;
}

.donut-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
  text-align: center;
}

.donut-center__label {
  font-family: var(--pq-font-utility);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
}

.donut-center__value {
  font-family: var(--pq-font-display);
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--pq-text);
  line-height: 1.1;
}

.donut-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  justify-content: center;
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  min-width: 0;
}

.donut-legend__item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  min-height: 44px;
  padding: 0 8px;
  background: none;
  border: 0;
  cursor: pointer;

  &--off {
    opacity: 0.45;
  }

  &--off .donut-legend__name {
    text-decoration: line-through;
  }
}

.donut-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.donut-legend__name {
  font-family: 'Satoshi', sans-serif;
  font-size: 11px;
  color: var(--pq-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}

@media (max-width: 600px) {
  .donut-wrap :deep(.base-chart) {
    height: 200px !important;
  }
  .donut-center__value {
    font-size: 0.95rem;
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
