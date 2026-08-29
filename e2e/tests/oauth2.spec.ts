import { test, expect } from '@playwright/test';

test.describe('Full OAuth2 user flow', () => {
  test('register → onboarding → logout → re-login enters existing tenant', async ({ page }) => {
    // Remove webdriver detection
    await page.addInitScript(() => {
      Object.defineProperty(navigator, 'webdriver', { get: () => false });
    });

    const timestamp = Date.now();
    const companyName = `E2E Bar ${timestamp}`;

    // ── Step 1: Landing → enter company name → register ──
    await page.goto('/#/');
    await page.waitForSelector('input', { timeout: 10_000 });
    const companyInput = page.locator('input').first();
    await companyInput.fill(companyName);
    await companyInput.press('Enter');
    await page.waitForURL(/register/, { timeout: 10_000 });

    // ── Step 2: Register → click Google ──
    const googleBtn = page.locator('button:has-text("Continuar con Google")');
    await expect(googleBtn).toBeVisible({ timeout: 5_000 });
    await googleBtn.click();

    // ── Step 3: Google OAuth2 (email → password → 2FA) ──
    await page.waitForURL(/accounts\.google\.com/, { timeout: 15_000 });
    await page.waitForTimeout(2_000);

    // Email — ponytail: creds via env, not hardcoded (prev leak b27872c)
    // CI-safe: skip gracefully if secrets not configured (no throw → no CI red)
    const e2eEmail = process.env.E2E_GOOGLE_EMAIL;
    const e2ePassword = process.env.E2E_GOOGLE_PASSWORD;
    if (!e2eEmail || !e2ePassword) {
      test.skip(true, 'Missing E2E_GOOGLE_EMAIL / E2E_GOOGLE_PASSWORD — set in e2e/.env or GitHub Secrets (see e2e/.env.example)');
      return;
    }
    const emailInput = page.locator('input[type="email"], input#identifierId');
    await emailInput.first().waitFor({ state: 'visible', timeout: 10_000 });
    await emailInput.first().fill(e2eEmail);
    await page.locator('#identifierNext').click();

    // Password
    await page.waitForTimeout(3_000);
    const passwordInput = page.locator('input[type="password"], input[name="Passwd"]');
    await passwordInput.first().waitFor({ state: 'visible', timeout: 10_000 });
    await passwordInput.first().fill(e2ePassword);
    await page.locator('#passwordNext').click();

    // Wait for 2FA + redirect back to app (may take a while)
    await page.waitForURL(/onboarding|dashboard/, { timeout: 120_000 });

    // ── Step 4: Onboarding → select industry → confirm ──
    if (page.url().includes('onboarding')) {
      // Select "Bares y Cantinas"
      const baresCard = page.locator('text=Bares y Cantinas');
      await baresCard.waitFor({ state: 'visible', timeout: 10_000 });
      await baresCard.click();

      // Wait for preview to load
      await page.waitForTimeout(3_000);

      // Click "Comenzar"
      const comenzarBtn = page.locator('button:has-text("Comenzar")');
      await comenzarBtn.waitFor({ state: 'visible', timeout: 10_000 });
      await comenzarBtn.click();

      // Wait for dashboard
      await page.waitForURL(/dashboard/, { timeout: 15_000 });
    }

    // ── Step 5: Verify on dashboard ──
    expect(page.url()).toContain('dashboard');

    // ── Step 6: Logout ──
    // Click user avatar to open menu
    const avatarBtn = page.locator('button[aria-label="Menú de usuario"]');
    await avatarBtn.click();

    // Click "Cerrar Sesión"
    const logoutBtn = page.locator('text=Cerrar Sesión');
    await logoutBtn.click();

    // Should redirect to landing page (not login form)
    await page.waitForURL(/\//, { timeout: 10_000 });
    // Wait for auth:401 to fire and clearSession to run
    await page.waitForTimeout(1_000);

    // Verify token is gone
    const tokenAfterLogout = await page.evaluate(() => localStorage.getItem('pymeq_token'));
    expect(tokenAfterLogout).toBeNull();

    // ── Step 7: Re-login via Google → enters existing tenant ──
    const googleBtnAgain = page.locator('button:has-text("Continuar con Google")');
    await expect(googleBtnAgain).toBeVisible({ timeout: 5_000 });
    await googleBtnAgain.click();

    // Google may have session cookies → could auto-redirect to callback
    // or show login page. Wait for either.
    await page.waitForURL(/accounts\.google\.com|dashboard|onboarding|auth\/callback/, { timeout: 15_000 });

    // If we landed on Google login page, fill credentials
    if (page.url().includes('accounts.google.com')) {
      await page.waitForTimeout(2_000);
      const emailInput2 = page.locator('input[type="email"], input#identifierId');
      await emailInput2.first().waitFor({ state: 'visible', timeout: 10_000 });
      await emailInput2.first().fill(e2eEmail);
      await page.locator('#identifierNext').click();

      await page.waitForTimeout(3_000);
      const passwordInput2 = page.locator('input[type="password"], input[name="Passwd"]');
      await passwordInput2.first().waitFor({ state: 'visible', timeout: 10_000 });
      await passwordInput2.first().fill(e2ePassword);
      await page.locator('#passwordNext').click();

      // Wait for 2FA + redirect
      await page.waitForURL(/dashboard|onboarding/, { timeout: 120_000 });
    } else {
      // Auto-redirected (Google session was active) — wait for final destination
      await page.waitForURL(/dashboard|onboarding/, { timeout: 30_000 });
    }

    // ── Step 8: Verify we're on dashboard with tenant ──
    await page.waitForTimeout(2_000);
    const finalUrl = page.url();
    console.log('Final URL after re-login:', finalUrl);

    // Should be on dashboard (not onboarding — tenant already exists)
    expect(finalUrl).toContain('dashboard');

    // Verify token exists
    const token = await page.evaluate(() => localStorage.getItem('pymeq_token'));
    expect(token).toBeTruthy();

    // Verify tenant context exists
    const userStr = await page.evaluate(() => localStorage.getItem('pymeq_user'));
    const userObj = JSON.parse(userStr || '{}');
    expect(userObj.tenantId).toBeTruthy();
  });
});
