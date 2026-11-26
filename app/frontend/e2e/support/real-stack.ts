import { spawn, type ChildProcess } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

/**
 * Helpers that start/stop extra REAL JAR instances (ADR-005 D10 faulty-configuration instances and the
 * DECISIONS_MODE matrix). Nothing here intercepts or fakes traffic: every instance still talks to the real
 * OpenRouter. Extra instances use ports 8081+ (8080 is the Playwright config webServer).
 */

export const FRONTEND_DIR = path.resolve(__dirname, '../..');
export const BACKEND_DIR = path.resolve(FRONTEND_DIR, '../backend');
export const LOG_DIR = path.join(FRONTEND_DIR, 'test-results', 'real-stack');
export const CASE_SIGNING_KEY = 'ZTJlLWZpeGVkLXNpZ25pbmcta2V5LTMyLWJ5dGVzISE='; // same as playwright.config.ts

export interface InstanceOptions {
  /** Short name used in the log file name. */
  name: string;
  port: number;
  /** Extra/overriding environment variables. */
  env?: Record<string, string>;
  /** Extra JVM application arguments, e.g. `--llm.timeouts.vision=1ms`. */
  args?: string[];
  /** Health wait timeout (default 60 s). */
  startTimeoutMs?: number;
}

export interface RealInstance {
  name: string;
  port: number;
  baseURL: string;
  logFile: string;
  stop(): Promise<void>;
}

export function jarPath(): string {
  const dir = path.join(BACKEND_DIR, 'target');
  const jar = fs
    .readdirSync(dir)
    .find((f) => /^service-copilot-.*\.jar$/.test(f) && !f.endsWith('.original'));
  if (!jar) throw new Error(`No service-copilot JAR in ${dir}; run ./mvnw -B verify first`);
  return path.join(dir, jar);
}

const running = new Map<number, ChildProcess>();

function pidFile(port: number): string {
  return path.join(LOG_DIR, `.pid-${port}`);
}

async function waitForHealth(baseURL: string, timeoutMs: number, child: ChildProcess): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) throw new Error(`JAR exited early with code ${child.exitCode}`);
    try {
      const res = await fetch(`${baseURL}/actuator/health`);
      if (res.ok && (await res.json()).status === 'UP') return;
    } catch {
      /* not up yet */
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`${baseURL}/actuator/health not UP within ${timeoutMs} ms`);
}

/** Starts a real JAR instance and resolves once /actuator/health is UP. */
export async function startInstance(opts: InstanceOptions): Promise<RealInstance> {
  fs.mkdirSync(LOG_DIR, { recursive: true });
  const logFile = path.join(LOG_DIR, `${opts.name}-${opts.port}.log`);
  const out = fs.openSync(logFile, 'a');
  const child = spawn('java', ['-jar', jarPath(), ...(opts.args ?? [])], {
    cwd: BACKEND_DIR,
    detached: true, // own process group
    stdio: ['ignore', out, out],
    env: {
      ...process.env,
      SERVER_PORT: String(opts.port),
      RATE_LIMIT_CASES_PER_HOUR: '1000',
      RATE_LIMIT_CHAT_PER_HOUR: '1000',
      CASE_SIGNING_KEY,
      OPENROUTER_API_KEY: process.env.OPENROUTER_API_KEY ?? '',
      ...(opts.env ?? {}),
    },
  });
  fs.closeSync(out);
  running.set(opts.port, child);
  fs.writeFileSync(pidFile(opts.port), String(child.pid));
  const baseURL = `http://localhost:${opts.port}`;
  try {
    await waitForHealth(baseURL, opts.startTimeoutMs ?? 60_000, child);
  } catch (e) {
    await killGroup(child);
    running.delete(opts.port);
    throw e;
  }
  return {
    name: opts.name,
    port: opts.port,
    baseURL,
    logFile,
    stop: async () => {
      await killGroup(child);
      running.delete(opts.port);
      try {
        fs.unlinkSync(pidFile(opts.port));
      } catch {
        /* already gone */
      }
    },
  };
}

async function killGroup(child: ChildProcess): Promise<void> {
  if (child.pid === undefined || child.exitCode !== null) return;
  const exited = new Promise<void>((resolve) => child.once('exit', () => resolve()));
  try {
    process.kill(-child.pid, 'SIGTERM');
  } catch {
    /* gone */
  }
  const timer = setTimeout(() => {
    try {
      process.kill(-child.pid!, 'SIGKILL');
    } catch {
      /* gone */
    }
  }, 8_000);
  await exited;
  clearTimeout(timer);
}

/** Kills JARs recorded by pid files that a crashed worker left behind (used by global teardown). */
export function killLeftovers(): number[] {
  const killed: number[] = [];
  if (!fs.existsSync(LOG_DIR)) return killed;
  for (const f of fs.readdirSync(LOG_DIR).filter((n) => n.startsWith('.pid-'))) {
    const pid = Number(fs.readFileSync(path.join(LOG_DIR, f), 'utf8'));
    try {
      process.kill(-pid, 'SIGTERM');
      killed.push(pid);
    } catch {
      /* not running */
    }
    fs.unlinkSync(path.join(LOG_DIR, f));
  }
  return killed;
}

// ---- ADR-005 D10 instances and the DECISIONS_MODE matrix (each still reaches the real OpenRouter)

type Base = { port: number };

/** Real 401 from OpenRouter -> 502 LLM_FAILED (AC-41, AC-43). */
export const invalidKey = ({ port }: Base) =>
  startInstance({ name: 'invalid-key', port, env: { OPENROUTER_API_KEY: 'sk-or-v1-invalid-e2e' } });

/** Real key, vision step timeout 1 ms -> 504 LLM_TIMEOUT (AC-41). */
export const visionTimeout1ms = ({ port }: Base) =>
  startInstance({ name: 'vision-timeout-1ms', port, args: ['--llm.timeouts.vision=1ms'] });

/** Second assessment is rejected by the real filter -> 429 RATE_LIMITED. */
export const rateLimit1 = ({ port }: Base) =>
  startInstance({ name: 'rate-limit-1', port, env: { RATE_LIMIT_CASES_PER_HOUR: '1' } });

/** Real OpenRouter host answers 404 for the Decisions path; the case continues through the vision LLM (AC-49). */
export const decisionsNotFound = ({ port }: Base) =>
  startInstance({
    name: 'decisions-not-found',
    port,
    env: {
      DECISIONS_MODE: 'gated',
      DECISIONS_URL: 'https://openrouter.ai/api/alpha/decisions-nonexistent-e2e',
    },
  });

export const shadow = ({ port }: Base) => startInstance({ name: 'shadow', port, env: { DECISIONS_MODE: 'shadow' } });
export const gated = ({ port }: Base) => startInstance({ name: 'gated', port, env: { DECISIONS_MODE: 'gated' } });
export const llmOnly = ({ port }: Base) => startInstance({ name: 'llm-only', port, env: { DECISIONS_MODE: 'llm-only' } });

/** Reads an instance log (empty string when missing). */
export function readLog(instance: { logFile: string }): string {
  return fs.existsSync(instance.logFile) ? fs.readFileSync(instance.logFile, 'utf8') : '';
}
