<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  code: string
  name: string
  icon: string
  desc: string
  selected: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [code: string]
}>()

const cardClasses = computed(() => ({
  'industry-card': true,
  'industry-card--selected': props.selected,
}))

const pressedState = computed(() => props.selected ? 'true' : 'false')
</script>

<template>
  <div
    :class="cardClasses"
    role="button"
    tabindex="0"
    :aria-pressed="pressedState"
    @click="emit('select', code)"
    @keydown.enter="emit('select', code)"
    @keydown.space.prevent="emit('select', code)"
  >
    <div class="industry-card__icon-wrap">
      <q-icon
        :name="icon"
        size="1.8rem"
        style="color: var(--pq-accent)"
        class="industry-card__icon"
      />
      <Transition name="check">
        <q-icon
          v-if="selected"
          name="check_circle"
          size="1.2rem"
          style="color: var(--pq-success)"
          class="industry-card__check"
          aria-hidden="true"
        />
      </Transition>
    </div>
    <div class="industry-card__name">{{ name }}</div>
    <div class="industry-card__desc">{{ desc }}</div>
  </div>
</template>

<style lang="scss" scoped>
.industry-card {
  background: rgba(18, 20, 26, 0.7);
  border: 1px solid rgba(53, 57, 69, 0.5);
  border-radius: var(--pq-radius-lg);
  padding: 20px 16px;
  text-align: center;
  cursor: pointer;
  transition: all var(--pq-motion-base);
  position: relative;

  &:hover {
    border-color: rgba(200, 150, 62, 0.25);
    transform: translateY(-2px);
    box-shadow: var(--pq-shadow-md);
  }

  &:focus-visible {
    outline: 2px solid var(--pq-accent);
    outline-offset: 2px;
  }

  &--selected {
    border-color: var(--pq-accent);
    background: rgba(200, 150, 62, 0.08);
    box-shadow: var(--pq-shadow-md);

    .industry-card__icon-wrap {
      background: rgba(200, 150, 62, 0.15);
    }
  }

  &__icon-wrap {
    width: 48px;
    height: 48px;
    border-radius: var(--pq-radius-full);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 12px;
    background: rgba(200, 150, 62, 0.06);
    transition: background var(--pq-motion-fast);
    position: relative;
  }

  &__check {
    position: absolute;
    top: -2px;
    right: -2px;
  }

  &__name {
    font-family: 'Geist', sans-serif;
    font-weight: 600;
    font-size: 14px;
    color: var(--pq-text);
    margin-bottom: 4px;
  }

  &__desc {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-muted);
    line-height: 1.4;
  }
}

.check-enter-active {
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.check-leave-active {
  transition: all 0.15s ease;
}

.check-enter-from,
.check-leave-to {
  opacity: 0;
  transform: scale(0.5);
}
</style>
