<script setup lang="ts">
defineProps<{
  modelValue: boolean
  title?: string
  icon: string
  iconColor: string
  message: string
  confirmLabel: string
  confirmColor: string
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()
</script>

<template>
  <q-dialog :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" dark>
    <q-card dark class="bg-surface-pine">
      <q-card-section v-if="title">
        <div class="text-h6 text-primary">{{ title }}</div>
      </q-card-section>
      <q-card-section class="row items-center q-gutter-x-md">
        <q-icon :name="icon" :color="iconColor" size="md" />
        <span v-html="message" />
      </q-card-section>
      <q-card-actions align="right">
        <q-btn flat label="Cancelar" color="accent" v-close-popup />
        <q-btn :label="confirmLabel" :color="confirmColor" :loading="loading" @click="emit('confirm')" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
