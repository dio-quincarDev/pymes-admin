import { computed, shallowRef } from 'vue';
import { useQuasar } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { toLocalISODate } from 'src/utils/format';
import { parseBackendError } from 'src/utils/errors';
import { ventaService } from '../services/venta.service';

// ponytail: single source for venta quick capture — used by inline bar and dialog (no duplication)
export function useRegistrarVenta() {
  const $q = useQuasar();
  const authStore = useAuthStore();

  const montoStr = shallowRef('');
  const descripcion = shallowRef('');
  const fecha = shallowRef(toLocalISODate(new Date()));
  const saving = shallowRef(false);
  const error = shallowRef('');

  const monto = computed<number | null>(() => {
    const n = parseFloat(montoStr.value.replace(',', '.'));
    return Number.isFinite(n) ? n : null;
  });

  const isValid = computed(() => monto.value !== null && monto.value > 0);

  function reset() {
    montoStr.value = '';
    descripcion.value = '';
    fecha.value = toLocalISODate(new Date());
    error.value = '';
  }

  async function save(): Promise<boolean> {
    const tenantId = authStore.user?.tenantId;
    if (!tenantId) {
      $q.notify({ type: 'warning', message: 'Sesión sin empresa — reingresá' });
      return false;
    }
    if (!isValid.value) return false;

    saving.value = true;
    error.value = '';
    try {
      await ventaService.create({
        tenantId,
        montoBruto: monto.value!,
        fecha: fecha.value,
        descripcion: descripcion.value || null,
      });
      $q.notify({ type: 'positive', message: `Venta registrada — B/. ${monto.value!.toFixed(2)}` });
      reset();
      return true;
    } catch (e: unknown) {
      const parsed = parseBackendError(e);
      error.value = parsed.message;
      $q.notify({ type: 'negative', message: parsed.message });
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    montoStr,
    descripcion,
    fecha,
    saving,
    error,
    monto,
    isValid,
    save,
    reset,
  };
}
