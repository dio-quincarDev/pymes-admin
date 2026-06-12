<template>
  <q-layout view="lHh Lpr lFf">
    <!-- Header with Brand Glow -->
    <q-header class="bg-dark text-secondary brand-glow">
      <q-toolbar class="q-px-lg">
        <BaseButton
          variant="ghost"
          icon-left="menu"
          class="q-mr-sm"
          @click="toggleLeftDrawer"
        />

        <q-toolbar-title>
          <span class="mesh-text-gradient text-h6 font-bold">PYMEQ</span>
          <span class="q-ml-xs text-weight-thin text-accent">Audit Toolkit</span>
        </q-toolbar-title>

        <div class="row items-center gap-sm">
          <div class="text-caption text-accent hide-mobile">v0.1.0</div>
          <q-btn round flat>
            <q-avatar size="32px">
              <img src="https://cdn.quasar.dev/img/avatar.png">
            </q-avatar>
            <q-menu dark class="bg-surface-pine border-light">
              <q-list style="min-width: 200px">
                <q-item clickable v-close-popup class="q-py-md">
                  <q-item-section avatar>
                    <q-icon name="person" color="primary" />
                  </q-item-section>
                  <q-item-section>Perfil</q-item-section>
                </q-item>
                <q-separator dark />
                <q-item clickable v-close-popup class="text-negative q-py-md" @click="handleLogout">
                  <q-item-section avatar>
                    <q-icon name="logout" color="negative" />
                  </q-item-section>
                  <q-item-section>Cerrar Sesión</q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn>
        </div>
      </q-toolbar>
    </q-header>

    <!-- Sidebar (Surface Pine) -->
    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      :width="280"
      class="bg-surface-pine text-secondary border-right"
    >
      <div class="column full-height">
        <div class="q-pa-lg">
          <div class="text-overline text-accent q-mb-md">Menú Principal</div>
          <q-list class="q-gutter-y-xs">
            <q-item
              v-for="link in linksList"
              :key="link.title"
              clickable
              v-ripple
              class="radius-sm interactive"
              :active="activeRoute === link.path"
              @click="navigateTo(link.path)"
            >
              <q-item-section avatar>
                <q-icon :name="link.icon" :color="activeRoute === link.path ? 'primary' : 'accent'" />
              </q-item-section>
              <q-item-section>
                <q-item-label :class="{ 'text-primary text-weight-bold': activeRoute === link.path }">
                  {{ link.title }}
                </q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </div>

        <q-space />

        <div class="q-pa-lg">
          <BaseCard variant="ghost" class="q-pa-md">
            <div class="row items-center gap-sm">
              <q-icon name="info" color="primary" />
              <div class="text-caption text-accent">
                Tu plan actual: <strong>Premium Trial</strong>
              </div>
            </div>
          </BaseCard>
        </div>
      </div>
    </q-drawer>

    <!-- Main Workspace -->
    <q-page-container class="bg-forest-deep">
      <div class="q-pa-lg q-pa-md-xl" style="max-width: 1400px; margin: 0 auto">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useQuasar } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useRouter, useRoute } from 'vue-router';
import BaseButton from 'src/components/base/BaseButton.vue';
import BaseCard from 'src/components/base/BaseCard.vue';

const $q = useQuasar();
const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

const leftDrawerOpen = ref(false);
const activeRoute = computed(() => route.path);

onMounted(() => {
  $q.dark.set(true);
});

const handleLogout = async () => {
  try {
    const response = await authStore.logout();
    const allSessions = response?.data?.allSessionsRevoked;

    $q.notify({
      type: 'info',
      message: allSessions ? 'Todas las sesiones cerradas' : 'Sesión finalizada',
      position: 'top-right'
    });
    
    void router.push('/login');
  } catch (error) {
    console.error('Logout error', error);
  }
};

const linksList = [
  { title: 'Dashboard', icon: 'dashboard', path: '/dashboard' },
  { title: 'Auditorías', icon: 'security', path: '/audits' },
  { title: 'Reportes', icon: 'analytics', path: '/reports' },
  { title: 'Configuración', icon: 'settings', path: '/settings' },
];

function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value;
}

function navigateTo(path: string) {
  void router.push(path);
}
</script>

<style lang="scss" scoped>
.border-right {
  border-right: 1px solid rgba(113, 131, 127, 0.1);
}

.border-light {
  border: 1px solid rgba(113, 131, 127, 0.1);
}

.hide-mobile {
  @media (max-width: 599px) {
    display: none;
  }
}

.gap-sm {
  gap: 8px;
}

:deep(.q-drawer--bordered) {
  border-right-color: rgba(113, 131, 127, 0.1);
}
</style>