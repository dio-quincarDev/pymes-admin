<template>
  <div class="accept-invitation-page-wrapper">
    <SkeletonLoader :is-loading="loadingInfo" layout="card">
      <BaseCard variant="elevated" class="q-pa-lg">
        <div class="text-center q-mb-lg">
          <div class="text-h6 text-weight-medium q-mb-sm">Invitación al Equipo</div>
          <p class="text-body2" style="color: var(--pq-text-muted);">
            Has sido invitado a colaborar en <strong>{{ invitationInfo?.tenantName || '...' }}</strong>
          </p>
        </div>

        <div v-if="success" class="text-center q-py-lg fade-in-up">
          <q-icon name="group_add" color="positive" size="4rem" class="q-mb-md" />
          <div class="text-h6" style="color: var(--pq-text);">¡Bienvenido al Equipo!</div>
          <p style="color: var(--pq-text-muted);" class="q-mt-sm">Ahora eres parte de <strong>{{ invitationInfo?.tenantName }}</strong>.</p>
          <BaseButton
            label="IR AL PANEL DE CONTROL"
            class="full-width q-mt-md"
            size="lg"
            @click="$router.push('/dashboard')"
          />
        </div>

        <div v-else-if="error" class="text-center q-py-lg">
          <q-icon name="error" color="negative" size="4rem" class="q-mb-md" />
          <div class="text-h6" style="color: var(--pq-danger);">Error de Invitación</div>
          <p style="color: var(--pq-text-muted);" class="q-mt-sm">{{ errorMessage }}</p>
          <BaseButton variant="ghost" class="q-mt-md" @click="$router.push('/login')">
            Volver al Login
          </BaseButton>
        </div>

        <template v-else-if="invitationInfo">
          <div v-if="emailMismatch" class="text-center q-py-md">
            <q-icon name="warning" color="amber" size="3rem" class="q-mb-md" />
            <div class="text-h6" style="color: var(--pq-text);">Email no coincide</div>
            <p style="color: var(--pq-text-muted);" class="q-mt-sm">
              Esta invitación es para <strong>{{ invitationInfo.email }}</strong>,<br>
              pero estás logueado como <strong>{{ authStore.user?.email }}</strong>.
            </p>
            <BaseButton
              label="CERRAR SESIÓN Y REGISTRARME"
              class="full-width q-mt-md"
              size="lg"
              color="negative"
              :loading="loading"
              @click="onLogoutAndRegister"
            />
            <BaseButton variant="ghost" class="q-mt-md" @click="$router.push('/')">
              Cancelar
            </BaseButton>
          </div>

          <div v-else-if="authStore.isAuthenticated" class="text-center q-py-md">
            <p class="text-body2 q-mb-md" style="color: var(--pq-text-muted);">
              Invitado como <strong>{{ invitationInfo.email }}</strong>
            </p>
            <BaseButton
              label="ACEPTAR Y UNIRME AL EQUIPO"
              class="full-width"
              size="lg"
              :loading="loading"
              @click="onAccept"
            />
            <BaseButton variant="ghost" class="q-mt-md" @click="$router.push('/')">
              Cancelar
            </BaseButton>
          </div>

          <div v-else class="q-py-md">
            <q-form @submit.prevent="onRegister" class="q-gutter-md">
              <q-input
                v-model="form.name"
                label="Tu nombre"
                dark
                filled
                color="primary"
                label-color="accent"
                dense
                :rules="[val => !!val || 'Nombre requerido']"
              />
              <q-input
                v-model="form.email"
                :label="invitationInfo.email"
                dark
                filled
                color="primary"
                label-color="accent"
                dense
                disable
              />
              <q-input
                v-model="form.password"
                label="Contraseña"
                type="password"
                dark
                filled
                color="primary"
                label-color="accent"
                dense
                :rules="[val => val && val.length >= 8 || 'Mínimo 8 caracteres']"
              />
              <q-input
                v-model="form.confirmPassword"
                label="Confirmar contraseña"
                type="password"
                dark
                filled
                color="primary"
                label-color="accent"
                dense
                :rules="[val => val === form.password || 'Las contraseñas no coinciden']"
              />
              <BaseButton
                label="CREAR CUENTA Y UNIRME"
                class="full-width"
                size="lg"
                type="submit"
                :loading="loading"
              />
            </q-form>
            <BaseButton variant="ghost" class="q-mt-md full-width" @click="$router.push('/login')">
              Ya tengo cuenta — Iniciar sesión
            </BaseButton>
          </div>
        </template>
      </BaseCard>
    </SkeletonLoader>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import { useRoute } from 'vue-router';
import { invitationService } from '../services/invitation.service';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';
import type { ApiResponse, InvitationInfo, AuthResponse } from '../types';

const route = useRoute();
const authStore = useAuthStore();
const $q = useQuasar();

const token = ref(route.query.token as string);
const loadingInfo = ref(true);
const loading = ref(false);
const success = ref(false);
const error = ref(false);
const errorMessage = ref('');
const invitationInfo = ref<InvitationInfo | null>(null);

const emailMismatch = computed(() =>
  authStore.isAuthenticated && invitationInfo.value && authStore.user?.email !== invitationInfo.value.email
);

const form = reactive({
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
});

onMounted(async () => {
  if (!token.value) {
    error.value = true;
    errorMessage.value = 'Token de invitación ausente.';
    loadingInfo.value = false;
    return;
  }

  try {
    const response = await invitationService.getInvitationInfo(token.value);
    const apiResponse = response.data as ApiResponse<InvitationInfo>;
    invitationInfo.value = apiResponse.data;
    form.email = apiResponse.data.email;
  } catch {
    error.value = true;
    errorMessage.value = 'Invitación no válida o expirada.';
  } finally {
    loadingInfo.value = false;
  }
});

const onLogoutAndRegister = () => {
  loading.value = true;
  localStorage.removeItem('pymeq_token');
  localStorage.removeItem('pymeq_refresh_token');
  localStorage.removeItem('pymeq_user');
  window.location.reload();
};

const onAccept = async () => {
  loading.value = true;
  try {
    await invitationService.acceptInvitation(token.value);
    await authStore.fetchCurrentUser();
    success.value = true;
    $q.notify({ type: 'positive', message: 'Acceso concedido correctamente.' });
  } catch (err: unknown) {
    error.value = true;
    const responseData = (err as { response?: { data?: { mensaje?: string } } })?.response?.data;
    errorMessage.value = responseData?.mensaje || 'No se pudo procesar la invitación.';
  } finally {
    loading.value = false;
  }
};

const onRegister = async () => {
  loading.value = true;
  try {
    const response = await invitationService.registerAndAccept(token.value, {
      name: form.name,
      email: invitationInfo.value!.email,
      password: form.password
    });
    const apiResponse = response.data as ApiResponse<AuthResponse>;
    const data = apiResponse.data;
    if (data?.accessToken) {
      authStore.setSession(data.accessToken, data.refreshToken, {
        ...data.user,
        tenantId: data.activeTenant?.id ?? undefined
      });
    }
    success.value = true;
    $q.notify({ type: 'positive', message: 'Cuenta creada y acceso concedido.' });
  } catch (err: unknown) {
    error.value = true;
    const responseData = (err as { response?: { data?: { mensaje?: string } } })?.response?.data;
    errorMessage.value = responseData?.mensaje || 'No se pudo crear la cuenta.';
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
