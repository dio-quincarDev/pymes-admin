import { ref, computed, watch } from 'vue';
import { analyticsService } from '../services/analytics.service';
import type {
  AnalyticsResponse,
  AnalyticsResponseWire,
  AbcItem,
  AbcItemWire,
  TrendItem,
  MarginItem,
  OpexItem,
  ProjectionItem,
  AlertItem,
  SupplierComparisonItem,
  SupplierRecommendationItem,
  PricePredictionItem,
  FinancialHealth,
  FinancialHealthAlert,
  FinancialHealthWire,
  FinancialHealthAlertWire,
} from '../types/analytics';
import { useAuthStore } from 'src/modules/auth/store';
import { usePeriod } from './usePeriod';

export function useAnalytics() {
  const authStore = useAuthStore();
  const { period, setPeriod } = usePeriod();
  const data = ref<AnalyticsResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  function toNumber(v: unknown, fallback = 0): number {
    if (typeof v === 'number' && Number.isFinite(v)) return v;
    if (typeof v === 'string') {
      const n = Number(v);
      return Number.isFinite(n) ? n : fallback;
    }
    return fallback;
  }

  function normalizeAbc(items: AbcItemWire[]): AbcItem[] {
    return items.map((i) => ({
      productId: i.productId,
      productName: i.productName,
      spend: toNumber(i.spend ?? i.totalSpend),
      pctTotal: toNumber(i.pctTotal ?? i.pct),
      cumulativePct: toNumber(i.cumulativePct),
      category: i.category,
    }));
  }

  function normalizeFinancialHealth(wire: FinancialHealthWire | FinancialHealth | undefined): FinancialHealth | null {
    if (!wire) return null;
    const w = wire as FinancialHealthWire;
    const alerts: FinancialHealthAlert[] = (w.criticalAlerts ?? []).map((a: FinancialHealthAlertWire) => ({
      code: a.code ?? a.type ?? '',
      title: a.title ?? '',
      description: a.description ?? a.message ?? '',
      current: toNumber(a.current ?? a.metric),
      threshold: toNumber(a.threshold),
      action: a.action ?? '',
    }));
    return {
      overallHealth: toNumber(w.overallHealth),
      breakdown: w.breakdown ?? {},
      criticalAlerts: alerts,
      investmentSignals: w.investmentSignals ?? [],
      expansionReadiness: w.expansionReadiness ?? { score: 0, status: 'SIN_DATOS', requirements: [] },
      recommendations: w.recommendations ?? [],
    };
  }

  async function fetch() {
    if (!authStore.user?.tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const res = await analyticsService.consultar(
        authStore.user.tenantId,
        period.value,
      );
      const wire = res.data as unknown as AnalyticsResponseWire;
      data.value = {
        ...wire,
        abc: normalizeAbc(wire.abc ?? []),
        financialHealth: normalizeFinancialHealth(wire.financialHealth as FinancialHealthWire),
      } as AnalyticsResponse;
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Error cargando analytics';
    } finally {
      loading.value = false;
    }
  }

  async function recalcular() {
    if (!authStore.user?.tenantId) return;
    loading.value = true;
    try {
      const res = await analyticsService.recalcular(
        authStore.user.tenantId,
        period.value,
      );
      const wire = res.data as unknown as AnalyticsResponseWire;
      data.value = {
        ...wire,
        abc: normalizeAbc(wire.abc ?? []),
        financialHealth: normalizeFinancialHealth(wire.financialHealth as FinancialHealthWire),
      } as AnalyticsResponse;
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Error recalculando';
    } finally {
      loading.value = false;
    }
  }

  const abc = computed<AbcItem[]>(() => data.value?.abc ?? []);
  const trend = computed<TrendItem[]>(() => data.value?.trend ?? []);
  const margin = computed<MarginItem[]>(() => data.value?.margin ?? []);
  const opexPct = computed<OpexItem[]>(() => data.value?.opexPct ?? []);
  const projection = computed<ProjectionItem[]>(
    () => data.value?.projection ?? [],
  );
  const alerts = computed<AlertItem[]>(() => data.value?.alerts ?? []);
  const supplierComparison = computed<SupplierComparisonItem[]>(
    () => data.value?.supplierComparison ?? [],
  );
  const supplierRecommendations = computed<SupplierRecommendationItem[]>(
    () => data.value?.supplierRecommendations ?? [],
  );
  const pricePrediction = computed<PricePredictionItem[]>(
    () => data.value?.pricePrediction ?? [],
  );
  const financialHealth = computed<FinancialHealth | null>(
    () => data.value?.financialHealth ?? null,
  );
  const criticalAlerts = computed<FinancialHealthAlert[]>(
    () => financialHealth.value?.criticalAlerts ?? [],
  );
  const recommendations = computed<string[]>(
    () => financialHealth.value?.recommendations ?? [],
  );

  watch(period, fetch, { immediate: true });

  return {
    data,
    loading,
    error,
    period,
    setPeriod,
    fetch,
    recalcular,
    abc,
    trend,
    margin,
    opexPct,
    projection,
    alerts,
    supplierComparison,
    supplierRecommendations,
    pricePrediction,
    financialHealth,
    criticalAlerts,
    recommendations,
  };
}
