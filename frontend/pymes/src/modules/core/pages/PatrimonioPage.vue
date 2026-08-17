<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { formatCurrency } from 'src/utils/format';
import { patrimonioService } from '../services/patrimonio.service';
import { prestamoService } from '../services/prestamo.service';
import { accountingService } from '../services/accounting.service';
import { usePeriod } from '../composables/usePeriod';
import type { Patrimonio, Prestamo, MetricasFinancieras } from '../types';

useMeta({ title: 'Inversión — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;
const { period } = usePeriod();

const data = ref<Patrimonio | null>(null);
const loading = ref(true);
const saving = ref(false);
const prestamos = ref<Prestamo[]>([]);
const metricas = ref<MetricasFinancieras | null>(null);

async function load() {
  if (!tenantId) return;
  loading.value = true;
  try {
    const [patRes, prestRes, metRes] = await Promise.all([
      patrimonioService.get(tenantId),
      prestamoService.getAll(tenantId),
      accountingService.consultar(tenantId, period.value),
    ]);
    data.value = patRes.data;
    prestamos.value = prestRes.data;
    metricas.value = metRes.data;
    form.value = {
      capitalInicial: patRes.data.capitalInicial,
      fechaInicio: patRes.data.fechaInicio,
    };
    capitalStr.value = rawCapital(patRes.data.capitalInicial);
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar patrimonio',
    });
  } finally {
    loading.value = false;
  }
}

const form = ref({ capitalInicial: 0, fechaInicio: null as string | null });
const editing = ref(false);

const capitalStr = ref('');
function onCapitalInput(val: string | number | null) {
  capitalStr.value = String(val ?? '')
    .replace(/[^0-9.]/g, '')
    .replace(/(\..*)\./g, '$1');
}
function formatCapitalInput() {
  const n = parseFloat(capitalStr.value);
  if (!isNaN(n) && capitalStr.value) {
    capitalStr.value = n.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    form.value.capitalInicial = n;
  }
}
function rawCapital(val: number) {
  return val
    ? val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '';
}

function toggleEdit() {
  if (!editing.value) {
    editing.value = true;
  } else {
    void save();
  }
}

const plataARecuperar = computed(() => {
  const capital = data.value?.capitalInicial ?? 0;
  const deudaActiva = prestamos.value
    .filter((p) => p.estado === 'ACTIVO')
    .reduce((s, p) => s + p.saldoPendiente, 0);
  return capital + deudaActiva;
});

const gananciaMensual = computed(() => {
  const m = metricas.value;
  return m ? (m.totalIngresos * m.margenNetoPct) / 100 : 0;
});

const mesesRecuperacion = computed(() => {
  if (gananciaMensual.value <= 0) return null;
  return Math.ceil(plataARecuperar.value / gananciaMensual.value);
});

async function save() {
  if (!tenantId) return;
  if (form.value.capitalInicial <= 0) {
    $q.notify({ type: 'warning', message: 'El capital inicial debe ser mayor a 0' });
    return;
  }
  saving.value = true;
  try {
    await patrimonioService.update(tenantId, {
      tenantId,
      capitalInicial: form.value.capitalInicial,
      fechaInicio: form.value.fechaInicio || null,
    });
    editing.value = false;
    $q.notify({ type: 'positive', message: 'Patrimonio actualizado' });
    await load();
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al guardar patrimonio',
    });
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  if (tenantId) void load();
});
</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-lg fade-in-up">
      <h1 class="text-h4 font-bold q-ma-none patrimonio-title">Inversión</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Capital inicial, inversión y ROI</p>
    </div>

    <div v-if="loading" class="row q-col-gutter-md">
      <div v-for="i in 3" :key="i" class="col-12 col-sm-4">
        <div class="metric-skeleton skeleton" />
      </div>
    </div>

    <div v-else-if="!data" class="text-center q-py-xl fade-in-up">
      <div class="empty-state glass">
        <q-icon name="savings" size="56px" class="text-accent" />
        <p class="text-accent q-mt-md text-body1">Registra tu capital inicial para comenzar</p>
      </div>
    </div>

    <template v-else>
      <div class="row q-col-gutter-md q-mb-lg stagger-children">
        <div class="col-12 col-sm-4">
          <div class="metric-card metric-card--copper">
            <div class="metric-card__label">Capital Inicial</div>
            <div class="metric-card__value">{{ formatCurrency(data.capitalInicial) }}</div>
            <div class="metric-card__bar" />
          </div>
        </div>
        <div class="col-12 col-sm-4">
          <div class="metric-card metric-card--sage">
            <div class="metric-card__label">Fecha de Inicio</div>
            <div class="metric-card__value metric-card__value--date">
              {{ data.fechaInicio || '—' }}
            </div>
            <div class="metric-card__bar" />
          </div>
        </div>
        <div class="col-12 col-sm-4">
          <div class="metric-card metric-card--positive">
            <div class="metric-card__label">Estado</div>
            <div class="metric-card__value">
              <span class="status-badge">Activo</span>
            </div>
            <div class="metric-card__bar" />
          </div>
        </div>
        <div class="col-12 col-sm-4">
          <div class="metric-card metric-card--copper">
            <div class="metric-card__label">Tiempo de recuperación</div>
            <div class="metric-card__value metric-card__value--date">
              {{ mesesRecuperacion === null ? '—' : `~${mesesRecuperacion} meses` }}
            </div>
            <div class="metric-card__bar" />
          </div>
        </div>
      </div>

      <div class="fade-in-up" style="animation-delay: 0.25s">
        <div class="config-card glass">
          <div class="config-card__header">
            <div class="config-card__title">
              <q-icon name="tune" size="1.1rem" class="text-primary" />
              <span class="text-h6 text-primary">Configuración</span>
            </div>
            <q-btn
              :icon="editing ? 'save' : 'edit'"
              :color="editing ? 'positive' : 'primary'"
              :label="editing ? 'Guardar' : 'Editar'"
              :loading="saving"
              @click="toggleEdit"
              class="config-card__action"
              dense
            />
          </div>
          <div class="config-card__body">
            <div class="row q-col-gutter-md">
              <div class="col-12 col-sm-6">
                <q-input
                  dark
                  filled
                  :disable="!editing"
                  :model-value="capitalStr"
                  @update:model-value="onCapitalInput"
                  @blur="formatCapitalInput"
                  label="Capital Inicial"
                  type="text"
                  inputmode="decimal"
                  prefix="$"
                />
              </div>
              <div class="col-12 col-sm-6">
                <q-input
                  dark
                  filled
                  :disable="!editing"
                  v-model="form.fechaInicio"
                  label="Fecha de Inicio"
                  type="date"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </q-page>
</template>

<style scoped lang="scss">
.patrimonio-title {
  font-family: 'Outfit', sans-serif;
  background: linear-gradient(135deg, #a3785e 0%, #c5a059 50%, #a3785e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.metric-skeleton {
  height: 110px;
  border-radius: 8px;
}

.empty-state {
  border-radius: 12px;
  padding: 3rem 2rem;
  max-width: 360px;
  margin: 0 auto;
}

.metric-card {
  background: rgba(27, 38, 36, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(113, 131, 127, 0.05);
  border-radius: 8px;
  padding: 1.25rem 1.5rem;
  position: relative;
  overflow: hidden;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    border-radius: 0 2px 2px 0;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }

  &--copper::before {
    background: #a3785e;
  }
  &--sage::before {
    background: #8a9e99;
  }
  &--positive::before {
    background: #2d5a27;
  }

  &__label {
    font-size: 0.72rem;
    font-weight: 500;
    color: #8a9e99;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    margin-bottom: 0.5rem;
  }

  &__value {
    font-family: 'Outfit', sans-serif;
    font-size: 1.6rem;
    font-weight: 700;
    color: #e2e8e4;
    line-height: 1;

    &--date {
      font-size: 1.2rem;
      font-weight: 600;
      letter-spacing: 0.02em;
    }
  }

  &__bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, rgba(163, 120, 94, 0.15), transparent);
  }
}

.status-badge {
  display: inline-block;
  font-size: 0.8rem;
  font-weight: 600;
  color: #2d5a27;
  background: rgba(45, 90, 39, 0.12);
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  letter-spacing: 0.03em;
}

.config-card {
  border-radius: 10px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1.25rem 1.5rem;
    border-bottom: 1px solid rgba(113, 131, 127, 0.08);
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  &__body {
    padding: 1.5rem;
  }
}
</style>
