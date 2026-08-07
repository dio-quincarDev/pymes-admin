<template>
  <Transition :name="transitionName" mode="out-in">
    <router-view />
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from 'src/modules/auth/store';

const router = useRouter();
const transitionName = ref('fade');

router.beforeEach((to, from) => {
  if (!from.path || from.path === to.path) { transitionName.value = 'fade'; return; }
  const delta = to.path.split('/').length - from.path.split('/').length;
  transitionName.value = delta >= 0 ? 'slide-right' : 'slide-left';
});

watch(
  () => router.currentRoute.value,
  () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    const main = document.querySelector('main');
    if (main) {
      main.setAttribute('tabindex', '-1');
      main.focus();
    }
  }
);

// Sync cross-tab: when email verification happens in another tab, navigate to dashboard
const onVerified = () => {
  const authStore = useAuthStore();
  const token = localStorage.getItem('pymeq_token');
  if (!token) return;
  authStore.accessToken = token;
  const user = localStorage.getItem('pymeq_user');
  if (user) authStore.user = JSON.parse(user);
  void router.push('/dashboard');
};

// Primary: BroadcastChannel (directo, funciona incluso entre PWA instalada y navegador)
const channel = new BroadcastChannel('pymeq-auth');
channel.onmessage = (e) => {
  if (e.data?.type === 'email-verified') onVerified();
};

// Fallback: StorageEvent para navegadores sin BroadcastChannel
const onStorage = (e: StorageEvent) => {
  if (e.key === 'pymeq_email_verified' && e.newValue === 'true') {
    localStorage.removeItem('pymeq_email_verified');
    onVerified();
  }
};

onMounted(() => {
  window.addEventListener('storage', onStorage);
  const auth = useAuthStore();
  if (auth.accessToken && auth.user?.tenantId && !auth.tenantName) {
    void auth.ensureTenantName();
  }
});
onUnmounted(() => {
  window.removeEventListener('storage', onStorage);
  channel.close();
});
</script>

<style lang="scss">
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.slide-right-enter-active,
.slide-right-leave-active,
.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 0.24s ease;
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
