<script setup lang="ts">
import { onMounted } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import { useAnalytics } from '../composables/useAnalytics';
import { useAnalisisGastos } from '../composables/useAnalisisGastos';
import AnalyticsHeader from 'src/modules/core/components/analytics/AnalyticsHeader.vue';
import MetricCard from 'src/modules/core/components/analytics/MetricCard.vue';
import CategoryBreakdownChart from 'src/modules/core/components/analytics/CategoryBreakdownChart.vue';
import AlertsPanel from '../components/dashboard/AlertsPanel.vue';
import FinancialHealthPanel from '../components/dashboard/FinancialHealthPanel.vue';

useMeta({ title: 'Análisis de Gastos — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;
const { formatCurrency } = useNumberFormat();

const { period, setPeriod, recalcular: recalcularAnalytics, loading: analyticsLoading, alerts, financialHealth } =
  useAnalytics();

const { totalInvestment, productCount, byCategory, categoryChartItems, loading, load } =
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

    <CategoryBreakdownChart
      :items="categoryChartItems"
      :loading="loading"
      :empty="categoryChartItems.length === 0"
    />

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
