<template>
  <section class="hero-section flex flex-center q-px-md">
    <div class="hero-content text-center stagger-children">
      <div class="hero-badge q-mb-lg">
        <span class="text-overline text-primary border-light q-px-md q-py-xs radius-full text-weight-bold">
          ⚡ GESTIÓN FINANCIERA INTELIGENTE
        </span>
      </div>

      <h1 class="text-h2 font-bold q-mb-md">
        Organiza tu empresa y <br />
        <span class="mesh-text-gradient">toma el control.</span>
      </h1>

      <p class="text-h5 text-accent text-weight-light q-mb-xl max-width-700 mx-auto line-height-relaxed">
        Pymeq es tu aliado estratégico para entender tus flujos de caja y optimizar cada centavo de tu negocio.
      </p>

      <div class="onboarding-container row justify-center q-mt-xl gap-sm">
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
            <q-icon name="store" color="primary" />
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

      <div class="text-caption text-accent q-mt-md opacity-50">
        Sin complicaciones • Diseñado para la realidad de LATAM
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
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 40%;
    left: 50%;
    width: 800px;
    height: 800px;
    background: radial-gradient(circle, rgba(163, 120, 94, 0.07) 0%, transparent 70%);
    transform: translate(-50%, -50%);
    pointer-events: none;
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 900px;
}

.max-width-700 { max-width: 700px; }
.mx-auto { margin-left: auto; margin-right: auto; }
.line-height-relaxed { line-height: 1.6; }

.onboarding-container {
  .company-input {
    width: 100%;
    max-width: 400px;

    :deep(.q-field__control) {
      height: 64px;
      border-radius: $pq-radius-sm;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(113, 131, 127, 0.2);
      transition: all 0.2s ease;

      &:hover {
        border-color: rgba(163, 120, 94, 0.5);
        background: rgba(255, 255, 255, 0.08);
      }
    }
  }

  .onboarding-btn {
    height: 64px;
    min-width: 200px;
  }
}

.border-light {
  border: 1px solid rgba(113, 131, 127, 0.1);
}

.opacity-50 { opacity: 0.5; }
</style>
