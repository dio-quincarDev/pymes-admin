<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat'
import { useAnalytics } from '../composables/useAnalytics'
import { productoService } from '../services/producto.service'
import { api } from 'src/boot/axios'
import AnalyticsHeader from 'src/modules/core/components/analytics/AnalyticsHeader.vue'
import MetricCard from 'src/modules/core/components/analytics/MetricCard.vue'
import CategoryBreakdownChart from 'src/modules/core/components/analytics/CategoryBreakdownChart.vue'
import DataTable from 'src/modules/core/components/analytics/DataTable.vue'
import SupplierComparisonTable from '../components/dashboard/SupplierComparisonTable.vue'
import SupplierRecommendationsCard from '../components/dashboard/SupplierRecommendationsCard.vue'
import PricePredictionsTable from '../components/dashboard/PricePredictionsTable.vue'
import type { Producto, SetupInfo, SetupCategory } from '../types'

useMeta({ title: 'Análisis de Gastos — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId
const { formatCurrency } = useNumberFormat()

const { period, setPeriod, recalcular: recalcularAnalytics, loading: analyticsLoading, supplierComparison, supplierRecommendations, pricePrediction } = useAnalytics()

const products = ref<Producto[]>([])
const setupCategories = ref<SetupCategory[]>([])
const loading = ref(false)

const totalInvestment = computed(() =>
  products.value.reduce((sum, p) => sum + (p.totalInvestment ?? 0), 0)
)

const productCount = computed(() => products.value.length)

const categoryNameMap = computed(() => {
  const map = new Map<string, string>()
  function walk(cats: SetupCategory[]) {
    for (const c of cats) {
      map.set(c.code, c.name)
      if (c.children?.length) walk(c.children)
    }
  }
  walk(setupCategories.value)
  return map
})

interface CategoryGroup {
  name: string
  total: number
  pct: number
  count: number
}

const byCategory = computed<CategoryGroup[]>(() => {
  const map = new Map<string, { total: number; count: number }>()
  let grandTotal = 0
  for (const p of products.value) {
    const cat = categoryNameMap.value.get(p.category) || p.category || 'Sin categoría'
    const inv = p.totalInvestment ?? 0
    const g = map.get(cat) ?? { total: 0, count: 0 }
    g.total += inv
    g.count++
    map.set(cat, g)
    grandTotal += inv
  }
  return Array.from(map.entries())
    .map(([name, g]) => ({ name, total: g.total, pct: grandTotal > 0 ? +(g.total / grandTotal * 100).toFixed(1) : 0, count: g.count }))
    .sort((a, b) => b.total - a.total)
})

const categoryChartItems = computed(() =>
  byCategory.value.map((c) => ({
    category: c.name,
    currentAmount: c.total,
    percentage: c.pct,
  }))
)

interface AlertInfo {
  productName: string
  type: 'OVER_MAX' | 'BELOW_MIN' | 'NO_PURCHASE'
  message: string
}

const alerts = computed<AlertInfo[]>(() => {
  const result: AlertInfo[] = []
  const now = new Date()
  for (const p of products.value) {
    if (p.maxQuantity != null && (p.totalInvestment ?? 0) > p.maxQuantity) {
      result.push({ productName: p.name, type: 'OVER_MAX', message: `Excedió presupuesto máximo de $${p.maxQuantity}` })
    }
    if (p.minQuantity != null && (p.totalInvestment ?? 0) < p.minQuantity && (p.totalInvestment ?? 0) > 0) {
      result.push({ productName: p.name, type: 'BELOW_MIN', message: `Debajo del mínimo esperado de $${p.minQuantity}` })
    }
    if (p.lastPurchaseDate && (p.totalInvestment ?? 0) > 0) {
      const days = Math.floor((now.getTime() - new Date(p.lastPurchaseDate).getTime()) / (1000 * 60 * 60 * 24))
      if (days > 60) {
        result.push({ productName: p.name, type: 'NO_PURCHASE', message: `Sin compras en los últimos ${days} días` })
      }
    }
  }
  return result
})

const tableColumns = [
  { name: 'name', label: 'Producto', field: 'name', align: 'left' as const, sortable: true },
  { name: 'lastUnitPrice', label: 'Últ. Precio Unit.', field: 'lastUnitPrice', align: 'right' as const, sortable: true },
  { name: 'lastPurchaseDate', label: 'Últ. Compra', field: (r: Record<string, unknown>) => r.lastPurchaseDate ? new Date(r.lastPurchaseDate as string).toLocaleDateString() : '—', align: 'center' as const, sortable: true },
  { name: 'totalInvestment', label: 'Inversión Total', field: 'totalInvestment', align: 'right' as const, sortable: true },
  { name: 'minQuantity', label: 'Min', field: 'minQuantity', align: 'right' as const, sortable: false },
  { name: 'maxQuantity', label: 'Max', field: 'maxQuantity', align: 'right' as const, sortable: false },
]

const filter = ref('')

async function load() {
  if (!tenantId) return
  loading.value = true
  try {
    const [prodRes, setupRes] = await Promise.all([
      productoService.getAll(tenantId),
      api.get<SetupInfo>(`/core/setup/${tenantId}`),
    ])
    products.value = prodRes.data
    setupCategories.value = setupRes.data.categories || []
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar datos' })
  } finally { loading.value = false }
}

onMounted(() => { if (tenantId) void load() })
</script>

<template>
  <q-page class="core-page">
    <AnalyticsHeader
      title="Análisis de Gastos"
      subtitle="¿Dónde va tu plata?"
      :period="period"
      :loading="analyticsLoading || loading"
      @update:period="setPeriod"
      @recalculate="recalcularAnalytics"
    />

    <div class="metric-row stagger-children">
      <MetricCard
        label="Inversión Total"
        :value="formatCurrency(totalInvestment)"
        accent="gold"
        :loading="loading"
      />
      <MetricCard
        label="Productos"
        :value="String(productCount)"
        accent="blue"
        :loading="loading"
      />
      <MetricCard
        label="Categorías"
        :value="String(byCategory.length)"
        accent="green"
        :loading="loading"
      />
    </div>

    <div class="analysis-grid">
      <div class="analysis-grid__main">
        <CategoryBreakdownChart
          :items="categoryChartItems"
          :loading="loading"
          :empty="categoryChartItems.length === 0"
        />

        <DataTable
          :rows="(products as unknown as Record<string, unknown>[])"
          :columns="tableColumns"
          row-key="id"
          :loading="loading"
          :filter="filter"
          title="Top 10 Inversión"
        />
      </div>

      <div class="analysis-grid__side">
        <div class="alerts-panel">
          <div class="alerts-panel__header">
            <div class="alerts-panel__accent" :class="{ 'alerts-panel__accent--danger': alerts.length > 0 }" />
            <h3 class="alerts-panel__title">Alertas de Precio</h3>
          </div>
          <div v-if="alerts.length === 0" class="alerts-panel__empty">
            <q-icon name="check_circle" size="1.5rem" style="color: var(--pq-success)" />
            Sin alertas activas
          </div>
          <div v-for="(a, i) in alerts" :key="i" class="alerts-panel__row">
            <span class="alerts-panel__badge" :class="`alerts-panel__badge--${a.type.toLowerCase()}`">
              {{ a.type === 'OVER_MAX' ? '!' : '⚡' }}
            </span>
            <div class="alerts-panel__body">
              <span class="alerts-panel__product">{{ a.productName }}</span>
              <span class="alerts-panel__message">{{ a.message }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="section-divider fade-in-up">
      <div class="section-divider__line" />
      <div class="section-divider__content">
        <q-icon name="analytics" size="1.2rem" class="section-divider__icon" />
        <div>
          <h2 class="section-divider__title">Análisis de Proveedores</h2>
          <p class="section-divider__subtitle">Comparativa cross-supplier, recomendaciones y predicciones de precio</p>
        </div>
      </div>
      <div class="section-divider__line" />
    </div>

    <div class="row q-col-gutter-md q-mb-lg stagger-children">
      <div class="col-12 col-lg-7">
        <SupplierComparisonTable :items="supplierComparison" :loading="analyticsLoading" />
      </div>
      <div class="col-12 col-lg-5">
        <SupplierRecommendationsCard :items="supplierRecommendations" />
      </div>
    </div>

    <div class="q-mb-lg fade-in-up">
      <PricePredictionsTable :items="pricePrediction" :loading="analyticsLoading" />
    </div>
  </q-page>
</template>

<style scoped lang="scss">
.metric-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.analysis-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }

  &__main,
  &__side {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
}

.alerts-panel {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 6px;

  &__header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    border-bottom: 1px solid var(--pq-border);
  }

  &__accent {
    width: 3px;
    height: 16px;
    border-radius: 2px;
    background: var(--pq-accent);

    &--danger {
      background: var(--pq-danger);
    }
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 14px;
    font-weight: 600;
    color: var(--pq-text);
    margin: 0;
  }

  &__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 24px 16px;
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    color: var(--pq-success);
  }

  &__row {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 10px 16px;
    border-bottom: 1px solid rgba(107, 104, 99, 0.06);

    &:last-child {
      border-bottom: none;
    }
  }

  &__badge {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 700;
    flex-shrink: 0;
    margin-top: 2px;

    &--over_max {
      background: rgba(160, 64, 56, 0.15);
      color: var(--pq-danger);
    }

    &--below_min,
    &--no_purchase {
      background: rgba(184, 134, 11, 0.15);
      color: var(--pq-warning);
    }
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__product {
    font-family: 'Satoshi', sans-serif;
    font-size: 13px;
    font-weight: 600;
    color: var(--pq-text);
  }

  &__message {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-muted);
  }
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 32px 0 24px;

  &__line {
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(200, 150, 62, 0.2), transparent);
  }

  &__content {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }

  &__icon {
    color: var(--pq-accent);
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 18px;
    font-weight: 700;
    color: var(--pq-text);
    margin: 0;
    line-height: 1.2;
  }

  &__subtitle {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-muted);
    margin: 2px 0 0;
  }
}
</style>
