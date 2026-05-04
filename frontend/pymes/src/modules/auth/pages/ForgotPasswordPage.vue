<template>
  <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
    <q-card-section class="text-center q-pb-none">
      <div class="text-h6 text-weight-medium q-mb-sm">¿Olvidaste tu contraseña?</div>
      <p class="text-body2 text-accent">
        Introduce tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña.
      </p>
    </q-card-section>

    <q-card-section>
      <div v-if="submitted" class="text-center q-py-lg">
        <q-icon name="check_circle" color="positive" size="4rem" class="q-mb-md" />
        <div class="text-h6">Correo enviado</div>
        <p class="text-accent q-mt-sm">
          Si existe una cuenta asociada a <strong>{{ email }}</strong>, recibirás un correo con instrucciones en unos minutos.
        </p>
        <q-btn flat label="Volver al Login" color="primary" to="/login" no-caps class="q-mt-md" />
      </div>

      <q-form v-else @submit="onSubmit" class="q-gutter-y-md">
        <q-input
          v-model="email"
          label="Correo Electrónico"
          dark filled color="primary" label-color="accent"
          :rules="[val => !!val || 'El email es requerido', val => /.+@.+\..+/.test(val) || 'Email inválido']"
        >
          <template v-slot:prepend><q-icon name="email" color="primary" /></template>
        </q-input>

        <div class="q-mt-xl">
          <q-btn
            label="ENVIAR ENLACE"
            type="submit"
            color="primary"
            class="full-width brand-glow text-weight-bold"
            size="lg"
            :loading="loading"
          />
        </div>

        <div class="text-center q-mt-md">
          <q-btn flat label="Volver al Login" color="accent" to="/login" no-caps />
        </div>
      </q-form>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { authService } from '../services/auth.service';
import { useQuasar } from 'quasar';

const $q = useQuasar();

const email = ref('');
const loading = ref(false);
const submitted = ref(false);

const onSubmit = async () => {
  loading.value = true;
  try {
    await authService.forgotPassword(email.value);
    submitted.value = true;
  } catch (err: unknown) {
    const message = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje;
    $q.notify({
      type: 'negative',
      message: message || 'Error al procesar la solicitud. Intenta de nuevo más tarde.',
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
