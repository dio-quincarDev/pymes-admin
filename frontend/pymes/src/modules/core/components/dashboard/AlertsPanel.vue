<script setup lang="ts">
import { computed, shallowRef } from 'vue';
import { useRouter } from 'vue-router';
import type { AlertItem } from '../../types/analytics';
import { alertDummyText, alertFallbackText, toNumber, withinTrustedAlertRange } from '../../utils/analyticsNormalize';
import { useNumberFormat } from 'src/modules/core/composables/useNumberFormat';

const router = useRouter();
const { formatCurrency } = useNumberFormat();

// Acción de 1 click: el producto con su nombre ya en el buscador de Productos
function goToProduct(productName: string) {
  void router.push({ name: 'productos', query: { buscar: productName } });
}

interface Props {
  items: AlertItem[];
}

const props = defineProps<Props>();

// ponytail: filtro UI puro — esconde basura 0.00 (Mayonesa 0.00 vs 3.88) sin tocar backend; si necesitás persistir, filtra en AnalyticsServiceImpl
const filteredAlerts = computed(() =>
  props.items.filter((a) => toNumber(a.currentPrice) > 0 && toNumber(a.avgPrice) > 0 && Number.isFinite(toNumber(a.variationPct, 0))),
);

// ponytail: tope fijo, sin scroll infinito — el backend ya ordena por cv/premium desc; subir TOP_VISIBLE si piden
const TOP_VISIBLE = 8;
const expanded = shallowRef(false);

const visibleAlerts = computed(() => {
  if (expanded.value) return filteredAlerts.value;
  // ponytail: por defecto solo alertas ≤ +100% (arriba es error de registro: Huevos +140%, Orégano +112%);
  // el resto queda tras "ver más", gemelo del rango 35–75 de recomendaciones
  return filteredAlerts.value
    .filter((a) => withinTrustedAlertRange(toNumber(a.variationPct, 0)))
    .slice(0, TOP_VISIBLE);
});
const hiddenCount = computed(() => filteredAlerts.value.length - visibleAlerts.value.length);

const hasCritical = computed(() => filteredAlerts.value.some((a) => a.severity === 'critical'));

// shortcut: cada fila usa su propia escala (max antes/ahora o pagaste/suele costar) — no compares longitudes entre productos;
// upgrade a escala global solo si se pide comparar montos absolutos entre filas
interface RowVm {
  key: string;
  name: string;
  detail: string;
  iconName: 'error' | 'warning';
  iconColor: 'negative' | 'warning';
  hasBars: boolean;
  topLabel: string;
  topWidth: string;
  topAmount: string;
  topFill: 'alerts-panel__fill--ok' | 'alerts-panel__fill--red' | 'alerts-panel__fill--yellow';
  bottomLabel: string;
  bottomWidth: string;
  bottomAmount: string;
  bottomFill: 'alerts-panel__fill--ok' | 'alerts-panel__fill--red' | 'alerts-panel__fill--yellow';
  overpay: string;
  critical: boolean;
}

const rows = computed<RowVm[]>(() =>
  visibleAlerts.value.map((alert) => {
    const critical = alert.severity === 'critical';
    const trail = alert.priceTrail;
    // PRICE_VARIATION nunca trae currentPrice real: barras solo con el recorrido de facturas
    // (primera vs última compra, mismo carril); sin historial → texto solo
    const trailBars =
      alert.alertKind !== 'SUPPLIER_PREMIUM' &&
      !!trail && trail.singleLane && trail.count >= 2 && trail.firstPrice !== trail.lastPrice;

    const vm: RowVm = {
      key: alert.productId,
      name: alert.productName,
      detail: alertDummyText(alert) || alertFallbackText(alert),
      iconName: critical ? 'error' : 'warning',
      iconColor: critical ? 'negative' : 'warning',
      hasBars: false,
      topLabel: '',
      topWidth: '0%',
      topAmount: '',
      topFill: 'alerts-panel__fill--ok',
      bottomLabel: '',
      bottomWidth: '0%',
      bottomAmount: '',
      bottomFill: 'alerts-panel__fill--ok',
      overpay: '',
      critical,
    };

    if (trailBars && trail) {
      const up = trail.lastPrice > trail.firstPrice;
      const max = Math.max(trail.firstPrice, trail.lastPrice, 1);
      vm.hasBars = true;
      vm.topLabel = 'Antes';
      vm.topWidth = `${Math.min(100, (trail.firstPrice / max) * 100)}%`;
      vm.topAmount = formatCurrency(trail.firstPrice);
      vm.bottomLabel = 'Ahora';
      vm.bottomWidth = `${Math.min(100, (trail.lastPrice / max) * 100)}%`;
      vm.bottomAmount = formatCurrency(trail.lastPrice);
      vm.bottomFill = up ? (critical ? 'alerts-panel__fill--red' : 'alerts-panel__fill--yellow') : 'alerts-panel__fill--ok';
      vm.overpay = `${up ? 'Subió' : 'Bajó'} ${formatCurrency(Math.abs(trail.lastPrice - trail.firstPrice))}`;
      return vm;
    }

    if (alert.alertKind === 'SUPPLIER_PREMIUM') {
      const paid = toNumber(alert.currentPrice);
      const fair = toNumber(alert.avgPrice);
      const max = Math.max(paid, fair, 1);
      const over = paid > fair ? paid - fair : 0;
      vm.hasBars = true;
      vm.topLabel = 'Pagaste';
      vm.topWidth = `${Math.min(100, (paid / max) * 100)}%`;
      vm.topAmount = formatCurrency(paid);
      vm.topFill = over > 0 ? (critical ? 'alerts-panel__fill--red' : 'alerts-panel__fill--yellow') : 'alerts-panel__fill--ok';
      vm.bottomLabel = 'Suele costar';
      vm.bottomWidth = `${Math.min(100, (fair / max) * 100)}%`;
      vm.bottomAmount = formatCurrency(fair);
      vm.overpay = over > 0 ? `Estás pagando ${formatCurrency(over)} de más` : '';
    }
    return vm;
  }),
);
</script>

<template>
  <div class="alerts-panel">
    <div class="alerts-panel__header">
      <span class="alerts-panel__title">Desvíos en Precios de Insumos</span>
      <q-badge v-if="filteredAlerts.length" :color="hasCritical ? 'negative' : 'warning'" rounded>
        {{ filteredAlerts.length }}
      </q-badge>
    </div>
    <p class="alerts-panel__subtitle">Productos con sobreprecio o variación anormal</p>

    <div v-if="!filteredAlerts.length" class="alerts-panel__empty">
      <q-icon name="check_circle" size="2rem" class="text-positive" />
      <span class="text-grey-6 q-mt-sm">Sin alertas activas</span>
    </div>

    <q-list v-else dense class="alerts-panel__list">
      <q-item v-for="row in rows" :key="row.key" class="alerts-panel__item">
        <q-item-section avatar>
          <q-icon :name="row.iconName" :color="row.iconColor" size="sm" />
        </q-item-section>
        <q-item-section>
          <q-item-label class="alerts-panel__name">{{ row.name }}</q-item-label>
          <div v-if="row.hasBars" class="alerts-panel__bars">
            <div class="alerts-panel__bar-row">
              <span class="alerts-panel__bar-label">{{ row.topLabel }}</span>
              <div class="alerts-panel__track"><span class="alerts-panel__fill" :class="row.topFill" :style="{ width: row.topWidth }" /></div>
              <span class="alerts-panel__bar-amount">{{ row.topAmount }}</span>
            </div>
            <div class="alerts-panel__bar-row">
              <span class="alerts-panel__bar-label">{{ row.bottomLabel }}</span>
              <div class="alerts-panel__track"><span class="alerts-panel__fill" :class="row.bottomFill" :style="{ width: row.bottomWidth }" /></div>
              <span class="alerts-panel__bar-amount">{{ row.bottomAmount }}</span>
            </div>
            <div v-if="row.overpay" class="alerts-panel__overpay">
              {{ row.overpay }}
            </div>
          </div>
          <q-item-label caption class="alerts-panel__detail">{{ row.detail }}</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-btn
            flat
            dense
            round
            icon="sym_r_open_in_new"
            color="primary"
            size="sm"
            class="alerts-panel__go"
            :aria-label="`Ver ${row.name} en Productos`"
            @click="goToProduct(row.name)"
          >
            <q-tooltip>Ver producto y sus compras</q-tooltip>
          </q-btn>
        </q-item-section>
      </q-item>
    </q-list>
    <q-btn
      v-if="hiddenCount > 0 || expanded"
      flat
      dense
      size="sm"
      color="warning"
      class="alerts-panel__more"
      :label="expanded ? 'Ver menos' : `+${hiddenCount} más este mes`"
      @click="expanded = !expanded"
    />
  </div>
</template>

<style scoped lang="scss">
.alerts-panel {
  background: rgba(11, 18, 16, 0.5);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(163, 120, 94, 0.1);
  border-radius: 8px;
  padding: 1rem;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.75rem;
  }

  &__title {
    font-size: 0.85rem;
    font-weight: 600;
    color: #e2e8e4;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__subtitle {
    font-size: 0.7rem;
    color: #8a9e99;
    margin: -0.5rem 0 0.75rem;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 1.5rem 0;
  }

  &__list {
    max-height: 300px;
    overflow-y: auto;
  }

  &__item {
    padding: 0.5rem 0;
    border-bottom: 1px solid rgba(138, 158, 153, 0.08);

    &:last-child {
      border-bottom: none;
    }
  }

  &__bars {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-top: 4px;
    max-width: 100%;
  }

  &__bar-row {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 8px;
    align-items: center;
    min-width: 0;
  }

  &__bar-label {
    font-size: 0.65rem;
    color: #8a9e99;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    white-space: nowrap;
  }

  &__track {
    height: 6px;
    border-radius: 3px;
    background: rgba(138, 158, 153, 0.15);
    overflow: hidden;
    min-width: 0;
  }

  &__fill {
    display: block;
    height: 100%;
    border-radius: 3px;

    &--ok { background: rgba(78, 138, 109, 0.75); }     // --pq-success translúcido
    &--red { background: rgba(192, 106, 96, 0.9); }     // --pq-danger
    &--yellow { background: rgba(196, 154, 82, 0.9); }  // --pq-warning
  }

  &__bar-amount {
    font-size: 0.7rem;
    font-variant-numeric: tabular-nums;
    color: #e2e8e4;
    white-space: nowrap;
  }

  &__overpay {
    font-size: 0.7rem;
    font-weight: 600;
    color: var(--pq-warning, #C49A52);
  }

  &__name {
    font-size: 0.8rem;
    color: #e2e8e4;
  }

  &__detail {
    font-size: 0.7rem;
    color: #8a9e99;
  }

  &__go {
    position: relative;

    // zona táctil ≥44px sin deformar el ícono 24px (responsive-design: touch targets)
    &::before {
      content: '';
      position: absolute;
      inset: -10px;
    }
  }

  &__more {
    margin: 0.25rem auto 0;
    display: block;
    font-size: 0.75rem;
  }
}
</style>
