import type { RouteRecordRaw } from 'vue-router';
import { authRoutes } from 'src/modules/auth/router/routes';

const routes: RouteRecordRaw[] = [
  // Public Landing Flow
  {
    path: '/',
    component: () => import('layouts/LandingLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
    ],
  },

  // Auth Flow - Login como ruta independiente (fuera del nested para evitar conflicto de layouts)
  {
    path: '/login',
    name: 'login',
    component: () => import('src/modules/auth/pages/LoginPage.vue'),
  },

  // Auth Flow (remaining pages in AuthLayout)
  ...authRoutes,

  // Private Dashboard Routes
  {
    path: '/dashboard',
    component: () => import('layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', component: () => import('pages/DashboardPage.vue') }
    ],
  },

  // Always leave this as last one
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
