<script setup lang="ts">
import { computed } from 'vue';
import { formatCurrency } from 'src/utils/format';
import type { Collaborador } from '../../types';
import EmptyState from 'src/components/ui/EmptyState.vue';

interface Props {
  items: readonly Collaborador[];
  loading: boolean;
}
interface Emits {
  (e: 'create'): void;
  (e: 'edit', value: Collaborador): void;
  (e: 'delete', value: Collaborador): void;
}
const props = defineProps<Props>();
defineEmits<Emits>();

const countLabel = computed(() =>
  `${props.items.length} integrante${props.items.length !== 1 ? 's' : ''}`,
);
</script>

<template>
  <div>
    <div class="toolbar">
      <span v-if="!loading && items.length" class="count-label">{{ countLabel }}</span>
      <q-btn v-if="items.length" color="primary" label="Nuevo" no-caps @click="$emit('create')" class="toolbar-new" />
    </div>
    <div v-if="!loading && !items.length" class="q-mt-lg">
      <EmptyState icon="sym_r_groups" title="Sin equipo" message="Registra a tu equipo para calcular el costo de salarios.">
        <q-btn color="primary" label="Nuevo Integrante" no-caps @click="$emit('create')" class="q-mt-sm" />
      </EmptyState>
    </div>
    <div v-if="loading" class="q-gutter-y-md q-mt-md">
      <div v-for="n in 3" :key="n"><q-skeleton type="rect" dark animation="pulse" height="72px" /></div>
    </div>
    <div v-for="c in items" :key="c.id" class="row-item">
      <div class="row-item__main">
        <div class="row-item__title">{{ c.nombre }}</div>
        <div class="row-item__meta"><span>{{ c.tipoPago }}</span></div>
      </div>
      <div class="row-item__amount">{{ formatCurrency(c.monto) }}</div>
      <div class="row-item__actions">
        <q-btn flat dense round icon="sym_r_edit" color="primary" size="sm" aria-label="Editar" @click="$emit('edit', c)" />
        <q-btn flat dense round icon="sym_r_delete" color="negative" size="sm" aria-label="Eliminar" @click="$emit('delete', c)" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }

.count-label {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
  text-transform: uppercase;
}

/* Nuevo button — filled cobre (mismo patrón que Proveedores) */
.toolbar-new {
  min-height: 36px;
  background: var(--pq-accent) !important;
  color: var(--pq-background) !important;
  font-weight: 600;
  border-radius: var(--pq-radius-sm);
  box-shadow: 0 2px 8px rgba(163, 120, 94, 0.25);
  transition: background-color 0.16s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.16s ease, transform 0.1s ease;
}

.toolbar-new:hover {
  background: var(--pq-accent-hover) !important;
  box-shadow: 0 4px 16px rgba(163, 120, 94, 0.35);
  transform: translateY(-1px);
}

.toolbar-new:active { transform: scale(0.985) translateY(0); }

.toolbar-new:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}

/* ponytail: sin search — izquierda en desktop, full-width en xs */
@media (max-width: 639px) {
  .toolbar { display: grid; grid-template-columns: 1fr; gap: 8px; }
  .toolbar .q-btn { width: 100%; white-space: nowrap; min-height: 44px; }
}

.row-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--pq-border);
}

.row-item:hover { background: rgba(27, 44, 38, 0.3); }

.row-item__title { font-size: 0.85rem; color: var(--pq-text); }

.row-item__meta {
  font-size: 0.75rem;
  color: var(--pq-text-subtle);
  margin-top: 2px;
}

.row-item__amount {
  font-family: var(--pq-font-utility);
  font-size: 0.9rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-text);
}

@media (max-width: 639px) {
  .row-item { grid-template-columns: 1fr; gap: 6px; padding: 12px; }
  .row-item__amount { order: 1; }
  .row-item__actions { display: flex; gap: 8px; justify-content: flex-end; }
  .row-item__actions :deep(.q-btn) { min-width: 44px; min-height: 44px; }
}
</style>
