<script setup lang="ts">
import { computed, shallowRef, onMounted } from 'vue';
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
import RegistrarVentaDialog from 'src/modules/core/components/dashboard/RegistrarVentaDialog.vue';
import RegistrarVentaInlineBar from 'src/modules/core/components/dashboard/RegistrarVentaInlineBar.vue';
import VentasVsCostosChart from 'src/modules/core/components/dashboard/VentasVsCostosChart.vue';
import { usePullToRefresh } from 'src/composables/usePullToRefresh';

useMeta({ title: 'Dashboard — PYMEQ' });

const authStore = useAuthStore();
const hasTenant = computed(() => !!authStore.user?.tenantId);
const isWriter = computed(() => ['OWNER', 'ADMIN'].includes(authStore.user?.role ?? ''));
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

// Inline quick capture — composition surface only (logic lives in RegistrarVentaInlineBar + useRegistrarVenta)
const showInline = shallowRef(false);
const showRegistrarVenta = shallowRef(false);

function onVentaCreada() {
  void fetch();
}

// KPIs for strip — always 3 slots so layout doesn't jump when API is slow/empty (ponytail: derived, no extra state)
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



// Chart data — últimos 7 días (Panamá UTC-5 local, no UTC)
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

// Category items
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

      <!-- Quick actions — hierarchy: 1 primary gold, 2 secondary -->
      <div class="dashboard-actions">
        <q-btn
          no-caps
          icon="sym_r_add"
          label="Registrar venta"
          color="primary"
          text-color="dark"
          class="dashboard-actions__btn dashboard-actions__btn--primary"
          :disable="!isWriter"
          @click="showInline = !showInline"
        >
          <q-tooltip v-if="!isWriter">Solo OWNER/ADMIN</q-tooltip>
        </q-btn>
        <div class="dashboard-actions__secondary">
          <q-btn
            no-caps
            icon="sym_r_analytics"
            label="Análisis"
            outline
            class="dashboard-actions__btn dashboard-actions__btn--secondary"
            @click="$router.push('/dashboard/analisis-gastos')"
          />
          <q-btn
            no-caps
            icon="sym_r_account_balance"
            label="Inversión"
            outline
            class="dashboard-actions__btn dashboard-actions__btn--secondary"
            @click="$router.push('/dashboard/patrimonio')"
          />
        </div>
      </div>

      <!-- Inline quick capture — extracted component (props down, events up) -->
      <RegistrarVentaInlineBar v-if="hasTenant" v-model="showInline" @created="onVentaCreada" />

      <!-- KPI Strip -->
      <KpiStrip :kpis="stripKpis" :loading="loading" />

      <!-- Chart -->
      <VentasVsCostosChart :data="chartData" :loading="loading" />

      <!-- Secondary section -->
      <div class="dashboard-secondary">
        <CategoryBreakdownChart
          :items="categoryItems"
          :loading="loading"
          :empty="categoryItems.length === 0"
        />
        <ActivityPanel
          :actividades="actividadReciente"
          :facturas="facturasPendientes"
          :loading="loading"
        />
        <FinancialHealthPanel :data="financialHealth" :loading="analyticsLoading" :recommendations="supplierRecommendations" />
      </div>

      <!-- Dialog kept for mobile FAB bottom-sheet -->
      <RegistrarVentaDialog v-model="showRegistrarVenta" @created="onVentaCreada" />

      <!-- Mobile FAB — thumb-reach, only writer, hidden when inline already open -->
      <q-page-sticky v-if="hasTenant && isWriter && !showInline" position="bottom-right" :offset="[16, 20]" class="venta-fab">
        <q-btn fab icon="sym_r_add" color="primary" text-color="dark" aria-label="Registrar venta" @click="showInline = true">
          <q-tooltip>Registrar venta</q-tooltip>
        </q-btn>
      </q-page-sticky>
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
  }
}

.dashboard-actions {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;

  &__btn {
    font-family: 'Satoshi', sans-serif;
    font-weight: 600;
    border-radius: 6px;
  }

  &__btn--primary {
    font-weight: 700;
  }

  &__btn--secondary {
    opacity: 0.9;
  }

  &__secondary {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  @media (max-width: 600px) {
    grid-template-columns: 1fr;

    &__btn,
    &__btn--primary,
    &__btn--secondary {
      width: 100%;
    }

    &__secondary {
      flex-direction: column;
      width: 100%;
      justify-content: stretch;
    }
  }
}

.venta-fab {
  // keep above mobile-bottom-nav (q-footer) — z 300
  z-index: 310;

  @media (min-width: 768px) {
    display: none;
  }
}

.dashboard-secondary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 24px;
  align-items: stretch;
  min-width: 0;
  max-width: 100%;

  > * {
    height: 100%;
    min-width: 0;
    max-width: 100%;
    overflow: hidden;
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
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
