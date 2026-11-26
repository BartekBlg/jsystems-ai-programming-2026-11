import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiError, toApiError } from './api.service';
import { SseEvent, SseParser } from './sse-parser';
import { CaseContext, ChatRequest, ChatRequestMessage, ChatStreamEvent, DecisionCategory } from './models';

/**
 * POST /api/chat/messages with native fetch and a streamed response (ADR-003 §3.5).
 * Completes after `done`; errors after `error`, a pre-stream problem, a network failure or an abort.
 */
@Injectable({ providedIn: 'root' })
export class ChatStreamService {
  send(
    caseContext: CaseContext,
    signature: string,
    messages: ChatRequestMessage[],
    signal?: AbortSignal,
  ): Observable<ChatStreamEvent> {
    return new Observable<ChatStreamEvent>((subscriber) => {
      const controller = new AbortController();
      const abortFromOutside = (): void => controller.abort();
      if (signal?.aborted) {
        controller.abort();
      } else {
        signal?.addEventListener('abort', abortFromOutside, { once: true });
      }
      let reader: ReadableStreamDefaultReader<string> | null = null;
      let finished = false;

      const fail = (error: ApiError): void => {
        if (!finished) {
          finished = true;
          subscriber.error(error);
        }
      };

      const run = async (): Promise<void> => {
        const request: ChatRequest = { caseContext, signature, messages };
        const response = await fetch('/api/chat/messages', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
          body: JSON.stringify(request),
          signal: controller.signal,
        });
        if (!response.ok) {
          let body: unknown = null;
          try {
            body = await response.json();
          } catch {
            // not problem+json: falls through to INTERNAL
          }
          fail(toApiError(response.status, body));
          return;
        }
        if (!response.body) {
          fail(new ApiError(response.status, 'INTERNAL', ''));
          return;
        }
        reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
        const parser = new SseParser();
        while (!finished) {
          const { done, value } = await reader.read();
          if (done) {
            break;
          }
          for (const event of parser.push(value)) {
            if (handle(event)) {
              return;
            }
          }
        }
        for (const event of parser.flush()) {
          if (handle(event)) {
            return;
          }
        }
        fail(new ApiError(response.status, 'INTERNAL', ''));
      };

      /** Returns true when the stream is finished (done or error). */
      const handle = (event: SseEvent): boolean => {
        let data: unknown;
        try {
          data = JSON.parse(event.data);
        } catch {
          if (event.event === 'decision' || event.event === 'delta' || event.event === 'done' || event.event === 'error') {
            fail(new ApiError(200, 'INTERNAL', ''));
            void reader?.cancel().catch(() => undefined);
            return true;
          }
          return false;
        }
        const record = (typeof data === 'object' && data !== null ? data : {}) as Record<string, unknown>;
        switch (event.event) {
          case 'decision':
            subscriber.next({ type: 'decision', category: record['category'] as DecisionCategory });
            return false;
          case 'delta':
            subscriber.next({ type: 'delta', text: String(record['text'] ?? '') });
            return false;
          case 'done':
            finished = true;
            subscriber.next({ type: 'done', category: record['category'] as DecisionCategory });
            subscriber.complete();
            void reader?.cancel().catch(() => undefined);
            return true;
          case 'error': {
            const code = record['code'] === 'LLM_TIMEOUT' ? 'LLM_TIMEOUT' : 'LLM_FAILED';
            fail(new ApiError(200, code, ''));
            void reader?.cancel().catch(() => undefined);
            return true;
          }
          default:
            return false;
        }
      };

      controller.signal.addEventListener(
        'abort',
        () => {
          fail(new ApiError(0, 'INTERNAL', ''));
          void reader?.cancel().catch(() => undefined);
        },
        { once: true },
      );
      if (controller.signal.aborted) {
        fail(new ApiError(0, 'INTERNAL', ''));
      }

      run().catch(() => fail(new ApiError(0, 'INTERNAL', '')));

      return () => {
        finished = true;
        signal?.removeEventListener('abort', abortFromOutside);
        controller.abort();
        void reader?.cancel().catch(() => undefined);
      };
    });
  }
}
