<script setup lang="ts">
import { computed, shallowRef, onMounted } from 'vue';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import CatalogDashboard from 'src/modules/core/components/dashboard/CatalogDashboard.vue';
import DashboardStats from 'src/components/dashboard/DashboardStats.vue';
import BaseCard from 'src/components/base/BaseCard.vue';

useMeta({ title: 'Dashboard — PYMEQ' });

const authStore = useAuthStore();
const hasTenant = computed(() => !!authStore.user?.tenantId);
const loading = shallowRef(true);

onMounted(() => {
  // ponytail: 200ms settle window for child components
  setTimeout(() => { loading.value = false; }, 200);
});
</script>

<template>
  <q-page class="dashboard-page">
    <template v-if="loading">
      <div class="row q-col-gutter-lg">
        <div v-for="i in 3" :key="i" class="col-12 col-sm-6 col-md-4">
          <BaseCard class="q-pa-lg">
            <div class="row items-center justify-between q-mb-md">
              <div class="skeleton skeleton-text w-40" />
              <div class="skeleton skeleton-circle" />
            </div>
            <div class="skeleton skeleton-value q-mb-sm" />
            <div class="skeleton skeleton-text w-30" />
          </BaseCard>
        </div>
      </div>
    </template>

    <CatalogDashboard v-else-if="hasTenant" />
    <DashboardStats v-else />
  </q-page>
</template>

<style scoped lang="scss">
.dashboard-page {
  width: 100%;
}

.skeleton {
  background: linear-gradient(
    90deg,
    rgba(27, 38, 36, 0.6) 0%,
    rgba(163, 120, 94, 0.15) 50%,
    rgba(27, 38, 36, 0.6) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 4px;

  &-text { height: 12px; }
  &-value { height: 28px; width: 60%; }
  &-circle { width: 24px; height: 24px; border-radius: 50%; }
}

.w-40 { width: 40%; }
.w-30 { width: 30%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
