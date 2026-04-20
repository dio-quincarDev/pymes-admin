<template>
  <q-page class="flex flex-center bg-forest-deep">
    <div class="options-container q-pa-md text-center">
      <div class="text-h4 font-bold text-primary q-mb-md">Elige cómo continuar</div>
      <div class="text-subtitle1 text-accent q-mb-xl">
        Configurando el espacio para: <strong>{{ pendingTenant?.name || 'Tu Empresa' }}</strong>
      </div>

      <div class="row q-col-gutter-lg justify-center">
        <!-- Opción Email -->
        <div class="col-12 col-sm-4">
          <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg full-height flex flex-center cursor-pointer hover-scale" @click="goToRegister">
            <q-card-section class="text-center">
              <q-icon name="email" size="lg" color="primary" class="q-mb-md" />
              <div class="text-h6">Email</div>
              <div class="text-caption text-accent q-mt-sm">Cuenta nativa</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Opción Google -->
        <div class="col-12 col-sm-4">
          <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg full-height flex flex-center cursor-pointer hover-scale" @click="loginWithGoogle">
            <q-card-section class="text-center">
              <q-icon name="ion-logo-google" size="lg" color="red" class="q-mb-md" />
              <div class="text-h6">Google</div>
              <div class="text-caption text-accent q-mt-sm">Acceso rápido</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Opción Facebook -->
        <div class="col-12 col-sm-4">
          <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg full-height flex flex-center cursor-pointer hover-scale" @click="loginWithFacebook">
            <q-card-section class="text-center">
              <q-icon name="ion-logo-facebook" size="lg" color="blue" class="q-mb-md" />
              <div class="text-h6">Facebook</div>
              <div class="text-caption text-accent q-mt-sm">Continuar con Meta</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <div class="q-mt-xl">
        <q-btn flat label="Volver a editar empresa" color="accent" icon="arrow_back" @click="router.push('/')" no-caps />
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { useAuthStore } from '../store';
import { useRouter } from 'vue-router';
import { openURL, useQuasar } from 'quasar';
import { authService } from '../services/auth.service';

const authStore = useAuthStore();
const { pendingTenant } = storeToRefs(authStore);
const router = useRouter();
const $q = useQuasar();

const goToRegister = () => {
  void router.push('/register');
};

const loginWithGoogle = async () => {
  await handleOAuth2Login('google');
};

const loginWithFacebook = async () => {
  await handleOAuth2Login('facebook');
};

const handleOAuth2Login = async (provider: 'google' | 'facebook') => {
  try {
    let url = `http://localhost:8080/oauth2/authorization/${provider}`;

    // Si hay una empresa pendiente, creamos el intent
    if (pendingTenant.value?.name && pendingTenant.value?.slug) {
      $q.loading.show({ message: 'Preparando registro...' });
      const { data } = await authService.createOAuth2Intent({
        companyName: pendingTenant.value.name,
        companySlug: pendingTenant.value.slug,
      });

      if (data.data?.intentId) {
        url += `?state=${data.data.intentId}`;
      }
    }

    openURL(url);
  } catch (error) {
    console.error(`Error initiating ${provider} login:`, error);
    $q.notify({
      type: 'negative',
      message: 'No se pudo iniciar el proceso. Intenta de nuevo.',
    });
  } finally {
    $q.loading.hide();
  }
};
</script>

<style lang="scss" scoped>
.options-container {
  width: 100%;
  max-width: 800px;
}
.hover-scale {
  transition: transform 0.3s ease;
  &:hover {
    transform: scale(1.02);
    border: 1px solid var(--q-primary);
  }
}
</style>
