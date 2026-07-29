<script setup lang="ts">
import { computed } from 'vue';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';

interface CategoryBreakdownItem {
  category: string;
  currentAmount: number;
  previousAmount?: number | undefined;
  percentage: number;
}

interface Props {
  items: CategoryBreakdownItem[];
  loading?: boolean;
  empty?: boolean;
  maxItems?: number;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  empty: false,
  maxItems: 5,
});

const { formatCurrency } = useNumberFormat();

const displayItems = computed(() => {
  const items = props.items.slice(0, props.maxItems);
  const othersTotal = props.items
    .slice(props.maxItems)
    .reduce((sum, item) => sum + item.currentAmount, 0);
  if (othersTotal > 0) {
    const othersPrevTotal = props.items
      .slice(props.maxItems)
      .reduce((sum, item) => sum + (item.previousAmount ?? 0), 0);
    const totalCurrent = props.items.reduce((s, i) => s + i.currentAmount, 0);
    items.push({
      category: 'Otros',
      currentAmount: othersTotal,
      previousAmount: othersPrevTotal || undefined,
      percentage: totalCurrent > 0 ? (othersTotal / totalCurrent) * 100 : 0,
    });
  }
  return items;
});

const maxAmount = computed(() =>
  Math.max(
    ...displayItems.value.map((i) => Math.max(i.currentAmount, i.previousAmount ?? 0)),
    1,
  ),
);

function barWidth(amount: number) {
  return `${(amount / maxAmount.value) * 100}%`;
}
</script>

<template>
  <div class="cat-chart">
    <template v-if="loading">
      <div v-for="i in 5" :key="i" class="cat-chart__row">
        <div class="skeleton skeleton-text" style="width: 80px; height: 12px" />
        <div class="cat-chart__track">
          <div class="skeleton" :style="{ width: `${60 - i * 8}%`, height: '6px' }" />
        </div>
        <div class="skeleton skeleton-text" style="width: 60px; height: 12px" />
      </div>
    </template>

    <template v-else-if="empty || displayItems.length === 0">
      <div class="cat-chart__empty">
        <q-icon name="bar_chart" size="32px" style="color: var(--pq-text-subtle)" aria-hidden="true" />
        <p>No hay gastos en este período</p>
      </div>
    </template>

    <template v-else>
      <div
        v-for="item in displayItems"
        :key="item.category"
        class="cat-chart__row"
        :aria-label="`${item.category}: ${formatCurrency(item.currentAmount)}, ${item.percentage.toFixed(1)}%`"
      >
        <span class="cat-chart__name">{{ item.category }}</span>
        <div class="cat-chart__track">
          <!-- Previous period overlay -->
          <div
            v-if="item.previousAmount"
            class="cat-chart__bar cat-chart__bar--prev"
            :style="{ width: barWidth(item.previousAmount) }"
          />
          <!-- Current period bar -->
          <div
            class="cat-chart__bar cat-chart__bar--current"
            :style="{ width: barWidth(item.currentAmount) }"
          />
        </div>
        <div class="cat-chart__meta">
          <span class="cat-chart__amount">{{ formatCurrency(item.currentAmount) }}</span>
          <span class="cat-chart__pct">{{ item.percentage.toFixed(0) }}%</span>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.cat-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__name {
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--pq-text);
    width: 110px;
    flex-shrink: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__track {
    flex: 1;
    height: 6px;
    background: var(--pq-surface);
    border-radius: 3px;
    overflow: hidden;
    position: relative;
    max-width: 600px;
  }

  &__bar {
    height: 100%;
    border-radius: 3px;
    position: absolute;
    left: 0;
    top: 0;
    transition: width 300ms cubic-bezier(0.4, 0, 0.2, 1);

    &--current {
      background: var(--pq-accent);
      z-index: 2;
    }

    &--prev {
      background: var(--pq-text-muted);
      opacity: 0.2;
      z-index: 1;
    }
  }

  &__meta {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    width: 90px;
    flex-shrink: 0;
  }

  &__amount {
    font-family: 'Geist Mono', monospace;
    font-size: 12px;
    font-weight: 500;
    color: var(--pq-text-muted);
    font-variant-numeric: tabular-nums;
  }

  &__pct {
    font-family: 'Geist Mono', monospace;
    font-size: 11px;
    color: var(--pq-text-subtle);
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 32px 0;
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
