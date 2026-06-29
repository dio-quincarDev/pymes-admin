<template>
  <div class="kpi-card" :class="`kpi-card--${accent}`">
    <div class="kpi-card__header">
      <span class="kpi-card__label">{{ label }}</span>
      <q-icon :name="icon" size="1.1rem" class="kpi-card__icon" />
    </div>
    <div class="kpi-card__value">{{ value }}</div>
    <div v-if="delta" class="kpi-card__delta" :class="deltaClass">
      <q-icon :name="trend === 'up' ? 'trending_up' : 'trending_down'" size="0.85rem" />
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
  background: rgba(27, 38, 36, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(113, 131, 127, 0.05);
  border-radius: 8px;
  padding: 1.25rem 1.5rem;
  position: relative;
  overflow: hidden;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

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
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }

  &--copper::before { background: #A3785E; }
  &--sage::before { background: #2D5A27; }
  &--gold::before { background: #C5A059; }
  &--negative::before { background: #e94560; }
  &--positive::before { background: #2D5A27; }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.75rem;
  }

  &__label {
    font-size: 0.75rem;
    font-weight: 500;
    color: #8A9E99;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__icon {
    color: rgba(163, 120, 94, 0.4);
  }

  &__value {
    font-family: 'Outfit', sans-serif;
    font-size: 2rem;
    font-weight: 700;
    color: #E2E8E4;
    line-height: 1;
  }

  &__delta {
    font-size: 0.75rem;
    margin-top: 0.5rem;
    display: flex;
    align-items: center;
    gap: 0.25rem;

    &--up { color: #2D5A27; }
    &--down { color: #e94560; }
  }
}
</style>
