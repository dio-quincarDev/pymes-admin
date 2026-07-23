<template>
  <div class="accept-invitation-page-wrapper">
    <SkeletonLoader :is-loading="loadingInfo || submitting" layout="card">
      <BaseCard variant="elevated" class="q-pa-lg">
        <div class="text-center q-mb-lg">
          <div class="text-caption q-mb-xs" style="color: var(--pq-text-subtle); letter-spacing: 1px; text-transform: uppercase; font-size: 11px; font-weight: 500;">INVITACIÓN AL EQUIPO</div>
          <div class="text-h6 text-weight-bold q-mt-sm" style="color: var(--pq-text);">{{ tenantName || 'Un equipo' }}</div>
          <p v-if="invitationEmail" class="text-body2 q-mt-xs" style="color: var(--pq-text-muted);">
            Fuiste invitado como <strong>{{ emailMask }}</strong>
          </p>
        </div>

        <div v-if="success" class="text-center q-py-lg">
          <q-icon name="group_add" color="positive" size="4rem" class="q-mb-md" />
          <div class="text-h6" style="color: var(--pq-text);">¡Bienvenido al Equipo!</div>
          <p style="color: var(--pq-text-muted);" class="q-mt-sm">Ahora eres parte de <strong>{{ tenantName }}</strong>.</p>
          <BaseButton tag="router-link" to="/dashboard" class="full-width q-mt-md" size="lg">
            IR AL PANEL DE CONTROL
          </BaseButton>
        </div>

        <div v-else-if="error" class="text-center q-py-lg">
          <q-icon name="error" color="negative" size="4rem" class="q-mb-md" />
          <div class="text-h6" style="color: var(--pq-danger);">Error de Invitación</div>
          <p style="color: var(--pq-text-muted);" class="q-mt-sm">{{ errorMessage }}</p>
          <BaseButton variant="ghost" tag="router-link" to="/" class="q-mt-md">
            Volver al inicio
          </BaseButton>
        </div>

        <div v-else-if="!authStore.isAuthenticated" class="q-py-md">
          <q-form @submit="onRegister" ref="formRef" class="q-gutter-y-md">
            <q-input
              :model-value="invitationEmail"
              label="Email"
              dark filled color="primary" label-color="accent"
              class="focus-ring radius-xs"
              readonly
            >
              <template v-slot:prepend><q-icon name="email" color="primary" /></template>
            </q-input>
            <q-input
              v-model="form.name"
              label="Nombre"
              placeholder="Tu nombre completo"
              dark filled color="primary" label-color="accent"
              class="focus-ring radius-xs"
              :disable="submitting"
              :rules="[(val) => !!val || 'Nombre requerido']"
            >
              <template v-slot:prepend><q-icon name="person" color="primary" /></template>
            </q-input>
            <q-input
              v-model="form.password"
              label="Contraseña"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Mínimo 8 caracteres"
              dark filled color="primary" label-color="accent"
              class="focus-ring radius-xs"
              :disable="submitting"
              :rules="[(val) => !!val || 'Contraseña requerida', (val) => val.length >= 8 || 'Mínimo 8 caracteres']"
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
              v-model="form.confirmPassword"
              label="Confirmar Contraseña"
              :type="showConfirmPassword ? 'text' : 'password'"
              placeholder="Repite tu contraseña"
              dark filled color="primary" label-color="accent"
              class="focus-ring radius-xs"
              :disable="submitting"
              :error="!!confirmPasswordError"
              :error-message="confirmPasswordError"
              :rules="[(val) => !!val || 'Confirma tu contraseña']"
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
              <BaseButton variant="primary" type="submit" class="full-width" size="lg" :loading="submitting">
                CREAR CUENTA Y ACEPTAR
              </BaseButton>
            </div>
          </q-form>
        </div>

        <div v-else class="text-center q-py-md">
          <BaseButton
            class="full-width"
            size="lg"
            :loading="submitting"
            @click="onAccept"
          >
            ACEPTAR INVITACIÓN
          </BaseButton>
        </div>
      </BaseCard>
    </SkeletonLoader>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useMeta } from 'quasar';

useMeta({ title: 'Invitación — PYMEQ' });
import { useRoute } from 'vue-router';
import { authService } from '../services/auth.service';
import { invitationService } from '../services/invitation.service';
import { useAuthStore } from '../store';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';
import type { ApiResponse, AuthResponse, InvitationInfo, InvitationResponse } from '../types';

const route = useRoute();
const authStore = useAuthStore();

const token = ref(route.query.token as string);
const loadingInfo = ref(false);
const submitting = ref(false);
const success = ref(false);
const error = ref(false);
const errorMessage = ref('');
const invitationEmail = ref('');
const tenantName = ref('');
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const confirmPasswordError = ref('');
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const form = ref({ name: '', password: '', confirmPassword: '' });

const emailMask = computed(() => invitationEmail.value || 'invitado');

watch(() => form.value.password, () => {
  if (form.value.confirmPassword && form.value.password !== form.value.confirmPassword) {
    confirmPasswordError.value = 'Las contraseñas no coinciden';
  } else {
    confirmPasswordError.value = '';
  }
});

watch(() => form.value.confirmPassword, () => {
  if (form.value.confirmPassword && form.value.password !== form.value.confirmPassword) {
    confirmPasswordError.value = 'Las contraseñas no coinciden';
  } else {
    confirmPasswordError.value = '';
  }
});

onMounted(async () => {
  if (!token.value) {
    error.value = true;
    errorMessage.value = 'Token de invitación ausente.';
    return;
  }
  const valid = await loadInvitationInfo();
  if (!valid) return;
  if (authStore.isAuthenticated) {
    void doAccept();
  }
});

async function loadInvitationInfo(): Promise<boolean> {
  loadingInfo.value = true;
  try {
    const res = await invitationService.getInvitationInfo(token.value);
    const info = (res.data as ApiResponse<InvitationInfo>).data;
    invitationEmail.value = info.email;
    tenantName.value = info.tenantName;
    return true;
  } catch (err: unknown) {
    error.value = true;
    const resp = (err as { response?: { data?: { mensaje?: string } } })?.response?.data;
    errorMessage.value = resp?.mensaje || 'El enlace de invitación no es válido o ya expiró.';
    return false;
  } finally {
    loadingInfo.value = false;
  }
}

async function onRegister() {
  if (!(await formRef.value?.validate())) return;
  if (confirmPasswordError.value) return;
  submitting.value = true;
  try {
    const res = await invitationService.registerAndAccept(token.value, {
      name: form.value.name,
      email: invitationEmail.value,
      password: form.value.password,
    });
    const authData = (res.data as ApiResponse<AuthResponse>).data;
    if (authData.accessToken) {
      const user = authData.activeTenant
        ? { ...authData.user, tenantId: authData.activeTenant.id, plan: authData.activeTenant.plan }
        : authData.user;
      authStore.setSession(authData.accessToken, authData.refreshToken, user);
    }
    success.value = true;
  } catch (err: unknown) {
    const resp = (err as { response?: { data?: { mensaje?: string; codigo?: string } } })?.response?.data;
    const codigo = resp?.codigo;
    if (codigo === 'AUTH001') {
      errorMessage.value = 'Ya existe una cuenta con este email. Iniciá sesión en otra ventana y volvé a abrir este enlace.';
    } else {
      errorMessage.value = resp?.mensaje || 'Error al crear la cuenta.';
    }
    error.value = true;
  } finally {
    submitting.value = false;
  }
}

async function doAccept() {
  submitting.value = true;
  try {
    const response = await authService.acceptInvitation(token.value);
    const apiResponse = response.data as ApiResponse<InvitationResponse>;
    const data = apiResponse.data;
    if (data?.tenantName) {
      tenantName.value = data.tenantName;
    }
    if (data?.tenantId) {
      await authStore.selectTenant(data.tenantId);
    }
    success.value = true;
  } catch (err: unknown) {
    const resp = (err as { response?: { data?: { mensaje?: string } } })?.response?.data;
    error.value = true;
    errorMessage.value = resp?.mensaje || 'No se pudo procesar la invitación.';
    // ponytail: we keep the active session on error so the user isn't kicked out of their account on invitation accept failures.
  } finally {
    submitting.value = false;
  }
}

const onAccept = doAccept;
</script>

<style lang="scss" scoped>
.accept-invitation-page-wrapper {
  width: 100%;
}
</style>
