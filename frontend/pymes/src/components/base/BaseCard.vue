<template>
  <div class="base-card hover-lift" :class="[`variant-${variant}`, { 'no-padding': !padding }]" role="group">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'default' | 'elevated' | 'outlined' | 'ghost';
  padding?: boolean;
}

withDefaults(defineProps<Props>(), {
  variant: 'default',
  padding: true
});
</script>

<style lang="scss" scoped>
.base-card {
  border-radius: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &.no-padding {
    padding: 0;
  }

  &.variant-default {
    background: rgba(27, 38, 36, 0.7);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid rgba(113, 131, 127, 0.1);
  }

  &.variant-elevated {
    background: rgba(27, 38, 36, 0.85);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    border: 1px solid rgba(113, 131, 127, 0.05);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }

  &.variant-outlined {
    background: transparent;
    backdrop-filter: none;
    border: 1px solid rgba(113, 131, 127, 0.2);
  }

  &.variant-ghost {
    background: rgba(11, 18, 16, 0.5);
    backdrop-filter: blur(4px);
    border: 1px solid rgba(163, 120, 94, 0.1);
  }
}
</style>