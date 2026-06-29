<template>
  <div class="error-page fullscreen bg-dark text-white text-center q-pa-md flex flex-center">
    <div>
      <div class="error-code">500</div>
      <div class="text-h5 text-grey-5 q-mb-lg">Error interno del servidor</div>
      <div class="text-grey-6 q-mb-lg">Algo salió mal. Intenta de nuevo o vuelve al dashboard.</div>
      <div class="row q-gutter-sm justify-center">
        <q-btn
          color="primary"
          unelevated
          label="Reintentar"
          no-caps
          icon="refresh"
          @click="retry"
        />
        <q-btn
          color="grey-7"
          unelevated
          :to="homeRoute"
          label="Volver al dashboard"
          no-caps
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
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.error-code {
  font-size: 28vh;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #e94560 0%, #f39c12 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
</style>
