<template>
  <q-layout view="lHh Lpr lFf" class="bg-forest-deep">
    <!-- Header: Fixed and Integrated -->
    <q-header elevated class="bg-dark glass-light border-bottom q-py-xs">
      <q-toolbar class="container-narrow mx-auto q-px-md">
        <q-toolbar-title class="text-h5 font-bold mesh-text-gradient cursor-pointer" @click="router.push('/')">
          PYMEQ
        </q-toolbar-title>
        
        <q-space />

        <!-- Navigation Links: Hidden on extra small screens only -->
        <nav class="gt-xs row items-center gap-md q-mr-md" aria-label="Navegación principal">
          <BaseButton variant="ghost" size="sm" @click="scrollTo('features')">Funciones</BaseButton>
          <BaseButton variant="ghost" size="sm" @click="scrollTo('trust')">Sectores</BaseButton>
        </nav>

        <BaseButton 
          variant="primary" 
          size="md" 
          class="brand-glow" 
          @click="router.push('/login')"
        >
          LOGIN
        </BaseButton>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <main>
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
      </main>
    </q-page-container>

    <!-- Footer: More compact and focused -->
    <q-footer class="bg-dark text-accent q-pa-lg border-top">
      <div class="container-narrow mx-auto">
        <div class="row q-col-gutter-md justify-between items-center">
          <div class="col-12 col-md-4 text-center text-md-left">
            <div class="text-h6 font-bold mesh-text-gradient q-mb-xs">PYMEQ</div>
            <div class="text-caption">
              Tu Capital, Bajo Control.
            </div>
          </div>
          <div class="col-12 col-md-4 text-center">
            <div class="text-caption opacity-50">
              &copy; 2026 Pymeq. Hecho para crecer.
            </div>
          </div>
          <div class="col-12 col-md-4 text-center text-md-right">
             <div class="row justify-center justify-md-end gap-sm">
                <BaseButton variant="ghost" size="xs" aria-disabled="true">Legal</BaseButton>
                <BaseButton variant="ghost" size="xs" aria-disabled="true">Contacto</BaseButton>
             </div>
          </div>
        </div>
      </div>
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import BaseButton from 'src/components/base/BaseButton.vue';

const router = useRouter();

const scrollTo = (id: string) => {
  const el = document.getElementById(id);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' });
  }
};
</script>

<style lang="scss" scoped>
.container-narrow {
  max-width: 1000px;
}

.border-bottom {
  border-bottom: 1px solid rgba(113, 131, 127, 0.1);
}

.border-top {
  border-top: 1px solid rgba(113, 131, 127, 0.1);
}

.gap-md {
  gap: 12px;
}

.mx-auto {
  margin-left: auto;
  margin-right: auto;
}
</style>
