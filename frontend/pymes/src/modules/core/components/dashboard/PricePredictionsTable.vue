<template>
  <div class="preds-card">
    <div class="preds-card__header">
      <div class="preds-card__accent" />
      <div class="preds-card__title-row">
        <div>
          <h3 class="preds-card__title">Predicción de Precios</h3>
          <p class="preds-card__subtitle">Regresión lineal con intervalo de confianza</p>
        </div>
        <q-input
          dark dense filled
          v-model="filter"
          placeholder="Buscar producto..."
          class="preds-card__search"
        >
          <template v-slot:prepend>
            <q-icon name="search" size="1.1rem" class="text-accent" />
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
      class="preds-card__table"
    >
      <template v-slot:body="props">
        <q-tr :props="props" class="preds-card__row" :style="{ animationDelay: `${props.rowIndex * 40}ms` }">
          <q-td key="productName" class="preds-card__cell preds-card__cell--product">
            <span class="preds-card__product-name">{{ props.row.productName }}</span>
          </q-td>
          <q-td key="lastPrice" class="preds-card__cell preds-card__cell--right">
            <span class="preds-card__price">{{ formatCurrency(props.row.lastPrice) }}</span>
          </q-td>
          <q-td key="predictedPrice" class="preds-card__cell preds-card__cell--right">
            <span class="preds-card__price preds-card__price--predicted">{{ formatCurrency(props.row.predictedPrice) }}</span>
          </q-td>
          <q-td key="pctChange" class="preds-card__cell preds-card__cell--right">
            <span
              class="preds-card__trend"
              :class="{
                'preds-card__trend--up': props.row.pctChange > 0,
                'preds-card__trend--down': props.row.pctChange < 0,
              }"
            >
              <q-icon
                :name="props.row.pctChange > 0 ? 'arrow_upward' : props.row.pctChange < 0 ? 'arrow_downward' : 'remove'"
                size="0.8rem"
              />
              {{ Math.abs(props.row.pctChange).toFixed(1) }}%
            </span>
          </q-td>
          <q-td key="confidence" class="preds-card__cell preds-card__cell--right">
            <div class="preds-card__confidence">
              <div class="preds-card__confidence-track">
                <div
                  class="preds-card__confidence-bar"
                  :class="confidenceClass(props.row.confidence)"
                  :style="{ width: `${props.row.confidence}%` }"
                />
              </div>
              <span class="preds-card__confidence-value">{{ props.row.confidence.toFixed(0) }}%</span>
            </div>
          </q-td>
          <q-td key="dataPoints" class="preds-card__cell preds-card__cell--right">
            <span class="preds-card__samples">{{ props.row.dataPoints }} pts</span>
          </q-td>
        </q-tr>
      </template>

      <template v-slot:no-data>
        <div class="preds-card__empty">
          <q-icon name="auto_graph" size="2.5rem" class="text-accent" />
          <p class="preds-card__empty-text">Sin datos de predicción para este período</p>
        </div>
      </template>
    </q-table>
  </div>
</template>

<script setup lang="ts">
import { shallowRef } from 'vue';
import type { PricePredictionItem } from '../../types/analytics';
import { formatCurrency } from 'src/utils/format';

defineProps<{ items: PricePredictionItem[]; loading?: boolean }>();

const columns = [
  { name: 'productName', label: 'Producto', field: 'productName', align: 'left' as const, sortable: true },
  { name: 'lastPrice', label: 'Último Precio', field: 'lastPrice', align: 'right' as const, sortable: true },
  { name: 'predictedPrice', label: 'Predicho', field: 'predictedPrice', align: 'right' as const, sortable: true },
  { name: 'pctChange', label: 'Cambio', field: 'pctChange', align: 'right' as const, sortable: true },
  { name: 'confidence', label: 'Confianza', field: 'confidence', align: 'right' as const, sortable: true },
  { name: 'dataPoints', label: 'Muestras', field: 'dataPoints', align: 'right' as const, sortable: true },
];

const pagination = shallowRef({ sortBy: 'pctChange', descending: true, page: 1, rowsPerPage: 10 });
const filter = shallowRef('');

function confidenceClass(conf: number): string {
  if (conf > 70) return 'confidence--high';
  if (conf > 40) return 'confidence--medium';
  return 'confidence--low';
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function filterFn(rows: readonly any[], terms: any) {
  const term = String(terms).toLowerCase();
  return rows.filter((r: PricePredictionItem) => r.productName.toLowerCase().includes(term));
}
</script>

<style scoped lang="scss">
.preds-card {
  background: rgba(27, 38, 36, 0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(113, 131, 127, 0.1);
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
    background: #C5A059;
    border-radius: 0 2px 2px 0;
  }

  &__title-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
  }

  &__title {
    font-family: 'Outfit', sans-serif;
    font-size: 1.1rem;
    font-weight: 600;
    color: #E2E8E4;
    margin: 0;
    line-height: 1.2;
  }

  &__subtitle {
    font-size: 0.75rem;
    color: #8A9E99;
    margin: 0.25rem 0 0;
  }

  &__search {
    max-width: 220px;
    flex-shrink: 0;
  }

  &__row {
    animation: predRowIn 0.3s ease forwards;
    opacity: 0;
    transition: background 0.2s ease;

    &:hover {
      background: rgba(163, 120, 94, 0.06) !important;
    }
  }

  &__cell {
    padding: 0.75rem 1rem !important;
    font-size: 0.85rem;
    color: #E2E8E4;

    &--right { text-align: right; }
    &--product { max-width: 200px; }
  }

  &__product-name {
    font-weight: 600;
    color: #E2E8E4;
  }

  &__price {
    font-family: 'Outfit', sans-serif;
    font-weight: 600;
    font-size: 0.9rem;

    &--predicted {
      color: #C5A059;
    }
  }

  &__trend {
    display: inline-flex;
    align-items: center;
    gap: 0.2rem;
    font-family: 'Outfit', sans-serif;
    font-size: 0.8rem;
    font-weight: 600;
    padding: 0.15rem 0.5rem;
    border-radius: 9999px;

    &--up {
      color: #8B4513;
      background: rgba(139, 69, 19, 0.12);
    }

    &--down {
      color: #2D5A27;
      background: rgba(45, 90, 39, 0.12);
    }
  }

  &__confidence {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 0.5rem;
  }

  &__confidence-track {
    width: 60px;
    height: 5px;
    background: rgba(113, 131, 127, 0.12);
    border-radius: 3px;
    overflow: hidden;
  }

  &__confidence-bar {
    height: 100%;
    border-radius: 3px;
    transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &__confidence-value {
    font-family: 'Outfit', sans-serif;
    font-size: 0.8rem;
    font-weight: 500;
    color: #8A9E99;
    min-width: 32px;
    text-align: right;
  }

  &__samples {
    font-family: 'Outfit', sans-serif;
    font-size: 0.75rem;
    color: #8A9E99;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 3rem 1rem;
    gap: 0.5rem;
  }

  &__empty-text {
    font-size: 0.85rem;
    color: #8A9E99;
    margin: 0;
  }
}

.confidence--high {
  background: linear-gradient(90deg, #2D5A27, #4a8a42);
}

.confidence--medium {
  background: linear-gradient(90deg, #C5A059, #d4b56e);
}

.confidence--low {
  background: linear-gradient(90deg, #8A9E99, #a0b4af);
}

@keyframes predRowIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:deep(.q-table) {
  background: transparent;

  .q-table__top {
    padding: 0;
    border-bottom: 1px solid rgba(113, 131, 127, 0.08);
  }

  .q-table__bottom {
    border-top: 1px solid rgba(113, 131, 127, 0.08);
    padding: 0.5rem 1rem;
  }

  thead tr th {
    font-size: 0.7rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: #8A9E99;
    padding: 0.75rem 1rem;
    border-bottom: 1px solid rgba(113, 131, 127, 0.1);
  }

  tbody tr {
    border-bottom: 1px solid rgba(113, 131, 127, 0.05);
  }
}
</style>
