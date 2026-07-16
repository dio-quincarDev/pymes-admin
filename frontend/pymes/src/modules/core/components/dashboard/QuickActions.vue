<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const emit = defineEmits<{
  'nuevo-gasto': [];
  'ver-reportes': [];
  'exportar': [];
}>();

function handleNuevoGasto() {
  emit('nuevo-gasto');
  void router.push('/dashboard/gastos');
}

function handleVerReportes() {
  emit('ver-reportes');
  void router.push('/dashboard/accounting');
}

function handleExportar() {
  emit('exportar');
}
</script>

<template>
  <div class="quick-actions">
    <h3 class="quick-actions__title">Acciones Rápidas</h3>
    <div class="quick-actions__grid">
      <button
        class="quick-actions__btn"
        aria-label="Registrar nuevo gasto"
        @click="handleNuevoGasto"
      >
        <q-icon name="add_circle" size="18px" aria-hidden="true" />
        <span>Registrar gasto</span>
      </button>
      <button
        class="quick-actions__btn"
        aria-label="Ver reportes contables"
        @click="handleVerReportes"
      >
        <q-icon name="assessment" size="18px" aria-hidden="true" />
        <span>Ver reportes</span>
      </button>
      <button
        class="quick-actions__btn"
        aria-label="Exportar datos financieros"
        @click="handleExportar"
      >
        <q-icon name="download" size="18px" aria-hidden="true" />
        <span>Exportar datos</span>
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.quick-actions {
  display: flex;
  flex-direction: column;

  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin: 0 0 12px;
  }

  &__grid {
    display: flex;
    flex-direction: column;
    gap: 8px;

    @media (min-width: 769px) {
      flex-direction: column;
    }
  }

  &__btn {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    height: 44px;
    padding: 0 16px;
    background: var(--pq-surface);
    border: 1px solid var(--pq-border);
    border-radius: 6px;
    color: var(--pq-text);
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition:
      background var(--pq-motion-fast),
      transform var(--pq-motion-fast);

    &:hover {
      background: var(--pq-elevated);
    }

    &:focus-visible {
      outline: 2px solid var(--pq-accent);
      outline-offset: 2px;
    }

    &:active {
      transform: scale(0.97);
    }
  }
}
</style>
