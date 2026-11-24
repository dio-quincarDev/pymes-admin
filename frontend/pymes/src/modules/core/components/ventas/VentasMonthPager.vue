<script setup lang="ts">
defineProps<{
  label: string;
}>();

defineEmits<{
  prev: [];
  next: [];
  today: [];
}>();
</script>

<template>
  <div class="month-pager">
    <q-btn
      round
      flat
      icon="sym_r_chevron_left"
      aria-label="Mes anterior"
      class="month-pager__nav"
      @click="$emit('prev')"
    />
    <q-btn flat no-caps class="month-pager__label" aria-label="Ir al mes actual" @click="$emit('today')">
      {{ label }}
    </q-btn>
    <q-btn
      round
      flat
      icon="sym_r_chevron_right"
      aria-label="Mes siguiente"
      class="month-pager__nav"
      @click="$emit('next')"
    />
  </div>
</template>

<style scoped>
.month-pager {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
  background: var(--pq-background);
  border-bottom: 1px solid var(--pq-border);
  min-height: 56px;
}

.month-pager__nav {
  min-width: 48px;
  min-height: 48px;
  color: var(--pq-text-muted);
}

.month-pager__nav:hover {
  color: var(--pq-accent);
}

.month-pager__label {
  font-family: var(--pq-font-display);
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--pq-text);
  text-transform: capitalize;
}

.month-pager__label:hover {
  color: var(--pq-accent);
}
</style>
