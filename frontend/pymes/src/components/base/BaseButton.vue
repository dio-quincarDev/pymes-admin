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
  const sizes = { xs: '14px', sm: '18px', md: '22px', lg: '26px' };
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
  font-family: inherit;
  font-weight: 600;
  cursor: pointer;
  border: none;
  border-radius: 6px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &:focus-visible {
    outline: 2px solid $primary;
    outline-offset: 2px;
  }

  &:active:not(.is-disabled) {
    transform: scale(0.96);
  }

  &.is-disabled {
    opacity: 0.5;
    cursor: not-allowed;
    pointer-events: none;
  }

  // Sizes
  &.size-xs {
    padding: 6px 12px;
    font-size: 12px;
  }

  &.size-sm {
    padding: 8px 16px;
    font-size: 13px;
  }

  &.size-md {
    padding: 10px 20px;
    font-size: 14px;
  }

  &.size-lg {
    padding: 14px 28px;
    font-size: 16px;
  }

  // Variants
  &.variant-primary {
    background: $primary;
    color: white;
    box-shadow: 0 0 15px rgba(163, 120, 94, 0.3);

    &:hover:not(.is-disabled) {
      background: #B08A6F;
      box-shadow: 0 0 20px rgba(163, 120, 94, 0.5);
    }
  }

  &.variant-secondary {
    background: $dark;
    color: $secondary;
    border: 1px solid rgba(113, 131, 127, 0.2);

    &:hover:not(.is-disabled) {
      background: rgba(27, 38, 36, 0.9);
      border-color: rgba(163, 120, 94, 0.3);
    }
  }

  &.variant-ghost {
    background: transparent;
    color: $accent;

    &:hover:not(.is-disabled) {
      background: rgba(113, 131, 127, 0.1);
      color: $secondary;
    }
  }

  &.variant-danger {
    background: $negative;
    color: white;

    &:hover:not(.is-disabled) {
      background: #7A3D11;
    }
  }

  &.variant-success {
    background: $positive;
    color: white;

    &:hover:not(.is-disabled) {
      background: #254D22;
    }
  }

  // Loading state
  &.is-loading {
    pointer-events: none;

    .spinner-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .content-wrapper {
      opacity: 0;
    }
  }

  .icon-left,
  .icon-right {
    font-size: 1.1em;
  }

  .content-wrapper {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
}
</style>