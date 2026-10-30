<script setup lang="ts">
import { useScrollReveal } from 'src/composables/useScrollReveal';

const { target, isVisible } = useScrollReveal();
</script>

<template>
  <section id="trust" ref="target" class="trust-section" :class="{ 'reveal-visible': isVisible }">
    <div class="container">
      <div class="trust-band reveal-up">
        <div class="trust-claim">
          <span class="trust-number">8</span>
          <span class="trust-text">industrias precargadas</span>
        </div>
        <div class="trust-separator" />
        <div class="trust-claim">
          <span class="trust-text">Sin inventario</span>
          <span class="trust-sub">Solo inteligencia financiera</span>
        </div>
        <div class="trust-separator" />
        <div class="trust-claim">
          <span class="trust-text">PWA offline</span>
          <span class="trust-sub">Funciona sin internet</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 clamp(16px, 4vw, 24px);
  width: 100%;
}

.trust-section {
  padding: clamp(48px, 8vw, 96px) 0;
  border-top: 1px solid var(--pq-border);
  border-bottom: 1px solid var(--pq-border);
}

.trust-band {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;

  @media (min-width: 768px) {
    flex-direction: row;
    justify-content: center;
    align-items: center;
  }
}

.trust-claim {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 32px;
  text-align: center;

  @media (min-width: 768px) {
    flex-direction: row;
    gap: 12px;
    align-items: baseline;
  }
}

.trust-number {
  font-family: var(--pq-font-utility);
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 500;
  color: var(--pq-accent);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.trust-text {
  font-family: var(--pq-font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--pq-text);
  line-height: 1.3;
}

.trust-sub {
  font-family: var(--pq-font-body);
  font-size: 14px;
  color: var(--pq-text-muted);
  line-height: 1.4;
}

.trust-separator {
  width: 48px;
  height: 1px;
  background: var(--pq-border);

  @media (min-width: 768px) {
    width: 1px;
    height: 48px;
  }
}

.reveal-up {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.32s cubic-bezier(0.22, 1, 0.36, 1),
              transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.reveal-visible .reveal-up {
  opacity: 1;
  transform: translateY(0);
}
</style>
