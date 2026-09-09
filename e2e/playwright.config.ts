import { defineConfig } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
// ponytail: load e2e/.env locally if present — CI injects via secrets, local via file
(() => {
  const envPath = path.resolve(__dirname, '.env');
  if (fs.existsSync(envPath)) {
    for (const line of fs.readFileSync(envPath, 'utf8').split('\n')) {
      const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/);
      if (m && !line.trim().startsWith('#') && !process.env[m[1]]) process.env[m[1]] = m[2];
    }
  }
})();

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
