<template>
  <section class="hero-section q-px-md">
    <div class="hero-grid container-narrow mx-auto">
      <!-- Left: copy + onboarding -->
      <div class="hero-left">
        <div class="stagger-children">
          <div class="hero-badge">
            <span class="badge-label">GESTIÓN FINANCIERA INTELIGENTE</span>
          </div>

          <h1 class="hero-headline">
            Organiza tu empresa y<br>
            <span class="hero-accent">toma el control.</span>
          </h1>

          <p class="hero-copy">
            Pymeq es tu aliado estratégico para entender tus flujos de caja
            y optimizar cada centavo de tu negocio.
          </p>

          <div class="onboarding-row">
            <q-input
              v-model="companyForm.name"
              placeholder="Nombre de tu negocio"
              aria-label="Nombre de tu negocio"
              dark
              filled
              color="primary"
              class="company-input focus-ring"
              @keyup.enter="$emit('start', companyForm.name)"
            >
              <template v-slot:prepend>
                <q-icon name="store" style="color: var(--pq-accent)" aria-hidden="true" />
              </template>
            </q-input>

            <BaseButton
              size="lg"
              class="onboarding-btn"
              @click="$emit('start', companyForm.name)"
            >
              CREAR MI ESPACIO
            </BaseButton>
          </div>

          <div class="hero-fine-print">
            Sin complicaciones · Diseñado para la realidad de LATAM
          </div>
        </div>
      </div>

      <!-- Right: KPI strip -->
      <div class="hero-right">
        <div class="kpi-strip stagger-children">
          <div class="kpi-block">
            <div class="kpi-number">500+</div>
            <div class="kpi-label">Empresas activas</div>
          </div>
          <div class="kpi-divider" />
          <div class="kpi-block">
            <div class="kpi-number">18%</div>
            <div class="kpi-label">Ahorro promedio</div>
          </div>
          <div class="kpi-divider" />
          <div class="kpi-block">
            <div class="kpi-number">24/7</div>
            <div class="kpi-label">Siempre disponible</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import BaseButton from 'src/components/base/BaseButton.vue';

defineEmits<{
  start: [name: string];
}>();

const companyForm = reactive({ name: '' });
</script>

<style lang="scss" scoped>
.hero-section {
  min-height: 90vh;
  display: flex;
  align-items: center;
}

.hero-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 48px;
  align-items: center;

  @media (min-width: 768px) {
    grid-template-columns: 3fr 2fr;
    gap: 64px;
  }
}

.container-narrow {
  max-width: 1000px;
}

.mx-auto {
  margin-left: auto;
  margin-right: auto;
}

/* --------------------------------------------------
   Left — copy
-------------------------------------------------- */
.hero-badge {
  margin-bottom: 24px;
}

.badge-label {
  display: inline-block;
  font-family: 'Satoshi', sans-serif;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--pq-accent);
  padding: 4px 16px;
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-full);
}

.hero-headline {
  font-family: 'Geist', sans-serif;
  font-size: 32px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--pq-text);
  margin: 0 0 16px;

  @media (min-width: 768px) {
    font-size: 40px;
  }
}

.hero-accent {
  color: var(--pq-accent);
}

.hero-copy {
  font-family: 'Satoshi', sans-serif;
  font-size: 16px;
  font-weight: 400;
  line-height: 1.6;
  color: var(--pq-text-muted);
  max-width: 500px;
  margin: 0 0 32px;
}

.onboarding-row {
  display: flex;
  flex-direction: column;
  gap: 12px;

  @media (min-width: 600px) {
    flex-direction: row;
    align-items: center;
  }
}

.company-input {
  width: 100%;
  max-width: 320px;

  :deep(.q-field__control) {
    height: 56px;
    border-radius: var(--pq-radius-sm);
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid var(--pq-border);
    transition: all var(--pq-motion-base);

    &:hover {
      border-color: rgba(200, 150, 62, 0.4);
      background: rgba(255, 255, 255, 0.06);
    }
  }
}

.onboarding-btn {
  height: 56px;
  min-width: 180px;
  white-space: nowrap;
}

.hero-fine-print {
  font-family: 'Satoshi', sans-serif;
  font-size: 12px;
  color: var(--pq-text-subtle);
  margin-top: 16px;
}

/* --------------------------------------------------
   Right — KPI strip
-------------------------------------------------- */
.kpi-strip {
  display: flex;
  flex-direction: column;
  gap: 0;

  @media (min-width: 768px) {
    align-items: flex-end;
    text-align: right;
  }
}

.kpi-block {
  padding: 24px 0;

  @media (min-width: 768px) {
    padding: 28px 0;
  }
}

.kpi-number {
  font-family: 'Geist Mono', monospace;
  font-size: 32px;
  font-weight: 500;
  color: var(--pq-accent);
  font-variant-numeric: tabular-nums;
  line-height: 1;

  @media (min-width: 768px) {
    font-size: 40px;
  }
}

.kpi-label {
  font-family: 'Satoshi', sans-serif;
  font-size: 12px;
  color: var(--pq-text-subtle);
  margin-top: 6px;
}

.kpi-divider {
  width: 48px;
  height: 1px;
  background: var(--pq-border);

  @media (min-width: 768px) {
    margin-left: auto;
  }
}

/* --------------------------------------------------
   Stagger animation
-------------------------------------------------- */
.stagger-children > * {
  opacity: 0;
  animation: fadeInUp 0.4s ease forwards;
  &:nth-child(1) { animation-delay: 0.05s; }
  &:nth-child(2) { animation-delay: 0.1s; }
  &:nth-child(3) { animation-delay: 0.15s; }
  &:nth-child(4) { animation-delay: 0.2s; }
  &:nth-child(5) { animation-delay: 0.25s; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
