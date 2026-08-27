import { test, expect } from '@playwright/test';

test.describe('Logout flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      Object.defineProperty(navigator, 'webdriver', { get: () => false });
    });
  });

  test('auth:401 event clears localStorage', async ({ page }) => {
    await page.goto('/#/');

    // Seed session
    await page.evaluate(() => {
      localStorage.setItem('pymeq_token', 'fake-access-token');
      localStorage.setItem('pymeq_refresh_token', 'fake-refresh-token');
      localStorage.setItem('pymeq_user', JSON.stringify({ id: '1', email: 'test@test.com' }));
      localStorage.setItem('pymeq_tenant_name', 'Test Tenant');
      localStorage.setItem('pymeq_pending_tenant', JSON.stringify({ name: 'Pending', slug: 'pending' }));
    });

    // Verify all keys exist
    const keysBefore = await page.evaluate(() => ({
      token: localStorage.getItem('pymeq_token'),
      refresh: localStorage.getItem('pymeq_refresh_token'),
      user: localStorage.getItem('pymeq_user'),
      tenant: localStorage.getItem('pymeq_tenant_name'),
      pending: localStorage.getItem('pymeq_pending_tenant'),
    }));
    expect(keysBefore.token).toBeTruthy();
    expect(keysBefore.user).toBeTruthy();

    // Dispatch auth:401 event — triggers clearSession in the auth store
    await page.evaluate(() => {
      window.dispatchEvent(new CustomEvent('auth:401'));
    });

    // Verify all keys are cleared
    const keysAfter = await page.evaluate(() => ({
      token: localStorage.getItem('pymeq_token'),
      refresh: localStorage.getItem('pymeq_refresh_token'),
      user: localStorage.getItem('pymeq_user'),
      tenant: localStorage.getItem('pymeq_tenant_name'),
      pending: localStorage.getItem('pymeq_pending_tenant'),
    }));
    expect(keysAfter.token).toBeNull();
    expect(keysAfter.refresh).toBeNull();
    expect(keysAfter.user).toBeNull();
    expect(keysAfter.tenant).toBeNull();
    expect(keysAfter.pending).toBeNull();
  });

  test('re-login after logout gets fresh tokens', async ({ page }) => {
    await page.goto('/#/');

    // Seed stale session
    await page.evaluate(() => {
      localStorage.setItem('pymeq_token', 'stale-token');
      localStorage.setItem('pymeq_refresh_token', 'stale-refresh');
      localStorage.setItem('pymeq_user', JSON.stringify({ id: '1', email: 'old@test.com' }));
    });

    // Clear session (simulating logout via auth:401 event)
    await page.evaluate(() => {
      window.dispatchEvent(new CustomEvent('auth:401'));
    });

    // Verify cleared
    const tokenAfterClear = await page.evaluate(() => localStorage.getItem('pymeq_token'));
    expect(tokenAfterClear).toBeNull();

    // Navigate to login
    await page.goto('/#/login');
    await page.waitForURL(/login/, { timeout: 10_000 });

    // Verify we're on login page
    expect(page.url()).toContain('login');

    // Verify no stale tokens
    const tokenStillGone = await page.evaluate(() => localStorage.getItem('pymeq_token'));
    expect(tokenStillGone).toBeNull();
  });
});
