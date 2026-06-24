<template>
  <q-page class="dashboard-page">
    <div class="dashboard-header fade-in-up">
      <h1 class="dashboard-title">
        {{ greeting }}, <span class="dashboard-title__name">{{ authStore.user?.nombre || 'Auditor' }}</span>
      </h1>
      <p class="dashboard-subtitle">Panel de control de auditoria inteligente</p>
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
import { useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { useGreeting } from 'src/composables/useGreeting'
import DashboardStats from 'src/components/dashboard/DashboardStats.vue'
import DashboardActionCard from 'src/components/dashboard/DashboardActionCard.vue'
import RecentActivity from 'src/components/dashboard/RecentActivity.vue'

useMeta({ title: 'Dashboard — PYMEQ' })

const authStore = useAuthStore()
const { greeting } = useGreeting()
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
