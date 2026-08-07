import { ref, onMounted } from 'vue';

export function useAuthForm(delay = 800) {
  const loading = ref(false);
  const initialLoading = ref(true);
  const showPassword = ref(false);
  const showConfirmPassword = ref(false);

  onMounted(() => {
    setTimeout(() => {
      initialLoading.value = false;
    }, delay);
  });

  return {
    loading,
    initialLoading,
    showPassword,
    showConfirmPassword,
  };
}
