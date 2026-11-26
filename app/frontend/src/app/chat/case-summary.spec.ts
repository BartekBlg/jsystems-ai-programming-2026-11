import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CaseStore } from '../core/case-store';
import { DECISION_LABELS, LABELS } from '../core/labels';
import { CaseContext, DecisionCategory, DecisionResponse } from '../core/models';
import { loadJsonRecording, loadSseRecording } from '../../testing/recording-loader';
import { CaseSummaryComponent } from './case-summary';
import { PhotoPreviewDialogComponent } from './photo-preview.dialog';

describe('CaseSummaryComponent', () => {
  let fixture: ComponentFixture<CaseSummaryComponent>;
  let store: {
    caseContext: ReturnType<typeof signal<CaseContext | null>>;
    currentDecision: ReturnType<typeof signal<DecisionCategory | null>>;
    thumbnailDataUrl: ReturnType<typeof signal<string>>;
    photoFileName: ReturnType<typeof signal<string>>;
  };
  let returnDecision: DecisionResponse;

  beforeEach(async () => {
    returnDecision = (await loadJsonRecording<DecisionResponse>('decision-return-ok')).body;
    store = {
      caseContext: signal(returnDecision.caseContext),
      currentDecision: signal(returnDecision.decision.category),
      thumbnailDataUrl: signal(''),
      photoFileName: signal(''),
    };
    await TestBed.configureTestingModule({
      imports: [CaseSummaryComponent, MatDialogModule],
      providers: [{ provide: CaseStore, useValue: store }],
    }).compileComponents();
    fixture = TestBed.createComponent(CaseSummaryComponent);
    fixture.detectChanges();
  });

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function badge(): HTMLElement {
    return fixture.nativeElement.querySelector('.decision-badge') as HTMLElement;
  }

  it('shows every case summary field and a Polish-formatted purchase date from the recording', () => {
    expect(text()).toContain(LABELS.chat.summaryTitle);
    expect(text()).toContain(LABELS.chat.caseType);
    expect(text()).toContain('Zwrot');
    expect(text()).toContain(LABELS.chat.category);
    expect(text()).toContain('Laptop');
    expect(text()).toContain(LABELS.chat.model);
    expect(text()).toContain(returnDecision.caseContext.form.modelName);
    expect(text()).toContain(LABELS.chat.purchaseDate);
    expect(text()).toContain('21 listopada 2026');
    expect(text()).toContain(LABELS.chat.reason);
    expect(text()).toContain(LABELS.chat.noReason);
    expect(badge().textContent).toContain(DECISION_LABELS[returnDecision.decision.category]);
  });

  it('renders a text label and the category color class for each decision present in T26 recordings', async () => {
    const categories: Array<{ recording: string; className: string }> = [
      { recording: 'decision-return-ok', className: 'decision-badge--escalated' },
      { recording: 'decision-complaint-ok', className: 'decision-badge--rejected' },
      { recording: 'decision-insufficient', className: 'decision-badge--more-info-needed' },
    ];

    for (const { recording, className } of categories) {
      const item = (await loadJsonRecording<DecisionResponse>(recording)).body;
      store.caseContext.set(item.caseContext);
      store.currentDecision.set(item.decision.category);
      fixture.detectChanges();
      expect(badge().textContent).toContain(DECISION_LABELS[item.decision.category]);
      expect(badge().classList).toContain(className);
    }
  });

  it('updates the badge from the recorded decision event without changing the signed context', async () => {
    const complaint = (await loadJsonRecording<DecisionResponse>('decision-complaint-ok')).body;
    const update = await loadSseRecording('chat-decision-update');
    const categoryLine = update.text.split('\n').find((line) => line.startsWith('data: {"category"'))!;
    const category = (JSON.parse(categoryLine.slice('data: '.length)) as { category: DecisionCategory }).category;
    store.caseContext.set(complaint.caseContext);
    store.currentDecision.set(complaint.decision.category);
    fixture.detectChanges();
    expect(badge().textContent).toContain(DECISION_LABELS[complaint.decision.category]);

    store.currentDecision.set(category);
    fixture.detectChanges();

    expect(badge().textContent).toContain(DECISION_LABELS[category]);
    expect(badge().classList).toContain('decision-badge--escalated');
    expect(store.caseContext()).toBe(complaint.caseContext);
  });

  it('opens the photo preview dialog with the thumbnail and Polish alt text', () => {
    store.thumbnailDataUrl.set('data:image/jpeg;base64,RECORDED_THUMBNAIL');
    fixture.detectChanges();
    const dialog = fixture.debugElement.injector.get(MatDialog);
    const open = vi.spyOn(dialog, 'open');

    (fixture.nativeElement.querySelector('.photo-button') as HTMLButtonElement).click();

    expect(open).toHaveBeenCalledWith(
      PhotoPreviewDialogComponent,
      expect.objectContaining({
        data: { src: 'data:image/jpeg;base64,RECORDED_THUMBNAIL', alt: LABELS.chat.photo },
      }),
    );
  });
});
