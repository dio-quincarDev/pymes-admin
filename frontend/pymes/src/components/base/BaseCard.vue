<template>
  <div class="base-card" :class="[`variant-${variant}`, { 'no-padding': !padding }]" role="group">
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
  transition: all 0.16s cubic-bezier(0.4, 0, 0.2, 1);

  &.no-padding { padding: 0; }

  &.variant-default {
    background: #12141A;
    border: 1px solid rgba(53, 57, 69, 0.3);
  }

  &.variant-elevated {
    background: #12141A;
    border: 1px solid rgba(53, 57, 69, 0.2);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
  }

  &.variant-outlined {
    background: transparent;
    border: 1px solid rgba(53, 57, 69, 0.5);
  }

  &.variant-ghost {
    background: rgba(200, 150, 62, 0.03);
    border: 1px solid rgba(200, 150, 62, 0.1);
  }
}
</style>
