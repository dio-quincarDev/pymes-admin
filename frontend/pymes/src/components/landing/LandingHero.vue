<script setup lang="ts">
import { shallowRef, computed } from 'vue';

const emit = defineEmits<{
  start: [name: string];
}>();

const companyName = shallowRef('');

const trimmedName = computed(() => companyName.value.trim());

function handleSubmit() {
  if (!trimmedName.value) return;
  emit('start', trimmedName.value);
}
</script>

<template>
  <section class="hero-section">
    <div class="hero-grid container">
      <div class="hero-left">
        <div class="hero-badge">
          <span class="badge-label">Sistema operativo financiero</span>
        </div>

        <h1 class="hero-title">
          El sistema operativo<br>
          financiero para tu PyME
        </h1>

        <p class="hero-copy">
          Registrá tus compras. El sistema calcula márgenes, encuentra fugas
          de dinero y te dice si tu negocio está saludable.
        </p>

        <form class="onboarding-row" @submit.prevent="handleSubmit" novalidate>
          <q-input
            v-model="companyName"
            placeholder="Nombre de tu negocio"
            aria-label="Nombre de tu negocio"
            dark
            filled
            color="primary"
            class="company-input focus-ring"
          >
            <template v-slot:prepend>
              <q-icon name="store" style="color: var(--pq-accent)" aria-hidden="true" />
            </template>
          </q-input>

          <q-btn
            color="primary"
            type="submit"
            class="onboarding-btn"
            :disable="!trimmedName"
          >
            CREAR MI ESPACIO
          </q-btn>
        </form>

        <div class="hero-fine-print">
          Sin complicaciones · Diseñado para la realidad de LATAM
        </div>
      </div>

      <aside class="hero-mayor" aria-label="Datos del producto">
        <div class="mayor-block">
          <div class="mayor-number" aria-label="Ocho industrias precargadas">8</div>
          <div class="mayor-label">Industrias precargadas</div>
        </div>
        <div class="mayor-rule" />
        <div class="mayor-block">
          <div class="mayor-number" aria-label="Diez motores de análisis">10</div>
          <div class="mayor-label">Motores de análisis</div>
        </div>
        <div class="mayor-rule" />
        <div class="mayor-block">
          <div class="mayor-number">24/7</div>
          <div class="mayor-label">Siempre disponible</div>
        </div>
      </aside>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.hero-section {
  min-height: 90dvh;
  display: flex;
  align-items: center;
  padding: clamp(24px, 4vw, 64px) 0;
}

.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 clamp(16px, 4vw, 24px);
  width: 100%;
}

.hero-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 48px;
  align-items: start;

  @media (min-width: 768px) {
    grid-template-columns: 9fr 3fr;
    gap: 24px;
  }
}

.hero-badge {
  margin-bottom: 24px;
}

.badge-label {
  display: inline-block;
  font-family: var(--pq-font-utility);
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-accent);
  padding: 4px 0;
  border-bottom: 1px solid var(--pq-border);
}

.hero-title {
  font-family: var(--pq-font-display);
  font-size: clamp(2.75rem, 6vw, 5rem);
  font-weight: 700;
  line-height: 0.98;
  letter-spacing: -0.035em;
  color: var(--pq-text);
  margin: 0 0 24px;
  text-wrap: balance;
}

.hero-copy {
  font-family: var(--pq-font-body);
  font-size: 16px;
  font-weight: 400;
  line-height: 1.6;
  color: var(--pq-text-muted);
  max-width: 520px;
  margin: 0 0 32px;
  text-wrap: pretty;
}

.onboarding-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 480px;

  @media (min-width: 640px) {
    flex-direction: row;
    align-items: stretch;
    max-width: none;
  }
}

.company-input {
  width: 100%;
  flex: 1 1 auto;

  @media (min-width: 640px) {
    max-width: 320px;
  }

  @media (max-width: 639px) {
    max-width: none;
  }

  :deep(.q-field__control) {
    min-height: 48px;
    height: 48px;
    box-sizing: border-box;
    border-radius: var(--pq-radius-sm);
    background: rgba(0, 0, 0, 0.25);
    border: 1px solid var(--pq-border);
    transition: all var(--pq-motion-base);

    &:hover {
      border-color: rgba(163, 120, 94, 0.4);
      background: rgba(0, 0, 0, 0.35);
    }
  }
}

.onboarding-btn {
  min-height: 48px;
  height: 48px;
  box-sizing: border-box;
  min-width: 180px;
  white-space: nowrap;
  border-radius: var(--pq-radius-sm);

  @media (max-width: 639px) {
    width: 100%;
  }

  @media (min-width: 640px) {
    flex: 0 0 180px;
  }
}

.hero-fine-print {
  font-family: var(--pq-font-body);
  font-size: 12px;
  color: var(--pq-text-subtle);
  margin-top: 16px;
}

.hero-mayor {
  display: flex;
  flex-direction: row;
  gap: 0;
  border-left: 1px solid var(--pq-accent);
  padding-left: 24px;

  @media (max-width: 767px) {
    flex-direction: row;
    border-left: none;
    border-top: 1px solid var(--pq-accent);
    padding-left: 0;
    padding-top: 24px;
    gap: 24px;
  }
}

.mayor-block {
  flex: 1;
  padding: 0 16px 0 0;

  @media (max-width: 767px) {
    padding: 0;
  }
}

.mayor-number {
  font-family: var(--pq-font-utility);
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 500;
  color: var(--pq-accent);
  font-variant-numeric: tabular-nums;
  line-height: 1;
  margin-bottom: 6px;
}

.mayor-label {
  font-family: var(--pq-font-body);
  font-size: 12px;
  color: var(--pq-text-subtle);
  line-height: 1.4;
}

.mayor-rule {
  width: 1px;
  height: 48px;
  background: var(--pq-border);
  align-self: center;
  margin: 0 16px;

  @media (max-width: 767px) {
    width: 1px;
    height: 32px;
  }
}
</style>
