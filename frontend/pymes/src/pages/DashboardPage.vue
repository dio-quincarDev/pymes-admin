<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useFinancialDashboard } from 'src/modules/core/composables/useFinancialDashboard';
import { useAnalytics } from 'src/modules/core/composables/useAnalytics';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';
import AnalyticsHeader from 'src/modules/core/components/analytics/AnalyticsHeader.vue';
import CategoryBreakdownChart from 'src/modules/core/components/analytics/CategoryBreakdownChart.vue';
import ActivityPanel from 'src/modules/core/components/dashboard/ActivityPanel.vue';
import FinancialHealthPanel from 'src/modules/core/components/dashboard/FinancialHealthPanel.vue';
import KpiStrip from 'src/modules/core/components/dashboard/KpiStrip.vue';
import ResumenCard from 'src/modules/core/components/dashboard/ResumenCard.vue';
import InversionCard from 'src/modules/core/components/dashboard/InversionCard.vue';
import RegistrarVentaDialog from 'src/modules/core/components/dashboard/RegistrarVentaDialog.vue';
import VentasVsCostosChart from 'src/modules/core/components/dashboard/VentasVsCostosChart.vue';
import { usePullToRefresh } from 'src/composables/usePullToRefresh';
import { patrimonioService } from 'src/modules/core/services/patrimonio.service';
import { prestamoService } from 'src/modules/core/services/prestamo.service';
import type { Patrimonio, Prestamo } from 'src/modules/core/types';

useMeta({ title: 'Dashboard — PYMEQ' });

const authStore = useAuthStore();
const hasTenant = computed(() => !!authStore.user?.tenantId);
const tenantId = computed(() => authStore.user?.tenantId ?? '');
const { formatCurrency } = useNumberFormat();

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

const { financialHealth, loading: analyticsLoading } = useAnalytics();
const { pullDistance, isRefreshing } = usePullToRefresh({ onRefresh: fetch });

// Patrimonio
const patrimonio = ref<Patrimonio | null>(null);
const prestamos = ref<Prestamo[]>([]);

async function loadPatrimonio() {
  if (!tenantId.value) return;
  try {
    const [pRes, prRes] = await Promise.all([
      patrimonioService.get(tenantId.value),
      prestamoService.getAll(tenantId.value),
    ]);
    patrimonio.value = pRes.data;
    prestamos.value = prRes.data;
  } catch {
    // ponytail: silent fail, card shows fallback
  }
}

onMounted(loadPatrimonio);

// Dialog
const showRegistrarVenta = ref(false);

function onVentaCreada() {
  void fetch();
  void loadPatrimonio();
}

// KPIs for strip
const stripKpis = computed(() => {
  const m = metricas.value;
  const cd = costoDiario.value;
  if (!m && !cd) return [];

  const items = [];

  if (cd) {
    const margen = cd.ventasHoy - cd.costoOperativoDiario;
    items.push({
      label: 'Ventas hoy',
      value: formatCurrency(cd.ventasHoy),
      accent: 'gold' as const,
    });
    items.push({
      label: 'Costos día',
      value: formatCurrency(cd.costoOperativoDiario),
      accent: 'red' as const,
    });
    items.push({
      label: 'Margen',
      value: formatCurrency(margen),
      accent: margen >= 0 ? ('green' as const) : ('red' as const),
    });
  }

  if (m) {
    items.push({
      label: 'ROI mes',
      value: `${(m.margenNetoPct ?? 0).toFixed(1)}%`,
      accent: m.margenNetoPct >= 0 ? ('green' as const) : ('red' as const),
    });
  }

  return items;
});

// Resumen data
const resumenVentas = computed(() => costoDiario.value?.ventasHoy ?? 0);
const resumenCostos = computed(() => costoDiario.value?.costoOperativoDiario ?? 0);
const resumenMargen = computed(() => resumenVentas.value - resumenCostos.value);
const resumenCantidadVentas = computed(() => {
  const today = new Date().toISOString().slice(0, 10);
  return ventas.value.filter(v => v.fecha === today).length;
});

// Inversión data
const capitalInicial = computed(() => patrimonio.value?.capitalInicial ?? 0);
const mesesRecuperacion = computed(() => {
  if (!patrimonio.value) return null;
  const capital = patrimonio.value.capitalInicial;
  const deudaActiva = prestamos.value
    .filter(p => p.estado === 'ACTIVO')
    .reduce((s, p) => s + p.saldoPendiente, 0);
  const total = capital + deudaActiva;
  const m = metricas.value;
  if (!m || m.margenNeto <= 0) return null;
  return Math.ceil(total / ((m.totalIngresos * m.margenNetoPct) / 100));
});

// Chart data — últimos 7 días
const chartData = computed(() => {
  const days: { label: string; ventas: number; costos: number }[] = [];
  const now = new Date();
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    const dateStr = d.toISOString().slice(0, 10);
    const dayLabel = d.toLocaleDateString('es-PE', { weekday: 'short' });
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
      <AnalyticsHeader
        title="Dashboard"
        subtitle="Cómo está mi negocio hoy"
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

      <!-- Quick actions -->
      <div class="dashboard-actions">
        <q-btn
          no-caps
          icon="sym_r_add"
          label="Registrar venta"
          color="positive"
          class="dashboard-actions__btn"
          @click="showRegistrarVenta = true"
        />
        <q-btn
          no-caps
          icon="sym_r_analytics"
          label="Análisis"
          outline
          class="dashboard-actions__btn"
          @click="$router.push('/dashboard/analisis-gastos')"
        />
        <q-btn
          no-caps
          icon="sym_r_account_balance"
          label="Inversión"
          outline
          class="dashboard-actions__btn"
          @click="$router.push('/dashboard/patrimonio')"
        />
      </div>

      <!-- KPI Strip -->
      <KpiStrip :kpis="stripKpis" :loading="loading" />

      <!-- Main grid: Resumen + Inversión -->
      <div class="dashboard-grid">
        <ResumenCard
          :ventas-hoy="resumenVentas"
          :costos-dia="resumenCostos"
          :margen="resumenMargen"
          :cantidad-ventas="resumenCantidadVentas"
          :loading="loading"
        />
        <InversionCard
          :capital-inicial="capitalInicial"
          :meses-recuperacion="mesesRecuperacion"
          :loading="loading"
        />
      </div>

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
        <FinancialHealthPanel :data="financialHealth" :loading="analyticsLoading" />
      </div>

      <!-- Dialog -->
      <RegistrarVentaDialog v-model="showRegistrarVenta" @created="onVentaCreada" />
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

.dashboard-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;

  &__btn {
    font-family: 'Satoshi', sans-serif;
    font-weight: 600;
    border-radius: 6px;
  }
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.dashboard-secondary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 24px;

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
