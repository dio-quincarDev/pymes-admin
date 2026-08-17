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
  return props.delta > 0 ? 'kpi-card__delta--up' : 'kpi-card__delta--down';
});
</script>

<template>
  <div
    v-if="loading"
    class="kpi-card"
  >
    <div class="skeleton skeleton-text" style="width: 80px; height: 12px" />
    <div class="skeleton skeleton-value" style="width: 100px; margin-top: 8px" />
    <div class="skeleton skeleton-text" style="width: 60px; margin-top: 12px" />
  </div>

  <div
    v-else
    class="kpi-card"
    :class="`kpi-card--${accent}`"
    :aria-label="`${label}: ${value}`"
  >
    <div class="kpi-card__top">
      <span class="kpi-card__label">{{ label }}</span>
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
    border-color: var(--pq-accent-muted);
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

  &__value {
    font-family: 'Geist Mono', monospace;
    font-size: 24px;
    font-weight: 500;
    color: var(--pq-text);
    line-height: 1;
    font-variant-numeric: tabular-nums;
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
</style>
