import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';

@Component({
  selector: 'app-confirm-new-case-dialog',
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './confirm-new-case.dialog.html',
  styleUrl: './confirm-new-case.dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmNewCaseDialogComponent {
  protected readonly labels = LABELS.confirmNewCase;
  private readonly store = inject(CaseStore);
  private readonly dialogRef = inject(MatDialogRef<ConfirmNewCaseDialogComponent>);

  protected cancel(): void {
    this.dialogRef.close(false);
  }

  protected confirm(): void {
    this.store.newCase();
    this.dialogRef.close(true);
  }
}
