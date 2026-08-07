<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  label: string;
  value: string;
  delta?: number | undefined;
  deltaLabel?: string;
  loading?: boolean;
  accent?: 'gold' | 'green' | 'red' | 'blue';
}

const props = withDefaults(defineProps<Props>(), {
  delta: undefined,
  deltaLabel: '',
  loading: false,
  accent: 'gold',
});

const deltaArrow = computed(() => {
  if (props.delta === undefined || props.delta === 0) return '';
  return props.delta > 0 ? '↑' : '↓';
});

const deltaClass = computed(() => {
  if (props.delta === undefined || props.delta === 0) return '';
  return props.delta > 0 ? 'metric-card__delta--up' : 'metric-card__delta--down';
});
</script>

<template>
  <div v-if="loading" class="metric-card">
    <div class="skeleton skeleton-value" style="width: 80px; height: 24px" />
    <div class="skeleton skeleton-text" style="width: 60px; height: 12px; margin-top: 6px" />
  </div>

  <div
    v-else
    class="metric-card"
    :class="`metric-card--${accent}`"
    :aria-label="`${label}: ${value}`"
  >
    <div class="metric-card__value">{{ value }}</div>
    <div class="metric-card__label">{{ label }}</div>
    <div v-if="delta !== undefined" class="metric-card__delta" :class="deltaClass">
      {{ deltaArrow }} {{ delta > 0 ? '+' : '' }}{{ delta }}% {{ deltaLabel }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.metric-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 6px;
  padding: 12px 16px;
  transition: background var(--pq-motion-fast);

  &:hover {
    background: var(--pq-elevated);
  }

  &--gold { border-left: 3px solid var(--pq-accent); }
  &--green { border-left: 3px solid var(--pq-success); }
  &--red { border-left: 3px solid var(--pq-danger); }
  &--blue { border-left: 3px solid var(--pq-info); }

  &__value {
    font-family: 'Geist Mono', monospace;
    font-size: 24px;
    font-weight: 700;
    color: var(--pq-text);
    line-height: 1;
    font-variant-numeric: tabular-nums;
  }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    font-weight: 400;
    color: var(--pq-text-muted);
    margin-top: 6px;
  }

  &__delta {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    margin-top: 4px;

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
