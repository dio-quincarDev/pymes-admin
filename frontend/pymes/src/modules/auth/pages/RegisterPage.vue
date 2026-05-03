<template>
  <q-page class="flex flex-center bg-forest-deep">
    <div class="register-card-container q-pa-md">
      <!-- Logo & Branding -->
      <div class="text-center q-mb-xl">
        <div class="text-h3 font-bold text-primary q-mb-xs">PYMEQ</div>
        <div class="text-subtitle1 text-accent text-weight-light letter-spacing-2">
          REGISTRO DE ADMINISTRADOR
        </div>
      </div>

      <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
        <div v-if="pendingTenant" class="q-mb-lg text-center">
          <div class="text-overline text-accent">Registrando cuenta para</div>
          <div class="text-h5 text-primary text-weight-bold">{{ pendingTenant.name }}</div>
        </div>

        <q-form @submit="onRegister" class="q-gutter-y-md">
          <q-input
            v-model="registerForm.name"
            label="Tu Nombre"
            dark filled color="primary" label-color="accent"
            :rules="[val => !!val || 'El nombre es requerido']"
          >
            <template v-slot:prepend><q-icon name="person" color="primary" /></template>
          </q-input>

          <q-input
            v-model="registerForm.email"
            label="Correo Electrónico"
            dark filled color="primary" label-color="accent"
            :rules="[val => !!val || 'El email es requerido', val => /.+@.+\..+/.test(val) || 'Email inválido']"
          >
            <template v-slot:prepend><q-icon name="email" color="primary" /></template>
          </q-input>

          <q-input
            v-model="registerForm.password"
            label="Contraseña"
            type="password"
            dark filled color="primary" label-color="accent"
            :rules="[val => !!val || 'La contraseña es requerida', val => val.length >= 8 || 'Mínimo 8 caracteres']"
          >
            <template v-slot:prepend><q-icon name="lock" color="primary" /></template>
          </q-input>

          <div class="q-mt-xl">
            <q-btn
              label="CREAR CUENTA"
              type="submit"
              color="primary"
              class="full-width brand-glow text-weight-bold"
              size="lg"
              :loading="loading"
            />
          </div>
        </q-form>

        <q-separator dark class="q-my-lg" label="O REGÍSTRATE CON" />

        <div class="row justify-center">
          <q-btn
            outline
            color="secondary"
            class="social-btn full-width"
            @click="loginWithGoogle"
          >
            <q-icon name="img:https://cdn.cdnlogo.com/logos/g/35/google-icon.svg" size="xs" class="q-mr-sm" />
            Google
          </q-btn>
        </div>

        <div class="q-mt-md text-center">
          <div class="text-caption text-accent">
            ¿Ya tienes cuenta?
            <q-btn flat no-caps label="Inicia Sesión" color="primary" class="q-px-xs" to="/login" />
          </div>
        </div>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useAuthStore } from '../store';
import { useQuasar, openURL } from 'quasar';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { authService } from '../services/auth.service';

const authStore = useAuthStore();
const { pendingTenant } = storeToRefs(authStore);
const $q = useQuasar();
const router = useRouter();

const loading = ref(false);

const registerForm = reactive({
  name: '',
  email: '',
  password: ''
});

onMounted(() => {
  if (!pendingTenant.value) {
    $q.notify({
      message: 'Debes configurar tu empresa primero',
      color: 'warning',
      position: 'top'
    });
    void router.push('/');
  }
});

const onRegister = async () => {
  if (!pendingTenant.value) return;

  loading.value = true;
  try {
    const fullPayload = {
      ...registerForm,
      companyName: pendingTenant.value.name,
      companySlug: pendingTenant.value.slug
    };

    await authStore.register(fullPayload);

    $q.notify({
      type: 'positive',
      message: 'Cuenta creada con éxito',
      caption: 'Revisa tu email para verificar la cuenta',
      position: 'top-right'
    });

    authStore.clearPendingTenant();
    void router.push('/login');
  } catch {
    $q.notify({
      type: 'negative',
      message: 'Error en el registro',
      caption: authStore.error || 'No se pudo crear la cuenta',
      position: 'top-right'
    });
  } finally {
    loading.value = false;
  }
};

const loginWithGoogle = async () => {
  await handleOAuth2Login('google');
};

const handleOAuth2Login = async (provider: 'google') => {
  try {
    let url = `http://localhost:8080/oauth2/authorization/${provider}`;

    if (pendingTenant.value?.name && pendingTenant.value?.slug) {
      $q.loading.show({ message: 'Preparando registro con Google...' });
      const { data } = await authService.createOAuth2Intent({
        companyName: pendingTenant.value.name,
        companySlug: pendingTenant.value.slug,
      });

      if (data.data?.intentId) {
        url += `?intentId=${data.data.intentId}`;
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
.register-card-container {
  width: 100%;
  max-width: 450px;
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
:deep(.q-field--filled .q-field__control) {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}
</style>
