<script setup lang="ts">
import { formatCurrency } from 'src/utils/format';
import type { CostoDiario, ConfigLaboral } from '../../types';

interface Props {
  diario: CostoDiario | null;
  costoFijoDiario: number;
  costoSalariosDiario: number;
  costoPrestamosDiario: number;
  costoOperativoDiario: number;
  gananciaPositiva: boolean;
  configForm: ConfigLaboral;
  configSaving: boolean;
}

interface Emits {
  (e: 'update:config', value: number): void;
  (e: 'save-config'): void;
}

defineProps<Props>();
defineEmits<Emits>();
</script>

<template>
  <q-card dark class="glass cost-summary q-mb-md" role="status" aria-live="polite">
    <div class="cost-summary__grid q-pa-sm">
      <div class="summary-item">
        <span class="summary-label">Costo fijo / día</span>
        <span class="summary-value font-mono">{{ diario ? formatCurrency(costoFijoDiario) : '—' }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">Salarios / día</span>
        <span class="summary-value font-mono">{{ diario ? formatCurrency(costoSalariosDiario) : '—' }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">Préstamos / día</span>
        <span class="summary-value font-mono">{{ diario ? formatCurrency(costoPrestamosDiario) : '—' }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">Total costo del día</span>
        <span class="summary-value summary-value--total font-mono">{{ diario ? formatCurrency(costoOperativoDiario) : '—' }}</span>
      </div>
      <div class="summary-arrow" aria-hidden="true"><q-icon name="arrow_forward" /></div>
      <div class="summary-item">
        <span class="summary-label">Ventas hoy</span>
        <span class="summary-value font-mono">{{ diario ? formatCurrency(diario.ventasHoy) : '—' }}</span>
      </div>
      <div class="summary-arrow" aria-hidden="true"><q-icon name="arrow_forward" /></div>
      <div class="summary-item">
        <span class="summary-label">Ganancia real estimada</span>
        <span class="summary-value font-mono" :class="gananciaPositiva ? 'text-positive' : 'text-negative'">{{ diario ? formatCurrency(diario.gananciaRealEstimada) : '—' }}</span>
      </div>
      <q-separator vertical dark class="cost-summary__sep q-mx-sm" />
      <div class="summary-item summary-item--config">
        <span class="summary-label">Días laborales / mes</span>
        <div class="row items-center q-gutter-x-xs">
          <q-input dark dense flat type="number" min="1" max="31" :model-value="configForm.diasLaborales" @update:model-value="(v) => $emit('update:config', Number(v))" @blur="$emit('save-config')" inputmode="numeric" style="max-width: 64px" class="config-inline-input" aria-label="Días laborales" />
          <q-btn flat dense round icon="sym_r_save" color="primary" size="sm" :loading="configSaving" @click="$emit('save-config')" aria-label="Guardar configuración" />
        </div>
      </div>
    </div>
  </q-card>
</template>

<style scoped lang="scss">
.cost-summary { position: sticky; top: 0; z-index: 10; border-radius: 8px; }
.cost-summary__grid { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; }
.summary-item { display: flex; flex-direction: column; gap: 2px; padding: 4px 8px; min-width: 0; }
.summary-label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em; color: rgba(163,120,94,0.55); }
.summary-value { font-size: 1.1rem; font-weight: 700; font-variant-numeric: tabular-nums; }
.summary-value--total { color: rgba(163,120,94,0.9); }
.summary-arrow { color: rgba(163,120,94,0.35); }
.config-inline-input :deep(.q-field__control) { min-height: 28px; padding: 0 4px; }
.config-inline-input :deep(.q-field__native) { font-size: 0.85rem; }

// ponytail: mobile ponytail — grid 2 cols tames wrap hell, hide arrows
@media (max-width: 600px) {
  .cost-summary { top: 48px; }
  .cost-summary__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
  .summary-arrow, .cost-summary__sep { display: none; }
  .summary-item--config { grid-column: 1 / -1; flex-direction: row; align-items: center; justify-content: space-between; }
}
</style>
