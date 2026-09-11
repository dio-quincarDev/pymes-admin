<script setup lang="ts">
import { onMounted } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import { useAnalytics } from '../composables/useAnalytics';
import { useAnalisisGastos } from '../composables/useAnalisisGastos';
import AnalyticsHeader from 'src/modules/core/components/analytics/AnalyticsHeader.vue';
import MetricCard from 'src/modules/core/components/analytics/MetricCard.vue';
import AbcGastosChart from 'src/modules/core/components/dashboard/AbcGastosChart.vue';
import SupplierRecommendationsCard from 'src/modules/core/components/dashboard/SupplierRecommendationsCard.vue';
import AlertsPanel from '../components/dashboard/AlertsPanel.vue';
import FinancialHealthPanel from '../components/dashboard/FinancialHealthPanel.vue';

useMeta({ title: 'Análisis de Gastos — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;
const { formatCurrency } = useNumberFormat();

const {
  period,
  setPeriod,
  recalcular: recalcularAnalytics,
  loading: analyticsLoading,
  alerts,
  financialHealth,
  abc,
  supplierRecommendations,
} = useAnalytics();

const { totalInvestment, productCount, byCategory, loading, load } =
  useAnalisisGastos(tenantId);

async function handleLoad() {
  try {
    await load();
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar datos' });
  }
}

onMounted(() => {
  if (tenantId) void handleLoad();
});
</script>

<template>
  <q-page class="core-page">
    <AnalyticsHeader
      title="Análisis de Gastos"
      subtitle="Dónde gasto y qué proveedores me convienen"
      :period="period"
      :loading="analyticsLoading || loading"
      @update:period="setPeriod"
      @recalculate="recalcularAnalytics"
    />

    <div class="metric-row stagger-children">
      <MetricCard
        label="Inversión en Productos"
        :value="formatCurrency(totalInvestment)"
        accent="gold"
        :loading="loading"
      />
      <MetricCard
        label="Productos"
        :value="String(productCount)"
        accent="blue"
        :loading="loading"
      />
      <MetricCard
        label="Categorías"
        :value="String(byCategory.length)"
        accent="green"
        :loading="loading"
      />
    </div>

    <!-- A) ABC Pareto — dónde se va el 80% -->
    <div class="analysis-card q-mb-lg">
      <div class="analysis-card__header">
        <h3 class="analysis-card__title">Concentración del gasto (ABC)</h3>
        <span class="analysis-card__hint">Pocos productos, mayor gasto</span>
      </div>
      <AbcGastosChart :data="abc" :height="300" />
    </div>

    <!-- B) Ahorro por proveedor — cuánto te ahorras -->
    <SupplierRecommendationsCard :items="supplierRecommendations" class="q-mb-lg" />

    <div class="analysis-vital">
      <FinancialHealthPanel :data="financialHealth" :loading="analyticsLoading" />
      <AlertsPanel :items="alerts" />
    </div>
  </q-page>
</template>

<style scoped lang="scss">
.metric-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.analysis-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 8px;
  padding: 16px;

  &__header {
    margin-bottom: 12px;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 13px;
    font-weight: 600;
    color: var(--pq-text);
    text-transform: uppercase;
    letter-spacing: 0.04em;
    margin: 0;
  }

  &__hint {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    color: var(--pq-text-muted);
  }
}

.analysis-vital {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}
</style>
