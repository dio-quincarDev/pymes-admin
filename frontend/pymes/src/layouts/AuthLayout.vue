<template>
  <q-layout view="lHh Lpr lFf" class="auth-layout">
    <q-page-container>
      <main class="auth-layout-wrapper flex flex-center q-pa-md">
        <div class="auth-container fade-in-up">
          <div
            class="text-center q-mb-xl auth-brand"
            role="button" tabindex="0" aria-label="Ir al inicio"
            @click="$router.push('/')"
            @keydown.enter="$router.push('/')"
            @keydown.space.prevent="$router.push('/')"
          >
            <img src="/icons/logo.svg" alt="" width="48" height="48" class="q-mb-sm" style="display: block; margin: 0 auto" />
            <div class="auth-logo">PYMEQ</div>
            <div class="auth-tagline">Finanzas inteligentes para tu PyME — by QCore System</div>
          </div>

          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>

          <footer class="text-center q-mt-xl text-caption" style="color: var(--pq-text-subtle); opacity: 0.7;">
            Tus números claros, tu negocio bajo control.
          </footer>
        </div>
      </main>
    </q-page-container>
  </q-layout>
</template>

<style lang="scss" scoped>
.auth-layout {
  background: var(--pq-background);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(ellipse 80% 60% at 50% -20%, rgba(#C8963E, 0.10) 0%, transparent 70%),
      radial-gradient(ellipse 40% 30% at 80% 80%, rgba(#C8A042, 0.06) 0%, transparent 60%);
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

.auth-logo {
  font-family: 'Geist', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-weight: 800;
  font-size: 28px;
  color: var(--pq-accent);
  letter-spacing: -0.01em;
  line-height: 1.2;
}

.auth-tagline {
  font-family: 'Satoshi', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-size: 14px;
  color: var(--pq-text-muted);
  font-weight: 300;
  letter-spacing: 2px;
  margin-top: 4px;
}
</style>
