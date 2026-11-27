import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';
import { StepStatus } from '../core/models';

/** Two-stage submission progress and its recoverable error state (PRD §9.2, AC-41). */
@Component({
  selector: 'app-progress-screen',
  imports: [MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './progress-screen.html',
  styleUrl: './progress-screen.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProgressScreenComponent {
  protected readonly store = inject(CaseStore);
  protected readonly labels = LABELS;
  protected readonly errorMessage = computed(
    () => this.store.errorDetail() ?? LABELS.errors.decisionFailed,
  );

  protected statusLabel(status: StepStatus): string {
    switch (status) {
      case 'running':
        return LABELS.progress.statusRunning;
      case 'done':
        return LABELS.progress.statusDone;
      default:
        return LABELS.progress.statusWaiting;
    }
  }
}
