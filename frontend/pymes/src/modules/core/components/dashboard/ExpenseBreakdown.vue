<script setup lang="ts">
import { computed } from 'vue';
import type { GastoPorCategoria } from 'src/modules/core/composables/useFinancialDashboard';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';

interface Props {
  gastos: GastoPorCategoria[];
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), { loading: false });

const { formatCurrency } = useNumberFormat();

const MAX_BARS = 5;

const displayItems = computed(() => {
  const items = props.gastos.slice(0, MAX_BARS);
  const othersTotal = props.gastos
    .slice(MAX_BARS)
    .reduce((sum, item) => sum + item.total, 0);
  if (othersTotal > 0) {
    const othersPct = props.gastos.length > MAX_BARS
      ? (othersTotal / props.gastos.reduce((s, i) => s + i.total, 0)) * 100
      : 0;
    items.push({ category: 'Otros', total: othersTotal, pct: othersPct });
  }
  return items;
});

const maxAmount = computed(() =>
  Math.max(...displayItems.value.map((i) => i.total), 1),
);

function barWidth(amount: number) {
  return `${(amount / maxAmount.value) * 100}%`;
}
</script>

<template>
  <div class="expense-breakdown">
    <h3 class="expense-breakdown__title">Desglose de Gastos</h3>

    <template v-if="loading">
      <div v-for="i in 5" :key="i" class="expense-breakdown__bar-row">
        <div class="skeleton skeleton-text" style="width: 100px; height: 12px" />
        <div class="expense-breakdown__bar-track">
          <div class="skeleton" :style="{ width: `${60 - i * 8}%`, height: '8px' }" />
        </div>
        <div class="skeleton skeleton-text" style="width: 60px; height: 12px" />
      </div>
    </template>

    <template v-else-if="displayItems.length === 0">
      <div class="expense-breakdown__empty">
        <q-icon name="receipt_long" size="32px" color="positive" aria-hidden="true" />
        <p>Sin gastos registrados en este período</p>
      </div>
    </template>

    <template v-else>
      <div
        v-for="item in displayItems"
        :key="item.category"
        class="expense-breakdown__bar-row"
        :aria-label="`${item.category}: ${formatCurrency(item.total)}, ${item.pct.toFixed(1)}% del total`"
      >
        <span class="expense-breakdown__cat-name">{{ item.category }}</span>
        <div class="expense-breakdown__bar-track">
          <div
            class="expense-breakdown__bar-fill"
            :style="{ width: barWidth(item.total) }"
            role="meter"
            :aria-valuenow="item.pct"
            aria-valuemin="0"
            aria-valuemax="100"
          />
        </div>
        <span class="expense-breakdown__amount">{{ formatCurrency(item.total) }}</span>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.expense-breakdown {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin: 0 0 4px;
  }

  &__bar-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__cat-name {
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--pq-text);
    width: 120px;
    flex-shrink: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__bar-track {
    flex: 1;
    height: 8px;
    background: var(--pq-surface);
    border-radius: 2px;
    overflow: hidden;
  }

  &__bar-fill {
    height: 100%;
    background: var(--pq-accent);
    border-radius: 2px;
    transition: width var(--pq-motion-base);
  }

  &__amount {
    font-family: 'Geist Mono', monospace;
    font-size: 13px;
    font-weight: 500;
    color: var(--pq-text-muted);
    font-variant-numeric: tabular-nums;
    width: 100px;
    text-align: right;
    flex-shrink: 0;
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
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
