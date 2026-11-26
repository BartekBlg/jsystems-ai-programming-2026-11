import { TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideMarkdown } from 'ngx-markdown';
import { loadJsonRecording } from '../testing/recording-loader';
import { App } from './app';
import { CaseStore } from './core/case-store';
import { LABELS } from './core/labels';
import { DecisionResponse, STORED_CASE_KEY, StoredCase } from './core/models';

describe('App shell', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideMarkdown(), provideNativeDateAdapter()],
    }).compileComponents();
  });

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the ElektroDemo text wordmark and the product name in the header (ADR-005 D8)', async () => {
    const el = await render();
    const header = el.querySelector('header');
    expect(header).not.toBeNull();
    expect(header!.querySelector('.wordmark')?.textContent?.trim()).toBe('ElektroDemo');
    expect(header!.textContent).toContain('Copilot serwisowy');
    expect(header!.querySelector('img')).toBeNull();
  });

  it('shows the AC-46 demo note as a note on every screen', async () => {
    const el = await render();
    const note = el.querySelector('[role="note"]');
    expect(note?.textContent?.trim()).toBe(
      'Wersja demonstracyjna — przykładowy regulamin fikcyjnego sklepu. Decyzja jest wstępna.',
    );
    expect(note?.textContent?.trim()).toBe(LABELS.demoNote);
  });

  async function seedStoredCase(): Promise<void> {
    const decision = (await loadJsonRecording<DecisionResponse>('decision-return-ok')).body;
    const stored: StoredCase = {
      version: 1,
      caseContext: decision.caseContext,
      signature: decision.signature,
      thumbnailDataUrl: '',
      photoFileName: 'laptop.webp',
      messages: [
        {
          id: 'm1',
          role: 'assistant',
          text: decision.decision.message,
          createdAt: '2026-11-26T12:00:00.000Z',
          decisionCategory: decision.decision.category,
        },
      ],
      currentDecision: decision.decision.category,
    };
    localStorage.setItem(STORED_CASE_KEY, JSON.stringify(stored));
  }

  function newCaseButton(el: HTMLElement): HTMLButtonElement | undefined {
    return Array.from(el.querySelectorAll('header button')).find(
      (b) => b.textContent?.trim() === LABELS.newCase,
    ) as HTMLButtonElement | undefined;
  }

  it('renders the form for a fresh store and hides the new-case button', async () => {
    const el = await render();
    expect(el.querySelector('app-case-form')).not.toBeNull();
    expect(el.querySelector('app-progress-screen')).toBeNull();
    expect(el.querySelector('app-chat-screen')).toBeNull();
    expect(newCaseButton(el)).toBeUndefined();
  });

  it('renders the chat screen for a restored case and shows the header new-case button', async () => {
    await seedStoredCase();
    const el = await render();
    expect(el.querySelector('app-chat-screen')).not.toBeNull();
    expect(el.querySelector('app-case-form')).toBeNull();
    expect(newCaseButton(el)).toBeDefined();
  });

  it('renders the progress screen when the store screen is progress', async () => {
    const fixture = TestBed.createComponent(App);
    TestBed.inject(CaseStore).screen.set('progress');
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('app-progress-screen')).not.toBeNull();
    expect(el.querySelector('app-case-form')).toBeNull();
    expect(newCaseButton(el)).toBeUndefined();
  });

  it('shows a snackbar when the store reports a storage warning', async () => {
    const open = vi.spyOn(TestBed.inject(MatSnackBar), 'open');
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    TestBed.inject(CaseStore).storageWarning.set(true);
    await fixture.whenStable();
    expect(open).toHaveBeenCalledWith(LABELS.errors.storageFailed, undefined, expect.anything());
  });
});
