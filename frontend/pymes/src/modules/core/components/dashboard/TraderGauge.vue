<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  score: number;
  label?: string;
  size?: number;
  showValue?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  label: '',
  size: 80,
  showValue: true,
});

const clamped = computed(() => Math.max(0, Math.min(100, props.score)));
const rotation = computed(() => (clamped.value / 100) * 180 - 90);
const color = computed(() => {
  if (clamped.value < 40) return 'var(--pq-danger)';
  if (clamped.value < 70) return 'var(--pq-warning)';
  return 'var(--pq-success)';
});

const containerStyle = computed(() => ({ width: props.size + 'px' }));
const arcStyle = computed(() => ({ width: props.size + 'px', height: props.size / 2 + 'px' }));
const innerStyle = computed(() => ({ width: props.size - 12 + 'px', height: (props.size - 12) / 2 + 'px' }));
const fillStyle = computed(() => ({
  background: `conic-gradient(from 270deg at 50% 100%, ${color.value} 0deg, ${color.value} ${clamped.value * 1.8}deg, var(--pq-elevated) ${clamped.value * 1.8}deg, var(--pq-elevated) 180deg)`,
}));
const needleStyle = computed(() => ({ transform: `translateX(-50%) rotate(${rotation.value}deg)` }));
const valueStyle = computed(() => ({ color: color.value }));
</script>

<template>
  <div
    class="trader-gauge"
    :style="containerStyle"
    role="img"
    :aria-label="`${label} ${clamped} de 100`"
  >
    <div class="trader-gauge__arc" :style="arcStyle">
      <div class="trader-gauge__bg" />
      <div class="trader-gauge__fill" :style="fillStyle" />
      <div class="trader-gauge__inner" :style="innerStyle" />
      <div class="trader-gauge__needle" :style="needleStyle" aria-hidden="true" />
      <div class="trader-gauge__center-dot" aria-hidden="true" />
    </div>
    <div v-if="showValue" class="trader-gauge__value" :style="valueStyle">{{ clamped }}</div>
    <div v-if="label" class="trader-gauge__label">{{ label }}</div>
  </div>
</template>

<style scoped lang="scss">
.trader-gauge {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
  max-width: 100%;

  &__arc {
    position: relative;
    overflow: hidden;
    flex-shrink: 0;
  }

  &__bg,
  &__fill,
  &__inner {
    position: absolute;
    left: 0;
    bottom: 0;
    width: 100%;
    border-radius: 9999px 9999px 0 0;
  }

  &__bg {
    height: 100%;
    background: var(--pq-elevated);
    border: 1px solid var(--pq-border);
    border-bottom: none;
  }

  &__fill {
    height: 100%;
    border-radius: 9999px 9999px 0 0;
  }

  &__inner {
    left: 6px;
    bottom: 0;
    background: var(--pq-surface);
    border-radius: 9999px 9999px 0 0;
  }

  &__needle {
    position: absolute;
    left: 50%;
    bottom: 0;
    width: 2px;
    height: 48%;
    background: var(--pq-text);
    transform-origin: bottom center;
    border-radius: 1px;
    transition: transform 600ms cubic-bezier(0.16, 1, 0.3, 1);
  }

  &__center-dot {
    position: absolute;
    left: 50%;
    bottom: 0;
    width: 8px;
    height: 8px;
    background: var(--pq-text);
    border-radius: 50%;
    transform: translate(-50%, 50%);
    border: 2px solid var(--pq-surface);
  }

  &__value {
    font-family: 'Geist Mono', monospace;
    font-size: 16px;
    font-weight: 600;
    line-height: 1;
    margin-top: 6px;
    font-variant-numeric: tabular-nums;
  }

  &__label {
    font-family: 'Satoshi', sans-serif;
    font-size: 10px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    margin-top: 2px;
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
  }
}
</style>
