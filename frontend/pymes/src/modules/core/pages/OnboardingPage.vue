<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { setupService } from '../services/setup.service'
import type { SetupInfo } from '../types'
import IndustryCard from 'src/components/onboarding/IndustryCard.vue'
import CategoryTree from 'src/components/onboarding/CategoryTree.vue'

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
    await authStore.fetchCurrentUser()
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
    <div class="onboarding-content">
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
          {{ step === 1 ? 'Configura tu empresa' : 'Preview de categorias' }}
        </h1>
        <p class="onboarding-subtitle">
          {{ step === 1
            ? 'Selecciona tu tipo de negocio para cargar plantillas automaticas'
            : 'Estas categorias se cargaran para tu industria'
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

      <!-- Step 2: Preview -->
      <div v-if="step === 2 && previewData" class="preview-section fade-in-up">
        <div class="preview-card">
          <div class="preview-card__header">
            <q-icon :name="industries.find(i => i.code === selected)?.icon || 'business'" size="1.5rem" color="primary" />
            <span class="preview-card__industry">
              {{ industries.find(i => i.code === selected)?.name }}
            </span>
          </div>

          <div class="preview-card__body">
            <div class="preview-group" v-if="previewData.categories.length">
              <div class="preview-group__title">
                <q-icon name="category" size="1rem" color="primary" />
                Categorias
              </div>
              <CategoryTree :categories="previewData.categories" />
            </div>

            <div class="preview-group" v-if="previewData.units.length">
              <div class="preview-group__title">
                <q-icon name="scale" size="1rem" color="primary" />
                Unidades
              </div>
              <div class="preview-list">
                <span v-for="u in previewData.units" :key="u.code" class="preview-chip">
                  {{ u.name }}
                </span>
              </div>
            </div>

            <div class="preview-group" v-if="previewData.locations.length">
              <div class="preview-group__title">
                <q-icon name="place" size="1rem" color="primary" />
                Ubicaciones
              </div>
              <div class="preview-list">
                <span v-for="l in previewData.locations" :key="l.code" class="preview-chip">
                  {{ l.name }}
                </span>
              </div>
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
          class="confirm-btn"
        >
          <span class="confirm-btn__label">Comenzar</span>
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

.preview-section {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 0.5rem;
}

.preview-card {
  background: rgba(27, 38, 36, 0.6);
  border: 1px solid rgba(163, 120, 94, 0.12);
  border-radius: 12px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 1rem 1.25rem;
    border-bottom: 1px solid rgba(163, 120, 94, 0.08);
  }

  &__industry {
    font-family: 'Outfit', sans-serif;
    font-weight: 600;
    font-size: 1rem;
    color: #E2E8E4;
  }

  &__body {
    padding: 1rem 1.25rem;
  }
}

.preview-group {
  margin-bottom: 1.25rem;

  &:last-child {
    margin-bottom: 0;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-family: 'Outfit', sans-serif;
    font-weight: 600;
    font-size: 0.8rem;
    color: #8A9E99;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 0.5rem;
  }
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.preview-chip {
  font-size: 0.78rem;
  color: #E2E8E4;
  background: rgba(163, 120, 94, 0.08);
  border: 1px solid rgba(163, 120, 94, 0.1);
  border-radius: 6px;
  padding: 0.25rem 0.6rem;
}

.onboarding-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 2.5rem;
}

.action-btn {
  min-width: 120px;
  border-radius: 8px;
  font-family: 'Outfit', sans-serif;
  font-weight: 600;
}

.confirm-btn {
  min-width: 200px;
  border-radius: 8px;
  font-family: 'Outfit', sans-serif;
  font-weight: 600;
  letter-spacing: 0.02em;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover:not(:disabled) {
    box-shadow: 0 0 20px rgba(163, 120, 94, 0.3);
  }

  &__label {
    font-size: 0.95rem;
  }
}

.loading-overlay {
  display: flex;
  justify-content: center;
  padding: 2rem;
}
</style>
