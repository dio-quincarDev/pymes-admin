<template>
  <div class="opex-gauge">
    <svg :viewBox="`0 0 ${size} ${size / 2 + 20}`" class="opex-gauge__svg">
      <!-- Background arc -->
      <path
        :d="arcPath(1)"
        fill="none"
        stroke="var(--pq-chart-grid)"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
      />
      <!-- Threshold warning -->
      <path
        v-if="normalizedValue < thresholds.critical"
        :d="arcPath(thresholds.warning / max)"
        fill="none"
        stroke="var(--pq-warning)"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        opacity="0.15"
      />
      <!-- Value arc -->
      <path
        :d="arcPath(normalizedValue)"
        fill="none"
        :stroke="gaugeColor"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
      />
      <!-- Center text -->
      <text
        :x="size / 2"
        :y="size / 2 - 5"
        text-anchor="middle"
        class="opex-gauge__value"
      >
        {{ value.toFixed(0) }}%
      </text>
      <text
        :x="size / 2"
        :y="size / 2 + 14"
        text-anchor="middle"
        class="opex-gauge__label"
      >
        Costo del día
      </text>
    </svg>
    <!-- Threshold markers (alineados a la curva del arco) -->
    <div class="opex-gauge__markers">
      <span class="opex-gauge__marker" :style="{ left: zeroLeft }">0</span>
      <span class="opex-gauge__marker opex-gauge__marker--warning" :style="{ left: warningPct }">
        {{ thresholds.warning }}
      </span>
      <span class="opex-gauge__marker opex-gauge__marker--critical" :style="{ left: criticalPct }">
        {{ thresholds.critical }}
      </span>
      <span class="opex-gauge__marker" :style="{ left: maxLeft }">{{ max }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

// shortcut: componente huérfano hoy — nadie lo importa; vuelve con la sección colapsable
// "Costo Operativo" (FRONTEND_PENDIENTES_STRATEGY fase 4c-3). Si ese plan muere, borrar este archivo.
interface Props {
  value: number;
  max: number;
  thresholds: { warning: number; critical: number };
}

const props = defineProps<Props>();

const size = 200;
const strokeWidth = 14;
const radius = (size - strokeWidth) / 2;
const centerX = size / 2;
const centerY = size / 2 + 5;

const normalizedValue = computed(() => Math.min(props.value / props.max, 1));
const warningPct = computed(() => markerLeft(props.thresholds.warning / props.max));
const criticalPct = computed(() => markerLeft(props.thresholds.critical / props.max));
const zeroLeft = computed(() => markerLeft(0));
const maxLeft = computed(() => markerLeft(1));

const gaugeColor = computed(() => {
  if (props.value >= props.thresholds.critical) return 'var(--pq-danger)';
  if (props.value >= props.thresholds.warning) return 'var(--pq-warning)';
  return 'var(--pq-chart-bar)';
});

// posición bajo un semicírculo: x = centerX - radius·cos(π·ratio) — escala lineal NO alinea con el arco
function markerLeft(ratio: number): string {
  const clamped = Math.max(0, Math.min(ratio, 1));
  return `${((centerX - radius * Math.cos(Math.PI * clamped)) / size) * 100}%`;
}

function arcPath(ratio: number) {
  const clamped = Math.max(0, Math.min(ratio, 1));
  const angle = Math.PI * clamped;
  const startX = centerX - radius;
  const endX = centerX - radius * Math.cos(angle);
  const endY = centerY - radius * Math.sin(angle);
  const largeArc = clamped > 0.5 ? 1 : 0;
  return `M ${startX} ${centerY} A ${radius} ${radius} 0 ${largeArc} 1 ${endX} ${endY}`;
}
</script>

<style scoped lang="scss">
.opex-gauge {
  display: flex;
  flex-direction: column;
  align-items: center;

  &__svg {
    width: 100%;
    max-width: 200px;
  }

  &__value {
    font-family: 'Geist', sans-serif;
    font-size: 28px;
    font-weight: 700;
    fill: var(--pq-text);
  }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    fill: var(--pq-text-muted);
  }

  &__markers {
    position: relative;
    width: 100%;
    max-width: 200px;
    height: 1rem;
    margin-top: 0.25rem;
  }

  &__marker {
    position: absolute;
    font-family: 'Geist Mono', monospace;
    font-size: 10px;
    color: var(--pq-text-muted);
    transform: translateX(-50%);

    &--warning { color: var(--pq-warning); }
    &--critical { color: var(--pq-danger); }
  }
}
</style>
