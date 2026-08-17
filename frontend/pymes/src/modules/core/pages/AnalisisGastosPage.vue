<script setup lang="ts">
import { ref, shallowRef, computed, onMounted } from 'vue'
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
import AbcGastosChart from '../components/dashboard/AbcGastosChart.vue'
import PriceTrendSparkline from '../components/dashboard/PriceTrendSparkline.vue'
import MarginImpactTable from '../components/dashboard/MarginImpactTable.vue'
import OpexGauge from '../components/dashboard/OpexGauge.vue'
import ProjectionTimeline from '../components/dashboard/ProjectionTimeline.vue'
import AlertsPanel from '../components/dashboard/AlertsPanel.vue'
import SupplierComparisonTable from '../components/dashboard/SupplierComparisonTable.vue'
import SupplierRecommendationsCard from '../components/dashboard/SupplierRecommendationsCard.vue'
import PricePredictionsTable from '../components/dashboard/PricePredictionsTable.vue'
import type { Producto, SetupInfo, SetupCategory } from '../types'

useMeta({ title: 'Análisis de Gastos — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId
const { formatCurrency } = useNumberFormat()

const {
  period, setPeriod, recalcular: recalcularAnalytics,
  loading: analyticsLoading,
  abc, trend, margin, opexPct, projection, alerts,
  supplierComparison, supplierRecommendations, pricePrediction,
} = useAnalytics()

const products = ref<Producto[]>([])
const setupCategories = ref<SetupCategory[]>([])
const loading = ref(false)
const trendTab = shallowRef<'trend' | 'margin'>('trend')

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

const tableColumns = [
  { name: 'name', label: 'Producto', field: 'name', align: 'left' as const, sortable: true },
  { name: 'lastUnitPrice', label: 'Últ. Precio Unit.', field: 'lastUnitPrice', align: 'right' as const, sortable: true },
  { name: 'lastPurchaseDate', label: 'Últ. Compra', field: (r: Record<string, unknown>) => r.lastPurchaseDate ? new Date(r.lastPurchaseDate as string).toLocaleDateString() : '—', align: 'center' as const, sortable: true },
  { name: 'totalInvestment', label: 'Inversión Total', field: 'totalInvestment', align: 'right' as const, sortable: true },
]

const filter = ref('')

const opexValue = computed(() => {
  const item = opexPct.value[0]
  if (!item?.totalSpend) return 0
  return (item.totalSpend / (item.projectedMonthly || 1)) * 100
})

const opexProjected = computed(() => opexPct.value[0]?.projectedMonthly ?? 0)

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
      subtitle="Dónde gasto y qué proveedores me convienen"
      :period="period"
      :loading="analyticsLoading || loading"
      @update:period="setPeriod"
      @recalculate="recalcularAnalytics"
    />

    <!-- Vital: siempre visible -->
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
    </div>

    <!-- Bajo demanda: 6 motores colapsados -->
    <div class="expansion-section">
      <q-expansion-item
        header-class="expansion-header"
        dense
        toggle-class="expansion-toggle"
        label="Alertas"
        caption="Variaciones de precio significativas"
      >
        <div class="expansion-content">
          <AlertsPanel :items="alerts" />
        </div>
      </q-expansion-item>

      <q-expansion-item
        header-class="expansion-header"
        dense
        toggle-class="expansion-toggle"
        label="Clasificación ABC"
        caption="Pareto de gastos por producto"
      >
        <div class="expansion-content">
          <AbcGastosChart :data="abc" :height="300" />
        </div>
      </q-expansion-item>

      <q-expansion-item
        header-class="expansion-header"
        dense
        toggle-class="expansion-toggle"
        label="Precios y márgenes"
        caption="Tendencias de precio e impacto en márgenes"
      >
        <div class="expansion-content">
          <q-tabs v-model="trendTab" class="q-mb-md" dense no-caps>
            <q-tab name="trend" label="Tendencias" />
            <q-tab name="margin" label="Impacto Márgenes" />
          </q-tabs>
          <PriceTrendSparkline v-if="trendTab === 'trend'" :items="trend" />
          <MarginImpactTable v-else :items="margin" />
        </div>
      </q-expansion-item>

      <q-expansion-item
        header-class="expansion-header"
        dense
        toggle-class="expansion-toggle"
        label="Costo del día"
        caption="Gasto operativo vs proyectado"
      >
        <div class="expansion-content">
          <OpexGauge
            :value="opexValue"
            :max="100"
            :thresholds="{ warning: 70, critical: 85 }"
          />
          <div class="text-caption text-accent text-center q-mt-sm">
            Proy. mensual: {{ formatCurrency(opexProjected) }}
          </div>
        </div>
      </q-expansion-item>

      <q-expansion-item
        header-class="expansion-header"
        dense
        toggle-class="expansion-toggle"
        label="Proyección 30/60/90"
        caption="Tendencia de gastos a futuro"
      >
        <div class="expansion-content">
          <ProjectionTimeline :items="projection" />
        </div>
      </q-expansion-item>

      <q-expansion-item
        header-class="expansion-header"
        dense
        toggle-class="expansion-toggle"
        label="Proveedores"
        caption="Comparativa, recomendaciones y predicciones"
      >
        <div class="expansion-content">
          <div class="row q-col-gutter-md q-mb-md">
            <div class="col-12 col-lg-7">
              <SupplierComparisonTable :items="supplierComparison" :loading="analyticsLoading" />
            </div>
            <div class="col-12 col-lg-5">
              <SupplierRecommendationsCard :items="supplierRecommendations" />
            </div>
          </div>
          <PricePredictionsTable :items="pricePrediction" :loading="analyticsLoading" />
        </div>
      </q-expansion-item>
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
  grid-template-columns: 1fr;
  gap: 24px;
  margin-bottom: 24px;

  &__main {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
}

.expansion-section {
  display: flex;
  flex-direction: column;
  gap: 2px;
  border: 1px solid var(--pq-border);
  border-radius: 6px;
  overflow: hidden;
  background: var(--pq-surface);
}

.expansion-header {
  font-family: 'Geist', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: var(--pq-text);
  min-height: 44px;
}

.expansion-toggle {
  color: var(--pq-accent);
}

.expansion-content {
  padding: 16px;
  border-top: 1px solid var(--pq-border);
}
</style>
