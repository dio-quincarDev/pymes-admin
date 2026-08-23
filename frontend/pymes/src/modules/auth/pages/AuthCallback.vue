<template>
  <div class="auth-callback flex flex-center" style="min-height: 100vh; background: var(--pq-background);">
    <div class="text-center">
      <q-spinner color="accent" size="2.5em" />
      <div class="text-h6 q-mt-md" style="color: var(--pq-text);">Sincronizando Identidad Pymeq...</div>
      <div class="text-caption q-mt-xs" style="color: var(--pq-text-muted);" role="status" aria-live="polite">{{ statusMessage }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useMeta } from 'quasar';

useMeta({ title: 'Sincronizando — PYMEQ' });
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import { api } from 'src/boot/axios';
import { setupService } from 'src/modules/core/services/setup.service';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const $q = useQuasar();

const statusMessages = [
  'Sincronizando identidad...',
  'Configurando tu espacio de trabajo...',
  'Preparando el panel de control...',
];
const statusMessage = ref(statusMessages[0]);
let msgIndex = 0;

const rotateMessage = setInterval(() => {
  msgIndex = (msgIndex + 1) % statusMessages.length;
  statusMessage.value = statusMessages[msgIndex];
}, 2000);

onMounted(async () => {
  const code = (route.query.code as string) || (new URLSearchParams(window.location.search).get('code') as string);

  window.history.replaceState({}, '', window.location.pathname + window.location.hash.replace(/\?.*$/, ''));

  if (code) {
    try {
      const { data } = await api.post('/auth/exchange', { code });
      const authData = data.data;
      if (!authData?.accessToken) {
        throw new Error('No se recibieron tokens');
      }
      await authStore.handleOAuthCallback(authData.accessToken, authData.refreshToken, authData.user, authData.activeTenant);

      authStore.clearPendingTenant();

      const tenantId = authStore.user?.tenantId;
      if (tenantId) {
        try {
          const { data: setup } = await setupService.get(tenantId);
          if (!setup.onboardingCompleted) {
            clearInterval(rotateMessage);
            void router.push('/onboarding');
            return;
          }
        } catch {
          // ponytail: si falla, ir al dashboard normalmente
        }
      } else {
        // Sin workspace (OAuth2 directo desde login, sin intent) → crear espacio de trabajo
        clearInterval(rotateMessage);
        void router.push('/onboarding');
        return;
      }

      $q.notify({
        type: 'positive',
        message: 'Acceso autorizado',
        caption: 'Bienvenido al Centro de Control',
        position: 'top-right'
      });

      clearInterval(rotateMessage);
      void router.push('/dashboard');
    } catch (error) {
      console.error('Error en el callback de auth:', error);
      $q.notify({
        type: 'negative',
        message: 'No se pudo completar el acceso. Intenta de nuevo.',
      });
      clearInterval(rotateMessage);
      void router.push('/login');
    }
  } else {
    clearInterval(rotateMessage);
    void router.push('/login');
  }
});
</script>
