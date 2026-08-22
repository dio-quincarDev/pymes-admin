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
const tenantId = authStore.user?.tenantId

const industries = [
  { code: 'restaurante', name: 'Restaurante', icon: 'sym_r_restaurant', desc: 'Comida, bebidas, insumos de cocina' },
  { code: 'bares', name: 'Bares y Cantinas', icon: 'sym_r_local_bar', desc: 'Bebidas, cocteles, botanas' },
  { code: 'salon_belleza', name: 'Salon de Belleza', icon: 'sym_r_content_cut', desc: 'Corte, color, tratamientos' },
  { code: 'ferreteria', name: 'Ferreteria', icon: 'sym_r_hardware', desc: 'Herramientas, materiales' },
  { code: 'mini_super', name: 'Mini Super', icon: 'sym_r_store', desc: 'Abarrotes, vienes, productos basicos' },
  { code: 'taller_mecanico', name: 'Taller Mecanico', icon: 'sym_r_build', desc: 'Refacciones, servicio, mantenimiento' },
  { code: 'farmacia', name: 'Farmacia', icon: 'sym_r_local_pharmacy', desc: 'Medicamentos, salud, higiene' },
  { code: 'default', name: 'General', icon: 'sym_r_business', desc: 'Negocio general o multi-rubro' },
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
const fallbackIndustry = { code: '', name: '', icon: 'sym_r_business', desc: '' }
const selectedIndustry = computed(() => industries.find(i => i.code === selected.value) || fallbackIndustry)

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
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al cargar preview. Intenta de nuevo.' })
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
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al guardar. Intenta de nuevo.' })
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
      <div v-if="step === 1" class="industry-grid stagger-children" role="region" aria-label="Paso 1 de 2: Selección de industria">
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
      <div v-if="step === 2 && previewData" class="preview-card fade-in-up">
        <div class="preview-card__bar"></div>
        <div class="preview-card__body">
          <div class="preview-card__header">
            <q-icon :name="selectedIndustry.icon" size="1.5rem" style="color: var(--pq-accent)" aria-hidden="true" />
            <div>
              <div class="preview-card__title">Panel de {{ selectedIndustry.name }}</div>
              <p class="preview-card__subtitle">Datos precargados para empezar a operar</p>
            </div>
          </div>

          <div class="preview-metrics">
            <div class="preview-metric">
              <span class="preview-metric__value">{{ previewData.products.length }}</span>
              <span class="preview-metric__label">Productos</span>
            </div>
            <div class="preview-metric">
              <span class="preview-metric__value">{{ totalCategories }}</span>
              <span class="preview-metric__label">Categorías</span>
            </div>
            <div class="preview-metric">
              <span class="preview-metric__value">{{ previewData.units.length }}</span>
              <span class="preview-metric__label">Unidades</span>
            </div>
          </div>

          <div v-if="previewData.products.length > 0" class="preview-products-section">
            <div class="preview-products-section__title">Productos precargados</div>
            <div class="preview-products">
              <div v-for="p in previewData.products.slice(0, 6)" :key="p.id" class="preview-product">
                <div class="preview-product__name">{{ p.name }}</div>
                <div class="preview-product__meta">
                  <span class="preview-product__unit">{{ p.baseUnit }}</span>
                  <span class="preview-product__category">{{ p.categoryName }}</span>
                </div>
              </div>
            </div>
            <div v-if="previewData.products.length > 6" class="preview-products__more">
              +{{ previewData.products.length - 6 }} productos más
            </div>
          </div>
        </div>
      </div>

      <div class="onboarding-actions fade-in-up">
        <q-btn
          v-if="step === 2"
          flat
          color="accent"
          @click="goBack"
        >
          Volver
        </q-btn>
        <q-btn
          v-if="step === 2"
          color="primary"
          :loading="saving"
          @click="confirm"
        >
          Comenzar
        </q-btn>
      </div>

      <div v-if="loadingPreview" class="loading-overlay" role="status" aria-live="polite">
        <q-spinner-dots size="2rem" />
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
  padding: 32px 16px;
  background: var(--pq-background);
}

.onboarding-content {
  max-width: 900px;
  width: 100%;
  transition: max-width var(--pq-motion-base);

  &--preview {
    max-width: 640px;
  }
}

.onboarding-header {
  text-align: center;
  margin-bottom: 40px;
}

.onboarding-title {
  font-family: 'Geist', sans-serif;
  font-weight: 700;
  font-size: 32px;
  color: var(--pq-text);
  margin: 0 0 8px;
}

.onboarding-subtitle {
  font-family: 'Satoshi', sans-serif;
  font-size: 16px;
  color: var(--pq-text-muted);
  margin: 0;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: var(--pq-radius-full);
    background: rgba(200, 150, 62, 0.2);
    transition: background var(--pq-motion-base);

    &--active {
      background: var(--pq-accent);
    }
  }

  &__line {
    width: 48px;
    height: 2px;
    background: rgba(200, 150, 62, 0.15);
    transition: background var(--pq-motion-base);

    &--active {
      background: var(--pq-accent);
    }
  }
}

.industry-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  @media (min-width: 600px) {
    grid-template-columns: repeat(4, 1fr);
  }
}

// Preview card
.preview-card {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-xl);
  overflow: clip;

  &__bar {
    height: 4px;
    background: var(--pq-accent);
  }

  &__body {
    padding: 24px;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }

  &__title {
    font-family: 'Geist', sans-serif;
    font-weight: 700;
    font-size: 20px;
    color: var(--pq-text);
    line-height: 1.2;
  }

  &__subtitle {
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    color: var(--pq-text-muted);
    margin: 2px 0 0;
  }
}

// Metric cards
.preview-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  margin-bottom: 20px;

  @media (max-width: 500px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.preview-metric {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-lg);
  padding: 12px 8px;
  text-align: center;

  &__value {
    display: block;
    font-family: 'Geist Mono', monospace;
    font-size: 20px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--pq-text);
    line-height: 1;
    margin-bottom: 2px;
  }

  &__label {
    display: block;
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    color: var(--pq-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }
}

// Products section
.preview-products-section {
  border-top: 1px solid var(--pq-border);
  padding-top: 16px;

  &__title {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--pq-text-subtle);
    margin-bottom: 12px;
  }
}

.preview-products {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;

  @media (max-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 435px) {
    grid-template-columns: 1fr;
  }

  &__more {
    font-family: 'Satoshi', sans-serif;
    font-size: 12px;
    color: var(--pq-text-subtle);
    margin-top: 10px;
  }
}

.preview-product {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-md);
  padding: 8px;

  &__name {
    font-family: 'Satoshi', sans-serif;
    font-weight: 500;
    font-size: 13px;
    color: var(--pq-text);
    margin-bottom: 4px;
    line-height: 1.2;
  }

  &__meta {
    display: flex;
    flex-direction: column;
    gap: 1px;
  }

  &__unit {
    font-family: 'Geist Mono', monospace;
    font-size: 11px;
    color: var(--pq-text-muted);
  }

  &__category {
    font-family: 'Satoshi', sans-serif;
    font-size: 10px;
    font-weight: 500;
    color: var(--pq-text-subtle);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }
}

.onboarding-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.loading-overlay {
  display: flex;
  justify-content: center;
  padding: 32px;

  :deep(.q-spinner) {
    color: var(--pq-accent);
  }
}

// prefers-reduced-motion
@media (prefers-reduced-motion: reduce) {
  .onboarding-content {
    transition: none;
  }

  .step-indicator__dot,
  .step-indicator__line {
    transition: none;
  }

  .stagger-children > * {
    animation: none;
    opacity: 1;
  }

  .fade-in-up {
    animation: none;
    opacity: 1;
  }
}
</style>
