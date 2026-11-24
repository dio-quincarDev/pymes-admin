<template>
  <div class="empty-state flex flex-center text-center">
    <div class="empty-illustration q-mb-lg empty-enter empty-enter--1">
      <div class="illustration-ring" />
      <q-icon :name="icon" size="2.5rem" color="primary" class="illustration-icon icon-pulse" />
    </div>
    <div class="text-h6 text-primary font-bold q-mb-sm empty-enter empty-enter--2">{{ title }}</div>
    <p class="text-body2 text-accent empty-enter empty-enter--3" style="max-width: clamp(240px, 50vw, 480px)">{{ message }}</p>
    <div class="empty-enter empty-enter--4">
      <slot />
    </div>
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
  padding: clamp(24px, 4vw, 40px) 24px;
  max-width: 480px;
  margin: 0 auto;
}

.empty-illustration {
  position: relative;
  width: clamp(56px, 8vw, 72px);
  height: clamp(56px, 8vw, 72px);
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
  opacity: 0;
  animation: ring-fade 0.4s ease forwards, spin-slow 8s linear 0.4s infinite;
}

.illustration-icon {
  position: relative;
  z-index: 1;
}

.icon-pulse {
  animation: icon-pulse 3s ease-in-out infinite;
}

.empty-enter {
  opacity: 0;
  transform: translateY(12px);
  animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.empty-enter--1 { animation-delay: 0.05s; }
.empty-enter--2 { animation-delay: 0.13s; }
.empty-enter--3 { animation-delay: 0.21s; }
.empty-enter--4 { animation-delay: 0.29s; }

@keyframes icon-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

@keyframes ring-fade {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes spin-slow {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .illustration-ring {
    animation: none;
    opacity: 1;
  }
  .icon-pulse {
    animation: none;
  }
  .empty-enter {
    opacity: 1;
    transform: none;
    animation: none;
  }
}
</style>