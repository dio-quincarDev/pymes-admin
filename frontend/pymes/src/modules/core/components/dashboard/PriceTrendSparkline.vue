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
import BaseChart from '../charts/BaseChart.vue';

defineProps<{ items: TrendItem[] }>();

const { formatCurrency } = useNumberFormat();

function miniChart(item: TrendItem) {
  const avg = item.movingAvg90d;
  const current = item.currentAvgPrice;
  return {
    labels: ['', '90d', '', '', 'Actual'],
    datasets: [
      {
        data: [avg * 0.95, avg * 0.97, avg * 0.99, avg, current],
        borderColor: current >= avg ? '#2D5A27' : '#e94560',
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
  border-bottom: 1px solid rgba(138, 158, 153, 0.08);

  &:last-child { border-bottom: none; }

  &__info {
    flex: 0 0 140px;
    min-width: 0;
  }

  &__name {
    display: block;
    font-size: 0.8rem;
    color: #E2E8E4;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__price {
    font-size: 0.7rem;
    color: #8A9E99;
  }

  &__chart {
    flex: 1;
    min-width: 80px;
    max-width: 160px;
  }

  &__change {
    flex: 0 0 60px;
    text-align: right;
    font-size: 0.75rem;
    font-weight: 600;
  }
}
</style>
