<script setup lang="ts">
import { formatCurrency } from 'src/utils/format';
import type { VentaDiaria } from '../../types';
import type { DayGroup } from '../../composables/useVentas';

defineProps<{
  groups: DayGroup[];
  canEdit: boolean;
}>();

defineEmits<{
  edit: [venta: VentaDiaria];
  delete: [venta: VentaDiaria];
}>();
</script>

<template>
  <div>
    <q-expansion-item
      v-for="group in groups"
      :key="group.date"
      default-opened
      expand-separator
      class="day-group"
      header-class="day-group__header"
    >
      <template #header>
        <q-item-section>
          <span class="day-group__label">{{ group.label }}</span>
        </q-item-section>
        <q-item-section side>
          <span class="day-group__total">{{ formatCurrency(group.total) }}</span>
        </q-item-section>
      </template>

      <div v-for="v in group.items" :key="v.id" class="sale-row">
        <div class="sale-row__desc">{{ v.descripcion || 'Sin descripción' }}</div>
        <div class="sale-row__amount">{{ formatCurrency(v.montoBruto) }}</div>
        <div v-if="canEdit" class="sale-row__actions">
          <q-btn
            flat
            dense
            round
            icon="sym_r_edit"
            color="primary"
            size="md"
            aria-label="Editar"
            @click="$emit('edit', v)"
          />
          <q-btn
            flat
            dense
            round
            icon="sym_r_delete"
            color="negative"
            size="md"
            aria-label="Eliminar"
            @click="$emit('delete', v)"
          />
        </div>
      </div>
    </q-expansion-item>
  </div>
</template>

<style scoped>
.day-group {
  margin-bottom: 12px;
  border-radius: 6px;
  background: rgba(27, 38, 36, 0.3);
}

.day-group__label {
  font-size: 0.9rem;
  font-weight: 600;
  text-transform: capitalize;
}

.day-group__total {
  font-family: var(--pq-font-utility);
  font-weight: 700;
  color: var(--pq-accent);
}

.sale-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-top: 1px solid rgba(113, 131, 127, 0.12);
  min-height: 56px;
}

.sale-row__desc {
  font-size: 0.9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sale-row__amount {
  font-family: var(--pq-font-utility);
  font-weight: 600;
}

.sale-row__actions {
  display: flex;
  gap: 4px;
}
</style>
