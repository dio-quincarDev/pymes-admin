<script setup lang="ts">
import { computed } from 'vue';
import type { MetricasFinancieras } from 'src/modules/core/types';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';

interface Props {
  metricas: MetricasFinancieras | null;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), { loading: false });

const { formatCurrency, formatPercent } = useNumberFormat();

interface StatItem {
  label: string;
  value: string;
  delta: number | null;
  deltaLabel: string;
}

const stats = computed<StatItem[]>(() => {
  const m = props.metricas;
  if (!m) {
    return [
      { label: 'Ingresos Totales', value: '$0.00', delta: null, deltaLabel: '—' },
      { label: 'Gastos Totales', value: '$0.00', delta: null, deltaLabel: '—' },
      { label: 'Margen Neto', value: '0.0%', delta: null, deltaLabel: '—' },
      { label: 'Margen Bruto', value: '0.0%', delta: null, deltaLabel: '—' },
      { label: 'Margen Operativo', value: '0.0%', delta: null, deltaLabel: '—' },
    ];
  }
  return [
    { label: 'Ingresos Totales', value: formatCurrency(m.totalIncome), delta: null, deltaLabel: '—' },
    { label: 'Gastos Totales', value: formatCurrency(m.totalExpenses), delta: null, deltaLabel: '—' },
    { label: 'Margen Neto', value: formatPercent(m.netMarginPct), delta: null, deltaLabel: '—' },
    { label: 'Margen Bruto', value: formatPercent(m.grossMarginPct), delta: null, deltaLabel: '—' },
    { label: 'Margen Operativo', value: formatPercent(m.operatingMarginPct), delta: null, deltaLabel: '—' },
  ];
});

function deltaClass(delta: number | null) {
  if (delta === null || delta === 0) return 'stat-strip__delta--neutral';
  return delta > 0 ? 'stat-strip__delta--up' : 'stat-strip__delta--down';
}

function deltaIcon(delta: number | null) {
  if (delta === null || delta === 0) return '—';
  return delta > 0 ? '▲' : '▼';
}
</script>

<template>
  <div class="stat-strip" role="region" aria-label="Métricas financieras del período">
    <template v-if="loading">
      <div v-for="i in 5" :key="i" class="stat-strip__item">
        <div class="stat-strip__label skeleton skeleton-text" />
        <div class="stat-strip__value skeleton skeleton-value" />
        <div class="stat-strip__delta skeleton skeleton-text" style="width: 80px" />
      </div>
    </template>
    <template v-else>
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="stat-strip__item"
        :aria-label="`${stat.label}: ${stat.value}${stat.delta !== null ? `, ${stat.delta > 0 ? 'subió' : 'bajó'} ${Math.abs(stat.delta)}%` : ''}`"
      >
        <span class="stat-strip__label">{{ stat.label }}</span>
        <span class="stat-strip__value">{{ stat.value }}</span>
        <span class="stat-strip__delta" :class="deltaClass(stat.delta)">
          {{ deltaIcon(stat.delta) }} {{ stat.deltaLabel }}
        </span>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.stat-strip {
  display: flex;
  gap: 0;
  padding: 16px 0;
  border-top: 1px solid var(--pq-border);
  border-bottom: 1px solid var(--pq-border);
  margin-bottom: 24px;

  @media (max-width: 768px) {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    padding: 16px 0;
  }

  &__item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 0 24px;
    border-right: 1px solid var(--pq-border);

    &:first-child {
      padding-left: 0;
    }

    &:last-child {
      border-right: none;
      padding-right: 0;
    }

    @media (max-width: 768px) {
      padding: 0;
      border-right: none;
      border-bottom: 1px solid var(--pq-border);
      padding-bottom: 12px;

      &:last-child {
        border-bottom: none;
      }
    }
  }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }

  &__value {
    font-family: 'Geist Mono', monospace;
    font-size: 24px;
    font-weight: 700;
    color: var(--pq-text);
    font-variant-numeric: tabular-nums;
    line-height: 1.1;
  }

  &__delta {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    font-weight: 500;

    &--up { color: var(--pq-accent-green); }
    &--down { color: var(--pq-danger); }
    &--neutral { color: var(--pq-text-muted); }
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

  &-text { height: 12px; width: 60%; }
  &-value { height: 28px; width: 80%; }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
