/**
 * Loads T26 backend recordings (src/testing/recordings) for specs.
 * Vitest runs in Node, so the files are read with fs; nothing here fabricates a response.
 */

// The project has no @types/node; the modules are resolved by Node at run time through a non-literal specifier.
interface NodeFs {
  readFileSync(path: string, encoding: 'utf8'): string;
}
const nodeFsModule = 'node:fs';

async function readFile(path: string): Promise<string> {
  const fs = (await import(/* @vite-ignore */ nodeFsModule)) as NodeFs;
  return fs.readFileSync(path, 'utf8');
}

/** Vitest runs from the frontend project root. */
const cwd = (globalThis as unknown as { process: { cwd(): string } }).process.cwd();
const RECORDINGS_DIR = `${cwd}/src/testing/recordings`;

export interface JsonRecording<T = object> {
  status: number;
  headers: Record<string, string>;
  body: T;
}

export function readRecordingText(file: string): Promise<string> {
  return readFile(`${RECORDINGS_DIR}/${file}`);
}

/** `<name>.json` = {status, headers, body}. */
export async function loadJsonRecording<T = object>(name: string): Promise<JsonRecording<T>> {
  return JSON.parse(await readRecordingText(`${name}.json`)) as JsonRecording<T>;
}

/** The exact body text of a JSON recording (`<name>.body.raw`). */
export function loadRawBody(name: string): Promise<string> {
  return readRecordingText(`${name}.body.raw`);
}

/** `<name>.sse` text (exact stream) with the recorded headers. */
export async function loadSseRecording(
  name: string,
): Promise<{ status: number; headers: Record<string, string>; text: string }> {
  const meta = JSON.parse(await readRecordingText(`${name}.headers.json`)) as JsonRecording<never>;
  return { status: meta.status, headers: meta.headers, text: await readRecordingText(`${name}.sse`) };
}

/** A fetch `Response` carrying a JSON recording's status, headers and exact body text. */
export async function responseFromRecording(name: string): Promise<Response> {
  const rec = await loadJsonRecording(name);
  return new Response(await loadRawBody(name), { status: rec.status, headers: rec.headers });
}

export type SseChunking = 'whole' | 'bytes';

/**
 * A fetch `Response` streaming an SSE recording's exact bytes: as one chunk ('whole') or one byte per chunk
 * ('bytes', TAC-003-03). `onCancel` fires when the consumer cancels the body reader.
 */
export async function sseResponseFromRecording(
  name: string,
  chunking: SseChunking = 'whole',
  onCancel?: () => void,
): Promise<Response> {
  const rec = await loadSseRecording(name);
  return sseResponseFromText(rec.text, rec.headers, chunking, onCancel);
}

/** Builds an SSE `Response` from (a prefix or other derivation of) recorded text. */
export function sseResponseFromText(
  text: string,
  headers: Record<string, string>,
  chunking: SseChunking = 'whole',
  onCancel?: () => void,
  /** When set, the stream errors after the chunks instead of closing (network failure). */
  failAfter?: boolean,
): Response {
  const bytes = new TextEncoder().encode(text);
  const chunks = chunking === 'whole' ? [bytes] : Array.from(bytes, (b) => new Uint8Array([b]));
  let index = 0;
  const body = new ReadableStream<Uint8Array>({
    pull(controller) {
      if (index < chunks.length) {
        controller.enqueue(chunks[index++]);
      } else if (failAfter) {
        controller.error(new TypeError('network error'));
      } else {
        controller.close();
      }
    },
    cancel() {
      onCancel?.();
    },
  });
  return new Response(body, { status: 200, headers });
}

/** A stream that delivers the recording's first event and then stays open until cancelled/aborted. */
export async function sseResponseHanging(name: string, onCancel: () => void): Promise<Response> {
  const rec = await loadSseRecording(name);
  const first = new TextEncoder().encode(rec.text.slice(0, rec.text.indexOf('\n\n') + 2));
  let sent = false;
  const body = new ReadableStream<Uint8Array>({
    pull(controller) {
      if (!sent) {
        sent = true;
        controller.enqueue(first);
        return undefined;
      }
      return new Promise<void>(() => undefined);
    },
    cancel() {
      onCancel();
    },
  });
  return new Response(body, { status: 200, headers: rec.headers });
}
