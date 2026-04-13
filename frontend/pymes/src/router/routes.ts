import type { RouteRecordRaw } from 'vue-router';
import { authRoutes } from 'src/modules/auth/router/routes';

const routes: RouteRecordRaw[] = [
  // Dashboard & Private Routes
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') }
    ],
  },

  // Auth Module Routes
  ...authRoutes,

  // Always leave this as last one
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
