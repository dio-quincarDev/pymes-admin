<template>
  <section id="trust" ref="target" class="trust-section q-pa-xl" :class="{ 'reveal-visible': isVisible }">
    <div class="reveal-up">
      <div class="trust-label">HECHO PARA LA REALIDAD LATAM</div>

      <div class="stat-strip">
        <div class="trust-block">
          <div class="trust-primary">8 industrias</div>
          <div class="trust-secondary">Restaurantes, ferreterías, farmacias y más.</div>
        </div>
        <div class="stat-divider" />
        <div class="trust-block">
          <div class="trust-primary">Sin inventario</div>
          <div class="trust-secondary">Solo inteligencia financiera.</div>
        </div>
        <div class="stat-divider" />
        <div class="trust-block">
          <div class="trust-primary">PWA offline</div>
          <div class="trust-secondary">Funciona sin internet.</div>
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

.trust-block {
  padding: 16px 32px;
  text-align: center;
}

.trust-primary {
  font-family: 'Satoshi', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--pq-text);
  line-height: 1.3;
}

.trust-secondary {
  font-family: 'Satoshi', sans-serif;
  font-size: 14px;
  font-weight: 400;
  color: var(--pq-text-muted);
  margin-top: 4px;
  line-height: 1.4;
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
