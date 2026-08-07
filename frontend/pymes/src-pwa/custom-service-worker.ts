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
  createHandlerBoundToURL,
} from 'workbox-precaching';
import { registerRoute, NavigationRoute } from 'workbox-routing';
import { StaleWhileRevalidate } from 'workbox-strategies';

void self.skipWaiting();
void clientsClaim();

precacheAndRoute(self.__WB_MANIFEST);

cleanupOutdatedCaches();

// ponytail: listen for skip-waiting from app dialog
self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') {
    void self.skipWaiting();
  }
});

// ponytail: StaleWhileRevalidate for GET API calls
// serves cached on offline, fetches fresh when online
registerRoute(
  /\/api\/v1\/core\/.*/,
  new StaleWhileRevalidate({
    cacheName: 'core-api-cache',
  }),
  'GET'
);

// Non-SSR fallbacks to index.html
// Production SSR fallbacks to offline.html (except for dev)
if (process.env.MODE !== 'ssr' || process.env.PROD) {
  registerRoute(
    new NavigationRoute(
      createHandlerBoundToURL(process.env.PWA_FALLBACK_HTML),
      { denylist: [new RegExp(process.env.PWA_SERVICE_WORKER_REGEX), /workbox-(.)*\.js$/] }
    )
  );
}
