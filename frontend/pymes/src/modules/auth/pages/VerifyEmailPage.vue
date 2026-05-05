<template>
  <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
    <q-card-section class="text-center">
      <div class="text-h6 text-weight-medium q-mb-md">Verificación de Identidad</div>

      <div v-if="loading" class="q-my-xl">
        <q-spinner-oval color="primary" size="4em" />
        <p class="q-mt-md text-subtitle1 text-accent">Validando tus credenciales...</p>
      </div>

      <div v-else-if="success" class="q-my-md text-center">
        <q-icon name="check_circle" color="positive" size="5em" class="q-mb-md" />
        <div class="text-h6 text-primary">¡Acceso Verificado!</div>
        <p class="text-body2 text-accent q-mt-sm">
          Tu cuenta ha sido activada correctamente en el sistema de auditoría.
        </p>
      </div>

      <div v-else-if="error" class="q-my-md text-center">
        <q-icon name="error" color="negative" size="5em" class="q-mb-md" />
        <div class="text-h6 text-negative">Error de Verificación</div>
        <p class="text-body2 text-accent q-mt-sm">{{ errorMessage }}</p>
      </div>
    </q-card-section>

    <q-card-actions align="center" class="q-mt-md">
      <q-btn
        v-if="success"
        label="IR AL DASHBOARD"
        color="primary"
        class="full-width brand-glow text-weight-bold"
        size="lg"
        @click="router.push('/dashboard')"
      />

      <q-btn
        v-if="error && errorType !== 'expired'"
        label="VOLVER AL LOGIN"
        color="primary"
        class="full-width brand-glow text-weight-bold"
        size="lg"
        to="/login"
      />

      <div v-if="error && errorType === 'expired'" class="full-width text-center">
        <q-btn
          label="REENVIAR ENLACE"
          color="primary"
          class="full-width brand-glow text-weight-bold q-mb-md"
          size="lg"
          :loading="resending"
          @click="handleResend"
        />
        <q-btn
          label="Volver al Login"
          flat
          color="accent"
          to="/login"
          no-caps
        />
      </div>
    </q-card-actions>
  </q-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authService } from '../services/auth.service';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();

const loading = ref(true);
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
    success.value = true;
    
    // Si tenemos tokens en la respuesta, fue un registro completo con auto-login
    if (response && response.accessToken) {
      $q.notify({
        type: 'positive',
        message: '¡Bienvenido a Pymeq!',
        caption: 'Tu cuenta ha sido creada y verificada.',
        position: 'top-right'
      });
    }
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
    loading.value = false;
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

onMounted(() => {
  if (!token.value) {
    loading.value = false;
    error.value = true;
    errorMessage.value = 'Token de verificación ausente.';
    return;
  }
  void verifyEmail(token.value, email.value);
});
</script>

<style lang="scss" scoped>
:deep(.q-field--filled .q-field__control) {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}
</style>
