<script setup lang="ts">
import { computed, shallowRef } from 'vue';
import { useNumberFormat } from '../../composables/useNumberFormat';
import type { SupplierRecommendationItem } from '../../types/analytics';

interface Props {
  items: SupplierRecommendationItem[];
}

const props = defineProps<Props>();
const { formatCurrency } = useNumberFormat();

// ponytail: filtro UI puro — esconde recomendaciones engañosas por compra mínima (ej 1kg a 2.25 vs 10kg a 2.20 = 2% → oculto con >5%); sin backend
const onlySignificant = shallowRef(false);
const onlyMultiSupplier = shallowRef(false);

const filteredItems = computed(() => {
  let list = props.items;
  if (onlySignificant.value) list = list.filter((r) => r.savingsPct > 5);
  if (onlyMultiSupplier.value) list = list.filter((r) => r.supplierCount > 2);
  return list;
});

function clearFilters() {
  onlySignificant.value = false;
  onlyMultiSupplier.value = false;
}
</script>

<template>
  <div class="recs-panel">
    <div class="recs-panel__header">
      <div class="recs-panel__accent" />
      <div class="recs-panel__header-content">
        <h3 class="recs-panel__title">Recomendaciones por Proveedor</h3>
        <p class="recs-panel__subtitle">Proveedores más económicos por producto</p>
      </div>
      <q-badge v-if="items.length" color="positive" rounded class="recs-panel__count">
        {{ items.length }}
      </q-badge>
    </div>

    <div v-if="items.length" class="recs-panel__filters">
      <q-chip
        :color="onlySignificant ? 'positive' : 'dark'"
        :text-color="onlySignificant ? 'white' : 'grey-5'"
        clickable
        dense
        :outline="!onlySignificant"
        icon="savings"
        @click="onlySignificant = !onlySignificant"
      >
        Ahorro &gt;5%
      </q-chip>
      <q-chip
        :color="onlyMultiSupplier ? 'positive' : 'dark'"
        :text-color="onlyMultiSupplier ? 'white' : 'grey-5'"
        clickable
        dense
        :outline="!onlyMultiSupplier"
        icon="verified"
        @click="onlyMultiSupplier = !onlyMultiSupplier"
      >
        Probados (3+ prov.)
      </q-chip>
      <q-btn
        v-if="onlySignificant || onlyMultiSupplier"
        flat
        dense
        size="sm"
        label="Limpiar"
        class="recs-panel__clear"
        @click="clearFilters"
      />
    </div>

    <div v-if="!items.length" class="recs-panel__empty">
      <q-icon name="verified" size="2.5rem" class="recs-panel__empty-icon" />
      <p class="recs-panel__empty-text">Sin recomendaciones disponibles</p>
      <p class="recs-panel__empty-hint">Se necesitan datos de al menos 2 proveedores por producto</p>
    </div>

    <div v-else-if="!filteredItems.length" class="recs-panel__empty">
      <q-icon name="filter_alt_off" size="2rem" class="recs-panel__empty-icon" />
      <p class="recs-panel__empty-text">Ninguna coincide con el filtro</p>
      <q-btn flat dense size="sm" color="positive" label="Limpiar filtros" @click="clearFilters" />
    </div>

    <div v-else class="recs-panel__list">
      <div
        v-for="(rec, idx) in filteredItems"
        :key="rec.productId"
        class="recs-panel__item"
        :style="{ animationDelay: `${idx * 50}ms` }"
      >
        <div class="recs-panel__item-accent" />
        <div class="recs-panel__item-content">
          <div class="recs-panel__item-header">
            <span class="recs-panel__product">{{ rec.productName }}</span>
            <div class="recs-panel__badges">
              <q-badge
                v-if="rec.supplierCount > 1"
                :class="{ 'recs-panel__badge--hot': rec.savingsPct > 15 }"
                color="positive"
                rounded
                :label="`${rec.savingsPct.toFixed(0)}%`"
              />
              <q-badge v-else label="único" color="grey-7" rounded />
            </div>
          </div>
          <div class="recs-panel__item-body">
            <div class="recs-panel__provider">
              <q-icon name="local_shipping" size="0.85rem" class="recs-panel__provider-icon" />
              <span class="recs-panel__provider-name">{{ rec.recommendedProviderName }}</span>
            </div>
            <div class="recs-panel__pricing">
              <span class="recs-panel__price">{{ formatCurrency(rec.recommendedPrice) }}</span>
              <span v-if="rec.supplierCount > 1" class="recs-panel__savings">
                ahorro {{ formatCurrency(rec.savingsPerUnit) }}/u
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.recs-panel {
  background: rgba(27, 38, 36, 0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(113, 131, 127, 0.1);
  border-radius: 8px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 1.25rem 1.5rem;
    position: relative;
  }

  &__accent {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: #2d5a27;
    border-radius: 0 2px 2px 0;
  }

  &__header-content {
    flex: 1;
  }

  &__title {
    font-family: 'Outfit', sans-serif;
    font-size: 1.1rem;
    font-weight: 600;
    color: #e2e8e4;
    margin: 0;
    line-height: 1.2;
  }

  &__subtitle {
    font-size: 0.75rem;
    color: #8a9e99;
    margin: 0.25rem 0 0;
  }

  &__count {
    font-family: 'Outfit', sans-serif;
    font-size: 0.8rem;
    font-weight: 600;
    padding: 0.25rem 0.6rem;
    margin-top: 0.15rem;
  }

  &__filters {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 1.5rem 0.75rem;
    flex-wrap: wrap;
  }

  &__clear {
    color: #8a9e99;
    font-size: 0.75rem;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 2.5rem 1rem;
    gap: 0.25rem;
  }

  &__empty-icon {
    color: rgba(45, 90, 39, 0.6);
    margin-bottom: 0.5rem;
  }

  &__empty-text {
    font-size: 0.9rem;
    color: #e2e8e4;
    font-weight: 500;
    margin: 0;
  }

  &__empty-hint {
    font-size: 0.75rem;
    color: #8a9e99;
    margin: 0;
  }

  &__list {
    max-height: 420px;
    overflow-y: auto;
    padding: 0 0.75rem 0.75rem;

    &::-webkit-scrollbar {
      width: 4px;
    }
    &::-webkit-scrollbar-track {
      background: transparent;
    }
    &::-webkit-scrollbar-thumb {
      background: rgba(163, 120, 94, 0.3);
      border-radius: 2px;
    }
  }

  &__item {
    display: flex;
    border-radius: 6px;
    overflow: hidden;
    margin-bottom: 0.35rem;
    animation: recSlideIn 0.3s ease forwards;
    opacity: 0;
    transition: background 0.2s ease;

    &:hover {
      background: rgba(163, 120, 94, 0.06);
    }

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__item-accent {
    width: 2px;
    background: rgba(45, 90, 39, 0.4);
    flex-shrink: 0;
    transition: background 0.2s ease;
  }

  &__item:hover &__item-accent {
    background: #2d5a27;
  }

  &__item-content {
    flex: 1;
    padding: 0.7rem 0.75rem;
  }

  &__item-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.3rem;
  }

  &__product {
    font-size: 0.85rem;
    font-weight: 600;
    color: #e2e8e4;
  }

  &__badges {
    flex-shrink: 0;
  }

  &__badge--hot {
    animation: badgePulse 2s ease-in-out infinite;
  }

  &__item-body {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
  }

  &__provider {
    display: flex;
    align-items: center;
    gap: 0.35rem;
  }

  &__provider-icon {
    color: #a3785e;
  }

  &__provider-name {
    font-size: 0.75rem;
    color: #8a9e99;
  }

  &__pricing {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
  }

  &__price {
    font-family: 'Outfit', sans-serif;
    font-size: 0.95rem;
    font-weight: 700;
    color: #e2e8e4;
  }

  &__savings {
    font-size: 0.7rem;
    color: #2d5a27;
    font-weight: 500;
  }
}

@keyframes recSlideIn {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes badgePulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(45, 90, 39, 0.4);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(45, 90, 39, 0);
  }
}
</style>
