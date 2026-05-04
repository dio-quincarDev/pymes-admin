<template>
  <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
    <!-- Header: Identidad de la Empresa -->
    <div class="q-mb-lg text-center">
      <div class="text-overline text-accent letter-spacing-1">PASO FINAL: CONFIGURACIÓN DE ACCESO</div>
      <div class="text-h4 text-primary text-weight-bold q-mt-sm">{{ pendingTenant?.name }}</div>
      <div class="text-caption text-accent q-mt-xs">Crea tu cuenta de administrador maestro</div>
    </div>

    <q-form @submit="onRegister" class="q-gutter-y-md">
      <q-input
        v-model="registerForm.name"
        label="Nombre del Administrador"
        placeholder="Tu nombre completo"
        dark filled color="primary" label-color="accent"
        :rules="[val => !!val || 'El nombre es requerido']"
      >
        <template v-slot:prepend><q-icon name="person" color="primary" /></template>
      </q-input>

      <q-input
        v-model="registerForm.email"
        label="Correo Corporativo"
        placeholder="ejemplo@tuempresa.com"
        dark filled color="primary" label-color="accent"
        :rules="[val => !!val || 'El email es requerido', val => /.+@.+\..+/.test(val) || 'Email inválido']"
      >
        <template v-slot:prepend><q-icon name="email" color="primary" /></template>
      </q-input>

      <q-input
        v-model="registerForm.password"
        label="Contraseña"
        type="password"
        placeholder="Mínimo 8 caracteres"
        dark filled color="primary" label-color="accent"
        :rules="[val => !!val || 'La contraseña es requerida', val => val.length >= 8 || 'Mínimo 8 caracteres']"
      >
        <template v-slot:prepend><q-icon name="lock" color="primary" /></template>
      </q-input>

      <div class="q-mt-xl">
        <q-btn
          label="FINALIZAR Y CREAR EMPRESA"
          type="submit"
          color="primary"
          class="full-width brand-glow text-weight-bold q-py-md"
          size="lg"
          :loading="loading"
        />
      </div>
    </q-form>

    <q-separator dark class="q-my-lg" label="O REGÍSTRATE CON GOOGLE" />

    <div class="row justify-center">
      <q-btn
        outline
        color="secondary"
        class="social-btn full-width q-py-sm"
        @click="loginWithGoogle"
      >
        <q-icon name="img:https://cdn.cdnlogo.com/logos/g/35/google-icon.svg" size="xs" class="q-mr-sm" />
        Continuar con Google
      </q-btn>
    </div>

    <div class="q-mt-md text-center">
      <q-btn flat no-caps label="Volver a cambiar nombre de empresa" color="accent" size="sm" @click="goBackToHome" />
    </div>
  </q-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
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
    void router.push('/');
  }
});

const goBackToHome = () => {
  authStore.clearPendingTenant();
  void router.push('/');
};

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
      message: 'Plataforma desplegada con éxito',
      caption: 'Verifica tu email para activar el acceso',
      position: 'top-right'
    });

    authStore.clearPendingTenant();
    void router.push('/login');
  } catch (err: unknown) {
    const error = err as { response?: { data?: { mensaje?: string } } };
    const message = error.response?.data?.mensaje || 'Error al aprovisionar el entorno';
    $q.notify({
      type: 'negative',
      message: 'Fallo en el despliegue',
      caption: message,
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
      $q.loading.show({ message: 'Sincronizando identidad empresarial...' });
      const response = await authService.createOAuth2Intent({
        companyName: pendingTenant.value.name,
        companySlug: pendingTenant.value.slug,
      });

      const intentId = response.data?.data?.intentId;
      if (intentId) {
        url += `?state=${intentId}`;
      }
    }

    window.location.href = url;
  } catch {
    $q.notify({
      type: 'negative',
      message: 'No se pudo iniciar el proceso social.',
    });
  } finally {
    $q.loading.hide();
  }
};
</script>

<style lang="scss" scoped>
.letter-spacing-1 {
  letter-spacing: 1px;
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
