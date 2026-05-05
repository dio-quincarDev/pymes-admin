<template>
  <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
    <q-card-section class="text-center">
      <div class="text-h6 text-weight-medium q-mb-md">Acceso al Centro de Control</div>
      <div class="text-caption text-accent">Introduce tus credenciales forenses</div>
    </q-card-section>

    <q-card-section>
      <q-input
        v-model="loginForm.email"
        label="Correo Electrónico"
        dark
        filled
        color="primary"
        label-color="accent"
      >
        <template v-slot:prepend>
          <q-icon name="email" color="primary" />
        </template>
      </q-input>

      <q-input
        v-model="loginForm.password"
        label="Contraseña"
        :type="showPassword ? 'text' : 'password'"
        dark
        filled
        color="primary"
        label-color="accent"
      >
        <template v-slot:prepend>
          <q-icon name="lock" color="primary" />
        </template>
        <template v-slot:append>
          <q-icon
            :name="showPassword ? 'visibility' : 'visibility_off'"
            class="cursor-pointer"
            color="primary"
            @click="showPassword = !showPassword"
          />
        </template>
      </q-input>

      <div class="row items-center justify-between q-mt-sm">
        <q-checkbox v-model="rememberMe" label="Recordar sesión" dark color="primary" class="text-caption text-accent" />
        <q-btn flat no-caps label="¿Olvidaste tu contraseña?" color="accent" size="sm" to="/forgot-password" />
      </div>

      <q-btn
        label="INICIAR SESIÓN"
        type="button"
        color="primary"
        class="full-width brand-glow text-weight-bold q-mt-xl"
        size="lg"
        :loading="loading"
        @click="handleLoginClick"
      />
    </q-card-section>

    <div class="relative-position q-my-lg">
      <q-separator dark />
      <div class="absolute-center bg-surface-pine q-px-sm text-caption text-accent text-weight-bold">
        O CONTINUA CON
      </div>
    </div>

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
const showPassword = ref(false);

const loginForm = reactive({
  email: localStorage.getItem('pymeq_email') || '',
  password: ''
});

const handleLoginClick = async () => {
  if (!loginForm.email || !loginForm.password) {
    $q.notify({
      type: 'warning',
      message: 'Completa todos los campos',
      caption: 'Email y contraseña son requeridos',
      position: 'top-right'
    });
    return;
  }

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
  } catch (err: unknown) {
    const error = err as { response?: { status?: number; data?: { codigo?: string; mensaje?: string } } };
    const errorStatus = error.response?.status;
    const errorCode = error.response?.data?.codigo;
    const errorMsg = error.response?.data?.mensaje;

    if (errorStatus === 403 || errorCode === 'VER001') {
      $q.notify({
        type: 'warning',
        message: 'Debes verificar tu email primero',
        caption: 'Revisa tu bandeja de entrada o solicita un nuevo enlace',
        position: 'top-right'
      });
    } else {
      $q.notify({
        type: 'negative',
        message: 'Fallo de autenticación',
        caption: errorMsg || authStore.error || 'Credenciales no reconocidas',
        position: 'top-right'
      });
    }
  } finally {
    loading.value = false;
  }
};

const loginWithSocial = async (provider: 'google') => {
  try {
    let url = `http://localhost:8080/oauth2/authorization/${provider}`;

    if (authStore.pendingTenant?.name && authStore.pendingTenant?.slug) {
      $q.loading.show({ message: 'Preparando entorno de empresa...' });

      const response = await authService.createOAuth2Intent({
        companyName: authStore.pendingTenant.name,
        companySlug: authStore.pendingTenant.slug,
      });

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

:deep(.q-field--filled .q-field__control) {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}
</style>