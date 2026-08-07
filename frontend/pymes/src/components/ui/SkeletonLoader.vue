<template>
  <Transition name="fade" mode="out-in">
    <div v-if="isLoading" :class="['skeleton-loader', `layout-${layout}`]" :data-loading-id="skeletonId" role="status" aria-label="Cargando contenido">
      <template v-if="layout === 'card'">
        <BaseCard>
          <div class="skeleton-content q-pa-md">
            <div class="skeleton-header row q-mb-md">
              <BaseSkeleton variant="circle" size="md" />
              <div class="skeleton-meta q-ml-md">
                <BaseSkeleton variant="text" size="sm" width="120px" />
                <BaseSkeleton variant="text" size="xs" width="80px" class="q-mt-xs" />
              </div>
            </div>
            <BaseSkeleton variant="text" size="md" />
            <BaseSkeleton variant="text" size="sm" width="80%" class="q-mt-sm" />
          </div>
        </BaseCard>
      </template>

      <template v-else-if="layout === 'form'">
        <BaseCard>
          <div class="skeleton-content q-pa-lg">
            <div class="form-row q-mb-md">
              <BaseSkeleton variant="text" size="xs" width="80px" />
              <BaseSkeleton variant="rectangle" size="lg" width="100%" class="q-mt-xs" />
            </div>
            <div class="form-row q-mb-md">
              <BaseSkeleton variant="text" size="xs" width="100px" />
              <BaseSkeleton variant="rectangle" size="lg" width="100%" class="q-mt-xs" />
            </div>
            <div class="form-row q-mt-lg">
              <BaseSkeleton variant="rectangle" size="lg" width="100%" height="48px" />
            </div>
          </div>
        </BaseCard>
      </template>

      <template v-else-if="layout === 'stats'">
        <div class="row q-col-gutter-md">
          <div v-for="i in count" :key="i" class="col-12 col-md-4">
            <BaseCard>
              <div class="skeleton-content q-pa-md">
                <BaseSkeleton variant="text" size="xs" width="100px" />
                <BaseSkeleton variant="text" size="xl" width="60px" class="q-mt-sm" />
              </div>
            </BaseCard>
          </div>
        </div>
      </template>

      <template v-else-if="layout === 'list'">
        <div class="skeleton-list">
          <BaseCard v-for="i in count" :key="i" class="q-mb-sm">
            <div class="skeleton-content row items-center q-pa-sm">
              <BaseSkeleton variant="circle" size="sm" />
              <div class="q-ml-md flex-grow-1">
                <BaseSkeleton variant="text" size="sm" width="60%" />
                <BaseSkeleton variant="text" size="xs" width="40%" class="q-mt-xs" />
              </div>
            </div>
          </BaseCard>
        </div>
      </template>

      <template v-else>
        <BaseSkeleton variant="rectangle" size="xl" width="100%" height="200px" />
      </template>
    </div>

    <div v-else class="skeleton-content-wrapper">
      <slot />
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseSkeleton from 'src/components/base/BaseSkeleton.vue';

interface Props {
  isLoading: boolean;
  layout?: 'card' | 'form' | 'stats' | 'list' | 'custom';
  count?: number;
  loadingId?: string | number;
}

const props = withDefaults(defineProps<Props>(), {
  layout: 'custom',
  count: 3
});

const skeletonId = computed(() => props.loadingId || `skeleton-${Date.now()}`);
</script>

<style lang="scss" scoped>
.skeleton-loader {
  width: 100%;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-header {
  align-items: center;
}

.skeleton-meta {
  flex: 1;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>