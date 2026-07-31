<script setup lang="ts">
import { ref, computed, shallowRef, onMounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat'
import { accountingService } from '../services/accounting.service'
import AnalyticsHeader from 'src/modules/core/components/analytics/AnalyticsHeader.vue'
import KpiCard from 'src/modules/core/components/analytics/KpiCard.vue'
import type { MetricasFinancieras } from '../types'

useMeta({ title: 'Contabilidad — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId
const { formatCurrency, formatPercent } = useNumberFormat()

const data = ref<MetricasFinancieras | null>(null)
const dataPrev = ref<MetricasFinancieras | null>(null)
const loading = shallowRef(true)
const error = shallowRef<string | null>(null)
const periodo = ref(new Date().toISOString().slice(0, 7))

const isLoading = computed(() => loading.value && !data.value)
const hasError = computed(() => error.value !== null && !data.value)
const isEmpty = computed(() => !loading.value && !error.value && kpis.value.length === 0)

function getPreviousPeriod(period: string): string {
  const parts = period.split('-').map(Number);
  const y = parts[0] ?? new Date().getFullYear();
  const m = parts[1] ?? 1;
  const d = new Date(y, m - 2, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function deltaPct(current: number, previous: number): number | undefined {
  if (!previous || previous === 0) return undefined
  return +((current - previous) / Math.abs(previous) * 100).toFixed(1)
}

const kpis = computed(() => {
  const m = data.value
  const p = dataPrev.value
  if (!m) return []
  return [
    {
      label: 'Ingresos Totales',
      value: formatCurrency(m.totalIngresos),
      delta: p ? deltaPct(m.totalIngresos, p.totalIngresos) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'gold' as const,
    },
    {
      label: 'Costo de Mercadería',
      value: formatCurrency(m.costoMercaderia),
      delta: p ? deltaPct(m.costoMercaderia, p.costoMercaderia) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'red' as const,
    },
    {
      label: 'Gastos Operativos',
      value: formatCurrency(m.gastosOperativos),
      delta: p ? deltaPct(m.gastosOperativos, p.gastosOperativos) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'blue' as const,
    },
    {
      label: 'Margen Bruto',
      value: formatPercent(m.margenBrutoPct),
      delta: p ? deltaPct(m.margenBrutoPct, p.margenBrutoPct) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'green' as const,
    },
    {
      label: 'Margen Operativo',
      value: formatPercent(m.margenOperativoPct),
      delta: p ? deltaPct(m.margenOperativoPct, p.margenOperativoPct) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'green' as const,
    },
    {
      label: 'Margen Neto',
      value: formatPercent(m.margenNetoPct),
      delta: p ? deltaPct(m.margenNetoPct, p.margenNetoPct) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: m.margenNetoPct >= 0 ? 'green' as const : 'red' as const,
    },
  ]
})

async function load() {
  if (!tenantId) return
  loading.value = true
  error.value = null
  try {
    const prev = getPreviousPeriod(periodo.value)
    const [res, prevRes] = await Promise.all([
      accountingService.consultar(tenantId, periodo.value),
      accountingService.consultar(tenantId, prev),
    ])
    data.value = res.data
    dataPrev.value = prevRes.data
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Error al cargar métricas'
    $q.notify({ type: 'negative', message: error.value })
  } finally { loading.value = false }
}

async function recalcular() {
  if (!tenantId) return
  loading.value = true
  try {
    const res = await accountingService.recalcular(tenantId, periodo.value)
    data.value = res.data
    $q.notify({ type: 'positive', message: 'Métricas recalculadas' })
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al recalcular' })
  } finally { loading.value = false }
}

onMounted(() => { if (tenantId) void load() })
</script>

<template>
  <q-page class="core-page">
    <AnalyticsHeader
      title="Contabilidad"
      subtitle="Rendimiento financiero consolidado"
      :period="periodo"
      :loading="loading"
      @update:period="(v) => { periodo = v; load() }"
      @recalculate="recalcular"
    />

    <div v-if="hasError" class="error-banner">
      <q-icon name="error_outline" size="18px" />
      <span>{{ error }}</span>
      <q-btn flat dense no-caps label="Reintentar" class="error-banner__retry" @click="load" />
    </div>

    <div v-if="isLoading" class="kpi-grid">
      <div v-for="i in 6" :key="i" class="skeleton skeleton--kpi" />
    </div>

    <div v-else-if="isEmpty" class="empty-state">
      <q-icon name="balance" size="48px" style="color: var(--pq-text-subtle)" />
      <p class="empty-state__title">No hay datos financieros para este período</p>
      <q-btn flat color="accent" label="Recalcular" @click="recalcular" />
    </div>

    <template v-else>
      <div class="kpi-grid stagger-children">
        <KpiCard
          v-for="kpi in kpis"
          :key="kpi.label"
          :label="kpi.label"
          :value="kpi.value"
          :delta="kpi.delta"
          :delta-label="kpi.deltaLabel"
          :accent="kpi.accent"
          :loading="false"
        />
      </div>

      <div v-if="data" class="summary-card fade-in-up">
        <div class="summary-card__header">
        <q-icon name="receipt_long" size="1.2rem" style="color: var(--pq-accent)" />
        <span class="summary-card__title">Resumen Consolidado</span>
      </div>
      <div class="summary-card__grid">
        <div class="summary-card__item">
          <span class="summary-card__item-label">Ingresos</span>
          <span class="summary-card__item-value">{{ formatCurrency(data.totalIngresos) }}</span>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__item-label">Costo de Ventas</span>
          <span class="summary-card__item-value">-{{ formatCurrency(data.costoMercaderia) }}</span>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__item-label">Gastos Operativos</span>
          <span class="summary-card__item-value">-{{ formatCurrency(data.gastosOperativos) }}</span>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__item-label">Pagos Préstamos</span>
          <span class="summary-card__item-value">-{{ formatCurrency(data.pagosPrestamos) }}</span>
        </div>
          <div class="summary-card__divider" />
        <div class="summary-card__item summary-card__item--result">
          <span class="summary-card__item-label">Resultado Neto</span>
          <span
            class="summary-card__item-value"
            :class="data.margenNeto >= 0 ? 'summary-card__item-value--positive' : 'summary-card__item-value--negative'"
          >
            {{ formatCurrency(data.margenNeto) }}
          </span>
        </div>
      </div>
    </div>
    </template>
  </q-page>
</template>

<style scoped lang="scss">
.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 24px;
  background: rgba(160, 64, 56, 0.1);
  border: 1px solid rgba(160, 64, 56, 0.2);
  border-radius: 6px;
  font-family: 'Satoshi', sans-serif;
  font-size: 13px;
  color: var(--pq-danger);

  &__retry {
    margin-left: auto;
    color: var(--pq-danger);
    font-weight: 500;
  }
}

.skeleton--kpi {
  height: 100px;
  border-radius: var(--pq-radius-md);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 24px;
  text-align: center;

  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    color: var(--pq-text-muted);
    margin: 0;
  }
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 639px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.summary-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 6px;
  padding: 20px;

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--pq-border);
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 14px;
    font-weight: 600;
    color: var(--pq-text);
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 12px;
  }

  &__item {
    text-align: center;
    padding: 10px;
    border-radius: 6px;
    background: rgba(0, 0, 0, 0.12);
    transition: background var(--pq-motion-fast);

    &:hover {
      background: rgba(0, 0, 0, 0.2);
    }

    &--result {
      background: rgba(200, 150, 62, 0.08);
    }
  }

  &__item-label {
    display: block;
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.04em;
    margin-bottom: 4px;
  }

  &__item-value {
    font-family: 'Geist Mono', monospace;
    font-size: 14px;
    font-weight: 500;
    color: var(--pq-text);
    font-variant-numeric: tabular-nums;

    &--positive {
      color: var(--pq-success);
    }

    &--negative {
      color: var(--pq-danger);
    }
  }

  &__divider {
    grid-column: 1 / -1;
    height: 1px;
    background: var(--pq-border);
    margin: 4px 0;
  }
}
</style>
