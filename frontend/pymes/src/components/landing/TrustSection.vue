<template>
  <section id="trust" ref="target" class="trust-section q-pa-xl text-center" :class="{ 'reveal-visible': isVisible }">
    <div class="reveal-up">
      <div class="text-overline text-accent opacity-70 q-mb-xl">CONFIABLE PARA CUALQUIER SECTOR</div>
      <div class="row justify-center gap-lg gap-md-xl items-center opacity-50 filter-grayscale text-weight-bolder">
        <div class="text-subtitle1 letter-spacing-2">COMERCIO</div>
        <div class="text-subtitle1 letter-spacing-2">SERVICIOS</div>
        <div class="text-subtitle1 letter-spacing-2">TALLERES</div>
        <div class="text-subtitle1 letter-spacing-2">EMPRENDEDORES</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useScrollReveal } from 'src/composables/useScrollReveal';

const { target, isVisible } = useScrollReveal();
</script>

<style scoped>
.reveal-up {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease 0.2s, transform 0.6s ease 0.2s;
}

.reveal-visible .reveal-up {
  opacity: 1;
  transform: translateY(0);
}

.letter-spacing-2 { letter-spacing: 2px; }
.filter-grayscale { filter: grayscale(1); }
.opacity-50 { opacity: 0.5; }
.opacity-70 { opacity: 0.7; }
.gap-lg { gap: 32px; }
.gap-md-xl { gap: 40px; }

@media (min-width: 1024px) {
  .gap-md-xl { gap: 40px; }
}
</style>
