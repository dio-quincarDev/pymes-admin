import { defineConfig } from 'vitest/config';

// ponytail: standalone vitest, no Vue plugin needed — testing pure TS utils
export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.spec.ts'],
  },
});
