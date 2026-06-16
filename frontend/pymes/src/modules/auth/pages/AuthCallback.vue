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

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const $q = useQuasar();

const statusMessage = ref('Preparando tu Toolkit de Auditoría');

onMounted(async () => {
  const token = route.query.token as string;
  const refreshToken = route.query.refresh_token as string;

  if (token && refreshToken) {
    try {
      // 1. Guardar tokens y obtener perfil
      await authStore.handleOAuthCallback(token, refreshToken);
      
      // 2. Limpiar el tenant pendiente tras login/registro exitoso
      // El backend ya manejó la creación del tenant si existía un intent (vía state)
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
