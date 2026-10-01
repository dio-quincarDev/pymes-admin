import { defineConfig } from 'vitest/config';
import { fileURLToPath } from 'node:url';

// ponytail: standalone vitest, no Vue plugin needed — testing pure TS utils
export default defineConfig({
  resolve: {
    // ponytail: mismo alias `src` que resuelve Quasar/Vite en build; sin esto el spec del auth store no resuelve imports
    alias: { src: fileURLToPath(new URL('./src', import.meta.url)) },
  },
  test: {
    environment: 'node',
    include: ['src/**/*.spec.ts'],
  },
});
