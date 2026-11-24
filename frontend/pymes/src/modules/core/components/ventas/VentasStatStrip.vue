<script setup lang="ts">
import { formatCurrency } from 'src/utils/format';
import type { MetodoPago } from '../../types';
import { labelMetodoPago } from '../../utils/metodosPago';

const props = defineProps<{
  total: number;
  count: number;
  average: number;
  metodoTotals?: Partial<Record<MetodoPago, number>>;
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
    <div v-if="props.metodoTotals && Object.keys(props.metodoTotals).length" class="stat-strip__methods">
      <span v-for="(amt, metodo) in props.metodoTotals" :key="metodo" class="method-chip">
        {{ labelMetodoPago(metodo as MetodoPago) }}: {{ formatCurrency(amt as number) }}
      </span>
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

.stat-strip__methods {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 16px 12px;
  border-top: 1px solid rgba(77, 112, 99, 0.2);
}

.method-chip {
  font-family: var(--pq-font-utility);
  font-size: 11px;
  padding: 4px 8px;
  border-radius: var(--pq-radius-sm);
  background: rgba(77, 112, 99, 0.12);
  border: 1px solid rgba(77, 112, 99, 0.25);
  color: var(--pq-text);
  white-space: nowrap;
}
</style>
