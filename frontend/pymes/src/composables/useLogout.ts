import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { useAuthStore } from 'src/modules/auth/store';

export function useLogout() {
  const $q = useQuasar();
  const router = useRouter();
  const authStore = useAuthStore();

  const logout = async () => {
    try {
      const response = await authStore.logout();
      const allSessions = response?.data?.allSessionsRevoked;

      $q.notify({
        type: 'info',
        message: allSessions ? 'Todas las sesiones cerradas' : 'Sesión finalizada',
        position: 'top-right',
      });

      void router.push('/');
    } catch (error) {
      console.error('Logout error', error);
    }
  };

  return { logout };
}
