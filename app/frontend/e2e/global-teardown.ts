import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { BACKEND_DIR, FRONTEND_DIR, LOG_DIR, killLeftovers } from './support/real-stack';

/**
 * "Proof that calls were real" (plan §4.1). Parses the collected application logs and asserts what applies to the
 * specs that actually ran; tolerant when only API specs ran or when the :8080 instance was reused (no log).
 * Log lines carry numbers and ids only (ADR-000 Decision 8.7), never text content.
 */

const ERROR_INSTANCES = /invalid-key|vision-timeout|decisions-not-found/;

function walk(dir: string): string[] {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = path.join(dir, e.name);
    return e.isDirectory() ? walk(p) : [p];
  });
}

export default async function globalTeardown(): Promise<void> {
  const problems: string[] = [];
  killLeftovers();

  // 1. Application logs
  const logs = fs.existsSync(LOG_DIR) ? fs.readdirSync(LOG_DIR).filter((f) => f.endsWith('.log')) : [];
  const info: string[] = [];
  for (const file of logs) {
    const text = fs.readFileSync(path.join(LOG_DIR, file), 'utf8');
    const usage = text.split('\n').filter((l) => /\bllm\.usage\b/.test(l) && /step=/.test(l));
    const steps = new Set<string>();
    for (const line of usage) {
      const m = /step=(\w+).*inputTokens=(\d+) outputTokens=(\d+)/.exec(line);
      if (!m) {
        problems.push(`${file}: llm.usage line without numeric token counts: ${line.slice(0, 160)}`);
        continue;
      }
      steps.add(m[1]);
    }
    const shadow = (text.match(/event=prescreen\.shadow(?!\.skipped)\b/g) ?? []).length;
    const gated = (text.match(/event=prescreen\.gated\b/g) ?? []).length;
    info.push(`${file}: usage steps=[${[...steps].join(',')}] lines=${usage.length} prescreen.shadow=${shadow} prescreen.gated=${gated}`);

    if (ERROR_INSTANCES.test(file)) continue; // faulty-configuration instances: failures are expected
    // A model step that ran must have a usage line: a real vision call implies a decision call unless the case was
    // rejected earlier, so only require the vision step when an assessment was served (usage present) at all.
    if (usage.length > 0 && !steps.has('vision')) problems.push(`${file}: LLM usage present but no vision step line`);
    if (/shadow-\d+\.log$|main-\d+\.log$/.test(file) && steps.has('vision') && shadow === 0) {
      problems.push(`${file}: shadow run completed vision calls but logged no prescreen.shadow event`);
    }
    if (/gated-\d+\.log$/.test(file) && usage.length + gated > 0 && gated === 0) {
      problems.push(`${file}: gated run logged no prescreen.gated event`);
    }
    if (/llm-only-\d+\.log$/.test(file) && /event=prescreen\./.test(text)) {
      problems.push(`${file}: llm-only run contains prescreen.* events (a Decisions call happened)`);
    }
  }

  // 2. No test doubles in the built JAR
  const jarDir = path.join(BACKEND_DIR, 'target');
  const jar = fs.existsSync(jarDir)
    ? fs.readdirSync(jarDir).find((f) => /^service-copilot-.*\.jar$/.test(f) && !f.endsWith('.original'))
    : undefined;
  if (jar) {
    const listing = execFileSync('jar', ['tf', path.join(jarDir, jar)], { maxBuffer: 256 * 1024 * 1024 }).toString();
    const doubles = listing.split('\n').filter((l) => /wiremock|mockito/i.test(l));
    if (doubles.length) problems.push(`JAR contains test doubles, e.g. ${doubles.slice(0, 3).join(', ')}`);
    info.push(`jar tf ${jar}: ${doubles.length} wiremock/mockito entries`);
  }

  // 3. No interception anywhere under e2e/ (this file's own pattern is built from parts so it does not match itself)
  const forbidden = new RegExp(['page\\.route', 'route\\.fulfill', 'context\\.route', 'serviceWorkers', 'routeFromHAR'].join('|'));
  const hits: string[] = [];
  for (const file of walk(path.join(FRONTEND_DIR, 'e2e')).filter((f) => f.endsWith('.ts') && !f.endsWith('global-teardown.ts'))) {
    fs.readFileSync(file, 'utf8')
      .split('\n')
      .forEach((line, i) => {
        if (forbidden.test(line) && !/^\s*(\/\/|\*)/.test(line)) hits.push(`${path.relative(FRONTEND_DIR, file)}:${i + 1}`);
      });
  }
  if (hits.length) problems.push(`interception found: ${hits.join(', ')}`);
  info.push(`interception grep over e2e/: ${hits.length} hits`);

  // eslint-disable-next-line no-console
  console.log(`[real-stack proof]\n  ${info.join('\n  ')}`);
  if (problems.length) throw new Error(`Real-stack proof failed:\n  ${problems.join('\n  ')}`);
}
