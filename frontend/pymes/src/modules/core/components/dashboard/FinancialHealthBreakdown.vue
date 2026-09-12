<script setup lang="ts">
import { computed } from 'vue';
import TraderGauge from './TraderGauge.vue';
import type { FinancialHealthBreakdown } from 'src/modules/core/types/analytics';

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
    .map((k) => ({
      key: k,
      label: labels[k] ?? k,
      score: props.breakdown[k]?.score ?? 0,
    }));
});
</script>

<template>
  <div
    v-if="items.length"
    class="fh-breakdown"
    role="group"
    aria-label="Desglose por pilar"
  >
    <TraderGauge
      v-for="item in items"
      :key="item.key"
      :score="item.score"
      :label="item.label"
      :size="78"
    />
  </div>
</template>

<style scoped lang="scss">
.fh-breakdown {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  min-width: 0;
}
</style>
