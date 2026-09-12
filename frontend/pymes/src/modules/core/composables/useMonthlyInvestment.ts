import { ref, shallowRef, computed, watch, readonly } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';
import { usePeriod } from './usePeriod';
import { facturaService } from '../services/factura.service';
import { costoService } from '../services/costo.service';
import type { Factura } from '../types';

// ponytail: solo PAGADO mes calendario — insumos+variable pagado real, sin fantasma estimado. Fase 2 proyección separada.
export function useMonthlyInvestment() {
  const authStore = useAuthStore();
  const { period } = usePeriod();

  const facturas = ref<Factura[]>([]);
  const costoOperativoMensual = shallowRef(0);
  const loading = shallowRef(false);
  const error = shallowRef<string | null>(null);

  // Solo facturas pagadas del mes seleccionado (yyyy-MM prefix, mismo formato que useFinancialDashboard)
  const facturasDelPeriodo = computed(() =>
    facturas.value.filter(
      (f) => f.status === 'PAGADA' && f.issueDate.startsWith(period.value),
    ),
  );

  // ponytail: GASTO_OPERATIVO = gasto variable pagado, resto = insumos pagado. Sin fantasma.
  const insumosMensual = computed(() =>
    facturasDelPeriodo.value
      .filter((f) => f.type !== 'GASTO_OPERATIVO')
      .reduce((s, f) => s + (f.total ?? 0), 0),
  );

  const gastoVariableMensual = computed(() =>
    facturasDelPeriodo.value
      .filter((f) => f.type === 'GASTO_OPERATIVO')
      .reduce((s, f) => s + (f.total ?? 0), 0),
  );

  // estimado informativo para fase 2, no suma en card
  const runningMensual = computed(() => costoOperativoMensual.value);

  const monthlyInvestment = computed(
    () => insumosMensual.value + gastoVariableMensual.value,
  );

  const breakdown = computed(() => ({
    insumos: insumosMensual.value,
    variable: gastoVariableMensual.value,
    fijo: 0,
    total: monthlyInvestment.value,
  }));

  async function fetch(signal?: AbortSignal) {
    const tenantId = authStore.user?.tenantId;
    if (!tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const [facturasRes, costoRes] = await Promise.all([
        facturaService.getAll(tenantId),
        costoService.getDiario(tenantId),
      ]);
      if (signal?.aborted) return;
      facturas.value = facturasRes.data ?? [];
      costoOperativoMensual.value = costoRes.data?.costoOperativoMensual ?? 0;
    } catch (e: unknown) {
      if (signal?.aborted) return;
      error.value = e instanceof Error ? e.message : 'Error cargando inversión mensual';
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
    facturas: readonly(facturas),
    insumosMensual: readonly(insumosMensual),
    gastoVariableMensual: readonly(gastoVariableMensual),
    runningMensual: readonly(runningMensual),
    monthlyInvestment: readonly(monthlyInvestment),
    breakdown: readonly(breakdown),
    loading: readonly(loading),
    error: readonly(error),
    periodo: readonly(period),
    fetch,
  };
}
