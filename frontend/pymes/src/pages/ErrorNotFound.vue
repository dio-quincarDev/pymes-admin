<template>
  <div class="error-page fullscreen bg-forest-deep text-secondary text-center q-pa-md flex flex-center">
    <div class="fade-in-up">
      <div class="error-code">404</div>
      <div class="text-h5 text-sage-muted q-mb-md">Página no encontrada</div>
      <p class="text-caption text-accent q-mb-lg" style="max-width: 360px">
        El enlace que seguiste puede ser incorrecto o la página fue movida.
      </p>
      <q-btn
        color="primary"
        unelevated
        no-caps
        :to="homeRoute"
        label="Volver al dashboard"
        icon="sym_r_arrow_back"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';

useMeta({ title: '404 — PYMEQ' });

const authStore = useAuthStore();
const homeRoute = computed(() => authStore.isAuthenticated ? '/dashboard' : '/');
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
