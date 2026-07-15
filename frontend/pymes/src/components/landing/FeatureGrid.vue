<template>
  <section id="features" ref="target" class="features-section q-pa-lg q-pa-md-xl" :class="{ 'reveal-visible': isVisible }">
    <div class="max-width-1200 mx-auto reveal-up">
      <div class="section-label">HERRAMIENTAS PARA CRECER</div>
      <h2 class="section-title">Control total de forma sencilla.</h2>

      <div class="bento-grid">
        <BaseCard class="bento-item feature-main q-pa-xl" variant="elevated">
          <div class="column full-height justify-between">
            <div>
              <q-icon name="history_edu" size="4rem" style="color: var(--pq-accent)" class="q-mb-md" aria-hidden="true" />
              <h3 class="feature-main-title q-mb-md">Orden Absoluto.</h3>
              <p class="feature-main-copy">
                Tus facturas y recibos en un solo lugar. Ya sea escaneando un QR o anotando a mano,
                nada se escapa. El primer paso para ahorrar es saber en qué gastas.
              </p>
            </div>
            <div class="visual-placeholder q-mt-lg flex flex-center">
               <q-icon name="receipt_long" size="8rem" style="color: var(--pq-accent); opacity: 0.08" aria-hidden="true" />
               <div class="absolute-bottom-right q-ma-lg">
                 <q-icon name="verified" size="3rem" style="color: var(--pq-accent)" aria-hidden="true" />
               </div>
            </div>
          </div>
        </BaseCard>

        <BaseCard class="bento-item feature-ai q-pa-lg" variant="ghost">
           <div class="row items-center q-mb-sm">
              <q-icon name="tips_and_updates" style="color: var(--pq-accent)" size="sm" class="q-mr-sm" aria-hidden="true" />
              <span class="feature-ai-tag">Ahorro Promedio: 18%</span>
            </div>
            <h4 class="feature-card-title q-mb-sm">Alertas que te cuidan</h4>
            <p class="feature-card-copy">
              Te avisamos si un gasto sube demasiado o si hay una factura duplicada.
              Cuidamos tu bolsillo como si fuera el nuestro.
            </p>
            <div class="mockup-alert q-mt-md q-pa-md">
              <div class="row items-center" style="gap: 6px">
                <q-icon name="notifications_active" style="color: var(--pq-warning)" aria-hidden="true" />
                <span class="mockup-alert-text">Gasto inusual detectado</span>
              </div>
            </div>
        </BaseCard>

        <BaseCard class="bento-item feature-pwa q-pa-lg" variant="elevated">
            <q-icon name="smartphone" size="3rem" style="color: var(--pq-accent)" class="q-mb-md" aria-hidden="true" />
            <h4 class="feature-card-title q-mb-sm">Tu oficina móvil.</h4>
            <p class="feature-card-copy">
              Lleva el control de tu empresa en el bolsillo. Funciona rápido, incluso sin internet.
            </p>
        </BaseCard>

        <BaseCard class="bento-item feature-security q-pa-lg" variant="outlined">
            <h4 class="feature-card-title q-mb-sm">Transparencia Total</h4>
            <p class="feature-card-copy-sm">
              Datos cifrados y seguros. Tú eres el único dueño de tu información financiera.
            </p>
            <div class="q-mt-sm row" style="gap: 8px">
              <q-icon name="lock" style="color: var(--pq-accent)" aria-hidden="true" />
              <q-icon name="shield" style="color: var(--pq-text-muted)" aria-hidden="true" />
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

.section-label {
  font-family: 'Satoshi', sans-serif;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--pq-accent);
  text-align: center;
  margin-bottom: 8px;
}

.section-title {
  font-family: 'Geist', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--pq-text);
  text-align: center;
  margin: 0 0 48px;

  @media (min-width: 768px) {
    font-size: 28px;
  }
}

/* --------------------------------------------------
   Reveal
-------------------------------------------------- */
.reveal-up {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.reveal-visible .reveal-up {
  opacity: 1;
  transform: translateY(0);
}

/* --------------------------------------------------
   Bento Grid
-------------------------------------------------- */
.bento-grid {
  display: grid;
  grid-template-columns: 1fr;
  grid-auto-rows: auto;
  gap: 24px;

  @media (min-width: 600px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 32px;
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

/* --------------------------------------------------
   Typography
-------------------------------------------------- */
.feature-main-title {
  font-family: 'Geist', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--pq-text);
  margin: 0;
}

.feature-main-copy {
  font-family: 'Satoshi', sans-serif;
  font-size: 14px;
  line-height: 1.6;
  color: var(--pq-text-muted);
  margin: 0;
}

.feature-card-title {
  font-family: 'Geist', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: var(--pq-text);
  margin: 0;
}

.feature-card-copy {
  font-family: 'Satoshi', sans-serif;
  font-size: 14px;
  line-height: 1.5;
  color: var(--pq-text-muted);
  margin: 0;
}

.feature-card-copy-sm {
  font-family: 'Satoshi', sans-serif;
  font-size: 12px;
  line-height: 1.5;
  color: var(--pq-text-muted);
  margin: 0;
}

.feature-ai-tag {
  font-family: 'Satoshi', sans-serif;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--pq-accent);
}

/* --------------------------------------------------
   Visual placeholder
-------------------------------------------------- */
.visual-placeholder {
  height: 180px;
  background: rgba(200, 150, 62, 0.03);
  border: 1px dashed rgba(200, 150, 62, 0.12);
  border-radius: var(--pq-radius-md);
  position: relative;
}

/* --------------------------------------------------
   Mockup alert
-------------------------------------------------- */
.mockup-alert {
  background: var(--pq-elevated);
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-sm);
}

.mockup-alert-text {
  font-family: 'Satoshi', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: var(--pq-text);
}
</style>
