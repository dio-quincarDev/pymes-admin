<script setup lang="ts">
import { formatCurrency } from 'src/utils/format';

defineProps<{
  total: number;
  count: number;
  average: number;
}>();
</script>

<template>
  <q-card dark class="glass q-pa-sm q-mb-sm">
    <div class="row q-gutter-x-lg q-pa-xs">
      <div>
        <div class="stat-label">Total mes</div>
        <div class="font-mono text-weight-bold text-h6">{{ formatCurrency(total) }}</div>
      </div>
      <div>
        <div class="stat-label">Registros</div>
        <div class="font-mono text-weight-bold text-h6">{{ count }}</div>
      </div>
      <div>
        <div class="stat-label">Venta promedio</div>
        <div class="font-mono text-weight-bold text-h6">{{ formatCurrency(average) }}</div>
        <div class="stat-sub">por día, este mes</div>
      </div>
    </div>
  </q-card>
</template>

<style scoped>
.stat-label {
  font-size: 0.72rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--pq-text-muted);
}

.stat-sub {
  font-size: 0.7rem;
  color: var(--pq-text-subtle);
}
</style>
