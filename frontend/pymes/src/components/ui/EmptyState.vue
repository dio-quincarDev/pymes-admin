<template>
  <div class="empty-state flex flex-center text-center q-pa-xl">
    <div class="empty-illustration q-mb-lg">
      <div class="illustration-ring" />
      <q-icon :name="icon" size="3rem" color="primary" class="illustration-icon" />
    </div>
    <div class="text-h6 text-primary font-bold q-mb-sm">{{ title }}</div>
    <p class="text-body2 text-accent" style="max-width: 320px">{{ message }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  icon?: string;
  title: string;
  message: string;
}>(), {
  icon: 'inbox',
});
</script>

<style lang="scss" scoped>
.empty-state {
  padding: 48px 24px;
}

.empty-illustration {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.illustration-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: conic-gradient(from 0deg, rgba(163, 120, 94, 0.15), rgba(197, 160, 89, 0.1), rgba(163, 120, 94, 0.05), rgba(163, 120, 94, 0.15));
  animation: spin-slow 8s linear infinite;
}

.illustration-icon {
  position: relative;
  z-index: 1;
}

@keyframes spin-slow {
  to { transform: rotate(360deg); }
}
</style>
