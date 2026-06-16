import { ref, onMounted, onUnmounted } from 'vue';

export function useScrollReveal(options?: { threshold?: number; rootMargin?: string }) {
  const isVisible = ref(false);
  const target = ref<HTMLElement | null>(null);
  let observer: IntersectionObserver | null = null;

  onMounted(() => {
    if (!target.value) return;

    observer = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting) {
          isVisible.value = true;
          observer?.unobserve(entry.target);
        }
      },
      { threshold: options?.threshold ?? 0.15, rootMargin: options?.rootMargin ?? '0px' },
    );

    observer.observe(target.value);
  });

  onUnmounted(() => {
    observer?.disconnect();
  });

  return { target, isVisible };
}
