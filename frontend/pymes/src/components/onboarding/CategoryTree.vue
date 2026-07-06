<script setup lang="ts">
import type { SetupCategory } from 'src/modules/core/types'

interface Props {
  categories: SetupCategory[]
  depth?: number
}

const props = withDefaults(defineProps<Props>(), { depth: 0 })
</script>

<template>
  <div class="category-tree">
    <div
      v-for="cat in props.categories"
      :key="cat.code"
      class="category-node"
    >
      <div class="category-node__row">
        <q-icon
          v-if="cat.children && cat.children.length"
          name="folder"
          size="1rem"
          color="primary"
          class="category-node__icon"
        />
        <q-icon
          v-else
          name="label"
          size="1rem"
          color="accent"
          class="category-node__icon"
        />
        <span class="category-node__name">{{ cat.name }}</span>
        <q-badge
          v-if="cat.children && cat.children.length"
          :label="cat.children.length"
          color="primary"
          class="category-node__count"
        />
      </div>
      <div v-if="cat.children && cat.children.length" class="category-node__children">
        <CategoryTree
          :categories="cat.children"
          :depth="props.depth + 1"
        />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.category-tree {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.category-node {
  &__row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.45rem 0.75rem;
    border-radius: 8px;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    background: rgba(255, 255, 255, 0.01);
    border: 1px solid rgba(255, 255, 255, 0.02);

    &:hover {
      background: rgba(163, 120, 94, 0.08);
      border-color: rgba(163, 120, 94, 0.15);
      transform: translateX(4px);

      .category-node__icon {
        color: #C5A059 !important;
      }
    }
  }

  &__children {
    margin-left: 0.75rem;
    border-left: 1px dashed rgba(163, 120, 94, 0.2);
    padding-left: 1rem;
    margin-top: 4px;
    margin-bottom: 4px;
  }

  &__icon {
    flex-shrink: 0;
    opacity: 0.85;
    transition: color 0.2s ease;
  }

  &__name {
    font-family: 'Outfit', sans-serif;
    font-size: 0.85rem;
    font-weight: 500;
    color: #E2E8E4;
    line-height: 1.3;
  }

  &__count {
    font-size: 0.65rem;
    font-weight: 600;
    margin-left: auto;
    padding: 2px 6px;
    background: rgba(163, 120, 94, 0.15) !important;
    color: #E2E8E4;
    border: 1px solid rgba(163, 120, 94, 0.25);
    border-radius: 4px;
  }
}
</style>
