<script setup lang="ts">
import { ref, computed } from 'vue'
import type { SetupCategory } from 'src/modules/core/types'

const props = defineProps<{
  categories: SetupCategory[]
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const expandedParent = ref<string | null>(null)

const parents = computed(() => props.categories)

function toggleParent(code: string) {
  expandedParent.value = expandedParent.value === code ? null : code
  emit('update:modelValue', code)
}

function selectChild(code: string) {
  emit('update:modelValue', code)
}

function deselectAll() {
  expandedParent.value = null
  emit('update:modelValue', '')
}

const childrenOfParent = computed(() => {
  if (!expandedParent.value) return []
  const parent = findCategory(props.categories, expandedParent.value)
  return parent?.children || []
})

function findCategory(cats: SetupCategory[], code: string): SetupCategory | null {
  for (const c of cats) {
    if (c.code === code) return c
    if (c.children?.length) {
      const found = findCategory(c.children, code)
      if (found) return found
    }
  }
  return null
}
</script>

<template>
  <div v-if="parents.length" class="product-cat-tabs q-mb-md" role="tablist" aria-label="Filtrar por categoría">
    <q-chip
      :color="!modelValue ? 'primary' : 'dark'"
      :text-color="!modelValue ? 'dark' : 'accent'"
      dense clickable @click="deselectAll" class="product-cat-tab"
      role="tab" :aria-selected="!modelValue">
      Todos
    </q-chip>

    <template v-for="cat in parents" :key="cat.code">
      <q-chip
        :color="modelValue === cat.code ? 'primary' : (expandedParent === cat.code ? 'accent' : 'dark')"
        :text-color="modelValue === cat.code ? 'dark' : 'accent'"
        dense clickable @click="toggleParent(cat.code)" class="product-cat-tab"
        role="tab" :aria-selected="modelValue === cat.code" :class="{ 'cat-expanded': expandedParent === cat.code }">
        {{ cat.name }}
      </q-chip>

      <div v-if="expandedParent === cat.code && childrenOfParent.length" class="subcategory-row">
        <q-chip
          v-for="child in childrenOfParent" :key="child.code"
          :color="modelValue === child.code ? 'primary' : 'dark'"
          :text-color="modelValue === child.code ? 'dark' : 'accent'"
          dense clickable @click="selectChild(child.code)" class="product-cat-tab subcategory-chip"
          role="tab" :aria-selected="modelValue === child.code">
          {{ child.name }}
        </q-chip>
      </div>
    </template>
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

.cat-expanded {
  border-color: rgba(163, 120, 94, 0.3);
}

.subcategory-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  width: 100%;
  padding: 0.25rem 0 0.25rem 1.25rem;
  margin-bottom: 0.25rem;
}

.subcategory-chip {
  opacity: 0.85;
  font-size: 0.73rem;
}
</style>
