<script setup lang="ts">
import { computed } from 'vue';
import { useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useFinancialDashboard } from 'src/modules/core/composables/useFinancialDashboard';
import PeriodSelector from 'src/modules/core/components/dashboard/PeriodSelector.vue';
import StatStrip from 'src/modules/core/components/dashboard/StatStrip.vue';
import ExpenseBreakdown from 'src/modules/core/components/dashboard/ExpenseBreakdown.vue';
import RecentActivity from 'src/modules/core/components/dashboard/RecentActivity.vue';
import PendingInvoices from 'src/modules/core/components/dashboard/PendingInvoices.vue';
import QuickActions from 'src/modules/core/components/dashboard/QuickActions.vue';
import BaseButton from 'src/components/base/BaseButton.vue';

useMeta({ title: 'Dashboard — PYMEQ' });

const authStore = useAuthStore();
const hasTenant = computed(() => !!authStore.user?.tenantId);

const {
  metricas,
  gastosPorCategoria,
  actividadReciente,
  facturasPendientes,
  loading,
  periodo,
  setPeriod,
  recalcular,
} = useFinancialDashboard();
</script>

<template>
  <q-page class="dashboard-page">
    <!-- No tenant empty state -->
    <template v-if="!hasTenant">
      <div class="no-tenant-state">
        <q-icon name="domain_disabled" size="64px" style="color: var(--pq-text-subtle)" aria-hidden="true" />
        <h1 class="no-tenant-headline">Tu negocio aún no está configurado</h1>
        <p class="no-tenant-copy">Completá el onboarding para empezar a usar PymeQ.</p>
        <BaseButton variant="primary" size="lg" @click="$router.push('/onboarding')">
          COMPLETAR ONBOARDING
        </BaseButton>
        <p class="no-tenant-hint">¿Ya empezaste? Revisá tu correo para el enlace de verificación.</p>
      </div>
    </template>

    <!-- Financial dashboard -->
    <template v-else>
      <!-- Header -->
      <div class="dashboard-header">
        <div class="dashboard-header__row">
          <h1 class="dashboard-title">Dashboard</h1>
          <PeriodSelector
            :model-value="periodo"
            :loading="loading"
            @update:model-value="setPeriod"
            @recalcular="recalcular"
          />
        </div>
      </div>

      <!-- Stat Strip -->
      <StatStrip :metricas="metricas" :loading="loading" />

      <!-- Two-column grid -->
      <div class="dashboard-grid">
        <ExpenseBreakdown :gastos="gastosPorCategoria" :loading="loading" />
        <RecentActivity :actividades="actividadReciente" :loading="loading" />
      </div>

      <!-- Two-column grid -->
      <div class="dashboard-grid">
        <PendingInvoices :facturas="facturasPendientes" :loading="loading" />
        <QuickActions />
      </div>
    </template>
  </q-page>
</template>

<style scoped lang="scss">
.dashboard-page {
  width: 100%;
}

.dashboard-header {
  margin-bottom: 8px;

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
  }
}

.dashboard-title {
  font-family: 'Geist', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--pq-text);
  margin: 0;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
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
