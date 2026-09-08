<template>
  <div class="sparkline-list">
    <div v-if="!items.length" class="text-grey-6 text-center q-pa-md">
      Sin tendencias para este período
    </div>
    <div v-for="item in items" :key="item.productId" class="sparkline-row">
      <div class="sparkline-row__info">
        <span class="sparkline-row__name">{{ item.productName }}</span>
        <span class="sparkline-row__price">{{ formatCurrency(item.currentAvgPrice) }}</span>
      </div>
      <div class="sparkline-row__chart">
        <BaseChart type="line" :data="miniChart(item)" :options="miniOptions" :height="40" />
      </div>
      <span
        class="sparkline-row__change"
        :class="item.pctChange >= 0 ? 'text-positive' : 'text-negative'"
      >
        {{ item.pctChange >= 0 ? '+' : '' }}{{ item.pctChange.toFixed(1) }}%
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TrendItem } from '../../types/analytics';
import { useNumberFormat } from '../../composables/useNumberFormat';
import { useChartTheme } from '../../composables/useChartTheme';
import BaseChart from '../charts/BaseChart.vue';

defineProps<{ items: TrendItem[] }>();

const { formatCurrency } = useNumberFormat();
const { colors } = useChartTheme();

function miniChart(item: TrendItem) {
  const avg = item.movingAvg90d;
  const current = item.currentAvgPrice;
  return {
    labels: ['', '90d', '', '', 'Actual'],
    datasets: [
      {
        data: [avg * 0.95, avg * 0.97, avg * 0.99, avg, current],
        borderColor: current >= avg ? colors.value.positive : colors.value.negative,
        backgroundColor: 'transparent',
        pointRadius: 0,
        tension: 0.4,
        borderWidth: 2,
      },
    ],
  };
}

const miniOptions = {
  plugins: { legend: { display: false }, tooltip: { enabled: false } },
  scales: { x: { display: false }, y: { display: false } },
  elements: { point: { radius: 0 } },
};
</script>

<style scoped lang="scss">
.sparkline-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.sparkline-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--pq-border);

  &:last-child { border-bottom: none; }

  &__info {
    flex: 0 0 140px;
    min-width: 0;
  }

  &__name {
    display: block;
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    color: var(--pq-text);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__price {
    font-family: 'Geist Mono', monospace;
    font-size: 11px;
    color: var(--pq-text-muted);
  }

  &__chart {
    flex: 1;
    min-width: 80px;
    max-width: 160px;
  }

  &__change {
    flex: 0 0 60px;
    text-align: right;
    font-family: 'Geist Mono', monospace;
    font-size: 12px;
    font-weight: 600;
  }
}
</style>
