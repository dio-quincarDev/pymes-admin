<template>
  <q-page class="flex flex-center bg-dark">
    <q-card class="verify-card q-pa-lg text-white bg-grey-10 shadow-2">
      <q-card-section class="text-center">
        <div class="text-h5 q-mb-md">
          <q-icon name="mail" size="lg" color="primary" class="q-mr-sm" />
          Verificación de Email
        </div>

        <div v-if="loading" class="q-my-xl">
          <q-spinner-oval color="primary" size="4em" />
          <p class="q-mt-md text-subtitle1">Verificando tu cuenta...</p>
        </div>

        <div v-else-if="success" class="q-my-md">
          <q-icon name="check_circle" color="positive" size="5em" />
          <p class="text-h6 q-mt-md">¡Email verificado!</p>
          <p class="text-body1 text-grey-5">Tu cuenta ha sido activada correctamente. Ya puedes iniciar sesión.</p>
        </div>

        <div v-else-if="error" class="q-my-md">
          <q-icon name="error" color="negative" size="5em" />
          <p class="text-h6 q-mt-md">Error de verificación</p>
          <p class="text-body1 text-grey-5">{{ errorMessage }}</p>
        </div>
      </q-card-section>

      <q-card-actions align="center" class="q-mt-md">
        <q-btn
          v-if="success || (error && errorType !== 'expired')"
          label="Ir al Login"
          color="primary"
          unelevated
          class="full-width"
          to="/login"
        />

        <div v-if="error && errorType === 'expired'" class="full-width text-center">
          <q-btn
            label="Reenviar enlace de verificación"
            color="primary"
            unelevated
            class="full-width q-mb-sm"
            :loading="resending"
            @click="handleResend"
          />
          <q-btn
            label="Volver al Login"
            flat
            color="grey-5"
            to="/login"
          />
        </div>
      </q-card-actions>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { authService } from '../services/auth.service';
import { useQuasar } from 'quasar';

const route = useRoute();
const $q = useQuasar();

const loading = ref(true);
const success = ref(false);
const error = ref(false);
const errorMessage = ref('');
const errorType = ref('');
const resending = ref(false);

const token = ref(route.query.token as string);
const email = ref(route.query.email as string);

const verifyEmail = async (verificationToken: string) => {
  try {
    await authService.verifyEmail(verificationToken);
    success.value = true;
  } catch (err: unknown) {
    error.value = true;
    const response = (err as { response?: { status?: number; data?: { codigo?: string; mensaje?: string } } })?.response;
    const status = response?.status;
    const errorData = response?.data;

    if (status === 400 && errorData?.codigo === 'TOKEN_EXPIRED') {
      errorType.value = 'expired';
      errorMessage.value = 'El enlace ha expirado. Por favor, solicita uno nuevo.';
    } else {
      errorMessage.value = errorData?.mensaje || 'No se pudo verificar el email. El token puede ser inválido o ya fue procesado.';
    }
  } finally {
    loading.value = false;
  }
};

const handleResend = async () => {
  if (!email.value) {
    $q.notify({
      type: 'negative',
      message: 'No se encontró el email asociado. Por favor intenta desde el login.'
    });
    return;
  }

  resending.value = true;
  try {
    await authService.resendVerification(email.value);
    $q.notify({
      type: 'positive',
      message: 'Se ha enviado un nuevo enlace de verificación a tu correo.'
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
  void verifyEmail(token.value);
});
</script>

<style scoped>
.verify-card {
  width: 100%;
  max-width: 400px;
  border-radius: 12px;
}
</style>
