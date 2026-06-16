<template>
  <q-page class="index-page bg-forest-deep text-secondary">
    <LandingHero @start="startOnboarding" />
    <FeatureGrid />
    <TrustSection />
  </q-page>
</template>

<script setup lang="ts">
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useRouter } from 'vue-router';
import LandingHero from 'src/components/landing/LandingHero.vue';
import FeatureGrid from 'src/components/landing/FeatureGrid.vue';
import TrustSection from 'src/components/landing/TrustSection.vue';

useMeta({ title: 'PYMEQ — Gestión Financiera Inteligente' });

const authStore = useAuthStore();
const router = useRouter();

const startOnboarding = (name: string) => {
  if (!name.trim()) return;

  const slug = name
    .toLowerCase()
    .trim()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_-]+/g, '-')
    .replace(/^-+|-+$/g, '');

  authStore.setPendingTenant(name, slug);
  void router.push('/register');
};
</script>
