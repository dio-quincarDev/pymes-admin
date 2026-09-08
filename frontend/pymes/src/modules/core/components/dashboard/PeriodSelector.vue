<template>
  <div class="period-selector row items-center q-gutter-sm">
    <q-select
      :model-value="modelValue"
      :options="periodOptions"
      dense
      dark
      standout
      emit-value
      map-options
      class="period-selector__select"
      @update:model-value="$emit('update:modelValue', $event)"
    />
    <q-btn
      color="primary"
      icon="sym_r_refresh"
      label="Recalcular"
      no-caps
      size="sm"
      :loading="loading"
      @click="$emit('recalcular')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  modelValue: string;
  loading?: boolean;
}

defineProps<Props>();
defineEmits<{ 'update:modelValue': [value: string]; recalcular: [] }>();

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

<style scoped lang="scss">
.period-selector {
  &__select {
    min-width: 180px;
  }
}
</style>
