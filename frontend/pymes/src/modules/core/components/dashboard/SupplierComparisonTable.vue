<template>
  <div class="comparison-card">
    <div class="comparison-card__header">
      <div class="comparison-card__accent" />
      <div class="comparison-card__title-row">
        <div>
          <h3 class="comparison-card__title">Comparativa por Proveedor</h3>
          <p class="comparison-card__subtitle">Análisis cruzado de precios por producto</p>
        </div>
        <q-input
          dark dense filled
          v-model="filter"
          placeholder="Buscar producto..."
          class="comparison-card__search"
          input-class="comparison-card__search-input"
        >
          <template v-slot:prepend>
            <q-icon name="search" size="1.1rem" style="color: var(--pq-accent)" />
          </template>
        </q-input>
      </div>
    </div>

    <q-table
      dark flat
      :rows="items"
      :columns="columns"
      row-key="productId"
      :loading="loading"
      :filter="filter"
      v-model:pagination="pagination"
      :rows-per-page-options="[10, 20, 50]"
      :filter-method="filterFn"
      class="comparison-card__table"
    >
      <template v-slot:body="props">
        <q-tr :props="props" class="comparison-card__row">
          <q-td key="productName" class="comparison-card__cell comparison-card__cell--product">
            <span class="comparison-card__product-name">{{ props.row.productName }}</span>
          </q-td>
          <q-td key="providerName" class="comparison-card__cell">
            <div class="comparison-card__supplier">
              <span class="comparison-card__supplier-dot" />
              <span>{{ props.row.providerName }}</span>
            </div>
          </q-td>
          <q-td key="avgPrice" class="comparison-card__cell comparison-card__cell--right">
            <span class="comparison-card__price">{{ formatCurrency(props.row.avgPrice) }}</span>
          </q-td>
          <q-td key="minMax" class="comparison-card__cell comparison-card__cell--right">
            <span class="comparison-card__price comparison-card__price--low">{{ formatCurrency(props.row.minPrice) }}</span>
            <span class="comparison-card__separator">—</span>
            <span class="comparison-card__price comparison-card__price--high">{{ formatCurrency(props.row.maxPrice) }}</span>
          </q-td>
          <q-td key="purchaseCount" class="comparison-card__cell comparison-card__cell--right">
            <span class="comparison-card__count">{{ props.row.purchaseCount }}</span>
          </q-td>
          <q-td key="priceStddev" class="comparison-card__cell comparison-card__cell--right comparison-card__cell--volatility">
            <div class="comparison-card__volatility">
              <div class="comparison-card__volatility-bar">
                <div
                  class="comparison-card__volatility-fill"
                  :class="volatilityClass(props.row.priceStddev)"
                  :style="{ width: volatilityWidth(props.row.priceStddev) }"
                />
              </div>
              <span
                class="comparison-card__volatility-label"
                :class="volatilityClass(props.row.priceStddev)"
              >
                {{ formatCurrency(props.row.priceStddev) }}
              </span>
            </div>
          </q-td>
        </q-tr>
      </template>

      <template v-slot:no-data>
        <div class="comparison-card__empty">
          <q-icon name="analytics" size="2.5rem" style="color: var(--pq-accent)" />
          <p class="comparison-card__empty-text">Sin datos de comparativa para este período</p>
        </div>
      </template>
    </q-table>
  </div>
</template>

<script setup lang="ts">
import { shallowRef } from 'vue';
import type { SupplierComparisonItem } from '../../types/analytics';
import { formatCurrency } from 'src/utils/format';

defineProps<{ items: SupplierComparisonItem[]; loading?: boolean }>();

const columns = [
  { name: 'productName', label: 'Producto', field: 'productName', align: 'left' as const, sortable: true },
  { name: 'providerName', label: 'Proveedor', field: 'providerName', align: 'left' as const, sortable: true },
  { name: 'avgPrice', label: 'Precio Prom.', field: 'avgPrice', align: 'right' as const, sortable: true },
  { name: 'minMax', label: 'Mín — Máx', field: 'minPrice', align: 'right' as const, sortable: true },
  { name: 'purchaseCount', label: 'Compras', field: 'purchaseCount', align: 'right' as const, sortable: true },
  { name: 'priceStddev', label: 'Volatilidad', field: 'priceStddev', align: 'right' as const, sortable: true },
];

const pagination = shallowRef({ sortBy: 'productName', descending: false, page: 1, rowsPerPage: 10 });
const filter = shallowRef('');

function volatilityClass(stddev: number): string {
  if (stddev > 3) return 'volatility--high';
  if (stddev > 1.5) return 'volatility--medium';
  return 'volatility--low';
}

function volatilityWidth(stddev: number): string {
  return `${Math.min((stddev / 5) * 100, 100)}%`;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function filterFn(rows: readonly any[], terms: any) {
  const lower = String(terms).toLowerCase();
  return rows.filter(
    (r: SupplierComparisonItem) =>
      r.productName.toLowerCase().includes(lower) ||
      r.providerName.toLowerCase().includes(lower),
  );
}
</script>

<style scoped lang="scss">
.comparison-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: 8px;
  overflow: hidden;

  &__header {
    padding: 1.25rem 1.5rem;
    position: relative;
  }

  &__accent {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: var(--pq-accent);
    border-radius: 0 2px 2px 0;
  }

  &__title-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 1.1rem;
    font-weight: 600;
    color: var(--pq-text);
    margin: 0;
    line-height: 1.2;
  }

  &__subtitle {
    font-family: 'Satoshi', sans-serif;
    font-size: 0.75rem;
    color: var(--pq-text-muted);
    margin: 0.25rem 0 0;
  }

  &__search {
    max-width: 220px;
    flex-shrink: 0;
  }

  &__row {
    transition: background 0.2s ease;

    &:hover {
      background: rgba(200, 150, 62, 0.06) !important;
    }
  }

  &__cell {
    padding: 0.75rem 1rem !important;
    font-family: 'Satoshi', sans-serif;
    font-size: 0.85rem;
    color: var(--pq-text);

    &--right {
      text-align: right;
    }

    &--product {
      max-width: 200px;
    }

    &--volatility {
      min-width: 130px;
    }
  }

  &__product-name {
    font-weight: 600;
    color: var(--pq-text);
  }

  &__supplier {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  &__supplier-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--pq-accent);
    flex-shrink: 0;
  }

  &__price {
    font-family: 'Geist Mono', monospace;
    font-weight: 600;
    font-size: 0.9rem;

    &--low { color: var(--pq-success); }
    &--high { color: var(--pq-danger); }
  }

  &__separator {
    color: var(--pq-text-muted);
    margin: 0 0.35rem;
    font-size: 0.7rem;
  }

  &__count {
    font-family: 'Geist Mono', monospace;
    font-weight: 500;
    color: var(--pq-text-muted);
  }

  &__volatility {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 0.5rem;
  }

  &__volatility-bar {
    width: 50px;
    height: 4px;
    background: var(--pq-chart-grid);
    border-radius: 2px;
    overflow: hidden;
    flex-shrink: 0;
  }

  &__volatility-fill {
    height: 100%;
    border-radius: 2px;
    transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &__volatility-label {
    font-family: 'Geist Mono', monospace;
    font-size: 0.8rem;
    font-weight: 500;
    min-width: 40px;
    text-align: right;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 3rem 1rem;
    gap: 0.5rem;
  }

  &__empty-text {
    font-family: 'Satoshi', sans-serif;
    font-size: 0.85rem;
    color: var(--pq-text-muted);
    margin: 0;
  }
}

.volatility--low {
  background: var(--pq-success);
  color: var(--pq-success);
}

.volatility--medium {
  background: var(--pq-warning);
  color: var(--pq-warning);
}

.volatility--high {
  background: var(--pq-danger);
  color: var(--pq-danger);
}

:deep(.q-table) {
  background: transparent;

  .q-table__top {
    padding: 0;
    border-bottom: 1px solid var(--pq-border);
  }

  .q-table__bottom {
    border-top: 1px solid var(--pq-border);
    padding: 0.5rem 1rem;
  }

  thead tr th {
    font-family: 'Geist', sans-serif;
    font-size: 0.7rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--pq-text-muted);
    padding: 0.75rem 1rem;
    border-bottom: 1px solid var(--pq-border);
  }

  tbody tr {
    border-bottom: 1px solid var(--pq-border);
  }
}
</style>
