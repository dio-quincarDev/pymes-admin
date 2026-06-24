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
</script>

<template>
  <div
    :class="cardClasses"
    role="button"
    tabindex="0"
    @click="emit('select', code)"
    @keydown.enter="emit('select', code)"
    @keydown.space.prevent="emit('select', code)"
  >
    <div class="industry-card__icon-wrap">
      <q-icon
        :name="icon"
        size="1.8rem"
        :color="selected ? 'primary' : 'accent'"
        class="industry-card__icon"
      />
      <Transition name="check">
        <q-icon
          v-if="selected"
          name="check_circle"
          size="1.2rem"
          color="primary"
          class="industry-card__check"
        />
      </Transition>
    </div>
    <div class="industry-card__name">{{ name }}</div>
    <div class="industry-card__desc">{{ desc }}</div>
  </div>
</template>

<style lang="scss" scoped>
.industry-card {
  background: rgba(27, 38, 36, 0.7);
  border: 1px solid rgba(163, 120, 94, 0.08);
  border-radius: 12px;
  padding: 1.25rem 1rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;

  &:hover {
    border-color: rgba(163, 120, 94, 0.25);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  }

  &--selected {
    border-color: #A3785E;
    background: rgba(163, 120, 94, 0.08);
    box-shadow: 0 0 20px rgba(163, 120, 94, 0.15), 0 8px 24px rgba(0, 0, 0, 0.2);

    .industry-card__icon-wrap {
      background: rgba(163, 120, 94, 0.15);
    }
  }

  &__icon-wrap {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 0.75rem;
    background: rgba(163, 120, 94, 0.06);
    transition: background 0.25s ease;
    position: relative;
  }

  &__check {
    position: absolute;
    top: -2px;
    right: -2px;
  }

  &__name {
    font-family: 'Outfit', sans-serif;
    font-weight: 600;
    font-size: 0.875rem;
    color: #E2E8E4;
    margin-bottom: 0.25rem;
  }

  &__desc {
    font-size: 0.75rem;
    color: #8A9E99;
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
