import { test, expect, TEXT } from './fixtures';
import { COMPLAINT_PHONE, DECISION_BADGE, FIRST_DECISION_TIMEOUT_MS, IMAGES, RETURN_LAPTOP, fillForm, type FormInput } from './support/flows';
import { gated, llmOnly, readLog, shadow, type RealInstance } from './support/real-stack';

/**
 * DECISIONS_MODE matrix on the real stack (plan §4.1 table). T04 gate labels: only the phone-1 complaint is CONFIDENT;
 * every other example photo is UNCERTAIN. assessmentSource is read from the real /api/cases/assessments response
 * observed by the browser (response listener only, no interception).
 */
const PORT = 8087;
test.use({ baseURL: `http://localhost:${PORT}` });

async function submitAndCapture(page: import('@playwright/test').Page, input: FormInput) {
  const assessment = page.waitForResponse((r) => r.url().endsWith('/api/cases/assessments') && r.request().method() === 'POST');
  await page.goto('/');
  await fillForm(page, input);
  await page.getByRole('button', { name: TEXT.form.submit }).click();
  const body = await (await assessment).json();
  await expect(page.getByText(TEXT.chat.summaryTitle)).toBeVisible({ timeout: FIRST_DECISION_TIMEOUT_MS });
  await expect(page.getByText(DECISION_BADGE).first()).toBeVisible();
  return body.caseContext as { assessmentSource: string };
}

function describeMode(title: string, start: () => Promise<RealInstance>, cases: (get: () => RealInstance) => void) {
  test.describe(title, () => {
    test.fixme(true, 'T35b: the UI shell wiring (form -> progress -> chat) is not in place yet; enabled by T41b.');
    let instance: RealInstance;
    test.beforeAll(async () => {
      instance = await start();
    });
    test.afterAll(async () => {
      await instance?.stop();
    });
    cases(() => instance);
  });
}

describeMode('shadow (domyślny)', () => shadow({ port: PORT }), (get) => {
  for (const [name, input] of [['zwrot', RETURN_LAPTOP], ['reklamacja', COMPLAINT_PHONE]] as const) {
    test(`${name}: assessmentSource=LLM i zdarzenie prescreen.shadow`, async ({ page }) => {
      const ctx = await submitAndCapture(page, input);
      expect(ctx.assessmentSource).toBe('LLM');
      expect(readLog(get())).toMatch(/event=prescreen\.shadow /);
    });
  }
});

describeMode('gated', () => gated({ port: PORT }), (get) => {
  test('CONFIDENT: reklamacja z phone-1 pomija model wizyjny (visionLlmSkipped=true)', async ({ page }) => {
    const ctx = await submitAndCapture(page, { ...COMPLAINT_PHONE, photo: IMAGES.phone1 });
    expect(ctx.assessmentSource).not.toBe('LLM');
    expect(readLog(get())).toMatch(/event=prescreen\.gated gate=CONFIDENT .*visionLlmSkipped=true/);
  });

  test('UNCERTAIN: zwrot z laptop-2 uruchamia model wizyjny (visionLlmSkipped=false)', async ({ page }) => {
    const ctx = await submitAndCapture(page, { ...RETURN_LAPTOP, photo: IMAGES.laptop2 });
    expect(ctx.assessmentSource).toBe('LLM');
    expect(readLog(get())).toMatch(/event=prescreen\.gated gate=UNCERTAIN .*visionLlmSkipped=false/);
  });
});

describeMode('llm-only', () => llmOnly({ port: PORT }), (get) => {
  for (const [name, input] of [['zwrot', RETURN_LAPTOP], ['reklamacja', COMPLAINT_PHONE]] as const) {
    test(`${name}: model wizyjny, brak wywołania Decisions`, async ({ page }) => {
      const ctx = await submitAndCapture(page, input);
      expect(ctx.assessmentSource).toBe('LLM');
      expect(readLog(get())).not.toMatch(/event=prescreen\./);
    });
  }
});
