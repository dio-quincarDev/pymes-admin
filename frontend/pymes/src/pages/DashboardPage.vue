<script setup lang="ts">
import { computed, shallowRef, onMounted } from 'vue';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import CatalogDashboard from 'src/modules/core/components/dashboard/CatalogDashboard.vue';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';

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

    <!-- Honest empty state — no fabricated demo data -->
    <div v-else class="no-tenant-state">
      <q-icon name="domain_disabled" size="64px" style="color: var(--pq-text-subtle)" aria-hidden="true" />
      <h1 class="no-tenant-headline">Tu negocio aún no está configurado</h1>
      <p class="no-tenant-copy">Completá el onboarding para empezar a usar PymeQ.</p>
      <BaseButton variant="primary" size="lg" @click="$router.push('/onboarding')">
        COMPLETAR ONBOARDING
      </BaseButton>
      <p class="no-tenant-hint">¿Ya empezaste? Revisá tu correo para el enlace de verificación.</p>
    </div>
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

/* --------------------------------------------------
   Empty state — no tenant
-------------------------------------------------- */
.no-tenant-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-height: 60vh;
  gap: 20px;
  max-width: 480px;
  margin: 0 auto;
}

.no-tenant-headline {
  font-family: 'Geist', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--pq-text);
  margin: 0;
}

.no-tenant-copy {
  font-family: 'Satoshi', sans-serif;
  font-size: 16px;
  font-weight: 400;
  color: var(--pq-text-muted);
  margin: 0;
  max-width: 35ch;
}

.no-tenant-hint {
  font-family: 'Satoshi', sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: var(--pq-text-subtle);
  margin: 8px 0 0;
}
</style>
