import type { RouteRecordRaw } from 'vue-router';

export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../pages/LoginPage.vue'),
  },
  {
    path: '/auth-options',
    name: 'auth-options',
    component: () => import('../pages/AuthOptionsPage.vue'),
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../pages/RegisterPage.vue'),
  },
  {
    path: '/auth/callback',
    name: 'auth-callback',
    component: () => import('../pages/AuthCallback.vue'),
  },
  {
    path: '/verify',
    name: 'verify-email',
    component: () => import('../pages/VerifyEmailPage.vue'),
    meta: { requiresAuth: false }
  }
];
