import { test, expect, type Page } from '@playwright/test';
import { TENANT_ID, disableTutorial, mockAuth, mockCore } from './support/mocks';

test.use({ serviceWorkers: 'block' });

test('OAuth2 callback exchanges the code and lands on dashboard with tenantId', async ({ page }: { page: Page }) => {
  // /users/me lento: el tenantId visible durante la ventana es el merge de activeTenant, no el de /me
  const auth = await mockAuth(page, { meDelayMs: 1_200 });
  await disableTutorial(page);
  await mockCore(page);

  await page.goto('/#/auth/callback?code=e2e-oauth-code');

  await expect.poll(async () => {
    const raw = await page.evaluate(() => localStorage.getItem('pymeq_user'));
    return raw ? JSON.parse(raw).tenantId : null;
  }).toBe(TENANT_ID);

  await page.waitForURL(/dashboard/);
  expect(await page.evaluate(() => localStorage.getItem('pymeq_token'))).toBe('oauth-access');
  expect(await page.evaluate(() => localStorage.getItem('pymeq_refresh_token'))).toBe('refresh-oauth-access');
  expect(auth.exchangeCode()).toBe('e2e-oauth-code');
});
