<script setup lang="ts">
import { formatCurrency } from 'src/utils/format';
import type { VentaDiaria } from '../../types';
import { labelMetodoPago } from '../../utils/metodosPago';
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
            aria-label="Editar venta"
            @click="$emit('edit', v)"
          />
          <q-btn
            flat
            dense
            round
            icon="sym_r_delete"
            color="negative"
            size="md"
            aria-label="Eliminar venta"
            @click="$emit('delete', v)"
          />
        </div>
      </div>
      <div v-if="group.byMethod && Object.keys(group.byMethod).length" class="day-breakdown">
        <span v-for="(amt, metodo) in group.byMethod" :key="metodo" class="day-breakdown__chip">
          {{ labelMetodoPago(metodo as any) }}: {{ formatCurrency(amt as number) }}
        </span>
      </div>
    </q-expansion-item>
  </div>
</template>

<style scoped>
/* Grupo de día — solo hairlines, sin card background (brief Mayor) */
.day-group {
  margin-bottom: 16px;
}

.day-group :deep(.q-item) {
  min-height: 48px;
  border-top: 1px solid var(--pq-border);
}

.day-group :deep(.q-expansion-item__content) {
  border-bottom: 1px solid var(--pq-border);
}

.day-group :deep(.q-item__section--main) {
  color: var(--pq-text-muted);
}

.day-group :deep(.day-group__header:hover) {
  background: var(--pq-surface);
}

.day-group__label {
  font-family: var(--pq-font-display);
  font-size: 0.9rem;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--pq-text);
  text-transform: capitalize;
}

.day-group__total {
  font-family: var(--pq-font-utility);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-accent);
}

.sale-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-top: 1px solid rgba(77, 112, 99, 0.2);
  min-height: 56px;
  transition: background 0.16s cubic-bezier(0.22, 1, 0.36, 1);
}

.sale-row:hover {
  background: var(--pq-surface);
}

.sale-row__desc {
  font-family: var(--pq-font-body);
  font-size: 0.9rem;
  color: var(--pq-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sale-row__amount {
  font-family: var(--pq-font-utility);
  font-size: 0.9rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-text);
}

.sale-row__actions {
  display: flex;
  gap: 4px;
}

/* mobile: descripción arriba, monto + acciones abajo (sin apretar a 320px) */
@media (max-width: 639px) {
  .sale-row {
    grid-template-columns: 1fr auto;
    grid-template-areas: 'desc desc' 'amount actions';
    gap: 6px;
    padding: 12px;
  }
  .sale-row__desc {
    grid-area: desc;
  }
  .sale-row__amount {
    grid-area: amount;
    align-self: center;
  }
  .sale-row__actions {
    grid-area: actions;
    justify-content: flex-end;
  }
  .sale-row__actions :deep(.q-btn) {
    min-width: 44px;
    min-height: 44px;
  }
}

/* tablet táctil: escala táctil 44px */
@media (pointer: coarse) {
  .sale-row__actions :deep(.q-btn) {
    min-width: 44px;
    min-height: 44px;
  }
}

.day-breakdown {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 12px 12px;
  border-top: 1px solid rgba(77, 112, 99, 0.15);
}

.day-breakdown__chip {
  font-family: var(--pq-font-utility);
  font-size: 11px;
  padding: 3px 8px;
  border-radius: var(--pq-radius-sm);
  background: rgba(163, 120, 94, 0.1);
  border: 1px solid rgba(163, 120, 94, 0.25);
  color: var(--pq-accent);
  white-space: nowrap;
}
</style>
