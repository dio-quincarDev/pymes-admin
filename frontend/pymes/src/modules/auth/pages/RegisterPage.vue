<template>
  <q-page class="flex flex-center bg-forest-deep">
    <div class="register-card-container q-pa-md">
      <!-- Logo & Branding -->
      <div class="text-center q-mb-xl">
        <div class="text-h3 font-bold text-primary q-mb-xs">PYMEQ</div>
        <div class="text-subtitle1 text-accent text-weight-light letter-spacing-2">
          REGISTRO DE ADMINISTRADOR
        </div>
      </div>

      <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
        <div v-if="pendingTenant" class="q-mb-lg bg-dark q-pa-md rounded-borders">
          <div class="text-overline text-accent">Espacio de trabajo</div>
          <div class="text-h6 text-primary">{{ pendingTenant.name }}</div>
          <div class="text-caption text-accent">Slug: {{ pendingTenant.slug }}</div>
        </div>

        <q-form @submit="onRegister" class="q-gutter-y-md">
          <q-input
            v-model="registerForm.name"
            label="Nombre Completo"
            dark filled color="primary" label-color="accent"
            :rules="[val => !!val || 'El nombre es requerido']"
          >
            <template v-slot:prepend><q-icon name="badge" color="primary" /></template>
          </q-input>

          <q-input
            v-model="registerForm.email"
            label="Correo Electrónico"
            dark filled color="primary" label-color="accent"
            :rules="[val => !!val || 'El email es requerido', val => /.+@.+\..+/.test(val) || 'Email inválido']"
          >
            <template v-slot:prepend><q-icon name="email" color="primary" /></template>
          </q-input>

          <q-input
            v-model="registerForm.password"
            label="Contraseña"
            type="password"
            dark filled color="primary" label-color="accent"
            :rules="[val => !!val || 'La contraseña es requerida', val => val.length >= 8 || 'Mínimo 8 caracteres']"
          >
            <template v-slot:prepend><q-icon name="lock" color="primary" /></template>
          </q-input>

          <div class="q-mt-xl">
            <q-btn
              label="FINALIZAR REGISTRO"
              type="submit"
              color="primary"
              class="full-width brand-glow text-weight-bold"
              size="lg"
              :loading="loading"
            />
          </div>

          <div class="q-mt-md text-center">
            <q-btn flat label="Volver a elegir método" color="accent" to="/auth-options" no-caps />
          </div>
        </q-form>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

const authStore = useAuthStore();
const { pendingTenant } = storeToRefs(authStore);
const $q = useQuasar();
const router = useRouter();

const loading = ref(false);

const registerForm = reactive({
  name: '',
  email: '',
  password: ''
});

onMounted(() => {
  if (!pendingTenant.value) {
    $q.notify({
      message: 'Debes configurar tu empresa primero',
      color: 'warning',
      position: 'top'
    });
    void router.push('/');
  }
});

const onRegister = async () => {
  if (!pendingTenant.value) return;

  loading.value = true;
  try {
    const fullPayload = {
      ...registerForm,
      companyName: pendingTenant.value.name,
      companySlug: pendingTenant.value.slug
    };

    await authStore.register(fullPayload);

    $q.notify({
      type: 'positive',
      message: 'Cuenta creada con éxito',
      caption: 'Revisa tu email para verificar la cuenta',
      position: 'top-right'
    });

    authStore.clearPendingTenant();
    void router.push('/login');
  } catch {
    $q.notify({
      type: 'negative',
      message: 'Error en el registro',
      caption: authStore.error || 'No se pudo crear la cuenta',
      position: 'top-right'
    });
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss" scoped>
.register-card-container {
  width: 100%;
  max-width: 500px;
}
.letter-spacing-2 {
  letter-spacing: 2px;
}
.rounded-borders {
  border-radius: 8px;
}
</style>
