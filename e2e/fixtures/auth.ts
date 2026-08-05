import { test as base, type Page } from '@playwright/test';

export const test = base.extend<{ authenticatedPage: Page }>({
  authenticatedPage: async ({ browser }, use) => {
    // TODO: implement login flow
    // 1. Navigate to /auth/login
    // 2. Fill email + password
    // 3. Submit and wait for redirect to /dashboard
    // 4. Pass the authenticated page to the test

    const context = await browser.newContext();
    const page = await context.newPage();

    // Placeholder: just use unauthenticated page for now
    await use(page);
    await context.close();
  },
});

export { expect } from '@playwright/test';
