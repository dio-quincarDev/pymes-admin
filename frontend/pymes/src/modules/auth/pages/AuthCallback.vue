<template>
  <q-page class="flex flex-center bg-forest-deep text-secondary">
    <div class="text-center">
      <q-spinner-grid color="primary" size="4em" class="brand-glow" />
      <div class="text-h6 q-mt-md text-primary">Sincronizando Identidad Pymeq...</div>
      <div class="text-caption text-accent">{{ statusMessage }}</div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useMeta } from 'quasar';

useMeta({ title: 'Sincronizando — PYMEQ' });
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import { api } from 'src/boot/axios';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const $q = useQuasar();

const statusMessage = ref('Preparando tu Toolkit de Auditoría');

onMounted(async () => {
  const code = route.query.code as string;

  window.history.replaceState({}, '', window.location.pathname + window.location.hash.replace(/\?.*$/, ''));

  if (code) {
    try {
      const { data } = await api.post('/auth/exchange', { code });
      const authData = data.data;
      if (!authData?.accessToken) {
        throw new Error('No se recibieron tokens');
      }
      await authStore.handleOAuthCallback(authData.accessToken, authData.refreshToken);

      authStore.clearPendingTenant();

      $q.notify({
        type: 'positive',
        message: 'Acceso autorizado',
        caption: 'Bienvenido al Centro de Control',
        position: 'top-right'
      });

      void router.push('/dashboard');
    } catch (error) {
      console.error('Error en el callback de auth:', error);
      $q.notify({
        type: 'negative',
        message: 'No se pudo completar el acceso. Intenta de nuevo.',
      });
      void router.push('/login');
    }
  } else {
    void router.push('/login');
  }
});
</script>
