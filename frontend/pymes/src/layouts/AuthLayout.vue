<template>
  <q-layout view="lHh Lpr lFf" class="bg-forest-deep">
    <q-page-container>
      <main class="auth-layout-wrapper flex flex-center q-pa-md">
        <div class="auth-container">
          <!-- Logo & Branding Persistente -->
          <div class="text-center q-mb-xl cursor-pointer" @click="$router.push('/')">
            <div class="text-h3 font-bold mesh-text-gradient q-mb-xs">PYMEQ</div>
            <div class="text-subtitle1 text-accent text-weight-light letter-spacing-2">
              Sistema de Auditoría Inteligente para Pymes
            </div>
          </div>

          <!-- Aquí se renderizan las páginas (Login, Register, ForgotPassword, etc.) -->
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>

          <!-- Footer de Seguridad -->
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
.auth-layout-wrapper {
  min-height: 100vh;
}
.auth-container {
  width: 100%;
  max-width: 450px;
}
.letter-spacing-2 {
  letter-spacing: 2px;
}
</style>
