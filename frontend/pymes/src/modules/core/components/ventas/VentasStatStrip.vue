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
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  min-width: 0;
}

/* Mobile: "Venta promedio" ocupa ambas columnas, separado por hairline, centrado */
.summary-item:nth-child(3) {
  grid-column: 1 / -1;
  border-top: 1px solid rgba(77, 112, 99, 0.2);
  align-items: center;
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
  font-size: 1.15rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-text);
}

.summary-value--total {
  color: var(--pq-accent);
  font-size: 1.35rem;
}

.summary-sub {
  font-family: var(--pq-font-utility);
  font-size: 10px;
  color: var(--pq-text-muted);
}

/* Desktop: 3 columnas iguales con hairlines verticales, sin amontonar */
@media (min-width: 640px) {
  .stat-strip__grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 0;
  }

  .summary-item {
    padding: 16px 24px;
  }

  .summary-item:not(:first-child) {
    border-left: 1px solid rgba(77, 112, 99, 0.2);
  }

  .summary-item:nth-child(3) {
    border-top: 0;
    grid-column: auto; /* reset del span full-row del mobile */
    align-items: flex-start; /* reset del centrado mobile */
  }

  .summary-value {
    font-size: 1.25rem;
  }

  .summary-value--total {
    font-size: 1.45rem;
  }
}
</style>
