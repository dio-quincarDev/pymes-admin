<template>
  <BaseCard class="action-card q-pa-xl">
    <div class="action-card__inner">
      <div class="action-card__icon-wrap">
        <q-icon name="analytics" size="5rem" color="primary" class="action-card__icon" />
      </div>
      <div class="action-card__text">
        <h2 class="action-card__title">Inicia un nuevo analisis</h2>
        <p class="action-card__desc">
          Selecciona una categoria para comenzar a auditar los movimientos de tu empresa.
        </p>
        <BaseButton size="lg" class="action-card__btn">COMENZAR AUDITORIA</BaseButton>
      </div>
    </div>
  </BaseCard>
</template>

<script setup lang="ts">
import BaseCard from 'src/components/base/BaseCard.vue'
import BaseButton from 'src/components/base/BaseButton.vue'
</script>

<style lang="scss" scoped>
.action-card {
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(
      ellipse at 20% 50%,
      rgba(163, 120, 94, 0.06) 0%,
      transparent 60%
    );
    pointer-events: none;
  }

  &__inner {
    display: flex;
    align-items: center;
    gap: 2rem;
    position: relative;
    z-index: 1;

    @media (max-width: 599px) {
      flex-direction: column;
      text-align: center;
    }
  }

  &__icon-wrap {
    flex-shrink: 0;
  }

  &__icon {
    opacity: 0.25;
    animation: breathe 4s ease-in-out infinite;
  }

  &__text {
    flex: 1;
  }

  &__title {
    font-family: 'Outfit', sans-serif;
    font-weight: 700;
    font-size: 1.25rem;
    color: #E2E8E4;
    margin: 0 0 0.5rem;
  }

  &__desc {
    font-size: 0.875rem;
    color: #8A9E99;
    margin: 0 0 1.25rem;
    line-height: 1.5;
  }

  &__btn {
    @media (max-width: 599px) {
      width: 100%;
    }
  }
}

@keyframes breathe {
  0%, 100% { opacity: 0.2; transform: scale(1); }
  50% { opacity: 0.35; transform: scale(1.05); }
}
</style>
