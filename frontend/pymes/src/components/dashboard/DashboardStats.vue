<template>
  <div class="row q-col-gutter-lg">
    <div v-for="(stat, i) in stats" :key="stat.label" class="col-12 col-sm-6 col-md-4 fade-in-up" :style="{ animationDelay: `${0.1 + i * 0.1}s` }">
      <BaseCard variant="elevated" class="q-pa-lg full-height">
        <div class="row items-center justify-between q-mb-md">
          <div class="text-overline text-accent">{{ stat.label }}</div>
          <q-icon :name="stat.icon" :color="stat.iconColor" size="sm" />
        </div>
        <div class="text-h3 font-bold" :class="`text-${stat.valueColor}`">{{ stat.value }}</div>
        <div class="text-caption q-mt-sm" :class="`text-${stat.metaColor}`">
          <q-icon v-if="stat.metaIcon" :name="stat.metaIcon" /> {{ stat.meta }}
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import BaseCard from 'src/components/base/BaseCard.vue';

const stats = [
  {
    label: 'Auditorías Activas',
    icon: 'security',
    iconColor: 'primary',
    value: '12',
    valueColor: 'primary',
    meta: '+2 este mes',
    metaColor: 'positive',
    metaIcon: 'trending_up',
  },
  {
    label: 'Riesgo Promedio',
    icon: 'gpp_maybe',
    iconColor: 'negative',
    value: 'Bajo',
    valueColor: 'negative',
    meta: 'Basado en 45 controles',
    metaColor: 'accent',
    metaIcon: '',
  },
  {
    label: 'Usuarios Activos',
    icon: 'people',
    iconColor: 'primary',
    value: '1',
    valueColor: 'primary',
    meta: 'Licencia: Business Core',
    metaColor: 'accent',
    metaIcon: '',
  },
];
</script>
