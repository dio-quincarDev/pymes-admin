<template>
  <q-page class="flex flex-center bg-forest-deep">
    <div class="options-container q-pa-md text-center">
      <div class="text-h4 font-bold text-primary q-mb-md">Elige cómo continuar</div>
      <div class="text-subtitle1 text-accent q-mb-xl">
        Configurando el espacio para: <strong>{{ pendingTenant?.name || 'Tu Empresa' }}</strong>
      </div>

      <div class="row q-col-gutter-lg justify-center">
        <!-- Opción Email -->
        <div class="col-12 col-sm-5">
          <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg full-height flex flex-center cursor-pointer hover-scale" @click="goToRegister">
            <q-card-section class="text-center">
              <q-icon name="email" size="lg" color="primary" class="q-mb-md" />
              <div class="text-h6">Registrarse con Email</div>
              <div class="text-caption text-accent q-mt-sm">Crea una cuenta nativa en Pymeq</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Opción Google -->
        <div class="col-12 col-sm-5">
          <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg full-height flex flex-center cursor-pointer hover-scale" @click="loginWithGoogle">
            <q-card-section class="text-center">
              <q-icon name="ion-logo-google" size="lg" color="red" class="q-mb-md" />
              <div class="text-h6">Continuar con Google</div>
              <div class="text-caption text-accent q-mt-sm">Acceso rápido y seguro</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <div class="q-mt-xl">
        <q-btn flat label="Volver a editar empresa" color="accent" icon="sym_r_arrow_back" @click="router.push('/')" no-caps />
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { useAuthStore } from '../store';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const { pendingTenant } = storeToRefs(authStore);
const router = useRouter();

const goToRegister = () => {
  void router.push('/register');
};

const loginWithGoogle = () => {
  // El gateway maneja la redirección a OAuth2
  const apiUrl = import.meta.env.VITE_API_URL?.trim();
  const gatewayBase = (apiUrl && apiUrl !== '') ? apiUrl.replace('/api/v1', '') : window.location.origin;
  window.location.href = `${gatewayBase}/oauth2/authorization/google`;
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
