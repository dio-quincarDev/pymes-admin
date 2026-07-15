<template>
  <section id="trust" ref="target" class="trust-section q-pa-xl" :class="{ 'reveal-visible': isVisible }">
    <div class="reveal-up">
      <div class="trust-label">CONFIABLE PARA CUALQUIER SECTOR</div>

      <div class="stat-strip">
        <div class="stat-block">
          <div class="stat-number">500+</div>
          <div class="stat-label">Empresas activas</div>
        </div>
        <div class="stat-divider" />
        <div class="stat-block">
          <div class="stat-number">18%</div>
          <div class="stat-label">Ahorro promedio</div>
        </div>
        <div class="stat-divider" />
        <div class="stat-block">
          <div class="stat-number">24/7</div>
          <div class="stat-label">Siempre disponible</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useScrollReveal } from 'src/composables/useScrollReveal';

const { target, isVisible } = useScrollReveal();
</script>

<style scoped>
.trust-section {
  text-align: center;
}

.reveal-up {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease 0.2s, transform 0.6s ease 0.2s;
}

.reveal-visible .reveal-up {
  opacity: 1;
  transform: translateY(0);
}

.trust-label {
  font-family: 'Satoshi', sans-serif;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--pq-text-subtle);
  margin-bottom: 32px;
}

.stat-strip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

@media (min-width: 600px) {
  .stat-strip {
    flex-direction: row;
    justify-content: center;
    gap: 0;
  }
}

.stat-block {
  padding: 16px 32px;
  text-align: center;
}

.stat-number {
  font-family: 'Geist Mono', monospace;
  font-size: 28px;
  font-weight: 500;
  color: var(--pq-text);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

@media (min-width: 600px) {
  .stat-number {
    font-size: 32px;
  }
}

.stat-label {
  font-family: 'Satoshi', sans-serif;
  font-size: 12px;
  color: var(--pq-text-muted);
  margin-top: 6px;
}

.stat-divider {
  width: 48px;
  height: 1px;
  background: var(--pq-border);
}

@media (min-width: 600px) {
  .stat-divider {
    width: 1px;
    height: 48px;
  }
}
</style>
