import { test, expect, type Page } from '@playwright/test';
import { TENANT_ID, disableTutorial, mockAuth, mockCore } from './support/mocks';

test.use({ serviceWorkers: 'block' });

async function loginByEmail(page: Page) {
  await page.goto('/#/login');
  await page.getByRole('button', { name: /iniciar sesión con email/i }).click();
  const inputs = page.locator('.q-form input');
  await inputs.nth(0).fill('owner@e2e.test');
  await inputs.nth(1).fill('e2e-password');
  await page.locator('button[type="submit"]').click();
  await page.waitForURL(/dashboard/, { timeout: 15_000 });
}

const ROUTES = [
  '/dashboard',
  '/dashboard/productos',
  '/dashboard/proveedores',
  '/dashboard/facturas',
  '/dashboard/ventas',
  '/dashboard/analisis-gastos',
  '/dashboard/prestamos',
  '/dashboard/costos',
  '/dashboard/patrimonio',
  '/dashboard/teams',
];

test('nav stress: 5 cycles without freeze or console errors', async ({ page }) => {
  const errors: string[] = [];
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('pageerror', (e) => errors.push(`PAGEERROR: ${e.message}`));

  await mockAuth(page);
  await disableTutorial(page);
  await mockCore(page);

  await loginByEmail(page);

  let heap0 = 0;
  for (let cycle = 0; cycle < 5; cycle++) {
    for (const r of ROUTES) {
      await page.goto(`/#${r}`);
      // wait for route to settle: no spinner longer than 8s
      await page.waitForTimeout(400);
      const hasBlank = await page.evaluate(() => document.body.innerText.trim().length === 0);
      if (hasBlank) throw new Error(`Blank screen at ${r} cycle ${cycle}`);
      const heap = await page.evaluate(() => (performance as any).memory?.usedJSHeapSize ?? 0);
      if (cycle === 0 && r === ROUTES[0]) heap0 = heap;
      if (cycle === 4 && r === ROUTES[ROUTES.length - 1] && heap0 > 0) {
        const growth = (heap - heap0) / heap0;
        console.log(`heap growth: ${(growth * 100).toFixed(1)}% (${heap0} -> ${heap})`);
        if (growth > 0.5) throw new Error(`Heap grew ${(growth * 100).toFixed(1)}%`);
      }
    }
  }

  // filter known benign errors
  const real = errors.filter((e) => !/ResizeObserver|favicon|net::ERR_FAILED.*sw/i.test(e));
  if (real.length) throw new Error(`Console errors:\n${real.join('\n')}`);
});
