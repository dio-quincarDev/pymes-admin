import { test, expect } from '@playwright/test';

test.describe('App loads', () => {
  test('landing page loads', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/PYMEQ|Quasar/i);
  });

  test('shows company name input on home', async ({ page }) => {
    await page.goto('/#/');
    await page.waitForSelector('input', { timeout: 10_000 });
    await expect(page.locator('input').first()).toBeVisible();
  });
});
