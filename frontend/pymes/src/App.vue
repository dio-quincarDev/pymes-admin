<template>
  <Transition name="fade" mode="out-in">
    <router-view />
  </Transition>
</template>

<script setup lang="ts">
import { watch } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

watch(
  () => router.currentRoute.value,
  () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
);
</script>

<style lang="scss">
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
