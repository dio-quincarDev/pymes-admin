<script setup lang="ts">
defineProps<{
  categories: string[]
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <div v-if="categories.length > 1" class="product-cat-tabs q-mb-md" role="tablist" aria-label="Filtrar por categoría">
    <q-chip
      :color="!modelValue ? 'primary' : 'dark'" :text-color="!modelValue ? 'dark' : 'accent'"
      dense clickable @click="emit('update:modelValue', '')" class="product-cat-tab"
      role="tab" :aria-selected="!modelValue">
      Todos
    </q-chip>
    <q-chip
      v-for="cat in categories" :key="cat"
      :color="modelValue === cat ? 'primary' : 'dark'" :text-color="modelValue === cat ? 'dark' : 'accent'"
      dense clickable @click="emit('update:modelValue', cat)" class="product-cat-tab"
      role="tab" :aria-selected="modelValue === cat">
      {{ cat }}
    </q-chip>
  </div>
</template>

<style scoped>
.product-cat-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.product-cat-tab {
  cursor: pointer;
  font-size: 0.78rem;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;

  &:hover {
    border-color: rgba(163, 120, 94, 0.2);
  }
}
</style>
