<script setup lang="ts">
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
defineProps<Props>();
defineEmits<Emits>();
</script>

<template>
  <div>
    <div class="toolbar">
      <q-space />
      <q-btn v-if="items.length" color="primary" icon="sym_r_add" label="Nuevo" @click="$emit('create')" />
    </div>
    <div v-if="!loading && !items.length" class="q-mt-lg">
      <EmptyState icon="sym_r_groups" title="Sin equipo" message="Registra a tu equipo para calcular el costo de salarios.">
        <q-btn color="primary" icon="sym_r_add" label="Nuevo Integrante" @click="$emit('create')" class="q-mt-sm" />
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
.toolbar { display:flex; align-items:center; flex-wrap:wrap; margin-bottom:16px; }
.row-item { display:grid; grid-template-columns: 1fr auto auto; align-items:center; gap:12px; padding:10px 12px; border-bottom:1px solid rgba(113,131,127,0.04); }
.row-item:hover { background: rgba(27,38,36,0.3); }
.row-item__title { font-size:0.85rem; }
.row-item__meta { font-size:0.75rem; color:rgba(163,120,94,0.5); margin-top:2px; }
.row-item__amount { font-family:'JetBrains Mono','SF Mono','Fira Code',monospace; font-size:0.9rem; font-weight:600; font-variant-numeric:tabular-nums; }
@media (max-width:600px){
  .row-item{ grid-template-columns: 1fr; gap:6px; padding:12px; }
  .row-item__amount{ order:1; }
  .row-item__actions{ display:flex; gap:8px; justify-content:flex-end; }
  .row-item__actions :deep(.q-btn){ min-width:44px; min-height:44px; }
}
</style>
