<template>
  <q-page class="dashboard-page">
    <CatalogDashboard v-if="hasTenant" />
    <DashboardStats v-else />
  </q-page>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from 'src/modules/auth/store';
import CatalogDashboard from 'src/modules/core/components/dashboard/CatalogDashboard.vue';
import DashboardStats from 'src/components/dashboard/DashboardStats.vue';

const authStore = useAuthStore();
const hasTenant = computed(() => !!authStore.user?.tenantId);
</script>

<style scoped>
.dashboard-page {
  width: 100%;
}
</style>
