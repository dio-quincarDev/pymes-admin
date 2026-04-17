<template>
  <q-page class="q-pa-lg">
    <div class="row q-col-gutter-lg">
      <!-- Welcome Header -->
      <div class="col-12">
        <div class="text-h4 text-primary font-bold">Bienvenido, {{ authStore.user?.nombre }}</div>
        <div class="text-subtitle1 text-accent">Panel de control de auditoría</div>
      </div>

      <!-- Stats Cards -->
      <div class="col-12 col-md-4">
        <q-card class="bg-surface-pine tight-shadow q-pa-md">
          <q-card-section>
            <div class="text-overline text-accent">Auditorías Activas</div>
            <div class="text-h3 text-primary font-bold">12</div>
          </q-card-section>
        </q-card>
      </div>

      <div class="col-12 col-md-4">
        <q-card class="bg-surface-pine tight-shadow q-pa-md">
          <q-card-section>
            <div class="text-overline text-accent">Riesgo Promedio</div>
            <div class="text-h3 text-negative font-bold">Bajo</div>
          </q-card-section>
        </q-card>
      </div>

      <div class="col-12 col-md-4">
        <q-card class="bg-surface-pine tight-shadow q-pa-md">
          <q-card-section>
            <div class="text-overline text-accent">Usuarios en el Tenant</div>
            <div class="text-h3 text-primary font-bold">1</div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { useAuthStore } from 'src/modules/auth/store';

const authStore = useAuthStore();
</script>
