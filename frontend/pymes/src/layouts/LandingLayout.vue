<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const scrollTo = (id: string) => {
  const el = document.getElementById(id);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' });
  }
};
</script>

<template>
  <q-layout view="lHh Lpr lFf" class="landing-layout">
    <a href="#main-content" class="skip-link">Saltar al contenido principal</a>

    <!-- Header -->
    <q-header class="landing-header">
      <q-toolbar class="container-narrow mx-auto q-px-md">
        <q-toolbar-title class="logo-text cursor-pointer row items-center no-wrap" @click="router.push('/')">
          <img src="/icons/logo.svg" alt="" width="28" height="28" class="q-mr-sm" />
          PYMEQ
        </q-toolbar-title>

        <q-space />

        <nav class="gt-xs row items-center q-mr-md" aria-label="Navegación principal">
          <q-btn flat color="accent" size="sm" @click="scrollTo('features')">Funciones</q-btn>
          <q-btn flat color="accent" size="sm" @click="scrollTo('trust')">Sectores</q-btn>
        </nav>

        <q-btn
          color="primary"
          size="md"
          @click="router.push('/login')"
        >
          LOGIN
        </q-btn>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <main id="main-content" tabindex="-1">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
      </main>
    </q-page-container>

    <!-- Footer -->
    <q-footer class="landing-footer">
      <div class="container-narrow mx-auto q-pa-lg">
        <div class="row q-col-gutter-md justify-between items-center">
          <div class="col-12 col-md-4 text-center text-md-left">
            <div class="logo-text q-mb-xs">PymeQ</div>
            <div class="text-caption" style="color: var(--pq-text-muted)">
              by QCore System
            </div>
          </div>
          <div class="col-12 col-md-4 text-center">
            <div class="text-caption" style="color: var(--pq-text-subtle)">
              &copy; 2026 PymeQ by QCore System. Hecho para crecer.
            </div>
          </div>
          <div class="col-12 col-md-4 text-center text-md-right">
             <div class="row justify-center justify-md-end gap-sm">
                <q-btn flat color="accent" size="xs" aria-disabled="true">Legal</q-btn>
                <q-btn flat color="accent" size="xs" aria-disabled="true">Contacto</q-btn>
             </div>
          </div>
        </div>
      </div>
    </q-footer>
  </q-layout>
</template>

<style lang="scss" scoped>
.landing-layout {
  background: var(--pq-background);
}

.landing-header {
  background: var(--pq-surface);
  border-bottom: 1px solid var(--pq-border);
}

.landing-footer {
  background: var(--pq-background);
  border-top: 1px solid var(--pq-border);
}

.logo-text {
  font-family: 'Geist', sans-serif;
  font-weight: 800;
  font-size: 16px;
  color: var(--pq-accent);
}

.container-narrow {
  max-width: 1000px;
}

.mx-auto {
  margin-left: auto;
  margin-right: auto;
}

.gap-sm {
  gap: 8px;
}

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
  &:focus { top: 0; }
}
</style>
