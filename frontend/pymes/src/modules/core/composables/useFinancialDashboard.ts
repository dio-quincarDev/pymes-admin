import { ref, computed, watch } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';
import { usePeriod } from './usePeriod';
import { accountingService } from '../services/accounting.service';
import { ventaService } from '../services/venta.service';
import { facturaService } from '../services/factura.service';
import { costoService } from '../services/costo.service';
import type { MetricasFinancieras, VentaDiaria, Factura, CostoDiario } from '../types';

export interface GastoPorCategoria {
  categoria: string;
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
  const ventas = ref<VentaDiaria[]>([]);
  const facturas = ref<Factura[]>([]);
  const costoDiario = ref<CostoDiario | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const gastosPorCategoria = computed<GastoPorCategoria[]>(() => {
    const gastosFacturas = facturas.value.filter(
      (f) => f.type === 'GASTO_OPERATIVO' && f.status === 'PAGADA',
    );
    if (!gastosFacturas.length) return [];
    const totals = new Map<string, number>();
    let grandTotal = 0;
    for (const f of gastosFacturas) {
      const cat = f.category || 'Sin categoría';
      totals.set(cat, (totals.get(cat) ?? 0) + f.total);
      grandTotal += f.total;
    }
    if (grandTotal === 0) return [];
    return Array.from(totals.entries())
      .map(([categoria, total]) => ({ categoria, total, pct: (total / grandTotal) * 100 }))
      .sort((a, b) => b.total - a.total);
  });

  // ponytail: comparación por período no estaba filtrada antes, se mantiene como empty
  const gastosPorCategoriaPrev = computed<GastoPorCategoria[]>(() => []);

  const actividadReciente = computed<ActividadItem[]>(() => {
    const gastosItems: ActividadItem[] = facturas.value
      .filter((f) => f.type === 'GASTO_OPERATIVO' && f.status === 'PAGADA')
      .map((f) => ({
        type: 'gasto' as const,
        description: f.category || 'Gasto',
        amount: f.total,
        date: f.issueDate,
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
      const [metricasRes, metricasPrevRes, ventasRes, facturasRes, costoRes] = await Promise.all([
        accountingService.consultar(tenantId, period.value),
        accountingService.consultar(tenantId, prev),
        ventaService.getAll(tenantId),
        facturaService.getAll(tenantId),
        costoService.getDiario(tenantId),
      ]);
      metricas.value = metricasRes.data;
      metricasPrev.value = metricasPrevRes.data;
      ventas.value = ventasRes.data;
      facturas.value = facturasRes.data;
      costoDiario.value = costoRes.data;
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
    ventas,
    facturas,
    costoDiario,
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
