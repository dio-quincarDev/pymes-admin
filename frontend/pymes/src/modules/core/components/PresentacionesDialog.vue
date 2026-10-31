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
const tenantId = authStore.user?.tenantId

const presItems = ref<Presentacion[]>([])
const presForm = ref<PresentacionRequest>({ name: '', conversion: 1 })
const baseUnitLabel = computed(() => props.unitLabel)
const addingPres = shallowRef(false)
const removingPres = shallowRef(false)

const conversionPreview = computed(() => {
  const name = presForm.value.name || '—'
  const conv = presForm.value.conversion
  const unit = baseUnitLabel.value
  return conv > 1 ? { ok: true, text: `1 ${name} = ${conv} ${unit}` }
    : { ok: false, text: `El empaque debe traer más de 1 ${unit}` }
})

const canAdd = computed(() => !!presForm.value.name.trim() && presForm.value.conversion > 1 && !addingPres.value)

watch(() => props.modelValue, (val) => {
  if (val && props.product) {
    presItems.value = [...(props.product.presentaciones || [])]
    presForm.value = { name: '', conversion: 1 }
  }
})

async function addPresentation() {
  if (!canAdd.value || !props.product || !tenantId) return
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
  if (!tenantId) return
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
    <q-card dark class="dialog-surface pres-dialog">
      <div class="pres-dialog__header">
        <q-icon name="inventory_2" size="1.25rem" style="color: var(--pq-accent)" />
        <div class="pres-dialog__header-text">
          <div class="pres-dialog__title">Presentaciones</div>
          <div class="pres-dialog__product-name">{{ product?.name }}</div>
        </div>
        <q-space />
        <q-btn flat round icon="sym_r_close" color="accent" size="sm" v-close-popup aria-label="Cerrar" />
      </div>
      <div class="rule" />

      <q-card-section class="pres-dialog__list">
        <TransitionGroup name="pres-list" tag="div" class="pres-list">
          <div v-if="!presItems.length" key="empty" class="pres-empty">
            <q-icon name="inventory_2" size="2rem" class="pres-empty__icon" />
            <span>Sin presentaciones</span>
            <span class="pres-empty__hint">Agrega la primera presentación abajo.</span>
          </div>
          <div v-for="p in presItems" :key="p.id" class="pres-row">
            <div class="pres-row__accent" aria-hidden="true" />
            <div class="pres-row__info">
              <span class="pres-row__name">{{ p.name }}</span>
              <span class="pres-row__conv">× {{ p.conversion }}</span>
            </div>
            <q-btn
              flat dense round icon="sym_r_delete" color="negative" size="sm"
              @click="removePresentation(p)" :disable="removingPres"
              aria-label="Eliminar presentación"
              class="pres-row__remove"
            />
          </div>
        </TransitionGroup>
      </q-card-section>

      <div class="rule" />

      <q-card-section class="pres-dialog__form">
        <div class="pres-form-title">Agregar presentación</div>
        <div class="pres-form-fields">
          <q-input dark dense outlined v-model="presForm.name" label="Nombre" placeholder="Ej: Caja x24" class="pres-input" />
          <q-input dark dense outlined v-model.number="presForm.conversion" label="Conversión" type="number" :min="2" inputmode="numeric" class="pres-input" />
          <div class="pres-hint">Unidades base que caben en esta presentación</div>
        </div>
        <div class="row items-center q-col-gutter-sm pres-form-actions">
          <div class="col">
            <div class="pres-preview" :class="{ 'pres-preview--warn': !conversionPreview.ok }">
              <q-icon name="swap_vert" size="0.9rem" />
              <span>{{ conversionPreview.text }}</span>
            </div>
          </div>
          <div class="col-auto">
            <q-btn
              color="primary" no-caps label="Agregar"
              :loading="addingPres" :disable="!canAdd" @click="addPresentation"
              class="pres-add-btn"
            />
          </div>
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.pres-dialog {
  background: var(--pq-elevated) !important;
  border-radius: var(--pq-radius-xl) !important;
  box-shadow: var(--pq-shadow-lg) !important;
  border: 1px solid rgba(77, 112, 99, 0.3);
  width: 90vw;
  max-width: 480px;
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
  gap: 2px;
}

.pres-dialog__title {
  font-family: var(--pq-font-display);
  font-weight: 600;
  font-size: 16px;
  color: var(--pq-text);
}

.pres-dialog__product-name {
  font-family: var(--pq-font-body);
  font-size: 13px;
  color: var(--pq-text-muted);
}

.rule {
  height: 1px;
  background: var(--pq-border);
  margin: 0;
  opacity: 0.4;
}

.pres-dialog__list {
  padding: 12px 20px;
  max-height: 240px;
  overflow-y: auto;
}

.pres-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pres-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px 0;
  color: var(--pq-text-muted);
  font-family: var(--pq-font-body);
  font-size: 13px;
  text-align: center;
}

.pres-empty__icon {
  opacity: 0.4;
  color: var(--pq-text-subtle);
}

.pres-empty__hint {
  font-size: 12px;
  color: var(--pq-text-subtle);
}

.pres-row {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px 8px 14px;
  background: var(--pq-surface);
  border: 1px solid rgba(77, 112, 99, 0.3);
  border-left: 2px solid var(--pq-accent-muted);
  border-radius: var(--pq-radius-md);
  transition: border-color 0.16s cubic-bezier(0.22, 1, 0.36, 1), border-left-color 0.16s ease, background 0.16s ease, transform 0.16s ease;
  overflow: hidden;
}

.pres-row:hover {
  border-color: rgba(77, 112, 99, 0.5);
  border-left-color: var(--pq-accent);
  background: rgba(163, 120, 94, 0.04);
  transform: translateX(2px);
}

.pres-row__accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--pq-accent), transparent 60%);
  opacity: 0;
  transition: opacity 0.16s ease;
}

.pres-row:hover .pres-row__accent {
  opacity: 1;
}

.pres-row__info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pres-row__name {
  font-family: var(--pq-font-body);
  font-weight: 500;
  font-size: 14px;
  color: var(--pq-text);
}

.pres-row__conv {
  display: inline-flex;
  align-items: center;
  font-family: var(--pq-font-utility);
  font-size: 12px;
  font-weight: 500;
  color: var(--pq-accent);
  background: rgba(163, 120, 94, 0.1);
  border: 1px solid rgba(163, 120, 94, 0.2);
  padding: 2px 8px;
  border-radius: var(--pq-radius-full);
  font-variant-numeric: tabular-nums;
}

.pres-row__remove {
  opacity: 0.4;
  transition: opacity 0.16s ease, background-color 0.16s ease, transform 0.1s ease;
  border-radius: var(--pq-radius-sm);
}

.pres-row__remove:hover {
  opacity: 1;
  background-color: rgba(192, 106, 96, 0.1);
  transform: scale(1.05);
}

.pres-row__remove:active {
  transform: scale(0.95);
}

.pres-row__remove:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}

.pres-dialog__form {
  padding: 12px 20px 16px;
}

.pres-form-title {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
  margin-bottom: 10px;
}

.pres-form-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.pres-form-actions {
  align-items: center;
}

.pres-input :deep(.q-field__control) {
  border-radius: var(--pq-radius-sm) !important;
}

.pres-hint {
  font-family: var(--pq-font-body);
  font-size: 12px;
  color: var(--pq-text-subtle);
  line-height: 1.4;
  margin-top: 4px;
}

/* Agregar button — filled cobre con shadow + lift */
.pres-add-btn {
  min-height: 36px;
  width: auto;
  min-width: 120px;
  background: var(--pq-accent) !important;
  color: var(--pq-background) !important;
  font-weight: 600;
  border-radius: var(--pq-radius-sm);
  box-shadow: 0 2px 8px rgba(163, 120, 94, 0.25);
  transition: background-color 0.16s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.16s ease, transform 0.1s ease;
}

.pres-add-btn:hover:not(.disabled) {
  background: var(--pq-accent-hover) !important;
  box-shadow: 0 4px 12px rgba(163, 120, 94, 0.35);
  transform: translateY(-1px);
}

.pres-add-btn:active:not(.disabled) {
  transform: scale(0.985) translateY(0);
}

.pres-add-btn:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}

.pres-add-btn.disabled {
  opacity: 0.45 !important;
  filter: saturate(0);
  box-shadow: none;
}

.pres-preview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 6px 10px;
  font-family: var(--pq-font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--pq-accent);
  background: rgba(163, 120, 94, 0.08);
  border: 1px solid rgba(163, 120, 94, 0.15);
  border-radius: var(--pq-radius-md);
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}

.pres-preview--warn {
  color: var(--pq-warning);
  background: rgba(196, 154, 82, 0.08);
  border-color: rgba(196, 154, 82, 0.2);
}

.pres-list-enter-active,
.pres-list-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.pres-list-enter-from {
  opacity: 0;
  transform: translateX(-8px);
}

.pres-list-leave-to {
  opacity: 0;
  transform: translateX(8px);
}

@media (prefers-reduced-motion: reduce) {
  .pres-list-enter-active,
  .pres-list-leave-active {
    transition: none;
  }
}

@media (max-width: 639px) {
  .pres-row__remove {
    min-width: 44px;
    min-height: 44px;
  }
  .pres-form-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .pres-add-btn {
    width: 100%;
  }
}
</style>