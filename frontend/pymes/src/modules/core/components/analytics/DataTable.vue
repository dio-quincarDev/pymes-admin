<script setup lang="ts">
import { ref } from 'vue';

interface Column {
  name: string;
  label: string;
  field: string | ((row: Record<string, unknown>) => unknown);
  align?: 'left' | 'center' | 'right';
  sortable?: boolean;
  format?: (val: unknown, row: Record<string, unknown>) => string;
  sort?: (a: unknown, b: unknown) => number;
  classes?: string;
}

interface Props {
  rows: Record<string, unknown>[];
  columns: Column[];
  rowKey: string;
  loading?: boolean;
  filter?: string;
  variant?: 'default' | 'compact';
  title?: string;
}

withDefaults(defineProps<Props>(), {
  loading: false,
  filter: '',
  variant: 'default',
  title: '',
});

const pagination = ref({
  sortBy: '',
  descending: false,
  page: 1,
  rowsPerPage: 15,
});
</script>

<template>
  <div class="data-table" :class="`data-table--${variant}`">
    <h3 v-if="title" class="data-table__title">{{ title }}</h3>
    <q-table
      dark
      flat
      :rows="rows"
      :columns="columns"
      :row-key="rowKey"
      :loading="loading"
      :filter="filter"
      v-model:pagination="pagination"
      :rows-per-page-options="[10, 15, 25]"
      class="data-table__qtable"
      :aria-label="title || 'Tabla de datos'"
    >
      <template v-slot:no-data>
        <div class="data-table__empty">
          <q-icon name="inbox" size="2rem" style="color: var(--pq-text-subtle)" />
          <p>Sin datos disponibles</p>
        </div>
      </template>
    </q-table>
  </div>
</template>

<style scoped lang="scss">
.data-table {
  width: 100%;

  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin: 0 0 12px;
  }

  &__qtable {
    :deep(.q-table) {
      background: transparent;

      thead tr th {
        font-family: 'Satoshi', sans-serif;
        font-size: 10px;
        font-weight: 500;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        color: var(--pq-text-subtle);
        padding: 8px 12px;
        border-bottom: 1px solid rgba(107, 104, 99, 0.1);
      }

      tbody tr {
        border-bottom: 1px solid rgba(107, 104, 99, 0.05);
        transition: background var(--pq-motion-fast);

        &:hover {
          background: var(--pq-elevated) !important;
        }
      }

      tbody td {
        font-family: 'Satoshi', sans-serif;
        font-size: 13px;
        font-weight: 400;
        color: var(--pq-text);
        padding: 8px 12px;
      }

      .q-table__bottom {
        font-family: 'Satoshi', sans-serif;
        font-size: 12px;
        color: var(--pq-text-muted);
        border-top: 1px solid rgba(107, 104, 99, 0.05);
      }
    }
  }

  &--compact {
    :deep(.q-table) {
      thead tr th {
        padding: 6px 8px;
      }

      tbody td {
        padding: 6px 8px;
        font-size: 12px;
      }
    }
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 32px 16px;
    gap: 8px;

    p {
      font-family: 'Satoshi', sans-serif;
      font-size: 13px;
      color: var(--pq-text-muted);
      margin: 0;
    }
  }
}
</style>
