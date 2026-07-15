<template>
  <div class="accept-invitation-page-wrapper">
    <SkeletonLoader :is-loading="loading && !success && !error" layout="card">
      <BaseCard variant="elevated" class="q-pa-lg">
        <div class="text-center q-mb-lg">
          <div class="text-h6 text-weight-medium q-mb-sm">Invitación al Equipo</div>
          <p class="text-body2" style="color: var(--pq-text-muted);">
            Has sido invitado a colaborar en un espacio de trabajo. Confirma tu acceso para unirte.
          </p>
        </div>

        <div v-if="success" class="text-center q-py-lg fade-in-up">
          <q-icon name="group_add" color="positive" size="4rem" class="q-mb-md" />
          <div class="text-h6" style="color: var(--pq-text);">¡Bienvenido al Equipo!</div>
          <p style="color: var(--pq-text-muted);" class="q-mt-sm">Ahora eres parte de <strong>{{ tenantName }}</strong>.</p>
          <BaseButton
            label="IR AL PANEL DE CONTROL"
            class="full-width q-mt-md"
            size="lg"
            to="/dashboard"
          >
            IR AL PANEL DE CONTROL
          </BaseButton>
        </div>

        <div v-else-if="error" class="text-center q-py-lg">
          <q-icon name="error" color="negative" size="4rem" class="q-mb-md" />
          <div class="text-h6" style="color: var(--pq-danger);">Error de Invitación</div>
          <p style="color: var(--pq-text-muted);" class="q-mt-sm">{{ errorMessage }}</p>
          <BaseButton variant="ghost" class="q-mt-md" to="/login">
            Volver al Login
          </BaseButton>
        </div>

        <div v-else class="text-center q-py-md">
          <BaseButton
            label="ACEPTAR Y UNIRME AL EQUIPO"
            class="full-width"
            size="lg"
            :loading="loading"
            @click="onAccept"
          >
            ACEPTAR Y UNIRME AL EQUIPO
          </BaseButton>
          <BaseButton variant="ghost" class="q-mt-md" to="/">
            Cancelar
          </BaseButton>
        </div>
      </BaseCard>
    </SkeletonLoader>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useMeta } from 'quasar';

useMeta({ title: 'Invitación — PYMEQ' });
import { useRoute, useRouter } from 'vue-router';
import { authService } from '../services/auth.service';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';
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
.accept-invitation-page-wrapper {
  width: 100%;
}
</style>
