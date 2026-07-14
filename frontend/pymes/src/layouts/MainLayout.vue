<template>
  <q-layout view="lHh Lpr lFf">
    <a href="#main-content" class="skip-link">Saltar al contenido principal</a>

    <!-- Offline banner -->
    <q-banner v-if="!online" class="bg-warning text-dark text-center q-py-xs" role="alert">
      <template v-slot:avatar>
        <q-icon name="wifi_off" />
      </template>
      Sin conexión — los datos mostrados pueden no estar actualizados
    </q-banner>

    <!-- Header with Brand Glow -->
    <q-header class="bg-dark text-secondary brand-glow">
      <q-toolbar class="q-px-lg">
        <BaseButton
          variant="ghost"
          icon-left="menu"
          class="q-mr-sm"
          aria-label="Abrir menú"
          @click="toggleLeftDrawer"
        />

        <q-toolbar-title>
          <span class="mesh-text-gradient text-h6 font-bold">PYMEQ</span>
          <span class="q-ml-xs text-weight-thin text-accent">Audit Toolkit</span>
        </q-toolbar-title>

        <div class="row items-center gap-sm">
          <div class="text-caption text-accent hide-mobile">v0.1.0</div>
          <q-btn round flat aria-label="Menú de usuario" aria-haspopup="menu">
            <q-avatar size="32px">
              <img src="https://cdn.quasar.dev/img/avatar.png" alt="" aria-hidden="true">
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
          <q-list class="q-gutter-y-xs" role="navigation" aria-label="Menú principal">
            <template v-for="(link, idx) in linksList" :key="link.title || 'sep-' + idx">
              <q-separator v-if="link.separator" dark class="q-mx-md q-my-sm" style="opacity: 0.3" />
              <q-item
                v-else-if="link.path"
                clickable v-ripple class="radius-sm interactive"
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
              <q-item
                v-else-if="link.disabled"
                disable class="radius-sm q-mb-xs"
                style="opacity: 0.4"
              >
                <q-item-section avatar>
                  <q-icon :name="link.icon" color="accent" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-accent">
                    {{ link.title }}
                    <q-badge flat color="transparent" text-color="sage-muted" size="xs" class="q-ml-xs">
                      próximamente
                    </q-badge>
                  </q-item-label>
                </q-item-section>
              </q-item>
            </template>
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
      <main id="main-content" class="q-pa-lg q-pa-md-xl" style="max-width: 1400px; margin: 0 auto" tabindex="-1">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </q-page-container>

    <!-- Mobile Bottom Nav -->
    <q-footer class="bg-surface-pine mobile-bottom-nav" reveal>
      <q-tabs
        :model-value="mobileTab"
        dense
        active-color="primary"
        indicator-color="primary"
        class="text-accent"
        align="justify"
        narrow-indicator
      >
        <q-route-tab to="/dashboard" icon="dashboard" aria-label="Dashboard" />
        <q-route-tab to="/dashboard/productos" icon="inventory_2" aria-label="Productos" />
        <q-route-tab to="/dashboard/facturas" icon="receipt_long" aria-label="Facturas" />
        <q-route-tab to="/dashboard/gastos" icon="money_off" aria-label="Gastos" />
        <q-route-tab icon="more_horiz" aria-label="Más" @click="toggleLeftDrawer" />
      </q-tabs>
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useQuasar } from 'quasar';
import { useRoute, useRouter } from 'vue-router';
import { useLogout } from 'src/composables/useLogout';
import BaseButton from 'src/components/base/BaseButton.vue';
import BaseCard from 'src/components/base/BaseCard.vue';

const $q = useQuasar();
const route = useRoute();
const router = useRouter();
const { logout: handleLogout } = useLogout();

const leftDrawerOpen = ref(false);
const activeRoute = computed(() => route.path);
const mobileTab = computed(() => {
  const p = route.path
  if (p === '/dashboard') return '/dashboard'
  if (p.startsWith('/dashboard/productos')) return '/dashboard/productos'
  if (p.startsWith('/dashboard/facturas')) return '/dashboard/facturas'
  if (p.startsWith('/dashboard/gastos')) return '/dashboard/gastos'
  return ''
})
const online = ref(navigator.onLine);

function onOnline() { online.value = true; }
function onOffline() { online.value = false; }

function onSwUpdate() {
  $q.dialog({
    title: 'Actualización disponible',
    message: 'Hay una nueva versión. ¿Actualizar ahora?',
    ok: 'Actualizar',
    cancel: 'Después',
    persistent: true,
  }).onOk(() => {
    // ponytail: SKIP_WAITING triggers controllerchange → reload
    void navigator.serviceWorker?.getRegistration().then(r => {
      r?.waiting?.postMessage({ type: 'SKIP_WAITING' });
    });
  });
}

function onSwControllerChange() {
  window.location.reload();
}

onMounted(() => {
  $q.dark.set(true);
  window.addEventListener('online', onOnline);
  window.addEventListener('offline', onOffline);
  window.addEventListener('sw-update-ready', onSwUpdate);
  navigator.serviceWorker?.addEventListener('controllerchange', onSwControllerChange);
});

onUnmounted(() => {
  window.removeEventListener('online', onOnline);
  window.removeEventListener('offline', onOffline);
  window.removeEventListener('sw-update-ready', onSwUpdate);
  navigator.serviceWorker?.removeEventListener('controllerchange', onSwControllerChange);
});

interface SidebarLink {
  title?: string
  icon?: string
  path?: string
  separator?: boolean
  disabled?: boolean
}

const linksList: SidebarLink[] = [
  { title: 'Dashboard', icon: 'dashboard', path: '/dashboard' },
  { separator: true },
  { title: 'Productos', icon: 'inventory_2', path: '/dashboard/productos' },
  { title: 'Proveedores', icon: 'people', path: '/dashboard/proveedores' },
  { title: 'Facturas', icon: 'receipt_long', path: '/dashboard/facturas' },
  { title: 'Análisis de Gastos', icon: 'analytics', path: '/dashboard/analisis-gastos' },
  { separator: true },
  { title: 'Gastos', icon: 'money_off', path: '/dashboard/gastos' },
  { title: 'Ventas', icon: 'point_of_sale', path: '/dashboard/ventas' },
  { title: 'Préstamos', icon: 'account_balance', path: '/dashboard/prestamos' },
  { title: 'Patrimonio', icon: 'savings', path: '/dashboard/patrimonio' },
  { title: 'Contabilidad', icon: 'balance', path: '/dashboard/accounting' },
  { separator: true },
  { title: 'Configuración', icon: 'settings', path: '/dashboard/configuracion' },
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

.mobile-bottom-nav {
  display: none;
  border-top: 1px solid rgba(113, 131, 127, 0.1);
}

@media (max-width: 767px) {
  .mobile-bottom-nav {
    display: block;
  }
}
</style>