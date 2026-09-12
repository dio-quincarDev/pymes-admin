import { ref, shallowRef, computed, readonly } from 'vue';
import { useQuasar } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { costoService } from '../services/costo.service';
import { proveedorService } from '../services/proveedor.service';
import { prestamoService } from '../services/prestamo.service';
import { cuotaMensual } from '../utils/prestamo';
import type { Collaborador, ConfigLaboral, CostoDiario, GastoFijoRecurrente, Prestamo, Proveedor } from '../types';

// ponytail: single source of truth for CostosPage — readonly state + explicit actions per composables.md
export function useCostos() {
  const $q = useQuasar();
  const authStore = useAuthStore();
  const tenantId = computed(() => authStore.user?.tenantId ?? '');

  const colaboradores = ref<Collaborador[]>([]);
  const gastosFijos = ref<GastoFijoRecurrente[]>([]);
  const proveedores = ref<Proveedor[]>([]);
  const prestamos = ref<Prestamo[]>([]);
  const diario = ref<CostoDiario | null>(null);
  const configuracion = ref<ConfigLaboral | null>(null);
  const loading = shallowRef(false);
  const configSaving = shallowRef(false);
  const configForm = ref<ConfigLaboral>({ tenantId: '', diasLaborales: 26 });

  const proveedorOptions = computed(() => proveedores.value.map((p) => ({ label: p.name, value: p.id })));

  const dias = computed(() => diario.value?.diasLaborales || 1);
  const prestamosActivos = computed(() => prestamos.value.filter((p) => p.estado === 'ACTIVO'));
  const cuotaPrestamosMensual = computed(() => prestamosActivos.value.reduce((s, p) => s + cuotaMensual(p.monto, p.tasaInteres, p.plazoMeses), 0));
  const costoFijoDiario = computed(() => (diario.value?.costoFijoMensual ?? 0) / dias.value);
  const costoSalariosDiario = computed(() => (diario.value?.costoSalariosMensual ?? 0) / dias.value);
  const costoPrestamosDiario = computed(() => cuotaPrestamosMensual.value / dias.value);
  const costoOperativoDiario = computed(() => costoFijoDiario.value + costoSalariosDiario.value + costoPrestamosDiario.value);
  const gananciaPositiva = computed(() => (diario.value?.gananciaRealEstimada ?? 0) >= 0);

  interface CatGroup { categoria: string; items: GastoFijoRecurrente[]; total: number; }
  const catGroups = computed<CatGroup[]>(() => {
    const groups = new Map<string, GastoFijoRecurrente[]>();
    for (const g of gastosFijos.value) {
      const cat = g.categoria || 'OTROS';
      if (!groups.has(cat)) groups.set(cat, []);
      groups.get(cat)!.push(g);
    }
    const result: CatGroup[] = [];
    for (const [categoria, list] of groups) {
      result.push({ categoria, items: list, total: list.reduce((s, g) => s + g.monto, 0) });
    }
    result.sort((a, b) => b.total - a.total);
    return result;
  });

  async function loadDiario() {
    const tid = tenantId.value;
    if (!tid) return;
    try {
      const res = await costoService.getDiario(tid);
      diario.value = res.data;
    } catch (err) {
      $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al calcular costo del día' });
    }
  }

  async function loadAll() {
    const tid = tenantId.value;
    if (!tid) return;
    loading.value = true;
    try {
      const [colabRes, gastosRes, configRes, provRes, prestamosRes] = await Promise.all([
        costoService.getAllCollaboradores(tid),
        costoService.getAllGastosFijos(tid),
        costoService.getConfiguracion(tid),
        proveedorService.getAll(tid),
        prestamoService.getAll(tid),
      ]);
      colaboradores.value = colabRes.data;
      gastosFijos.value = gastosRes.data;
      proveedores.value = provRes.data;
      prestamos.value = prestamosRes.data;
      configuracion.value = configRes.data;
      configForm.value = { tenantId: tid, diasLaborales: configRes.data.diasLaborales };
    } catch (err) {
      $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar costos' });
    } finally { loading.value = false; }
    void loadDiario();
  }

  async function saveConfig() {
    const tid = tenantId.value;
    if (!tid) return;
    const diasVal = Number(configForm.value.diasLaborales);
    if (!Number.isInteger(diasVal) || diasVal < 1 || diasVal > 31) {
      $q.notify({ type: 'negative', message: 'Los días laborales deben estar entre 1 y 31' });
      return;
    }
    configSaving.value = true;
    try {
      const res = await costoService.updateConfiguracion(tid, { diasLaborales: diasVal });
      configuracion.value = res.data;
      configForm.value = res.data;
      $q.notify({ type: 'positive', message: 'Configuración actualizada' });
      void loadDiario();
    } catch (err) {
      $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al guardar configuración' });
    } finally { configSaving.value = false; }
  }

  async function createProveedor(name: string) {
    const tid = tenantId.value;
    if (!tid || !name.trim()) return null;
    const res = await proveedorService.create({ tenantId: tid, name: name.trim() });
    proveedores.value = [...proveedores.value, res.data];
    $q.notify({ type: 'positive', message: `Proveedor "${res.data.name}" creado` });
    return res.data;
  }

  function removeLocalColaborador(id: string) { colaboradores.value = colaboradores.value.filter((r) => r.id !== id); }
  function upsertLocalColaborador(data: Collaborador, editingId: string | null) {
    if (editingId) {
      const idx = colaboradores.value.findIndex((r) => r.id === editingId);
      if (idx >= 0) colaboradores.value[idx] = data;
    } else colaboradores.value.unshift(data);
  }
  function removeLocalGasto(id: string) { gastosFijos.value = gastosFijos.value.filter((r) => r.id !== id); }
  function upsertLocalGasto(data: GastoFijoRecurrente, editingId: string | null) {
    if (editingId) {
      const idx = gastosFijos.value.findIndex((r) => r.id === editingId);
      if (idx >= 0) gastosFijos.value[idx] = data;
    } else gastosFijos.value.unshift(data);
  }

  return {
    // state readonly
    colaboradores: readonly(colaboradores),
    gastosFijos: readonly(gastosFijos),
    proveedores: readonly(proveedores),
    prestamos: readonly(prestamos),
    diario: readonly(diario),
    configuracion: readonly(configuracion),
    loading: readonly(loading),
    configSaving: readonly(configSaving),
    configForm,
    proveedorOptions: readonly(proveedorOptions),
    dias: readonly(dias),
    costoFijoDiario: readonly(costoFijoDiario),
    costoSalariosDiario: readonly(costoSalariosDiario),
    costoPrestamosDiario: readonly(costoPrestamosDiario),
    costoOperativoDiario: readonly(costoOperativoDiario),
    gananciaPositiva: readonly(gananciaPositiva),
    catGroups: readonly(catGroups),
    tenantId: readonly(tenantId),
    // actions
    loadAll,
    loadDiario,
    saveConfig,
    createProveedor,
    removeLocalColaborador,
    upsertLocalColaborador,
    removeLocalGasto,
    upsertLocalGasto,
  };
}
