<template>
  <div class="row q-col-gutter-lg">
    <div
      v-for="(stat, i) in stats"
      :key="stat.label"
      class="col-12 col-sm-6 col-md-4 fade-in-up"
      :style="{ animationDelay: `${0.1 + i * 0.1}s` }"
    >
      <div class="stat-card" :class="`stat-card--${stat.accent}`">
        <div class="stat-card__header">
          <span class="stat-card__label">{{ stat.label }}</span>
          <q-icon :name="stat.icon" size="1.1rem" class="stat-card__icon" />
        </div>
        <div class="stat-card__value">{{ stat.value }}</div>
        <div v-if="stat.meta" class="stat-card__meta">
          <q-icon v-if="stat.metaIcon" :name="stat.metaIcon" size="0.75rem" class="q-mr-xs" />
          {{ stat.meta }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const stats = [
  {
    label: 'Auditorias Activas',
    icon: 'security',
    value: '12',
    accent: 'copper',
    meta: '+2 este mes',
    metaIcon: 'trending_up',
  },
  {
    label: 'Riesgo Promedio',
    icon: 'gpp_maybe',
    value: 'Bajo',
    accent: 'sage',
    meta: 'Basado en 45 controles',
    metaIcon: '',
  },
  {
    label: 'Usuarios Activos',
    icon: 'people',
    value: '1',
    accent: 'gold',
    meta: 'Licencia: Business Core',
    metaIcon: '',
  },
]
</script>

<style lang="scss" scoped>
.stat-card {
  background: rgba(27, 38, 36, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(113, 131, 127, 0.05);
  border-radius: 8px;
  padding: 1.25rem 1.5rem;
  position: relative;
  overflow: hidden;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    border-radius: 0 2px 2px 0;
    transition: opacity 0.2s ease;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }

  &--copper::before { background: #A3785E; }
  &--sage::before { background: #2D5A27; }
  &--gold::before { background: #C5A059; }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.75rem;
  }

  &__label {
    font-size: 0.75rem;
    font-weight: 500;
    color: #8A9E99;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__icon {
    color: rgba(163, 120, 94, 0.4);
  }

  &__value {
    font-family: 'Outfit', sans-serif;
    font-size: 2rem;
    font-weight: 700;
    color: #E2E8E4;
    line-height: 1;
  }

  &__meta {
    font-size: 0.75rem;
    color: #8A9E99;
    margin-top: 0.5rem;
    display: flex;
    align-items: center;
  }
}
</style>
