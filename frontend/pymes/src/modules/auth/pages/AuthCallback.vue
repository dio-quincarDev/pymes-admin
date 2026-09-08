<template>
  <div class="auth-callback flex flex-center" style="min-height: 100vh; background: var(--pq-background);">
    <!-- ponytail: whitelabel duplicate tenant -->
    <q-card v-if="showDuplicate" flat class="q-pa-xl text-center" style="max-width: 420px; background: var(--pq-surface); border: 1px solid var(--pq-border);">
      <q-img src="/icons/logo.svg" width="48px" height="48px" class="q-mb-md" style="margin: 0 auto;" />
      <div class="text-h6" style="color: var(--pq-text);">Este nombre ya está registrado</div>
      <div class="text-body2 q-mt-sm" style="color: var(--pq-text-muted);">El negocio que intentaste crear ya existe. Elegí otro nombre o iniciá sesión en tu espacio existente.</div>
      <div class="q-gutter-sm q-mt-lg">
        <q-btn label="Elegir otro nombre" color="primary" unelevated to="/" />
        <q-btn label="Iniciar sesión" flat color="accent" to="/login" />
      </div>
    </q-card>
    <div v-else class="text-center">
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
import { parseBackendError } from 'src/utils/errors';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const $q = useQuasar();
const showDuplicate = ref(false);

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
  // ponytail: duplicate tenant whitelabel — check before stripping hash query
  const errorCode = (route.query.error as string) || new URLSearchParams(window.location.hash.split('?')[1] || '').get('error');
  if (errorCode === 'TNT003') {
    clearInterval(rotateMessage);
    showDuplicate.value = true;
    return;
  }

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
      const parsed = parseBackendError(error);
      if (parsed.code === 'TNT003' || parsed.status === 409) {
        clearInterval(rotateMessage);
        showDuplicate.value = true;
        return;
      }
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
