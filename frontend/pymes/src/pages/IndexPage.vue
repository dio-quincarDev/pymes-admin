<template>
  <q-page class="index-page bg-forest-deep text-secondary">
    <!-- Hero Section: Practical Education -->
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

        <!-- Onboarding Group -->
        <div class="onboarding-container row justify-center q-mt-xl gap-sm">
          <q-input
            v-model="companyForm.name"
            placeholder="Nombre de tu negocio"
            dark
            filled
            color="primary"
            class="company-input focus-ring"
            @keyup.enter="startOnboarding"
          >
            <template v-slot:prepend>
              <q-icon name="store" color="primary" />
            </template>
          </q-input>
          
          <BaseButton 
            size="lg" 
            class="onboarding-btn"
            @click="startOnboarding"
          >
            CREAR MI ESPACIO
          </BaseButton>
        </div>
        
        <div class="text-caption text-accent q-mt-md opacity-50">
          Sin complicaciones • Diseñado para la realidad de LATAM
        </div>
      </div>
    </section>

    <!-- Bento Features Grid -->
    <section id="features" class="features-section q-pa-lg q-pa-md-xl">
      <div class="max-width-1200 mx-auto">
        <div class="text-overline text-primary text-center q-mb-sm">HERRAMIENTAS PARA CRECER</div>
        <h2 class="text-h4 text-center font-bold q-mb-xl">Control total de forma sencilla.</h2>
        
        <div class="bento-grid">
          <!-- Feature 1: Scan & Manual -->
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

          <!-- Feature 2: Silent Assistant -->
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

          <!-- Feature 3: PWA / Mobile -->
          <BaseCard class="bento-item feature-pwa q-pa-lg" variant="elevated">
              <q-icon name="smartphone" size="3rem" color="primary" class="q-mb-md" />
              <h4 class="text-h5 font-bold q-mb-sm">Tu oficina móvil.</h4>
              <p class="text-body2 text-accent">
                Lleva el control de tu empresa en el bolsillo. Funciona rápido, incluso sin internet.
              </p>
          </BaseCard>

          <!-- Feature 4: Security -->
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

    <!-- Social Proof / Sectors -->
    <section id="trust" class="trust-section q-pa-xl text-center">
      <div class="text-overline text-accent opacity-70 q-mb-xl">CONFIABLE PARA CUALQUIER SECTOR</div>
      <div class="row justify-center gap-lg gap-md-xl items-center opacity-50 filter-grayscale text-weight-bolder">
        <div class="text-subtitle1 letter-spacing-2">COMERCIO</div>
        <div class="text-subtitle1 letter-spacing-2">SERVICIOS</div>
        <div class="text-subtitle1 letter-spacing-2">TALLERES</div>
        <div class="text-subtitle1 letter-spacing-2">EMPRENDEDORES</div>
      </div>
    </section>
  </q-page>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';
import { useRouter } from 'vue-router';
import BaseButton from 'src/components/base/BaseButton.vue';
import BaseCard from 'src/components/base/BaseCard.vue';

const authStore = useAuthStore();
const router = useRouter();

const companyForm = reactive({
  name: ''
});

const startOnboarding = () => {
  if (!companyForm.name.trim()) return;

  const slug = companyForm.name
    .toLowerCase()
    .trim()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '') // Remove accents
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_-]+/g, '-')
    .replace(/^-+|-+$/g, '');

  authStore.setPendingTenant(companyForm.name, slug);
  void router.push('/register');
};
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
.max-width-1200 { max-width: 1200px; }
.mx-auto { margin-left: auto; margin-right: auto; }
.line-height-relaxed { line-height: 1.6; }
.letter-spacing-2 { letter-spacing: 2px; }

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

.bento-grid {
  display: grid;
  grid-template-columns: 1fr; // Mobile First: 1 columna por defecto
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

.filter-grayscale {
  filter: grayscale(1);
}

.opacity-10 { opacity: 0.1; }
.opacity-20 { opacity: 0.2; }
.opacity-50 { opacity: 0.5; }
.opacity-70 { opacity: 0.7; }

.gap-xs { gap: 4px; }
</style>
