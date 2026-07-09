<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar, useMeta } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'

useMeta({ title: 'Onboarding — PYMEQ' });
import { setupService } from '../services/setup.service'
import type { SetupInfo, SetupCategory } from '../types'
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

// ponytail: UI state for template preview dashboard
const activeTab = ref('summary')
const searchQuery = ref('')

function countCategories(cats: SetupCategory[]): number {
  return cats.reduce((acc, c) => acc + 1 + (c.children?.length ? countCategories(c.children) : 0), 0)
}

const totalCategories = computed(() => previewData.value ? countCategories(previewData.value.categories) : 0)

const filteredProducts = computed(() => {
  const list = previewData.value?.products || []
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return list
  return list.filter(p => p.name.toLowerCase().includes(q) || (p.categoryName || '').toLowerCase().includes(q))
})

const stats = computed(() => [
  { label: 'Productos', value: previewData.value?.products?.length || 0, icon: 'inventory_2' },
  { label: 'Categorías', value: totalCategories.value, icon: 'category' },
  { label: 'Unidades', value: previewData.value?.units?.length || 0, icon: 'scale' },
  { label: 'Ubicaciones', value: previewData.value?.locations?.length || 0, icon: 'place' },
])

const checklist = computed(() => [
  { title: 'Base de datos base', desc: `Se precargarán los productos y su estructura para la plantilla de ${industries.find(i => i.code === selected.value)?.name || 'General'}.` },
  { title: 'Taxonomía y Clasificación', desc: `Se configurarán ${totalCategories.value} categorías optimizadas.` },
  { title: 'Logística e Inventario', desc: `Configuración inicial de ubicaciones físicas y unidades de medida.` },
])

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

      <!-- Step 2: Preview (ponytail: interactive Quasar tabbed dashboard preview) -->
      <div v-if="step === 2 && previewData" class="preview-dashboard fade-in-up">
        <!-- Header -->
        <div class="dashboard-header">
          <div>
            <h2 class="dashboard-header__title">Plantilla de {{ industries.find(i => i.code === selected)?.name }}</h2>
            <p class="dashboard-header__subtitle">Inicialización inteligente de base de datos e inventarios</p>
          </div>
          <div class="dashboard-header__status">
            <span class="pulse-indicator" role="status" aria-label="Estado: listo"></span>
            Listo para cargar
          </div>
        </div>

        <!-- Dashboard Layout -->
        <div class="q-pa-md">
          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-3">
              <q-tabs
                v-model="activeTab"
                vertical
                dense
                align="left"
                class="text-accent text-weight-medium bg-dark rounded-borders q-pa-sm"
                active-color="primary"
                active-bg-color="transparent"
                indicator-color="primary"
              >
                <q-tab name="summary" icon="dashboard" label="Resumen" />
                <q-tab name="products" icon="inventory_2" label="Productos" />
                <q-tab name="categories" icon="category" label="Categorías" />
                <q-tab name="settings" icon="settings" label="Configuración" />
              </q-tabs>
            </div>

            <div class="col-12 col-md-9">
              <q-tab-panels
                v-model="activeTab"
                animated
                swipeable
                vertical
                transition-prev="jump-up"
                transition-next="jump-down"
                class="bg-dark text-secondary rounded-borders shadow-2 q-pa-md preview-panels"
              >
                <!-- Vista General -->
                <q-tab-panel name="summary" class="q-pa-none">
                  <div class="row q-col-gutter-sm q-mb-md">
                    <div class="col-6 col-sm-3" v-for="stat in stats" :key="stat.label">
                      <q-card flat bordered class="bg-surface-pine border-accent text-center q-pa-sm">
                        <q-icon :name="stat.icon" size="sm" color="primary" class="q-mb-xs" aria-hidden="true" />
                        <div class="text-h6 text-weight-bold">{{ stat.value }}</div>
                        <div class="text-caption text-accent">{{ stat.label }}</div>
                      </q-card>
                    </div>
                  </div>

                  <div class="text-subtitle2 text-weight-bold q-mb-sm text-primary">Procesos de inicialización</div>
                  <q-list dense>
                    <q-item v-for="item in checklist" :key="item.title" class="q-px-none">
                      <q-item-section avatar min-width="24px">
                        <q-icon name="check_circle" color="positive" size="xs" aria-hidden="true" />
                      </q-item-section>
                      <q-item-section>
                        <q-item-label class="text-weight-medium">{{ item.title }}</q-item-label>
                        <q-item-label caption class="text-accent">{{ item.desc }}</q-item-label>
                      </q-item-section>
                    </q-item>
                  </q-list>
                </q-tab-panel>

                <!-- Productos -->
                <q-tab-panel name="products" class="q-pa-none">
                  <div class="row q-col-gutter-sm items-center q-mb-md">
                    <div class="col text-subtitle2 text-weight-bold text-primary">Productos Base</div>
                    <div class="col-12 col-sm-6">
                      <q-input v-model="searchQuery" filled dense placeholder="Buscar..." color="primary">
                        <template v-slot:append>
                          <q-icon v-if="searchQuery" name="clear" class="cursor-pointer" @click="searchQuery = ''" />
                          <q-icon name="search" aria-hidden="true" />
                        </template>
                      </q-input>
                    </div>
                  </div>

                  <div class="scroll-container preview-scroll">
                    <q-list bordered separator v-if="filteredProducts.length">
                      <q-item v-for="p in filteredProducts" :key="p.name">
                        <q-item-section>
                          <q-item-label class="text-weight-medium">{{ p.name }}</q-item-label>
                          <q-item-label caption class="text-accent">{{ p.categoryName || 'Sin categoría' }}</q-item-label>
                        </q-item-section>
                        <q-item-section side>
                          <q-badge color="primary" outline>{{ p.baseUnit }}</q-badge>
                        </q-item-section>
                      </q-item>
                    </q-list>
                    <div v-else class="text-center q-pa-md text-accent">
                      No se encontraron productos para "{{ searchQuery }}"
                    </div>
                  </div>
                </q-tab-panel>

                <!-- Categorías -->
                <q-tab-panel name="categories" class="q-pa-none">
                  <div class="text-subtitle2 text-weight-bold q-mb-md text-primary">Estructura de Categorías</div>
                  <div class="scroll-container q-pa-xs preview-scroll--tall">
                    <CategoryTree :categories="previewData.categories" />
                  </div>
                </q-tab-panel>

                <!-- Configuración -->
                <q-tab-panel name="settings" class="q-pa-none">
                  <div class="row q-col-gutter-md">
                    <div class="col-12 col-sm-6">
                      <div class="text-subtitle2 text-weight-bold text-primary q-mb-sm">Ubicaciones</div>
                      <q-list bordered separator>
                        <q-item v-for="l in previewData.locations" :key="l.code">
                          <q-item-section avatar>
                            <q-icon name="place" color="accent" aria-hidden="true" />
                          </q-item-section>
                          <q-item-section>
                            <q-item-label class="text-weight-medium">{{ l.name }}</q-item-label>
                            <q-item-label caption class="text-accent">Código: {{ l.code }}</q-item-label>
                          </q-item-section>
                        </q-item>
                      </q-list>
                    </div>

                    <div class="col-12 col-sm-6">
                      <div class="text-subtitle2 text-weight-bold text-primary q-mb-sm">Unidades de Medida</div>
                      <q-list bordered separator>
                        <q-item v-for="u in previewData.units" :key="u.code">
                          <q-item-section avatar>
                            <q-icon name="scale" color="accent" aria-hidden="true" />
                          </q-item-section>
                          <q-item-section>
                            <q-item-label class="text-weight-medium">{{ u.name }}</q-item-label>
                            <q-item-label caption class="text-accent">Código: {{ u.code }}</q-item-label>
                          </q-item-section>
                        </q-item>
                      </q-list>
                    </div>
                  </div>
                </q-tab-panel>
              </q-tab-panels>
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
  transition: max-width 0.3s ease;

  &--preview {
    max-width: 1100px;
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

// ponytail: preview dashboard layout styles
.preview-dashboard {
  background: rgba(27, 38, 36, 0.6);
  border: 1px solid rgba(163, 120, 94, 0.12);
  border-radius: 12px;
  overflow: hidden;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid rgba(163, 120, 94, 0.08);

  &__title {
    font-family: 'Outfit', sans-serif;
    font-weight: 700;
    font-size: 1.25rem;
    color: #E2E8E4;
    margin: 0;
  }

  &__subtitle {
    font-size: 0.85rem;
    color: #8A9E99;
    margin: 0.1rem 0 0;
  }

  &__status {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.8rem;
    font-weight: 600;
    color: #E2E8E4;
    background: rgba(45, 90, 39, 0.2);
    border: 1px solid rgba(45, 90, 39, 0.3);
    padding: 0.25rem 0.6rem;
    border-radius: 9999px;
  }
}

.pulse-indicator {
  width: 6px;
  height: 6px;
  background-color: #2D5A27;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(45, 90, 39, 0.7);
  animation: pulse 1.6s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(45, 90, 39, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(45, 90, 39, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(45, 90, 39, 0);
  }
}

.border-accent {
  border-color: rgba(163, 120, 94, 0.15) !important;
}

.scroll-container {
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.1);
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(163, 120, 94, 0.3);
    border-radius: 3px;
  }
}

.loading-overlay {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.preview-panels {
  min-height: 350px;
}

.preview-scroll {
  max-height: 250px;
  overflow-y: auto;

  &--tall {
    max-height: 280px;
  }
}
</style>
