<template>
  <q-layout view="lHh Lpr lFf">
    <a href="#main-content" class="skip-link">Saltar al contenido principal</a>

    <!-- Offline banner -->
    <q-banner v-if="!online" class="offline-banner text-center q-py-xs" role="alert">
      <template v-slot:avatar>
        <q-icon name="wifi_off" />
      </template>
      Sin conexión — los datos mostrados pueden no estar actualizados
    </q-banner>

    <q-banner
      v-if="showInstallBanner && (deferredPrompt || isIOS)"
      class="install-banner text-center q-py-xs"
      role="alert"
    >
      <template v-slot:avatar>
        <q-icon name="download" />
      </template>
      <template v-if="isIOS && !deferredPrompt">
        Instala PYMEQ: tocá Compartir <q-icon name="ios_share" /> y luego "Agregar a pantalla de inicio"
      </template>
      <template v-else>
        Instala PYMEQ para una experiencia más rápida
      </template>
      <template v-slot:action>
        <q-btn
          v-if="!isIOS || deferredPrompt"
          flat dense no-caps label="Instalar"
          class="install-banner__btn"
          @click="installApp"
        />
        <q-btn flat dense no-caps :label="isIOS && !deferredPrompt ? 'Listo' : 'Ahora no'" class="install-banner__dismiss" @click="dismissInstall" />
      </template>
    </q-banner>

    <!-- Header — minimal, institutional -->
    <q-header class="main-header">
      <q-toolbar class="q-px-lg">
        <BaseButton
          variant="ghost"
          icon-left="menu"
          class="q-mr-sm"
          aria-label="Abrir menú"
          @click="toggleLeftDrawer"
        />

        <q-toolbar-title>
          <span class="logo-text">PYMEQ</span>
        </q-toolbar-title>

        <q-btn round flat aria-label="Menú de usuario" aria-haspopup="menu">
          <q-avatar size="32px" style="background: var(--pq-accent); color: var(--pq-background); font-family: 'Geist', sans-serif; font-weight: 700; font-size: 14px;">
            {{ userInitials }}
          </q-avatar>
          <q-menu dark class="user-menu">
            <q-list style="min-width: 220px">
              <q-item class="q-py-sm" v-if="userName || userEmail">
                <q-item-section>
                  <q-item-label class="user-menu__name">{{ userName }}</q-item-label>
                  <q-item-label caption class="user-menu__email">{{ userEmail }}</q-item-label>
                </q-item-section>
              </q-item>
              <q-separator dark style="border-color: var(--pq-border)" />
              <q-item clickable v-close-popup class="q-py-md">
                <q-item-section avatar>
                  <q-icon name="person" style="color: var(--pq-accent)" />
                </q-item-section>
                <q-item-section>Perfil</q-item-section>
              </q-item>
              <q-separator dark style="border-color: var(--pq-border)" />
              <q-item clickable v-close-popup class="q-py-md" @click="handleLogout">
                <q-item-section avatar>
                  <q-icon name="logout" style="color: var(--pq-danger)" />
                </q-item-section>
                <q-item-section>Cerrar Sesión</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <!-- Sidebar — Swiss grouped navigation -->
    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      :width="260"
      class="sidebar-drawer"
    >
      <div class="column full-height">
        <div class="q-pa-md">
          <nav role="navigation" aria-label="Menú principal">
            <template v-for="(group, gIdx) in navGroups" :key="group.label">
              <div v-if="gIdx > 0" class="nav-separator" />
              <div class="nav-section-label">{{ group.label }}</div>
              <q-list class="q-gutter-y-xs">
                <q-item
                  v-for="link in group.items"
                  :key="link.path"
                  clickable v-ripple
                  class="nav-item"
                  :class="{ 'nav-item--active': activeRoute === link.path }"
                  @click="navigateTo(link.path)"
                >
                  <q-item-section avatar>
                    <q-icon
                      :name="link.icon"
                      :style="{ color: activeRoute === link.path ? 'var(--pq-accent)' : 'var(--pq-text-subtle)' }"
                    />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>{{ link.title }}</q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
            </template>
          </nav>
        </div>

        <q-space />

        <!-- Version — subtle, bottom -->
        <div class="q-pa-md text-center">
          <span class="version-label">v0.1.0</span>
        </div>
      </div>
    </q-drawer>

    <!-- Main Workspace -->
    <q-page-container class="page-container">
      <main id="main-content" class="page-workspace q-pa-lg q-pa-md-xl" tabindex="-1">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </q-page-container>

    <!-- Mobile Bottom Nav -->
    <q-footer class="mobile-bottom-nav" reveal>
      <q-tabs
        :model-value="mobileTab"
        dense
        class="mobile-tabs"
        align="justify"
        narrow-indicator
      >
        <q-route-tab to="/dashboard" icon="dashboard" aria-label="Dashboard" />
        <q-route-tab to="/dashboard/productos" icon="inventory_2" aria-label="Productos" />
        <q-route-tab to="/dashboard/facturas" icon="receipt_long" aria-label="Facturas" />
        <q-route-tab to="/dashboard/prestamos" icon="account_balance" aria-label="Préstamos" />
        <q-route-tab icon="more_horiz" aria-label="Más" @click="toggleLeftDrawer" />
      </q-tabs>
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted, onUnmounted, computed } from 'vue';
import { useQuasar } from 'quasar';
import { useRoute, useRouter } from 'vue-router';
import { useLogout } from 'src/composables/useLogout';
import { useAuthStore } from 'src/modules/auth/store';
import BaseButton from 'src/components/base/BaseButton.vue';

const $q = useQuasar();
const route = useRoute();
const router = useRouter();
const { logout: handleLogout } = useLogout();
const authStore = useAuthStore();

const leftDrawerOpen = ref(false);
const activeRoute = computed(() => route.path);
const userName = computed(() => authStore.user?.name ?? '');
const userEmail = computed(() => authStore.user?.email ?? '');
const userInitials = computed(() => {
  const name = authStore.user?.name;
  if (!name) return '?';
  return name.split(' ').map(n => n[0]).filter(Boolean).slice(0, 2).join('').toUpperCase();
});
const mobileTab = computed(() => {
  const p = route.path;
  if (p === '/dashboard') return '/dashboard';
  if (p.startsWith('/dashboard/productos')) return '/dashboard/productos';
  if (p.startsWith('/dashboard/facturas')) return '/dashboard/facturas';
  return '';
});
const online = ref(navigator.onLine);
const deferredPrompt = shallowRef<Event | null>(null);
const isIOS = typeof window !== 'undefined' && /iphone|ipad|ipod/i.test(navigator.userAgent);
const showInstallBanner = ref(
  typeof window !== 'undefined' && !window.matchMedia('(display-mode: standalone)').matches
    && !localStorage.getItem('pwa_install_dismissed'),
);

function onOnline() { online.value = true; }
function onOffline() { online.value = false; }

function onBeforeInstall(e: Event) {
  e.preventDefault();
  deferredPrompt.value = e;
  showInstallBanner.value = true;
}

async function installApp() {
  const prompt = deferredPrompt.value as Event & { prompt: () => void; userChoice: Promise<{ outcome: string }> } | null;
  if (!prompt) {
    $q.notify({
      type: 'info',
      message: 'La opción de instalar aparecerá tras usar la app por un momento. Volvé a tocar Instalar en un rato.',
      position: 'top-right'
    });
    return;
  }
  prompt.prompt();
  const result = await prompt.userChoice;
  if (result.outcome === 'accepted') showInstallBanner.value = false;
  deferredPrompt.value = null;
}

function dismissInstall() {
  showInstallBanner.value = false;
  localStorage.setItem('pwa_install_dismissed', 'true');
}

function onSwUpdate() {
  $q.dialog({
    title: 'Actualización disponible',
    message: 'Hay una nueva versión. ¿Actualizar ahora?',
    ok: 'Actualizar',
    cancel: 'Después',
    persistent: true,
  }).onOk(() => {
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
  window.addEventListener('beforeinstallprompt', onBeforeInstall);
  navigator.serviceWorker?.addEventListener('controllerchange', onSwControllerChange);
});

onUnmounted(() => {
  window.removeEventListener('online', onOnline);
  window.removeEventListener('offline', onOffline);
  window.removeEventListener('sw-update-ready', onSwUpdate);
  window.removeEventListener('beforeinstallprompt', onBeforeInstall);
  navigator.serviceWorker?.removeEventListener('controllerchange', onSwControllerChange);
});

interface NavItem {
  title: string
  icon: string
  path: string
}

interface NavGroup {
  label: string
  items: NavItem[]
}

const navGroups = computed<NavGroup[]>(() => {
  const userRole = authStore.user?.role || '';
  const canSeeTeams = userRole === 'OWNER' || userRole === 'ADMIN';

  return [
    {
      label: 'Operaciones',
      items: [
        { title: 'Dashboard', icon: 'dashboard', path: '/dashboard' },
        { title: 'Productos', icon: 'inventory_2', path: '/dashboard/productos' },
        { title: 'Proveedores', icon: 'people', path: '/dashboard/proveedores' },
        { title: 'Facturas', icon: 'receipt_long', path: '/dashboard/facturas' },
        { title: 'Costos', icon: 'money_off', path: '/dashboard/costos' },
      ],
    },
    {
      label: 'Análisis',
      items: [
        { title: 'Análisis', icon: 'analytics', path: '/dashboard/analisis-gastos' },
        { title: 'Ventas', icon: 'point_of_sale', path: '/dashboard/ventas' },
        { title: 'Patrimonio', icon: 'savings', path: '/dashboard/patrimonio' },
        { title: 'Préstamos', icon: 'account_balance', path: '/dashboard/prestamos' },
      ],
    },
    {
      label: 'Sistema',
      items: [
        { title: 'Contabilidad', icon: 'balance', path: '/dashboard/accounting' },
        ...(canSeeTeams ? [{ title: 'Equipo', icon: 'groups', path: '/dashboard/teams' }] : []),
        { title: 'Configuración', icon: 'settings', path: '/dashboard/configuracion' },
      ],
    },
  ];
});

function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value;
}

function navigateTo(path: string) {
  void router.push(path);
}
</script>

<style lang="scss" scoped>
/* --------------------------------------------------
   Header
-------------------------------------------------- */
.main-header {
  background: var(--pq-surface) !important;
  border-bottom: 1px solid var(--pq-border);
}

.logo-text {
  font-family: 'Geist', sans-serif;
  font-weight: 800;
  font-size: 16px;
  color: var(--pq-accent);
}

.user-menu {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);

  &__name {
    font-family: 'Geist', sans-serif;
    font-size: 14px;
    font-weight: 600;
    color: var(--pq-text);
  }

  &__email {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-muted);
  }
}

/* --------------------------------------------------
   Sidebar
-------------------------------------------------- */
.sidebar-drawer {
  background: var(--pq-surface);
  border-right: 1px solid var(--pq-border);
}

.nav-section-label {
  font-family: 'Satoshi', sans-serif;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
  padding: 16px 16px 6px;
}

.nav-separator {
  height: 1px;
  background: var(--pq-border);
  margin: 8px 16px;
  opacity: 0.4;
}

.nav-item {
  border-radius: var(--pq-radius-sm);
  min-height: 40px;
  transition: background var(--pq-motion-fast), color var(--pq-motion-fast);
  color: var(--pq-text-muted);
  border-left: 3px solid transparent;

  &:hover {
    background: rgba(200, 150, 62, 0.04);
  }

  &--active {
    background: rgba(200, 150, 62, 0.06);
    color: var(--pq-text);
    border-left-color: var(--pq-accent);
    font-weight: 600;

    :deep(.q-item__label) {
      color: var(--pq-text);
    }
  }

  :deep(.q-item__label) {
    color: inherit;
    font-size: 14px;
  }
}

.version-label {
  font-family: 'Geist Mono', monospace;
  font-size: 11px;
  color: var(--pq-text-subtle);
  opacity: 0.5;
}

/* --------------------------------------------------
   Page Workspace
-------------------------------------------------- */
.page-container {
  background: var(--pq-background);
}

.page-workspace {
  max-width: 1280px;
  margin: 0 auto;
}

/* --------------------------------------------------
   Offline Banner
-------------------------------------------------- */
.offline-banner {
  background: var(--pq-warning);
  color: var(--pq-background);
}

/* --------------------------------------------------
   Mobile Bottom Nav
-------------------------------------------------- */
.mobile-bottom-nav {
  display: none;
  border-top: 1px solid var(--pq-border);
  background: var(--pq-surface);
}

.mobile-tabs {
  color: var(--pq-text-subtle);

  :deep(.q-tab--active) {
    color: var(--pq-accent);
  }

  :deep(.q-tabs__content .q-tab__indicator) {
    background: var(--pq-accent);
  }
}

@media (max-width: 767px) {
  .mobile-bottom-nav {
    display: block;
  }
}

/* --------------------------------------------------
   Install Banner
-------------------------------------------------- */
.install-banner {
  background: var(--pq-accent);
  color: var(--pq-background);
  font-family: 'Satoshi', sans-serif;
  font-size: 13px;

  &__btn {
    color: var(--pq-background) !important;
    font-weight: 700;
  }

  &__dismiss {
    color: var(--pq-background) !important;
    opacity: 0.7;
  }
}

/* --------------------------------------------------
   Skip Link (a11y)
-------------------------------------------------- */
.skip-link {
  position: absolute;
  top: -100%;
  left: 0;
  z-index: 800;
  padding: 0.75rem 1.5rem;
  background: var(--pq-accent);
  color: var(--pq-background);
  font-weight: 700;
  text-decoration: none;
  border-radius: 0 0 var(--pq-radius-sm) 0;

  &:focus {
    top: 0;
  }
}
</style>
