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
    path: 'gastos',
    redirect: () => ({ path: '/dashboard/costos', query: { tab: 'gastosFijos' } }),
  },
  {
    path: 'costos',
    name: 'costos',
    component: () => import('../pages/CostosPage.vue'),
    meta: { title: 'Costos' },
  },
  {
    path: 'ventas',
    name: 'ventas',
    component: () => import('../pages/VentasPage.vue'),
    meta: { title: 'Ventas' },
  },
  {
    path: 'prestamos',
    name: 'prestamos',
    component: () => import('../pages/PrestamosPage.vue'),
    meta: { title: 'Préstamos' },
  },
  {
    path: 'patrimonio',
    name: 'patrimonio',
    component: () => import('../pages/PatrimonioPage.vue'),
    meta: { title: 'Patrimonio' },
  },
  {
    path: 'accounting',
    name: 'accounting',
    component: () => import('../pages/AccountingPage.vue'),
    meta: { title: 'Contabilidad' },
  },
]

export const onboardingRoute: RouteRecordRaw = {
  path: '/onboarding',
  name: 'onboarding',
  component: () => import('../pages/OnboardingPage.vue'),
  meta: { requiresAuth: true },
}
