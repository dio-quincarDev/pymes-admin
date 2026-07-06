<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { useGreeting } from 'src/composables/useGreeting'
import { productoService } from '../../services/producto.service'
import { proveedorService } from '../../services/proveedor.service'
import { api } from 'src/boot/axios'
import { useNumberFormat } from '../../composables/useNumberFormat'
import type { Producto, SetupCategory } from '../../types'
import type { SetupInfo } from '../../types'
import KpiCard from './KpiCard.vue'

const $q = useQuasar()
const authStore = useAuthStore()
const { greeting } = useGreeting()
const tenantId = authStore.user?.tenantId || ''
const { formatCurrency } = useNumberFormat()

useMeta({ title: 'Dashboard — PYMEQ' })

const products = ref<Producto[]>([])
const setup = ref<SetupInfo | null>(null)
const supplierCount = ref(0)
const loading = ref(true)
const error = ref<string | null>(null)
const search = ref('')

const expandedCategories = ref<Set<string>>(new Set())

function toggleCategory(code: string) {
  const next = new Set(expandedCategories.value)
  if (next.has(code)) next.delete(code)
  else next.add(code)
  expandedCategories.value = next
}

function expandAll() {
  const all = new Set<string>()
  function walk(cats: SetupCategory[]) {
    for (const c of cats) {
      all.add(c.code)
      walk(c.children || [])
    }
  }
  walk(setup.value?.categories || [])
  expandedCategories.value = all
}

function collapseAll() {
  expandedCategories.value = new Set()
}

const unitMap = computed(() => {
  const map = new Map<string, string>()
  for (const u of setup.value?.units || []) map.set(u.code, u.name)
  return map
})

const productsByCategory = computed(() => {
  const map = new Map<string, Producto[]>()
  for (const p of products.value) {
    const code = p.category
    if (!map.has(code)) map.set(code, [])
    map.get(code)!.push(p)
  }
  return map
})

interface CategoryNode {
  category: SetupCategory
  products: Producto[]
  children: CategoryNode[]
}

function buildTree(cats: SetupCategory[]): CategoryNode[] {
  return cats.map(cat => ({
    category: cat,
    products: productsByCategory.value.get(cat.code) || [],
    children: buildTree(cat.children || []),
  }))
}

const tree = computed(() => buildTree(setup.value?.categories || []))

function countAllProducts(node: CategoryNode): number {
  let count = node.products.length
  for (const child of node.children) count += countAllProducts(child)
  return count
}

interface TreeRow {
  type: 'category' | 'product' | 'empty'
  depth: number
  label: string
  categoryCode?: string
  productCount?: number
  expanded?: boolean
  sku?: string
  unit?: string
  lastUnitPrice?: number | null
  presentaciones?: number
}

const treeRows = computed(() => {
  const rows: TreeRow[] = []
  function walk(nodes: CategoryNode[], depth: number) {
    for (const node of nodes) {
      const total = countAllProducts(node)
      rows.push({
        type: 'category',
        depth,
        label: node.category.name,
        categoryCode: node.category.code,
        productCount: total,
        expanded: expandedCategories.value.has(node.category.code),
      })
      if (expandedCategories.value.has(node.category.code)) {
        for (const p of node.products) {
          rows.push({
            type: 'product',
            depth: depth + 1,
            label: p.name,
            sku: p.sku,
            unit: unitMap.value.get(p.baseUnit) || p.baseUnit,
            lastUnitPrice: p.lastUnitPrice,
            presentaciones: p.presentaciones?.length || 0,
          })
        }
        if (node.children.length > 0) walk(node.children, depth + 1)
        if (node.products.length === 0 && node.children.length === 0) {
          rows.push({ type: 'empty', depth: depth + 1, label: 'Sin productos' })
        }
      }
    }
  }
  walk(tree.value, 0)
  return rows
})

const kpis = computed(() => [
  { label: 'Productos', value: String(products.value.length), icon: 'inventory_2', accent: 'copper' as const },
  { label: 'Categorías', value: String(tree.value.length), icon: 'category', accent: 'sage' as const },
  { label: 'Proveedores', value: String(supplierCount.value), icon: 'people', accent: 'gold' as const },
  {
    label: 'Inversión Total',
    value: formatCurrency(products.value.reduce((s, p) => s + (p.totalInvestment || 0), 0)),
    icon: 'payments',
    accent: 'copper' as const,
  },
])

const filteredRows = computed(() => {
  if (!search.value.trim()) return null
  const q = search.value.toLowerCase()
  return products.value.filter(p =>
    p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)
  )
})

async function loadData() {
  if (!tenantId) { loading.value = false; return }
  loading.value = true
  error.value = null
  try {
    const [prodRes, provRes, setupRes] = await Promise.all([
      productoService.getAll(tenantId),
      proveedorService.getAll(tenantId),
      api.get<SetupInfo>(`/core/setup/${tenantId}`),
    ])
    products.value = prodRes.data
    supplierCount.value = provRes.data.length
    setup.value = setupRes.data
    expandAll()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Error al cargar datos del dashboard'
    $q.notify({ type: 'negative', message: 'Error al cargar datos del dashboard' })
  } finally { loading.value = false }
}

onMounted(loadData)
</script>

<template>
  <div class="catalog-dashboard">
    <!-- Header -->
    <div class="dashboard-header fade-in-up">
      <h1 class="dashboard-title">
        {{ greeting }},
        <span class="dashboard-title__name">{{ authStore.user?.name || 'Usuario' }}</span>
      </h1>
      <p class="dashboard-subtitle">Panel de control — Catálogo de productos</p>
    </div>

    <!-- KPIs -->
    <div class="row q-col-gutter-md q-mb-lg">
      <div class="col-12 col-sm-6 col-lg-3" v-for="kpi in kpis" :key="kpi.label">
        <KpiCard v-bind="kpi" />
      </div>
    </div>

    <!-- Loading state -->
    <template v-if="loading">
      <div class="row q-col-gutter-lg">
        <div class="col-12">
          <div class="catalog-card">
            <div v-for="i in 6" :key="i" class="skeleton-row" :style="{ animationDelay: `${i * 0.08}s` }">
              <div class="skeleton-line skeleton-line--sm" />
              <div class="skeleton-line skeleton-line--lg" />
              <div class="skeleton-line skeleton-line--md" />
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Error state -->
    <template v-else-if="error">
      <div class="catalog-card text-center q-py-xl">
        <q-icon name="error_outline" size="3rem" color="negative" aria-hidden="true" />
        <p class="text-accent q-mt-md">{{ error }}</p>
        <q-btn flat color="primary" label="Reintentar" @click="loadData" class="q-mt-sm" />
      </div>
    </template>

    <!-- Main content -->
    <template v-else>
      <!-- Search bar -->
      <div class="catalog-card q-mb-lg">
        <div class="row items-center q-col-gutter-md">
          <div class="col-12 col-md-6">
            <q-input
              dark dense filled v-model="search"
              placeholder="Buscar productos por nombre o SKU..."
              clearable
              class="search-input"
            >
              <template v-slot:prepend><q-icon name="search" color="accent" aria-hidden="true" /></template>
              <template v-slot:append>
                <q-chip v-if="search" dense dark color="primary" text-color="dark" class="q-px-sm">
                  {{ filteredRows?.length || 0 }} resultados
                </q-chip>
              </template>
            </q-input>
          </div>
          <div class="col-12 col-md-6 row justify-end q-gutter-x-sm items-center">
            <span class="text-caption text-accent">
              {{ products.length }} productos · {{ tree.length }} categorías
            </span>
            <q-btn flat dense color="accent" size="sm" label="Expandir todo" @click="expandAll" />
            <q-btn flat dense color="accent" size="sm" label="Colapsar" @click="collapseAll" />
          </div>
        </div>
      </div>

      <!-- Search results (flat) -->
      <div v-if="filteredRows" class="catalog-card">
        <div class="catalog-card__title">Resultados de búsqueda</div>
        <div v-if="filteredRows.length === 0" class="text-center q-py-lg text-accent" role="status">
          <q-icon name="search_off" size="2rem" class="q-mb-sm" aria-hidden="true" />
          <p>No se encontraron productos para "{{ search }}"</p>
        </div>
        <div v-else class="search-results">
          <div
            v-for="p in filteredRows"
            :key="p.id"
            class="search-row row items-center q-py-sm q-px-md"
          >
            <div class="col-5">
              <span class="text-secondary text-weight-medium">{{ p.name }}</span>
            </div>
            <div class="col-2 text-caption text-accent">{{ p.sku }}</div>
            <div class="col-2 text-caption text-accent">{{ p.category }}</div>
            <div class="col-1 text-caption text-accent">{{ unitMap.get(p.baseUnit) || p.baseUnit }}</div>
            <div class="col-2 text-right text-caption text-secondary text-weight-medium">
              {{ p.lastUnitPrice ? formatCurrency(p.lastUnitPrice) : '—' }}
            </div>
          </div>
        </div>
      </div>

      <!-- Category tree -->
      <div v-else class="catalog-tree">
        <div v-if="treeRows.length === 0" class="catalog-card text-center q-py-xl" role="status">
          <q-icon name="inventory_2" size="3rem" color="accent" class="q-mb-sm" aria-hidden="true" />
          <p class="text-accent">No hay productos en el catálogo</p>
          <q-btn flat color="primary" label="Ir a Productos" :to="'/dashboard/productos'" class="q-mt-sm" />
        </div>

        <div
          v-for="(row, i) in treeRows"
          :key="i"
          class="tree-row"
          :class="[
            `tree-row--${row.type}`,
            { 'tree-row--expanded': row.expanded }
          ]"
          :style="{ paddingLeft: `${12 + row.depth * 24}px` }"
        >
          <!-- Category row -->
          <div
            v-if="row.type === 'category'"
            class="tree-row__content row items-center"
            @click="toggleCategory(row.categoryCode!)"
            role="button"
            :aria-expanded="row.expanded"
            tabindex="0"
            @keydown.enter="toggleCategory(row.categoryCode!)"
            @keydown.space.prevent="toggleCategory(row.categoryCode!)"
          >
            <q-icon
              :name="row.expanded ? 'expand_more' : 'chevron_right'"
              size="1.25rem"
              class="tree-row__chevron text-accent q-mr-sm"
            />
            <span class="tree-row__cat-name">{{ row.label }}</span>
            <q-chip dense dark color="primary" text-color="dark" size="sm" class="q-ml-sm q-px-sm">
              {{ row.productCount }}
            </q-chip>
            <q-space />
            <q-icon name="folder" size="1rem" color="accent" class="tree-row__folder-icon" aria-hidden="true" />
          </div>

          <!-- Product row -->
          <div v-else-if="row.type === 'product'" class="tree-row__content row items-center no-hover">
            <div class="col-5 col-sm-4">
              <span class="text-secondary text-weight-medium">{{ row.label }}</span>
            </div>
            <div class="col-2 col-sm-2 text-caption text-accent hide-xs">
              <q-badge outline color="accent" class="q-px-xs">{{ row.sku }}</q-badge>
            </div>
            <div class="col-2 col-sm-2 text-caption text-accent">
              <q-chip dense dark color="transparent" text-color="sage-muted" size="sm" icon="straighten" class="q-px-xs">
                {{ row.unit }}
              </q-chip>
            </div>
            <div class="col-2 col-sm-2 text-caption text-accent hide-xs">
              <span v-if="row.presentaciones">{{ row.presentaciones }} pres.</span>
            </div>
            <div class="col-3 col-sm-2 text-right text-caption text-secondary text-weight-medium">
              {{ row.lastUnitPrice ? formatCurrency(row.lastUnitPrice) : '—' }}
            </div>
          </div>

          <!-- Empty state row -->
          <div v-else-if="row.type === 'empty'" class="tree-row__content tree-row__empty no-hover">
            <span class="text-accent text-caption q-ml-lg">{{ row.label }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.catalog-dashboard {
  width: 100%;
}

.dashboard-header {
  margin-bottom: 1.5rem;
}

.dashboard-title {
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 1.75rem;
  color: #E2E8E4;
  margin: 0;
  text-shadow: 0 0 20px rgba(163, 120, 94, 0.15);

  &__name {
    color: #A3785E;
  }
}

.dashboard-subtitle {
  font-size: 0.95rem;
  color: #8A9E99;
  margin: 0.25rem 0 0;
}

.catalog-card {
  background: rgba(27, 38, 36, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(113, 131, 127, 0.08);
  border-radius: 8px;
  padding: 1.25rem;

  &__title {
    font-size: 0.8rem;
    font-weight: 600;
    color: #8A9E99;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 1rem;
  }
}

.search-input {
  :deep(.q-field__control) {
    border-radius: 8px;
  }
}

.search-results {
  .search-row {
    border-bottom: 1px solid rgba(113, 131, 127, 0.06);
    transition: background 0.15s;

    &:hover {
      background: rgba(163, 120, 94, 0.04);
    }

    &:last-child { border-bottom: none; }
  }
}

.tree-row {
  transition: background 0.15s;

  &--category {
    .tree-row__content {
      cursor: pointer;
      padding: 0.625rem 0;
      border-radius: 6px;
      transition: background 0.15s;
      user-select: none;

      &:hover {
        background: rgba(163, 120, 94, 0.06);
      }
    }
  }

  &--product {
    .tree-row__content {
      padding: 0.45rem 0;
      border-bottom: 1px solid rgba(113, 131, 127, 0.04);
      transition: background 0.15s;
    }

    &:hover .tree-row__content {
      background: rgba(163, 120, 94, 0.03);
    }
  }

  &--expanded > .tree-row__content {
    .tree-row__chevron { color: #A3785E !important; }
  }

  &__cat-name {
    font-size: 0.9rem;
    font-weight: 600;
    color: #E2E8E4;
  }

  &__chevron {
    transition: transform 0.2s ease;
    flex-shrink: 0;
  }

  &__folder-icon {
    opacity: 0.3;
  }

  &__empty {
    padding: 0.4rem 0;
  }

  .no-hover { cursor: default; }
}

.skeleton-row {
  display: flex;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid rgba(113, 131, 127, 0.06);
  animation: shimmer 1.5s infinite;
  animation-delay: inherit;

  .skeleton-line {
    height: 12px;
    border-radius: 4px;
    background: linear-gradient(90deg, rgba(138, 158, 153, 0.08) 25%, rgba(163, 120, 94, 0.12) 50%, rgba(138, 158, 153, 0.08) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;

    &--sm { width: 20%; }
    &--md { width: 30%; }
    &--lg { width: 40%; }
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 599px) {
  .hide-xs { display: none; }
}
</style>
