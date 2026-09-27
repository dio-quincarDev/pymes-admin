import { computed, ref, shallowRef, watch } from 'vue';
import { ventaService } from '../services/venta.service';
import type { VentaDiaria, VentaRequest } from '../types';

export interface DayGroup {
  date: string;
  label: string;
  items: VentaDiaria[];
  total: number;
}

const STORAGE_KEY = 'pymeq_ventas_month';

function currentMonthKey() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

function shiftMonth(key: string, delta: number) {
  const parts = key.split('-').map(Number);
  const y = parts[0] ?? 0;
  const m = parts[1] ?? 1;
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function dayLabel(date: string) {
  return new Date(date + 'T00:00:00').toLocaleDateString('es-PA', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  });
}

export function useVentas(tenantId: string | undefined) {
  const rows = ref<VentaDiaria[]>([]);
  const loading = shallowRef(false);
  const error = shallowRef<string | null>(null);
  const monthKey = ref(localStorage.getItem(STORAGE_KEY) || currentMonthKey());

  watch(monthKey, (val) => localStorage.setItem(STORAGE_KEY, val));

  const monthLabel = computed(() => {
    const parts = monthKey.value.split('-').map(Number);
    const y = parts[0] ?? 0;
    const m = parts[1] ?? 1;
    const s = new Date(y, m - 1, 1).toLocaleDateString('es-PA', {
      month: 'long',
      year: 'numeric',
    });
    return s.charAt(0).toUpperCase() + s.slice(1);
  });

  const monthRows = computed(() => rows.value.filter((r) => r.fecha.startsWith(monthKey.value)));

  const dayGroups = computed<DayGroup[]>(() => {
    const groups = new Map<string, VentaDiaria[]>();
    for (const v of monthRows.value) {
      if (!groups.has(v.fecha)) groups.set(v.fecha, []);
      groups.get(v.fecha)!.push(v);
    }
    return [...groups.entries()]
      .map(([date, items]) => ({
        date,
        label: dayLabel(date),
        items,
        total: items.reduce((s, v) => s + v.montoBruto, 0),
      }))
      .sort((a, b) => b.date.localeCompare(a.date));
  });

  const totalMes = computed(() => monthRows.value.reduce((s, r) => s + r.montoBruto, 0));
  const registroCount = computed(() => monthRows.value.length);
  const ticketPromedio = computed(() => (registroCount.value ? totalMes.value / registroCount.value : 0));

  function prevMonth() {
    monthKey.value = shiftMonth(monthKey.value, -1);
  }

  function nextMonth() {
    monthKey.value = shiftMonth(monthKey.value, 1);
  }

  function goToday() {
    monthKey.value = currentMonthKey();
  }

  async function load() {
    if (!tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const res = await ventaService.getAll(tenantId);
      rows.value = res.data;
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al cargar ventas';
    } finally {
      loading.value = false;
    }
  }

  async function create(data: VentaRequest) {
    const res = await ventaService.create(data);
    rows.value.unshift(res.data);
  }

  async function update(id: string, data: VentaRequest) {
    const res = await ventaService.update(id, data);
    const idx = rows.value.findIndex((r) => r.id === id);
    if (idx >= 0) rows.value[idx] = res.data;
  }

  async function remove(id: string, tid: string) {
    await ventaService.remove(id, tid);
    rows.value = rows.value.filter((r) => r.id !== id);
  }

  return {
    rows,
    loading,
    error,
    monthKey,
    monthLabel,
    dayGroups,
    totalMes,
    registroCount,
    ticketPromedio,
    prevMonth,
    nextMonth,
    goToday,
    load,
    create,
    update,
    remove,
  };
}
