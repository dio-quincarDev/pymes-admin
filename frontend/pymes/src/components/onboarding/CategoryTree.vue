<script setup lang="ts">
import type { SetupCategory } from 'src/modules/core/types'

interface Props {
  categories: SetupCategory[]
  depth?: number
}

const props = withDefaults(defineProps<Props>(), { depth: 0 })
</script>

<template>
  <div class="category-tree" :style="{ '--depth': props.depth }">
    <div
      v-for="cat in props.categories"
      :key="cat.code"
      class="category-node"
    >
      <div class="category-node__row">
        <q-icon
          v-if="cat.children.length"
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
          v-if="cat.children.length"
          :label="cat.children.length"
          color="primary"
          class="category-node__count"
        />
      </div>
      <CategoryTree
        v-if="cat.children.length"
        :categories="cat.children"
        :depth="props.depth + 1"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.category-tree {
  padding-left: v-bind('`${props.depth * 1.25}rem`');
}

.category-node {
  &__row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.4rem 0.6rem;
    border-radius: 6px;
    transition: background 0.15s ease;

    &:hover {
      background: rgba(163, 120, 94, 0.06);
    }
  }

  &__icon {
    flex-shrink: 0;
    opacity: 0.7;
  }

  &__name {
    font-family: 'Outfit', sans-serif;
    font-size: 0.85rem;
    color: #E2E8E4;
    line-height: 1.3;
  }

  &__count {
    font-size: 0.65rem;
    margin-left: auto;
  }
}
</style>
