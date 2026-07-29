<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';

interface Props {
  label: string;
  value: string;
  delta?: number | undefined;
  deltaLabel?: string;
  trend?: number[] | undefined;
  variant?: 'default' | 'compact';
  loading?: boolean;
  accent?: 'gold' | 'green' | 'red' | 'blue';
}

const props = withDefaults(defineProps<Props>(), {
  delta: undefined,
  deltaLabel: '',
  variant: 'default',
  loading: false,
  accent: 'gold',
});

const mounted = ref(false);
onMounted(() => { mounted.value = true; });

const deltaArrow = computed(() => {
  if (props.delta === undefined || props.delta === 0) return '';
  return props.delta > 0 ? '↑' : '↓';
});

const deltaClass = computed(() => {
  if (props.delta === undefined || props.delta === 0) return '';
  return props.delta > 0 ? 'kpi-card__delta--up' : 'kpi-card__delta--down';
});

const sparklinePoints = computed(() => {
  if (!props.trend || props.trend.length < 2) return '';
  const values = props.trend;
  const max = Math.max(...values);
  const min = Math.min(...values);
  const range = max - min || 1;
  const w = 80;
  const h = 28;
  const step = w / (values.length - 1);
  return values
    .map((v, i) => {
      const x = i * step;
      const y = h - ((v - min) / range) * h;
      return `${x},${y}`;
    })
    .join(' ');
});

const sparklineArea = computed(() => {
  if (!props.trend || props.trend.length < 2) return '';
  const points = sparklinePoints.value;
  return `${points} 80,28 0,28`;
});
</script>

<template>
  <div
    v-if="loading"
    class="kpi-card"
    :class="`kpi-card--${variant}`"
  >
    <div class="skeleton skeleton-text" style="width: 80px; height: 12px" />
    <div class="skeleton skeleton-value" style="width: 100px; height: 28px; margin-top: 8px" />
    <div v-if="variant === 'default'" class="skeleton skeleton-text" style="width: 60px; height: 12px; margin-top: 12px" />
  </div>

  <div
    v-else
    class="kpi-card"
    :class="[`kpi-card--${variant}`, `kpi-card--${accent}`]"
    :aria-label="`${label}: ${value}`"
  >
    <div class="kpi-card__top">
      <span class="kpi-card__label">{{ label }}</span>
      <svg
        v-if="variant === 'default' && trend && trend.length >= 2"
        class="kpi-card__sparkline"
        viewBox="0 0 80 28"
        aria-hidden="true"
        preserveAspectRatio="none"
      >
        <polygon :points="sparklineArea" fill="currentColor" opacity="0.08" />
        <polyline
          :points="sparklinePoints"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </div>
    <div class="kpi-card__value">{{ value }}</div>
    <div v-if="delta !== undefined" class="kpi-card__delta" :class="deltaClass">
      {{ deltaArrow }} {{ delta > 0 ? '+' : '' }}{{ delta }}% {{ deltaLabel }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.kpi-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 6px;
  padding: 16px;
  transition: background var(--pq-motion-fast);

  &:hover {
    background: var(--pq-elevated);
  }

  &--default {
    padding: 16px;
  }

  &--compact {
    padding: 12px 16px;
  }

  // Accent left border
  &--gold { border-left: 3px solid var(--pq-accent); }
  &--green { border-left: 3px solid var(--pq-success); }
  &--red { border-left: 3px solid var(--pq-danger); }
  &--blue { border-left: 3px solid var(--pq-info); }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.06em;
  }

  &__sparkline {
    width: 80px;
    height: 28px;
    color: var(--pq-accent);
    flex-shrink: 0;
  }

  &__value {
    font-family: 'Geist Mono', monospace;
    font-size: 24px;
    font-weight: 500;
    color: var(--pq-text);
    line-height: 1;
    font-variant-numeric: tabular-nums;
  }

  &--compact &__value {
    font-size: 20px;
  }

  &__delta {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    font-weight: 500;
    margin-top: 8px;

    &--up { color: var(--pq-success); }
    &--down { color: var(--pq-danger); }
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
