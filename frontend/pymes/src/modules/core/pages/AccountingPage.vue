<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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
const loading = ref(true)
const periodo = ref(new Date().toISOString().slice(0, 7))

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
      value: formatCurrency(m.totalIncome),
      delta: p ? deltaPct(m.totalIncome, p.totalIncome) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'gold' as const,
    },
    {
      label: 'Costo de Mercadería',
      value: formatCurrency(m.costOfGoods),
      delta: p ? deltaPct(m.costOfGoods, p.costOfGoods) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'red' as const,
    },
    {
      label: 'Gastos Operativos',
      value: formatCurrency(m.operatingExpenses),
      delta: p ? deltaPct(m.operatingExpenses, p.operatingExpenses) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'blue' as const,
    },
    {
      label: 'Margen Bruto',
      value: formatPercent(m.grossMarginPct),
      delta: p ? deltaPct(m.grossMarginPct, p.grossMarginPct) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'green' as const,
    },
    {
      label: 'Margen Operativo',
      value: formatPercent(m.operatingMarginPct),
      delta: p ? deltaPct(m.operatingMarginPct, p.operatingMarginPct) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: 'green' as const,
    },
    {
      label: 'Margen Neto',
      value: formatPercent(m.netMarginPct),
      delta: p ? deltaPct(m.netMarginPct, p.netMarginPct) : undefined,
      deltaLabel: 'vs mes anterior',
      accent: m.netMarginPct >= 0 ? 'green' as const : 'red' as const,
    },
  ]
})

async function load() {
  if (!tenantId) return
  loading.value = true
  try {
    const prev = getPreviousPeriod(periodo.value)
    const [res, prevRes] = await Promise.all([
      accountingService.consultar(tenantId, periodo.value),
      accountingService.consultar(tenantId, prev),
    ])
    data.value = res.data
    dataPrev.value = prevRes.data
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar métricas' })
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

    <div class="kpi-grid stagger-children">
      <KpiCard
        v-for="kpi in kpis"
        :key="kpi.label"
        :label="kpi.label"
        :value="kpi.value"
        :delta="kpi.delta"
        :delta-label="kpi.deltaLabel"
        :accent="kpi.accent"
        :loading="loading"
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
          <span class="summary-card__item-value">{{ formatCurrency(data.totalIncome) }}</span>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__item-label">Costo de Ventas</span>
          <span class="summary-card__item-value">-{{ formatCurrency(data.costOfGoods) }}</span>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__item-label">Gastos Operativos</span>
          <span class="summary-card__item-value">-{{ formatCurrency(data.operatingExpenses) }}</span>
        </div>
        <div class="summary-card__item">
          <span class="summary-card__item-label">Pagos Préstamos</span>
          <span class="summary-card__item-value">-{{ formatCurrency(data.loanPayments) }}</span>
        </div>
        <div class="summary-card__divider" />
        <div class="summary-card__item summary-card__item--result">
          <span class="summary-card__item-label">Resultado Neto</span>
          <span
            class="summary-card__item-value"
            :class="data.netMargin >= 0 ? 'summary-card__item-value--positive' : 'summary-card__item-value--negative'"
          >
            {{ formatCurrency(data.netMargin) }}
          </span>
        </div>
      </div>
    </div>
  </q-page>
</template>

<style scoped lang="scss">
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
