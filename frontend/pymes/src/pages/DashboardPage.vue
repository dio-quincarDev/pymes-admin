<script setup lang="ts">
import { computed } from 'vue';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useFinancialDashboard } from 'src/modules/core/composables/useFinancialDashboard';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import AnalyticsHeader from 'src/modules/core/components/analytics/AnalyticsHeader.vue';
import KpiCard from 'src/modules/core/components/analytics/KpiCard.vue';
import CategoryBreakdownChart from 'src/modules/core/components/analytics/CategoryBreakdownChart.vue';
import RecentActivity from 'src/modules/core/components/dashboard/RecentActivity.vue';
import PendingInvoices from 'src/modules/core/components/dashboard/PendingInvoices.vue';
import QuickActions from 'src/modules/core/components/dashboard/QuickActions.vue';
import BaseButton from 'src/components/base/BaseButton.vue';

useMeta({ title: 'Dashboard — PYMEQ' });

const authStore = useAuthStore();
const hasTenant = computed(() => !!authStore.user?.tenantId);
const { formatCurrency, formatPercent } = useNumberFormat();

const {
  metricas,
  metricasPrev,
  gastosPorCategoria,
  gastosPorCategoriaPrev,
  actividadReciente,
  facturasPendientes,
  loading,
  error,
  periodo,
  setPeriod,
  recalcular,
} = useFinancialDashboard();

function deltaPct(current: number, previous: number): number | undefined {
  if (!previous || previous === 0) return undefined;
  return +((current - previous) / Math.abs(previous) * 100).toFixed(1);
}

const kpis = computed(() => {
  const m = metricas.value;
  const p = metricasPrev.value;
  if (!m) return [];
  return [
    {
      label: 'Ingresos',
      value: formatCurrency(m.totalIncome),
      delta: p ? deltaPct(m.totalIncome, p.totalIncome) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'gold' as const,
    },
    {
      label: 'Costos',
      value: formatCurrency(m.costOfGoods),
      delta: p ? deltaPct(m.costOfGoods, p.costOfGoods) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'red' as const,
    },
    {
      label: 'Margen Bruto',
      value: formatPercent(m.grossMarginPct),
      delta: p ? deltaPct(m.grossMarginPct, p.grossMarginPct) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'green' as const,
    },
    {
      label: 'Gastos Operativos',
      value: formatCurrency(m.operatingExpenses),
      delta: p ? deltaPct(m.operatingExpenses, p.operatingExpenses) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'blue' as const,
    },
  ];
});

const categoryItems = computed(() =>
  gastosPorCategoria.value.map((g) => {
    const prev = gastosPorCategoriaPrev.value.find((p) => p.category === g.category);
    return {
      category: g.category,
      currentAmount: g.total,
      previousAmount: prev?.total,
      percentage: g.pct,
    };
  }),
);
</script>

<template>
  <q-page class="dashboard-page">
    <template v-if="!hasTenant">
      <div class="no-tenant-state">
        <q-icon name="domain_disabled" size="64px" style="color: var(--pq-text-subtle)" aria-hidden="true" />
        <h1 class="no-tenant-headline">Tu negocio aún no está configurado</h1>
        <p class="no-tenant-copy">Completá el onboarding para empezar a usar PymeQ.</p>
        <BaseButton variant="primary" size="lg" @click="$router.push('/onboarding')">
          COMPLETAR ONBOARDING
        </BaseButton>
        <p class="no-tenant-hint">¿Ya empezaste? Revisá tu correo para el enlace de verificación.</p>
      </div>
    </template>

    <template v-else>
      <AnalyticsHeader
        title="Dashboard"
        subtitle="Resumen financiero del período"
        :period="periodo"
        :loading="loading"
        @update:period="setPeriod"
        @recalculate="recalcular"
      />

      <div v-if="error && !loading" class="dashboard-error-banner">
        <q-icon name="error_outline" size="18px" />
        <span>{{ error }}</span>
        <q-btn flat dense no-caps label="Reintentar" class="dashboard-error-banner__retry" @click="recalcular" />
      </div>

      <div class="kpi-row stagger-children">
        <KpiCard
          v-for="kpi in kpis"
          :key="kpi.label"
          :label="kpi.label"
          :value="kpi.value"
          :delta="kpi.delta"
          :delta-label="kpi.deltaLabel"
          :accent="kpi.accent"
          :loading="loading"
        />
      </div>

      <div class="dashboard-content">
        <div class="dashboard-content__main">
          <CategoryBreakdownChart
            :items="categoryItems"
            :loading="loading"
            :empty="categoryItems.length === 0"
          />
          <RecentActivity :actividades="actividadReciente" :loading="loading" />
        </div>
        <div class="dashboard-content__side">
          <PendingInvoices :facturas="facturasPendientes" :loading="loading" />
          <QuickActions />
        </div>
      </div>
    </template>
  </q-page>
</template>

<style scoped lang="scss">
.dashboard-page {
  width: 100%;
}

.dashboard-error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 24px;
  background: rgba(160, 64, 56, 0.1);
  border: 1px solid rgba(160, 64, 56, 0.2);
  border-radius: 6px;
  font-family: 'Satoshi', sans-serif;
  font-size: 13px;
  color: var(--pq-danger);

  &__retry {
    margin-left: auto;
    color: var(--pq-danger);
    font-weight: 500;
  }
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 1024px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 639px) {
    grid-template-columns: 1fr;
  }
}

.dashboard-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }

  &__main,
  &__side {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
}

.no-tenant-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-height: 60vh;
  gap: 20px;
  max-width: 480px;
  margin: 0 auto;
}

.no-tenant-headline {
  font-family: 'Geist', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--pq-text);
  margin: 0;
}

.no-tenant-copy {
  font-family: 'Satoshi', sans-serif;
  font-size: 16px;
  font-weight: 400;
  color: var(--pq-text-muted);
  margin: 0;
  max-width: 35ch;
}

.no-tenant-hint {
  font-family: 'Satoshi', sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: var(--pq-text-subtle);
  margin: 8px 0 0;
}
</style>
