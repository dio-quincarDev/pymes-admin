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

  // Global Navigation Guard: Protección de rutas Pymeq
  Router.beforeEach((to) => {
    const authStore = useAuthStore();
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

    if (requiresAuth && !authStore.isAuthenticated) {
      // Si la ruta es protegida y no hay token, al login
      return {
        path: '/login',
        query: { redirect: to.fullPath } // Guardamos a dónde quería ir
      };
    }
    // Si no se retorna nada o se retorna undefined, la navegación continúa normalmente
  });

  return Router;
});
