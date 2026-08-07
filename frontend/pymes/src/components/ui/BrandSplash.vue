<template>
  <Transition name="splash-fade">
    <div v-if="show" class="brand-splash" aria-hidden="true">
      <div class="splash-content">
        <div class="splash-logo mesh-text-gradient">PYMEQ</div>
        <div class="splash-bar">
          <div class="splash-bar-fill" />
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{ active: boolean }>();
const show = ref(false);

watch(() => props.active, (val) => {
  show.value = val;
}, { immediate: true });
</script>

<style lang="scss" scoped>
.brand-splash {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: $dark-page;
  display: flex;
  align-items: center;
  justify-content: center;
}

.splash-content {
  text-align: center;
}

.splash-logo {
  font-family: 'Outfit', sans-serif;
  font-size: 48px;
  font-weight: 800;
  letter-spacing: 4px;
  animation: breathe 2s ease-in-out infinite;
}

.splash-bar {
  width: 120px;
  height: 3px;
  background: rgba(113, 131, 127, 0.15);
  border-radius: 2px;
  margin: 24px auto 0;
  overflow: hidden;
}

.splash-bar-fill {
  width: 40%;
  height: 100%;
  background: linear-gradient(90deg, transparent, $primary, transparent);
  border-radius: 2px;
  animation: slide 1.2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.03); }
}

@keyframes slide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(350%); }
}

.splash-fade-leave-active {
  transition: opacity 0.4s ease;
}

.splash-fade-leave-to {
  opacity: 0;
}
</style>
