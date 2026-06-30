import type { RouteRecordRaw } from 'vue-router'

export const coreRoutes: RouteRecordRaw[] = [
  {
    path: 'productos',
    name: 'productos',
    component: () => import('../pages/ProductosPage.vue'),
    meta: { title: 'Productos' },
  },
  {
    path: 'proveedores',
    name: 'proveedores',
    component: () => import('../pages/ProveedoresPage.vue'),
    meta: { title: 'Proveedores' },
  },
  {
    path: 'facturas',
    name: 'facturas',
    component: () => import('../pages/FacturasPage.vue'),
    meta: { title: 'Facturas' },
  },
  {
    path: 'analisis-gastos',
    name: 'analisis-gastos',
    component: () => import('../pages/AnalisisGastosPage.vue'),
    meta: { title: 'Análisis de Gastos' },
  },
  {
    path: 'configuracion',
    name: 'configuracion',
    component: () => import('../pages/ConfiguracionPage.vue'),
    meta: { title: 'Configuración' },
  },
]

export const onboardingRoute: RouteRecordRaw = {
  path: '/onboarding',
  name: 'onboarding',
  component: () => import('../pages/OnboardingPage.vue'),
  meta: { requiresAuth: true },
}
