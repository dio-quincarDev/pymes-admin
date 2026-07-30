import { ref, readonly, onMounted, onUnmounted } from 'vue';

interface UsePullToRefreshOptions {
  onRefresh: () => Promise<void>
  threshold?: number
}

export function usePullToRefresh({ onRefresh, threshold = 80 }: UsePullToRefreshOptions) {
  const pullDistance = ref(0);
  const isRefreshing = ref(false);
  let startY = 0;
  let pulling = false;

  function onTouchStart(e: TouchEvent) {
    const touch = e.touches[0];
    if (window.scrollY > 0 || isRefreshing.value || !touch) return;
    startY = touch.clientY;
    pulling = true;
  }

  function onTouchMove(e: TouchEvent) {
    const touch = e.touches[0];
    if (!pulling || isRefreshing.value || !touch) return;
    const dy = touch.clientY - startY;
    if (dy <= 0) { pullDistance.value = 0; return; }
    pullDistance.value = Math.min(dy * 0.5, threshold * 1.5);
  }

  function onTouchEnd() {
    pulling = false;
    if (pullDistance.value >= threshold && !isRefreshing.value) {
      isRefreshing.value = true;
      void onRefresh().finally(() => {
        isRefreshing.value = false;
        pullDistance.value = 0;
      });
    } else {
      pullDistance.value = 0;
    }
  }

  onMounted(() => {
    window.addEventListener('touchstart', onTouchStart, { passive: true });
    window.addEventListener('touchmove', onTouchMove, { passive: true });
    window.addEventListener('touchend', onTouchEnd, { passive: true });
  });

  onUnmounted(() => {
    window.removeEventListener('touchstart', onTouchStart);
    window.removeEventListener('touchmove', onTouchMove);
    window.removeEventListener('touchend', onTouchEnd);
  });

  return { pullDistance: readonly(pullDistance), isRefreshing: readonly(isRefreshing) };
}
