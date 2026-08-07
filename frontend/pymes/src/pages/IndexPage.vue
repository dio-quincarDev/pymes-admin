<template>
  <q-page class="index-page">
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

useMeta({
  title: 'PYMEQ — Gestión Financiera Inteligente',
  meta: {
    description: {
      name: 'description',
      content: 'Organiza tu empresa y toma el control. Gestión de facturas, productos y gastos en un solo lugar para PYMES en LATAM.'
    },
    'og:url': { property: 'og:url', content: 'https://pymeq.com/' },
    'og:image': { property: 'og:image', content: 'https://pymeq.com/og-image.png' },
    'twitter:image': { name: 'twitter:image', content: 'https://pymeq.com/og-image.png' },
  },
  script: {
    ldJson: {
      type: 'application/ld+json',
      innerHTML: JSON.stringify({
        '@context': 'https://schema.org',
        '@type': 'SoftwareApplication',
        'name': 'PYMEQ',
        'applicationCategory': 'BusinessApplication',
        'operatingSystem': 'Web',
        'description': 'Gestión financiera inteligente para PYMES. Organiza facturas, productos y gastos.',
        'url': 'https://pymeq.com',
        'offers': {
          '@type': 'Offer',
          'price': '0',
          'priceCurrency': 'USD',
        },
        'author': {
          '@type': 'Organization',
          'name': 'PYMEQ',
          'url': 'https://pymeq.com',
        },
      }),
    },
  },
});

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

<style scoped>
.index-page {
  background: var(--pq-background);
  color: var(--pq-text);
}
</style>
