<template>
  <q-page class="dashboard-page">
    <div class="q-mb-md fade-in-up">
      <h1 class="text-h4 text-primary font-bold q-ma-none brand-glow-text">
        Bienvenido, {{ authStore.user?.nombre || 'Auditor' }}
      </h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Panel de control de auditoría inteligente</p>
    </div>

    <DashboardStats />

    <div class="row q-col-gutter-lg q-mt-lg">
      <div class="col-12 col-md-8 fade-in-up" style="animation-delay: 0.4s">
        <DashboardActionCard />
      </div>
      <div class="col-12 col-md-4 fade-in-up" style="animation-delay: 0.5s">
        <RecentActivity />
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';

useMeta({ title: 'Dashboard — PYMEQ' });
import DashboardStats from 'src/components/dashboard/DashboardStats.vue';
import DashboardActionCard from 'src/components/dashboard/DashboardActionCard.vue';
import RecentActivity from 'src/components/dashboard/RecentActivity.vue';

const authStore = useAuthStore();
</script>

<style lang="scss" scoped>
.dashboard-page {
  width: 100%;
}

.brand-glow-text {
  text-shadow: 0 0 20px rgba(163, 120, 94, 0.3);
}
</style>
