<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { setupService } from '../services/setup.service'

const router = useRouter()
const authStore = useAuthStore()
const $q = useQuasar()
const tenantId = authStore.user?.tenantId || ''

const industries = [
  { code: 'restaurante', name: 'Restaurante', icon: 'restaurant', desc: 'Comida, bebidas, insumos de cocina' },
  { code: 'bares', name: 'Bares y Cantinas', icon: 'local_bar', desc: 'Bebidas, cocteles, botanas' },
  { code: 'salon_belleza', name: 'Salón de Belleza', icon: 'content_cut', desc: 'Corte, color, tratamientos' },
  { code: 'ferreteria', name: 'Ferretería', icon: 'hardware', desc: 'Herramientas, materiales, ferretería' },
  { code: 'mini_super', name: 'Mini Super', icon: 'store', desc: 'Abarrotes, víveres, productos básicos' },
  { code: 'taller_mecanico', name: 'Taller Mecánico', icon: 'build', desc: 'Refacciones, servicio, mantenimiento' },
  { code: 'farmacia', name: 'Farmacia', icon: 'local_pharmacy', desc: 'Medicamentos, salud, higiene' },
  { code: 'default', name: 'General', icon: 'business', desc: 'Negocio general o multi-rubro' },
]

const selected = ref<string | null>(null)
const saving = ref(false)

async function confirm() {
  if (!selected.value || !tenantId) return
  saving.value = true
  try {
    await setupService.completeOnboarding(tenantId, selected.value)
    await authStore.fetchCurrentUser()
    $q.notify({ type: 'positive', message: '¡Configuración completada!' })
    void router.push('/dashboard')
  } catch {
    $q.notify({ type: 'negative', message: 'Error al guardar. Intenta de nuevo.' })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <q-page class="flex flex-center bg-forest-deep text-secondary">
    <div class="column items-center q-pa-xl" style="max-width: 700px; width: 100%">
      <div class="text-h4 text-primary text-center q-mb-xs" style="font-weight: 700">Configura tu empresa</div>
      <div class="text-body1 text-accent text-center q-mb-lg">Selecciona tu tipo de negocio para cargar plantillas automáticas</div>

      <div class="row q-col-gutter-sm full-width justify-center">
        <div
          v-for="ind in industries"
          :key="ind.code"
          class="col-5 col-sm-4"
        >
          <q-card
            dark
            clickable
            @click="selected = ind.code"
            class="industry-card cursor-pointer"
            :class="{ 'industry-card--selected': selected === ind.code }"
          >
            <q-card-section class="column items-center text-center q-pa-md">
              <q-icon :name="ind.icon" size="2rem" :color="selected === ind.code ? 'primary' : 'accent'" class="q-mb-sm" />
              <div class="text-body2 text-secondary" style="font-weight: 600">{{ ind.name }}</div>
              <div class="text-caption text-accent q-mt-xs">{{ ind.desc }}</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <q-btn
        label="Comenzar"
        color="primary"
        :disable="!selected"
        :loading="saving"
        @click="confirm"
        class="q-mt-lg"
        style="min-width: 200px"
      />
    </div>
  </q-page>
</template>

<style scoped>
.industry-card {
  background: rgba(19, 42, 26, 0.7);
  border: 1px solid transparent;
  border-radius: 12px;
  transition: all 0.2s ease;
}
.industry-card:hover {
  border-color: rgba(76, 175, 80, 0.3);
  transform: translateY(-2px);
}
.industry-card--selected {
  border-color: var(--q-primary);
  background: rgba(76, 175, 80, 0.1);
  box-shadow: 0 0 20px rgba(76, 175, 80, 0.15);
}
</style>
