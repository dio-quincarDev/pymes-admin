<template>
  <div
    class="base-skeleton"
    :class="[`skeleton-${variant}`, `skeleton-${size}`]"
    :style="{ width: customWidth, height: customHeight }"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
interface Props {
  variant?: 'text' | 'circle' | 'rectangle' | 'card';
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  width?: string;
  height?: string;
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'text',
  size: 'md'
});

const customWidth = props.width || undefined;
const customHeight = props.height || undefined;
</script>

<style lang="scss" scoped>
.base-skeleton {
  background: linear-gradient(
    90deg,
    rgba(27, 38, 36, 0.6) 0%,
    rgba(163, 120, 94, 0.15) 50%,
    rgba(27, 38, 36, 0.6) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 4px;

  &.skeleton-xs {
    height: 8px;
    width: 60px;
  }

  &.skeleton-sm {
    height: 12px;
    width: 100px;
  }

  &.skeleton-md {
    height: 16px;
    width: 200px;
  }

  &.skeleton-lg {
    height: 24px;
    width: 300px;
  }

  &.skeleton-xl {
    height: 32px;
    width: 400px;
  }

  &.skeleton-text {
    border-radius: 4px;
  }

  &.skeleton-circle {
    border-radius: 50%;
    width: 40px;
    height: 40px;
  }

  &.skeleton-rectangle {
    border-radius: 6px;
  }

  &.skeleton-card {
    border-radius: 8px;
    height: 120px;
    width: 100%;
  }
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}
</style>