<script setup lang="ts">
interface Kpi {
  label: string;
  value: string;
  delta?: number;
  accent: 'gold' | 'green' | 'red' | 'blue';
}

defineProps<{ kpis: Kpi[]; loading?: boolean }>();
</script>

<template>
  <div class="kpi-strip">
    <template v-if="loading">
      <div v-for="n in 4" :key="n" class="kpi-strip__item">
        <div class="skeleton" style="width: 60px; height: 10px" />
        <div class="skeleton" style="width: 80px; height: 20px; margin-top: 6px" />
      </div>
    </template>
    <template v-else>
      <div
        v-for="kpi in kpis"
        :key="kpi.label"
        class="kpi-strip__item"
        :class="`kpi-strip__item--${kpi.accent}`"
      >
        <span class="kpi-strip__label">{{ kpi.label }}</span>
        <span class="kpi-strip__value">{{ kpi.value }}</span>
        <span v-if="kpi.delta !== undefined" class="kpi-strip__delta">
          {{ kpi.delta > 0 ? '+' : '' }}{{ kpi.delta }}%
        </span>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.kpi-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  &__item {
    background: var(--pq-surface);
    border: 1px solid var(--pq-border);
    border-radius: 6px;
    padding: 12px 14px;
    display: flex;
    flex-direction: column;
    gap: 4px;
    border-left: 3px solid transparent;

    &--gold { border-left-color: var(--pq-accent); }
    &--green { border-left-color: var(--pq-success); }
    &--red { border-left-color: var(--pq-danger); }
    &--blue { border-left-color: var(--pq-info); }
  }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 10px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--pq-text-muted);
  }

  &__value {
    font-family: 'Geist Mono', monospace;
    font-size: 20px;
    font-weight: 500;
    color: var(--pq-text);
    font-variant-numeric: tabular-nums;
  }

  &__delta {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-success);
  }
}
</style>
