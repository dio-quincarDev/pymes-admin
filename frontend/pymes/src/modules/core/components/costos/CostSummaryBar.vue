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
  <div class="q-mb-md">
    <q-card dark class="cost-summary" role="status" aria-live="polite">
      <div class="cost-summary__grid q-pa-sm">
        <div class="summary-item">
          <span class="summary-label">Costo fijo / día</span>
          <span class="summary-value font-mono">{{ diario ? formatCurrency(costoFijoDiario) : '—' }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">Salarios / día</span>
          <span class="summary-value font-mono">{{ diario ? formatCurrency(costoSalariosDiario) : '—' }}</span>
        </div>
        <div v-if="costoPrestamosDiario > 0" class="summary-item">
          <span class="summary-label">Préstamos / día</span>
          <span class="summary-value font-mono">{{ diario ? formatCurrency(costoPrestamosDiario) : '—' }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">Total costo del día</span>
          <span class="summary-value summary-value--total font-mono">{{ diario ? formatCurrency(costoOperativoDiario) : '—' }}</span>
        </div>
        <div class="summary-arrow" aria-hidden="true"><q-icon name="arrow_forward" /></div>
        <div class="summary-item summary-item--payoff">
          <span class="summary-label">Ventas hoy</span>
          <span class="summary-value summary-value--payoff font-mono">{{ diario ? formatCurrency(diario.ventasHoy) : '—' }}</span>
        </div>
        <div class="summary-arrow" aria-hidden="true"><q-icon name="arrow_forward" /></div>
        <div class="summary-item summary-item--payoff">
          <span class="summary-label">Ganancia real estimada</span>
          <span class="summary-value summary-value--payoff font-mono" :class="gananciaPositiva ? 'text-positive' : 'text-negative'">{{ diario ? formatCurrency(diario.gananciaRealEstimada) : '—' }}</span>
        </div>
      </div>
    </q-card>
    <div class="config-line">
      <span class="summary-label">Días laborales / mes</span>
      <div class="row items-center q-gutter-x-xs">
        <q-input dark dense flat type="number" min="1" max="31" :model-value="configForm.diasLaborales" @update:model-value="(v) => $emit('update:config', Number(v))" @blur="$emit('save-config')" inputmode="numeric" style="max-width: 64px" class="config-inline-input" aria-label="Días laborales" />
        <q-btn flat dense round icon="sym_r_save" color="primary" size="sm" :loading="configSaving" @click="$emit('save-config')" aria-label="Guardar configuración" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.cost-summary {
  position: sticky;
  top: 0;
  z-index: 10;
  border-radius: var(--pq-radius-md);
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  box-shadow: var(--pq-shadow-subtle);
}

.cost-summary__grid {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 8px;
  min-width: 0;
}

.summary-label {
  font-family: var(--pq-font-utility);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
}

.summary-value {
  font-family: var(--pq-font-utility);
  font-size: 1.1rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-text);
}

.summary-value--total { color: var(--pq-accent); }

.summary-arrow { color: var(--pq-text-muted); }

.config-inline-input :deep(.q-field__control) { min-height: 28px; padding: 0 4px; }
.config-inline-input :deep(.q-field__native) { font-size: 0.85rem; }

.config-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px 0;
}

/* mobile: grid 2 cols, hide arrows, ventas + ganancia primero y más grandes */
@media (max-width: 639px) {
  .cost-summary { top: 48px; }
  .cost-summary__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
  .summary-arrow { display: none; }
  .summary-item--payoff { order: -1; }
  .summary-value--payoff { font-size: 1.35rem; }
}
</style>
