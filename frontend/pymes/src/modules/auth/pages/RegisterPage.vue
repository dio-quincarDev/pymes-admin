<template>
  <div class="register-page-wrapper">
    <SkeletonLoader :is-loading="initialLoading" layout="form">
      <BaseCard variant="elevated" class="q-pa-lg">
        <!-- Header: Identidad de la Empresa -->
        <div class="q-mb-lg text-center">
          <div class="text-overline text-accent letter-spacing-1">PASO FINAL: CONFIGURACIÓN DE ACCESO</div>
          <div class="text-h4 text-primary text-weight-bold q-mt-sm">{{ pendingTenant?.name }}</div>
          <div class="text-caption text-accent q-mt-xs">Crea tu cuenta de administrador maestro</div>
        </div>

        <q-form @submit.prevent="onRegister" class="q-gutter-y-md">
          <q-input
            v-model="registerForm.name"
            label="Nombre del Administrador"
            placeholder="Tu nombre completo"
            dark filled color="primary" label-color="accent"
            class="focus-ring radius-xs"
            :rules="[val => !!val || 'El nombre es requerido']"
          >
            <template v-slot:prepend><q-icon name="person" color="primary" /></template>
          </q-input>

          <q-input
            v-model="registerForm.email"
            label="Correo Corporativo"
            placeholder="ejemplo@tuempresa.com"
            dark filled color="primary" label-color="accent"
            class="focus-ring radius-xs"
            :rules="[val => !!val || 'El email es requerido', val => /.+@.+\..+/.test(val) || 'Email inválido']"
          >
            <template v-slot:prepend><q-icon name="email" color="primary" /></template>
          </q-input>

          <q-input
            v-model="registerForm.password"
            label="Contraseña"
            :type="showPassword ? 'text' : 'password'"
            placeholder="Mínimo 8 caracteres"
            dark filled color="primary" label-color="accent"
            class="focus-ring radius-xs"
            :rules="[val => !!val || 'La contraseña es requerida', val => val.length >= 8 || 'Mínimo 8 caracteres']"
          >
            <template v-slot:prepend><q-icon name="lock" color="primary" /></template>
            <template v-slot:append>
              <q-icon
                :name="showPassword ? 'visibility' : 'visibility_off'"
                class="cursor-pointer"
                color="primary"
                @click="showPassword = !showPassword"
              />
            </template>
          </q-input>

          <q-input
            v-model="registerForm.confirmPassword"
            label="Confirmar Contraseña"
            :type="showConfirmPassword ? 'text' : 'password'"
            placeholder="Repite tu contraseña"
            dark filled color="primary" label-color="accent"
            class="focus-ring radius-xs"
            :error="!!confirmPasswordError"
            :error-message="confirmPasswordError"
            :rules="[val => !!val || 'Confirma tu contraseña']"
          >
            <template v-slot:prepend><q-icon name="lock_outline" color="primary" /></template>
            <template v-slot:append>
              <q-icon
                :name="showConfirmPassword ? 'visibility' : 'visibility_off'"
                class="cursor-pointer"
                color="primary"
                @click="showConfirmPassword = !showConfirmPassword"
              />
            </template>
          </q-input>

          <div class="q-mt-xl">
            <BaseButton
              label="FINALIZAR Y CREAR EMPRESA"
              type="submit"
              class="full-width"
              size="lg"
              :loading="loading"
            >
              FINALIZAR Y CREAR EMPRESA
            </BaseButton>
          </div>
        </q-form>

        <div class="relative-position q-my-xl">
          <q-separator dark />
          <div class="absolute-center bg-dark-page q-px-md text-caption text-accent text-weight-bold">
            O REGÍSTRATE CON GOOGLE
          </div>
        </div>

        <div class="row justify-center q-mb-lg">
          <BaseButton
            variant="secondary"
            class="full-width"
            @click="loginWithGoogle"
          >
            <q-icon name="img:https://cdn.cdnlogo.com/logos/g/35/google-icon.svg" size="xs" class="q-mr-sm" />
            Continuar con Google
          </BaseButton>
        </div>

        <div class="text-center">
          <BaseButton variant="ghost" size="sm" @click="goBackToHome">
            Volver a cambiar nombre de empresa
          </BaseButton>
        </div>
      </BaseCard>
    </SkeletonLoader>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { authService } from '../services/auth.service';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';

const authStore = useAuthStore();
const { pendingTenant } = storeToRefs(authStore);
const $q = useQuasar();
const router = useRouter();

const loading = ref(false);
const initialLoading = ref(true);
const confirmPasswordError = ref('');
const showPassword = ref(false);
const showConfirmPassword = ref(false);

const registerForm = reactive({
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
});

watch(() => registerForm.password, () => {
  if (registerForm.confirmPassword && registerForm.password !== registerForm.confirmPassword) {
    confirmPasswordError.value = 'Las contraseñas no coinciden';
  } else {
    confirmPasswordError.value = '';
  }
});

watch(() => registerForm.confirmPassword, () => {
  if (registerForm.confirmPassword && registerForm.password !== registerForm.confirmPassword) {
    confirmPasswordError.value = 'Las contraseñas no coinciden';
  } else {
    confirmPasswordError.value = '';
  }
});

onMounted(() => {
  if (!pendingTenant.value) {
    void router.push('/');
  }
  setTimeout(() => {
    initialLoading.value = false;
  }, 800);
});

const goBackToHome = () => {
  authStore.clearPendingTenant();
  void router.push('/');
};

const onRegister = async () => {
  if (!pendingTenant.value) return;

  loading.value = true;
  try {
    authStore.clearSession();

    const fullPayload = {
      name: registerForm.name,
      email: registerForm.email,
      password: registerForm.password,
      companyName: pendingTenant.value.name,
      companySlug: pendingTenant.value.slug
    };

    await authStore.register(fullPayload);

    $q.notify({
      type: 'positive',
      message: 'Revisa tu correo electrónico',
      caption: 'Te enviamos un enlace para verificar tu cuenta',
      position: 'top-right'
    });

    authStore.clearPendingTenant();
    void router.push('/verify');
  } catch (err: unknown) {
    const error = err as { response?: { data?: { mensaje?: string } } };
    const message = error.response?.data?.mensaje || 'Error al registrar';
    $q.notify({
      type: 'negative',
      message: 'Error en el registro',
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
.register-page-wrapper {
  width: 100%;
}

.letter-spacing-1 {
  letter-spacing: 1px;
}

:deep(.q-field--filled .q-field__control) {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(113, 131, 127, 0.1);
  transition: all 0.2s ease;
  
  &:hover {
    background: rgba(0, 0, 0, 0.3);
    border-color: rgba(163, 120, 94, 0.3);
  }
}

:deep(.q-field--focused .q-field__control) {
  border-color: $primary;
  box-shadow: 0 0 10px rgba(163, 120, 94, 0.2);
}
</style>