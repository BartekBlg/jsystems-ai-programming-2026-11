import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  forwardRef,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { LABELS } from '../core/labels';
import { detectFileImageKind, isWithinSizeLimit } from '../core/image-utils';

export type PhotoFileError = 'photoTooLarge' | 'photoUnsupported';

const MB = 1024 * 1024;

/**
 * Photo drop zone with a file input. Implements ControlValueAccessor so the form holds a File or null.
 * An invalid file is never kept: the value is reset to null and `fileError` tells the form which
 * message to show (the form then marks the control invalid). A previous valid photo is removed in that case.
 */
@Component({
  selector: 'app-photo-picker',
  imports: [MatButtonModule],
  templateUrl: './photo-picker.html',
  styleUrl: './photo-picker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PhotoPickerComponent),
      multi: true,
    },
  ],
})
export class PhotoPickerComponent implements ControlValueAccessor, OnDestroy {
  readonly hint = input<string | null>(null);
  readonly fileError = output<PhotoFileError>();

  protected readonly labels = LABELS.form;
  protected readonly file = signal<File | null>(null);
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly sizeLabel = computed(() => {
    const file = this.file();
    return file ? formatSize(file.size) : '';
  });

  private readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');
  private onChange: (value: File | null) => void = () => {};
  private onTouched: () => void = () => {};
  /** Incremented on every choice so a slow detection of an older file cannot overwrite a newer one. */
  private choiceId = 0;

  protected readonly acceptTypes = 'image/jpeg,image/png,image/webp';

  writeValue(value: File | null): void {
    this.setFile(value);
  }

  registerOnChange(fn: (value: File | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  ngOnDestroy(): void {
    this.revokePreview();
  }

  protected openFileDialog(): void {
    this.fileInput().nativeElement.click();
  }

  protected onInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.chooseFiles(input.files);
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.chooseFiles(event.dataTransfer?.files ?? null);
  }

  protected remove(): void {
    this.choiceId++;
    this.setFile(null);
    this.onTouched();
    this.onChange(null);
  }

  private chooseFiles(files: ArrayLike<File> | null): void {
    const file = files?.[0];
    if (file) {
      void this.accept(file);
    }
  }

  private async accept(file: File): Promise<void> {
    const id = ++this.choiceId;
    this.onTouched();
    if (!isWithinSizeLimit(file)) {
      this.reject('photoTooLarge');
      return;
    }
    const kind = await detectFileImageKind(file);
    if (id !== this.choiceId) {
      return;
    }
    if (!kind) {
      this.reject('photoUnsupported');
      return;
    }
    this.setFile(file);
    this.onChange(file);
  }

  private reject(error: PhotoFileError): void {
    this.choiceId++;
    this.setFile(null);
    this.onChange(null);
    this.fileError.emit(error);
  }

  private setFile(file: File | null): void {
    this.revokePreview();
    this.file.set(file);
    this.previewUrl.set(file ? URL.createObjectURL(file) : null);
  }

  private revokePreview(): void {
    const url = this.previewUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
  }
}

/** Polish decimal comma; MB with one decimal, KB below 1 MB. */
export function formatSize(bytes: number): string {
  if (bytes < MB) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${(bytes / MB).toFixed(1).replace('.', ',')} MB`;
}
