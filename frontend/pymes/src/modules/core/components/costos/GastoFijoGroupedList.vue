<script setup lang="ts">
import { computed } from 'vue';
import { formatCurrency } from 'src/utils/format';
import type { GastoFijoRecurrente } from '../../types';
import EmptyState from 'src/components/ui/EmptyState.vue';

interface CatGroup { categoria: string; items: readonly GastoFijoRecurrente[]; total: number; }
interface Props { groups: readonly CatGroup[]; loading: boolean; }
interface Emits { (e:'create'):void; (e:'edit', v:GastoFijoRecurrente):void; (e:'delete', v:GastoFijoRecurrente):void; }
const props = defineProps<Props>();
defineEmits<Emits>();

const totalCount = computed(() => props.groups.reduce((n, g) => n + g.items.length, 0));
const summaryLabel = computed(() => {
  const n = totalCount.value;
  const total = props.groups.reduce((sum, g) => sum + g.total, 0);
  return `${n} gasto${n !== 1 ? 's' : ''} · ${formatCurrency(total)}/mes`;
});
</script>

<template>
  <div>
    <div class="toolbar">
      <span v-if="!loading && groups.length" class="count-label">{{ summaryLabel }}</span>
      <q-btn v-if="groups.length" color="primary" label="Nuevo" no-caps @click="$emit('create')" class="toolbar-new" />
    </div>
    <div v-if="!loading && !groups.length" class="q-mt-lg">
      <EmptyState icon="sym_r_receipt" title="Sin gastos fijos" message="Agrega alquiler, internet, luz y otros gastos recurrentes.">
        <q-btn color="primary" label="Nuevo Gasto Fijo" no-caps @click="$emit('create')" class="q-mt-sm" />
      </EmptyState>
    </div>
    <div v-if="loading" class="q-gutter-y-md q-mt-md">
      <div v-for="n in 3" :key="n"><q-skeleton type="rect" dark animation="pulse" height="72px" /></div>
    </div>
    <div v-for="group in groups" :key="group.categoria" class="cat-group">
      <div class="cat-group__header">
        <span class="cat-group__title">{{ group.categoria }}</span>
        <span class="cat-group__count">{{ group.items.length }} gasto{{ group.items.length!==1?'s':'' }}</span>
        <q-space />
        <span class="cat-group__total">{{ formatCurrency(group.total) }}</span>
      </div>
      <div v-for="g in group.items" :key="g.id" class="row-item">
        <div class="row-item__main">
          <div class="row-item__title">{{ g.descripcion || g.categoria }}</div>
          <div class="row-item__meta">
            <span>Día {{ g.diaEjecucion }}</span>
            <span v-if="g.metodoPago" class="row-item__sep">·</span><span v-if="g.metodoPago">{{ g.metodoPago }}</span>
            <span v-if="g.proveedorName" class="row-item__sep">·</span><span v-if="g.proveedorName">{{ g.proveedorName }}</span>
          </div>
        </div>
        <div class="row-item__amount">{{ formatCurrency(g.monto) }}</div>
        <div class="row-item__actions">
          <q-btn flat dense round icon="sym_r_edit" color="primary" size="sm" aria-label="Editar" @click="$emit('edit', g)" />
          <q-btn flat dense round icon="sym_r_delete" color="negative" size="sm" aria-label="Eliminar" @click="$emit('delete', g)" />
        </div>
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

@media (max-width: 639px) {
  .toolbar { display: grid; grid-template-columns: 1fr; gap: 8px; }
  .toolbar .q-btn { width: 100%; white-space: nowrap; min-height: 44px; }
}

.cat-group { margin-bottom: 20px; }
.cat-group:last-child { margin-bottom: 0; }

.cat-group__header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 8px 12px;
  background: var(--pq-elevated);
  border-radius: var(--pq-radius-sm);
  margin-bottom: 4px;
  flex-wrap: wrap;
  border-bottom: 1px solid var(--pq-border);
}

.cat-group__title { font-size: 0.85rem; font-weight: 600; color: var(--pq-text); }
.cat-group__count { font-size: 0.72rem; color: var(--pq-text-subtle); }
.cat-group__total {
  font-family: var(--pq-font-utility);
  font-size: 0.9rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-accent);
}

.row-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--pq-border);
}

.row-item:hover { background: var(--pq-elevated); }

.row-item__title { font-size: 0.85rem; color: var(--pq-text); }
.row-item__meta { font-size: 0.75rem; color: var(--pq-text-subtle); margin-top: 2px; }
.row-item__sep { margin: 0 4px; }

.row-item__amount {
  font-family: var(--pq-font-utility);
  font-size: 0.9rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-text);
}

@media (max-width: 639px) {
  .row-item { grid-template-columns: 1fr; gap: 6px; padding: 12px; }
  .row-item__actions { display: flex; gap: 8px; justify-content: flex-end; }
  .row-item__actions :deep(.q-btn) { min-width: 44px; min-height: 44px; }
}
</style>
