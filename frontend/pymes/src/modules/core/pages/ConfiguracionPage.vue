<template>
  <q-page class="core-page">
    <div class="q-mb-md fade-in-up">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Configuración</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Datos base del negocio</p>
    </div>

    <SkeletonLoader :is-loading="loading" shape="card">
      <div v-if="setup" class="row q-col-gutter-lg">
        <div class="col-12 col-md-6">
          <q-card dark class="bg-surface-pine">
            <q-card-section>
              <div class="text-h6 text-primary">Industria</div>
              <q-separator dark class="q-my-sm" />
              <div class="text-body1 text-secondary">{{ setup.industry || 'No definida' }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-12 col-md-6">
          <q-card dark class="bg-surface-pine">
            <q-card-section>
              <div class="text-h6 text-primary">Categorías</div>
              <q-separator dark class="q-my-sm" />
              <div v-if="setup.categories?.length" class="row q-col-gutter-xs">
                <div v-for="cat in setup.categories" :key="cat.code" class="col-6 col-md-4">
                  <q-chip dense dark color="accent" text-color="dark">{{ cat.name }}</q-chip>
                </div>
              </div>
              <div v-else class="text-accent text-caption">Sin categorías cargadas</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-12 col-md-6">
          <q-card dark class="bg-surface-pine">
            <q-card-section>
              <div class="text-h6 text-primary">Unidades</div>
              <q-separator dark class="q-my-sm" />
              <div v-if="setup.units?.length" class="row q-col-gutter-xs">
                <div v-for="unit in setup.units" :key="unit.code" class="col-6 col-md-4">
                  <q-chip dense dark color="accent" text-color="dark">{{ unit.name }}</q-chip>
                </div>
              </div>
              <div v-else class="text-accent text-caption">Sin unidades cargadas</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-12 col-md-6">
          <q-card dark class="bg-surface-pine">
            <q-card-section>
              <div class="text-h6 text-primary">Ubicaciones</div>
              <q-separator dark class="q-my-sm" />
              <div v-if="setup.locations?.length" class="row q-col-gutter-xs">
                <div v-for="loc in setup.locations" :key="loc.code" class="col-12 col-md-6">
                  <q-chip dense dark color="accent" text-color="dark">{{ loc.name }}</q-chip>
                </div>
              </div>
              <div v-else class="text-accent text-caption">Sin ubicaciones cargadas</div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </SkeletonLoader>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { api } from 'src/boot/axios'
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue'
import type { SetupInfo } from '../types'

const $q = useQuasar()
const authStore = useAuthStore()
const loading = ref(true)
const setup = ref<SetupInfo | null>(null)

const tenantId = authStore.user?.tenantId

async function loadSetup() {
  if (!tenantId) return
  loading.value = true
  try {
    const res = await api.get<SetupInfo>(`/core/setup/${tenantId}`)
    setup.value = res.data
  } catch {
    $q.notify({ type: 'negative', message: 'Error al cargar configuración' })
  } finally {
    loading.value = false
  }
}

onMounted(loadSetup)
</script>
