<script setup lang="ts">
import { formatCurrency } from 'src/utils/format';
import type { GastoFijoRecurrente } from '../../types';
import EmptyState from 'src/components/ui/EmptyState.vue';

interface CatGroup { categoria: string; items: readonly GastoFijoRecurrente[]; total: number; }
interface Props { groups: readonly CatGroup[]; loading: boolean; }
interface Emits { (e:'create'):void; (e:'edit', v:GastoFijoRecurrente):void; (e:'delete', v:GastoFijoRecurrente):void; }
defineProps<Props>();
defineEmits<Emits>();
</script>

<template>
  <div>
    <div class="toolbar">
      <q-space />
      <q-btn v-if="groups.length" color="primary" icon="sym_r_add" label="Nuevo" @click="$emit('create')" />
    </div>
    <div v-if="!loading && !groups.length" class="q-mt-lg">
      <EmptyState icon="sym_r_receipt" title="Sin gastos fijos" message="Agrega alquiler, internet, luz y otros gastos recurrentes.">
        <q-btn color="primary" icon="sym_r_add" label="Nuevo Gasto Fijo" @click="$emit('create')" class="q-mt-sm" />
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
.toolbar{display:flex;align-items:center;flex-wrap:wrap;margin-bottom:16px;}
.cat-group{margin-bottom:20px;} .cat-group:last-child{margin-bottom:0;}
.cat-group__header{display:flex;align-items:baseline;gap:10px;padding:8px 12px;background:rgba(27,38,36,0.3);border-radius:6px;margin-bottom:4px;flex-wrap:wrap;}
.cat-group__title{font-size:0.85rem;font-weight:600;} .cat-group__count{font-size:0.72rem;color:rgba(163,120,94,0.45);}
.cat-group__total{font-family:'JetBrains Mono','SF Mono',monospace;font-size:0.9rem;font-weight:600;font-variant-numeric:tabular-nums;color:rgba(163,120,94,0.7);}
.row-item{display:grid;grid-template-columns:1fr auto auto;align-items:center;gap:12px;padding:10px 12px;border-bottom:1px solid rgba(113,131,127,0.04);}
.row-item:hover{background:rgba(27,38,36,0.3);} .row-item__title{font-size:0.85rem;} .row-item__meta{font-size:0.75rem;color:rgba(163,120,94,0.5);margin-top:2px;} .row-item__sep{margin:0 4px;}
.row-item__amount{font-family:'JetBrains Mono',monospace;font-size:0.9rem;font-weight:600;font-variant-numeric:tabular-nums;}
@media (max-width:600px){
  .row-item{grid-template-columns:1fr;gap:6px;padding:12px;}
  .row-item__actions{display:flex;gap:8px;justify-content:flex-end;}
  .row-item__actions :deep(.q-btn){min-width:44px;min-height:44px;}
}
</style>
