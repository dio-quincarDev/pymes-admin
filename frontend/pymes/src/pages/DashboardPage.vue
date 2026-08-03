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
import { usePullToRefresh } from 'src/composables/usePullToRefresh';

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
  costoDiario,
  loading,
  error,
  periodo,
  setPeriod,
  fetch,
  recalcular,
} = useFinancialDashboard();

const { pullDistance, isRefreshing } = usePullToRefresh({ onRefresh: fetch });

function deltaPct(current: number, previous: number): number | undefined {
  if (!previous || previous === 0) return undefined;
  return +((current - previous) / Math.abs(previous) * 100).toFixed(1);
}

function sparkline(prev: number, cur: number): number[] {
  return [prev, cur];
}

interface KpiItem {
  label: string;
  value: string;
  delta: number | undefined;
  deltaLabel: string;
  trend: number[] | undefined;
  accent: 'gold' | 'green' | 'red' | 'blue';
}

const kpis = computed<KpiItem[]>(() => {
  const m = metricas.value;
  const p = metricasPrev.value;
  if (!m) return [];
  return [
    {
      label: 'Ingresos',
      value: formatCurrency(m.totalIngresos),
      delta: p ? deltaPct(m.totalIngresos, p.totalIngresos) : undefined,
      deltaLabel: 'vs mes anterior',
      trend: p ? sparkline(p.totalIngresos, m.totalIngresos) : undefined,
      accent: 'gold' as const,
    },
    {
      label: 'Costos',
      value: formatCurrency(m.costoMercaderia),
      delta: p ? deltaPct(m.costoMercaderia, p.costoMercaderia) : undefined,
      deltaLabel: 'vs mes anterior',
      trend: p ? sparkline(p.costoMercaderia, m.costoMercaderia) : undefined,
      accent: 'red' as const,
    },
    {
      label: 'Margen Bruto',
      value: formatPercent(m.margenBrutoPct),
      delta: p ? deltaPct(m.margenBrutoPct, p.margenBrutoPct) : undefined,
      deltaLabel: 'vs mes anterior',
      trend: p ? sparkline(p.margenBrutoPct, m.margenBrutoPct) : undefined,
      accent: 'green' as const,
    },
    {
      label: 'Gastos Operativos',
      value: formatCurrency(m.gastosOperativos),
      delta: p ? deltaPct(m.gastosOperativos, p.gastosOperativos) : undefined,
      deltaLabel: 'vs mes anterior',
      trend: p ? sparkline(p.gastosOperativos, m.gastosOperativos) : undefined,
      accent: 'blue' as const,
    },
    ...(costoKpi ? [costoKpi] : []),
  ];
});

const costoKpi: KpiItem | null = costoDiario.value
  ? {
      label: 'Costo / Día',
      value: formatCurrency(costoDiario.value.costoOperativoDiario),
      delta:
        costoDiario.value.costoOperativoDiario > 0
          ? +(
              ((costoDiario.value.ventasHoy - costoDiario.value.costoOperativoDiario) /
                costoDiario.value.costoOperativoDiario) *
              100
            ).toFixed(1)
          : undefined,
      deltaLabel: 'vs costo diario',
      trend: undefined,
      accent:
        costoDiario.value.ventasHoy >= costoDiario.value.costoOperativoDiario ? 'green' : 'red',
    }
  : null;

const categoryItems = computed(() =>
  gastosPorCategoria.value.map((g) => {
    const prev = gastosPorCategoriaPrev.value.find((p) => p.categoria === g.categoria);
    return {
      category: g.categoria,
      currentAmount: g.total,
      previousAmount: prev?.total,
      percentage: g.pct,
    };
  }),
);
</script>

<template>
  <q-page class="dashboard-page">
    <transition name="ptr">
      <div
        v-show="isRefreshing || pullDistance > 0"
        class="ptr-indicator"
        :style="{ height: `${isRefreshing ? 44 : pullDistance}px` }"
        role="status"
        aria-live="polite"
      >
        <q-spinner v-if="isRefreshing" size="20px" color="accent" />
        <q-icon v-else name="arrow_downward" size="20px" color="accent" />
      </div>
    </transition>

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
          :trend="kpi.trend"
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

.ptr-indicator {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--pq-surface);
  border-bottom: 1px solid var(--pq-border);
  pointer-events: none;
}

.ptr-enter-active,
.ptr-leave-active {
  transition: opacity var(--pq-motion-fast);
}

.ptr-enter-from,
.ptr-leave-to {
  opacity: 0;
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
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
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
