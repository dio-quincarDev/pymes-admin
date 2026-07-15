<template>
  <Transition name="fade" mode="out-in">
    <router-view />
  </Transition>
</template>

<script setup lang="ts">
import { watch, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from 'src/modules/auth/store';

const router = useRouter();

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

onMounted(() => window.addEventListener('storage', onStorage));
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
</style>
