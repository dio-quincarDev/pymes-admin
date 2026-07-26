<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { formatCurrency, formatPct } from 'src/utils/format'
import { accountingService } from '../services/accounting.service'
import type { MetricasFinancieras } from '../types'
import KpiCard from 'src/modules/core/components/dashboard/KpiCard.vue'

useMeta({ title: 'Contabilidad — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId

const data = ref<MetricasFinancieras | null>(null)
const loading = ref(true)
const recalculando = ref(false)
const periodo = ref(new Date().toISOString().slice(0, 7))

async function load() {
  loading.value = true
  try {
    const res = await accountingService.consultar(tenantId, periodo.value)
    data.value = res.data
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar métricas' })
  } finally { loading.value = false }
}

async function recalcular() {
  recalculando.value = true
  try {
    const res = await accountingService.recalcular(tenantId, periodo.value)
    data.value = res.data
    $q.notify({ type: 'positive', message: 'Métricas recalculadas' })
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al recalcular' })
  } finally { recalculando.value = false }
}

onMounted(() => { if (tenantId) void load() })
</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-lg fade-in-up">
      <div class="row items-center justify-between">
        <div>
          <h1 class="text-h4 font-bold q-ma-none accounting-title">Contabilidad</h1>
          <p class="text-subtitle1 text-accent q-mt-xs">Métricas financieras consolidadas</p>
        </div>
        <div class="row items-center q-gutter-sm">
          <q-input dark dense filled v-model="periodo" label="Período" mask="####-##" class="accounting-period-input" />
          <q-btn color="primary" icon="refresh" label="Recalcular" :loading="recalculando" @click="recalcular" class="recalcular-btn" />
        </div>
      </div>
    </div>

    <div v-if="loading" class="row q-col-gutter-lg">
      <div v-for="i in 6" :key="i" class="col-12 col-sm-6 col-md-4">
        <div class="metric-skeleton skeleton" />
      </div>
    </div>

    <template v-else-if="data">
      <div class="row q-col-gutter-md q-mb-lg stagger-children">
        <div class="col-12 col-sm-6 col-md-4">
          <KpiCard label="Ingresos Totales" :value="formatCurrency(data.totalIncome)" icon="trending_up" accent="positive" />
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <KpiCard label="Costo de Mercadería" :value="formatCurrency(data.costOfGoods)" icon="shopping_cart" accent="negative" />
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <KpiCard label="Gastos Operativos" :value="formatCurrency(data.operatingExpenses)" icon="receipt" accent="gold" />
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <KpiCard
            label="Margen Bruto"
            :value="formatCurrency(data.grossMargin)"
            :delta="formatPct(data.grossMarginPct)"
            :trend="data.grossMarginPct >= 0 ? 'up' : 'down'"
            icon="paid"
            accent="copper"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <KpiCard
            label="Margen Operativo"
            :value="formatCurrency(data.operatingMargin)"
            :delta="formatPct(data.operatingMarginPct)"
            :trend="data.operatingMarginPct >= 0 ? 'up' : 'down'"
            icon="account_balance"
            accent="sage"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <KpiCard
            label="Margen Neto"
            :value="formatCurrency(data.netMargin)"
            :delta="formatPct(data.netMarginPct)"
            :trend="data.netMarginPct >= 0 ? 'up' : 'down'"
            icon="savings"
            accent="gold"
          />
        </div>
      </div>

      <div class="fade-in-up" style="animation-delay: 0.3s">
        <div class="summary-card glass">
          <div class="summary-card__header">
            <q-icon name="receipt_long" size="1.2rem" class="text-primary" />
            <span class="text-h6 text-primary">Resumen de Gastos</span>
          </div>
          <div class="summary-card__grid">
            <div class="summary-card__item">
              <span class="summary-card__item-label">Total Gastos</span>
              <span class="summary-card__item-value">{{ formatCurrency(data.totalExpenses) }}</span>
            </div>
            <div class="summary-card__item">
              <span class="summary-card__item-label">Pagos Préstamos</span>
              <span class="summary-card__item-value">{{ formatCurrency(data.loanPayments) }}</span>
            </div>
            <div class="summary-card__item">
              <span class="summary-card__item-label">Costo + G. Operativos</span>
              <span class="summary-card__item-value">{{ formatCurrency(data.costOfGoods + data.operatingExpenses) }}</span>
            </div>
            <div class="summary-card__item">
              <span class="summary-card__item-label">Resultado Neto</span>
              <span class="summary-card__item-value" :class="data.netMargin >= 0 ? 'text-positive' : 'text-negative'">{{ formatCurrency(data.netMargin) }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </q-page>
</template>

<style scoped lang="scss">
.accounting-title {
  font-family: 'Outfit', sans-serif;
  background: linear-gradient(135deg, #A3785E 0%, #C5A059 50%, #A3785E 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.accounting-period-input {
  :deep(.q-field__control) {
    border-radius: 8px;
  }
}

.recalcular-btn {
  border-radius: 8px;
}

.metric-skeleton {
  height: 110px;
  border-radius: 8px;
}

.summary-card {
  border-radius: 10px;
  padding: 1.5rem;

  &__header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.25rem;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid rgba(113, 131, 127, 0.1);
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 1.25rem;
  }

  &__item {
    text-align: center;
    padding: 0.75rem;
    border-radius: 6px;
    background: rgba(0, 0, 0, 0.15);
    transition: background 0.2s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.25);
    }
  }

  &__item-label {
    display: block;
    font-size: 0.72rem;
    font-weight: 500;
    color: #8A9E99;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    margin-bottom: 0.35rem;
  }

  &__item-value {
    font-family: 'Outfit', sans-serif;
    font-size: 1.1rem;
    font-weight: 600;
    color: #E2E8E4;
  }
}
</style>
