import { register } from 'register-service-worker';

// ponytail: dispatch DOM events — MainLayout listens and shows dialog
register(process.env.SERVICE_WORKER_FILE, {

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
