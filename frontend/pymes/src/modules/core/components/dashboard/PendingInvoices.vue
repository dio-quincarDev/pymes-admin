<script setup lang="ts">
import type { Factura } from 'src/modules/core/types';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';

interface Props {
  facturas: Factura[];
  loading?: boolean;
}

withDefaults(defineProps<Props>(), { loading: false });

const { formatCurrency } = useNumberFormat();

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  return d.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' });
}

function statusClass(status: string) {
  if (status === 'PENDIENTE') return 'pending-invoices__badge--warning';
  if (status === 'VENCIDA') return 'pending-invoices__badge--danger';
  return 'pending-invoices__badge--info';
}
</script>

<template>
  <div class="pending-invoices">
    <h3 class="pending-invoices__title">Facturas Pendientes</h3>

    <template v-if="loading">
      <div v-for="i in 3" :key="i" class="pending-invoices__row">
        <div class="pending-invoices__info">
          <div class="skeleton skeleton-text" style="width: 60%" />
          <div class="skeleton skeleton-text" style="width: 40%" />
        </div>
        <div class="skeleton skeleton-text" style="width: 70px" />
      </div>
    </template>

    <template v-else-if="facturas.length === 0">
      <div class="pending-invoices__empty">
        <q-icon name="check_circle" size="32px" color="positive" aria-hidden="true" />
        <p>No hay facturas pendientes</p>
      </div>
    </template>

    <ul v-else class="pending-invoices__list" role="list">
      <li
        v-for="factura in facturas"
        :key="factura.id"
        class="pending-invoices__row"
        :aria-label="`${factura.providerName}, factura ${factura.invoiceNumber}, ${formatCurrency(factura.total)}, ${factura.status}`"
      >
        <div class="pending-invoices__info">
          <span class="pending-invoices__supplier">{{ factura.providerName }}</span>
          <span class="pending-invoices__number">{{ factura.invoiceNumber }}</span>
        </div>
        <div class="pending-invoices__meta">
          <span class="pending-invoices__total">{{ formatCurrency(factura.total) }}</span>
          <span class="pending-invoices__date">{{ formatDate(factura.issueDate) }}</span>
        </div>
        <span class="pending-invoices__badge" :class="statusClass(factura.status)">
          {{ factura.status }}
        </span>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.pending-invoices {
  display: flex;
  flex-direction: column;

  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin: 0 0 12px;
  }

  &__list {
    list-style: none;
    padding: 0;
    margin: 0;
    max-height: 240px;
    overflow-y: auto;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;
    border-bottom: 1px solid var(--pq-border);
    transition: background var(--pq-motion-fast);

    &:last-child { border-bottom: none; }
    &:hover { background: rgba(255, 255, 255, 0.02); }
  }

  &__info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__supplier {
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    font-weight: 500;
    color: var(--pq-text);
  }

  &__number {
    font-family: 'Geist Mono', monospace;
    font-size: 12px;
    font-weight: 400;
    color: var(--pq-text-muted);
  }

  &__meta {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 2px;
    flex-shrink: 0;
  }

  &__total {
    font-family: 'Geist Mono', monospace;
    font-size: 14px;
    font-weight: 500;
    color: var(--pq-text);
    font-variant-numeric: tabular-nums;
  }

  &__date {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    font-weight: 400;
    color: var(--pq-text-muted);
  }

  &__badge {
    font-family: 'Satoshi', sans-serif;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.04em;
    padding: 2px 8px;
    border-radius: 9999px;
    flex-shrink: 0;

    &--warning {
      background: rgba(200, 160, 66, 0.2);
      color: var(--pq-warning);
    }

    &--danger {
      background: rgba(160, 64, 56, 0.2);
      color: var(--pq-danger);
    }

    &--info {
      background: rgba(110, 139, 184, 0.2);
      color: var(--pq-info);
    }
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 24px 0;
    text-align: center;

    p {
      font-family: 'Satoshi', sans-serif;
      font-size: 13px;
      color: var(--pq-text-muted);
      margin: 0;
    }
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

  &-text { height: 12px; }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
