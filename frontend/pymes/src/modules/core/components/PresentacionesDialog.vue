<script setup lang="ts">
import { ref, shallowRef, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/modules/auth/store'
import { productoService } from '../services/producto.service'
import type { Producto, Presentacion, PresentacionRequest } from '../types'

interface Props {
  modelValue: boolean
  product: Producto | null
  unitLabel: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'updated': []
}>()

const $q = useQuasar()
const authStore = useAuthStore()
const tenantId = authStore.user?.tenantId || ''

const presItems = ref<Presentacion[]>([])
const presForm = ref<PresentacionRequest>({ name: '', conversion: 1 })
const baseUnitLabel = computed(() => props.unitLabel)
const addingPres = shallowRef(false)
const removingPres = shallowRef(false)

const conversionPreview = computed(() => {
  const name = presForm.value.name || '—'
  const conv = presForm.value.conversion
  const unit = baseUnitLabel.value
  return conv > 1 ? `1 ${name} = ${conv} ${unit}` : ''
})

watch(() => props.modelValue, (val) => {
  if (val && props.product) {
    presItems.value = [...(props.product.presentaciones || [])]
    presForm.value = { name: '', conversion: 1 }
  }
})

async function addPresentation() {
  if (!presForm.value.name || !props.product) return
  addingPres.value = true
  try {
    const res = await productoService.addPresentation(props.product.id, presForm.value, tenantId)
    presItems.value.push(res.data)
    presForm.value = { name: '', conversion: 1 }
    emit('updated')
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al agregar presentación' })
  } finally {
    addingPres.value = false
  }
}

async function removePresentation(p: Presentacion) {
  removingPres.value = true
  try {
    await productoService.removePresentation(p.id, tenantId)
    presItems.value = presItems.value.filter(x => x.id !== p.id)
    emit('updated')
  } catch (err) {
    $q.notify({ type: 'negative', message: err instanceof Error ? err.message : 'Error al eliminar presentación' })
  } finally {
    removingPres.value = false
  }
}
</script>

<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" dark>
    <q-card dark class="bg-surface-pine pres-dialog">
      <div class="pres-dialog__header">
        <q-icon name="inventory_2" size="1.3rem" class="text-primary" />
        <div class="pres-dialog__header-text">
          <div class="text-h6 text-primary q-ma-none">Presentaciones</div>
          <div class="pres-dialog__product-name">{{ product?.name }}</div>
        </div>
      </div>
      <q-separator dark class="pres-dialog__sep" />

      <q-card-section class="pres-dialog__list">
        <TransitionGroup name="pres-list" tag="div" class="pres-list">
          <div v-if="!presItems.length" key="empty" class="pres-empty">
            <q-icon name="inventory" size="2rem" class="pres-empty__icon" />
            <span>Sin presentaciones</span>
          </div>
          <div v-for="p in presItems" :key="p.id" class="pres-row">
            <div class="pres-row__info">
              <span class="pres-row__name">{{ p.name }}</span>
              <span class="pres-row__conv">
                <q-icon name="close" size="0.7rem" />
                {{ p.conversion }}
              </span>
            </div>
            <q-btn
              flat dense round icon="delete_outline" color="negative" size="sm"
              @click="removePresentation(p)" :disable="removingPres"
              aria-label="Eliminar presentación"
              class="pres-row__remove"
            />
          </div>
        </TransitionGroup>
      </q-card-section>

      <q-separator dark class="pres-dialog__sep" />

      <q-card-section class="pres-dialog__form">
        <div class="pres-form-title">Agregar presentación</div>
        <div class="row q-col-gutter-sm items-start">
          <div class="col-xs-12 col-sm-5">
            <q-input dark dense outlined v-model="presForm.name" label="Nombre" placeholder="Ej: Caja x24" class="pres-input" />
          </div>
          <div class="col-xs-12 col-sm-4">
            <q-input dark dense outlined v-model.number="presForm.conversion" label="Conversión" type="text" inputmode="numeric" class="pres-input" />
            <div class="pres-hint">
              Unidades base que caben en esta presentación
            </div>
          </div>
          <div class="col-xs-12 col-sm-3">
            <q-btn
              label="Agregar" color="primary" no-caps
              :loading="addingPres" @click="addPresentation"
              icon="add"
              class="pres-add-btn"
            />
          </div>
        </div>
        <Transition name="preview-fade">
          <div v-if="conversionPreview" class="pres-preview">
            <q-icon name="swap_vert" size="0.9rem" />
            <span>{{ conversionPreview }}</span>
          </div>
        </Transition>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.pres-dialog {
  width: 90vw;
  max-width: 480px;
  border-radius: 14px;
  overflow: hidden;
}

.pres-dialog__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px 12px;
}

.pres-dialog__header-text {
  display: flex;
  flex-direction: column;
}

.pres-dialog__product-name {
  font-size: 0.78rem;
  color: rgba(163, 120, 94, 0.5);
}

.pres-dialog__sep {
  opacity: 0.2;
}

.pres-dialog__list {
  padding: 12px 20px;
}

.pres-dialog__form {
  padding: 12px 20px 16px;
}

.pres-form-title {
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgba(163, 120, 94, 0.6);
  margin-bottom: 10px;
}

.pres-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pres-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 0;
  color: rgba(163, 120, 94, 0.35);
  font-size: 0.8rem;
}

.pres-empty__icon {
  opacity: 0.4;
}

.pres-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(27, 38, 36, 0.5);
  border: 1px solid rgba(113, 131, 127, 0.08);
  border-radius: 8px;
  transition: border-color 0.15s ease;
}

.pres-row:hover {
  border-color: rgba(163, 120, 94, 0.15);
}

.pres-row__info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pres-row__name {
  font-size: 0.85rem;
  font-weight: 500;
}

.pres-row__conv {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.72rem;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  color: rgba(163, 120, 94, 0.5);
  background: rgba(163, 120, 94, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
}

.pres-row__remove {
  opacity: 0.3;
  transition: opacity 0.15s ease;
}

.pres-row__remove:hover {
  opacity: 1;
}

.pres-list-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.pres-list-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.pres-list-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.pres-list-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

.pres-preview {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  padding: 5px 10px;
  font-size: 0.75rem;
  font-weight: 500;
  color: rgba(34, 211, 238, 0.8);
  background: rgba(34, 211, 238, 0.08);
  border: 1px solid rgba(34, 211, 238, 0.15);
  border-radius: 6px;
}

.pres-hint {
  font-size: 0.7rem;
  color: rgba(163, 120, 94, 0.45);
  line-height: 1.4;
  margin-top: 4px;
}

.pres-input :deep(.q-field__control) {
  border-radius: 8px !important;
}

.pres-add-btn {
  border-radius: 8px;
  font-weight: 600;
}

.preview-fade-enter-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.preview-fade-leave-active {
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.preview-fade-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}

.preview-fade-leave-to {
  opacity: 0;
}
</style>
