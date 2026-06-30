<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { productoService } from '../services/producto.service'
import BaseCard from 'src/components/base/BaseCard.vue'
import type { Producto } from '../types'

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const products = ref<Producto[]>([])
const loading = ref(false)

const totalInvestment = computed(() =>
  products.value.reduce((sum, p) => sum + (p.totalInvestment ?? 0), 0)
)

const productCount = computed(() => products.value.length)

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
    const cat = p.category || 'Sin categoría'
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

const pagination = ref({ sortBy: 'lastPurchaseDate', descending: true, page: 1, rowsPerPage: 15 })
const filter = ref('')

async function load() {
  loading.value = true
  try {
    const res = await productoService.getAll(tenantId)
    products.value = res.data
  } catch { $q.notify({ type: 'negative', message: 'Error al cargar productos' })
  } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-md fade-in-up">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Análisis de Gastos</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">
        Motor de análisis de gastos por producto
      </p>
    </div>

    <!-- Summary Cards -->
    <div class="row q-col-gutter-md q-mb-lg">
      <div class="col-12 col-sm-6 col-md-3">
        <BaseCard variant="default" class="q-pa-lg">
          <div class="text-overline text-accent">Inversión Total</div>
          <div class="text-h4 text-primary font-bold q-mt-sm">${{ totalInvestment.toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</div>
        </BaseCard>
      </div>
      <div class="col-12 col-sm-6 col-md-3">
        <BaseCard variant="default" class="q-pa-lg">
          <div class="text-overline text-accent">Productos</div>
          <div class="text-h4 text-primary font-bold q-mt-sm">{{ productCount }}</div>
        </BaseCard>
      </div>
      <div class="col-12 col-sm-6 col-md-3">
        <BaseCard variant="default" class="q-pa-lg">
          <div class="text-overline text-accent">Categorías</div>
          <div class="text-h4 text-primary font-bold q-mt-sm">{{ byCategory.length }}</div>
        </BaseCard>
      </div>
      <div class="col-12 col-sm-6 col-md-3">
        <BaseCard variant="default" class="q-pa-lg">
          <div class="text-overline text-accent">Alertas</div>
          <div class="text-h4 text-primary font-bold q-mt-sm" :class="{ 'text-warning': alerts.length > 0 }">{{ alerts.length }}</div>
        </BaseCard>
      </div>
    </div>

    <!-- Investment by Category + Alerts -->
    <div class="row q-col-gutter-md q-mb-lg">
      <div class="col-12 col-md-6">
        <BaseCard variant="default" class="q-pa-lg">
          <div class="text-h6 text-primary q-mb-md">Inversión por Categoría</div>
          <div v-if="byCategory.length === 0" class="text-accent text-caption">Sin datos</div>
          <div v-for="cat in byCategory" :key="cat.name" class="q-mb-sm">
            <div class="row items-center justify-between">
              <div class="text-body2 text-secondary">{{ cat.name }}</div>
              <div class="text-caption text-accent">${{ cat.total.toLocaleString('en-US', { minimumFractionDigits: 2 }) }} ({{ cat.pct }}%)</div>
            </div>
            <q-linear-progress :value="cat.pct / 100" color="primary" class="q-mt-xs" size="8px" rounded />
          </div>
        </BaseCard>
      </div>
      <div class="col-12 col-md-6">
        <BaseCard variant="default" class="q-pa-lg">
          <div class="text-h6 text-primary q-mb-md">Alertas</div>
          <div v-if="alerts.length === 0" class="text-accent text-caption">Sin alertas activas</div>
          <div v-for="(a, i) in alerts" :key="i" class="q-mb-sm row items-center q-gutter-x-sm">
            <q-icon v-if="a.type === 'OVER_MAX'" name="warning" color="negative" size="sm" />
            <q-icon v-else name="info" color="warning" size="sm" />
            <div>
              <div class="text-body2 text-secondary">{{ a.productName }}</div>
              <div class="text-caption text-accent">{{ a.message }}</div>
            </div>
          </div>
        </BaseCard>
      </div>
    </div>

    <!-- Last Prices Table -->
    <BaseCard variant="default" padding>
      <q-table
        dark flat
        :rows="products"
        :columns="lastPricesColumns"
        row-key="id"
        :loading="loading"
        :filter="filter"
        v-model:pagination="pagination"
        :rows-per-page-options="[10, 20, 50]"
      >
        <template v-slot:top>
          <div class="text-h6 text-primary">Últimos Precios Unitarios</div>
          <q-space />
          <q-input dark dense filled v-model="filter" placeholder="Buscar producto..." class="q-mr-sm" style="max-width: 250px">
            <template v-slot:prepend><q-icon name="search" /></template>
          </q-input>
        </template>

        <template v-slot:body-cell-lastUnitPrice="{ row }">
          <td class="text-right">
            <span class="text-weight-medium">{{ row.lastUnitPrice != null ? `$${row.lastUnitPrice.toFixed(2)}` : '—' }}</span>
          </td>
        </template>

        <template v-slot:body-cell-totalInvestment="{ row }">
          <td class="text-right">
            <span>{{ row.totalInvestment != null ? `$${row.totalInvestment.toFixed(2)}` : '$0.00' }}</span>
          </td>
        </template>

        <template v-slot:body-cell-minQuantity="{ row }">
          <td class="text-right text-accent">{{ row.minQuantity != null ? `$${row.minQuantity}` : '—' }}</td>
        </template>

        <template v-slot:body-cell-maxQuantity="{ row }">
          <td class="text-right text-accent">{{ row.maxQuantity != null ? `$${row.maxQuantity}` : '—' }}</td>
        </template>
      </q-table>
    </BaseCard>
  </q-page>
</template>
