<script setup lang="ts">
import { computed } from 'vue';
import { formatCurrency } from 'src/utils/format';
import { calcularPlanRecuperacion } from '../../utils/recuperacion';
import type { MetricasFinancieras } from '../../types';

interface Props {
  capital: number;
  metricas: MetricasFinancieras | null;
}

const props = defineProps<Props>();

// ponytail: mes con puros ceros = mes sin movimientos = "sin datos", no protege-caja.
const plan = computed(() => {
  const m = props.metricas;
  if (!m || m.totalIngresos + m.totalGastos === 0) {
    return calcularPlanRecuperacion(props.capital, null, null);
  }
  return calcularPlanRecuperacion(props.capital, m.margenNeto, m.margenNetoPct);
});
</script>

<template>
  <div class="recup-card glass">
    <div class="recup-card__header">
      <q-icon name="savings" size="1.1rem" class="text-primary" />
      <span class="text-h6 text-primary">Cuánto apartar este mes</span>
    </div>

    <p v-if="plan.estado === 'sin-datos'" class="recup-card__note">
      Todavía no hay números de este mes.
    </p>
    <p v-else-if="plan.estado === 'protege-caja'" class="recup-card__note">
      Este mes protege la caja: no apartes nada.
    </p>

    <ul v-if="plan.filas.length" class="recup-card__list">
      <li
        v-for="f in plan.filas"
        :key="f.plazo"
        class="recup-card__row"
        :class="{ 'recup-card__row--star': f.calificado }"
      >
        <span class="recup-card__plazo">
          <template v-if="f.calificado">★ </template>{{ f.plazo }} meses
        </span>
        <span class="recup-card__aporte">{{ formatCurrency(f.aporte) }} al mes</span>
        <span class="recup-card__flag" :class="{ 'recup-card__flag--no': !f.alcanza }">
          {{ f.alcanza ? 'te alcanza' : 'no te alcanza' }}
        </span>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.recup-card {
  border-radius: 10px;
  padding: 1.25rem 1.5rem;

  &__header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.75rem;
  }

  &__note {
    color: #8a9e99;
    margin: 0 0 0.5rem;
  }

  &__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.6rem 0.75rem;
    border: 1px solid rgba(113, 131, 127, 0.12);
    border-radius: 8px;

    &--star {
      border-color: #c5a059;
      background: rgba(197, 160, 89, 0.08);
    }
  }

  &__plazo {
    font-weight: 700;
    color: #e2e8e4;
    min-width: 7rem;
  }

  .recup-card__row--star &__plazo {
    color: #c5a059;
  }

  &__aporte {
    flex: 1;
    color: #e2e8e4;
  }

  &__flag {
    font-size: 0.8rem;
    color: #2d5a27;

    &--no {
      color: #8a9e99;
    }
  }
}

@media (max-width: 599px) {
  .recup-card__row {
    display: grid;
    grid-template-columns: 1fr;
    gap: 0.15rem;
  }
}
</style>
