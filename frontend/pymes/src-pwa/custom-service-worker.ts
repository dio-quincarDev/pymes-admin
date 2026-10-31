/*
 * This file (which will be your service worker)
 * is picked up by the build system ONLY if
 * quasar.config file > pwa > workboxMode is set to "InjectManifest"
 */

declare const self: ServiceWorkerGlobalScope &
  typeof globalThis & { skipWaiting: () => void };

import { clientsClaim } from 'workbox-core';
import {
  precacheAndRoute,
  cleanupOutdatedCaches,
} from 'workbox-precaching';
import { registerRoute, NavigationRoute } from 'workbox-routing';
import { NetworkFirst } from 'workbox-strategies';
import { CacheableResponsePlugin } from 'workbox-cacheable-response';
import type { WorkboxPlugin } from 'workbox-core/types';

void self.skipWaiting();
void clientsClaim();

precacheAndRoute(self.__WB_MANIFEST);

cleanupOutdatedCaches();

// Clean only legacy runtime caches on activate (never touch precache or unknown caches).
self.addEventListener('activate', (event) => {
  (event as unknown as { waitUntil: (p: Promise<unknown>) => void }).waitUntil((async () => {
    const legacy = new Set(['core-api-cache', 'app-shell']);
    const keys = await caches.keys();
    await Promise.all(
      keys.map((key) => (legacy.has(key) ? caches.delete(key) : Promise.resolve())),
    );
  })());
});

// ponytail: listen for app messages
self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') {
    void self.skipWaiting();
  }
  if (event.data?.type === 'CLEAR_API_CACHE') {
    void caches.delete('core-api-cache-v1').then(() => {
      // also drop legacy name from older SW versions
      void caches.delete('core-api-cache');
      console.log('[SW] core-api-cache cleared');
    });
  }
});

// NetworkFirst: una sola request de red por visita (datos frescos), fallback a cache si offline.
// (Antes StaleWhileRevalidate duplicaba cada GET: stale + revalidación en paralelo.)
registerRoute(
  /\/api\/v1\/core\/.*/,
  new NetworkFirst({
    cacheName: 'core-api-cache-v1',
    networkTimeoutSeconds: 10,
    fetchOptions: { cache: 'no-store' },
    plugins: [
      new CacheableResponsePlugin({ statuses: [0, 200] }) as unknown as WorkboxPlugin,
    ],
  }),
  'GET',
);

// App shell via NetworkFirst (fresh index.html per navigation) instead of precached index.html.
// Prevents blackout when an old SW serves a precached index.html referencing removed bundles.
const appShellHandler = new NetworkFirst({
  cacheName: 'app-shell-v1',
  fetchOptions: { cache: 'no-store' },
  plugins: [new CacheableResponsePlugin({ statuses: [0, 200] }) as unknown as WorkboxPlugin],
});

// Non-SSR fallbacks to index.html
// Production SSR fallbacks to offline.html (except for dev)
if (process.env.MODE !== 'ssr' || process.env.PROD) {
  registerRoute(
    new NavigationRoute(appShellHandler, {
      denylist: [
        new RegExp(process.env.PWA_SERVICE_WORKER_REGEX),
        /workbox-(.)*\.js$/,
        /^\/oauth2/,
        /^\/login/,
      ],
    }),
  );
}
