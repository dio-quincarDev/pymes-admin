import type { Page } from '@playwright/test';

export const TENANT_ID = 'e2e00000-0000-4000-8000-000000000001';
export const USER_ID = 'e2e00000-0000-4000-8000-000000000002';

const USER = { id: USER_ID, email: 'owner@e2e.test', name: 'E2E Owner' };
const ACTIVE_TENANT = { id: TENANT_ID, name: 'E2E Tenant', slug: 'e2e-tenant', plan: 'FREE' };

const authData = (accessToken: string) => ({
  accessToken,
  refreshToken: `refresh-${accessToken}`,
  // tenantId ausente a propósito: el store debe mergear activeTenant.id
  user: { ...USER },
  activeTenant: ACTIVE_TENANT,
});

const userWithTenant = () => ({ ...USER, tenantId: TENANT_ID, role: 'OWNER', plan: 'FREE' });

export async function disableTutorial(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('pymeq_tour_seen', 'true');
    localStorage.setItem('pymeq_hint_seen', 'true');
  });
}

export async function seedSession(page: Page) {
  await disableTutorial(page);
  await page.addInitScript(({ tenantId }) => {
    localStorage.setItem('pymeq_token', 'seeded-access');
    localStorage.setItem('pymeq_refresh_token', 'seeded-refresh');
    localStorage.setItem('pymeq_user', JSON.stringify({ id: 'u-e2e', email: 'owner@e2e.test', name: 'E2E Owner', tenantId, role: 'OWNER', plan: 'FREE' }));
    localStorage.setItem('pymeq_tenant_name', 'E2E Tenant');
  }, { tenantId: TENANT_ID });
}

export async function mockAuth(page: Page, opts: { meDelayMs?: number } = {}) {
  let loginCount = 0;
  let exchangeCode: string | undefined;

  await page.route('**/api/v1/auth/login', async (route) => {
    loginCount += 1;
    await route.fulfill({ json: { data: authData(`access-${loginCount}`) } });
  });

  await page.route('**/api/v1/auth/exchange', async (route) => {
    exchangeCode = route.request().postDataJSON()?.code;
    await route.fulfill({ json: { data: authData('oauth-access') } });
  });

  await page.route('**/api/v1/auth/logout', async (route) => {
    await route.fulfill({ json: { data: { success: true, message: 'logout', timestamp: new Date().toISOString(), allSessionsRevoked: true } } });
  });

  await page.route('**/api/v1/users/me', async (route) => {
    if (opts.meDelayMs) await new Promise((resolve) => setTimeout(resolve, opts.meDelayMs));
    await route.fulfill({ json: { data: userWithTenant() } });
  });

  return {
    loginToken: () => `access-${loginCount}`,
    exchangeCode: () => exchangeCode,
  };
}

export interface InvoicePayload {
  tenantId?: string;
  items?: Array<{ cantidadPresentacion?: number; valorPresentacion?: number; itbmsTasa?: number | null }>;
}

export async function mockCore(page: Page, onInvoiceCreated?: (payload: InvoicePayload) => void) {
  await page.route('**/api/v1/core/**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;

    if (request.method() === 'POST' && path.endsWith('/core/facturas')) {
      onInvoiceCreated?.(request.postDataJSON());
      await route.fulfill({ json: { id: 'factura-e2e', tenantId: TENANT_ID, invoiceNumber: 'F-E2E-001', providerId: 'proveedor-e2e', providerName: 'Proveedor E2E', colaboradorId: null, collaboratorName: null, issueDate: '2026-10-26', type: 'FACTURA', paymentMethod: null, category: null, globalDiscount: 0, status: 'REGISTRADA', total: 317, items: [] } });
      return;
    }
    if (path.includes('/core/setup/')) {
      await route.fulfill({ json: { onboardingCompleted: true, industry: 'bares', categories: [{ code: 'bebidas', name: 'Bebidas', parentId: null, children: [] }], units: [{ code: 'UNIT', name: 'Unidad' }], products: [] } });
      return;
    }
    if (path.includes('/core/proveedores')) {
      await route.fulfill({ json: [{ id: 'proveedor-e2e', tenantId: TENANT_ID, name: 'Proveedor E2E', contactName: null, contactPhone: null, contactEmail: null, isActive: true }] });
      return;
    }
    if (path.includes('/core/productos')) {
      await route.fulfill({ json: [{ id: 'producto-e2e', tenantId: TENANT_ID, name: 'Producto E2E', sku: 'E2E-SKU', category: 'bebidas', baseUnit: 'UNIT', imageUrl: null, isActive: true, createdAt: null, updatedAt: null, presentaciones: [], lastUnitPrice: 50, totalInvestment: null, lastPurchaseDate: null, minQuantity: null, maxQuantity: null, proveedorId: 'proveedor-e2e', proveedorName: 'Proveedor E2E' }] });
      return;
    }
    if (path.includes('/core/costos/collaboradores') || path.includes('/core/ventas') || path.includes('/core/facturas') || path.includes('/core/costos/gastos-fijos')) {
      await route.fulfill({ json: [] });
      return;
    }
    if (path.includes('/core/costos/diario')) {
      await route.fulfill({ json: { costoOperativoDiario: 0 } });
      return;
    }
    if (path.includes('/core/accounting')) {
      await route.fulfill({ json: { ingresoTotal: 0, gastoTotal: 0, margenNeto: 0, margenNetoPct: 0 } });
      return;
    }
    await route.fulfill({ json: {} });
  });
}
