<script setup lang="ts">
import { computed } from 'vue';
import type { FinancialHealthAlert, FinancialHealth, SupplierRecommendationItem } from 'src/modules/core/types/analytics';
import { isGoodDependence } from 'src/modules/core/utils/financialGuide';

interface Props {
  alerts: FinancialHealthAlert[];
  health?: FinancialHealth | null;
  recommendations?: SupplierRecommendationItem[];
}

const props = withDefaults(defineProps<Props>(), { health: null, recommendations: () => [] });

const goodDependence = computed(() => isGoodDependence(props.health ?? null, props.recommendations ?? []));

const alertColors = computed(() => {
  const map = new Map<string, string>();
  for (const alert of props.alerts) {
    if (alert.code?.includes('NEGATIVE') || alert.code?.includes('OVER_LEVERAGED')) map.set(alert.code, 'var(--pq-danger)');
    else if (alert.code?.includes('SUPPLIER_CONCENTRATION')) map.set(alert.code, goodDependence.value ? 'var(--pq-warning)' : 'var(--pq-danger)');
    else map.set(alert.code, 'var(--pq-warning)');
  }
  return map;
});

function alertColor(alert: FinancialHealthAlert): string {
  return alertColors.value.get(alert.code) ?? 'var(--pq-warning)';
}
</script>

<template>
  <div v-if="alerts.length" class="fh-alerts">
    <h4 class="fh-alerts__title">
      <q-icon name="warning" size="14px" :style="{ color: 'var(--pq-warning)' }" />
      Alertas ({{ alerts.length }})
    </h4>
    <ul class="fh-alerts__list" role="list">
      <li
        v-for="alert in alerts"
        :key="alert.code"
        class="fh-alerts__item"
      >
        <span class="fh-alerts__dot" :style="{ background: alertColor(alert) }" />
        <div class="fh-alerts__info">
          <span class="fh-alerts__name">{{ alert.title }}</span>
          <span class="fh-alerts__action">{{ alert.action }}</span>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.fh-alerts {
  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    font-weight: 500;
    color: var(--pq-text);
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 0 8px;
  }

  &__list {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  &__item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 0;
    border-bottom: 1px solid var(--pq-border);

    &:last-child { border-bottom: none; }
  }

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    margin-top: 5px;
    flex-shrink: 0;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__name {
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--pq-text);
  }

  &__action {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-muted);
  }
}
</style>
