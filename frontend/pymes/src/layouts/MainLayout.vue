<template>
  <q-layout view="lHh Lpr lFf">
    <!-- Header with Brand Glow -->
    <q-header elevated class="bg-dark text-secondary brand-glow">
      <q-toolbar class="q-px-lg">
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="toggleLeftDrawer" />

        <q-toolbar-title class="q-ml-md">
          <span class="mesh-text-gradient text-h6 font-bold">PYMEQ</span>
          <span class="q-ml-xs text-weight-thin">Audit Toolkit</span>
        </q-toolbar-title>

        <div class="text-caption text-accent">v0.1.0</div>
      </q-toolbar>
    </q-header>

    <!-- Sidebar (Surface Pine - Part of the 3:9 Grid) -->
    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      bordered
      :width="300"
      class="bg-surface-pine text-secondary"
    >
      <q-scroll-area class="fit">
        <q-list padding>
          <q-item-label header class="text-accent text-overline q-pt-md">
            AUDITORÍA & CONTROL
          </q-item-label>

          <q-item
            v-for="link in linksList"
            :key="link.title"
            clickable
            v-ripple
            class="q-mx-sm q-my-xs rounded-borders"
            active-class="bg-primary text-white brand-glow"
          >
            <q-item-section avatar>
              <q-icon :name="link.icon" />
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ link.title }}</q-item-label>
              <q-item-label caption class="text-accent">{{ link.caption }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-separator dark class="q-my-md q-mx-sm" />

          <q-item
            clickable
            v-ripple
            class="q-mx-sm q-my-xs rounded-borders text-negative"
            @click="handleLogout"
          >
            <q-item-section avatar>
              <q-icon name="logout" color="negative" />
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-weight-bold">Cerrar Sesión</q-item-label>
              <q-item-label caption class="text-accent">Finalizar Centro de Control</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-scroll-area>
    </q-drawer>

    <!-- Main Workspace (Part of the 3:9 Grid) -->
    <q-page-container class="bg-forest-deep">
      <div class="q-pa-xl" style="max-width: 1400px; margin: 0 auto">
        <router-view />
      </div>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useRouter } from 'vue-router';

const $q = useQuasar();
const authStore = useAuthStore();
const router = useRouter();

onMounted(() => {
  // Force dark mode to match the Deep Forest theme
  $q.dark.set(true);
});

const handleLogout = async () => {
  try {
    const response = await authStore.logout();
    const allSessions = response?.data?.allSessionsRevoked;

    $q.notify({
      type: 'info',
      message: allSessions ? 'Todas las sesiones cerradas' : 'Sesión finalizada',
      caption: allSessions 
        ? 'Se han invalidado todos los accesos de tu cuenta' 
        : 'Hasta pronto en Pymeq',
      position: 'top-right',
      icon: allSessions ? 'security' : 'logout'
    });
    
    // clearSession ya redirige a #/login, pero router push asegura el estado local
    void router.push('/login');
  } catch (error) {
    console.error('Error durante el cierre de sesión', error);
  }
};

const linksList = [
  {
    title: 'Dashboard',
    caption: 'Resumen Financiero',
    icon: 'dashboard',
    link: '#',
  },
  {
    title: 'Auditorías',
    caption: 'Gestión de Controles',
    icon: 'security',
    link: '#',
  },
  {
    title: 'Reportes',
    caption: 'Análisis de Riesgo',
    icon: 'analytics',
    link: '#',
  },
  {
    title: 'Configuración',
    caption: 'Parámetros del Sistema',
    icon: 'settings',
    link: '#',
  },
];

const leftDrawerOpen = ref(false);

function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value;
}
</script>

<style lang="scss">
.bg-forest-deep {
  background-color: #0b1210;
}
.rounded-borders {
  border-radius: 8px;
}
</style>
