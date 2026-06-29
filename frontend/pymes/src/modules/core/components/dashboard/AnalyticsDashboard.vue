<template>
  <div class="analytics-dashboard">
    <div class="dashboard-header row items-center justify-between q-mb-lg">
      <div>
        <h2 class="text-h5 text-primary q-ma-none">Análisis de Gastos</h2>
        <span class="text-caption text-accent">Período: {{ period }}</span>
      </div>
      <PeriodSelector
        :model-value="period"
        :loading="loading"
        @update:model-value="setPeriod"
        @recalcular="recalcular"
      />
    </div>

    <div v-if="loading" class="row q-col-gutter-lg">
      <div class="col-12"><SkeletonLoader :is-loading="true" :count="6" layout="stats" /></div>
    </div>

    <div v-else class="row q-col-gutter-lg">
      <div class="col-12 col-sm-6 col-lg-3" v-for="kpi in kpis" :key="kpi.label">
        <KpiCard v-bind="kpi" />
      </div>

      <div class="col-12 col-xl-8">
        <div class="analytics-card">
          <div class="analytics-card__title">Clasificación ABC de Gastos</div>
          <AbcGastosChart :data="abc" :height="300" />
        </div>
      </div>

      <div class="col-12 col-xl-4">
        <AlertsPanel :items="alerts" />
      </div>

      <div class="col-12 col-lg-6">
        <div class="analytics-card">
          <q-tabs v-model="trendTab" class="q-mb-md">
            <q-tab name="trend" label="Tendencias Precios" no-caps />
            <q-tab name="margin" label="Impacto Márgenes" no-caps />
          </q-tabs>
          <PriceTrendSparkline v-if="trendTab === 'trend'" :items="trend" />
          <MarginImpactTable v-else :items="margin" />
        </div>
      </div>

      <div class="col-12 col-lg-6">
        <div class="analytics-card">
          <div class="analytics-card__title">Costo Operativo</div>
          <OpexGauge
            :value="opexValue"
            :max="100"
            :thresholds="{ warning: 70, critical: 85 }"
          />
          <div class="q-mt-md text-caption text-accent text-center">
            Proy. mensual: {{ formatCurrency(opexProjected) }}
          </div>
        </div>
        <div class="analytics-card q-mt-lg">
          <div class="analytics-card__title">Proyección 30/60/90d</div>
          <ProjectionTimeline :items="projection" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAnalytics } from '../../composables/useAnalytics';
import { useNumberFormat } from '../../composables/useNumberFormat';
import KpiCard from './KpiCard.vue';
import AbcGastosChart from './AbcGastosChart.vue';
import PriceTrendSparkline from './PriceTrendSparkline.vue';
import MarginImpactTable from './MarginImpactTable.vue';
import OpexGauge from './OpexGauge.vue';
import ProjectionTimeline from './ProjectionTimeline.vue';
import AlertsPanel from './AlertsPanel.vue';
import PeriodSelector from './PeriodSelector.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';

const {
  loading,
  period,
  setPeriod,
  recalcular,
  abc,
  trend,
  margin,
  opexPct,
  projection,
  alerts,
} = useAnalytics();
const { formatCurrency } = useNumberFormat();
const trendTab = ref<'trend' | 'margin'>('trend');

const kpis = computed(() => [
  {
    label: 'Gasto Total',
    value: formatCurrency(opexPct.value[0]?.totalSpend ?? 0),
    accent: 'copper' as const,
    icon: 'receipt_long',
  },
  {
    label: 'Proy. Mensual',
    value: formatCurrency(opexPct.value[0]?.projectedMonthly ?? 0),
    delta: '+5%',
    trend: 'up' as const,
    accent: 'sage' as const,
    icon: 'trending_up',
  },
  {
    label: 'Productos (ABC-A)',
    value: String(abc.value.filter((a) => a.category === 'A').length),
    accent: 'gold' as const,
    icon: 'inventory_2',
  },
  {
    label: 'Alertas Críticas',
    value: String(alerts.value.filter((a) => a.severity === 'critical').length),
    delta: alerts.value.length > 0 ? 'Revisar' : '',
    trend: alerts.value.length > 0 ? ('down' as const) : ('up' as const),
    accent: alerts.value.some((a) => a.severity === 'critical')
      ? ('negative' as const)
      : ('positive' as const),
    icon: 'warning',
  },
]);

const opexValue = computed(() => {
  const item = opexPct.value[0];
  if (!item?.totalSpend) return 0;
  return (item.totalSpend / (item.projectedMonthly || 1)) * 100;
});

const opexProjected = computed(() => opexPct.value[0]?.projectedMonthly ?? 0);
</script>

<style scoped lang="scss">
.analytics-dashboard {
  width: 100%;
}

.analytics-card {
  background: rgba(11, 18, 16, 0.5);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(163, 120, 94, 0.1);
  border-radius: 8px;
  padding: 1.25rem;

  &__title {
    font-size: 0.85rem;
    font-weight: 600;
    color: #E2E8E4;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 1rem;
  }
}
</style>
