<script setup lang="ts">
import type { FinancialHealth, FinancialHealthAlert } from 'src/modules/core/types/analytics';

interface Props {
  data: FinancialHealth | null;
  loading?: boolean;
}

withDefaults(defineProps<Props>(), { loading: false });

function healthColor(score: number): string {
  if (score < 40) return 'var(--pq-danger)';
  if (score < 70) return 'var(--pq-warning)';
  return 'var(--pq-success)';
}

function alertColor(alert: FinancialHealthAlert): string {
  if (alert.code?.includes('NEGATIVE') || alert.code?.includes('OVER_LEVERAGED')) return 'var(--pq-danger)';
  return 'var(--pq-warning)';
}
</script>

<template>
  <div class="fh-panel">
    <h3 class="fh-panel__title">Salud Financiera</h3>

    <template v-if="loading">
      <div class="fh-panel__score-row">
        <div class="skeleton" style="width: 64px; height: 64px; border-radius: 50%" />
        <div class="skeleton" style="width: 120px; height: 12px" />
      </div>
    </template>

    <template v-else-if="!data">
      <div class="fh-panel__empty">
        <q-icon name="heart_broken" size="32px" style="color: var(--pq-text-subtle)" aria-hidden="true" />
        <p>Sin datos de salud financiera</p>
      </div>
    </template>

    <template v-else>
      <div class="fh-panel__score-row">
        <div
          class="fh-panel__score"
          :style="{ borderColor: healthColor(data.overallHealth) }"
        >
          <span
            class="fh-panel__score-value"
            :style="{ color: healthColor(data.overallHealth) }"
          >{{ data.overallHealth }}</span>
          <span class="fh-panel__score-label">/ 100</span>
        </div>
        <div class="fh-panel__score-meta">
          <span class="fh-panel__score-title">Índice General</span>
          <span class="fh-panel__score-sub">
            {{ data.overallHealth >= 70 ? 'Saludable' : data.overallHealth >= 40 ? 'En desarrollo' : 'Crítico' }}
          </span>
        </div>
      </div>

      <div v-if="data.criticalAlerts.length" class="fh-panel__section">
        <h4 class="fh-panel__section-title">
          <q-icon name="warning" size="14px" :style="{ color: 'var(--pq-warning)' }" />
          Alertas ({{ data.criticalAlerts.length }})
        </h4>
        <ul class="fh-panel__alerts" role="list">
          <li
            v-for="alert in data.criticalAlerts"
            :key="alert.code"
            class="fh-panel__alert"
          >
            <span class="fh-panel__alert-dot" :style="{ background: alertColor(alert) }" />
            <div class="fh-panel__alert-info">
              <span class="fh-panel__alert-title">{{ alert.title }}</span>
              <span class="fh-panel__alert-action">{{ alert.action }}</span>
            </div>
          </li>
        </ul>
      </div>

      <div v-if="data.recommendations.length" class="fh-panel__section">
        <h4 class="fh-panel__section-title">
          <q-icon name="lightbulb" size="14px" :style="{ color: 'var(--pq-accent)' }" />
          Recomendaciones
        </h4>
        <ul class="fh-panel__list" role="list">
          <li
            v-for="(rec, i) in data.recommendations"
            :key="i"
            class="fh-panel__list-item"
          >{{ rec }}</li>
        </ul>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.fh-panel {
  display: flex;
  flex-direction: column;

  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin: 0 0 16px;
  }

  &__score-row {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
  }

  &__score {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    border: 3px solid;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__score-value {
    font-family: 'Geist Mono', monospace;
    font-size: 20px;
    font-weight: 600;
    line-height: 1;
  }

  &__score-label {
    font-family: 'Satoshi', sans-serif;
    font-size: 9px;
    color: var(--pq-text-muted);
    line-height: 1;
  }

  &__score-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__score-title {
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    font-weight: 500;
    color: var(--pq-text);
  }

  &__score-sub {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-muted);
  }

  &__section {
    margin-bottom: 16px;

    &:last-child { margin-bottom: 0; }
  }

  &__section-title {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    font-weight: 500;
    color: var(--pq-text);
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 0 8px;
  }

  &__alerts {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  &__alert {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 0;
    border-bottom: 1px solid var(--pq-border);

    &:last-child { border-bottom: none; }
  }

  &__alert-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    margin-top: 5px;
    flex-shrink: 0;
  }

  &__alert-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__alert-title {
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--pq-text);
  }

  &__alert-action {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-muted);
  }

  &__list {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  &__list-item {
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    color: var(--pq-text);
    padding: 6px 0;
    border-bottom: 1px solid var(--pq-border);
    line-height: 1.4;

    &:last-child { border-bottom: none; }
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 24px 0;
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
