import { provideHttpClient, withFetch } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  loadJsonRecording,
  loadSseRecording,
  responseFromRecording,
  sseResponseFromRecording,
  sseResponseFromText,
  sseResponseHanging,
} from '../../testing/recording-loader';
import { CaseStore } from './case-store';
import { LABELS } from './labels';
import {
  AssessmentResponse,
  CaseForm,
  DecisionResponse,
  FORM_DRAFT_KEY,
  FieldError,
  STORED_CASE_KEY,
  StoredCase,
} from './models';

const FORM: CaseForm = {
  caseType: 'RETURN',
  category: 'LAPTOP',
  modelName: 'Lenovo ThinkPad T14 Gen 4',
  purchaseDate: '2026-11-21',
  reason: '',
};
const PHOTO = new File(['photo-bytes'], 'laptop-2.webp', { type: 'image/webp' });
const ASSESS_URL = '/api/cases/assessments';
const DECIDE_URL = '/api/cases/decisions';

describe('CaseStore', () => {
  let store: CaseStore;
  let http: HttpTestingController;
  let fetchMock: ReturnType<typeof vi.fn>;
  let rafQueue: FrameRequestCallback[];

  function createStore(): CaseStore {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(withFetch()), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const created = TestBed.inject(CaseStore);
    created.init();
    return created;
  }

  async function flushRecording(url: string, recording: string): Promise<void> {
    const rec = await loadJsonRecording(recording);
    const req = await vi.waitFor(() => http.expectOne(url));
    req.flush(rec.body, {
      status: rec.status,
      statusText: rec.status === 200 ? 'OK' : 'Error',
      headers: rec.headers,
    });
  }

  async function submitOk(assessment = 'assessment-return-ok', decision = 'decision-return-ok'): Promise<void> {
    const promise = store.submit(FORM, PHOTO);
    await flushRecording(ASSESS_URL, assessment);
    await flushRecording(DECIDE_URL, decision);
    await promise;
  }

  function stored(): StoredCase {
    return JSON.parse(localStorage.getItem(STORED_CASE_KEY) as string) as StoredCase;
  }

  async function sentStreamDone(): Promise<void> {
    await vi.waitFor(() => expect(store.streaming()).toBe(false));
  }

  beforeEach(() => {
    localStorage.clear();
    rafQueue = [];
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => rafQueue.push(cb));
    vi.stubGlobal('cancelAnimationFrame', () => undefined);
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    // jsdom has no createImageBitmap / canvas: stub the browser primitives behind createThumbnailDataUrl.
    vi.stubGlobal('createImageBitmap', async () => ({ width: 640, height: 480, close: () => undefined }));
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({
      drawImage: () => undefined,
    } as unknown as CanvasRenderingContext2D);
    vi.spyOn(HTMLCanvasElement.prototype, 'toDataURL').mockReturnValue('data:image/jpeg;base64,THUMB');
    store = createStore();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  describe('startup', () => {
    it('starts on an empty form without storage', () => {
      expect(store.screen()).toBe('form');
      expect(store.interruptedNotice()).toBe(false);
      expect(store.formDraft()).toBeNull();
    });

    it('corrupted JSON in storage is cleared and the app starts on the form', () => {
      localStorage.setItem(STORED_CASE_KEY, '{not json');
      store = createStore();
      expect(store.screen()).toBe('form');
      expect(localStorage.getItem(STORED_CASE_KEY)).toBeNull();
    });

    it('a schema-mismatched case is cleared', () => {
      localStorage.setItem(STORED_CASE_KEY, JSON.stringify({ version: 99 }));
      store = createStore();
      expect(store.screen()).toBe('form');
      expect(localStorage.getItem(STORED_CASE_KEY)).toBeNull();
    });

    it('an interrupted submission restores the draft values, no photo, and the notice (AC-40)', async () => {
      void store.submit(FORM, PHOTO);
      await vi.waitFor(() => http.expectOne(ASSESS_URL));
      expect(JSON.parse(localStorage.getItem(FORM_DRAFT_KEY) as string).interrupted).toBe(true);

      store = createStore(); // reload while the request was in flight

      expect(store.screen()).toBe('form');
      expect(store.interruptedNotice()).toBe(true);
      expect(store.formDraft()).toMatchObject(FORM);
      expect(store.photo()).toBeNull();
    });
  });

  describe('submit', () => {
    it('happy path: assessment then decision, chat screen, persisted case, first message', async () => {
      const decision = await loadJsonRecording<DecisionResponse>('decision-return-ok');
      const promise = store.submit(FORM, PHOTO);

      expect(store.screen()).toBe('progress');
      expect(store.progress()).toEqual({ assessment: 'running', decision: 'waiting', error: false });
      await flushRecording(ASSESS_URL, 'assessment-return-ok');
      await vi.waitFor(() => expect(store.progress()).toEqual({ assessment: 'done', decision: 'running', error: false }));
      await flushRecording(DECIDE_URL, 'decision-return-ok');
      await promise;

      expect(store.screen()).toBe('chat');
      expect(store.currentDecision()).toBe(decision.body.decision.category);
      expect(store.signature()).toBe(decision.body.signature);
      expect(store.caseContext()).toEqual(decision.body.caseContext);
      expect(store.messages()).toHaveLength(1);
      expect(store.messages()[0]).toMatchObject({
        role: 'assistant',
        text: decision.body.decision.message,
        decisionCategory: decision.body.decision.category,
      });
      expect(localStorage.getItem(FORM_DRAFT_KEY)).toBeNull();
      expect(stored()).toMatchObject({
        version: 1,
        signature: decision.body.signature,
        thumbnailDataUrl: 'data:image/jpeg;base64,THUMB',
        photoFileName: 'laptop-2.webp',
        currentDecision: decision.body.decision.category,
      });
      expect(stored().messages).toEqual(store.messages());
    });

    it('sends the signed context returned by the assessment to the decision endpoint', async () => {
      const assessment = await loadJsonRecording<AssessmentResponse>('assessment-return-ok');
      const promise = store.submit(FORM, PHOTO);
      await flushRecording(ASSESS_URL, 'assessment-return-ok');
      const req = await vi.waitFor(() => http.expectOne(DECIDE_URL));
      expect(req.request.body).toEqual({
        caseContext: assessment.body.caseContext,
        signature: assessment.body.signature,
      });
      const rec = await loadJsonRecording('decision-return-ok');
      req.flush(rec.body, { status: 200, statusText: 'OK' });
      await promise;
    });

    it('guards against a double submit', async () => {
      const first = store.submit(FORM, PHOTO);
      void store.submit(FORM, PHOTO);
      const requests = http.match(ASSESS_URL);
      expect(requests).toHaveLength(1);
      const rec = await loadJsonRecording('assessment-return-ok');
      requests[0].flush(rec.body, { status: 200, statusText: 'OK' });
      await flushRecording(DECIDE_URL, 'decision-return-ok');
      await first;
      expect(store.messages()).toHaveLength(1);
    });

    it('a 60 s overall timeout ends in the error state with the AC-41 text and cancels the request', async () => {
      vi.useFakeTimers();
      const promise = store.submit(FORM, PHOTO);
      const req = http.expectOne(ASSESS_URL);

      await vi.advanceTimersByTimeAsync(59_999);
      expect(store.progress().error).toBe(false);
      await vi.advanceTimersByTimeAsync(1);
      await promise;

      expect(store.screen()).toBe('progress');
      expect(store.progress().error).toBe(true);
      expect(store.errorDetail()).toBe(LABELS.errors.decisionFailed);
      expect(req.cancelled).toBe(true);
    });

    it('the timer is cleared on success (no late error)', async () => {
      vi.useFakeTimers();
      await submitOk();
      await vi.advanceTimersByTimeAsync(120_000);
      expect(store.screen()).toBe('chat');
      expect(store.progress().error).toBe(false);
    });

    describe('error mapping', () => {
      async function failAssessment(recording: string): Promise<void> {
        const promise = store.submit(FORM, PHOTO);
        await flushRecording(ASSESS_URL, recording);
        await promise;
      }

      it('VALIDATION_FAILED returns to the form with fieldErrors, values and photo kept', async () => {
        const rec = await loadJsonRecording<{ fieldErrors: FieldError[] }>('problem-400-validation');
        await failAssessment('problem-400-validation');
        expect(store.screen()).toBe('form');
        expect(store.fieldErrors()).toEqual(rec.body.fieldErrors);
        expect(store.formDraft()).toMatchObject(FORM);
        expect(store.photo()).toBe(PHOTO);
        expect(store.interruptedNotice()).toBe(false);
      });

      for (const [recording, label] of [
        ['problem-400-unsupported-image', 'UNSUPPORTED_IMAGE'],
        ['problem-413-too-large', 'PHOTO_TOO_LARGE'],
      ] as const) {
        it(`${label} returns to the form with a photo field error (server detail)`, async () => {
          const rec = await loadJsonRecording<{ detail: string }>(recording);
          await failAssessment(recording);
          expect(store.screen()).toBe('form');
          expect(store.fieldErrors()).toEqual([{ field: 'photo', message: rec.body.detail }]);
          expect(store.photo()).toBe(PHOTO);
        });
      }

      it('RATE_LIMITED shows the server Polish detail in the progress error', async () => {
        const rec = await loadJsonRecording<{ detail: string }>('problem-429');
        await failAssessment('problem-429');
        expect(store.screen()).toBe('progress');
        expect(store.progress().error).toBe(true);
        expect(store.errorDetail()).toBe(rec.body.detail);
      });

      it('INVALID_CASE_SIGNATURE shows the invalidSignature label', async () => {
        const promise = store.submit(FORM, PHOTO);
        await flushRecording(ASSESS_URL, 'assessment-return-ok');
        await flushRecording(DECIDE_URL, 'problem-401-signature');
        await promise;
        expect(store.progress().error).toBe(true);
        expect(store.errorDetail()).toBe(LABELS.errors.invalidSignature);
      });

      it('502 LLM_FAILED shows the AC-41 text and never the raw technical text', async () => {
        await failAssessment('problem-502-llm-failed');
        expect(store.progress().error).toBe(true);
        expect(store.errorDetail()).toBe(LABELS.errors.decisionFailed);
      });

      // No 504 / 404 / 500 recording exists (problem-504-llm-timeout was not recorded); LLM_TIMEOUT is handled by the
      // same generic AC-41 branch as 502 (code not special-cased), covered by the 502 and 409 cases here.
      it('409 DECISION_ALREADY_MADE at the decision step shows the AC-41 text', async () => {
        const promise = store.submit(FORM, PHOTO);
        await flushRecording(ASSESS_URL, 'assessment-return-ok');
        await flushRecording(DECIDE_URL, 'problem-409-decided');
        await promise;
        expect(store.screen()).toBe('progress');
        expect(store.errorDetail()).toBe(LABELS.errors.decisionFailed);
      });

      it('a network error shows the AC-41 text', async () => {
        const promise = store.submit(FORM, PHOTO);
        http.expectOne(ASSESS_URL).error(new ProgressEvent('error'));
        await promise;
        expect(store.errorDetail()).toBe(LABELS.errors.decisionFailed);
      });
    });

    describe('retry and back', () => {
      it('retry after a decision failure re-runs only the decision, without a duplicate case', async () => {
        const promise = store.submit(FORM, PHOTO);
        await flushRecording(ASSESS_URL, 'assessment-return-ok');
        await flushRecording(DECIDE_URL, 'problem-502-llm-failed');
        await promise;
        expect(store.progress()).toMatchObject({ assessment: 'done', error: true });

        const retrying = store.retry();
        http.expectNone(ASSESS_URL);
        await flushRecording(DECIDE_URL, 'decision-return-ok');
        await retrying;

        expect(store.screen()).toBe('chat');
        expect(store.messages()).toHaveLength(1);
        expect(stored().messages).toHaveLength(1);
      });

      it('retry after an assessment failure re-sends the in-memory photo', async () => {
        const promise = store.submit(FORM, PHOTO);
        await flushRecording(ASSESS_URL, 'problem-502-llm-failed');
        await promise;

        const retrying = store.retry();
        const req = await vi.waitFor(() => http.expectOne(ASSESS_URL));
        expect((req.request.body as FormData).get('photo')).toBe(PHOTO);
        const rec = await loadJsonRecording('assessment-return-ok');
        req.flush(rec.body, { status: 200, statusText: 'OK' });
        await flushRecording(DECIDE_URL, 'decision-return-ok');
        await retrying;
        expect(store.screen()).toBe('chat');
      });

      it('backToForm returns to the form keeping values and photo, and leaves no interrupted draft', async () => {
        const promise = store.submit(FORM, PHOTO);
        await flushRecording(ASSESS_URL, 'problem-502-llm-failed');
        await promise;

        store.backToForm();

        expect(store.screen()).toBe('form');
        expect(store.formDraft()).toMatchObject(FORM);
        expect(store.photo()).toBe(PHOTO);
        expect(store.progress().error).toBe(false);
        store = createStore();
        expect(store.interruptedNotice()).toBe(false);
      });
    });

    it('a storage write failure keeps the app working and raises storageWarning', async () => {
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new DOMException('quota', 'QuotaExceededError');
      });
      await submitOk();
      expect(store.screen()).toBe('chat');
      expect(store.messages()).toHaveLength(1);
      expect(store.storageWarning()).toBe(true);
    });
  });

  describe('persistence and restore', () => {
    it('a new store instance over the same storage restores the chat (messages, order, badge, thumbnail)', async () => {
      await submitOk();
      fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-plain'));
      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();
      const before = { messages: store.messages(), badge: store.currentDecision(), signature: store.signature() };

      store = createStore();

      expect(store.screen()).toBe('chat');
      expect(store.messages()).toEqual(before.messages);
      expect(store.messages().map((m) => m.role)).toEqual(['assistant', 'user', 'assistant']);
      expect(store.currentDecision()).toBe(before.badge);
      expect(store.signature()).toBe(before.signature);
      expect(store.thumbnailDataUrl()).toBe('data:image/jpeg;base64,THUMB');
      expect(store.photoFileName()).toBe('laptop-2.webp');
    });

    it('a reload in the middle of a stream marks the unanswered user message failed and drops the partial reply', async () => {
      await submitOk();
      fetchMock.mockResolvedValue(await sseResponseHanging('chat-plain', () => undefined));
      void store.sendMessage('Pytanie testowe');
      await vi.waitFor(() => expect(store.messages()).toHaveLength(3));
      rafQueue.splice(0).forEach((cb) => cb(0));

      store = createStore();

      expect(store.messages().map((m) => [m.role, m.status])).toEqual([
        ['assistant', undefined],
        ['user', 'failed'],
      ]);
    });
  });

  describe('chat', () => {
    beforeEach(async () => {
      await submitOk();
    });

    it('streams a plain reply: user pending then sent, assistant text equals the deltas, badge unchanged', async () => {
      fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-plain'));
      const badge = store.currentDecision();
      const promise = store.sendMessage('Pytanie testowe');

      expect(store.streaming()).toBe(true);
      expect(store.messages().at(-1)).toMatchObject({ role: 'user', text: 'Pytanie testowe', status: 'pending' });
      await promise;
      await sentStreamDone();

      const expectedText = [...(await loadSseRecording('chat-plain')).text.matchAll(/^data: (\{"text".*\})$/gm)]
        .map((m) => (JSON.parse(m[1]) as { text: string }).text)
        .join('');
      expect(store.messages().map((m) => m.role)).toEqual(['assistant', 'user', 'assistant']);
      expect(store.messages()[1].status).toBe('sent');
      expect(store.messages()[2].text).toBe(expectedText);
      expect(store.messages()[2].decisionCategory).toBeUndefined();
      expect(store.currentDecision()).toBe(badge);
      expect(stored().messages).toEqual(store.messages());
    });

    it('sends the history without failed messages and with the new user message last', async () => {
      fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-plain'));
      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();
      const body = JSON.parse((fetchMock.mock.calls[0][1] as RequestInit).body as string);
      expect(body.messages.map((m: { role: string }) => m.role)).toEqual(['assistant', 'user']);
      expect(body.messages.at(-1).text).toBe('Pytanie testowe');
      expect(body.signature).toBe(store.signature());
    });

    it('ignores empty text and sends while streaming', async () => {
      fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-plain'));
      await store.sendMessage('   ');
      expect(fetchMock).not.toHaveBeenCalled();
      void store.sendMessage('Pierwsze');
      void store.sendMessage('Drugie');
      await sentStreamDone();
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(store.messages().filter((m) => m.role === 'user')).toHaveLength(1);
    });

    it('persistence while streaming is throttled to the animation frame and immediate on done', async () => {
      fetchMock.mockResolvedValue(await sseResponseHanging('chat-plain', () => undefined));
      void store.sendMessage('Pytanie testowe');
      await vi.waitFor(() => expect(store.messages()).toHaveLength(3));
      expect(stored().messages).toHaveLength(2); // user message persisted; delta not yet
      expect(rafQueue.length).toBeGreaterThan(0);

      rafQueue.splice(0).forEach((cb) => cb(0));
      expect(stored().messages).toHaveLength(3);
      expect(stored().messages[2].text).not.toBe('');
    });

    it('a decision event updates the badge and marks the assistant message (decision update)', async () => {
      // Start from the REJECTED complaint case so the ESCALATED update from chat-decision-update is visible.
      localStorage.clear();
      store = createStore();
      await submitOk('assessment-complaint-ok', 'decision-complaint-ok');
      expect(store.currentDecision()).toBe('REJECTED');
      fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-decision-update'));

      await store.sendMessage('Czy mogę prosić o ponowną ocenę?');
      await sentStreamDone();

      expect(store.currentDecision()).toBe('ESCALATED');
      expect(store.messages().at(-1)).toMatchObject({ role: 'assistant', decisionCategory: 'ESCALATED' });
      expect(stored().currentDecision).toBe('ESCALATED');
    });

    it('a stream that fails after a decision event restores the pre-send decision (badge and persisted case)', async () => {
      // Derived from chat-decision-update.sse: keep the events before `done` (decision + deltas), then the body errors.
      localStorage.clear();
      store = createStore();
      await submitOk('assessment-complaint-ok', 'decision-complaint-ok');
      expect(store.currentDecision()).toBe('REJECTED');
      const rec = await loadSseRecording('chat-decision-update');
      const frames = rec.text.split('\n\n').filter((f) => f.length > 0);
      const doneIndex = frames.findIndex((f) => f.startsWith('event:done'));
      expect(doneIndex).toBeGreaterThan(1);
      const truncated = frames.slice(0, doneIndex).join('\n\n') + '\n\n';
      fetchMock.mockResolvedValue(sseResponseFromText(truncated, rec.headers, 'whole', undefined, true));

      await store.sendMessage('Czy mogę prosić o ponowną ocenę?');
      await sentStreamDone();

      expect(store.messages().at(-1)).toMatchObject({ role: 'user', status: 'failed' });
      expect(store.currentDecision()).toBe('REJECTED');
      expect(stored().currentDecision).toBe('REJECTED');
    });

    it('an error event removes the partial reply and marks the user message failed', async () => {
      fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-error'));
      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();
      expect(store.messages().map((m) => [m.role, m.status])).toEqual([
        ['assistant', undefined],
        ['user', 'failed'],
      ]);
      expect(stored().messages).toEqual(store.messages());
    });

    it('a network cut after some deltas removes the partial assistant message', async () => {
      // Derived from chat-plain.sse: first two events delivered, then the body stream errors.
      const rec = await loadSseRecording('chat-plain');
      const cut = rec.text.split('\n\n').slice(0, 2).join('\n\n') + '\n\n';
      fetchMock.mockResolvedValue(sseResponseFromText(cut, rec.headers, 'whole', undefined, true));
      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();
      expect(store.messages().map((m) => [m.role, m.status])).toEqual([
        ['assistant', undefined],
        ['user', 'failed'],
      ]);
    });

    it('a malformed recorded delta removes the partial reply and marks the user message failed', async () => {
      // Derived from chat-plain.sse (T26 recording): corrupt only the JSON in its second delta frame.
      const recording = await loadSseRecording('chat-plain');
      const deltaFrames = [...recording.text.matchAll(/^event:delta\ndata: (\{"text":.*\})$/gm)];
      expect(deltaFrames.length).toBeGreaterThan(1);
      const secondFrame = deltaFrames[1][0];
      const malformedFrame = secondFrame.replace(/\}$/, '');
      const corrupted = recording.text.replace(secondFrame, malformedFrame);
      expect(corrupted).not.toBe(recording.text);
      fetchMock.mockResolvedValue(sseResponseFromText(corrupted, recording.headers));

      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();

      expect(store.messages().map((message) => [message.role, message.status])).toEqual([
        ['assistant', undefined],
        ['user', 'failed'],
      ]);
    });

    it('a pre-stream problem (401 signature) marks the user message failed', async () => {
      fetchMock.mockResolvedValue(await responseFromRecording('problem-401-chat-signature'));
      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();
      expect(store.messages().at(-1)).toMatchObject({ role: 'user', status: 'failed' });
    });

    it('resend after a failure re-sends the same history and leaves exactly one user message (sent)', async () => {
      fetchMock.mockResolvedValueOnce(await sseResponseFromRecording('chat-error'));
      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();
      const failed = store.messages().at(-1)!;
      const firstBody = JSON.parse((fetchMock.mock.calls[0][1] as RequestInit).body as string);

      fetchMock.mockResolvedValueOnce(await sseResponseFromRecording('chat-plain'));
      await store.resend(failed.id);
      await sentStreamDone();

      const users = store.messages().filter((m) => m.role === 'user');
      expect(users).toHaveLength(1);
      expect(users[0]).toMatchObject({ id: failed.id, status: 'sent' });
      expect(store.messages().map((m) => m.role)).toEqual(['assistant', 'user', 'assistant']);
      expect(JSON.parse((fetchMock.mock.calls[1][1] as RequestInit).body as string).messages).toEqual(
        firstBody.messages,
      );
    });

    it('a double click on resend produces a single request (AC-42)', async () => {
      fetchMock.mockResolvedValueOnce(await sseResponseFromRecording('chat-error'));
      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();
      const failed = store.messages().at(-1)!;

      fetchMock.mockResolvedValueOnce(await sseResponseFromRecording('chat-plain'));
      void store.resend(failed.id);
      void store.resend(failed.id);
      await sentStreamDone();

      expect(fetchMock).toHaveBeenCalledTimes(2);
      expect(store.messages().filter((m) => m.role === 'user')).toHaveLength(1);
    });

    it('resend ignores messages that are not failed', async () => {
      const first = store.messages()[0];
      await store.resend(first.id);
      await store.resend('missing');
      expect(fetchMock).not.toHaveBeenCalled();
    });
  });

  describe('newCase', () => {
    it('clears storage and signals and returns to the form', async () => {
      await submitOk();
      fetchMock.mockResolvedValue(await sseResponseFromRecording('chat-plain'));
      await store.sendMessage('Pytanie testowe');
      await sentStreamDone();

      store.newCase();

      expect(store.screen()).toBe('form');
      expect(store.messages()).toEqual([]);
      expect(store.caseContext()).toBeNull();
      expect(store.signature()).toBeNull();
      expect(store.currentDecision()).toBeNull();
      expect(store.photo()).toBeNull();
      expect(store.formDraft()).toBeNull();
      expect(store.streaming()).toBe(false);
      expect(localStorage.getItem(STORED_CASE_KEY)).toBeNull();
      expect(localStorage.getItem(FORM_DRAFT_KEY)).toBeNull();
      store = createStore();
      expect(store.screen()).toBe('form');
    });
  });
});
