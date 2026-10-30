import { test, expect } from '@playwright/test';

test('logout event clears core-api-cache from the service worker scope', async ({ page }) => {
  await page.goto('/');

  // El SW debe controlar la página: sin controller no hay postMessage CLEAR_API_CACHE
  await expect.poll(() => page.evaluate(() => !!navigator.serviceWorker.controller)).toBe(true);

  await page.evaluate(async () => {
    const cache = await caches.open('core-api-cache-v1');
    await cache.put('/e2e-core-cache-marker', new Response('{}'));
  });
  await expect.poll(() => page.evaluate(() => caches.match('/e2e-core-cache-marker').then(Boolean))).toBe(true);

  // auth:401 dispara clearSession() → postMessage al SW → caches.delete dentro del scope del SW
  await page.evaluate(() => window.dispatchEvent(new CustomEvent('auth:401')));

  await expect.poll(() => page.evaluate(() => caches.match('/e2e-core-cache-marker').then(Boolean))).toBe(false);
});
