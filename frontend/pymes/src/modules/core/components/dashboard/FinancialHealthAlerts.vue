<script setup lang="ts">
import type { FinancialHealthAlert } from 'src/modules/core/types/analytics';

interface Props {
  alerts: FinancialHealthAlert[];
}

defineProps<Props>();

function alertColor(alert: FinancialHealthAlert): string {
  if (alert.code?.includes('NEGATIVE') || alert.code?.includes('OVER_LEVERAGED')) return 'var(--pq-danger)';
  return 'var(--pq-warning)';
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
