import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';
import { DecisionResponse, ErrorCode } from '../core/models';
import { loadJsonRecording } from '../../testing/recording-loader';
import { ProgressScreenComponent } from './progress-screen';

describe('ProgressScreenComponent', () => {
  let fixture: ComponentFixture<ProgressScreenComponent>;
  let store: {
    progress: ReturnType<typeof signal<{ assessment: 'waiting' | 'running' | 'done'; decision: 'waiting' | 'running' | 'done'; error: boolean }>>;
    errorDetail: ReturnType<typeof signal<string | null>>;
    errorCode: ReturnType<typeof signal<ErrorCode | null>>;
    retry: ReturnType<typeof vi.fn>;
    backToForm: ReturnType<typeof vi.fn>;
    newCase: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    store = {
      progress: signal({ assessment: 'running', decision: 'waiting', error: false }),
      errorDetail: signal(null),
      errorCode: signal(null),
      retry: vi.fn(),
      backToForm: vi.fn(),
      newCase: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [ProgressScreenComponent],
      providers: [{ provide: CaseStore, useValue: store }],
    }).compileComponents();
    fixture = TestBed.createComponent(ProgressScreenComponent);
    fixture.detectChanges();
  });

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function button(label: string): HTMLButtonElement {
    return Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).find(
      (item) => item.textContent?.trim() === label,
    )!;
  }

  it('renders both progress stages, their accessible status, spinner and duration note', () => {
    expect(text()).toContain(LABELS.progress.analysing);
    expect(text()).toContain(LABELS.progress.deciding);
    expect(text()).toContain(LABELS.progress.statusRunning);
    expect(text()).toContain(LABELS.progress.statusWaiting);
    expect(text()).toContain(LABELS.progress.note);
    expect(fixture.nativeElement.querySelector('mat-progress-spinner')).not.toBeNull();
  });

  it('updates stage status text and icons when the store progress advances', () => {
    store.progress.set({ assessment: 'done', decision: 'running', error: false });
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('.progress-step') as NodeListOf<HTMLElement>;
    expect(rows[0].textContent).toContain(LABELS.progress.statusDone);
    expect(rows[1].textContent).toContain(LABELS.progress.statusRunning);

    store.progress.set({ assessment: 'done', decision: 'done', error: false });
    fixture.detectChanges();
    expect(rows[1].textContent).toContain(LABELS.progress.statusDone);
  });

  it('shows error detail and retry/back actions that call the store methods', () => {
    store.errorDetail.set(LABELS.errors.decisionFailed);
    store.progress.set({ assessment: 'running', decision: 'waiting', error: true });
    fixture.detectChanges();

    expect(text()).toContain(LABELS.errors.decisionFailed);
    expect(fixture.nativeElement.querySelector('.progress-step')).toBeNull();
    button(LABELS.progress.retry).click();
    button(LABELS.progress.backToForm).click();
    expect(store.retry).toHaveBeenCalledTimes(1);
    expect(store.backToForm).toHaveBeenCalledTimes(1);
  });

  it('shows the recorded Polish rate-limit detail from T26', async () => {
    const recording = await loadJsonRecording<{ detail: string }>('problem-429');
    store.errorDetail.set(recording.body.detail);
    store.errorCode.set('RATE_LIMITED');
    store.progress.set({ assessment: 'running', decision: 'waiting', error: true });
    fixture.detectChanges();
    expect(text()).toContain(recording.body.detail);
  });

  it('shows Nowe zgłoszenie instead of retry for an invalid signature', () => {
    store.errorCode.set('INVALID_CASE_SIGNATURE');
    store.errorDetail.set(LABELS.errors.invalidSignature);
    store.progress.set({ assessment: 'done', decision: 'running', error: true });
    fixture.detectChanges();

    expect(text()).toContain(LABELS.errors.invalidSignature);
    expect(Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).map((item) => item.textContent?.trim()))
      .toContain(LABELS.newCase);
    expect(fixture.nativeElement.textContent).not.toContain(LABELS.progress.retry);
    button(LABELS.newCase).click();
    expect(store.newCase).toHaveBeenCalledTimes(1);
    expect(store.backToForm).not.toHaveBeenCalled();
  });

  it('keeps the first recorded decision fixture shape available to screen integrations', async () => {
    const recording = await loadJsonRecording<DecisionResponse>('decision-return-ok');
    expect(recording.body.caseContext.assessment.photoSufficient).toBe(true);
  });
});
