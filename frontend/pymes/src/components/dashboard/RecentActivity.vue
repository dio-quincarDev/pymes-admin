<template>
  <BaseCard variant="ghost" class="activity-card q-pa-lg full-height">
    <h3 class="activity-card__title">Actividad Reciente</h3>
    <q-list dark separator class="activity-card__list">
      <q-item
        v-for="(event, i) in events"
        :key="i"
        class="activity-card__item"
      >
        <q-item-section avatar>
          <div class="activity-card__icon-wrap" :class="`activity-card__icon-wrap--${event.type}`">
            <q-icon :name="event.icon" size="xs" />
          </div>
        </q-item-section>
        <q-item-section>
          <q-item-label class="activity-card__label">{{ event.text }}</q-item-label>
          <q-item-label caption class="activity-card__time">{{ event.time }}</q-item-label>
        </q-item-section>
      </q-item>
    </q-list>
    <BaseButton variant="ghost" size="sm" class="full-width q-mt-md">Ver todo</BaseButton>
  </BaseCard>
</template>

<script setup lang="ts">
import BaseCard from 'src/components/base/BaseCard.vue'
import BaseButton from 'src/components/base/BaseButton.vue'

const events = [
  { icon: 'login', type: 'login', text: 'Sesion iniciada', time: 'Hace 2 horas' },
  { icon: 'security', type: 'audit', text: 'Auditoria creada', time: 'Hace 5 horas' },
  { icon: 'warning', type: 'alert', text: 'Alerta de inventario', time: 'Ayer' },
]
</script>

<style lang="scss" scoped>
.activity-card {
  &__title {
    font-family: 'Outfit', sans-serif;
    font-weight: 600;
    font-size: 1rem;
    color: #E2E8E4;
    margin: 0 0 1rem;
  }

  &__list {
    border: 1px solid rgba(113, 131, 127, 0.08);
    border-radius: 6px;
    overflow: hidden;
  }

  &__item {
    padding: 0.75rem 0.5rem;
  }

  &__icon-wrap {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(163, 120, 94, 0.08);
    color: #A3785E;

    &--login {
      background: rgba(163, 120, 94, 0.1);
      color: #A3785E;
    }

    &--audit {
      background: rgba(45, 90, 39, 0.12);
      color: #2D5A27;
    }

    &--alert {
      background: rgba(197, 160, 89, 0.1);
      color: #C5A059;
    }
  }

  &__label {
    font-size: 0.8125rem;
    color: #E2E8E4;
  }

  &__time {
    font-size: 0.6875rem;
    color: #8A9E99;
  }
}
</style>
