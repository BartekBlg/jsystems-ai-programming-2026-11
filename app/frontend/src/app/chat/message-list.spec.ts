import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';
import { CaseContext, ChatMessage, DecisionResponse } from '../core/models';
import { provideMarkdown } from 'ngx-markdown';
import { loadJsonRecording } from '../../testing/recording-loader';
import { MessageListComponent } from './message-list';

describe('MessageListComponent', () => {
  let fixture: ComponentFixture<MessageListComponent>;
  let store: {
    caseContext: ReturnType<typeof signal<CaseContext | null>>;
    messages: ReturnType<typeof signal<ChatMessage[]>>;
    streaming: ReturnType<typeof signal<boolean>>;
    resend: ReturnType<typeof vi.fn<(id: string) => Promise<void>>>;
  };

  async function recordedMessage(recording: string): Promise<{ response: DecisionResponse; message: ChatMessage }> {
    const response = (await loadJsonRecording<DecisionResponse>(recording)).body;
    return {
      response,
      message: {
        id: recording,
        role: 'assistant',
        text: response.decision.message,
        createdAt: '2026-11-26T12:00:00.000Z',
        decisionCategory: response.decision.category,
      },
    };
  }

  beforeEach(async () => {
    const { response, message } = await recordedMessage('decision-return-ok');
    store = {
      caseContext: signal(response.caseContext),
      messages: signal([message]),
      streaming: signal(false),
      resend: vi.fn(() => Promise.resolve()),
    };
    await TestBed.configureTestingModule({
      imports: [MessageListComponent],
      providers: [{ provide: CaseStore, useValue: store }, provideMarkdown()],
    }).compileComponents();
    fixture = TestBed.createComponent(MessageListComponent);
    fixture.detectChanges();
  });

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('renders the recorded agent decision as sanitized Markdown with headings', () => {
    const heading = fixture.nativeElement.querySelector('h2') as HTMLElement;
    expect(heading.textContent?.trim()).toBe('Decyzja');
    expect(fixture.nativeElement.querySelector('.message--agent')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.message--client')).toBeNull();
  });

  it('shows a typing indicator with hidden text while the store is streaming', () => {
    store.streaming.set(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.typing-indicator')).not.toBeNull();
    expect(text()).toContain(LABELS.chat.typing);
  });

  it('shows a failed customer message and retries its id only once on a double click', () => {
    const user: ChatMessage = {
      id: 'failed-user-message',
      role: 'user',
      text: 'Czy otrzymali Państwo moje zgłoszenie?',
      createdAt: '2026-11-26T12:01:00.000Z',
      status: 'failed',
    };
    store.messages.set([...store.messages(), user]);
    fixture.detectChanges();
    expect(text()).toContain(user.text);
    expect(text()).toContain(LABELS.chat.notSent);

    const retry = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).find(
      (button) => button.textContent?.trim() === LABELS.chat.retry,
    )!;
    retry.click();
    retry.click();
    expect(store.resend).toHaveBeenCalledTimes(1);
    expect(store.resend).toHaveBeenCalledWith(user.id);
  });

  it('offers Nowe zgłoszenie only below the first message when the recorded photo is insufficient', async () => {
    const { response, message } = await recordedMessage('decision-insufficient');
    store.caseContext.set(response.caseContext);
    store.messages.set([message]);
    fixture.detectChanges();

    const button = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).find(
      (item) => item.textContent?.trim() === LABELS.newCase,
    );
    expect(button).toBeDefined();
    expect(button!.parentElement?.previousElementSibling?.classList.contains('message--agent')).toBe(true);

    const sufficient = await recordedMessage('decision-return-ok');
    store.caseContext.set(sufficient.response.caseContext);
    store.messages.set([sufficient.message]);
    fixture.detectChanges();
    expect(Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).some(
      (item) => item.textContent?.trim() === LABELS.newCase,
    )).toBe(false);
  });

  describe('scroll position', () => {
    const CLIENT_HEIGHT = 300;

    /** jsdom has no layout: derive scrollHeight from rendered nodes and clamp scrollTop like a browser. */
    function stubLayout(viewport: HTMLElement): void {
      let top = 0;
      const maxTop = () => Math.max(0, viewport.scrollHeight - viewport.clientHeight);
      Object.defineProperty(viewport, 'clientHeight', { configurable: true, get: () => CLIENT_HEIGHT });
      Object.defineProperty(viewport, 'scrollHeight', {
        configurable: true,
        get: () => 100 * viewport.querySelectorAll('.message-content, .message-content h2, .message-content p, .message-content li').length,
      });
      Object.defineProperty(viewport, 'scrollTop', {
        configurable: true,
        get: () => Math.min(top, maxTop()),
        set: (value: number) => {
          top = Math.min(value, maxTop());
        },
      });
    }

    it('scrolls to the latest message after restored messages render (reload)', async () => {
      const first = await recordedMessage('decision-return-ok');
      const second = await recordedMessage('decision-return-more-info');
      const restored = TestBed.createComponent(MessageListComponent);
      const viewport = (restored.nativeElement as HTMLElement).querySelector('.message-list') as HTMLElement;
      stubLayout(viewport);
      store.messages.set([
        first.message,
        { ...second.message, id: 'm2' },
        { ...first.message, id: 'm3' },
        { ...second.message, id: 'm4' },
      ]);
      restored.detectChanges();
      await restored.whenStable();
      await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)));
      await restored.whenStable();

      expect(viewport.scrollHeight).toBeGreaterThan(CLIENT_HEIGHT);
      expect(viewport.scrollTop).toBe(viewport.scrollHeight - viewport.clientHeight);
    });
  });
});
