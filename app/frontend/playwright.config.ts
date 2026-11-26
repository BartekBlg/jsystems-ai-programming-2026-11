import { defineConfig } from '@playwright/test';

/**
 * Real-stack E2E: the packaged Spring Boot JAR (serves the Angular build on :8080) with real OpenRouter.
 * No request interception of any kind. Run with `npx playwright test` from app/frontend.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 180_000,
  expect: { timeout: 90_000 },
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: {
    command: 'java -jar ../backend/target/service-copilot-0.0.1-SNAPSHOT.jar',
    url: 'http://localhost:8080/actuator/health',
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
    env: {
      RATE_LIMIT_CASES_PER_HOUR: '1000',
      RATE_LIMIT_CHAT_PER_HOUR: '1000',
      CASE_SIGNING_KEY: 'ZTJlLWZpeGVkLXNpZ25pbmcta2V5LTMyLWJ5dGVzISE=',
      OPENROUTER_API_KEY: process.env.OPENROUTER_API_KEY ?? '',
    },
    stdout: 'pipe',
    stderr: 'pipe',
    gracefulShutdown: { signal: 'SIGTERM', timeout: 5_000 },
  },
  projects: [
    {
      name: 'mobile-360',
      use: { browserName: 'chromium', viewport: { width: 360, height: 800 } },
    },
    {
      name: 'desktop-1280',
      use: { browserName: 'chromium', viewport: { width: 1280, height: 800 } },
    },
  ],
});
