<template>
  <div class="opex-gauge">
    <svg :viewBox="`0 0 ${size} ${size / 2 + 20}`" class="opex-gauge__svg">
      <!-- Background arc -->
      <path
        :d="arcPath(1)"
        fill="none"
        stroke="rgba(138, 158, 153, 0.1)"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
      />
      <!-- Threshold warning -->
      <path
        v-if="normalizedValue < thresholds.critical"
        :d="arcPath(thresholds.warning / max)"
        fill="none"
        stroke="rgba(197, 160, 89, 0.15)"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
      />
      <!-- Value arc -->
      <path
        :d="arcPath(normalizedValue)"
        fill="none"
        :stroke="gaugeColor"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        class="opex-gauge__arc"
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
        Costo Operativo
      </text>
    </svg>
    <!-- Threshold markers -->
    <div class="opex-gauge__markers">
      <span class="opex-gauge__marker" style="left: 0%">0</span>
      <span class="opex-gauge__marker opex-gauge__marker--warning" :style="{ left: warningPct + '%' }">
        {{ thresholds.warning }}
      </span>
      <span class="opex-gauge__marker opex-gauge__marker--critical" :style="{ left: criticalPct + '%' }">
        {{ thresholds.critical }}
      </span>
      <span class="opex-gauge__marker" style="left: 100%">{{ max }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

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
const warningPct = computed(() => (props.thresholds.warning / props.max) * 100);
const criticalPct = computed(() => (props.thresholds.critical / props.max) * 100);

const gaugeColor = computed(() => {
  if (props.value >= props.thresholds.critical) return '#e94560';
  if (props.value >= props.thresholds.warning) return '#C5A059';
  return '#A3785E';
});

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

  &__arc {
    transition: stroke-dashoffset 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &__value {
    font-family: 'Outfit', sans-serif;
    font-size: 28px;
    font-weight: 700;
    fill: #E2E8E4;
  }

  &__label {
    font-size: 11px;
    fill: #8A9E99;
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
    font-size: 0.6rem;
    color: #8A9E99;
    transform: translateX(-50%);

    &--warning { color: #C5A059; }
    &--critical { color: #e94560; }
  }
}
</style>
