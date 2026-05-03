<template>
  <q-page class="bg-forest-deep text-secondary overflow-x-hidden">
    <!-- Hero Section -->
    <div class="hero-container flex flex-center q-px-md q-py-xl text-center">
      <div class="max-width-800">
        <h1 class="text-h2 font-bold text-primary q-mb-md brand-glow-text">
          Tu negocio en orden, sin complicaciones.
        </h1>
        <p class="text-h5 text-accent text-weight-light q-mb-xl line-height-relaxed">
          Pymeq es la herramienta sencilla para organizar tus cuentas, entender tus gastos y tener el control total de tu empresa desde tu celular.
        </p>
        <q-btn
          label="CREAR MI ESPACIO DE TRABAJO"
          color="primary"
          class="brand-glow q-px-xl q-py-md text-weight-bold no-border-radius"
          size="lg"
          @click="scrollToForm"
          no-caps
        />
      </div>
    </div>

    <!-- Core Features Grid -->
    <div class="features-section q-pa-xl bg-surface-pine shadow-inner">
      <div class="row q-col-gutter-xl items-center justify-center max-width-1200 mx-auto">
        <!-- Manual + QR Input -->
        <div class="col-12 col-md-5 text-center text-md-left">
          <div class="feature-icon q-mb-md">
            <q-icon name="qr_code_scanner" size="4rem" color="primary" />
            <q-icon name="edit_note" size="4rem" color="accent" class="q-ml-sm" />
          </div>
          <h2 class="text-h4 font-bold q-mb-md">Registra facturas QR o recibos a mano.</h2>
          <p class="text-body1 text-accent line-height-relaxed">
            Diseñado para la realidad de tu día a día. Escanea facturas digitales en segundos o anota rápidamente ese recibo que te entregaron por escrito. Nada se pierde, todo queda organizado en un solo lugar.
          </p>
        </div>
        <div class="col-12 col-md-5 flex flex-center">
          <div class="visual-placeholder q-pa-xl flex flex-center shadow-24">
            <q-icon name="receipt_long" size="8rem" color="primary" class="opacity-20" />
            <q-icon name="check_circle" size="3rem" color="primary" class="absolute-bottom-right q-ma-lg" />
          </div>
        </div>
      </div>
    </div>

    <!-- Silent Assistant (Subtle AI) -->
    <div class="ai-section q-pa-xl">
      <div class="row q-col-gutter-xl items-center justify-center reverse-md max-width-1200 mx-auto">
        <div class="col-12 col-md-5 flex flex-center">
          <div class="alert-mockup q-pa-lg bg-dark tight-shadow rounded-borders">
            <div class="row items-center q-mb-sm">
              <q-icon name="error" color="negative" size="sm" class="q-mr-sm" />
              <span class="text-overline text-negative">Alerta de Seguridad</span>
            </div>
            <div class="text-subtitle2 text-secondary">
              Gasto inusual detectado en "Servicios".
              <span class="text-primary block q-mt-xs">+35% respecto al mes pasado</span>
            </div>
          </div>
        </div>
        <div class="col-12 col-md-5 text-center text-md-left">
          <h2 class="text-h4 font-bold q-mb-md">Alertas que cuidan tu dinero.</h2>
          <p class="text-body1 text-accent line-height-relaxed">
            Pymeq aprende de tus gastos y te avisa si algo anda mal: un aumento inusual en un servicio o una factura que parece duplicada. No necesitas ser contador para saber que tu dinero está bien cuidado.
          </p>
        </div>
      </div>
    </div>

    <!-- PWA / Accessibility -->
    <div class="pwa-section q-pa-xl bg-surface-pine shadow-inner">
      <div class="row q-col-gutter-xl items-center justify-center max-width-1200 mx-auto">
        <div class="col-12 col-md-5 text-center text-md-left">
          <h2 class="text-h4 font-bold q-mb-md">Siempre contigo, en cualquier lugar.</h2>
          <p class="text-body1 text-accent line-height-relaxed">
            Instala Pymeq como una aplicación en tu celular. Funciona rápido, es liviano y está listo para que registres un gasto justo en el momento que sucede. Tu oficina está donde tú estés.
          </p>
        </div>
        <div class="col-12 col-md-5 flex flex-center">
          <q-icon name="devices" size="10rem" color="primary" class="opacity-50" />
        </div>
      </div>
    </div>

    <!-- Final Call to Action / Space Creation -->
    <div id="setup-form" class="cta-section q-pa-xl flex flex-center min-height-80vh">
      <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-xl no-border-radius max-width-600 w-100">
        <q-card-section class="text-center">
          <div class="text-h4 font-bold q-mb-md">Empieza a organizar tu negocio hoy mismo.</div>
          <div class="text-subtitle1 text-accent q-mb-lg">Comienza configurando la identidad de tu empresa</div>
        </q-card-section>

        <q-card-section>
          <q-form @submit="startOnboarding" class="q-gutter-y-lg">
            <q-input
              v-model="companyForm.name"
              label="Nombre de la Empresa"
              placeholder="Ej. Mi Abarrotería S.A."
              dark filled color="primary" label-color="accent"
              :rules="[val => !!val || 'El nombre es obligatorio']"
            >
              <template v-slot:prepend><q-icon name="apartment" color="primary" /></template>
            </q-input>

            <div class="q-mt-xl">
              <q-btn
                label="COMENZAR"
                type="submit"
                color="primary"
                class="full-width brand-glow text-weight-bold q-py-md"
                size="lg"
                no-caps
              />
            </div>
          </q-form>
        </q-card-section>

        <q-card-section class="text-center q-pt-none text-accent">
          <div class="text-caption">
            Al registrarte, accedes a un entorno de alta seguridad para tus datos.
          </div>
        </q-card-section>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';
import { useRouter } from 'vue-router';
import { scroll } from 'quasar';

const { getScrollTarget, setVerticalScrollPosition } = scroll;

const authStore = useAuthStore();
const router = useRouter();

const companyForm = reactive({
  name: ''
});

const startOnboarding = () => {
  // Generamos el slug automáticamente pero no lo mostramos
  const slug = companyForm.name
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_-]+/g, '-')
    .replace(/^-+|-+$/g, '');

  authStore.setPendingTenant(companyForm.name, slug);
  void router.push('/register');
};

const scrollToForm = () => {
  const el = document.getElementById('setup-form');
  if (el) {
    const target = getScrollTarget(el);
    const offset = el.offsetTop;
    setVerticalScrollPosition(target, offset, 500);
  }
};
</script>

<style lang="scss" scoped>
.hero-container {
  min-height: 80vh;
}
.max-width-800 { max-width: 800px; }
.max-width-1200 { max-width: 1200px; }
.max-width-600 { max-width: 600px; }
.w-100 { width: 100%; }
.mx-auto { margin-left: auto; margin-right: auto; }
.min-height-80vh { min-height: 80vh; }
.opacity-50 { opacity: 0.5; }
.opacity-20 { opacity: 0.2; }
.line-height-relaxed { line-height: 1.8; }

.visual-placeholder {
  width: 280px;
  height: 280px;
  background: rgba(163, 120, 94, 0.05);
  border: 1px solid rgba(163, 120, 94, 0.1);
  border-radius: 20px;
  position: relative;
}

.no-border-radius {
  border-radius: 0;
}

.brand-glow-text {
  text-shadow: 0 0 20px rgba(163, 120, 94, 0.4);
}

.alert-mockup {
  border-left: 4px solid var(--q-negative);
  width: 100%;
  max-width: 320px;
}

.shadow-inner {
  box-shadow: inset 0 10px 30px rgba(0,0,0,0.5);
}

.reverse-md {
  @media (min-width: 1024px) {
    flex-direction: row-reverse;
  }
}
</style>
