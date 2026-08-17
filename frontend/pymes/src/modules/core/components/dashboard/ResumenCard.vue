<script setup lang="ts">
import { useNumberFormat } from '../../composables/useNumberFormat';

defineProps<{
  ventasHoy: number;
  costosDia: number;
  margen: number;
  cantidadVentas: number;
  loading?: boolean;
}>();

const { formatCurrency } = useNumberFormat();
</script>

<template>
  <div class="resumen-card">
    <div class="resumen-card__header">
      <span class="resumen-card__title">Resumen de hoy</span>
    </div>

    <template v-if="loading">
      <div v-for="n in 3" :key="n" class="resumen-card__row">
        <div class="skeleton" style="width: 80px; height: 12px" />
        <div class="skeleton" style="width: 60px; height: 14px" />
      </div>
    </template>

    <template v-else>
      <div class="resumen-card__row">
        <span class="resumen-card__label">Ventas</span>
        <span class="resumen-card__value resumen-card__value--green">
          {{ formatCurrency(ventasHoy) }}
          <span class="resumen-card__count">({{ cantidadVentas }} registro{{ cantidadVentas !== 1 ? 's' : '' }})</span>
        </span>
      </div>
      <div class="resumen-card__row">
        <span class="resumen-card__label">Costos</span>
        <span class="resumen-card__value resumen-card__value--red">
          {{ formatCurrency(costosDia) }}
        </span>
      </div>
      <div class="resumen-card__divider" />
      <div class="resumen-card__row resumen-card__row--total">
        <span class="resumen-card__label">Margen</span>
        <span
          class="resumen-card__value"
          :class="margen >= 0 ? 'resumen-card__value--green' : 'resumen-card__value--red'"
        >
          {{ formatCurrency(margen) }}
        </span>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.resumen-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 8px;
  padding: 16px;

  &__header {
    margin-bottom: 14px;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 13px;
    font-weight: 600;
    color: var(--pq-text);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  &__row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 0;

    &--total {
      padding-top: 8px;
    }
  }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    color: var(--pq-text-muted);
  }

  &__value {
    font-family: 'Geist Mono', monospace;
    font-size: 15px;
    font-weight: 500;
    font-variant-numeric: tabular-nums;

    &--green { color: var(--pq-success); }
    &--red { color: var(--pq-danger); }
  }

  &__count {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    color: var(--pq-text-subtle);
    margin-left: 4px;
  }

  &__divider {
    height: 1px;
    background: var(--pq-border);
    margin: 6px 0;
  }
}
</style>
