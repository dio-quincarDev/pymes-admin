import { register } from 'register-service-worker';

// ponytail: dispatch DOM events — MainLayout listens and shows dialog.
// Cache-bust the SW URL with a build-time timestamp so browsers re-download it
// on every deploy instead of serving the stale precache (24h SW update throttle).
const swUrl = `${process.env.SERVICE_WORKER_FILE}?v=${process.env.SW_BUILD_TIME}`;
register(swUrl, {

  ready () {},

  registered () {},

  cached () {},

  updatefound () {
    window.dispatchEvent(new CustomEvent('sw-update-found'));
  },

  updated () {
    window.dispatchEvent(new CustomEvent('sw-update-ready'));
  },

  offline () {},

  error () {},
});
