import { test, expect } from '@playwright/test';

test.describe('Login flow', () => {
  test('shows login form', async ({ page }) => {
    await page.goto('/auth/login');
    // Wait for form to render
    await expect(page.locator('input[type="email"], input[name="email"], input[placeholder*="email" i]').first()).toBeVisible({ timeout: 10_000 });
  });

  test('login with invalid credentials shows error', async ({ page }) => {
    await page.goto('/auth/login');

    const emailInput = page.locator('input[type="email"], input[name="email"], input[placeholder*="email" i]').first();
    const passwordInput = page.locator('input[type="password"]').first();

    await emailInput.fill('nonexistent@test.com');
    await passwordInput.fill('wrongpassword123');

    // Find and click submit button
    const submitBtn = page.locator('button[type="submit"], button:has-text("Iniciar"), button:has-text("Entrar"), button:has-text("Login")').first();
    await submitBtn.click();

    // Should show some error message
    await expect(page.locator('.q-notification, .q-banner, [class*="error"], [role="alert"]').first()).toBeVisible({ timeout: 10_000 });
  });
});
