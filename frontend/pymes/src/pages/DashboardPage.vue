<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useTutorial } from 'src/composables/useTutorial';
import { useFinancialDashboard } from 'src/modules/core/composables/useFinancialDashboard';
import { useAnalytics } from 'src/modules/core/composables/useAnalytics';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import { toLocalISODate } from 'src/utils/format';
import AnalyticsHeader from 'src/modules/core/components/analytics/AnalyticsHeader.vue';
import CategoryBreakdownChart from 'src/modules/core/components/analytics/CategoryBreakdownChart.vue';
import ActivityPanel from 'src/modules/core/components/dashboard/ActivityPanel.vue';
import FinancialHealthPanel from 'src/modules/core/components/dashboard/FinancialHealthPanel.vue';
import KpiStrip from 'src/modules/core/components/dashboard/KpiStrip.vue';
import VentasVsCostosChart from 'src/modules/core/components/dashboard/VentasVsCostosChart.vue';
import { usePullToRefresh } from 'src/composables/usePullToRefresh';

useMeta({ title: 'Dashboard — PYMEQ' });

const authStore = useAuthStore();
const hasTenant = computed(() => !!authStore.user?.tenantId);
const { formatCurrency } = useNumberFormat();
const { showForCurrentRoute } = useTutorial();

onMounted(() => {
  void showForCurrentRoute();
});

const {
  metricas,
  gastosPorCategoria,
  gastosPorCategoriaPrev,
  ventas,
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

const { financialHealth, supplierRecommendations, loading: analyticsLoading } = useAnalytics();
const { pullDistance, isRefreshing } = usePullToRefresh({ onRefresh: fetch });

const stripKpis = computed(() => {
  const m = metricas.value;
  const cd = costoDiario.value;

  const pendientes = facturasPendientes.value.length;

  return [
    {
      label: 'Costos día',
      value: cd ? formatCurrency(cd.costoOperativoDiario) : '—',
      accent: 'red' as const,
    },
    {
      label: 'Facturas pendientes',
      value: String(pendientes),
      accent: pendientes > 0 ? ('red' as const) : ('green' as const),
    },
    {
      label: 'Rentabilidad',
      value: m ? `${(m.margenNetoPct ?? 0).toFixed(1)}%` : '—',
      accent: !m ? ('gold' as const) : m.margenNetoPct >= 0 ? ('green' as const) : ('red' as const),
    },
  ];
});

const chartData = computed(() => {
  const days: { label: string; ventas: number; costos: number }[] = [];
  const now = new Date();
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    const dateStr = toLocalISODate(d);
    const dayLabel = d.toLocaleDateString('es-PA', { weekday: 'short', day: 'numeric' });
    const ventasDia = ventas.value
      .filter(v => v.fecha === dateStr)
      .reduce((s, v) => s + v.montoBruto, 0);
    const costosDia = costoDiario.value?.costoOperativoDiario ?? 0;
    days.push({ label: dayLabel, ventas: ventasDia, costos: costosDia });
  }
  return days;
});

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
        <q-btn color="primary" size="lg" @click="$router.push('/onboarding')">
          COMPLETAR ONBOARDING
        </q-btn>
        <p class="no-tenant-hint">¿Ya empezaste? Revisá tu correo para el enlace de verificación.</p>
      </div>
    </template>

    <template v-else>
      <div data-tour="dashboard">
        <AnalyticsHeader
          title="Dashboard"
          subtitle="Cómo está mi negocio hoy"
          :period="periodo"
          :loading="loading"
          @update:period="setPeriod"
          @recalculate="recalcular"
        />
      </div>

      <div v-if="error && !loading" class="dashboard-error-banner">
        <q-icon name="error_outline" size="18px" />
        <span>{{ error }}</span>
        <q-btn flat dense no-caps label="Reintentar" class="dashboard-error-banner__retry" @click="recalcular" />
      </div>

      <!-- KPI Strip — full width, no card background, editorial -->
      <KpiStrip :kpis="stripKpis" :loading="loading" />

      <!-- Hairline separator -->
      <div class="rule" />

      <!-- Chart — single, full width -->
      <VentasVsCostosChart :data="chartData" :loading="loading" />

      <!-- Hairline separator -->
      <div class="rule" />

      <!-- Secondary panels — 2 cols desktop, 1 col mobile. Hairlines, no cards -->
      <div class="dashboard-secondary">
        <CategoryBreakdownChart
          :items="categoryItems"
          :loading="loading"
          :empty="categoryItems.length === 0"
        />
        <FinancialHealthPanel :data="financialHealth" :loading="analyticsLoading" :recommendations="supplierRecommendations" />
      </div>

      <!-- Hairline separator -->
      <div class="rule" />

      <!-- Activity — table, not cards -->
      <ActivityPanel
        :actividades="actividadReciente"
        :facturas="facturasPendientes"
        :loading="loading"
      />

      <!-- Nav links — text, not buttons -->
      <nav class="dashboard-nav" aria-label="Accesos rápidos">
        <a class="dashboard-nav__link" @click="$router.push('/dashboard/analisis-gastos')">
          Análisis de gastos →
        </a>
        <a class="dashboard-nav__link" @click="$router.push('/dashboard/patrimonio')">
          Inversión y patrimonio →
        </a>
      </nav>
    </template>
  </q-page>
</template>

<style scoped lang="scss">
.dashboard-page {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: hidden;
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
    min-height: 32px;
    display: inline-flex;
    align-items: center;
  }
}

/* --------------------------------------------------
   Hairline rule — separador editorial
   -------------------------------------------------- */
.rule {
  height: 1px;
  background: var(--pq-border);
  margin: 32px 0;
  opacity: 0.4;
}

/* --------------------------------------------------
   Secondary panels — 2 cols desktop, 1 col mobile
   -------------------------------------------------- */
.dashboard-secondary {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  align-items: start;

  @media (min-width: 768px) {
    grid-template-columns: 1fr 1fr;
    gap: 32px;
  }
}

/* --------------------------------------------------
   Nav links — texto, no botones
   -------------------------------------------------- */
.dashboard-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--pq-border);

  @media (min-width: 640px) {
    flex-direction: row;
    gap: 24px;
  }
}

.dashboard-nav__link {
  font-family: 'Satoshi', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--pq-text-muted);
  cursor: pointer;
  transition: color var(--pq-motion-fast);
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  padding: 0 4px;

  &:hover {
    color: var(--pq-accent);
  }
}

.no-tenant-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-height: 60dvh;
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
