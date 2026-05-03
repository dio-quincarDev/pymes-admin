<template>
  <q-page class="flex flex-center bg-forest-deep">
    <div class="auth-card-container q-pa-md">
      <!-- Logo & Branding -->
      <div class="text-center q-mb-xl">
        <div class="text-h3 font-bold text-primary q-mb-xs">PYMEQ</div>
        <div class="text-subtitle1 text-accent text-weight-light letter-spacing-2">
          INVITACIÓN AL EQUIPO
        </div>
      </div>

      <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
        <q-card-section class="text-center q-pb-none">
          <div class="text-h6 text-weight-medium q-mb-sm">Aceptar Invitación</div>
          <p class="text-body2 text-accent">
            Has sido invitado a colaborar en un espacio de trabajo. Confirma para unirte al equipo.
          </p>
        </q-card-section>

        <q-card-section>
          <div v-if="success" class="text-center q-py-lg">
            <q-icon name="group_add" color="positive" size="4rem" class="q-mb-md" />
            <div class="text-h6">¡Bienvenido al equipo!</div>
            <p class="text-accent q-mt-sm">Ahora eres parte de <strong>{{ tenantName }}</strong>.</p>
            <q-btn label="Ir al Dashboard" color="primary" unelevated to="/dashboard" no-caps class="full-width q-mt-md" />
          </div>

          <div v-else-if="error" class="text-center q-py-lg">
            <q-icon name="error" color="negative" size="4rem" class="q-mb-md" />
            <div class="text-h6">Error al aceptar</div>
            <p class="text-accent q-mt-sm">{{ errorMessage }}</p>
            <q-btn flat label="Ir al Login" color="primary" to="/login" no-caps class="q-mt-md" />
          </div>

          <div v-else class="text-center q-py-md">
            <q-btn
              label="UNIRME AL EQUIPO"
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
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authService } from '../services/auth.service';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';

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
    const data = response.data.data;
    tenantName.value = data.tenant?.name || 'la empresa';
    success.value = true;
    $q.notify({
      type: 'positive',
      message: 'Has aceptado la invitación correctamente.'
    });
  } catch (err: unknown) {
    error.value = true;
    const responseData = (err as { response?: { data?: { mensaje?: string } } })?.response?.data;
    errorMessage.value = responseData?.mensaje || 'No se pudo aceptar la invitación. El enlace puede haber expirado o ya fue utilizado.';
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss" scoped>
.auth-card-container {
  width: 100%;
  max-width: 450px;
}
.letter-spacing-2 {
  letter-spacing: 2px;
}
</style>
