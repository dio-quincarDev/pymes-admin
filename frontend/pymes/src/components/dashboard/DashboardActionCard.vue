<template>
  <BaseCard class="q-pa-xl flex flex-center text-center">
    <div style="max-width: 400px">
      <q-icon name="analytics" size="6rem" color="primary" class="opacity-20 q-mb-lg" />
      <div class="text-h5 font-bold q-mb-md">Inicia un nuevo análisis</div>
      <p class="text-body2 text-accent q-mb-xl">
        Selecciona una categoría para comenzar a auditar los movimientos de tu empresa.
      </p>
      <BaseButton size="lg">COMENZAR AUDITORÍA</BaseButton>
    </div>
  </BaseCard>
</template>

<script setup lang="ts">
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
</script>

<style scoped>
.opacity-20 { opacity: 0.2; }
</style>
