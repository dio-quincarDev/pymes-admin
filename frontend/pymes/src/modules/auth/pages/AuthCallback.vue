<template>
  <q-layout view="lHh Lpr lFf">
    <q-page-container>
      <q-page class="flex flex-center bg-forest-deep text-secondary">
        <div class="text-center">
          <q-spinner-grid color="primary" size="4em" class="brand-glow" />
          <div class="text-h6 q-mt-md mesh-text-gradient">Sincronizando Identidad Pymeq...</div>
          <div class="text-caption text-accent">Preparando tu Toolkit de Auditoría</div>
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const $q = useQuasar();

onMounted(async () => {
  const token = route.query.token as string;
  const refreshToken = route.query.refresh_token as string;

  if (token && refreshToken) {
    try {
      // 1. Guardar tokens y obtener perfil de usuario
      await authStore.handleOAuthCallback(token, refreshToken);
      
      $q.notify({
        type: 'positive',
        message: 'Bienvenido a Pymeq',
        caption: 'Acceso concedido exitosamente',
        position: 'top-right'
      });

      // 2. Redirigir al Dashboard
      void router.push('/');
    } catch (error) {
      console.error('Error en el callback de auth:', error);
      $q.notify({
        type: 'negative',
        message: 'Error de Sincronización',
        caption: 'No se pudo validar la identidad',
        position: 'top-right'
      });
      void router.push('/login');
    }
  } else {
    // Si no hay tokens en la URL, algo salió mal
    void router.push('/login');
  }
});
</script>

<style lang="scss" scoped>
.bg-forest-deep {
  background-color: #0B1210;
}
</style>
