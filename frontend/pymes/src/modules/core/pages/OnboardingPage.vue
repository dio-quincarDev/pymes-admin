<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'

useMeta({ title: 'Onboarding — PYMEQ' });
import { setupService } from '../services/setup.service'
import type { SetupInfo, SetupCategory } from '../types'
import IndustryCard from 'src/components/onboarding/IndustryCard.vue'


const router = useRouter()
const authStore = useAuthStore()
const $q = useQuasar()
const tenantId = authStore.user?.tenantId || ''

const industries = [
  { code: 'restaurante', name: 'Restaurante', icon: 'restaurant', desc: 'Comida, bebidas, insumos de cocina' },
  { code: 'bares', name: 'Bares y Cantinas', icon: 'local_bar', desc: 'Bebidas, cocteles, botanas' },
  { code: 'salon_belleza', name: 'Salon de Belleza', icon: 'content_cut', desc: 'Corte, color, tratamientos' },
  { code: 'ferreteria', name: 'Ferreteria', icon: 'hardware', desc: 'Herramientas, materiales' },
  { code: 'mini_super', name: 'Mini Super', icon: 'store', desc: 'Abarrotes, vienes, productos basicos' },
  { code: 'taller_mecanico', name: 'Taller Mecanico', icon: 'build', desc: 'Refacciones, servicio, mantenimiento' },
  { code: 'farmacia', name: 'Farmacia', icon: 'local_pharmacy', desc: 'Medicamentos, salud, higiene' },
  { code: 'default', name: 'General', icon: 'business', desc: 'Negocio general o multi-rubro' },
]

const step = ref(1)
const selected = ref<string | null>(null)
const previewData = ref<SetupInfo | null>(null)
const loadingPreview = ref(false)
const saving = ref(false)

function countCategories(cats: SetupCategory[]): number {
  return cats.reduce((acc, c) => acc + 1 + (c.children?.length ? countCategories(c.children) : 0), 0)
}

const totalCategories = computed(() => previewData.value ? countCategories(previewData.value.categories) : 0)

function onSelect(code: string) {
  selected.value = code
  void loadPreview(code)
}

async function loadPreview(industry: string) {
  loadingPreview.value = true
  try {
    const { data } = await setupService.preview(industry)
    previewData.value = data
    step.value = 2
  } catch {
    $q.notify({ type: 'negative', message: 'Error al cargar preview. Intenta de nuevo.' })
  } finally {
    loadingPreview.value = false
  }
}

function goBack() {
  step.value = 1
  previewData.value = null
}

async function confirm() {
  if (!selected.value || !tenantId) return
  saving.value = true
  try {
    await setupService.completeOnboarding(tenantId, selected.value)
    $q.notify({ type: 'positive', message: 'Configuracion completada' })
    void router.push('/dashboard')
  } catch {
    $q.notify({ type: 'negative', message: 'Error al guardar. Intenta de nuevo.' })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="onboarding-page">
    <div class="onboarding-content" :class="{ 'onboarding-content--preview': step === 2 }">
      <div class="onboarding-header fade-in-up">
        <div class="step-indicator">
          <span
            class="step-indicator__dot"
            :class="{ 'step-indicator__dot--active': step >= 1 }"
          />
          <span class="step-indicator__line" :class="{ 'step-indicator__line--active': step >= 2 }" />
          <span
            class="step-indicator__dot"
            :class="{ 'step-indicator__dot--active': step >= 2 }"
          />
        </div>
        <h1 class="onboarding-title">
          {{ step === 1 ? 'Configura tu empresa' : 'Preview de tu plantilla' }}
        </h1>
        <p class="onboarding-subtitle">
          {{ step === 1
            ? 'Selecciona tu tipo de negocio para cargar plantillas automaticas'
            : 'Estos datos se precargaran para tu industria'
          }}
        </p>
      </div>

      <!-- Step 1: Industry selection -->
      <div v-if="step === 1" class="industry-grid stagger-children">
        <IndustryCard
          v-for="ind in industries"
          :key="ind.code"
          :code="ind.code"
          :name="ind.name"
          :icon="ind.icon"
          :desc="ind.desc"
          :selected="selected === ind.code"
          @select="onSelect"
        />
      </div>

      <!-- Step 2: Preview (compact confirmation card) -->
      <div v-if="step === 2 && previewData" class="preview-card fade-in-up">
        <div class="preview-card__body">
          <div class="preview-card__title">
            <h2>Plantilla de {{ industries.find(i => i.code === selected)?.name }}</h2>
            <p>Se precargarán los siguientes datos:</p>
          </div>

          <div class="preview-card__stats">
            <div class="stat-chip">
              <q-icon name="inventory_2" size="sm" color="primary" />
              <span class="stat-chip__value">{{ previewData.products.length }}</span>
              <span class="stat-chip__label">productos</span>
            </div>
            <div class="stat-chip">
              <q-icon name="category" size="sm" color="primary" />
              <span class="stat-chip__value">{{ totalCategories }}</span>
              <span class="stat-chip__label">categorías</span>
            </div>
            <div class="stat-chip">
              <q-icon name="scale" size="sm" color="primary" />
              <span class="stat-chip__value">{{ previewData.units.length }}</span>
              <span class="stat-chip__label">unidades</span>
            </div>
            <div class="stat-chip">
              <q-icon name="place" size="sm" color="primary" />
              <span class="stat-chip__value">{{ previewData.locations.length }}</span>
              <span class="stat-chip__label">ubicaciones</span>
            </div>
          </div>

          <div class="preview-card__checklist">
            <div class="check-item">
              <q-icon name="check_circle" color="positive" size="xs" />
              <span>Base de datos con productos y estructura de {{ industries.find(i => i.code === selected)?.name || 'General' }}</span>
            </div>
            <div class="check-item">
              <q-icon name="check_circle" color="positive" size="xs" />
              <span>{{ totalCategories }} categorías optimizadas para tu industria</span>
            </div>
            <div class="check-item">
              <q-icon name="check_circle" color="positive" size="xs" />
              <span>Ubicaciones físicas y unidades de medida preconfiguradas</span>
            </div>
          </div>
        </div>
      </div>

      <div class="onboarding-actions fade-in-up" style="animation-delay: 0.6s">
        <q-btn
          v-if="step === 2"
          flat
          color="accent"
          label="Volver"
          icon="arrow_back"
          @click="goBack"
          class="action-btn"
        />
        <q-btn
          v-if="step === 2"
          color="primary"
          size="lg"
          :loading="saving"
          @click="confirm"
        >
          Comenzar
          <q-icon v-if="!saving" name="arrow_forward" size="1.2rem" class="q-ml-sm" />
        </q-btn>
      </div>

      <div v-if="loadingPreview" class="loading-overlay">
        <q-spinner-dots size="2rem" color="primary" />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.onboarding-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
}

.onboarding-content {
  max-width: 900px;
  width: 100%;
  transition: max-width 0.3s ease;

  &--preview {
    max-width: 600px;
  }
}

.onboarding-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.onboarding-title {
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 2rem;
  color: #E2E8E4;
  margin: 0 0 0.5rem;
}

.onboarding-subtitle {
  font-size: 1rem;
  color: #8A9E99;
  margin: 0;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 1.5rem;

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: rgba(163, 120, 94, 0.2);
    transition: background 0.3s ease;

    &--active {
      background: #A3785E;
      box-shadow: 0 0 8px rgba(163, 120, 94, 0.4);
    }
  }

  &__line {
    width: 48px;
    height: 2px;
    background: rgba(163, 120, 94, 0.15);
    transition: background 0.3s ease;

    &--active {
      background: #A3785E;
    }
  }
}

.industry-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;

  @media (min-width: 600px) {
    grid-template-columns: repeat(4, 1fr);
  }
}

// Preview card (compact confirmation)
.preview-card {
  background: rgba(27, 38, 36, 0.6);
  border: 1px solid rgba(163, 120, 94, 0.12);
  border-radius: 12px;

  &__body {
    padding: 2rem;
  }

  &__title {
    margin-bottom: 1.5rem;

    h2 {
      font-family: 'Outfit', sans-serif;
      font-weight: 700;
      font-size: 1.25rem;
      color: #E2E8E4;
      margin: 0 0 0.25rem;
    }

    p {
      color: #8A9E99;
      font-size: 0.9rem;
      margin: 0;
    }
  }

  &__stats {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
  }

  &__checklist {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(163, 120, 94, 0.08);
  border: 1px solid rgba(163, 120, 94, 0.15);
  border-radius: 9999px;
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;

  &__value {
    font-weight: 700;
    color: #E2E8E4;
  }

  &__label {
    color: #8A9E99;
  }
}

.check-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #E2E8E4;
  line-height: 1.4;
}

.loading-overlay {
  display: flex;
  justify-content: center;
  padding: 2rem;
}
</style>
