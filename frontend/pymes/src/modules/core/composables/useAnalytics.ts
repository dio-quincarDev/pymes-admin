import { ref, computed, watch } from 'vue';
import { analyticsService } from '../services/analytics.service';
import type {
  AnalyticsResponse,
  AbcItem,
  TrendItem,
  MarginItem,
  OpexItem,
  ProjectionItem,
  AlertItem,
} from '../types/analytics';
import { useAuthStore } from 'src/modules/auth/store';
import { usePeriod } from './usePeriod';

export function useAnalytics() {
  const authStore = useAuthStore();
  const { period, setPeriod } = usePeriod();
  const data = ref<AnalyticsResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetch() {
    if (!authStore.user?.tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const res = await analyticsService.consultar(
        authStore.user.tenantId,
        period.value,
      );
      data.value = res.data;
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
      data.value = res.data;
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
  };
}
