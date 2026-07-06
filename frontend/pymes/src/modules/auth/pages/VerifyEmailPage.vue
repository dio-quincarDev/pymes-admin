<template>
  <div class="verify-page-wrapper">
    <SkeletonLoader :is-loading="loading" layout="card">
      <BaseCard variant="elevated" class="q-pa-lg text-center">
        <div v-if="pending" class="verify-pending fade-in-up">
          <q-icon name="mark_email_unread" color="primary" size="5em" class="q-mb-md" />
          <div class="text-h6 text-primary text-weight-bold">Revisa tu correo electrónico</div>
          <p class="text-body2 text-accent q-mt-sm q-mb-lg">
            Te enviamos un enlace para verificar tu cuenta.
            <br>Si no lo encuentras, revisa tu bandeja de spam.
          </p>
          <BaseButton
            label="IR AL LOGIN"
            class="full-width"
            size="lg"
            @click="router.push('/login')"
          >
            IR AL LOGIN
          </BaseButton>
        </div>

        <div v-else-if="success" class="verify-success fade-in-up">
          <q-icon name="check_circle" color="positive" size="5em" class="q-mb-md brand-glow" />
          <div class="text-h6 text-primary text-weight-bold">¡Acceso Verificado!</div>
          <p class="text-body2 text-accent q-mt-sm">
            Tu cuenta ha sido activada correctamente en el sistema de auditoría.
          </p>
          <div class="q-mt-xl">
            <BaseButton
              label="IR AL DASHBOARD"
              class="full-width"
              size="lg"
              @click="goToDashboard"
            >
              IR AL DASHBOARD
            </BaseButton>
          </div>
        </div>

        <div v-else-if="error" class="verify-error fade-in-up">
          <q-icon name="error" color="negative" size="5em" class="q-mb-md" />
          <div class="text-h6 text-negative text-weight-bold">Error de Verificación</div>
          <p class="text-body2 text-accent q-mt-sm">{{ errorMessage }}</p>

          <div class="q-mt-xl">
            <div v-if="errorType === 'expired'" class="full-width">
              <BaseButton
                label="REENVIAR ENLACE"
                class="full-width q-mb-md"
                size="lg"
                :loading="resending"
                @click="handleResend"
              >
                REENVIAR ENLACE
              </BaseButton>
              <BaseButton
                variant="ghost"
                class="full-width"
                @click="router.push('/login')"
              >
                Volver al Login
              </BaseButton>
            </div>
            <BaseButton
              v-else
              label="VOLVER AL LOGIN"
              class="full-width"
              size="lg"
              @click="router.push('/login')"
            >
              VOLVER AL LOGIN
            </BaseButton>
          </div>
        </div>
      </BaseCard>
    </SkeletonLoader>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useMeta } from 'quasar';

useMeta({ title: 'Verificar Email — PYMEQ' });
import { useRoute, useRouter } from 'vue-router';
import { authService } from '../services/auth.service';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';
import { setupService } from 'src/modules/core/services/setup.service';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();

const loading = ref(true);
const pending = ref(false);
const success = ref(false);
const error = ref(false);
const errorMessage = ref('');
const errorType = ref('');
const resending = ref(false);

const token = ref(route.query.token as string);
const email = ref(route.query.email as string);

const verifyEmail = async (verificationToken: string, userEmail: string) => {
  try {
    const response = await authStore.verifyEmail(verificationToken, userEmail);

    if (response?.accessToken) {
      localStorage.setItem('pymeq_email_verified', 'true');

      $q.notify({
        type: 'positive',
        message: '¡Bienvenido a Pymeq!',
        caption: 'Tu cuenta ha sido creada y verificada.',
        position: 'top-right'
      });

      const tenantId = authStore.user?.tenantId;
      if (tenantId) {
        try {
          const { data: setup } = await setupService.get(tenantId);
          if (!setup.onboardingCompleted) {
            void router.push('/onboarding');
            return;
          }
        } catch { /* ponytail: ir al dashboard igual */ }
      }

      void router.push('/dashboard');
      return;
    }

    success.value = true;
  } catch (err: unknown) {
    error.value = true;
    const response = (err as { response?: { status?: number; data?: { codigo?: string; mensaje?: string } } })?.response;
    const status = response?.status;
    const errorData = response?.data;

    if (status === 400 && errorData?.codigo === 'TOKEN_EXPIRED') {
      errorType.value = 'expired';
      errorMessage.value = 'El enlace de seguridad ha expirado.';
    } else {
      errorMessage.value = errorData?.mensaje || 'No se pudo verificar la identidad. El token puede ser inválido.';
    }
  } finally {
    setTimeout(() => {
      loading.value = false;
    }, 600);
  }
};

const handleResend = async () => {
  if (!email.value) {
    $q.notify({
      type: 'negative',
      message: 'Email no detectado. Reintenta desde el login.'
    });
    return;
  }

  resending.value = true;
  try {
    await authService.resendVerification(email.value);
    $q.notify({
      type: 'positive',
      message: 'Nuevo enlace de seguridad enviado con éxito.'
    });
    errorType.value = '';
    errorMessage.value = 'Nuevo enlace enviado. Revisa tu bandeja de entrada.';
  } catch (err: unknown) {
    const message = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje;
    $q.notify({
      type: 'negative',
      message: message || 'Error al reenviar la verificación.'
    });
  } finally {
    resending.value = false;
  }
};

async function goToDashboard() {
  const tenantId = authStore.user?.tenantId;
  if (tenantId) {
    try {
      const { data: setup } = await setupService.get(tenantId);
      if (!setup.onboardingCompleted) {
        void router.push('/onboarding');
        return;
      }
    } catch { /* ponytail: si falla, ir al dashboard igual */ }
  }
  void router.push('/dashboard');
}

onMounted(() => {
  if (!token.value) {
    loading.value = false;
    pending.value = true;
    return;
  }
  window.history.replaceState({}, '', window.location.pathname + window.location.hash.replace(/\?.*$/, ''));
  void verifyEmail(token.value, email.value);
});
</script>

<style lang="scss" scoped>
.verify-page-wrapper {
  width: 100%;
}
</style>