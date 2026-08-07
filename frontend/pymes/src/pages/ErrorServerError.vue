<template>
  <div class="error-page fullscreen bg-forest-deep text-secondary text-center q-pa-md flex flex-center">
    <div class="fade-in-up">
      <div class="error-code">500</div>
      <div class="text-h5 text-sage-muted q-mb-md">Error interno del servidor</div>
      <p class="text-caption text-accent q-mb-lg" style="max-width: 360px">
        Algo salió mal. Nuestros logs ya registraron el incidente.
        Intenta de nuevo o vuelve al inicio.
      </p>
      <div class="row q-gutter-sm justify-center">
        <q-btn
          color="primary"
          unelevated
          no-caps
          label="Reintentar"
          icon="refresh"
          @click="retry"
        />
        <q-btn
          color="dark"
          unelevated
          no-caps
          :to="homeRoute"
          label="Volver al dashboard"
          icon="arrow_back"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';

useMeta({ title: '500 — PYMEQ' });

const router = useRouter();
const authStore = useAuthStore();
const homeRoute = computed(() => authStore.isAuthenticated ? '/dashboard' : '/');

function retry() {
  router.go(0);
}
</script>

<style scoped lang="scss">
.error-page {
  background: #0B1210;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(ellipse at 50% 0%, rgba(163, 120, 94, 0.08) 0%, transparent 70%);
    pointer-events: none;
  }
}

.error-code {
  font-size: 28vh;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #A3785E 0%, #C5A059 50%, #A3785E 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.03em;
}
</style>
