<template>
  <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
    <q-card-section class="text-center q-pb-none">
      <div class="text-h6 text-weight-medium q-mb-sm">Restablecer Contraseña</div>
      <p class="text-body2 text-accent">
        Introduce tu nueva contraseña maestra para recuperar el acceso a tu cuenta.
      </p>
    </q-card-section>

    <q-card-section>
      <div v-if="success" class="text-center q-py-lg">
        <q-icon name="check_circle" color="positive" size="4rem" class="q-mb-md" />
        <div class="text-h6 text-primary">Contraseña actualizada</div>
        <p class="text-accent q-mt-sm">Tu contraseña ha sido cambiada correctamente.</p>
        <q-btn label="IR AL CENTRO DE CONTROL" color="primary" class="full-width brand-glow text-weight-bold q-mt-md" size="lg" to="/login" no-caps />
      </div>

      <q-form v-else @submit="onSubmit" class="q-gutter-y-md">
        <q-input
          v-model="newPassword"
          label="Nueva Contraseña"
          type="password"
          dark filled color="primary" label-color="accent"
          :rules="[val => !!val || 'La contraseña es requerida', val => val.length >= 8 || 'Mínimo 8 caracteres']"
        >
          <template v-slot:prepend><q-icon name="lock" color="primary" /></template>
        </q-input>

        <q-input
          v-model="confirmPassword"
          label="Confirmar Contraseña"
          type="password"
          dark filled color="primary" label-color="accent"
          :rules="[
            val => !!val || 'Debes confirmar la contraseña',
            val => val === newPassword || 'Las contraseñas no coinciden'
          ]"
        >
          <template v-slot:prepend><q-icon name="lock_reset" color="primary" /></template>
        </q-input>

        <div class="q-mt-xl">
          <q-btn
            label="ACTUALIZAR CONTRASEÑA"
            type="submit"
            color="primary"
            class="full-width brand-glow text-weight-bold"
            size="lg"
            :loading="loading"
          />
        </div>
      </q-form>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authService } from '../services/auth.service';
import { useQuasar } from 'quasar';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();

const token = ref(route.query.token as string);
const newPassword = ref('');
const confirmPassword = ref('');
const loading = ref(false);
const success = ref(false);

onMounted(() => {
  if (!token.value) {
    $q.notify({
      type: 'negative',
      message: 'Token de seguridad ausente. Solicita un nuevo enlace.'
    });
    void router.push('/forgot-password');
  }
});

const onSubmit = async () => {
  loading.value = true;
  try {
    await authService.resetPassword({
      token: token.value,
      newPassword: newPassword.value
    });
    success.value = true;
    $q.notify({
      type: 'positive',
      message: 'Contraseña restablecida con éxito'
    });
  } catch (err: unknown) {
    const message = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje;
    $q.notify({
      type: 'negative',
      message: message || 'No se pudo restablecer la contraseña. El enlace puede haber expirado.',
      position: 'top-right'
    });
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
