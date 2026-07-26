<script setup lang="ts">
import { ref, shallowRef, computed, onMounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'

useMeta({ title: 'Análisis de Gastos — PYMEQ' });
import { api } from 'src/boot/axios'
import { productoService } from '../services/producto.service'
import { useAnalytics } from '../composables/useAnalytics'
import BaseCard from 'src/components/base/BaseCard.vue'
import KpiCard from '../components/dashboard/KpiCard.vue'
import PeriodSelector from '../components/dashboard/PeriodSelector.vue'
import SupplierComparisonTable from '../components/dashboard/SupplierComparisonTable.vue'
import SupplierRecommendationsCard from '../components/dashboard/SupplierRecommendationsCard.vue'
import PricePredictionsTable from '../components/dashboard/PricePredictionsTable.vue'
import type { Producto, SetupInfo, SetupCategory } from '../types'

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId

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

const lastPricesColumns = [
  { name: 'name', label: 'Producto', field: 'name', align: 'left' as const, sortable: true },
  { name: 'lastUnitPrice', label: 'Últ. Precio Unit.', field: 'lastUnitPrice', align: 'right' as const, sortable: true },
  { name: 'lastPurchaseDate', label: 'Últ. Compra', field: (r: Producto) => r.lastPurchaseDate ? new Date(r.lastPurchaseDate).toLocaleDateString() : '—', align: 'center' as const, sortable: true },
  { name: 'totalInvestment', label: 'Inversión Total', field: 'totalInvestment', align: 'right' as const, sortable: true },
  { name: 'minQuantity', label: 'Min', field: 'minQuantity', align: 'right' as const, sortable: false },
  { name: 'maxQuantity', label: 'Max', field: 'maxQuantity', align: 'right' as const, sortable: false },
]

const pagination = shallowRef({ sortBy: 'lastPurchaseDate', descending: true, page: 1, rowsPerPage: 15 })
const filter = shallowRef('')

async function load() {
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
    <!-- Header -->
    <div class="page-header fade-in-up">
      <div class="page-header__text">
        <h1 class="page-header__title">Análisis de Gastos</h1>
        <p class="page-header__subtitle">Motor de análisis de gastos por producto</p>
      </div>
      <PeriodSelector
        :model-value="period"
        :loading="analyticsLoading"
        @update:model-value="setPeriod"
        @recalcular="recalcularAnalytics"
      />
    </div>

    <!-- KPI Row -->
    <div class="row q-col-gutter-md q-mb-lg stagger-children">
      <div class="col-12 col-sm-6 col-md-3">
        <KpiCard
          label="Inversión Total"
          :value="`$${totalInvestment.toLocaleString('en-US', { minimumFractionDigits: 2 })}`"
          icon="payments"
          accent="copper"
        />
      </div>
      <div class="col-12 col-sm-6 col-md-3">
        <KpiCard
          label="Productos"
          :value="String(productCount)"
          icon="inventory_2"
          accent="sage"
        />
      </div>
      <div class="col-12 col-sm-6 col-md-3">
        <KpiCard
          label="Categorías"
          :value="String(byCategory.length)"
          icon="category"
          accent="gold"
        />
      </div>
      <div class="col-12 col-sm-6 col-md-3">
        <KpiCard
          label="Alertas"
          :value="String(alerts.length)"
          icon="warning"
          :accent="alerts.length > 0 ? 'negative' : 'positive'"
        />
      </div>
    </div>

    <!-- Category Breakdown + Alerts -->
    <div class="row q-col-gutter-md q-mb-lg">
      <div class="col-12 col-md-6">
        <BaseCard variant="default" class="section-card">
          <div class="section-card__header">
            <div class="section-card__accent" />
            <h3 class="section-card__title">Inversión por Categoría</h3>
          </div>
          <div v-if="byCategory.length === 0" class="section-card__empty">Sin datos</div>
          <div v-for="(cat, idx) in byCategory" :key="cat.name" class="category-row" :style="{ animationDelay: `${idx * 60}ms` }">
            <div class="category-row__header">
              <span class="category-row__name">{{ cat.name }}</span>
              <span class="category-row__meta">${{ cat.total.toLocaleString('en-US', { minimumFractionDigits: 2 }) }} · {{ cat.pct }}%</span>
            </div>
            <div class="category-row__track">
              <div
                class="category-row__bar"
                :style="{ width: `${cat.pct}%` }"
              />
            </div>
          </div>
        </BaseCard>
      </div>
      <div class="col-12 col-md-6">
        <BaseCard variant="default" class="section-card">
          <div class="section-card__header">
            <div class="section-card__accent section-card__accent--alert" />
            <h3 class="section-card__title">Alertas</h3>
          </div>
          <div v-if="alerts.length === 0" class="section-card__empty section-card__empty--success">
            <q-icon name="check_circle" size="1.5rem" class="text-positive" />
            Sin alertas activas
          </div>
          <div v-for="(a, i) in alerts" :key="i" class="alert-row">
            <div class="alert-row__icon">
              <q-icon v-if="a.type === 'OVER_MAX'" name="error" color="negative" size="sm" />
              <q-icon v-else name="warning" color="warning" size="sm" />
            </div>
            <div class="alert-row__body">
              <span class="alert-row__product">{{ a.productName }}</span>
              <span class="alert-row__message">{{ a.message }}</span>
            </div>
          </div>
        </BaseCard>
      </div>
    </div>

    <!-- Last Prices Table -->
    <BaseCard variant="default" class="section-card q-mb-lg">
      <div class="section-card__header">
        <div class="section-card__accent" />
        <div class="section-card__header-content">
          <h3 class="section-card__title">Últimos Precios Unitarios</h3>
          <p class="section-card__subtitle">Historial de precios por producto</p>
        </div>
        <q-input
          dark dense filled
          v-model="filter"
          placeholder="Buscar producto..."
          class="section-card__search"
        >
          <template v-slot:prepend>
            <q-icon name="search" size="1.1rem" class="text-accent" />
          </template>
        </q-input>
      </div>

      <q-table
        dark flat
        :rows="products"
        :columns="lastPricesColumns"
        row-key="id"
        :loading="loading"
        :filter="filter"
        v-model:pagination="pagination"
        :rows-per-page-options="[10, 20, 50]"
        class="section-card__table"
      >
        <template v-slot:body-cell-lastUnitPrice="{ row }">
          <td class="text-right">
            <span class="price-cell">{{ row.lastUnitPrice != null ? `$${row.lastUnitPrice.toFixed(2)}` : '—' }}</span>
          </td>
        </template>

        <template v-slot:body-cell-totalInvestment="{ row }">
          <td class="text-right">
            <span class="price-cell">{{ row.totalInvestment != null ? `$${row.totalInvestment.toFixed(2)}` : '$0.00' }}</span>
          </td>
        </template>

        <template v-slot:body-cell-minQuantity="{ row }">
          <td class="text-right text-accent">{{ row.minQuantity != null ? `$${row.minQuantity}` : '—' }}</td>
        </template>

        <template v-slot:body-cell-maxQuantity="{ row }">
          <td class="text-right text-accent">{{ row.maxQuantity != null ? `$${row.maxQuantity}` : '—' }}</td>
        </template>

        <template v-slot:no-data>
          <div class="empty-table">
            <q-icon name="receipt_long" size="2rem" class="text-accent" />
            <p>Sin productos registrados</p>
          </div>
        </template>
      </q-table>
    </BaseCard>

    <!-- Supplier Analytics Divider -->
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

    <!-- Supplier Cards -->
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
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 1.75rem;
  gap: 1rem;

  &__title {
    font-family: 'Outfit', sans-serif;
    font-size: 1.75rem;
    font-weight: 700;
    color: #E2E8E4;
    margin: 0;
    line-height: 1.1;
  }

  &__subtitle {
    font-size: 0.85rem;
    color: #8A9E99;
    margin: 0.25rem 0 0;
  }
}

.section-card {
  :deep(.base-card) {
    padding: 0;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1.25rem 1.5rem;
    position: relative;
    gap: 1rem;
  }

  &__header-content {
    flex: 1;
  }

  &__accent {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: #A3785E;
    border-radius: 0 2px 2px 0;

    &--alert {
      background: #8B4513;
    }
  }

  &__title {
    font-family: 'Outfit', sans-serif;
    font-size: 1rem;
    font-weight: 600;
    color: #E2E8E4;
    margin: 0;
  }

  &__subtitle {
    font-size: 0.7rem;
    color: #8A9E99;
    margin: 0.2rem 0 0;
  }

  &__search {
    max-width: 220px;
    flex-shrink: 0;
  }

  &__empty {
    padding: 2rem 1.5rem;
    text-align: center;
    font-size: 0.85rem;
    color: #8A9E99;

    &--success {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      color: #2D5A27;
    }
  }

  &__table {
    :deep(.q-table) {
      background: transparent;

      thead tr th {
        font-size: 0.7rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        color: #8A9E99;
        padding: 0.65rem 1rem;
        border-bottom: 1px solid rgba(113, 131, 127, 0.1);
      }

      tbody tr {
        border-bottom: 1px solid rgba(113, 131, 127, 0.05);
        transition: background 0.2s ease;

        &:hover {
          background: rgba(163, 120, 94, 0.04) !important;
        }
      }
    }
  }
}

.category-row {
  padding: 0 1.5rem;
  animation: catRowIn 0.35s ease forwards;
  opacity: 0;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.3rem;
  }

  &__name {
    font-size: 0.82rem;
    color: #E2E8E4;
    font-weight: 500;
  }

  &__meta {
    font-family: 'Outfit', sans-serif;
    font-size: 0.75rem;
    color: #8A9E99;
  }

  &__track {
    height: 5px;
    background: rgba(113, 131, 127, 0.1);
    border-radius: 3px;
    overflow: hidden;
    margin-bottom: 0.75rem;
  }

  &__bar {
    height: 100%;
    background: linear-gradient(90deg, #A3785E, #C5A059);
    border-radius: 3px;
    transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.alert-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.6rem 1.5rem;
  border-bottom: 1px solid rgba(113, 131, 127, 0.06);

  &:last-child {
    border-bottom: none;
  }

  &__icon {
    padding-top: 0.1rem;
    flex-shrink: 0;
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
  }

  &__product {
    font-size: 0.82rem;
    font-weight: 600;
    color: #E2E8E4;
  }

  &__message {
    font-size: 0.72rem;
    color: #8A9E99;
  }
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  margin: 2.5rem 0 1.75rem;

  &__line {
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(163, 120, 94, 0.3), transparent);
  }

  &__content {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-shrink: 0;
  }

  &__icon {
    color: #A3785E;
  }

  &__title {
    font-family: 'Outfit', sans-serif;
    font-size: 1.25rem;
    font-weight: 700;
    color: #E2E8E4;
    margin: 0;
    line-height: 1.2;
  }

  &__subtitle {
    font-size: 0.75rem;
    color: #8A9E99;
    margin: 0.15rem 0 0;
  }
}

.price-cell {
  font-family: 'Outfit', sans-serif;
  font-weight: 600;
  font-size: 0.9rem;
}

.empty-table {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem 1rem;
  gap: 0.5rem;

  p {
    font-size: 0.85rem;
    color: #8A9E99;
    margin: 0;
  }
}

@keyframes catRowIn {
  from {
    opacity: 0;
    transform: translateX(-6px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
