import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CASE_TYPE_LABELS, CATEGORY_LABELS, DECISION_LABELS, LABELS } from '../core/labels';
import { CaseStore } from '../core/case-store';
import { DecisionCategory } from '../core/models';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { PhotoPreviewDialogComponent } from './photo-preview.dialog';

@Component({
  selector: 'app-case-summary',
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './case-summary.html',
  styleUrl: './case-summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CaseSummaryComponent {
  protected readonly store = inject(CaseStore);
  private readonly dialog = inject(MatDialog);
  protected readonly labels = LABELS.chat;
  protected readonly caseTypeLabels = CASE_TYPE_LABELS;
  protected readonly categoryLabels = CATEGORY_LABELS;
  protected readonly decisionLabels = DECISION_LABELS;
  protected readonly reasonExpanded = signal(false);
  protected readonly decision = computed(
    () => this.store.currentDecision() ?? this.store.caseContext()?.decision?.category ?? null,
  );
  protected readonly decisionClass = computed(() => badgeClass(this.decision()));

  protected toggleReason(): void {
    this.reasonExpanded.update((value) => !value);
  }

  protected formatDate(value: string): string {
    const date = new Date(`${value}T00:00:00Z`);
    return Number.isNaN(date.getTime())
      ? value
      : new Intl.DateTimeFormat('pl-PL', {
          day: 'numeric',
          month: 'long',
          year: 'numeric',
          timeZone: 'UTC',
        }).format(date);
  }

  protected openPhoto(): void {
    const src = this.store.thumbnailDataUrl();
    if (src) {
      this.dialog.open(PhotoPreviewDialogComponent, {
        data: { src, alt: LABELS.chat.photo },
        maxWidth: 'calc(100vw - 32px)',
      });
    }
  }
}

function badgeClass(category: DecisionCategory | null): string {
  switch (category) {
    case 'ACCEPTED':
      return 'decision-badge--accepted';
    case 'REJECTED':
      return 'decision-badge--rejected';
    case 'MORE_INFO_NEEDED':
      return 'decision-badge--more-info-needed';
    case 'ESCALATED':
      return 'decision-badge--escalated';
    default:
      return '';
  }
}
