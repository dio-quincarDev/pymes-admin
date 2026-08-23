<script setup lang="ts">
import type { ActividadItem } from 'src/modules/core/composables/useFinancialDashboard';
import type { Factura } from 'src/modules/core/types';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import { formatDate } from 'src/utils/format';

interface Props {
  actividades: ActividadItem[];
  facturas: Factura[];
  loading?: boolean;
}

withDefaults(defineProps<Props>(), { loading: false });

const { formatCurrency } = useNumberFormat();

function statusClass(status: string) {
  if (status === 'PENDIENTE') return 'activity-panel__badge--warning';
  if (status === 'VENCIDA') return 'activity-panel__badge--danger';
  return 'activity-panel__badge--info';
}
</script>

<template>
  <div class="activity-panel">
    <h3 class="activity-panel__title">Actividad</h3>

    <template v-if="loading">
      <div v-for="i in 5" :key="i" class="activity-panel__row">
        <div class="skeleton skeleton-circle" />
        <div class="activity-panel__info">
          <div class="skeleton skeleton-text" style="width: 70%" />
          <div class="skeleton skeleton-text" style="width: 40%" />
        </div>
        <div class="skeleton skeleton-text" style="width: 60px" />
      </div>
    </template>

    <template v-else-if="facturas.length === 0 && actividades.length === 0">
      <div class="activity-panel__empty">
        <q-icon name="inbox" size="32px" color="positive" aria-hidden="true" />
        <p>Sin actividad reciente</p>
      </div>
    </template>

    <template v-else>
      <div class="activity-panel__scroll">
        <ul v-if="facturas.length > 0" class="activity-panel__list" role="list">
          <li
            v-for="factura in facturas"
            :key="factura.id"
            class="activity-panel__row"
            :aria-label="`${factura.providerName}, factura ${factura.invoiceNumber}, ${formatCurrency(factura.total)}, ${factura.status}`"
          >
            <div class="activity-panel__info">
              <span class="activity-panel__supplier">{{ factura.providerName }}</span>
              <span class="activity-panel__date">{{ formatDate(factura.issueDate, true) }}</span>
            </div>
            <span class="activity-panel__amount">{{ formatCurrency(factura.total) }}</span>
            <span class="activity-panel__badge" :class="statusClass(factura.status)">
              {{ factura.status }}
            </span>
          </li>
        </ul>

        <ul v-if="actividades.length > 0" class="activity-panel__list" role="list">
          <li
            v-for="(item, idx) in actividades"
            :key="`${item.type}-${item.date}-${idx}`"
            class="activity-panel__row"
            :aria-label="`${item.type === 'gasto' ? 'Gasto' : 'Venta'}: ${item.description}, ${formatCurrency(item.amount)}, ${formatDate(item.date)}`"
          >
            <q-icon
              :name="item.type === 'gasto' ? 'receipt_long' : 'trending_up'"
              size="18px"
              class="activity-panel__icon"
              :class="item.type === 'gasto' ? 'activity-panel__icon--gasto' : 'activity-panel__icon--venta'"
              aria-hidden="true"
            />
            <div class="activity-panel__info">
              <span class="activity-panel__desc">{{ item.description }}</span>
              <span class="activity-panel__date">{{ formatDate(item.date) }}</span>
            </div>
            <span
              class="activity-panel__amount"
              :class="item.type === 'gasto' ? 'activity-panel__amount--gasto' : 'activity-panel__amount--venta'"
            >
              {{ item.type === 'gasto' ? '−' : '+' }}{{ formatCurrency(item.amount) }}
            </span>
          </li>
        </ul>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.activity-panel {
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

  &__scroll {
    max-height: 320px;
    overflow-y: auto;
  }

  &__list {
    list-style: none;
    padding: 0;
    margin: 0;
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

  &__icon {
    flex-shrink: 0;

    &--gasto { color: var(--pq-text-muted); }
    &--venta { color: var(--pq-success); }
  }

  &__info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__desc,
  &__supplier {
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    font-weight: 500;
    color: var(--pq-text);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__date {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    font-weight: 400;
    color: var(--pq-text-muted);
  }

  &__amount {
    font-family: 'Geist Mono', monospace;
    font-size: 14px;
    font-weight: 500;
    font-variant-numeric: tabular-nums;
    flex-shrink: 0;

    &--gasto { color: var(--pq-danger); }
    &--venta { color: var(--pq-success); }
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
</style>