import { shallowRef, computed, watch, readonly } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';
import { usePeriod } from './usePeriod';
import { costoService } from '../services/costo.service';
import { analyticsService } from '../services/analytics.service';

// ponytail: proyección mensual combinada frontend-only — fijo (costoDiario) + variable proyectado (analytics opex). Sin endpoint nuevo, promedio 3M futuro.
export function useMonthlyProjection() {
  const authStore = useAuthStore();
  const { period } = usePeriod();

  const costoOperativoMensual = shallowRef(0);
  const variableProyectado = shallowRef(0);
  const loading = shallowRef(false);
  const error = shallowRef<string | null>(null);

  // ponytail: con 1 mes de datos el promedio no es fiable → baja confianza
  const confianzaBaja = computed(() => variableProyectado.value === 0);

  const proyeccionMensual = computed(
    () => costoOperativoMensual.value + variableProyectado.value,
  );

  const breakdown = computed(() => ({
    fijo: costoOperativoMensual.value,
    variable: variableProyectado.value,
    total: proyeccionMensual.value,
  }));

  async function fetch(signal?: AbortSignal) {
    const tenantId = authStore.user?.tenantId;
    if (!tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const [costoRes, analyticsRes] = await Promise.all([
        costoService.getDiario(tenantId),
        analyticsService.consultar(tenantId, period.value),
      ]);
      if (signal?.aborted) return;
      costoOperativoMensual.value = costoRes.data?.costoOperativoMensual ?? 0;
      const opex = analyticsRes.data?.opexPct?.[0];
      // opex.projectedMonthly = avgDailySpend * daysInMonth (motor gastoVariable)
      // fallback a projection[0] si opex vacío (1 mes de datos)
      const projected =
        opex?.projectedMonthly ??
        (analyticsRes.data?.projection?.[0] as unknown as { projection30d?: number; projectedSpend?: number })?.projection30d ??
        (analyticsRes.data?.projection?.[0] as unknown as { projectedSpend?: number })?.projectedSpend ??
        0;
      variableProyectado.value = Number(projected) || 0;
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
