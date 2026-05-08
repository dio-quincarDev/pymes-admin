<template>
  <q-page class="dashboard-page">
    <div class="row q-col-gutter-lg">
      <!-- Welcome Header -->
      <div class="col-12 q-mb-md fade-in-up">
        <h1 class="text-h4 text-primary font-bold q-ma-none brand-glow-text">
          Bienvenido, {{ authStore.user?.nombre || 'Auditor' }}
        </h1>
        <p class="text-subtitle1 text-accent q-mt-xs">Panel de control de auditoría inteligente</p>
      </div>

      <!-- Stats Cards Row -->
      <div class="col-12 col-sm-6 col-md-4 fade-in-up" style="animation-delay: 0.1s">
        <BaseCard variant="elevated" class="q-pa-lg full-height">
          <div class="row items-center justify-between q-mb-md">
            <div class="text-overline text-accent">Auditorías Activas</div>
            <q-icon name="security" color="primary" size="sm" />
          </div>
          <div class="text-h3 text-primary font-bold">12</div>
          <div class="text-caption text-positive q-mt-sm">
            <q-icon name="trending_up" /> +2 este mes
          </div>
        </BaseCard>
      </div>

      <div class="col-12 col-sm-6 col-md-4 fade-in-up" style="animation-delay: 0.2s">
        <BaseCard variant="elevated" class="q-pa-lg full-height">
          <div class="row items-center justify-between q-mb-md">
            <div class="text-overline text-accent">Riesgo Promedio</div>
            <q-icon name="gpp_maybe" color="negative" size="sm" />
          </div>
          <div class="text-h3 text-negative font-bold">Bajo</div>
          <div class="text-caption text-accent q-mt-sm">Basado en 45 controles</div>
        </BaseCard>
      </div>

      <div class="col-12 col-md-4 fade-in-up" style="animation-delay: 0.3s">
        <BaseCard variant="elevated" class="q-pa-lg full-height">
          <div class="row items-center justify-between q-mb-md">
            <div class="text-overline text-accent">Usuarios Activos</div>
            <q-icon name="people" color="primary" size="sm" />
          </div>
          <div class="text-h3 text-primary font-bold">1</div>
          <div class="text-caption text-accent q-mt-sm">Licencia: Business Core</div>
        </BaseCard>
      </div>

      <!-- Secondary Row: Main Actions -->
      <div class="col-12 col-md-8 fade-in-up" style="animation-delay: 0.4s">
        <BaseCard class="q-pa-xl flex flex-center text-center">
          <div style="max-width: 400px">
            <q-icon name="analytics" size="6rem" color="primary" class="opacity-20 q-mb-lg" />
            <div class="text-h5 font-bold q-mb-md">Inicia un nuevo análisis</div>
            <p class="text-body2 text-accent q-mb-xl">
              Selecciona una categoría para comenzar a auditar los movimientos de tu empresa.
            </p>
            <BaseButton size="lg">COMENZAR AUDITORÍA</BaseButton>
          </div>
        </BaseCard>
      </div>

      <div class="col-12 col-md-4 fade-in-up" style="animation-delay: 0.5s">
        <BaseCard variant="ghost" class="q-pa-lg full-height">
          <div class="text-subtitle2 font-bold q-mb-md">Actividad Reciente</div>
          <q-list dark separator class="border-light radius-sm">
            <q-item v-for="i in 3" :key="i" class="q-py-md">
              <q-item-section avatar>
                <q-icon name="history" color="accent" size="xs" />
              </q-item-section>
              <q-item-section>
                <q-item-label class="text-body2">Inicio de sesión detectado</q-item-label>
                <q-item-label caption>Hace {{ i * 2 }} horas</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
          <BaseButton variant="ghost" size="sm" class="full-width q-mt-md">Ver todo</BaseButton>
        </BaseCard>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { useAuthStore } from 'src/modules/auth/store';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';

const authStore = useAuthStore();
</script>

<style lang="scss" scoped>
.dashboard-page {
  width: 100%;
}

.brand-glow-text {
  text-shadow: 0 0 20px rgba(163, 120, 94, 0.3);
}

.opacity-20 {
  opacity: 0.2;
}

.border-light {
  border: 1px solid rgba(113, 131, 127, 0.1);
}
</style>