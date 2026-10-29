<script setup lang="ts">
import { formatCurrency } from 'src/utils/format';

defineProps<{
  total: number;
  count: number;
  average: number;
}>();
</script>

<template>
  <q-card dark class="stat-strip" role="status" aria-live="polite">
    <div class="stat-strip__grid">
      <div class="summary-item">
        <div class="summary-label">Total mes</div>
        <div class="summary-value summary-value--total">{{ formatCurrency(total) }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">Registros</div>
        <div class="summary-value">{{ count }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">Venta promedio</div>
        <div class="summary-value">{{ formatCurrency(average) }}</div>
        <div class="summary-sub">por registro, este mes</div>
      </div>
    </div>
  </q-card>
</template>

<style scoped>
/* shortcut: top 56px = min-height del sticky month-pager, para no solaparse */
.stat-strip {
  position: sticky;
  top: 56px;
  z-index: 9;
  border-radius: var(--pq-radius-md);
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  box-shadow: var(--pq-shadow-subtle);
}

.stat-strip__grid {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 8px;
  min-width: 0;
}

.summary-label {
  font-family: var(--pq-font-utility);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
}

.summary-value {
  font-family: var(--pq-font-utility);
  font-size: 1.1rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-text);
}

.summary-value--total {
  color: var(--pq-accent);
}

.summary-sub {
  font-family: var(--pq-font-utility);
  font-size: 10px;
  color: var(--pq-text-muted);
}

/* mobile: grid 2 cols, total + promedio arriba y más grandes (la respuesta primero) */
@media (max-width: 639px) {
  .stat-strip__grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  /* Total mes es el dato decisivo: domina en móvil */
  .summary-value--total {
    font-size: 1.35rem;
  }
}
</style>
