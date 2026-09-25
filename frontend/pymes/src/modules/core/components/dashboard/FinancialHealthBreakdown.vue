<script setup lang="ts">
import { computed } from 'vue';
import TraderGauge from './TraderGauge.vue';
import type { FinancialHealthBreakdown } from 'src/modules/core/types/analytics';
import { guideForPillar } from 'src/modules/core/utils/financialGuide';

interface Props {
  breakdown: Record<string, FinancialHealthBreakdown>;
}

const props = defineProps<Props>();

const items = computed(() => {
  const order = ['profitability', 'efficiency', 'stability', 'growth'] as const;
  const labels: Record<string, string> = {
    profitability: 'Rentabilidad',
    efficiency: 'Eficiencia',
    stability: 'Estabilidad',
    growth: 'Crecimiento',
  };
  return order
    .filter((k) => k in props.breakdown)
    .map((k) => {
      const bd = props.breakdown[k];
      const guide = guideForPillar(k, bd);
      return {
        key: k,
        label: labels[k] ?? k,
        score: bd?.score ?? 0,
        guide,
      };
    });
});
</script>

<template>
  <div
    v-if="items.length"
    class="fh-breakdown"
    role="group"
    aria-label="Desglose por pilar"
  >
    <div
      v-for="item in items"
      :key="item.key"
      class="fh-breakdown__item"
      :title="item.guide.tooltip"
    >
      <TraderGauge
        :score="item.score"
        :label="item.label"
        :size="78"
      />
      <div class="fh-breakdown__guide">
        <span class="fh-breakdown__guide-label">{{ item.guide.label }}</span>
        <span class="fh-breakdown__guide-detail">{{ item.guide.detail }}</span>
      </div>
      <q-tooltip anchor="top middle" self="bottom middle" class="bg-dark text-white text-caption" max-width="220px">
        {{ item.guide.tooltip }}
      </q-tooltip>
    </div>
  </div>
</template>

<style scoped lang="scss">
.fh-breakdown {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  min-width: 0;

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 0;
    text-align: center;
  }

  &__guide {
    display: flex;
    flex-direction: column;
    gap: 2px;
    margin-top: 6px;
    min-width: 0;
  }

  &__guide-label {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text);
    line-height: 1.2;
  }

  &__guide-detail {
    font-family: 'Satoshi', sans-serif;
    font-size: 10px;
    color: var(--pq-text-muted);
    line-height: 1.2;
  }
}
</style>
