import { defineConfig } from '@playwright/test';

/**
 * Real-stack E2E: the packaged Spring Boot JAR (serves the Angular build on :8080) with real OpenRouter.
 * No request interception of any kind. Run with `npx playwright test` from app/frontend.
 */
/** Main instance port; override with E2E_PORT when :8080 is occupied by another process. */
const PORT = Number(process.env.E2E_PORT ?? 8080);

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 180_000,
  expect: { timeout: 90_000 },
  reporter: 'list',
  // Keep per-test artifacts apart from test-results/real-stack (application logs for the global teardown).
  outputDir: './test-results/artifacts',
  globalTeardown: './e2e/global-teardown.ts',
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: {
    // The JAR log is kept for the global teardown's "calls were real" proof (numbers and ids only).
    command:
      'mkdir -p test-results/real-stack && exec java -jar ../backend/target/service-copilot-0.0.1-SNAPSHOT.jar > test-results/real-stack/main-' + PORT + '.log 2>&1',
    url: `http://localhost:${PORT}/actuator/health`,
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
    env: {
      SERVER_PORT: String(PORT),
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
