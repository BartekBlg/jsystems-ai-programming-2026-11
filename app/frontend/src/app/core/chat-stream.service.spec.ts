import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  loadJsonRecording,
  loadSseRecording,
  responseFromRecording,
  sseResponseFromRecording,
  sseResponseFromText,
  sseResponseHanging,
} from '../../testing/recording-loader';
import { ApiError } from './api.service';
import { ChatStreamService } from './chat-stream.service';
import { AssessmentResponse, ChatRequestMessage, ChatStreamEvent } from './models';

interface Outcome {
  events: ChatStreamEvent[];
  error?: unknown;
  completed: boolean;
}

function collect(source: Observable<ChatStreamEvent>): Promise<Outcome> {
  return new Promise((resolve) => {
    const outcome: Outcome = { events: [], completed: false };
    source.subscribe({
      next: (event) => outcome.events.push(event),
      error: (error: unknown) => resolve({ ...outcome, error }),
      complete: () => resolve({ ...outcome, completed: true }),
    });
  });
}

const HISTORY: ChatRequestMessage[] = [{ id: 'u1', role: 'user', text: 'Pytanie testowe' }];

describe('ChatStreamService', () => {
  let service: ChatStreamService;
  let fetchMock: ReturnType<typeof vi.fn>;
  let context: AssessmentResponse;

  beforeEach(async () => {
    service = TestBed.inject(ChatStreamService);
    context = (await loadJsonRecording<AssessmentResponse>('assessment-return-ok')).body;
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const send = (signal?: AbortSignal) => service.send(context.caseContext, context.signature, HISTORY, signal);

  it('POSTs the JSON request with the SSE accept header', async () => {
    fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-plain'));
    await collect(send());

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/chat/messages');
    expect(init.method).toBe('POST');
    expect(init.headers).toEqual({ 'Content-Type': 'application/json', Accept: 'text/event-stream' });
    expect(JSON.parse(init.body as string)).toEqual({
      caseContext: context.caseContext,
      signature: context.signature,
      messages: HISTORY,
    });
  });

  for (const name of ['chat-plain', 'chat-decision-update']) {
    it(`${name}: whole stream and one-byte chunks give identical events (TAC-003-03)`, async () => {
      fetchMock.mockResolvedValueOnce(await sseResponseFromRecording(name, 'whole'));
      const whole = await collect(send());
      fetchMock.mockResolvedValueOnce(await sseResponseFromRecording(name, 'bytes'));
      const bytes = await collect(send());

      expect(whole.completed).toBe(true);
      expect(whole.events.length).toBeGreaterThan(2);
      expect(bytes.events).toEqual(whole.events);
      expect(bytes.completed).toBe(true);
      expect(whole.events.at(-1)?.type).toBe('done');
    });
  }

  it('chat-plain: deltas reassemble the recorded text and end with done', async () => {
    fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-plain'));
    const { events } = await collect(send());
    const recorded = (await loadSseRecording('chat-plain')).text;
    const expectedText = [...recorded.matchAll(/^data: (\{"text".*\})$/gm)]
      .map((m) => (JSON.parse(m[1]) as { text: string }).text)
      .join('');
    const text = events.flatMap((e) => (e.type === 'delta' ? [e.text] : [])).join('');
    expect(text).toBe(expectedText);
    expect(events.some((e) => e.type === 'decision')).toBe(false);
    expect(events.at(-1)).toEqual({ type: 'done', category: 'ESCALATED' });
  });

  it('chat-decision-update: the decision event comes first', async () => {
    fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-decision-update'));
    const { events } = await collect(send());
    expect(events[0]).toEqual({ type: 'decision', category: 'ESCALATED' });
    expect(events[1].type).toBe('delta');
  });

  it('chat-error: an error event errors the observable with the code', async () => {
    fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-error'));
    const outcome = await collect(send());
    expect(outcome.completed).toBe(false);
    expect(outcome.events).toEqual([]);
    expect(outcome.error).toBeInstanceOf(ApiError);
    expect((outcome.error as ApiError).code).toBe('LLM_FAILED');
  });

  it('non-2xx before the stream (401 problem) becomes a typed error with the code', async () => {
    fetchMock.mockResolvedValue(await responseFromRecording('problem-401-chat-signature'));
    const outcome = await collect(send());
    const error = outcome.error as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(401);
    expect(error.code).toBe('INVALID_CASE_SIGNATURE');
  });

  it('400 chat-history problem keeps its code', async () => {
    fetchMock.mockResolvedValue(await responseFromRecording('problem-400-chat-history'));
    const rec = await loadJsonRecording<{ code: string }>('problem-400-chat-history');
    expect(((await collect(send())).error as ApiError).code).toBe(rec.body.code);
  });

  it('a rejected fetch (network failure) errors with INTERNAL', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));
    const error = (await collect(send())).error as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.code).toBe('INTERNAL');
  });

  it('a stream cut mid-way (network error after some events) errors with INTERNAL', async () => {
    // Derived from chat-plain.sse: only the first two events are delivered, then the body stream errors.
    const rec = await loadSseRecording('chat-plain');
    const cut = rec.text.split('\n\n').slice(0, 2).join('\n\n') + '\n\n';
    fetchMock.mockResolvedValue(sseResponseFromText(cut, rec.headers, 'whole', undefined, true));
    const outcome = await collect(send());
    expect(outcome.events).toHaveLength(2);
    expect((outcome.error as ApiError).code).toBe('INTERNAL');
  });

  it('a stream that closes without done errors with INTERNAL', async () => {
    // Derived from chat-plain.sse: truncated before the done event, stream closes cleanly.
    const rec = await loadSseRecording('chat-plain');
    const cut = rec.text.slice(0, rec.text.indexOf('event:done'));
    fetchMock.mockResolvedValue(sseResponseFromText(cut, rec.headers));
    const outcome = await collect(send());
    expect(outcome.completed).toBe(false);
    expect((outcome.error as ApiError).code).toBe('INTERNAL');
  });

  it('unsubscribing aborts the fetch and cancels the reader', async () => {
    const cancelled = vi.fn();
    let fetchSignal: AbortSignal | undefined;
    fetchMock.mockImplementation(async (_url: string, init: RequestInit) => {
      fetchSignal = init.signal as AbortSignal;
      return sseResponseHanging('chat-plain', cancelled);
    });
    const events: ChatStreamEvent[] = [];
    const subscription = send().subscribe((e) => events.push(e));
    await vi.waitFor(() => expect(events.length).toBe(1));

    subscription.unsubscribe();

    expect(fetchSignal?.aborted).toBe(true);
    await vi.waitFor(() => expect(cancelled).toHaveBeenCalled());
  });

  it('an external abort signal errors the observable and cancels the reader', async () => {
    const cancelled = vi.fn();
    fetchMock.mockImplementation(async () => sseResponseHanging('chat-plain', cancelled));
    const controller = new AbortController();
    const promise = collect(send(controller.signal));
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalled());
    await new Promise((r) => setTimeout(r, 20));

    controller.abort();

    const outcome = await promise;
    expect((outcome.error as ApiError).code).toBe('INTERNAL');
    await vi.waitFor(() => expect(cancelled).toHaveBeenCalled());
  });

  it('an already aborted signal errors without a result', async () => {
    fetchMock.mockImplementation((_u: string, init: RequestInit) =>
      (init.signal as AbortSignal).aborted ? Promise.reject(new DOMException('aborted', 'AbortError')) : Promise.resolve(new Response()),
    );
    const controller = new AbortController();
    controller.abort();
    expect(((await collect(send(controller.signal))).error as ApiError).code).toBe('INTERNAL');
  });
});
