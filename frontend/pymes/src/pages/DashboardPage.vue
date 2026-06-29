<template>
  <q-page class="dashboard-page">
    <div class="dashboard-header fade-in-up">
      <h1 class="dashboard-title">
        {{ greeting }}, <span class="dashboard-title__name">{{ authStore.user?.nombre || 'Auditor' }}</span>
      </h1>
      <p class="dashboard-subtitle">Panel de control de auditoría inteligente</p>
    </div>

    <AnalyticsDashboard v-if="hasTenant" />
    <DashboardStats v-else />
  </q-page>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useGreeting } from 'src/composables/useGreeting';
import AnalyticsDashboard from 'src/modules/core/components/dashboard/AnalyticsDashboard.vue';
import DashboardStats from 'src/components/dashboard/DashboardStats.vue';

useMeta({ title: 'Dashboard — PYMEQ' });

const authStore = useAuthStore();
const { greeting } = useGreeting();
const hasTenant = computed(() => !!authStore.user?.tenantId);
</script>

<style lang="scss" scoped>
.dashboard-page {
  width: 100%;
}

.dashboard-header {
  margin-bottom: 1.5rem;
}

.dashboard-title {
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 1.75rem;
  color: #E2E8E4;
  margin: 0;
  text-shadow: 0 0 20px rgba(163, 120, 94, 0.15);

  &__name {
    color: #A3785E;
  }
}

.dashboard-subtitle {
  font-size: 0.95rem;
  color: #8A9E99;
  margin: 0.25rem 0 0;
}
</style>
