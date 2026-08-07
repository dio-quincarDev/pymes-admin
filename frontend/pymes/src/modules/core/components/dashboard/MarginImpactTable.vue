<template>
  <q-table
    :rows="items"
    :columns="columns"
    row-key="productId"
    flat
    dense
    dark
    :rows-per-page-options="[5, 10]"
    class="margin-table"
    :filter="filter"
  >
    <template #top-right>
      <q-input
        v-model="filter"
        dense
        dark
        standout
        placeholder="Buscar..."
        class="margin-table__search"
      >
        <template #prepend>
          <q-icon name="search" size="xs" />
        </template>
      </q-input>
    </template>
    <template #body-cell-change="props">
      <q-td :props="props">
        <span :class="props.value >= 0 ? 'text-positive' : 'text-negative'">
          {{ props.value >= 0 ? '+' : '' }}{{ props.value.toFixed(1) }}%
        </span>
      </q-td>
    </template>
  </q-table>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { MarginItem } from '../../types/analytics';
import { useNumberFormat } from '../../composables/useNumberFormat';

defineProps<{ items: MarginItem[] }>();

const { formatCurrency } = useNumberFormat();
const filter = ref('');

const columns = [
  {
    name: 'productName',
    required: true,
    label: 'Producto',
    field: 'productName',
    align: 'left' as const,
    sortable: true,
  },
  {
    name: 'currentPrice',
    label: 'Precio Actual',
    field: 'currentPrice',
    align: 'right' as const,
    sortable: true,
    format: (val: number) => formatCurrency(val),
  },
  {
    name: 'previousPrice',
    label: 'Anterior',
    field: 'previousPrice',
    align: 'right' as const,
    sortable: true,
    format: (val: number) => formatCurrency(val),
  },
  {
    name: 'pctChange',
    label: '% Cambio',
    field: 'pctChange',
    align: 'right' as const,
    sortable: true,
  },
];
</script>

<style scoped lang="scss">
.margin-table {
  background: transparent;

  &__search {
    width: 200px;
  }
}
</style>
