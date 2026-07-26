<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { formatCurrency } from 'src/utils/format'
import { patrimonioService } from '../services/patrimonio.service'
import type { Patrimonio } from '../types'

useMeta({ title: 'Patrimonio — PYMEQ' })

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId

const data = ref<Patrimonio | null>(null)
const loading = ref(true)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await patrimonioService.get(tenantId)
    data.value = res.data
    form.value = {
      initialCapital: res.data.initialCapital,
      startDate: res.data.startDate || '',
      notes: res.data.notes || '',
    }
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar patrimonio' })
  } finally { loading.value = false }
}

const form = ref({ initialCapital: 0, startDate: '', notes: '' })
const editing = ref(false)

function toggleEdit() {
  if (!editing.value) {
    editing.value = true
  } else {
    void save()
  }
}

async function save() {
  saving.value = true
  try {
    await patrimonioService.update(tenantId, form.value)
    editing.value = false
    $q.notify({ type: 'positive', message: 'Patrimonio actualizado' })
    await load()
  } catch (err) { $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al guardar patrimonio' })
  } finally { saving.value = false }
}

onMounted(() => { if (tenantId) void load() })
</script>

<template>
  <q-page class="core-page">
    <div class="q-mb-lg fade-in-up">
      <h1 class="text-h4 font-bold q-ma-none patrimonio-title">Patrimonio</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Capital inicial, inversión y ROI</p>
    </div>

    <div v-if="loading" class="row q-col-gutter-md">
      <div v-for="i in 3" :key="i" class="col-12 col-sm-4">
        <div class="metric-skeleton skeleton" />
      </div>
    </div>

    <div v-else-if="!data" class="text-center q-py-xl fade-in-up">
      <div class="empty-state glass">
        <q-icon name="savings" size="56px" class="text-accent" />
        <p class="text-accent q-mt-md text-body1">Registra tu capital inicial para comenzar</p>
      </div>
    </div>

    <template v-else>
      <div class="row q-col-gutter-md q-mb-lg stagger-children">
        <div class="col-12 col-sm-4">
          <div class="metric-card metric-card--copper">
            <div class="metric-card__label">Capital Inicial</div>
            <div class="metric-card__value">{{ formatCurrency(data.initialCapital) }}</div>
            <div class="metric-card__bar" />
          </div>
        </div>
        <div class="col-12 col-sm-4">
          <div class="metric-card metric-card--sage">
            <div class="metric-card__label">Fecha de Inicio</div>
            <div class="metric-card__value metric-card__value--date">{{ data.startDate || '—' }}</div>
            <div class="metric-card__bar" />
          </div>
        </div>
        <div class="col-12 col-sm-4">
          <div class="metric-card metric-card--positive">
            <div class="metric-card__label">Estado</div>
            <div class="metric-card__value">
              <span class="status-badge">Activo</span>
            </div>
            <div class="metric-card__bar" />
          </div>
        </div>
      </div>

      <div class="fade-in-up" style="animation-delay: 0.25s">
        <div class="config-card glass">
          <div class="config-card__header">
            <div class="config-card__title">
              <q-icon name="tune" size="1.1rem" class="text-primary" />
              <span class="text-h6 text-primary">Configuración</span>
            </div>
            <q-btn
              :icon="editing ? 'save' : 'edit'"
              :color="editing ? 'positive' : 'primary'"
              :label="editing ? 'Guardar' : 'Editar'"
              :loading="saving"
              @click="toggleEdit"
              class="config-card__action"
              round dense
            />
          </div>
          <div class="config-card__body">
            <div class="row q-col-gutter-md">
              <div class="col-12 col-sm-6">
                <q-input dark filled :disable="!editing" v-model.number="form.initialCapital" label="Capital Inicial" type="number" min="0" step="0.01" prefix="$" />
              </div>
              <div class="col-12 col-sm-6">
                <q-input dark filled :disable="!editing" v-model="form.startDate" label="Fecha de Inicio" type="date" />
              </div>
              <div class="col-12">
                <q-input dark filled :disable="!editing" v-model="form.notes" label="Notas" type="textarea" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </q-page>
</template>

<style scoped lang="scss">
.patrimonio-title {
  font-family: 'Outfit', sans-serif;
  background: linear-gradient(135deg, #A3785E 0%, #C5A059 50%, #A3785E 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.metric-skeleton {
  height: 110px;
  border-radius: 8px;
}

.empty-state {
  border-radius: 12px;
  padding: 3rem 2rem;
  max-width: 360px;
  margin: 0 auto;
}

.metric-card {
  background: rgba(27, 38, 36, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(113, 131, 127, 0.05);
  border-radius: 8px;
  padding: 1.25rem 1.5rem;
  position: relative;
  overflow: hidden;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    border-radius: 0 2px 2px 0;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }

  &--copper::before { background: #A3785E; }
  &--sage::before { background: #8A9E99; }
  &--positive::before { background: #2D5A27; }

  &__label {
    font-size: 0.72rem;
    font-weight: 500;
    color: #8A9E99;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    margin-bottom: 0.5rem;
  }

  &__value {
    font-family: 'Outfit', sans-serif;
    font-size: 1.6rem;
    font-weight: 700;
    color: #E2E8E4;
    line-height: 1;

    &--date {
      font-size: 1.2rem;
      font-weight: 600;
      letter-spacing: 0.02em;
    }
  }

  &__bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, rgba(163, 120, 94, 0.15), transparent);
  }
}

.status-badge {
  display: inline-block;
  font-size: 0.8rem;
  font-weight: 600;
  color: #2D5A27;
  background: rgba(45, 90, 39, 0.12);
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  letter-spacing: 0.03em;
}

.config-card {
  border-radius: 10px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1.25rem 1.5rem;
    border-bottom: 1px solid rgba(113, 131, 127, 0.08);
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  &__body {
    padding: 1.5rem;
  }
}
</style>
