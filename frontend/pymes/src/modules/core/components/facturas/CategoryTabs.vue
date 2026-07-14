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
  const parent = props.categories.find(c => c.code === expandedParent.value)
  return parent?.children || []
})
</script>

<template>
  <div v-if="categories.length" class="category-chips" role="tablist" aria-label="Filtrar por categoría">
    <q-chip
      :selected="!modelValue"
      clickable
      @click="deselectAll"
      color="primary"
      text-color="dark"
      size="sm"
      class="category-chip"
      role="tab"
      :aria-selected="!modelValue"
    >Todos</q-chip>

    <template v-for="cat in categories" :key="cat.code">
      <q-chip
        :selected="modelValue === cat.code"
        clickable
        @click="toggleParent(cat.code)"
        :removable="!!cat.children?.length"
        @remove="toggleParent(cat.code)"
        color="primary"
        text-color="dark"
        size="sm"
        class="category-chip"
        role="tab"
        :aria-selected="modelValue === cat.code"
      >{{ cat.name }}</q-chip>

      <Transition name="subcats">
        <div v-if="expandedParent === cat.code && childrenOfParent.length" class="subcategory-chips">
          <q-chip
            v-for="child in childrenOfParent"
            :key="child.code"
            :selected="modelValue === child.code"
            clickable
            @click="selectChild(child.code)"
            outline
            color="accent"
            size="xs"
            class="subcategory-chip"
            role="tab"
            :aria-selected="modelValue === child.code"
          >{{ child.name }}</q-chip>
        </div>
      </Transition>
    </template>
  </div>
</template>

<style scoped>
.category-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.category-chip {
  font-weight: 500;
}

.subcategory-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  width: 100%;
  padding: 2px 0 4px 4px;
}

.subcategory-chip {
  font-size: 0.7rem;
}

.subcats-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.subcats-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.subcats-enter-from,
.subcats-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.subcats-enter-to,
.subcats-leave-from {
  opacity: 1;
  max-height: 80px;
}
</style>
