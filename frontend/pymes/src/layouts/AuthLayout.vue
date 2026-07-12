<template>
  <q-layout view="lHh Lpr lFf" class="auth-layout">
    <q-page-container>
      <main class="auth-layout-wrapper flex flex-center q-pa-md">
        <div class="auth-container fade-in-up">
          <!-- Logo & Branding -->
          <div class="text-center q-mb-xl cursor-pointer" @click="$router.push('/')">
            <div class="text-h3 font-bold mesh-text-gradient q-mb-xs">PYMEQ</div>
            <div class="text-subtitle1 text-accent text-weight-light letter-spacing-2">
              Sistema de Auditoría Inteligente para Pymes
            </div>
          </div>

          <!-- Router pages (Login, Register, etc.) -->
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>

          <!-- Footer -->
          <footer class="text-center q-mt-xl text-accent text-caption opacity-70">
            <q-icon name="security" size="xs" class="q-mr-xs" aria-hidden="true" />
            Plataforma Segura y Confiable para la Gestión de Auditorías
          </footer>
        </div>
      </main>
    </q-page-container>
  </q-layout>
</template>

<style lang="scss" scoped>
.auth-layout {
  background: $dark-page;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(ellipse 80% 60% at 50% -20%, rgba(163, 120, 94, 0.10) 0%, transparent 70%),
      radial-gradient(ellipse 40% 30% at 80% 80%, rgba(197, 160, 89, 0.06) 0%, transparent 60%);
    pointer-events: none;
  }
}

.auth-layout-wrapper {
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

.auth-container {
  width: 100%;
  max-width: 450px;
}

.letter-spacing-2 {
  letter-spacing: 2px;
}
</style>
