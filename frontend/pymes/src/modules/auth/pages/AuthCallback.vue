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
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store';
import { tenantService } from '../services/tenant.service';
import { useQuasar } from 'quasar';
import { storeToRefs } from 'pinia';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { pendingTenant } = storeToRefs(authStore);
const $q = useQuasar();

const statusMessage = ref('Preparando tu Toolkit de Auditoría');

onMounted(async () => {
  const token = route.query.token as string;
  const refreshToken = route.query.refresh_token as string;

  if (token && refreshToken) {
    try {
      // 1. Guardar tokens y obtener perfil
      await authStore.handleOAuthCallback(token, refreshToken);
      
      // 2. Verificar si el usuario tiene un tenant activo
      // En el JWT que viene del OAuth2SuccessHandler, el tenantId puede ser null si es nuevo
      const user = authStore.user;
      
      // Si no tenemos empresa activa y hay una pendiente en el registro previo
      if (user && !authStore.accessToken?.includes('tenantId') && pendingTenant.value) {
        statusMessage.value = 'Configurando tu nuevo espacio de trabajo...';
        
        try {
          await tenantService.createTenant({
            name: pendingTenant.value.name,
            slug: pendingTenant.value.slug
          });
          
          // Limpiar el tenant pendiente tras creación exitosa
          authStore.clearPendingTenant();
          
          $q.notify({
            type: 'positive',
            message: 'Espacio Creado',
            caption: 'Tu empresa ha sido vinculada a tu cuenta de Google',
          });

          // Re-loguear o refrescar para obtener un token con el tenantId nuevo
          // Por ahora, refrescamos el perfil completo
          await authStore.fetchCurrentUser();
        } catch (tenantErr) {
          console.error('Error al crear tenant automático:', tenantErr);
        }
      }

      $q.notify({
        type: 'positive',
        message: 'Bienvenido a Pymeq',
        position: 'top-right'
      });

      void router.push('/dashboard');
    } catch (error) {
      console.error('Error en el callback de auth:', error);
      void router.push('/login');
    }
  } else {
    void router.push('/login');
  }
});
</script>
