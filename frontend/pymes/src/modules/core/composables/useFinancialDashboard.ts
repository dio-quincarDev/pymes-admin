import { ref, computed, watch } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';
import { usePeriod } from './usePeriod';
import { accountingService } from '../services/accounting.service';
import { gastoService } from '../services/gasto.service';
import { ventaService } from '../services/venta.service';
import { facturaService } from '../services/factura.service';
import type { MetricasFinancieras, GastoOperativo, VentaDiaria, Factura } from '../types';

export interface GastoPorCategoria {
  category: string;
  total: number;
  pct: number;
}

export interface ActividadItem {
  type: 'gasto' | 'venta';
  description: string;
  amount: number;
  date: string;
}

function getPreviousPeriod(period: string): string {
  const parts = period.split('-').map(Number);
  const y = parts[0] ?? new Date().getFullYear();
  const m = parts[1] ?? 1;
  const d = new Date(y, m - 2, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export function useFinancialDashboard() {
  const authStore = useAuthStore();
  const { period, setPeriod } = usePeriod();

  const metricas = ref<MetricasFinancieras | null>(null);
  const metricasPrev = ref<MetricasFinancieras | null>(null);
  const gastos = ref<GastoOperativo[]>([]);
  const gastosPrev = ref<GastoOperativo[]>([]);
  const ventas = ref<VentaDiaria[]>([]);
  const facturas = ref<Factura[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const gastosPorCategoria = computed<GastoPorCategoria[]>(() => {
    if (!gastos.value.length) return [];
    const totals = new Map<string, number>();
    let grandTotal = 0;
    for (const g of gastos.value) {
      totals.set(g.category, (totals.get(g.category) ?? 0) + g.amount);
      grandTotal += g.amount;
    }
    if (grandTotal === 0) return [];
    return Array.from(totals.entries())
      .map(([category, total]) => ({ category, total, pct: (total / grandTotal) * 100 }))
      .sort((a, b) => b.total - a.total);
  });

  const gastosPorCategoriaPrev = computed<GastoPorCategoria[]>(() => {
    if (!gastosPrev.value.length) return [];
    const totals = new Map<string, number>();
    let grandTotal = 0;
    for (const g of gastosPrev.value) {
      totals.set(g.category, (totals.get(g.category) ?? 0) + g.amount);
      grandTotal += g.amount;
    }
    if (grandTotal === 0) return [];
    return Array.from(totals.entries())
      .map(([category, total]) => ({ category, total, pct: (total / grandTotal) * 100 }))
      .sort((a, b) => b.total - a.total);
  });

  const actividadReciente = computed<ActividadItem[]>(() => {
    const gastosItems: ActividadItem[] = gastos.value.map((g) => ({
      type: 'gasto' as const,
      description: g.description || g.category,
      amount: g.amount,
      date: g.expenseDate,
    }));
    const ventasItems: ActividadItem[] = ventas.value.map((v) => ({
      type: 'venta' as const,
      description: v.description || 'Venta del día',
      amount: v.grossAmount,
      date: v.saleDate,
    }));
    return [...gastosItems, ...ventasItems]
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
      .slice(0, 10);
  });

  const facturasPendientes = computed(() =>
    facturas.value.filter((f) => f.status !== 'PAGADA'),
  );

  async function fetch() {
    const tenantId = authStore.user?.tenantId;
    if (!tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const prev = getPreviousPeriod(period.value);
      const [metricasRes, metricasPrevRes, gastosRes, gastosPrevRes, ventasRes, facturasRes] = await Promise.all([
        accountingService.consultar(tenantId, period.value),
        accountingService.consultar(tenantId, prev),
        gastoService.getAll(tenantId),
        gastoService.getAll(tenantId),
        ventaService.getAll(tenantId),
        facturaService.getAll(tenantId),
      ]);
      metricas.value = metricasRes.data;
      metricasPrev.value = metricasPrevRes.data;
      gastos.value = gastosRes.data;
      gastosPrev.value = gastosPrevRes.data;
      ventas.value = ventasRes.data;
      facturas.value = facturasRes.data;
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Error cargando datos financieros';
    } finally {
      loading.value = false;
    }
  }

  async function recalcular() {
    const tenantId = authStore.user?.tenantId;
    if (!tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const metricasRes = await accountingService.recalcular(tenantId, period.value);
      metricas.value = metricasRes.data;
      await fetch();
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Error recalculando';
    } finally {
      loading.value = false;
    }
  }

  watch(period, fetch, { immediate: true });

  return {
    metricas,
    metricasPrev,
    gastos,
    gastosPrev,
    ventas,
    facturas,
    gastosPorCategoria,
    gastosPorCategoriaPrev,
    actividadReciente,
    facturasPendientes,
    loading,
    error,
    periodo: period,
    setPeriod,
    fetch,
    recalcular,
  };
}
