import type { RouteRecordRaw } from 'vue-router';

export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/auth',
    component: () => import('src/layouts/AuthLayout.vue'),
    children: [
      {
        path: '/register',
        name: 'register',
        component: () => import('../pages/RegisterPage.vue'),
      },
      {
        path: '/verify',
        name: 'verify-email',
        component: () => import('../pages/VerifyEmailPage.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: '/forgot-password',
        name: 'forgot-password',
        component: () => import('../pages/ForgotPasswordPage.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: '/reset-password',
        name: 'reset-password',
        component: () => import('../pages/ResetPasswordPage.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: '/accept-invitation',
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
