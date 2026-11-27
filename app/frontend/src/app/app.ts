import { ChangeDetectionStrategy, Component, Injector, effect, inject, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChatScreenComponent } from './chat/chat-screen';
import { CaseStore } from './core/case-store';
import { LABELS } from './core/labels';
import { CaseForm } from './core/models';
import { CaseFormComponent } from './form/case-form';
import { ProgressScreenComponent } from './progress/progress-screen';

@Component({
  selector: 'app-root',
  imports: [MatButtonModule, CaseFormComponent, ProgressScreenComponent, ChatScreenComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly labels = LABELS;
  protected readonly store = inject(CaseStore);
  private readonly injector = inject(Injector);
  private readonly snackBar = inject(MatSnackBar);

  constructor() {
    this.store.init();
    effect(() => {
      if (this.store.storageWarning()) {
        untracked(() => this.snackBar.open(LABELS.errors.storageFailed, undefined, { duration: 8000 }));
      }
    });
  }

  protected submit(event: { form: CaseForm; photo: File }): void {
    void this.store.submit(event.form, event.photo);
  }

  protected async openNewCaseDialog(): Promise<void> {
    const { ConfirmNewCaseDialogComponent } = await import('./chat/confirm-new-case.dialog');
    const { MatDialog } = await import('@angular/material/dialog');
    this.injector.get(MatDialog).open(ConfirmNewCaseDialogComponent, { maxWidth: 'calc(100vw - 32px)' });
  }
}
