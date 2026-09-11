import { ref, computed, readonly, shallowRef } from 'vue';
import { useQuasar } from 'quasar';
import { toLocalISODate } from 'src/utils/format';
import { ventaService } from '../services/venta.service';
import type { VentaDiaria } from '../types';

function getMondayStr(d = new Date()): string {
  const local = new Date(d);
  const day = local.getDay(); // 0 dom, 1 lun
  const diff = day === 0 ? -6 : 1 - day;
  local.setDate(local.getDate() + diff);
  return toLocalISODate(local);
}

export function useVentasSemanales(tenantId: string | undefined) {
  const $q = useQuasar();
  const ventas = ref<VentaDiaria[]>([]);
  const loading = shallowRef(false);

  async function fetch() {
    if (!tenantId) return;
    loading.value = true;
    try {
      const res = await ventaService.getAll(tenantId);
      ventas.value = res.data;
    } catch (err) {
      $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar ventas' });
    } finally {
      loading.value = false;
    }
  }

  // ponytail: deriva directo con new Date() cada recompute — evita computed cacheado sin deps
  const ventasSemanales = computed(() => {
    const mondayStr = getMondayStr();
    return ventas.value
      .filter((v) => v.fecha >= mondayStr)
      .reduce((s, v) => s + v.montoBruto, 0);
  });

  const ventasSemanalesRango = computed(() => {
    const mon = getMondayStr();
    const sun = (() => {
      const d = new Date(mon + 'T00:00:00');
      d.setDate(d.getDate() + 6);
      return toLocalISODate(d);
    })();
    return `${mon} → ${sun}`;
  });

  return {
    ventas: readonly(ventas),
    loading: readonly(loading),
    ventasSemanales,
    ventasSemanalesRango,
    fetchVentas: fetch,
  };
}
