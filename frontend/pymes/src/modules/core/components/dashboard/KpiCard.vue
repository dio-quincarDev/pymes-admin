<template>
  <div class="kpi-card" :class="`kpi-card--${accent}`" :aria-label="`${label}: ${value}`">
    <div class="kpi-card__header">
      <span class="kpi-card__label">{{ label }}</span>
      <q-icon :name="icon" size="1.1rem" class="kpi-card__icon" aria-hidden="true" />
    </div>
    <div class="kpi-card__value">{{ value }}</div>
    <div v-if="delta" class="kpi-card__delta" :class="deltaClass">
      <q-icon :name="trend === 'up' ? 'trending_up' : 'trending_down'" size="0.85rem" aria-hidden="true" />
      {{ delta }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  label: string;
  value: string;
  delta?: string;
  trend?: 'up' | 'down';
  icon: string;
  accent?: 'copper' | 'sage' | 'gold' | 'negative' | 'positive';
}

const props = withDefaults(defineProps<Props>(), {
  delta: '',
  accent: 'copper',
});

const deltaClass = computed(() => {
  if (props.trend === 'up') return 'kpi-card__delta--up';
  if (props.trend === 'down') return 'kpi-card__delta--down';
  return '';
});
</script>

<style scoped lang="scss">
.kpi-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 6px;
  padding: 1.25rem 1.5rem;
  position: relative;
  overflow: hidden;
  transition: transform 160ms cubic-bezier(0.4, 0, 0.2, 1), box-shadow 160ms cubic-bezier(0.4, 0, 0.2, 1);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    border-radius: 0 2px 2px 0;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--pq-shadow-md);
  }

  &--copper::before { background: var(--pq-accent); }
  &--sage::before { background: var(--pq-success); }
  &--gold::before { background: var(--pq-warning); }
  &--negative::before { background: var(--pq-danger); }
  &--positive::before { background: var(--pq-success); }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.75rem;
  }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 0.75rem;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__icon {
    color: var(--pq-accent-muted);
  }

  &__value {
    font-family: 'Geist', sans-serif;
    font-size: 2rem;
    font-weight: 700;
    color: var(--pq-text);
    line-height: 1;
  }

  &__delta {
    font-family: 'Satoshi', sans-serif;
    font-size: 0.75rem;
    margin-top: 0.5rem;
    display: flex;
    align-items: center;
    gap: 0.25rem;

    &--up { color: var(--pq-success); }
    &--down { color: var(--pq-danger); }
  }
}
</style>
