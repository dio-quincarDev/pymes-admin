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
  font-family: 'Outfit', 'Source Sans 3', sans-serif;
  font-weight: 600;
  cursor: pointer;
  border: none;
  border-radius: 12px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  // Ripple effect
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at var(--ripple-x, 50%) var(--ripple-y, 50%), rgba(255, 255, 255, 0.3) 0%, transparent 60%);
    opacity: 0;
    transition: opacity 0.4s ease;
    pointer-events: none;
  }

  &:active:not(.is-disabled)::after {
    opacity: 1;
    transition: opacity 0s;
  }

  &:focus-visible {
    outline: 2px solid $primary;
    outline-offset: 3px;
    box-shadow: 0 0 0 6px rgba(163, 120, 94, 0.15);
  }

  &:active:not(.is-disabled) {
    transform: scale(0.97);
  }

  &.is-disabled {
    opacity: 0.5;
    cursor: not-allowed;
    pointer-events: none;
  }

  // Sizes
  &.size-xs {
    padding: 4px 10px;
    font-size: 12px;
  }

  &.size-sm {
    padding: 6px 14px;
    font-size: 13px;
  }

  &.size-md {
    padding: 8px 18px;
    font-size: 14px;
  }

  &.size-lg {
    padding: 12px 24px;
    font-size: 16px;
  }

  // Variants
  &.variant-primary {
    background: linear-gradient(135deg, $primary 0%, #B08A6F 100%);
    color: white;
    box-shadow: 0 2px 12px rgba(163, 120, 94, 0.3);

    &:hover:not(.is-disabled) {
      background: linear-gradient(135deg, #B08A6F 0%, #C5A07A 100%);
      box-shadow: 0 4px 20px rgba(163, 120, 94, 0.5);
      transform: translateY(-1px);
    }
  }

  &.variant-secondary {
    background: $dark;
    color: $secondary;
    border: 1px solid rgba(113, 131, 127, 0.2);

    &:hover:not(.is-disabled) {
      background: rgba(27, 38, 36, 0.9);
      border-color: rgba(163, 120, 94, 0.4);
      transform: translateY(-1px);
    }
  }

  &.variant-ghost {
    background: transparent;
    color: $accent;

    &:hover:not(.is-disabled) {
      background: rgba(113, 131, 127, 0.12);
      color: $secondary;
    }
  }

  &.variant-danger {
    background: linear-gradient(135deg, $negative 0%, #A0522D 100%);
    color: white;

    &:hover:not(.is-disabled) {
      background: linear-gradient(135deg, #A0522D 0%, #B8653A 100%);
      transform: translateY(-1px);
    }
  }

  &.variant-success {
    background: linear-gradient(135deg, $positive 0%, #3A7A33 100%);
    color: white;

    &:hover:not(.is-disabled) {
      background: linear-gradient(135deg, #3A7A33 0%, #4A9A42 100%);
      transform: translateY(-1px);
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