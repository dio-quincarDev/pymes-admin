<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';

interface Props {
  title: string;
  subtitle: string;
  period: string;
  loading?: boolean;
}

defineProps<Props>();
defineEmits<{
  'update:period': [value: string];
  recalculate: [];
}>();

const authStore = useAuthStore();
const tenantName = computed(() => authStore.tenantName);

const periodOptions = computed(() => {
  const options: { label: string; value: string }[] = [];
  const now = new Date();
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    const label = d.toLocaleDateString('es-PE', { year: 'numeric', month: 'long' });
    options.push({ label, value });
  }
  return options;
});
</script>

<template>
  <div class="analytics-header" role="banner" aria-label="Encabezado de análisis">
    <div class="analytics-header__text">
      <h1 class="analytics-header__title">{{ tenantName ? `${tenantName} · ${title}` : title }}</h1>
      <p class="analytics-header__subtitle">{{ subtitle }}</p>
    </div>
    <div class="analytics-header__actions">
      <q-select
        :model-value="period"
        :options="periodOptions"
        dense
        dark
        standout
        emit-value
        map-options
        class="analytics-header__select"
        aria-label="Seleccionar período"
        @update:model-value="$emit('update:period', $event)"
      />
      <q-btn
        flat
        round
        dense
        icon="refresh"
        aria-label="Recalcular datos"
        :loading="loading"
        class="analytics-header__recalc"
        @click="$emit('recalculate')"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.analytics-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;

  &__text {
    min-width: 0;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-size: 24px;
    font-weight: 700;
    color: var(--pq-text);
    margin: 0;
    line-height: 1.1;
  }

  &__subtitle {
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    font-weight: 400;
    color: var(--pq-text-muted);
    margin: 4px 0 0;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  &__select {
    min-width: 160px;
  }

  &__recalc {
    color: var(--pq-text-muted);

    &:hover {
      color: var(--pq-accent);
    }
  }
}

@media (max-width: 639px) {
  .analytics-header {
    flex-direction: column;
    gap: 12px;

    &__select {
      min-width: 140px;
    }
  }
}
</style>
