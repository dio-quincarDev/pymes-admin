<template>
  <component
    :is="tag"
    class="base-button"
    :class="[
      `variant-${variant}`,
      `size-${size}`,
      { 'is-loading': loading, 'is-disabled': disabled }
    ]"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
    v-bind="$attrs"
    @click="handleClick"
  >
    <span v-if="loading" class="spinner-wrapper">
      <q-spinner-dots :size="spinnerSize" color="current" />
    </span>
    <span v-else class="content-wrapper">
      <q-icon v-if="iconLeft" :name="iconLeft" class="icon-left" />
      <slot />
      <q-icon v-if="iconRight" :name="iconRight" class="icon-right" />
    </span>
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'success';
  size?: 'xs' | 'sm' | 'md' | 'lg';
  tag?: string;
  loading?: boolean;
  disabled?: boolean;
  iconLeft?: string;
  iconRight?: string;
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  tag: 'button',
  loading: false,
  disabled: false
});

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();

const spinnerSize = computed(() => {
  const sizes = { xs: '14px', sm: '16px', md: '20px', lg: '24px' };
  return sizes[props.size];
});

const handleClick = (e: MouseEvent) => {
  if (!props.disabled && !props.loading) {
    emit('click', e);
  }
};
</script>

<style lang="scss" scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: 'Geist', 'Satoshi', sans-serif;
  font-weight: 600;
  cursor: pointer;
  border: none;
  border-radius: 6px;
  transition: all 0.16s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &:focus-visible {
    outline: 2px solid $primary;
    outline-offset: 2px;
  }

  &:active:not(.is-disabled) {
    transform: scale(0.97);
  }

  &.is-disabled {
    opacity: 0.4;
    cursor: not-allowed;
    pointer-events: none;
  }

  // Sizes
  &.size-xs {
    padding: 3px 8px;
    font-size: 12px;
    height: 28px;
  }

  &.size-sm {
    padding: 5px 12px;
    font-size: 13px;
    height: 34px;
  }

  &.size-md {
    padding: 7px 16px;
    font-size: 14px;
    height: 40px;
  }

  &.size-lg {
    padding: 10px 24px;
    font-size: 15px;
    height: 46px;
  }

  // Variants — no gradients, flat solid colors
  &.variant-primary {
    background: $primary;
    color: #08090D;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);

    &:hover:not(.is-disabled) {
      background: #D4A552;
      box-shadow: 0 4px 12px rgba(200, 150, 62, 0.3);
    }
  }

  &.variant-secondary {
    background: $dark;
    color: $secondary;
    border: 1px solid rgba(53, 57, 69, 0.4);

    &:hover:not(.is-disabled) {
      background: #1E2129;
      border-color: rgba(200, 150, 62, 0.4);
    }
  }

  &.variant-ghost {
    background: transparent;
    color: #9B9790;

    &:hover:not(.is-disabled) {
      background: rgba(200, 150, 62, 0.06);
      color: $secondary;
    }
  }

  &.variant-danger {
    background: #A04038;
    color: #F5F3EF;

    &:hover:not(.is-disabled) {
      background: #B84A42;
    }
  }

  &.variant-success {
    background: #3D7A5A;
    color: #F5F3EF;

    &:hover:not(.is-disabled) {
      background: #4A8E6A;
    }
  }

  &.is-loading {
    pointer-events: none;
    .spinner-wrapper { display: flex; }
    .content-wrapper { opacity: 0; }
  }

  .icon-left, .icon-right { font-size: 1.1em; }
  .content-wrapper {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
}
</style>
