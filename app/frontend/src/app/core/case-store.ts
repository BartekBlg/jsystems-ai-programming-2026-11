import { Injectable, inject, signal } from '@angular/core';
import { ApiError, ApiService } from './api.service';
import { ChatStreamService } from './chat-stream.service';
import { createThumbnailDataUrl } from './image-utils';
import { LABELS } from './labels';
import {
  AssessmentResponse,
  CaseContext,
  CaseForm,
  ChatMessage,
  ChatRequestMessage,
  DecisionCategory,
  ErrorCode,
  FieldError,
  FormDraft,
  Screen,
  StepStatus,
  StoredCase,
} from './models';
import { StorageService } from './storage.service';

export const SUBMIT_TIMEOUT_MS = 60_000;

export interface ProgressState {
  assessment: StepStatus;
  decision: StepStatus;
  error: boolean;
}

const IDLE_PROGRESS: ProgressState = { assessment: 'waiting', decision: 'waiting', error: false };
const FORM_ERROR_CODES: readonly ErrorCode[] = ['VALIDATION_FAILED', 'UNSUPPORTED_IMAGE', 'PHOTO_TOO_LARGE'];

/**
 * Single source of truth for the case flow (ADR-003 §3.3): screens, submission pipeline, chat and persistence.
 * The browser never modifies the signed CaseContext; it stores and echoes it.
 */
@Injectable({ providedIn: 'root' })
export class CaseStore {
  private readonly api = inject(ApiService);
  private readonly chat = inject(ChatStreamService);
  private readonly storage = inject(StorageService);

  readonly screen = signal<Screen>('form');
  readonly formDraft = signal<FormDraft | null>(null);
  readonly photo = signal<File | null>(null);
  readonly progress = signal<ProgressState>(IDLE_PROGRESS);
  readonly caseContext = signal<CaseContext | null>(null);
  readonly signature = signal<string | null>(null);
  readonly messages = signal<ChatMessage[]>([]);
  readonly currentDecision = signal<DecisionCategory | null>(null);
  readonly streaming = signal(false);
  readonly interruptedNotice = signal(false);
  /** Polish text for the progress error state (server detail for RATE_LIMITED, otherwise a label). */
  readonly errorDetail = signal<string | null>(null);
  readonly errorCode = signal<ErrorCode | null>(null);
  readonly fieldErrors = signal<FieldError[]>([]);
  readonly thumbnailDataUrl = signal('');
  readonly photoFileName = signal('');
  readonly storageWarning = signal(false);

  private busy = false;
  private form: CaseForm | null = null;
  private assessed: AssessmentResponse | null = null;
  private generation = 0;
  private streamController: AbortController | null = null;
  private persistScheduled = false;

  /** Startup logic (AC-38, AC-40). */
  init(): void {
    const stored = this.storage.loadCase();
    if (stored) {
      this.restore(stored);
      return;
    }
    const draft = this.storage.loadDraft();
    if (draft?.interrupted) {
      this.formDraft.set(draft);
      this.interruptedNotice.set(true);
    }
    this.screen.set('form');
  }

  async submit(form: CaseForm, photo: File): Promise<void> {
    if (this.busy) {
      return;
    }
    this.busy = true;
    this.form = form;
    this.assessed = null;
    this.photo.set(photo);
    this.fieldErrors.set([]);
    this.errorDetail.set(null);
    this.errorCode.set(null);
    this.interruptedNotice.set(false);
    this.saveDraft(form, true);
    this.screen.set('progress');
    this.progress.set({ assessment: 'running', decision: 'waiting', error: false });
    await this.runPipeline();
  }

  async retry(): Promise<void> {
    const photo = this.photo();
    if (this.busy || !this.progress().error || !this.form || !photo) {
      return;
    }
    this.busy = true;
    this.errorDetail.set(null);
    this.errorCode.set(null);
    this.progress.set(
      this.assessed
        ? { assessment: 'done', decision: 'running', error: false }
        : { assessment: 'running', decision: 'waiting', error: false },
    );
    await this.runPipeline();
  }

  backToForm(): void {
    if (this.busy) {
      return;
    }
    if (this.form) {
      this.saveDraft(this.form, false);
    }
    this.progress.set(IDLE_PROGRESS);
    this.errorDetail.set(null);
    this.errorCode.set(null);
    this.screen.set('form');
  }

  async sendMessage(text: string): Promise<void> {
    const trimmed = text.trim();
    if (!trimmed || this.streaming() || !this.caseContext()) {
      return;
    }
    const message: ChatMessage = {
      id: newId(),
      role: 'user',
      text: trimmed,
      createdAt: new Date().toISOString(),
      status: 'pending',
    };
    this.messages.update((list) => [...list, message]);
    await this.streamReply(message.id);
  }

  /** Re-sends the history of a failed user message without adding a new message (AC-42). */
  async resend(messageId: string): Promise<void> {
    if (this.streaming()) {
      return;
    }
    const target = this.messages().find((m) => m.id === messageId);
    if (!target || target.role !== 'user' || target.status !== 'failed') {
      return;
    }
    this.updateMessage(messageId, { status: 'pending' });
    await this.streamReply(messageId);
  }

  newCase(): void {
    this.generation++;
    this.streamController?.abort();
    this.streamController = null;
    this.persistScheduled = false;
    this.storage.clearCase();
    this.storage.clearDraft();
    this.busy = false;
    this.form = null;
    this.assessed = null;
    this.screen.set('form');
    this.formDraft.set(null);
    this.photo.set(null);
    this.progress.set(IDLE_PROGRESS);
    this.caseContext.set(null);
    this.signature.set(null);
    this.messages.set([]);
    this.currentDecision.set(null);
    this.streaming.set(false);
    this.interruptedNotice.set(false);
    this.errorDetail.set(null);
    this.errorCode.set(null);
    this.fieldErrors.set([]);
    this.thumbnailDataUrl.set('');
    this.photoFileName.set('');
    this.storageWarning.set(false);
  }

  // --- submission pipeline -------------------------------------------------------------------------------------

  private async runPipeline(): Promise<void> {
    const form = this.form;
    const photo = this.photo();
    if (!form || !photo) {
      this.busy = false;
      return;
    }
    const generation = this.generation;
    const controller = new AbortController();
    let timedOut = false;
    const timer = setTimeout(() => {
      timedOut = true;
      controller.abort();
    }, SUBMIT_TIMEOUT_MS);
    let step: 'assessment' | 'decision' = this.assessed ? 'decision' : 'assessment';
    try {
      if (!this.assessed) {
        this.assessed = await this.api.assess(form, photo, controller.signal);
        this.progress.update((p) => ({ ...p, assessment: 'done', decision: 'running' }));
      }
      step = 'decision';
      const result = await this.api.decide(this.assessed.caseContext, this.assessed.signature, controller.signal);
      if (generation !== this.generation) {
        return;
      }
      const thumbnail = await createThumbnailDataUrl(photo).catch(() => '');
      this.progress.update((p) => ({ ...p, decision: 'done' }));
      this.caseContext.set(result.caseContext);
      this.signature.set(result.signature);
      this.thumbnailDataUrl.set(thumbnail);
      this.photoFileName.set(photo.name);
      this.currentDecision.set(result.decision.category);
      this.messages.set([
        {
          id: newId(),
          role: 'assistant',
          text: result.decision.message,
          createdAt: new Date().toISOString(),
          decisionCategory: result.decision.category,
        },
      ]);
      this.persistNow();
      this.storage.clearDraft();
      this.formDraft.set(null);
      this.screen.set('chat');
    } catch (error) {
      if (generation === this.generation) {
        this.failPipeline(error, timedOut, step);
      }
    } finally {
      clearTimeout(timer);
      if (generation === this.generation) {
        this.busy = false;
      }
    }
  }

  private failPipeline(error: unknown, timedOut: boolean, step: 'assessment' | 'decision'): void {
    const api = !timedOut && error instanceof ApiError ? error : null;
    const code = api?.code ?? 'INTERNAL';
    if (api && FORM_ERROR_CODES.includes(code)) {
      this.fieldErrors.set(
        api.fieldErrors?.length ? api.fieldErrors : [{ field: 'photo', message: api.detail }],
      );
      if (this.form) {
        this.saveDraft(this.form, false);
      }
      this.progress.set(IDLE_PROGRESS);
      this.screen.set('form');
      return;
    }
    this.errorCode.set(code);
    if (api && code === 'RATE_LIMITED' && api.detail) {
      this.errorDetail.set(api.detail);
    } else if (code === 'INVALID_CASE_SIGNATURE') {
      this.errorDetail.set(LABELS.errors.invalidSignature);
    } else {
      this.errorDetail.set(LABELS.errors.decisionFailed);
    }
    this.progress.update((p) => ({
      assessment: step === 'assessment' ? 'running' : p.assessment,
      decision: step === 'decision' ? 'running' : p.decision,
      error: true,
    }));
  }

  // --- chat ----------------------------------------------------------------------------------------------------

  private streamReply(userMessageId: string): Promise<void> {
    const context = this.caseContext();
    const signature = this.signature();
    if (!context || signature === null) {
      return Promise.resolve();
    }
    this.streaming.set(true);
    this.persistNow();

    const list = this.messages();
    const index = list.findIndex((m) => m.id === userMessageId);
    const history: ChatRequestMessage[] = list
      .slice(0, index + 1)
      .filter((m) => m.id === userMessageId || m.status !== 'failed')
      .map(({ id, role, text }) => ({ id, role, text }));

    const generation = this.generation;
    const controller = new AbortController();
    this.streamController = controller;
    let assistantId: string | null = null;
    // A failed send must not leave a badge from a reply that was removed again.
    const decisionBeforeSend = this.currentDecision();

    const ensureAssistant = (): string => {
      if (assistantId === null) {
        const id = newId();
        assistantId = id;
        this.messages.update((m) => [
          ...m,
          { id, role: 'assistant', text: '', createdAt: new Date().toISOString() },
        ]);
      }
      return assistantId;
    };
    const stale = (): boolean => generation !== this.generation;

    return new Promise<void>((resolve) => {
      this.chat.send(context, signature, history, controller.signal).subscribe({
        next: (event) => {
          if (stale()) {
            return;
          }
          if (event.type === 'decision') {
            this.currentDecision.set(event.category);
            this.updateMessage(ensureAssistant(), { decisionCategory: event.category });
            this.schedulePersist();
          } else if (event.type === 'delta') {
            const id = ensureAssistant();
            this.messages.update((m) => m.map((x) => (x.id === id ? { ...x, text: x.text + event.text } : x)));
            this.schedulePersist();
          }
        },
        complete: () => {
          if (!stale()) {
            this.updateMessage(userMessageId, { status: 'sent' });
            this.streaming.set(false);
            this.persistNow();
          }
          resolve();
        },
        error: () => {
          if (!stale()) {
            const partial = assistantId;
            if (partial !== null) {
              this.messages.update((m) => m.filter((x) => x.id !== partial));
            }
            this.currentDecision.set(decisionBeforeSend);
            this.updateMessage(userMessageId, { status: 'failed' });
            this.streaming.set(false);
            this.persistNow();
          }
          resolve();
        },
      });
    });
  }

  private updateMessage(id: string, patch: Partial<ChatMessage>): void {
    this.messages.update((list) => list.map((m) => (m.id === id ? { ...m, ...patch } : m)));
  }

  // --- persistence ---------------------------------------------------------------------------------------------

  private restore(stored: StoredCase): void {
    let messages = stored.messages;
    // A reload in the middle of a stream: the unanswered user message failed, the partial reply is dropped.
    const pending = messages.findIndex((m) => m.role === 'user' && m.status === 'pending');
    if (pending !== -1) {
      messages = messages
        .slice(0, pending + 1)
        .map((m, i) => (i === pending ? { ...m, status: 'failed' as const } : m));
    }
    this.caseContext.set(stored.caseContext);
    this.signature.set(stored.signature);
    this.thumbnailDataUrl.set(stored.thumbnailDataUrl);
    this.photoFileName.set(stored.photoFileName);
    this.messages.set(messages);
    this.currentDecision.set(stored.currentDecision);
    this.screen.set('chat');
    if (pending !== -1) {
      this.persistNow();
    }
  }

  private persistNow(): void {
    this.persistScheduled = false;
    const context = this.caseContext();
    const signature = this.signature();
    const decision = this.currentDecision();
    if (!context || signature === null || !decision) {
      return;
    }
    const ok = this.storage.saveCase({
      version: 1,
      caseContext: context,
      signature,
      thumbnailDataUrl: this.thumbnailDataUrl(),
      photoFileName: this.photoFileName(),
      messages: this.messages(),
      currentDecision: decision,
    });
    this.storageWarning.set(!ok);
  }

  /** Streaming writes are debounced to one per animation frame (ADR-003 §3.3). */
  private schedulePersist(): void {
    if (this.persistScheduled) {
      return;
    }
    this.persistScheduled = true;
    const run = (): void => {
      if (this.persistScheduled) {
        this.persistNow();
      }
    };
    if (typeof requestAnimationFrame === 'function') {
      requestAnimationFrame(run);
    } else {
      setTimeout(run, 16);
    }
  }

  private saveDraft(form: CaseForm, interrupted: boolean): void {
    const draft: FormDraft = { ...form, interrupted };
    this.formDraft.set(draft);
    if (!this.storage.saveDraft(draft)) {
      this.storageWarning.set(true);
    }
  }
}

function newId(): string {
  return crypto.randomUUID();
}
