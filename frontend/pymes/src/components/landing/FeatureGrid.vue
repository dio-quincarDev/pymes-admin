<template>
  <section id="features" ref="target" class="features-section q-pa-lg q-pa-md-xl" :class="{ 'reveal-visible': isVisible }">
    <div class="max-width-1200 mx-auto reveal-up">
      <div class="text-overline text-primary text-center q-mb-sm">HERRAMIENTAS PARA CRECER</div>
      <h2 class="text-h4 text-center font-bold q-mb-xl">Control total de forma sencilla.</h2>

      <div class="bento-grid">
        <BaseCard class="bento-item feature-main q-pa-xl" variant="elevated">
          <div class="column full-height justify-between">
            <div>
              <q-icon name="history_edu" size="4rem" color="primary" class="q-mb-md" />
              <h3 class="text-h4 font-bold q-mb-md">Orden Absoluto.</h3>
              <p class="text-body1 text-accent line-height-relaxed">
                Tus facturas y recibos en un solo lugar. Ya sea escaneando un QR o anotando a mano,
                nada se escapa. El primer paso para ahorrar es saber en qué gastas.
              </p>
            </div>
            <div class="visual-placeholder q-mt-lg flex flex-center">
               <q-icon name="receipt_long" size="8rem" color="primary" class="opacity-10" />
               <div class="absolute-bottom-right q-ma-lg">
                 <q-icon name="verified" size="3rem" color="primary" class="brand-glow" />
               </div>
            </div>
          </div>
        </BaseCard>

        <BaseCard class="bento-item feature-ai q-pa-lg" variant="ghost">
           <div class="row items-center q-mb-sm">
              <q-icon name="tips_and_updates" color="primary" size="sm" class="q-mr-sm" />
              <span class="text-overline text-primary">Ahorro Promedio: 18%</span>
            </div>
            <h4 class="text-h5 font-bold q-mb-sm">Alertas que te cuidan</h4>
            <p class="text-body2 text-accent">
              Te avisamos si un gasto sube demasiado o si hay una factura duplicada.
              Cuidamos tu bolsillo como si fuera el nuestro.
            </p>
            <div class="mockup-alert q-mt-md q-pa-md bg-dark border-light radius-sm">
              <div class="row items-center gap-xs">
                <q-icon name="notifications_active" color="warning" />
                <span class="text-caption text-weight-bold">Gasto inusual detectado</span>
              </div>
            </div>
        </BaseCard>

        <BaseCard class="bento-item feature-pwa q-pa-lg" variant="elevated">
            <q-icon name="smartphone" size="3rem" color="primary" class="q-mb-md" />
            <h4 class="text-h5 font-bold q-mb-sm">Tu oficina móvil.</h4>
            <p class="text-body2 text-accent">
              Lleva el control de tu empresa en el bolsillo. Funciona rápido, incluso sin internet.
            </p>
        </BaseCard>

        <BaseCard class="bento-item feature-security q-pa-lg" variant="outlined">
            <h4 class="text-h6 font-bold q-mb-sm">Transparencia Total</h4>
            <p class="text-caption text-accent">
              Datos cifrados y seguros. Tú eres el único dueño de tu información financiera.
            </p>
            <div class="q-mt-sm row gap-xs">
              <q-icon name="lock" color="primary" />
              <q-icon name="shield" color="accent" />
            </div>
        </BaseCard>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import BaseCard from 'src/components/base/BaseCard.vue';
import { useScrollReveal } from 'src/composables/useScrollReveal';

const { target, isVisible } = useScrollReveal();
</script>

<style lang="scss" scoped>
.max-width-1200 { max-width: 1200px; }
.mx-auto { margin-left: auto; margin-right: auto; }
.line-height-relaxed { line-height: 1.6; }
.gap-xs { gap: 4px; }

.reveal-up {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.reveal-visible .reveal-up {
  opacity: 1;
  transform: translateY(0);
}

.bento-grid {
  display: grid;
  grid-template-columns: 1fr;
  grid-auto-rows: auto;
  gap: $pq-space-md;

  @media (min-width: 600px) {
    grid-template-columns: repeat(2, 1fr);
    gap: $pq-space-lg;
  }

  @media (min-width: 1024px) {
    grid-template-columns: repeat(4, 1fr);
    grid-template-rows: repeat(2, 320px);
  }
}

.feature-main {
  grid-column: span 1;

  @media (min-width: 600px) {
    grid-column: span 2;
    grid-row: span 2;
  }
}

.feature-ai {
  grid-column: span 1;

  @media (min-width: 600px) {
    grid-column: span 2;
  }
}

.feature-pwa, .feature-security {
  grid-column: span 1;
}

.visual-placeholder {
  height: 180px;
  background: rgba(163, 120, 94, 0.03);
  border: 1px dashed rgba(163, 120, 94, 0.1);
  border-radius: $pq-radius-md;
  position: relative;
}

.border-light {
  border: 1px solid rgba(113, 131, 127, 0.1);
}

.opacity-10 { opacity: 0.1; }
</style>
