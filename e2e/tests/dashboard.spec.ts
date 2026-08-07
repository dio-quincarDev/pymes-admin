import { test, expect } from '@playwright/test';

// These tests require a valid session — skip if no auth helper yet
// To run: create a fixture that logs in and stores the session

test.describe('Dashboard', () => {
  test.skip('shows KPIs after login', async ({ page }) => {
    // TODO: implement login fixture
    // await page.goto('/dashboard');
    // await expect(page.locator('text=Ingresos')).toBeVisible();
  });
});
