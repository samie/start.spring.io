// @ts-check
const { defineConfig, devices } = require('@playwright/test');

/**
 * Playwright configuration for the start.spring.io Vaadin UI validation suite.
 *
 * The app under test is the start-site Spring Boot application. By default the
 * suite expects it to already be running at http://localhost:8080 (start it with
 * `cd start-site && ../mvnw spring-boot:run`, or run the production jar). Set
 * START_SITE_BASE_URL to point at a different instance.
 */
const baseURL = process.env.START_SITE_BASE_URL || 'http://localhost:8080';

module.exports = defineConfig({
  testDir: './tests',
  // Vaadin server round-trips and the in-JVM project generation can be slow.
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    headless: true,
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    acceptDownloads: true
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});
