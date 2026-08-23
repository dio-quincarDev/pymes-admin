<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useNumberFormat } from '../../composables/useNumberFormat';

defineProps<{
  capitalInicial: number;
  mesesRecuperacion: number | null;
  loading?: boolean;
}>();

const router = useRouter();
const { formatCurrency } = useNumberFormat();
</script>

<template>
  <div class="inversion-card" @click="router.push('/dashboard/patrimonio')">
    <div class="inversion-card__header">
      <span class="inversion-card__title">Inversión</span>
      <q-icon name="arrow_forward_ios" size="14px" class="inversion-card__arrow" />
    </div>

    <template v-if="loading">
      <div class="skeleton" style="width: 100px; height: 20px; margin-top: 8px" />
      <div class="skeleton" style="width: 80px; height: 12px; margin-top: 6px" />
    </template>

    <template v-else>
      <div class="inversion-card__capital">
        {{ formatCurrency(capitalInicial) }}
      </div>
      <div class="inversion-card__roi">
        <template v-if="mesesRecuperacion !== null">
          Recuperación: ~{{ mesesRecuperacion }} mes{{ mesesRecuperacion !== 1 ? 'es' : '' }}
        </template>
        <template v-else>
          Sin datos de ROI
        </template>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.inversion-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: background var(--pq-motion-fast), border-color var(--pq-motion-fast);

  &:hover {
    background: var(--pq-elevated);
    border-color: var(--pq-accent-muted);
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 13px;
    font-weight: 600;
    color: var(--pq-text);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  &__arrow {
    color: var(--pq-text-subtle);
  }

  &__capital {
    font-family: 'Geist Mono', monospace;
    font-size: 22px;
    font-weight: 500;
    color: var(--pq-text);
    font-variant-numeric: tabular-nums;
  }

  &__roi {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-muted);
    margin-top: 4px;
  }
}
</style>
