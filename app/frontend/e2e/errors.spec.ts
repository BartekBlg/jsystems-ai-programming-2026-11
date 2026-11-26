import { test, expect, TEXT } from './fixtures';
import { RETURN_LAPTOP, FIRST_DECISION_TIMEOUT_MS, fillForm } from './support/flows';
import { decisionsNotFound, invalidKey, visionTimeout1ms, type RealInstance } from './support/real-stack';

/**
 * Error paths on REAL faulty-configuration instances (ADR-005 D10). Every request still reaches OpenRouter.
 * Each describe runs its own instance on :8086; the browser origin follows it.
 */
const PORT = 8086;
const FAILED = 'Nie udało się przygotować decyzji. Spróbuj ponownie za chwilę.';

test.use({ baseURL: `http://localhost:${PORT}` });

async function expectFailureScreen(page: import('@playwright/test').Page): Promise<void> {
  await expect(page.getByText(FAILED)).toBeVisible({ timeout: FIRST_DECISION_TIMEOUT_MS });
  await expect(page.getByRole('button', { name: TEXT.progress.retry })).toBeVisible();
  await expect(page.getByRole('button', { name: TEXT.progress.backToForm })).toBeVisible();
  // AC-43: no technical detail in the UI.
  await expect(page.locator('body')).not.toContainText(/openrouter|sk-or|Exception|401|gpt|jev/i);
  // AC-41: "Wróć do formularza" keeps the form data.
  await page.getByRole('button', { name: TEXT.progress.backToForm }).click();
  await expect(page.getByLabel(TEXT.form.modelName)).toHaveValue(RETURN_LAPTOP.model);
}

for (const [title, start] of [
  ['nieprawidłowy klucz -> 502 LLM_FAILED (AC-41, AC-43)', () => invalidKey({ port: PORT })],
  ['limit 1 ms dla kroku wizyjnego -> 504 LLM_TIMEOUT (AC-41)', () => visionTimeout1ms({ port: PORT })],
] as const) {
  test.describe(`Błąd: ${title}`, () => {
    test.fixme(true, 'T35b: the UI shell wiring (form -> progress -> chat) is not in place yet; enabled by T41b.');

    let instance: RealInstance;
    test.beforeAll(async () => {
      instance = await start();
    });
    test.afterAll(async () => {
      await instance?.stop();
    });

    test('ekran błędu z przyciskami Spróbuj ponownie i Wróć do formularza', async ({ page }) => {
      await page.goto('/');
      await fillForm(page, RETURN_LAPTOP);
      await page.getByRole('button', { name: TEXT.form.submit }).click();
      await expectFailureScreen(page);
    });
  });
}

test.describe('Pre-screening fail-open: ścieżka Decisions zwraca 404 (AC-49)', () => {
  test.fixme(true, 'T35b: the UI shell wiring (form -> progress -> chat) is not in place yet; enabled by T41b.');

  let instance: RealInstance;
  test.beforeAll(async () => {
    instance = await decisionsNotFound({ port: PORT });
  });
  test.afterAll(async () => {
    await instance?.stop();
  });

  test('klient nie widzi różnicy: sprawa kończy się czatem z decyzją', async ({ page }) => {
    await page.goto('/');
    await fillForm(page, RETURN_LAPTOP);
    await page.getByRole('button', { name: TEXT.form.submit }).click();
    await expect(page.getByText(TEXT.chat.summaryTitle)).toBeVisible({ timeout: FIRST_DECISION_TIMEOUT_MS });
    await expect(page.getByText(FAILED)).toHaveCount(0);
    const log = (await import('./support/real-stack')).readLog(instance);
    expect(log).toMatch(/prescreen/); // the failed pre-screen attempt is logged, the case continued via the vision LLM
  });
});
