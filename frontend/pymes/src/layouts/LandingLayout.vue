<template>
  <q-layout view="lHh Lpr lFf" class="bg-forest-deep">
    <q-header flat class="bg-transparent q-py-md">
      <q-toolbar class="container max-width-xl mx-auto q-px-xl">
        <q-toolbar-title class="text-h4 font-bold mesh-text-gradient">
          PYMEQ
        </q-toolbar-title>
        
        <q-space />

        <div class="q-gutter-sm">
          <q-btn flat label="Planes" color="accent" no-caps />
          <q-btn flat label="Contacto" color="accent" no-caps />
          <q-btn unelevated label="Iniciar Sesión" color="primary" class="brand-glow q-px-lg" to="/login" no-caps />
        </div>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>

    <q-footer class="bg-transparent text-accent q-pa-xl text-center">
      <div class="text-caption">
        &copy; 2026 Pymeq Admin. Sistema de Auditoría Inteligente para Pymes.
      </div>
    </q-footer>
  </q-layout>
</template>

<style lang="scss" scoped>
.container {
  max-width: 1200px;
}
</style>
