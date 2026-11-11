<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { FinancialHealthAlert, FinancialHealth, SupplierRecommendationItem } from 'src/modules/core/types/analytics';
import { isGoodDependence } from 'src/modules/core/utils/financialGuide';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';

const router = useRouter();
const { formatCurrency } = useNumberFormat();

// Mismo destino que AlertsPanel: el producto con su nombre ya en el buscador
function goToProduct(productName: string) {
  void router.push({ name: 'productos', query: { buscar: productName } });
}

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
      Riesgos del Negocio ({{ alerts.length }})
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
          <span v-if="alert.description" class="fh-alerts__desc">{{ alert.description }}</span>
          <span v-if="alert.code === 'DEAD_INVENTORY' && alert.current > 0" class="fh-alerts__money">
            {{ formatCurrency(alert.current) }} parados en total
          </span>
          <ul v-if="alert.code === 'DEAD_INVENTORY' && alert.items?.length" class="fh-alerts__products" role="list">
            <li v-for="item in alert.items" :key="item.id">
              <button type="button" class="fh-alerts__product" @click="goToProduct(item.name)">
                <span class="fh-alerts__product-name">{{ item.name }}</span>
                <span class="fh-alerts__product-amount">{{ formatCurrency(item.totalInvestment) }}</span>
              </button>
            </li>
          </ul>
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

  &__desc {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text);
  }

  &__money {
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--pq-text);
  }

  &__products {
    list-style: none;
    padding: 0;
    margin: 2px 0 0;
    width: 100%;
  }

  &__product {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    width: 100%;
    min-height: 44px;
    padding: 6px 8px;
    margin: 0 -8px;
    border: 0;
    border-radius: var(--pq-radius-sm, 6px);
    background: transparent;
    cursor: pointer;
    text-align: left;

    &:hover,
    &:focus-visible {
      background: var(--pq-elevated);
    }
  }

  &__product-name {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }

  &__product-amount {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: var(--pq-text-muted);
    white-space: nowrap;
  }
}
</style>
