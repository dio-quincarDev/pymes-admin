<template>
  <BaseCard variant="ghost" class="q-pa-lg full-height">
    <div class="text-subtitle2 font-bold q-mb-md">Actividad Reciente</div>
    <q-list dark separator class="border-light radius-sm">
      <q-item v-for="i in 3" :key="i" class="q-py-md">
        <q-item-section avatar>
          <q-icon name="history" color="accent" size="xs" />
        </q-item-section>
        <q-item-section>
          <q-item-label class="text-body2">Inicio de sesión detectado</q-item-label>
          <q-item-label caption>Hace {{ i * 2 }} horas</q-item-label>
        </q-item-section>
      </q-item>
    </q-list>
    <BaseButton variant="ghost" size="sm" class="full-width q-mt-md">Ver todo</BaseButton>
  </BaseCard>
</template>

<script setup lang="ts">
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
</script>

<style scoped>
.border-light {
  border: 1px solid rgba(113, 131, 127, 0.1);
}
</style>
