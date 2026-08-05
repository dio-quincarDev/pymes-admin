import { test, expect } from '@playwright/test';

test.describe('App loads', () => {
  test('landing page loads', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/PYMEQ|Quasar/i);
  });

  test('redirects to login or shows auth', async ({ page }) => {
    await page.goto('/');
    // After load, user lands on login or dashboard depending on session
    const url = page.url();
    const hasAuth = url.includes('/auth') || url.includes('/login');
    const hasDashboard = url.includes('/dashboard');
    expect(hasAuth || hasDashboard).toBeTruthy();
  });
});
