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
}

test('login merges activeTenant.id into user.tenantId before /users/me resolves', async ({ page }) => {
  // /users/me lento: obliga a leer el user que dejó setSession, no el que sobreescribe fetchCurrentUser
  const auth = await mockAuth(page, { meDelayMs: 1_200 });
  await disableTutorial(page);
  await mockCore(page);

  await loginByEmail(page);

  await expect.poll(async () => {
    const raw = await page.evaluate(() => localStorage.getItem('pymeq_user'));
    return raw ? JSON.parse(raw).tenantId : null;
  }).toBe(TENANT_ID);

  await page.waitForURL(/dashboard/);
  expect(await page.evaluate(() => localStorage.getItem('pymeq_token'))).toBe(auth.loginToken());
});

test('logout → re-login gets fresh tokens and keeps the same tenantId', async ({ page }) => {
  const auth = await mockAuth(page);
  await disableTutorial(page);
  await mockCore(page);

  await loginByEmail(page);
  await page.waitForURL(/dashboard/);
  expect(await page.evaluate(() => localStorage.getItem('pymeq_token'))).toBe('access-1');

  await page.getByRole('button', { name: 'Menú de usuario' }).click();
  await page.getByText('Cerrar Sesión').click();
  await page.waitForURL((url) => url.hash === '#/');
  expect(await page.evaluate(() => localStorage.getItem('pymeq_token'))).toBeNull();

  await loginByEmail(page);
  await page.waitForURL(/dashboard/);
  expect(await page.evaluate(() => localStorage.getItem('pymeq_token'))).toBe('access-2');

  const user = await page.evaluate(() => JSON.parse(localStorage.getItem('pymeq_user') || '{}'));
  expect(user.tenantId).toBe(TENANT_ID);
});
