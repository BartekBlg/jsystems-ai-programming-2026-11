import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { LABELS } from '../core/labels';

export interface PhotoPreviewData {
  src: string;
  alt: string;
}

@Component({
  selector: 'app-photo-preview-dialog',
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './photo-preview.dialog.html',
  styleUrl: './photo-preview.dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PhotoPreviewDialogComponent {
  protected readonly data = inject<PhotoPreviewData>(MAT_DIALOG_DATA);
  protected readonly labels = LABELS.chat;
  private readonly dialogRef = inject(MatDialogRef<PhotoPreviewDialogComponent>);

  protected close(): void {
    this.dialogRef.close();
  }
}
