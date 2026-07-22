<template>
  <div class="accept-invitation-page-wrapper">
    <SkeletonLoader :is-loading="loadingInfo || submitting" layout="card">
      <BaseCard variant="elevated" class="q-pa-lg">
        <div class="text-center q-mb-lg">
          <div class="text-h6 text-weight-medium q-mb-sm">Invitación al Equipo</div>
          <p v-if="invitationEmail" class="text-body2" style="color: var(--pq-text-muted);">
            Fuiste invitado a <strong>{{ tenantName || 'un equipo' }}</strong> como {{ emailMask }}
          </p>
        </div>

        <div v-if="success" class="text-center q-py-lg fade-in-up">
          <q-icon name="group_add" color="positive" size="4rem" class="q-mb-md" />
          <div class="text-h6" style="color: var(--pq-text);">¡Bienvenido al Equipo!</div>
          <p style="color: var(--pq-text-muted);" class="q-mt-sm">Ahora eres parte de <strong>{{ tenantName }}</strong>.</p>
          <BaseButton class="full-width q-mt-md" size="lg" to="/dashboard">
            IR AL PANEL DE CONTROL
          </BaseButton>
        </div>

        <div v-else-if="error" class="text-center q-py-lg">
          <q-icon name="error" color="negative" size="4rem" class="q-mb-md" />
          <div class="text-h6" style="color: var(--pq-danger);">Error de Invitación</div>
          <p style="color: var(--pq-text-muted);" class="q-mt-sm">{{ errorMessage }}</p>
          <BaseButton variant="ghost" class="q-mt-md" to="/">
            Volver al inicio
          </BaseButton>
        </div>

        <div v-else-if="!authStore.isAuthenticated" class="q-py-md">
          <q-form @submit="onRegister" ref="formRef">
            <q-input
              :model-value="invitationEmail"
              label="Email"
              dark
              dense
              filled
              readonly
              class="q-mb-md"
            />
            <q-input
              v-model="form.name"
              label="Nombre"
              dark
              dense
              filled
              :rules="[(val) => !!val || 'Nombre requerido']"
              class="q-mb-md"
            />
            <q-input
              v-model="form.password"
              label="Contraseña"
              type="password"
              dark
              dense
              filled
              :rules="[(val) => !!val || 'Contraseña requerida', (val) => val.length >= 8 || 'Mínimo 8 caracteres']"
              class="q-mb-md"
            />
            <BaseButton variant="primary" type="submit" class="full-width" size="lg" :loading="submitting">
              CREAR CUENTA Y ACEPTAR
            </BaseButton>
          </q-form>
        </div>

        <div v-else class="text-center q-py-md">
          <BaseButton
            label="ACEPTAR INVITACIÓN"
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
import { ref, onMounted, computed } from 'vue';
import { useMeta } from 'quasar';

useMeta({ title: 'Invitación — PYMEQ' });
import { useRoute } from 'vue-router';
import { authService } from '../services/auth.service';
import { invitationService } from '../services/invitation.service';
import { useAuthStore } from '../store';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';
import type { ApiResponse, InvitationInfo } from '../types';

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
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const form = ref({ name: '', password: '' });

const emailMask = computed(() => invitationEmail.value || 'invitado');

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
  submitting.value = true;
  try {
    await authStore.register({
      name: form.value.name,
      email: invitationEmail.value,
      password: form.value.password,
      invitationToken: token.value,
    });
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
    const apiResponse = response.data as ApiResponse<{ tenant?: { name: string } }>;
    const data = apiResponse.data;
    if (data?.tenant?.name) {
      tenantName.value = data.tenant.name;
    }
    success.value = true;
  } catch (err: unknown) {
    const resp = (err as { response?: { data?: { mensaje?: string } } })?.response?.data;
    error.value = true;
    errorMessage.value = resp?.mensaje || 'No se pudo procesar la invitación.';
    if (authStore.isAuthenticated) {
      await authStore.logout();
    }
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
