import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 5_000 },
  fullyParallel: false,
  retries: 1,
  reporter: [['html', { open: 'never' }], ['list']],

  use: {
    baseURL: 'http://localhost:9200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
    locale: 'es-VE',
    timezoneId: 'America/Caracas',
  },

  projects: [
    {
      name: 'chromium',
      use: {
        browserName: 'chromium',
        headless: false,
        launchOptions: {
          args: [
            '--disable-blink-features=AutomationControlled',
          ],
        },
        contextOptions: {
          userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
        },
      },
    },
  ],

  webServer: {
    command: 'docker compose up -d',
    url: 'http://localhost:9200',
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
