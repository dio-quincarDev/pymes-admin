import { shallowRef, computed, watch, readonly } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';
import { usePeriod } from './usePeriod';
import { analyticsService } from '../services/analytics.service';

// ponytail: proyección mensual = solo facturas PAGADA promedio proyectado. Sin fijo de config, sin duplicar.
export function useMonthlyProjection() {
  const authStore = useAuthStore();
  const { period } = usePeriod();

  const projectedMonthly = shallowRef(0);
  const avgDailySpend = shallowRef(0);
  const loading = shallowRef(false);
  const error = shallowRef<string | null>(null);

  // ponytail: con <3 meses de datos el promedio no es fiable
  const confianzaBaja = computed(() => projectedMonthly.value === 0);

  const proyeccionMensual = computed(() => projectedMonthly.value);

  const breakdown = computed(() => ({
    promedioDiario: avgDailySpend.value,
    total: projectedMonthly.value,
  }));

  async function fetch(signal?: AbortSignal) {
    const tenantId = authStore.user?.tenantId;
    if (!tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const analyticsRes = await analyticsService.consultar(tenantId, period.value);
      if (signal?.aborted) return;
      const opex = analyticsRes.data?.opexPct?.[0];
      // opex.projectedMonthly = avgDailySpend * daysInMonth (solo PAGADA, sin fijo de config)
      // fallback a projection[0] si opex vacío
      const projected =
        opex?.projectedMonthly ??
        (analyticsRes.data?.projection?.[0] as unknown as { projectedSpend?: number })?.projectedSpend ??
        0;
      projectedMonthly.value = Number(projected) || 0;
      avgDailySpend.value = opex?.avgDailySpend ?? 0;
    } catch (e: unknown) {
      if (signal?.aborted) return;
      error.value = e instanceof Error ? e.message : 'Error cargando proyección';
    } finally {
      if (!signal?.aborted) loading.value = false;
    }
  }

  watch(
    [period, () => authStore.user?.tenantId],
    (_val, _old, onCleanup) => {
      const ctrl = new AbortController();
      onCleanup(() => ctrl.abort());
      void fetch(ctrl.signal);
    },
    { immediate: true },
  );

  return {
    proyeccionMensual: readonly(proyeccionMensual),
    breakdown: readonly(breakdown),
    confianzaBaja: readonly(confianzaBaja),
    loading: readonly(loading),
    error: readonly(error),
    periodo: readonly(period),
    fetch,
  };
}
