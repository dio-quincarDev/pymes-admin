<template>
  <div class="register-page-wrapper">
    <SkeletonLoader :is-loading="initialLoading" layout="form">
      <BaseCard variant="elevated" class="q-pa-lg">
        <template v-if="currentMode === 'oauth-primary'">
          <div class="q-mb-lg text-center">
            <div class="text-caption q-mb-xs" style="color: var(--pq-text-subtle); letter-spacing: 1px; text-transform: uppercase; font-size: 11px; font-weight: 500;">PASO FINAL: CONFIGURACIÓN DE ACCESO</div>
            <div class="text-h4 text-weight-bold q-mt-sm" style="color: var(--pq-text);">{{ pendingTenant?.name }}</div>
            <div class="text-caption q-mt-xs" style="color: var(--pq-text-muted);">Crea tu cuenta de administrador maestro</div>
          </div>

          <BaseButton
            variant="secondary"
            class="full-width"
            size="lg"
            :loading="oauthLoading"
            @click="loginWithGoogle"
          >
            <q-icon name="img:https://cdn.cdnlogo.com/logos/g/35/google-icon.svg" size="xs" class="q-mr-sm" />
            Continuar con Google
          </BaseButton>

          <div class="text-center q-mt-lg">
            <button
              class="auth-mode-toggle"
              @click="currentMode = 'email'"
              type="button"
              aria-label="Registrarse con email y contraseña"
            >
              o regístrate con email y contraseña
              <q-icon name="chevron_right" size="xs" />
            </button>
          </div>
        </template>

        <template v-else>
          <div class="q-mb-lg text-center" style="position: relative;">
            <button
              class="auth-back-link"
              @click="currentMode = 'oauth-primary'"
              type="button"
              aria-label="Volver a registro con Google"
            >
              <q-icon name="chevron_left" size="xs" />
              Volver
            </button>
            <div class="text-caption q-mb-xs" style="color: var(--pq-text-subtle); letter-spacing: 1px; text-transform: uppercase; font-size: 11px; font-weight: 500;">PASO FINAL: CONFIGURACIÓN DE ACCESO</div>
            <div class="text-h4 text-weight-bold q-mt-sm" style="color: var(--pq-text);">{{ pendingTenant?.name }}</div>
            <div class="text-caption q-mt-xs" style="color: var(--pq-text-muted);">Crea tu cuenta de administrador maestro</div>
          </div>

          <q-form @submit.prevent="onRegister" class="q-gutter-y-md">
            <q-input
              v-model="registerForm.name"
              label="Nombre del Administrador"
              placeholder="Tu nombre completo"
              dark filled color="primary" label-color="accent"
              class="focus-ring radius-xs"
              :disable="loading"
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
              :disable="loading"
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
              :disable="loading"
              :rules="[val => !!val || 'La contraseña es requerida', val => val.length >= 8 || 'Mínimo 8 caracteres']"
            >
              <template v-slot:prepend><q-icon name="lock" color="primary" /></template>
              <template v-slot:append>
                <q-icon
                  :name="showPassword ? 'visibility' : 'visibility_off'"
                  class="cursor-pointer" color="primary" role="button" tabindex="0"
                  :aria-label="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                  @click="showPassword = !showPassword"
                  @keydown.enter="showPassword = !showPassword"
                  @keydown.space.prevent="showPassword = !showPassword"
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
              :disable="loading"
              :error="!!confirmPasswordError"
              :error-message="confirmPasswordError"
              :rules="[val => !!val || 'Confirma tu contraseña']"
            >
              <template v-slot:prepend><q-icon name="lock_outline" color="primary" /></template>
              <template v-slot:append>
                <q-icon
                  :name="showConfirmPassword ? 'visibility' : 'visibility_off'"
                  class="cursor-pointer" color="primary" role="button" tabindex="0"
                  :aria-label="showConfirmPassword ? 'Ocultar confirmación' : 'Mostrar confirmación'"
                  @click="showConfirmPassword = !showConfirmPassword"
                  @keydown.enter="showConfirmPassword = !showConfirmPassword"
                  @keydown.space.prevent="showConfirmPassword = !showConfirmPassword"
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
        </template>

        <div class="text-center q-mt-lg">
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
import { useMeta } from 'quasar';

useMeta({ title: 'Registro — PYMEQ' });
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { authService } from '../services/auth.service';
import { useAuthForm } from 'src/composables/useAuthForm';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';

const authStore = useAuthStore();
const { pendingTenant } = storeToRefs(authStore);
const $q = useQuasar();
const router = useRouter();
const { loading, initialLoading, showPassword, showConfirmPassword } = useAuthForm();

const currentMode = ref<'oauth-primary' | 'email'>('oauth-primary');
const oauthLoading = ref(false);
const confirmPasswordError = ref('');

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
  oauthLoading.value = true;
  try {
    let url = `${window.location.origin}/oauth2/authorization/google`;

    if (pendingTenant.value?.name && pendingTenant.value?.slug) {
      $q.loading.show({ message: 'Sincronizando identidad empresarial...' });
      const response = await authService.createOAuth2Intent({
        companyName: pendingTenant.value.name,
        companySlug: pendingTenant.value.slug,
      });

      const intentId = response.data?.data?.intentId;
      if (intentId) {
        url += `?intentId=${intentId}`;
      }
    }

    window.location.href = url;
  } catch {
    $q.notify({
      type: 'negative',
      message: 'No se pudo iniciar el proceso social.',
    });
    oauthLoading.value = false;
  } finally {
    $q.loading.hide();
  }
};
</script>

<style lang="scss" scoped>
.register-page-wrapper {
  width: 100%;
}

.auth-mode-toggle {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--pq-text-muted);
  font-family: 'Satoshi', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-size: 14px;
  font-weight: 400;
  padding: 8px 12px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  transition: color var(--pq-motion-fast);

  &:hover { color: var(--pq-accent); }
  &:focus-visible { outline: 2px solid var(--pq-accent); outline-offset: 2px; }
}

.auth-back-link {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--pq-text-muted);
  font-family: 'Satoshi', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-size: 13px;
  font-weight: 400;
  padding: 4px 8px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  position: absolute;
  left: 0;
  top: 4px;
  transition: color var(--pq-motion-fast);

  &:hover { color: var(--pq-accent); }
  &:focus-visible { outline: 2px solid var(--pq-accent); outline-offset: 2px; }
}
</style>
