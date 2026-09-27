<script setup lang="ts">
import { computed, shallowRef } from 'vue';
import type { AlertItem } from '../../types/analytics';
import { useNumberFormat } from '../../composables/useNumberFormat';
import { toNumber } from '../../utils/analyticsNormalize';

interface Props {
  items: AlertItem[];
}

const props = defineProps<Props>();
const { formatCurrency } = useNumberFormat();

// ponytail: filtro UI puro — esconde basura 0.00 (Mayonesa 0.00 vs 3.88) sin tocar backend; si necesitás persistir, filtra en AnalyticsServiceImpl
const filteredAlerts = computed(() =>
  props.items.filter((a) => toNumber(a.currentPrice) > 0 && toNumber(a.avgPrice) > 0 && Number.isFinite(toNumber(a.variationPct, 0))),
);

// ponytail: tope fijo, sin scroll infinito — el backend ya ordena por cv/premium desc; subir TOP_VISIBLE si piden
const TOP_VISIBLE = 8;
const expanded = shallowRef(false);

const visibleAlerts = computed(() =>
  expanded.value ? filteredAlerts.value : filteredAlerts.value.slice(0, TOP_VISIBLE),
);
const hiddenCount = computed(() => filteredAlerts.value.length - visibleAlerts.value.length);

const hasCritical = computed(() => filteredAlerts.value.some((a) => a.severity === 'critical'));

function evidenceSuffix(alert: AlertItem): string {
  if (!alert.purchaseCount) return '';
  const buys = `${alert.purchaseCount} compra${alert.purchaseCount === 1 ? '' : 's'}`;
  if (alert.alertKind === 'SUPPLIER_PREMIUM' || !alert.providerCount) return `, basado en ${buys}`;
  return `, basado en ${buys} en ${alert.providerCount} proveedore${alert.providerCount === 1 ? '' : 's'}`;
}
</script>

<template>
  <div class="alerts-panel">
    <div class="alerts-panel__header">
      <span class="alerts-panel__title">Alertas</span>
      <q-badge v-if="filteredAlerts.length" :color="hasCritical ? 'negative' : 'warning'" rounded>
        {{ filteredAlerts.length }}
      </q-badge>
    </div>

    <div v-if="!filteredAlerts.length" class="alerts-panel__empty">
      <q-icon name="check_circle" size="2rem" class="text-positive" />
      <span class="text-grey-6 q-mt-sm">Sin alertas activas</span>
    </div>

    <q-list v-else dense class="alerts-panel__list">
      <q-item v-for="alert in visibleAlerts" :key="alert.productId" class="alerts-panel__item">
        <q-item-section avatar>
          <q-icon
            :name="alert.severity === 'critical' ? 'error' : 'warning'"
            :color="alert.severity === 'critical' ? 'negative' : 'warning'"
            size="sm"
          />
        </q-item-section>
        <q-item-section>
          <q-item-label class="alerts-panel__name">{{ alert.productName }}</q-item-label>
          <q-item-label caption class="alerts-panel__detail">
            <template v-if="alert.alertKind === 'SUPPLIER_PREMIUM' && alert.providerName">{{ alert.providerName }} cobra {{ formatCurrency(alert.currentPrice) }} sobre el promedio {{ formatCurrency(alert.avgPrice) }}</template>
            <template v-else>Subiendo: {{ formatCurrency(alert.currentPrice) }} vs promedio {{ formatCurrency(alert.avgPrice) }}</template>{{ evidenceSuffix(alert) }}.
          </q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-badge
            :color="alert.severity === 'critical' ? 'negative' : 'warning'"
            :label="`${alert.variationPct > 0 ? '+' : ''}${alert.variationPct?.toFixed(1) ?? '0.0'}%`"
            rounded
          />
        </q-item-section>
      </q-item>
    </q-list>
    <q-btn
      v-if="hiddenCount > 0 || expanded"
      flat
      dense
      size="sm"
      color="warning"
      class="alerts-panel__more"
      :label="expanded ? 'Ver menos' : `+${hiddenCount} más este mes`"
      @click="expanded = !expanded"
    />
  </div>
</template>

<style scoped lang="scss">
.alerts-panel {
  background: rgba(11, 18, 16, 0.5);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(163, 120, 94, 0.1);
  border-radius: 8px;
  padding: 1rem;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.75rem;
  }

  &__title {
    font-size: 0.85rem;
    font-weight: 600;
    color: #e2e8e4;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 1.5rem 0;
  }

  &__list {
    max-height: 300px;
    overflow-y: auto;
  }

  &__item {
    padding: 0.5rem 0;
    border-bottom: 1px solid rgba(138, 158, 153, 0.08);

    &:last-child {
      border-bottom: none;
    }
  }

  &__name {
    font-size: 0.8rem;
    color: #e2e8e4;
  }

  &__detail {
    font-size: 0.7rem;
    color: #8a9e99;
  }

  &__more {
    margin: 0.25rem auto 0;
    display: block;
    font-size: 0.75rem;
  }
}
</style>
