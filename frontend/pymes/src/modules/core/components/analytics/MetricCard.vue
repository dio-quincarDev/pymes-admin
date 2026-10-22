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
    <div class="skeleton skeleton-text" style="width: 60px; height: 12px" />
    <div class="skeleton skeleton-value" style="width: 80px; height: 32px; margin-top: 12px" />
  </div>

  <div
    v-else
    class="metric-card"
    :class="`metric-card--${accent}`"
    :aria-label="`${label}: ${value}`"
  >
    <div class="metric-card__label">{{ label }}</div>
    <div class="metric-card__value">{{ value }}</div>
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
  padding: 1.25rem 1.5rem;
  transition: transform 160ms cubic-bezier(0.4, 0, 0.2, 1), box-shadow 160ms cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--pq-shadow-md);
  }

  &--gold { border-left: 3px solid var(--pq-accent); }
  &--green { border-left: 3px solid var(--pq-success); }
  &--red { border-left: 3px solid var(--pq-danger); }
  &--blue { border-left: 3px solid var(--pq-info); }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 0.75rem;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__value {
    font-family: 'Geist', sans-serif;
    font-size: 2rem;
    font-weight: 700;
    color: var(--pq-text);
    line-height: 1;
    margin-top: 0.75rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__delta {
    font-family: 'Satoshi', sans-serif;
    font-size: 0.75rem;
    font-weight: 500;
    margin-top: 0.5rem;

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
