<template>
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
          <q-btn flat no-caps label="¿Olvidaste tu contraseña?" color="accent" size="sm" to="/forgot-password" />
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
    <q-card-section class="row justify-center">
      <q-btn
        outline
        color="secondary"
        class="social-btn full-width"
        @click="loginWithSocial('google')"
      >
        <q-icon name="img:https://cdn.cdnlogo.com/logos/g/35/google-icon.svg" size="xs" class="q-mr-sm" />
        Google
      </q-btn>
    </q-card-section>

    <q-card-section class="text-center q-pt-none">
      <div class="text-caption text-accent">
        ¿No tienes una empresa?
        <q-btn flat no-caps label="Crea tu espacio de trabajo" color="primary" class="q-px-xs" to="/" />
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { authService } from '../services/auth.service';

const authStore = useAuthStore();
const $q = useQuasar();
const router = useRouter();

const loading = ref(false);
const rememberMe = ref(localStorage.getItem('pymeq_remember') === 'true');

const loginForm = reactive({
  email: localStorage.getItem('pymeq_email') || '',
  password: ''
});

const onLogin = async () => {
  loading.value = true;
  try {
    if (rememberMe.value) {
      localStorage.setItem('pymeq_remember', 'true');
      localStorage.setItem('pymeq_email', loginForm.email);
    } else {
      localStorage.removeItem('pymeq_remember');
      localStorage.removeItem('pymeq_email');
    }

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

const loginWithSocial = async (provider: 'google') => {
  try {
    let url = `http://localhost:8080/oauth2/authorization/${provider}`;

    // Si hay una empresa pendiente en el store, creamos un Intent primero
    if (authStore.pendingTenant?.name && authStore.pendingTenant?.slug) {
      $q.loading.show({ message: 'Preparando entorno de empresa...' });
      
      const response = await authService.createOAuth2Intent({
        companyName: authStore.pendingTenant.name,
        companySlug: authStore.pendingTenant.slug,
      });

      // El backend devuelve el intentId que usaremos como 'state' para OAuth2
      const intentId = response.data?.data?.intentId;
      if (intentId) {
        url += `?state=${intentId}`;
      }
    }

    window.location.href = url;
  } catch (error) {
    console.error(`Error al iniciar login con ${provider}:`, error);
    $q.notify({
      type: 'negative',
      message: 'No se pudo iniciar el proceso social. Intenta de nuevo.',
      position: 'top-right'
    });
  } finally {
    $q.loading.hide();
  }
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
