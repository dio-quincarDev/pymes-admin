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
  <div v-if="parents.length" class="category-tabs" role="tablist" aria-label="Filtrar por categoría">
    <div class="category-tabs__list">
      <!-- Todos -->
      <button
        class="category-tab"
        :class="{ 'category-tab--active': !modelValue }"
        @click="deselectAll"
        role="tab"
        :aria-selected="!modelValue"
      >
        <span class="category-tab__label">Todos</span>
        <span class="category-tab__underline" />
      </button>

      <!-- Parent categories -->
      <template v-for="cat in parents" :key="cat.code">
        <button
          class="category-tab"
          :class="{
            'category-tab--active': modelValue === cat.code,
            'category-tab--expanded': expandedParent === cat.code
          }"
          @click="toggleParent(cat.code)"
          role="tab"
          :aria-selected="modelValue === cat.code"
        >
          <span class="category-tab__label">{{ cat.name }}</span>
          <span class="category-tab__chevron" :class="{ 'category-tab__chevron--open': expandedParent === cat.code }">
            <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
              <path d="M2 3.5L5 6.5L8 3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
          <span class="category-tab__underline" />
        </button>

        <!-- Subcategories -->
        <Transition name="subcats">
          <div v-if="expandedParent === cat.code && childrenOfParent.length" class="subcategory-container">
            <button
              v-for="child in childrenOfParent"
              :key="child.code"
              class="subcategory-tab"
              :class="{ 'subcategory-tab--active': modelValue === child.code }"
              @click="selectChild(child.code)"
              role="tab"
              :aria-selected="modelValue === child.code"
            >
              <span class="subcategory-tab__dot" />
              <span class="subcategory-tab__label">{{ child.name }}</span>
            </button>
          </div>
        </Transition>
      </template>
    </div>
  </div>
</template>

<style scoped>
.category-tabs {
  margin-bottom: 12px;
}

.category-tabs__list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
}

/* ─── Tab button ─── */
.category-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 0.78rem;
  font-weight: 500;
  color: rgba(163, 120, 94, 0.55);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  user-select: none;
}

.category-tab:hover {
  color: rgba(163, 120, 94, 0.85);
  background: rgba(163, 120, 94, 0.06);
}

.category-tab--active {
  color: rgba(212, 175, 55, 0.95);
  background: rgba(212, 175, 55, 0.1);
}

.category-tab--expanded {
  background: rgba(163, 120, 94, 0.08);
}

/* ─── Underline ─── */
.category-tab__underline {
  position: absolute;
  bottom: 2px;
  left: 50%;
  width: 0;
  height: 2px;
  background: rgba(212, 175, 55, 0.7);
  border-radius: 1px;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.category-tab--active .category-tab__underline {
  width: 60%;
  left: 20%;
}

/* ─── Chevron ─── */
.category-tab__chevron {
  display: flex;
  align-items: center;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  color: inherit;
  opacity: 0.5;
}

.category-tab__chevron--open {
  transform: rotate(180deg);
  opacity: 0.8;
}

/* ─── Subcategories container ─── */
.subcategory-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  width: 100%;
  padding: 4px 8px 4px 16px;
}

/* ─── Subcategory tab ─── */
.subcategory-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  font-size: 0.73rem;
  font-weight: 500;
  color: rgba(163, 120, 94, 0.5);
  background: rgba(163, 120, 94, 0.04);
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
}

.subcategory-tab:hover {
  color: rgba(163, 120, 94, 0.8);
  background: rgba(163, 120, 94, 0.08);
  border-color: rgba(163, 120, 94, 0.1);
}

.subcategory-tab--active {
  color: rgba(212, 175, 55, 0.95);
  background: rgba(212, 175, 55, 0.08);
  border-color: rgba(212, 175, 55, 0.15);
}

.subcategory-tab__dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.4;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.subcategory-tab--active .subcategory-tab__dot {
  opacity: 0.9;
  transform: scale(1.3);
}

/* ─── Transitions ─── */
.subcats-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.subcats-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.subcats-enter-from {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.subcats-enter-to {
  opacity: 1;
  max-height: 120px;
}

.subcats-leave-from {
  opacity: 1;
  max-height: 120px;
}

.subcats-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}
</style>
