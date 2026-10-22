import { defineRouter } from '#q-app/wrappers';
import {
  createMemoryHistory,
  createRouter,
  createWebHashHistory,
  createWebHistory,
} from 'vue-router';
import routes from './routes';
import { useAuthStore } from 'src/modules/auth/store';

export default defineRouter(function (/* { store, ssrContext } */) {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : process.env.VUE_ROUTER_MODE === 'history'
      ? createWebHistory
      : createWebHashHistory;

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE),
  });

  Router.beforeEach(async (to) => {
    const authStore = useAuthStore();
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

    if (requiresAuth && !authStore.isAuthenticated) {
      return { path: '/login', query: { redirect: to.fullPath } };
    }

    // ponytail: onboarding obligatorio — sin negocio o con onboarding pendiente
    // solo se puede estar en /onboarding; fail-open si el backend falla
    if (requiresAuth && to.name !== 'onboarding') {
      await authStore.ensureOnboarding();
      const tid = authStore.user?.tenantId;
      if (tid && authStore.onboardingCompleted === false) {
        return { path: '/onboarding' };
      }
    }
  });

  return Router;
});
