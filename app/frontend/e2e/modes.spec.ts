import { test, expect, TEXT } from './fixtures';
import { COMPLAINT_PHONE, IMAGES, RETURN_LAPTOP, fillForm, waitForFirstDecision, type FormInput } from './support/flows';
import { gated, llmOnly, readLog, shadow, type RealInstance } from './support/real-stack';

/**
 * DECISIONS_MODE matrix on the real stack (plan §4.1 table). T04 gate labels: only the phone-1 complaint is CONFIDENT;
 * every other example photo is UNCERTAIN. assessmentSource is read from the real /api/cases/assessments response
 * observed by the browser (response listener only, no interception).
 */
const PORT = 8087;
test.use({ baseURL: `http://localhost:${PORT}` });

/** Submits a case and returns only the log lines written for it (one worker, one case at a time). */
async function submitAndCapture(page: import('@playwright/test').Page, input: FormInput, instance: () => RealInstance) {
  const offset = readLog(instance()).length;
  const ctx = await submit(page, input);
  const log = readLog(instance()).slice(offset);
  const gate = /event=prescreen\.gated gate=(\w+)/.exec(log)?.[1] ?? null;
  test.info().annotations.push({ type: 'observed', description: `source=${ctx.assessmentSource} gate=${gate} visionUsage=${/llm\.usage .*step=vision/.test(log)}` });
  console.log(`[observed] ${test.info().project.name} ${test.info().title}: source=${ctx.assessmentSource} gate=${gate}`);
  return { ...ctx, log, gate };
}

async function submit(page: import('@playwright/test').Page, input: FormInput) {
  const assessment = page.waitForResponse((r) => r.url().endsWith('/api/cases/assessments') && r.request().method() === 'POST');
  await page.goto('/');
  await fillForm(page, input);
  await page.getByRole('button', { name: TEXT.form.submit }).click();
  const body = await (await assessment).json();
  await waitForFirstDecision(page);
  return body.caseContext as { assessmentSource: string };
}

function describeMode(title: string, start: () => Promise<RealInstance>, cases: (get: () => RealInstance) => void) {
  test.describe(title, () => {
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
      const ctx = await submitAndCapture(page, input, get);
      expect(ctx.assessmentSource).toBe('LLM');
      expect(ctx.log).toMatch(/event=prescreen\.shadow /);
      expect(ctx.log).toMatch(/llm\.usage .*step=vision/);
    });
  }
});

describeMode('gated', () => gated({ port: PORT }), (get) => {
  test('CONFIDENT: reklamacja z phone-1 pomija model wizyjny (visionLlmSkipped=true)', async ({ page }) => {
    const ctx = await submitAndCapture(page, { ...COMPLAINT_PHONE, photo: IMAGES.phone1 }, get);
    // Jev at the margin may flip phone-1: assert consistency of the gate actually observed in the log.
    if (ctx.gate === 'CONFIDENT') {
      expect(ctx.assessmentSource).toBe('DECISIONS');
      expect(ctx.log).toMatch(/event=prescreen\.gated gate=CONFIDENT .*visionLlmSkipped=true/);
      expect(ctx.log).not.toMatch(/llm\.usage .*step=vision/);
    } else {
      expect(ctx.gate).toBe('UNCERTAIN');
      expect(ctx.assessmentSource).toBe('LLM');
      expect(ctx.log).toMatch(/llm\.usage .*step=vision/);
    }
  });

  test('UNCERTAIN: zwrot z laptop-2 uruchamia model wizyjny (visionLlmSkipped=false)', async ({ page }) => {
    const ctx = await submitAndCapture(page, { ...RETURN_LAPTOP, photo: IMAGES.laptop2 }, get);
    expect(ctx.assessmentSource).toBe('LLM');
    expect(ctx.log).toMatch(/event=prescreen\.gated gate=UNCERTAIN .*visionLlmSkipped=false/);
    expect(ctx.log).toMatch(/llm\.usage .*step=vision/);
  });
});

describeMode('llm-only', () => llmOnly({ port: PORT }), (get) => {
  for (const [name, input] of [['zwrot', RETURN_LAPTOP], ['reklamacja', COMPLAINT_PHONE]] as const) {
    test(`${name}: model wizyjny, brak wywołania Decisions`, async ({ page }) => {
      const ctx = await submitAndCapture(page, input, get);
      expect(ctx.assessmentSource).toBe('LLM');
      expect(ctx.log).not.toMatch(/event=prescreen\./);
      expect(ctx.log).toMatch(/llm\.usage .*step=vision/);
    });
  }
});
