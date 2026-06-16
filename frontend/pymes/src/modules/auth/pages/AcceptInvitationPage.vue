<template>
  <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
    <q-card-section class="text-center q-pb-none">
      <div class="text-h6 text-weight-medium q-mb-sm">Invitación al Equipo</div>
      <p class="text-body2 text-accent">
        Has sido invitado a colaborar en un espacio de trabajo. Confirma tu acceso para unirte.
      </p>
    </q-card-section>

    <q-card-section>
      <div v-if="success" class="text-center q-py-lg">
        <q-icon name="group_add" color="positive" size="4rem" class="q-mb-md" />
        <div class="text-h6 text-primary">¡Bienvenido al Equipo!</div>
        <p class="text-accent q-mt-sm">Ahora eres parte de <strong>{{ tenantName }}</strong>.</p>
        <q-btn label="IR AL PANEL DE CONTROL" color="primary" class="full-width brand-glow text-weight-bold q-mt-md" size="lg" to="/dashboard" no-caps />
      </div>

      <div v-else-if="error" class="text-center q-py-lg">
        <q-icon name="error" color="negative" size="4rem" class="q-mb-md" />
        <div class="text-h6 text-negative">Error de Invitación</div>
        <p class="text-accent q-mt-sm">{{ errorMessage }}</p>
        <q-btn flat label="Volver al Login" color="primary" to="/login" no-caps class="q-mt-md" />
      </div>

      <div v-else class="text-center q-py-md">
        <q-btn
          label="ACEPTAR Y UNIRME AL EQUIPO"
          color="primary"
          class="full-width brand-glow text-weight-bold"
          size="lg"
          :loading="loading"
          @click="onAccept"
        />
        <q-btn flat label="Cancelar" color="accent" to="/" no-caps class="q-mt-md" />
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useMeta } from 'quasar';

useMeta({ title: 'Invitación — PYMEQ' });
import { useRoute, useRouter } from 'vue-router';
import { authService } from '../services/auth.service';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import type { ApiResponse, InvitationResponse } from '../types';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const $q = useQuasar();

const token = ref(route.query.token as string);
const loading = ref(false);
const success = ref(false);
const error = ref(false);
const errorMessage = ref('');
const tenantName = ref('');

onMounted(() => {
  if (!token.value) {
    error.value = true;
    errorMessage.value = 'Token de invitación ausente.';
    return;
  }

  if (!authStore.isAuthenticated) {
    $q.notify({
      type: 'info',
      message: 'Debes iniciar sesión primero para aceptar la invitación.'
    });
    void router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`);
  }
});

const onAccept = async () => {
  loading.value = true;
  try {
    const response = await authService.acceptInvitation(token.value);
    const apiResponse = response.data as ApiResponse<InvitationResponse>;
    const data = apiResponse.data;
    tenantName.value = data?.tenant?.name || 'la empresa';
    success.value = true;
    $q.notify({
      type: 'positive',
      message: 'Acceso concedido correctamente.'
    });
  } catch (err: unknown) {
    error.value = true;
    const responseData = (err as { response?: { data?: { mensaje?: string } } })?.response?.data;
    errorMessage.value = responseData?.mensaje || 'No se pudo procesar la invitación. El enlace puede ser inválido o ya expiró.';
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss" scoped>
:deep(.q-field--filled .q-field__control) {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}
</style>
