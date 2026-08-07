<template>
  <span class="base-badge" :class="[`badge-${variant}`, size]" role="status">
    <slot />
  </span>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'accent'
  size?: 'sm' | 'md'
}

withDefaults(defineProps<Props>(), {
  variant: 'info',
  size: 'sm',
})
</script>

<style scoped>
.base-badge {
  display: inline-flex;
  align-items: center;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  white-space: nowrap;
  border-radius: var(--pq-radius-full);
}

.sm {
  font-size: 0.65rem;
  padding: 2px 8px;
}

.md {
  font-size: 0.75rem;
  padding: 4px 12px;
}

.badge-success {
  background: color-mix(in srgb, var(--pq-success) 20%, transparent);
  color: var(--pq-success);
}

.badge-warning {
  background: color-mix(in srgb, var(--pq-warning) 20%, transparent);
  color: var(--pq-warning);
}

.badge-danger {
  background: color-mix(in srgb, var(--pq-danger) 20%, transparent);
  color: var(--pq-danger);
}

.badge-info {
  background: color-mix(in srgb, var(--pq-info) 20%, transparent);
  color: var(--pq-info);
}

.badge-accent {
  background: color-mix(in srgb, var(--pq-accent) 20%, transparent);
  color: var(--pq-accent);
}
</style>
