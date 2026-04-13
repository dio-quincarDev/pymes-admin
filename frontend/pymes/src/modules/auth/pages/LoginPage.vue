<template>
  <q-layout view="lHh Lpr lFf">
    <q-page-container>
      <q-page class="flex flex-center bg-forest-deep">
        <div class="login-card-container q-pa-md">
          <!-- Logo & Branding -->
          <div class="text-center q-mb-xl">
            <div class="text-h3 font-bold mesh-text-gradient q-mb-xs">PYMEQ</div>
            <div class="text-subtitle1 text-accent text-weight-light letter-spacing-2">
              INTELLIGENT AUDIT TOOLKIT
            </div>
          </div>

          <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
            <q-card-section class="text-center">
              <div class="text-h6 text-weight-medium q-mb-md">Acceso al Centro de Control</div>
              <div class="text-caption text-accent">Introduce tus credenciales forenses</div>
            </q-card-section>

            <q-card-section>
              <q-form @submit="onLogin" class="q-gutter-md">
                <q-input
                  v-model="loginForm.email"
                  label="Correo Electrónico"
                  dark
                  filled
                  color="primary"
                  label-color="accent"
                  :rules="[val => !!val || 'El email es requerido']"
                >
                  <template v-slot:prepend>
                    <q-icon name="email" color="primary" />
                  </template>
                </q-input>

                <q-input
                  v-model="loginForm.password"
                  label="Contraseña"
                  type="password"
                  dark
                  filled
                  color="primary"
                  label-color="accent"
                  :rules="[val => !!val || 'La contraseña es requerida']"
                >
                  <template v-slot:prepend>
                    <q-icon name="lock" color="primary" />
                  </template>
                </q-input>

                <div class="row items-center justify-between q-mt-sm">
                  <q-checkbox v-model="rememberMe" label="Recordar sesión" dark color="primary" class="text-caption text-accent" />
                  <q-btn flat no-caps label="¿Olvidaste tu contraseña?" color="accent" size="sm" />
                </div>

                <div class="q-mt-xl">
                  <q-btn
                    label="INICIAR SESIÓN"
                    type="submit"
                    color="primary"
                    class="full-width brand-glow text-weight-bold"
                    size="lg"
                    :loading="loading"
                  />
                </div>
              </q-form>
            </q-card-section>

            <q-separator dark class="q-my-lg" label="O CONTINUA CON" />

            <!-- Social Logins -->
            <q-card-section class="row q-gutter-sm justify-center">
              <q-btn
                outline
                color="secondary"
                class="social-btn flex-1"
                @click="loginWithSocial('google')"
              >
                <q-icon name="img:https://cdn.cdnlogo.com/logos/g/35/google-icon.svg" size="xs" class="q-mr-sm" />
                Google
              </q-btn>
              <q-btn
                outline
                color="secondary"
                class="social-btn flex-1"
                @click="loginWithSocial('facebook')"
              >
                <q-icon name="facebook" color="blue-8" size="xs" class="q-mr-sm" />
                Facebook
              </q-btn>
            </q-card-section>

            <q-card-section class="text-center q-pt-none">
              <div class="text-caption text-accent">
                ¿No tienes una cuenta?
                <q-btn flat no-caps label="Crea tu empresa" color="primary" class="q-px-xs" to="/register" />
              </div>
            </q-card-section>
          </q-card>

          <!-- Footer Info -->
          <div class="text-center q-mt-xl text-accent text-caption">
            <q-icon name="security" size="xs" class="q-mr-xs" />
            Encriptación de grado militar AES-256
          </div>
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const $q = useQuasar();
const router = useRouter();

const loading = ref(false);
const rememberMe = ref(false);

const loginForm = reactive({
  email: '',
  password: ''
});

const onLogin = async () => {
  loading.value = true;
  try {
    await authStore.login(loginForm);
    $q.notify({
      type: 'positive',
      message: 'Acceso autorizado',
      caption: 'Bienvenido al Centro de Control',
      position: 'top-right'
    });
    void router.push('/');
  } catch {
    $q.notify({
      type: 'negative',
      message: 'Fallo de autenticación',
      caption: authStore.error || 'Credenciales no reconocidas',
      position: 'top-right'
    });
  } finally {
    loading.value = false;
  }
};

const loginWithSocial = (provider: 'google' | 'facebook') => {
  // Redirección directa al Gateway que maneja OAuth2
  const gatewayUrl = 'http://localhost:8080/api/v1/oauth2/authorization/';
  window.location.href = `${gatewayUrl}${provider}`;
};
</script>

<style lang="scss" scoped>
.login-card-container {
  width: 100%;
  max-width: 450px;
}

.no-border-radius-custom {
  border-radius: 4px;
}

.letter-spacing-2 {
  letter-spacing: 2px;
}

.social-btn {
  text-transform: none;
  border-color: rgba(226, 232, 228, 0.2);
  &:hover {
    background: rgba(113, 131, 127, 0.1);
  }
}

.flex-1 {
  flex: 1;
}

/* Override input background for the theme */
:deep(.q-field--filled .q-field__control) {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}
</style>
