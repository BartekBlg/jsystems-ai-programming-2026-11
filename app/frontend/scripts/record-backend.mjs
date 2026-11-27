#!/usr/bin/env node
// Records REAL backend responses (verbatim) for frontend unit-test mocks (hsdc-poc T26).
// Usage: node app/frontend/scripts/record-backend.mjs <scenario>
// Env: BASE_URL (default http://localhost:8080), COMMIT (short hash), INSTANCE_NOTE (instance env, no secrets).
// No secrets are read or written here. Node 24 built-ins only.
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const OUT = resolve(here, '../src/testing/recordings');
const REPO = resolve(here, '../../..');
const IMG = join(REPO, 'assets/example-images');
const BASE = process.env.BASE_URL ?? 'http://localhost:8080';
const COMMIT = process.env.COMMIT ?? 'unknown';
const NOTE = process.env.INSTANCE_NOTE ?? 'RATE_LIMIT_CASES_PER_HOUR=1000, RATE_LIMIT_CHAT_PER_HOUR=1000, fixed CASE_SIGNING_KEY, DECISIONS_MODE=shadow';
const MODE = process.env.DECISIONS_MODE_NOTE ?? 'shadow';
mkdirSync(OUT, { recursive: true });

const today = new Date().toLocaleDateString('en-CA', { timeZone: 'Europe/Warsaw' });
const daysAgo = (n) => {
  const d = new Date(`${today}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() - n);
  return d.toISOString().slice(0, 10);
};

const mime = { png: 'image/png', webp: 'image/webp', jpg: 'image/jpeg', jpeg: 'image/jpeg' };

function writeMeta(name, endpoint, request) {
  const md = `# ${name}

- Recorded: ${new Date().toISOString()} (Europe/Warsaw date ${today})
- App commit: ${COMMIT}
- Endpoint: ${endpoint}
- Request: ${request}
- Instance env: ${NOTE}
- DECISIONS_MODE: ${MODE}
- Data: synthetic form values, real photos from assets/example-images only. Responses saved verbatim from the real backend and real OpenRouter.
`;
  writeFileSync(join(OUT, `${name}.meta.md`), md);
}

function headersObject(res) {
  const o = {};
  for (const [k, v] of res.headers.entries()) o[k] = v;
  return o;
}

async function saveJson(name, res, endpoint, request) {
  const raw = await res.text();
  let body;
  try { body = JSON.parse(raw); } catch { body = raw; }
  writeFileSync(join(OUT, `${name}.json`), JSON.stringify({ status: res.status, headers: headersObject(res), body }, null, 2) + '\n');
  writeFileSync(join(OUT, `${name}.body.raw`), raw);
  writeMeta(name, endpoint, request);
  console.log(`${name}: HTTP ${res.status}`);
  return { status: res.status, body };
}

async function saveSse(name, res, endpoint, request) {
  const raw = await res.text();
  writeFileSync(join(OUT, `${name}.sse`), raw);
  writeFileSync(join(OUT, `${name}.headers.json`), JSON.stringify({ status: res.status, headers: headersObject(res) }, null, 2) + '\n');
  writeMeta(name, endpoint, request);
  console.log(`${name}: HTTP ${res.status}, ${raw.length} bytes, events: ${[...raw.matchAll(/^event:\s*(\S+)/gm)].map((m) => m[1]).join(',')}`);
  return { status: res.status, raw };
}

const load = (name) => JSON.parse(readFileSync(join(OUT, `${name}.json`), 'utf8'));

async function postAssessment(form, photoBytes, photoName, photoType) {
  const fd = new FormData();
  fd.append('form', new Blob([JSON.stringify(form)], { type: 'application/json' }));
  fd.append('photo', new Blob([photoBytes], { type: photoType }), photoName);
  return fetch(`${BASE}/api/cases/assessments`, { method: 'POST', body: fd });
}

const photo = (file) => ({ bytes: readFileSync(join(IMG, file)), name: file, type: mime[file.split('.').pop()] });
const formSummary = (f, p) => `multipart form=${JSON.stringify(f)}, photo=${p}`;

async function assessment(name, form, file) {
  const p = photo(file);
  const res = await postAssessment(form, p.bytes, p.name, p.type);
  return saveJson(name, res, 'POST /api/cases/assessments', formSummary(form, file));
}

async function postDecision(ctx, signature) {
  return fetch(`${BASE}/api/cases/decisions`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ caseContext: ctx, signature }),
  });
}

async function decision(name, assessmentName) {
  const a = load(assessmentName).body;
  const res = await postDecision(a.caseContext, a.signature);
  return saveJson(name, res, 'POST /api/cases/decisions', `JSON {caseContext, signature} taken verbatim from ${assessmentName}.json`);
}

async function chat(name, decisionName, userText, extra = {}) {
  const d = load(decisionName).body;
  const body = {
    caseContext: d.caseContext, signature: d.signature,
    messages: [
      { id: 'm1', role: 'assistant', text: d.decision.message },
      { id: 'm2', role: 'user', text: userText },
    ],
    ...extra,
  };
  const res = await fetch(`${BASE}/api/chat/messages`, {
    method: 'POST', headers: { 'content-type': 'application/json', accept: 'text/event-stream' },
    body: JSON.stringify(body),
  });
  return { res, body };
}

const returnForm = (model, days, reason = '') => ({
  caseType: 'RETURN', category: 'LAPTOP', modelName: model, purchaseDate: daysAgo(days), reason,
});

const scenarios = {
  async health() {
    const start = Date.now();
    for (let i = 0; i < 120; i++) {
      try {
        const r = await fetch(`${BASE}/actuator/health`);
        const t = await r.text();
        if (r.status === 200 && t.includes('"UP"')) { console.log(`health UP after ${Date.now() - start} ms (polling)`); return; }
      } catch { /* not up yet */ }
      await new Promise((r) => setTimeout(r, 500));
    }
    console.log('health: NOT UP'); process.exit(1);
  },

  async 'return-ok'() {
    await assessment('assessment-return-ok', returnForm('Lenovo ThinkPad T14 Gen 4', 5), 'laptop-2.webp');
    await decision('decision-return-ok', 'assessment-return-ok');
  },
  async 'complaint-ok'() {
    await assessment('assessment-complaint-ok', {
      caseType: 'COMPLAINT', category: 'SMARTPHONE', modelName: 'Samsung Galaxy S24', purchaseDate: daysAgo(60),
      reason: 'Ekran pękł po upadku z biurka.',
    }, 'phone-1.jpg');
    await decision('decision-complaint-ok', 'assessment-complaint-ok');
  },
  async insufficient() {
    await assessment('assessment-insufficient', returnForm('Lenovo ThinkPad T14 Gen 4', 5), 'phone-1.jpg');
    await decision('decision-insufficient', 'assessment-insufficient');
  },
  async 'return-more-info'() {
    await assessment('assessment-return-more-info', returnForm('Lenovo ThinkPad T14 Gen 4', 20), 'laptop-2.webp');
    await decision('decision-return-more-info', 'assessment-return-more-info');
  },

  // No LLM call happens in any of these.
  async problems() {
    // 400 validation
    const bad = { caseType: 'COMPLAINT', category: 'SMARTPHONE', modelName: '', purchaseDate: daysAgo(-3), reason: 'abc' };
    const p = photo('phone-1.jpg');
    let res = await postAssessment(bad, p.bytes, p.name, p.type);
    await saveJson('problem-400-validation', res, 'POST /api/cases/assessments', formSummary(bad, 'phone-1.jpg'));

    // 400 unsupported image: real non-image text bytes named photo.jpg
    const text = readFileSync(join(REPO, 'docs/README.md'));
    const ok = returnForm('Lenovo ThinkPad T14 Gen 4', 5);
    res = await postAssessment(ok, text, 'photo.jpg', 'image/jpeg');
    await saveJson('problem-400-unsupported-image', res, 'POST /api/cases/assessments',
      `${formSummary(ok, 'photo.jpg = bytes of docs/README.md (text, not an image), content-type image/jpeg')}`);

    // 413: > 10 MB of the real laptop-1.png bytes
    const l1 = readFileSync(join(IMG, 'laptop-1.png'));
    const parts = [];
    let size = 0;
    while (size <= 10 * 1024 * 1024) { parts.push(l1); size += l1.length; }
    res = await postAssessment(ok, Buffer.concat(parts), 'laptop-1.png', 'image/png');
    await saveJson('problem-413-too-large', res, 'POST /api/cases/assessments',
      `${formSummary(ok, `laptop-1.png bytes repeated x${parts.length} = ${size} bytes`)}`);

    // 401 signature: one changed field
    const a = load('assessment-return-ok').body;
    const tampered = structuredClone(a.caseContext);
    tampered.form.modelName = 'Lenovo ThinkPad T14 Gen 5';
    res = await postDecision(tampered, a.signature);
    await saveJson('problem-401-signature', res, 'POST /api/cases/decisions',
      'caseContext from assessment-return-ok.json with form.modelName changed to "Lenovo ThinkPad T14 Gen 5"; original signature');

    // 409 decided
    const d = load('decision-return-ok').body;
    res = await postDecision(d.caseContext, d.signature);
    await saveJson('problem-409-decided', res, 'POST /api/cases/decisions',
      'caseContext + signature from decision-return-ok.json response (already decided)');

    // chat 400 history: last message is not a user message
    const base = {
      caseContext: d.caseContext, signature: d.signature,
      messages: [{ id: 'm1', role: 'assistant', text: d.decision.message }],
    };
    res = await fetch(`${BASE}/api/chat/messages`, {
      method: 'POST', headers: { 'content-type': 'application/json', accept: 'text/event-stream' }, body: JSON.stringify(base),
    });
    await saveJson('problem-400-chat-history', res, 'POST /api/chat/messages', 'history = [assistant decision message] only (last message not from user)');

    // chat 401 signature
    const t2 = structuredClone(d.caseContext);
    t2.form.modelName = 'Lenovo ThinkPad T14 Gen 5';
    res = await fetch(`${BASE}/api/chat/messages`, {
      method: 'POST', headers: { 'content-type': 'application/json', accept: 'text/event-stream' },
      body: JSON.stringify({ ...base, caseContext: t2, messages: [...base.messages, { id: 'm2', role: 'user', text: 'Jak mam zapakować urządzenie?' }] }),
    });
    await saveJson('problem-401-chat-signature', res, 'POST /api/chat/messages',
      'caseContext from decision-return-ok.json with form.modelName changed; original signature; valid history');
  },

  async 'problem-429'() {
    const text = readFileSync(join(REPO, 'docs/README.md'));
    const ok = returnForm('Lenovo ThinkPad T14 Gen 4', 5);
    const first = await postAssessment(ok, text, 'photo.jpg', 'image/jpeg');
    console.log(`first request (not saved): HTTP ${first.status}`);
    await first.text();
    const res = await postAssessment(ok, text, 'photo.jpg', 'image/jpeg');
    await saveJson('problem-429', res, 'POST /api/cases/assessments',
      `second request of the hour; ${formSummary(ok, 'photo.jpg = bytes of docs/README.md (text), so no LLM call')}`);
  },

  async 'problem-502'() {
    await assessment('problem-502-llm-failed', returnForm('Lenovo ThinkPad T14 Gen 4', 5), 'laptop-2.webp');
  },
  async 'problem-504'() {
    await assessment('problem-504-llm-timeout', returnForm('Lenovo ThinkPad T14 Gen 4', 5), 'laptop-2.webp');
  },

  async 'chat-plain'() {
    const { res } = await chat('chat-plain', 'decision-return-ok', 'Jak mam zapakować urządzenie?');
    await saveSse('chat-plain', res, 'POST /api/chat/messages',
      'history = [assistant message (decision-return-ok decision.message), user "Jak mam zapakować urządzenie?"]; caseContext+signature from decision-return-ok.json');
  },
  async 'chat-decision-update'() {
    const text = 'Urządzenie zostało dostarczone 3 dni temu.';
    const { res } = await chat('chat-decision-update', 'decision-return-more-info', text);
    await saveSse('chat-decision-update', res, 'POST /api/chat/messages',
      `RETURN case purchased 20 days ago (decision-return-more-info); history = [assistant decision message, user "${text}"]`);
  },
  async 'chat-error'() {
    const { res } = await chat('chat-error', 'decision-return-ok', 'Jak mam zapakować urządzenie?');
    await saveSse('chat-error', res, 'POST /api/chat/messages',
      'same request as chat-plain, sent to an instance with an invalid OpenRouter key (same CASE_SIGNING_KEY) so the stream ends with event: error');
  },
};

const name = process.argv[2];
if (!name || !scenarios[name]) {
  console.error(`Usage: record-backend.mjs <${Object.keys(scenarios).join('|')}>`);
  process.exit(2);
}
await scenarios[name]();
