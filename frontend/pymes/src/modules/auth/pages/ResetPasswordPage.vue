<template>
  <div class="reset-password-page-wrapper">
    <SkeletonLoader :is-loading="initialLoading" layout="form">
      <BaseCard variant="elevated" class="q-pa-lg">
        <div class="text-center q-mb-lg">
          <div class="text-h6 text-weight-medium q-mb-xs">Nueva Contraseña</div>
          <div class="text-caption text-accent">Establece tus nuevas credenciales</div>
        </div>

        <q-form @submit.prevent="handleResetPassword" class="q-gutter-y-md">
          <q-input
            v-model="passwordForm.password"
            label="Nueva Contraseña"
            :type="showPassword ? 'text' : 'password'"
            placeholder="Mínimo 8 caracteres"
            dark filled color="primary" label-color="accent"
            class="focus-ring radius-xs"
            :rules="[val => !!val || 'La contraseña es requerida', val => val.length >= 8 || 'Mínimo 8 caracteres']"
          >
            <template v-slot:prepend><q-icon name="lock" color="primary" /></template>
            <template v-slot:append>
              <q-icon
                :name="showPassword ? 'visibility' : 'visibility_off'"
                class="cursor-pointer"
                color="primary"
                @click="showPassword = !showPassword"
              />
            </template>
          </q-input>

          <q-input
            v-model="passwordForm.confirmPassword"
            label="Confirmar Contraseña"
            :type="showConfirmPassword ? 'text' : 'password'"
            placeholder="Repite tu contraseña"
            dark filled color="primary" label-color="accent"
            class="focus-ring radius-xs"
            :error="passwordMismatch"
            error-message="Las contraseñas no coinciden"
            :rules="[val => !!val || 'Confirma tu contraseña']"
          >
            <template v-slot:prepend><q-icon name="lock_outline" color="primary" /></template>
            <template v-slot:append>
              <q-icon
                :name="showConfirmPassword ? 'visibility' : 'visibility_off'"
                class="cursor-pointer"
                color="primary"
                @click="showConfirmPassword = !showConfirmPassword"
              />
            </template>
          </q-input>

          <div class="q-mt-xl">
            <BaseButton
              label="CAMBIAR CONTRASEÑA"
              type="submit"
              class="full-width"
              size="lg"
              :loading="loading"
              :disabled="passwordMismatch || !passwordForm.password"
            >
              CAMBIAR CONTRASEÑA
            </BaseButton>
          </div>
        </q-form>
      </BaseCard>
    </SkeletonLoader>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { authService } from '../services/auth.service';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();

const token = route.query.token as string;
const email = route.query.email as string;

const loading = ref(false);
const initialLoading = ref(true);
const showPassword = ref(false);
const showConfirmPassword = ref(false);

const passwordForm = reactive({
  password: '',
  confirmPassword: ''
});

const passwordMismatch = computed(() => {
  return passwordForm.confirmPassword !== '' && passwordForm.password !== passwordForm.confirmPassword;
});

onMounted(() => {
  if (!token || !email) {
    $q.notify({
      type: 'negative',
      message: 'Enlace inválido',
      caption: 'Faltan parámetros de seguridad'
    });
    void router.push('/login');
    return;
  }
  
  setTimeout(() => {
    initialLoading.value = false;
  }, 600);
});

const handleResetPassword = async () => {
  loading.value = true;
  try {
    await authService.resetPassword({
      token,
      newPassword: passwordForm.password
    });

    $q.notify({
      type: 'positive',
      message: 'Contraseña actualizada',
      caption: 'Ya puedes iniciar sesión con tu nueva contraseña',
      position: 'top-right'
    });

    void router.push('/login');
  } catch (err: unknown) {
    const message = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje;
    $q.notify({
      type: 'negative',
      message: 'Error al cambiar contraseña',
      caption: message || 'El enlace puede haber expirado',
      position: 'top-right'
    });
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss" scoped>
.reset-password-page-wrapper {
  width: 100%;
}

:deep(.q-field--filled .q-field__control) {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(113, 131, 127, 0.1);
  transition: all 0.2s ease;
  
  &:hover {
    background: rgba(0, 0, 0, 0.3);
    border-color: rgba(163, 120, 94, 0.3);
  }
}

:deep(.q-field--focused .q-field__control) {
  border-color: $primary;
  box-shadow: 0 0 10px rgba(163, 120, 94, 0.2);
}
</style>