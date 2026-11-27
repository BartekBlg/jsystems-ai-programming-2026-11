import fs from 'node:fs';
import path from 'node:path';
import { test, expect, type APIResponse } from '@playwright/test';
import { rateLimit1, type RealInstance } from './support/real-stack';

/**
 * Real-stack API endpoint checks (plan §4.1 "Every endpoint is exercised for real"; TAC-05, TAC-001-01/03/06).
 * Real JAR on :8080 (config webServer), real OpenRouter for the one real assessment. No interception.
 */

const REPO = path.resolve(__dirname, '../../..');
const LAPTOP_PNG = fs.readFileSync(path.join(REPO, 'assets/example-images/laptop-1.png'));
const LAPTOP_WEBP = fs.readFileSync(path.join(REPO, 'assets/example-images/laptop-2.webp'));
// Not an image: the PRD file named photo.jpg. (There is no real GIF in the repo and generating images is
// forbidden, so this replaces the plan's "GIF renamed .jpg".)
const NOT_AN_IMAGE = fs.readFileSync(path.join(REPO, 'docs/PRD.md'));

/** TAC-05 no-leak rule applied to every response body. */
const LEAK = /Exception|at pl\.|openrouter|OpenRouter|gpt|jev|sk-or/;

async function bodyText(res: APIResponse): Promise<string> {
  return (await res.body()).toString('utf8');
}

async function expectNoLeak(res: APIResponse, label: string): Promise<string> {
  const text = await bodyText(res);
  expect(text, `${label}: response body leaks internals`).not.toMatch(LEAK);
  return text;
}

function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const VALID_FORM = {
  caseType: 'RETURN',
  category: 'LAPTOP',
  modelName: 'Lenovo ThinkPad T14 Gen 4',
  purchaseDate: isoDaysAgo(5),
  reason: null as string | null,
};

function assessmentParts(photo: { buffer: Buffer; name: string; mimeType: string }, form: object = VALID_FORM) {
  return {
    form: { name: 'form.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(form)) },
    photo,
  };
}

function expectProblem(body: string, status: number, code: string): Record<string, unknown> {
  const json = JSON.parse(body);
  expect(json.status).toBe(status);
  expect(json.code).toBe(code);
  expect(typeof json.detail).toBe('string');
  return json;
}

test.describe('Real JAR — static, health and error endpoints', () => {
  test('GET /actuator/health is UP', async ({ request }) => {
    const res = await request.get('/actuator/health');
    expect(res.status()).toBe(200);
    const text = await bodyText(res);
    expect(JSON.parse(text).status).toBe('UP');
  });

  for (const route of ['/', '/some/spa/route']) {
    test(`GET ${route} serves the Angular shell`, async ({ request }) => {
      const res = await request.get(route);
      expect(res.status()).toBe(200);
      expect(res.headers()['content-type']).toContain('text/html');
      expect(await bodyText(res)).toContain('<app-root');
    });
  }

  test('GET /api/unknown is 404 problem+json NOT_FOUND', async ({ request }) => {
    const res = await request.get('/api/unknown');
    expect(res.status()).toBe(404);
    expect(res.headers()['content-type']).toContain('application/problem+json');
    expectProblem(await expectNoLeak(res, 'GET /api/unknown'), 404, 'NOT_FOUND');
  });
});

test.describe('Real JAR — POST /api/cases/assessments rejections (no LLM call)', () => {
  test('11 MB real JPEG -> 413 PHOTO_TOO_LARGE', async ({ request }) => {
    const chunks: Buffer[] = [];
    let size = 0;
    while (size <= 10 * 1024 * 1024) {
      // concatenated real image bytes, just over 10 MB in total
      chunks.push(LAPTOP_PNG);
      size += LAPTOP_PNG.length;
    }
    const big = Buffer.concat(chunks);
    expect(big.length).toBeGreaterThan(10 * 1024 * 1024);
    const res = await request.post('/api/cases/assessments', {
      multipart: assessmentParts({ buffer: big, name: 'photo.jpg', mimeType: 'image/jpeg' }),
    });
    expect(res.status()).toBe(413);
    expectProblem(await expectNoLeak(res, '413'), 413, 'PHOTO_TOO_LARGE');
  });

  test('non-image file named photo.jpg -> 400 UNSUPPORTED_IMAGE', async ({ request }) => {
    const res = await request.post('/api/cases/assessments', {
      multipart: assessmentParts({ buffer: NOT_AN_IMAGE, name: 'photo.jpg', mimeType: 'image/jpeg' }),
    });
    expect(res.status()).toBe(400);
    expectProblem(await expectNoLeak(res, 'UNSUPPORTED_IMAGE'), 400, 'UNSUPPORTED_IMAGE');
  });

  test('invalid form -> 400 VALIDATION_FAILED with Polish fieldErrors', async ({ request }) => {
    const res = await request.post('/api/cases/assessments', {
      multipart: assessmentParts(
        { buffer: LAPTOP_WEBP, name: 'laptop-2.webp', mimeType: 'image/webp' },
        { caseType: null, category: null, modelName: 'x', purchaseDate: null, reason: null },
      ),
    });
    expect(res.status()).toBe(400);
    const json = expectProblem(await expectNoLeak(res, 'VALIDATION_FAILED'), 400, 'VALIDATION_FAILED');
    const errors = json.fieldErrors as { field: string; message: string }[];
    expect(errors.length).toBeGreaterThanOrEqual(4);
    const byField = Object.fromEntries(errors.map((e) => [e.field, e.message]));
    expect(byField['caseType']).toBe('Wybierz rodzaj zgłoszenia.');
    expect(byField['category']).toBe('Wybierz kategorię sprzętu.');
    expect(byField['modelName']).toBe('Podaj nazwę lub model urządzenia (2–100 znaków).');
    expect(byField['purchaseDate']).toBe('Podaj datę zakupu.');
  });
});

test.describe('Real JAR — assessment, decision, signature and chat (real OpenRouter)', () => {
  test('assessment 200 -> decision 200 -> tampered 401 -> repeat 409', async ({ request }) => {
    // 1. Real assessment (laptop-2.webp, RETURN/LAPTOP)
    const assessed = await request.post('/api/cases/assessments', {
      multipart: assessmentParts({ buffer: LAPTOP_WEBP, name: 'laptop-2.webp', mimeType: 'image/webp' }),
    });
    expect(assessed.status(), await bodyText(assessed)).toBe(200);
    const assessedText = await expectNoLeak(assessed, 'assessment 200');
    const { caseContext, signature } = JSON.parse(assessedText);
    expect(typeof signature).toBe('string');
    expect(caseContext.caseType).toBe('RETURN');

    // 2. Real decision
    const decided = await request.post('/api/cases/decisions', { data: { caseContext, signature } });
    expect(decided.status(), await bodyText(decided)).toBe(200);
    const decidedText = await expectNoLeak(decided, 'decision 200');
    const decidedJson = JSON.parse(decidedText);
    expect(decidedJson.decision).toBeTruthy();
    expect(typeof decidedJson.signature).toBe('string');

    // 3. One changed field -> 401 on /decisions and /chat/messages
    const tampered = structuredClone(caseContext);
    tampered.form.modelName = 'Tampered Model 9000';
    const badDecision = await request.post('/api/cases/decisions', { data: { caseContext: tampered, signature } });
    expect(badDecision.status()).toBe(401);
    expectProblem(await expectNoLeak(badDecision, 'tampered /decisions'), 401, 'INVALID_CASE_SIGNATURE');

    const history = [
      { id: 'a1', role: 'assistant', text: 'Decyzja' },
      { id: 'u1', role: 'user', text: 'Dziękuję, a co dalej?' },
    ];
    const badChat = await request.post('/api/chat/messages', {
      data: { caseContext: tampered, signature, messages: history },
    });
    expect(badChat.status()).toBe(401);
    expectProblem(await expectNoLeak(badChat, 'tampered /chat/messages'), 401, 'INVALID_CASE_SIGNATURE');

    // 4. Repeated decision on the already decided context -> 409
    const repeat = await request.post('/api/cases/decisions', {
      data: { caseContext: decidedJson.caseContext, signature: decidedJson.signature },
    });
    expect(repeat.status()).toBe(409);
    expectProblem(await expectNoLeak(repeat, 'repeat /decisions'), 409, 'DECISION_ALREADY_MADE');
  });
});

test.describe('Real JAR — rate limit instance (ADR-005 D10)', () => {
  let instance: RealInstance;
  test.beforeAll(async () => {
    instance = await rateLimit1({ port: 8081 });
  });
  test.afterAll(async () => {
    await instance?.stop();
  });

  test('second assessment -> 429 RATE_LIMITED (non-image, so no LLM call)', async ({ playwright }) => {
    const api = await playwright.request.newContext({ baseURL: instance.baseURL });
    const attempt = () =>
      api.post('/api/cases/assessments', {
        multipart: assessmentParts({ buffer: NOT_AN_IMAGE, name: 'photo.jpg', mimeType: 'image/jpeg' }),
      });
    const first = await attempt();
    expect(first.status()).toBe(400); // consumed the single allowed attempt, rejected as UNSUPPORTED_IMAGE
    await expectNoLeak(first, 'rate-limit first');
    const second = await attempt();
    expect(second.status()).toBe(429);
    expectProblem(await expectNoLeak(second, 'RATE_LIMITED'), 429, 'RATE_LIMITED');
    await api.dispose();
  });
});
