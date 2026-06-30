# Analytics Dashboard Frontend Strategy

> **Contexto:** Pivot en Core Backend — el módulo `analytics` (6 motores CTE) ahora impulsa el dashboard financiero. Frontend debe consumir `GET /api/v1/core/analytics/consultar` y visualizar los datos.

---

## 1. Contrato de Datos (Backend → Frontend)

**Endpoint:** `GET /api/v1/core/analytics/consultar?tenantId={uuid}&periodo=YYYY-MM`

**Response (`AnalyticsResponse`):**

| Motor                  | Campo          | Estructura                                                                                           | Descripción                         |
| ---------------------- | -------------- | ---------------------------------------------------------------------------------------------------- | ----------------------------------- |
| **ABC Gastos**         | `abc[]`        | `{ productId, productName, spend, pctTotal, cumulativePct, category: 'A', 'B', 'C' }`                | Pareto: 80/20 productos por gasto   |
| **Tendencias Precios** | `trend[]`      | `{ productId, productName, currentAvgPrice, movingAvg90d, pctChange }`                               | % cambio vs media móvil 90d         |
| **Impacto Márgenes**   | `margin[]`     | `{ productId, productName, currentPrice, previousPrice, pctChange }`                                 | Delta precio unitario %             |
| **Costo Operativo**    | `opexPct[]`    | `{ period, totalSpend, invoiceCount, productCount, providerCount, projectedMonthly, avgDailySpend }` | Gasto % ventas + proyección mensual |
| **Proyección**         | `projection[]` | `{ period, projectedSpend, confidence }`                                                             | Forecast 30/60/90d                  |
| **Alertas**            | `alerts[]`     | `{ productId, productName, currentPrice, avgPrice, variationPct, severity: 'warning', 'critical' }`  | Variación >15% (CV)                 |

---

## 2. Arquitectura Frontend

```
src/modules/core/
├── types/
│   └── analytics.ts                    # Tipos TypeScript (mirror backend)
├── services/
│   └── analytics.service.ts            # API calls tipadas
├── composables/
│   ├── useAnalytics.ts                 # Fetch + cache reactivo + getters por motor
│   └── usePeriod.ts                    # Período reactivo + persist localStorage
├── components/
│   ├── dashboard/
│   │   ├── AnalyticsDashboard.vue      # Vista principal (composición grid)
│   │   ├── KpiCard.vue                 # KPI reutilizable (valor, delta, trend, icon)
│   │   ├── AbcGastosChart.vue          # Barras apiladas + línea acumulada (Pareto)
│   │   ├── PriceTrendSparkline.vue     # Mini sparkline por producto
│   │   ├── MarginImpactTable.vue       # QTable sortable/filterable
│   │   ├── OpexGauge.vue               # Gauge SVG (sin dep extra)
│   │   ├── ProjectionTimeline.vue      # Línea + área (Chart.js)
│   │   ├── AlertsPanel.vue             # Lista accionable con severidad
│   │   └── PeriodSelector.vue          # Select YYYY-MM + botón recalcular
│   └── charts/
│       ├── BaseChart.vue               # Wrapper Chart.js común
│       ├── BarChart.vue
│       ├── LineChart.vue
│       └── GaugeChart.vue              # SVG gauge
└── pages/
    └── DashboardPage.vue               # Refactor: compone AnalyticsDashboard
```

---

## 3. Implementación por Fases

### Fase 1 — Base (Tipos, Service, Composables) ⏱️ ~3h

**`src/modules/core/types/analytics.ts`**

```typescript
export interface AbcItem {
  productId: string;
  productName: string;
  spend: number;
  pctTotal: number;
  cumulativePct: number;
  category: 'A' | 'B' | 'C';
}

export interface TrendItem {
  productId: string;
  productName: string;
  currentAvgPrice: number;
  movingAvg90d: number;
  pctChange: number;
}

export interface MarginItem {
  productId: string;
  productName: string;
  currentPrice: number;
  previousPrice: number;
  pctChange: number;
}

export interface OpexItem {
  period: string;
  totalSpend: number;
  invoiceCount: number;
  productCount: number;
  providerCount: number;
  projectedMonthly: number;
  avgDailySpend: number;
}

export interface ProjectionItem {
  period: string;
  projectedSpend: number;
  confidence: number;
}

export interface AlertItem {
  productId: string;
  productName: string;
  currentPrice: number;
  avgPrice: number;
  variationPct: number;
  severity: 'warning' | 'critical';
}

export interface AnalyticsResponse {
  id: string;
  tenantId: string;
  period: string;
  abc: AbcItem[];
  trend: TrendItem[];
  margin: MarginItem[];
  opexPct: OpexItem[];
  projection: ProjectionItem[];
  alerts: AlertItem[];
}
```

**`src/modules/core/services/analytics.service.ts`**

```typescript
import { api } from 'src/boot/axios';
import type { AnalyticsResponse } from '../types/analytics';

export const analyticsService = {
  consultar(tenantId: string, periodo?: string) {
    return api.get<AnalyticsResponse>('/core/analytics/consultar', {
      params: { tenantId, ...(periodo && { periodo }) },
    });
  },
  recalcular(tenantId: string, periodo: string) {
    return api.post<AnalyticsResponse>('/core/analytics/recalcular', null, {
      params: { tenantId, periodo },
    });
  },
};
```

**`src/modules/core/composables/useAnalytics.ts`**

```typescript
import { ref, computed, watch } from 'vue';
import { analyticsService } from '../services/analytics.service';
import type {
  AnalyticsResponse,
  AbcItem,
  TrendItem,
  MarginItem,
  OpexItem,
  ProjectionItem,
  AlertItem,
} from '../types/analytics';
import { useAuthStore } from 'src/modules/auth/store';
import { usePeriod } from './usePeriod';

export function useAnalytics() {
  const authStore = useAuthStore();
  const { period, setPeriod } = usePeriod();
  const data = ref<AnalyticsResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetch() {
    if (!authStore.user?.tenantId) return;
    loading.value = true;
    error.value = null;
    try {
      const res = await analyticsService.consultar(authStore.user.tenantId, period.value);
      data.value = res.data;
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Error cargando analytics';
    } finally {
      loading.value = false;
    }
  }

  async function recalcular() {
    if (!authStore.user?.tenantId) return;
    loading.value = true;
    try {
      const res = await analyticsService.recalcular(authStore.user.tenantId, period.value);
      data.value = res.data;
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Error recalculando';
    } finally {
      loading.value = false;
    }
  }

  // Getters por motor (reactivos)
  const abc = computed<AbcItem[]>(() => data.value?.abc ?? []);
  const trend = computed<TrendItem[]>(() => data.value?.trend ?? []);
  const margin = computed<MarginItem[]>(() => data.value?.margin ?? []);
  const opexPct = computed<OpexItem[]>(() => data.value?.opexPct ?? []);
  const projection = computed<ProjectionItem[]>(() => data.value?.projection ?? []);
  const alerts = computed<AlertItem[]>(() => data.value?.alerts ?? []);

  // Auto-fetch cuando cambia período
  watch(period, fetch, { immediate: true });

  return {
    data,
    loading,
    error,
    period,
    setPeriod,
    fetch,
    recalcular,
    abc,
    trend,
    margin,
    opexPct,
    projection,
    alerts,
  };
}
```

**`src/modules/core/composables/usePeriod.ts`**

```typescript
import { ref, onMounted, watch } from 'vue';

const STORAGE_KEY = 'pymeq_analytics_period';

export function usePeriod() {
  const period = ref<string>(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) return stored;
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });

  onMounted(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) period.value = stored;
  });

  watch(period, (val) => localStorage.setItem(STORAGE_KEY, val));

  function setPeriod(val: string) {
    period.value = val;
  }

  return { period, setPeriod };
}
```

---

### Fase 2 — Componentes UI ⏱️ ~10h

| Componente            | Props                                                                           | Librería               | Notas                                                           |
| --------------------- | ------------------------------------------------------------------------------- | ---------------------- | --------------------------------------------------------------- |
| `KpiCard`             | `label, value, delta?, trend?: 'up'\|'down', icon, accent`                      | BaseCard               | Reutilizable en todo el dashboard                               |
| `AbcGastosChart`      | `data: AbcItem[], height?: number`                                              | Chart.js (bar + line)  | Eje Y: spend; eje Y2: cumulativePct; colores A/B/C              |
| `PriceTrendSparkline` | `items: TrendItem[], inline?: boolean`                                          | Chart.js (line mini)   | Tooltip con % cambio; color verde/rojo                          |
| `MarginImpactTable`   | `items: MarginItem[]`                                                           | QTable                 | Columnas: producto, precio actual, anterior, % cambio; sortable |
| `OpexGauge`           | `value: number, max: number, thresholds: { warning: number, critical: number }` | SVG gauge              | Sin dep extra; animado                                          |
| `ProjectionTimeline`  | `items: ProjectionItem[]`                                                       | Chart.js (line + area) | 3 puntos: 30/60/90d; banda confianza                            |
| `AlertsPanel`         | `items: AlertItem[], onDismiss?: (id) => void`                                  | QList + QBadge         | Severidad: warning (ámbar) / critical (rojo); acción sugerida   |
| `PeriodSelector`      | `modelValue: string, @update:modelValue`                                        | QSelect                | Opciones: últimos 12 meses; botón "Recalcular"                  |

**`src/modules/core/components/charts/BaseChart.vue`** — Wrapper Chart.js con `onMounted`/`onUnmounted` cleanup, responsive, theme dark.

---

### Fase 3 — Composición Dashboard ⏱️ ~3h

**`src/modules/core/components/dashboard/AnalyticsDashboard.vue`**

```vue
<template>
  <div class="analytics-dashboard">
    <!-- Header con selector período + botón recalcular -->
    <div class="dashboard-header row items-center justify-between q-mb-lg">
      <div>
        <h2 class="text-h5 text-primary q-ma-none">Análisis de Gastos</h2>
        <span class="text-caption text-accent">Período: {{ period }}</span>
      </div>
      <div class="row items-center q-gutter-sm">
        <PeriodSelector v-model="period" @update:modelValue="setPeriod" />
        <q-btn
          color="primary"
          icon="refresh"
          label="Recalcular"
          @click="recalcular"
          :loading="loading"
        />
      </div>
    </div>

    <!-- Skeleton mientras carga -->
    <SkeletonLoader v-if="loading" :count="6" layout="grid" />

    <!-- Grid responsive de widgets -->
    <div v-else class="row q-col-gutter-lg">
      <!-- KPIs principales -->
      <div class="col-12 col-sm-6 col-lg-3" v-for="kpi in kpis" :key="kpi.label">
        <KpiCard v-bind="kpi" />
      </div>

      <!-- ABC Gastos (Pareto) - ancho completo móvil, 2/3 desktop -->
      <div class="col-12 col-xl-8">
        <BaseCard variant="ghost" class="h-full">
          <template #title>Clasificación ABC de Gastos</template>
          <AbcGastosChart :data="abc" height="300" />
        </BaseCard>
      </div>

      <!-- Alertas - 1/3 desktop -->
      <div class="col-12 col-xl-4">
        <AlertsPanel :items="alerts" />
      </div>

      <!-- Tendencias + Márgenes (tabs) -->
      <div class="col-12 col-lg-6">
        <BaseCard variant="ghost" class="h-full">
          <q-tabs v-model="trendTab" class="q-mb-md">
            <q-tab name="trend" label="Tendencias Precios" />
            <q-tab name="margin" label="Impacto Márgenes" />
          </q-tabs>
          <PriceTrendSparkline v-if="trendTab === 'trend'" :items="trend" />
          <MarginImpactTable v-else :items="margin" />
        </BaseCard>
      </div>

      <!-- Costo Operativo + Proyección -->
      <div class="col-12 col-lg-6">
        <BaseCard variant="ghost" class="h-full">
          <template #title>Costo Operativo</template>
          <OpexGauge :value="opexValue" :max="100" :thresholds="{ warning: 70, critical: 85 }" />
          <div class="q-mt-md text-caption text-accent">
            Proy. mensual: {{ formatCurrency(opexProjected) }}
          </div>
        </BaseCard>
        <BaseCard variant="ghost" class="h-full q-mt-lg">
          <template #title>Proyección 30/60/90d</template>
          <ProjectionTimeline :items="projection" />
        </BaseCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAnalytics } from '../../composables/useAnalytics';
import { useNumberFormat } from '../../composables/useNumberFormat';
import KpiCard from './KpiCard.vue';
import AbcGastosChart from './AbcGastosChart.vue';
import PriceTrendSparkline from './PriceTrendSparkline.vue';
import MarginImpactTable from './MarginImpactTable.vue';
import OpexGauge from './OpexGauge.vue';
import ProjectionTimeline from './ProjectionTimeline.vue';
import AlertsPanel from './AlertsPanel.vue';
import PeriodSelector from './PeriodSelector.vue';
import BaseCard from 'src/components/base/BaseCard.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';

const {
  data,
  loading,
  period,
  setPeriod,
  fetch,
  recalcular,
  abc,
  trend,
  margin,
  opexPct,
  projection,
  alerts,
} = useAnalytics();
const { formatCurrency } = useNumberFormat();
const trendTab = ref<'trend' | 'margin'>('trend');

const kpis = computed(() => [
  {
    label: 'Gasto Total',
    value: formatCurrency(opexPct.value[0]?.totalSpend ?? 0),
    accent: 'copper',
    icon: 'receipt_long',
  },
  {
    label: 'Proy. Mensual',
    value: formatCurrency(opexPct.value[0]?.projectedMonthly ?? 0),
    delta: '+5%',
    trend: 'up',
    accent: 'sage',
    icon: 'trending_up',
  },
  {
    label: 'Productos (ABC-A)',
    value: String(abc.value.filter((a) => a.category === 'A').length),
    accent: 'gold',
    icon: 'inventory_2',
  },
  {
    label: 'Alertas Críticas',
    value: String(alerts.value.filter((a) => a.severity === 'critical').length),
    delta: alerts.value.length > 0 ? 'Revisar' : '',
    trend: alerts.value.length > 0 ? 'down' : 'up',
    accent: alerts.value.some((a) => a.severity === 'critical') ? 'negative' : 'positive',
    icon: 'warning',
  },
]);

const opexValue = computed(() =>
  opexPct.value[0]?.totalSpend
    ? (opexPct.value[0].totalSpend / (opexPct.value[0].projectedMonthly || 1)) * 100
    : 0,
);
const opexProjected = computed(() => opexPct.value[0]?.projectedMonthly ?? 0);
</script>
```

---

### Fase 4 — Integración `DashboardPage.vue` ⏱️ ~1h

```vue
<!-- src/pages/DashboardPage.vue -->
<template>
  <q-page class="dashboard-page">
    <div class="dashboard-header fade-in-up">
      <h1 class="dashboard-title">
        {{ greeting }},
        <span class="dashboard-title__name">{{ authStore.user?.nombre || 'Auditor' }}</span>
      </h1>
      <p class="dashboard-subtitle">Panel de control de auditoría inteligente</p>
    </div>

    <AnalyticsDashboard v-if="hasTenant" />
    <EmptyDashboardState v-else @setup="goToOnboarding" />
  </q-page>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from 'src/modules/auth/store';
import { useGreeting } from 'src/composables/useGreeting';
import AnalyticsDashboard from 'src/modules/core/components/dashboard/AnalyticsDashboard.vue';
import EmptyDashboardState from './components/EmptyDashboardState.vue';

const router = useRouter();
const authStore = useAuthStore();
const { greeting } = useGreeting();
const hasTenant = computed(() => !!authStore.user?.tenantId);
const goToOnboarding = () => router.push('/onboarding');
</script>
```

---

## 4. Decisiones Técnicas (Tradeoffs)

| Decisión            | Opción Elegida            | Justificación                                                 |
| ------------------- | ------------------------- | ------------------------------------------------------------- |
| **Chart lib**       | Chart.js                  | Ya en Quasar, ligero, tree-shakable, barras/lineas nativas    |
| **Gauge Opex**      | SVG custom                | Evita dep ECharts pesado; gauge simple es 50 líneas SVG       |
| **Cache/Estado**    | Composable `ref` + watch  | Simple, reactivo, no requiere Pinia global                    |
| **Período default** | Mes actual (localStorage) | UX: recuerda última selección; backend usa mes actual si null |
| **Recálculo**       | Botón manual + toast      | Async (virtual threads), usuario controla cuándo              |
| **Empty state**     | Componente dedicado       | CTA claro → "Registra tu primera factura"                     |

---

## 5. Checklist de Implementación

| #   | Tarea                                  | Archivos                                       | Estado |
| --- | -------------------------------------- | ---------------------------------------------- | ------ |
| 1   | Tipos TypeScript                       | `types/analytics.ts`                           | ⬜     |
| 2   | Service API                            | `services/analytics.service.ts`                | ⬜     |
| 3   | Composable `useAnalytics`              | `composables/useAnalytics.ts`                  | ⬜     |
| 4   | Composable `usePeriod`                 | `composables/usePeriod.ts`                     | ⬜     |
| 5   | Composable `useNumberFormat`           | `composables/useNumberFormat.ts`               | ⬜     |
| 6   | Wrapper Chart.js                       | `components/charts/BaseChart.vue`              | ⬜     |
| 7   | KpiCard                                | `components/dashboard/KpiCard.vue`             | ⬜     |
| 8   | AbcGastosChart                         | `components/dashboard/AbcGastosChart.vue`      | ⬜     |
| 9   | PriceTrendSparkline                    | `components/dashboard/PriceTrendSparkline.vue` | ⬜     |
| 10  | MarginImpactTable                      | `components/dashboard/MarginImpactTable.vue`   | ⬜     |
| 11  | OpexGauge (SVG)                        | `components/dashboard/OpexGauge.vue`           | ⬜     |
| 12  | ProjectionTimeline                     | `components/dashboard/ProjectionTimeline.vue`  | ⬜     |
| 13  | AlertsPanel                            | `components/dashboard/AlertsPanel.vue`         | ⬜     |
| 14  | PeriodSelector                         | `components/dashboard/PeriodSelector.vue`      | ⬜     |
| 15  | AnalyticsDashboard (composición)       | `components/dashboard/AnalyticsDashboard.vue`  | ⬜     |
| 16  | Refactor DashboardPage                 | `pages/DashboardPage.vue`                      | ⬜     |
| 17  | EmptyDashboardState                    | `pages/components/EmptyDashboardState.vue`     | ⬜     |
| 18  | Tests unitarios (composables + charts) | `__tests__/`                                   | ⬜     |
| 19  | Lint + Build check                     | `npm run lint && npm run build`                | ⬜     |

---

## 6. Endpoints Backend (Referencia)

```bash
# Consultar analytics (cache 5min Redis en backend)
GET /api/v1/core/analytics/consultar?tenantId={uuid}&periodo=2026-06

# Recalcular forzar (async, virtual threads)
POST /api/v1/core/analytics/recalcular?tenantId={uuid}&periodo=2026-06
```

---

## 7. Próximos Pasos Post-MVP

~~

- [ ] WebSocket / SSE para updates en tiempo real cuando listener termina
- [ ] Exportar PDF/Excel del dashboard
- [ ] Drill-down: click en producto ABC → detalle tendencia + margen
- [ ] Comparativa multi-período (mes actual vs anterior)
- [ ] Alertas push (notificación navegador) para severity critical

---

**Creado:** 2026-06-26  
**Pivot:** Core Analytics → Dashboard Financiero  
**Responsable:** Frontend Team
