import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BreakpointObserver } from '@angular/cdk/layout';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';
import { CaseContext, ChatMessage, DecisionResponse } from '../core/models';
import { loadJsonRecording, loadSseRecording } from '../../testing/recording-loader';
import { provideMarkdown } from 'ngx-markdown';
import { of } from 'rxjs';
import { ChatScreenComponent } from './chat-screen';

describe('ChatScreenComponent', () => {
  let fixture: ComponentFixture<ChatScreenComponent>;
  let breakpoint: { observe: ReturnType<typeof vi.fn> };
  let store: {
    caseContext: ReturnType<typeof signal<CaseContext | null>>;
    currentDecision: ReturnType<typeof signal<DecisionResponse['decision']['category'] | null>>;
    thumbnailDataUrl: ReturnType<typeof signal<string>>;
    photoFileName: ReturnType<typeof signal<string>>;
    messages: ReturnType<typeof signal<ChatMessage[]>>;
    streaming: ReturnType<typeof signal<boolean>>;
    resend: ReturnType<typeof vi.fn<(id: string) => Promise<void>>>;
    sendMessage: ReturnType<typeof vi.fn<(text: string) => Promise<void>>>;
  };

  beforeEach(async () => {
    const decision = (await loadJsonRecording<DecisionResponse>('decision-return-ok')).body;
    const initial: ChatMessage = {
      id: 'decision-return-ok',
      role: 'assistant',
      text: decision.decision.message,
      createdAt: '2026-11-26T12:00:00.000Z',
      decisionCategory: decision.decision.category,
    };
    store = {
      caseContext: signal(decision.caseContext),
      currentDecision: signal(decision.decision.category),
      thumbnailDataUrl: signal(''),
      photoFileName: signal(''),
      messages: signal([initial]),
      streaming: signal(false),
      resend: vi.fn(() => Promise.resolve()),
      sendMessage: vi.fn(() => Promise.resolve()),
    };
    breakpoint = {
      observe: vi.fn(() => of({ matches: false, breakpoints: { '(min-width: 960px)': false } })),
    };
    await TestBed.configureTestingModule({
      imports: [ChatScreenComponent],
      providers: [
        { provide: CaseStore, useValue: store },
        { provide: BreakpointObserver, useValue: breakpoint },
        provideMarkdown(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ChatScreenComponent);
    fixture.detectChanges();
  });

  it('uses a collapsible summary above the conversation at narrow widths', () => {
    expect(fixture.nativeElement.querySelector('mat-expansion-panel')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.desktop-summary')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-message-list')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-composer')).not.toBeNull();
  });

  it('contains no file input, so the chat accepts no uploads (AC-37)', () => {
    expect(fixture.nativeElement.querySelector('input[type="file"]')).toBeNull();
  });

  it('announces the recorded agent reply politely after streaming completes', async () => {
    const stream = await loadSseRecording('chat-plain');
    const reply = stream.text
      .split('\n')
      .filter((line) => line.startsWith('data: '))
      .map((line) => JSON.parse(line.slice('data: '.length)) as { text?: string })
      .map((event) => event.text ?? '')
      .join('');
    const first = store.messages()[0];
    store.streaming.set(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]')?.textContent?.trim()).toBe('');

    store.messages.set([
      first,
      { id: 'customer-question', role: 'user', text: 'Jakie są kolejne kroki?', createdAt: first.createdAt },
      { id: 'recorded-reply', role: 'assistant', text: reply, createdAt: first.createdAt },
    ]);
    store.streaming.set(false);
    fixture.detectChanges();

    const liveRegion = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement;
    expect(liveRegion.getAttribute('aria-live')).toBe('polite');
    expect(liveRegion.textContent?.trim()).toBe(LABELS.chat.newReply);
  });
});
