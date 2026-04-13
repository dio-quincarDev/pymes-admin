<template>
  <q-layout view="lHh Lpr lFf">
    <q-page-container>
      <q-page class="flex flex-center bg-forest-deep">
        <div class="register-card-container q-pa-md">
          <!-- Logo & Branding -->
          <div class="text-center q-mb-xl">
            <div class="text-h3 font-bold mesh-text-gradient q-mb-xs">PYMEQ</div>
            <div class="text-subtitle1 text-accent text-weight-light letter-spacing-2">
              NEW AUDIT ENVIRONMENT
            </div>
          </div>

          <q-card class="bg-surface-pine text-secondary tight-shadow q-pa-lg no-border-radius-custom">
            <q-card-section class="text-center">
              <div class="text-h6 text-weight-medium q-mb-md">Registro de Entidad</div>
              <div class="text-caption text-accent">Configura tu espacio de auditoría inteligente</div>
            </q-card-section>

            <q-card-section>
              <q-form @submit="onRegister" class="q-gutter-md">
                <!-- Sección: Usuario -->
                <div class="text-overline text-primary q-mt-sm">Datos del Administrador</div>
                <q-input
                  v-model="registerForm.nombre"
                  label="Nombre Completo"
                  dark filled color="primary" label-color="accent"
                  :rules="[val => !!val || 'El nombre es requerido']"
                >
                  <template v-slot:prepend><q-icon name="person" color="primary" /></template>
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
                  label="Contraseña de Seguridad"
                  type="password"
                  dark filled color="primary" label-color="accent"
                  :rules="[val => !!val || 'La contraseña es requerida', val => val.length >= 8 || 'Mínimo 8 caracteres']"
                >
                  <template v-slot:prepend><q-icon name="lock" color="primary" /></template>
                </q-input>

                <!-- Sección: Empresa -->
                <div class="text-overline text-primary q-mt-lg">Configuración de la Empresa</div>
                <q-input
                  v-model="registerForm.companyName"
                  label="Nombre de la Empresa"
                  dark filled color="primary" label-color="accent"
                  :rules="[val => !!val || 'El nombre de la empresa es requerido']"
                  @update:model-value="generateSlug"
                >
                  <template v-slot:prepend><q-icon name="business" color="primary" /></template>
                </q-input>

                <q-input
                  v-model="registerForm.companySlug"
                  label="Identificador de Acceso (Slug)"
                  dark filled color="primary" label-color="accent"
                  hint="pymeq.com/tu-empresa"
                  :rules="[val => !!val || 'El slug es requerido']"
                >
                  <template v-slot:prepend><q-icon name="link" color="primary" /></template>
                </q-input>

                <div class="q-mt-xl">
                  <q-btn
                    label="CREAR ENTORNO PYMEQ"
                    type="submit"
                    color="primary"
                    class="full-width brand-glow text-weight-bold"
                    size="lg"
                    :loading="loading"
                  />
                </div>
              </q-form>
            </q-card-section>

            <q-card-section class="text-center q-pt-none">
              <div class="text-caption text-accent">
                ¿Ya tienes una cuenta?
                <q-btn flat no-caps label="Accede a tu cuenta" color="primary" class="q-px-xs" to="/login" />
              </div>
            </q-card-section>
          </q-card>

          <!-- Footer Info -->
          <div class="text-center q-mt-xl text-accent text-caption">
            <q-icon name="info" size="xs" class="q-mr-xs" />
            Al registrarte, aceptas nuestros términos de auditoría y privacidad.
          </div>
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useAuthStore } from '../store';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const $q = useQuasar();
const router = useRouter();

const loading = ref(false);

const registerForm = reactive({
  nombre: '',
  email: '',
  password: '',
  companyName: '',
  companySlug: ''
});

const generateSlug = (val: string | number | null) => {
  if (typeof val !== 'string') return;
  registerForm.companySlug = val
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_-]+/g, '-')
    .replace(/^-+|-+$/g, '');
};

const onRegister = async () => {
  loading.value = true;
  try {
    // 1. Llamada al servicio de registro con el payload completo
    await authStore.register(registerForm);
    
    $q.notify({
      type: 'positive',
      message: 'Empresa registrada',
      caption: 'Tu entorno Pymeq está listo',
      position: 'top-right'
    });
    void router.push('/');
  } catch {
    $q.notify({
      type: 'negative',
      message: 'Error en el registro',
      caption: authStore.error || 'No se pudo crear la entidad',
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

.no-border-radius-custom {
  border-radius: 4px;
}

.letter-spacing-2 {
  letter-spacing: 2px;
}

/* Override input background for the theme */
:deep(.q-field--filled .q-field__control) {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}
</style>
