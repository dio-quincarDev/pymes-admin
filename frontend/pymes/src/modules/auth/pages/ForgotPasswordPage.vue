<template>
  <div class="forgot-password-page-wrapper">
    <SkeletonLoader :is-loading="initialLoading" layout="form">
      <BaseCard variant="elevated" class="q-pa-lg">
        <div class="text-center q-mb-lg">
          <div class="text-h6 text-weight-medium q-mb-xs">Recuperar Contraseña</div>
          <div class="text-caption text-accent">Te enviaremos un enlace de restauración</div>
        </div>

        <div v-if="!submitted">
          <q-form @submit.prevent="handleForgotPassword" class="q-gutter-y-md">
            <q-input
              v-model="email"
              label="Correo Electrónico"
              dark
              filled
              color="primary"
              label-color="accent"
              class="focus-ring radius-xs"
              :rules="[val => !!val || 'El email es requerido', val => /.+@.+\..+/.test(val) || 'Email inválido']"
            >
              <template v-slot:prepend>
                <q-icon name="email" color="primary" />
              </template>
            </q-input>

            <div class="q-mt-xl">
              <BaseButton
                label="ENVIAR ENLACE"
                type="submit"
                class="full-width"
                size="lg"
                :loading="loading"
              >
                ENVIAR ENLACE
              </BaseButton>
            </div>

            <div class="text-center q-mt-md">
              <BaseButton variant="ghost" size="sm" @click="router.push('/login')">
                Volver al Login
              </BaseButton>
            </div>
          </q-form>
        </div>

        <div v-else class="text-center fade-in-up">
          <q-icon name="mark_email_read" color="primary" size="5em" class="q-mb-md brand-glow" />
          <div class="text-h6 text-primary">Email Enviado</div>
          <p class="text-body2 text-accent q-mt-sm">
            Si la cuenta <strong>{{ email }}</strong> existe, recibirás un enlace para cambiar tu contraseña en breve.
          </p>
          <div class="q-mt-xl">
            <BaseButton
              label="VOLVER AL LOGIN"
              class="full-width"
              size="lg"
              @click="router.push('/login')"
            >
              VOLVER AL LOGIN
            </BaseButton>
          </div>
        </div>
      </BaseCard>
    </SkeletonLoader>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { authService } from '../services/auth.service';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';

const router = useRouter();
const $q = useQuasar();

const email = ref('');
const loading = ref(false);
const initialLoading = ref(true);
const submitted = ref(false);

onMounted(() => {
  setTimeout(() => {
    initialLoading.value = false;
  }, 600);
});

const handleForgotPassword = async () => {
  loading.value = true;
  try {
    await authService.forgotPassword(email.value);
    submitted.value = true;
    $q.notify({
      type: 'positive',
      message: 'Email enviado',
      caption: 'Revisa tu bandeja de entrada',
      position: 'top-right'
    });
  } catch (err: unknown) {
    const message = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje;
    $q.notify({
      type: 'negative',
      message: 'Error al solicitar enlace',
      caption: message || 'Inténtalo de nuevo más tarde',
      position: 'top-right'
    });
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss" scoped>
.forgot-password-page-wrapper {
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