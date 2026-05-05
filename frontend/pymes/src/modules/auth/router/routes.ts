import type { RouteRecordRaw } from 'vue-router';

export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('src/layouts/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'login',
        component: () => import('../pages/LoginPage.vue'),
      }
    ]
  },
  {
    path: '/register',
    component: () => import('src/layouts/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'register',
        component: () => import('../pages/RegisterPage.vue'),
      }
    ]
  },
  {
    path: '/verify',
    component: () => import('src/layouts/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'verify-email',
        component: () => import('../pages/VerifyEmailPage.vue'),
        meta: { requiresAuth: false }
      }
    ]
  },
  {
    path: '/forgot-password',
    component: () => import('src/layouts/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'forgot-password',
        component: () => import('../pages/ForgotPasswordPage.vue'),
        meta: { requiresAuth: false }
      }
    ]
  },
  {
    path: '/reset-password',
    component: () => import('src/layouts/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'reset-password',
        component: () => import('../pages/ResetPasswordPage.vue'),
        meta: { requiresAuth: false }
      }
    ]
  },
  {
    path: '/accept-invitation',
    component: () => import('src/layouts/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'accept-invitation',
        component: () => import('../pages/AcceptInvitationPage.vue'),
        meta: { requiresAuth: false }
      }
    ]
  },
  {
    path: '/auth/callback',
    name: 'auth-callback',
    component: () => import('../pages/AuthCallback.vue'),
  }
];
