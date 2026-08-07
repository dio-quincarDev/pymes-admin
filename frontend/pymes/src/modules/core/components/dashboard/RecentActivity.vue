<script setup lang="ts">
import type { ActividadItem } from 'src/modules/core/composables/useFinancialDashboard';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';

interface Props {
  actividades: ActividadItem[];
  loading?: boolean;
}

withDefaults(defineProps<Props>(), { loading: false });

const { formatCurrency } = useNumberFormat();

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  return d.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' });
}
</script>

<template>
  <div class="recent-activity">
    <h3 class="recent-activity__title">Actividad Reciente</h3>

    <template v-if="loading">
      <div v-for="i in 5" :key="i" class="recent-activity__row">
        <div class="skeleton skeleton-circle" />
        <div class="recent-activity__info">
          <div class="skeleton skeleton-text" style="width: 70%" />
          <div class="skeleton skeleton-text" style="width: 40%" />
        </div>
        <div class="skeleton skeleton-text" style="width: 60px" />
      </div>
    </template>

    <template v-else-if="actividades.length === 0">
      <div class="recent-activity__empty">
        <q-icon name="inbox" size="32px" color="positive" aria-hidden="true" />
        <p>Sin actividad reciente</p>
      </div>
    </template>

    <ul v-else class="recent-activity__list" role="list">
      <li
        v-for="(item, idx) in actividades"
        :key="`${item.type}-${item.date}-${idx}`"
        class="recent-activity__row"
        :aria-label="`${item.type === 'gasto' ? 'Gasto' : 'Venta'}: ${item.description}, ${formatCurrency(item.amount)}, ${formatDate(item.date)}`"
      >
        <q-icon
          :name="item.type === 'gasto' ? 'receipt_long' : 'trending_up'"
          size="18px"
          class="recent-activity__icon"
          :class="item.type === 'gasto' ? 'recent-activity__icon--gasto' : 'recent-activity__icon--venta'"
          aria-hidden="true"
        />
        <div class="recent-activity__info">
          <span class="recent-activity__desc">{{ item.description }}</span>
          <span class="recent-activity__date">{{ formatDate(item.date) }}</span>
        </div>
        <span
          class="recent-activity__amount"
          :class="item.type === 'gasto' ? 'recent-activity__amount--gasto' : 'recent-activity__amount--venta'"
        >
          {{ item.type === 'gasto' ? '−' : '+' }}{{ formatCurrency(item.amount) }}
        </span>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.recent-activity {
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
    &--venta { color: var(--pq-accent-green); }
  }

  &__info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__desc {
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    font-weight: 400;
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
    &--venta { color: var(--pq-accent-green); }
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
  &-circle { width: 18px; height: 18px; border-radius: 50%; flex-shrink: 0; }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
