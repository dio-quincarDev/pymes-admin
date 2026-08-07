import { defineBoot } from '#q-app/wrappers';
import { Notify } from 'quasar';

// ponytail: global error handler, replaces ErrorBoundary component
export default defineBoot(({ app }) => {
  app.config.errorHandler = (err, _instance, info) => {
    console.error(`[Vue error] ${info}:`, err);

    Notify.create({
      type: 'negative',
      message: 'Ha ocurrido un error inesperado',
      timeout: 5000,
    });
  };
});
