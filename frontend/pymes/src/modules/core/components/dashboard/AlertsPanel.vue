<template>
  <div class="alerts-panel">
    <div class="alerts-panel__header">
      <span class="alerts-panel__title">Alertas</span>
      <q-badge v-if="items.length" :color="hasCritical ? 'negative' : 'warning'" rounded>
        {{ items.length }}
      </q-badge>
    </div>

    <div v-if="!items.length" class="alerts-panel__empty">
      <q-icon name="check_circle" size="2rem" class="text-positive" />
      <span class="text-grey-6 q-mt-sm">Sin alertas activas</span>
    </div>

    <q-list v-else dense class="alerts-panel__list">
      <q-item v-for="alert in items" :key="alert.productId" class="alerts-panel__item">
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
            {{ formatCurrency(alert.currentPrice) }} vs {{ formatCurrency(alert.avgPrice) }}
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
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { AlertItem } from '../../types/analytics';
import { useNumberFormat } from '../../composables/useNumberFormat';

const props = defineProps<{ items: AlertItem[] }>();
const { formatCurrency } = useNumberFormat();

const hasCritical = computed(() => props.items.some((a) => a.severity === 'critical'));
</script>

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
    color: #E2E8E4;
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

    &:last-child { border-bottom: none; }
  }

  &__name {
    font-size: 0.8rem;
    color: #E2E8E4;
  }

  &__detail {
    font-size: 0.7rem;
    color: #8A9E99;
  }
}
</style>
