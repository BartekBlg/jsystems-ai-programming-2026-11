import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { TextFieldModule } from '@angular/cdk/text-field';
import { CASE_TYPE_DESCRIPTIONS, CASE_TYPE_LABELS, CATEGORY_LABELS, LABELS } from '../core/labels';
import { CaseForm, CaseType, EquipmentCategory, FormDraft } from '../core/models';
import { PhotoFileError, PhotoPickerComponent } from './photo-picker';
import {
  REASON_MAX,
  modelNameValidator,
  parseIsoLocalDate,
  purchaseDateNotFutureValidator,
  reasonValidator,
  toIsoLocalDate,
} from './form-validators';

type FieldName = 'caseType' | 'category' | 'modelName' | 'purchaseDate' | 'reason' | 'photo';

const FIELD_ORDER: readonly FieldName[] = [
  'caseType',
  'category',
  'modelName',
  'purchaseDate',
  'reason',
  'photo',
];

/** Error key -> Polish message, per field. Server errors carry their message as the error value. */
const MESSAGES: Record<FieldName, Record<string, string>> = {
  caseType: { required: LABELS.errors.caseTypeRequired },
  category: { required: LABELS.errors.categoryRequired },
  modelName: { modelName: LABELS.errors.modelName, required: LABELS.errors.modelName },
  purchaseDate: {
    required: LABELS.errors.purchaseDateRequired,
    matDatepickerParse: LABELS.errors.purchaseDateRequired,
    purchaseDateFuture: LABELS.errors.purchaseDateFuture,
    matDatepickerMax: LABELS.errors.purchaseDateFuture,
  },
  reason: {
    reasonComplaint: LABELS.errors.reasonComplaint,
    required: LABELS.errors.reasonComplaint,
    reasonTooLong: LABELS.errors.reasonTooLong,
    maxlength: LABELS.errors.reasonTooLong,
  },
  photo: {
    photoTooLarge: LABELS.errors.photoTooLarge,
    photoUnsupported: LABELS.errors.photoUnsupported,
    required: LABELS.errors.photoRequired,
  },
};

/** Form screen (PRD §9.1, AC-01..AC-14). Emits only a valid, trimmed form and its photo; it performs no HTTP. */
@Component({
  selector: 'app-case-form',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatSelectModule,
    TextFieldModule,
    PhotoPickerComponent,
  ],
  templateUrl: './case-form.html',
  styleUrl: './case-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CaseFormComponent {
  readonly initialDraft = input<FormDraft | null>(null);
  readonly initialPhoto = input<File | null>(null);
  readonly submitting = input(false);
  readonly interruptedNotice = input(false);
  readonly serverFieldErrors = input<{ field: string; message: string }[]>([]);
  readonly submitted = output<{ form: CaseForm; photo: File }>();

  protected readonly labels = LABELS.form;
  protected readonly caseTypes: readonly CaseType[] = ['RETURN', 'COMPLAINT'];
  protected readonly categories = Object.keys(CATEGORY_LABELS) as EquipmentCategory[];
  protected readonly caseTypeLabels = CASE_TYPE_LABELS;
  protected readonly caseTypeDescriptions = CASE_TYPE_DESCRIPTIONS;
  protected readonly categoryLabels = CATEGORY_LABELS;
  protected readonly maxReasonLength = REASON_MAX;
  protected readonly today = new Date();
  protected readonly submitAttempted = signal(false);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private locked = false;
  private sawSubmitting = false;

  private readonly caseTypeControl = new FormControl<CaseType | null>(null, Validators.required);

  protected readonly form = new FormGroup({
    caseType: this.caseTypeControl,
    category: new FormControl<EquipmentCategory | null>(null, Validators.required),
    modelName: new FormControl('', { nonNullable: true, validators: [modelNameValidator] }),
    purchaseDate: new FormControl<Date | null>(null, [
      Validators.required,
      purchaseDateNotFutureValidator,
    ]),
    reason: new FormControl('', {
      nonNullable: true,
      validators: [reasonValidator(this.caseTypeControl)],
    }),
    photo: new FormControl<File | null>(null, Validators.required),
  });

  private readonly caseTypeValue = toSignal(this.caseTypeControl.valueChanges, {
    initialValue: this.caseTypeControl.value,
  });
  private readonly reasonValue = toSignal(this.form.controls.reason.valueChanges, {
    initialValue: this.form.controls.reason.value,
  });

  protected readonly isComplaint = computed(() => this.caseTypeValue() === 'COMPLAINT');
  protected readonly reasonLabel = computed(() =>
    this.caseTypeValue() === 'RETURN' ? LABELS.form.reasonOptional : LABELS.form.reasonNeutral,
  );
  protected readonly reasonLength = computed(() => (this.reasonValue() ?? '').length);
  protected readonly photoHint = computed(() => {
    const type = this.caseTypeValue();
    return type ? LABELS.photoHint[type] : null;
  });

  constructor() {
    this.caseTypeControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.form.controls.reason.updateValueAndValidity();
    });

    effect(() => {
      const errors = this.serverFieldErrors();
      untracked(() => {
        for (const { field, message } of errors) {
          const control = this.controlFor(field);
          control?.setErrors({ server: message });
        }
      });
    });

    effect(() => {
      const submitting = this.submitting();
      if (submitting) {
        this.sawSubmitting = true;
      } else if (this.sawSubmitting) {
        this.sawSubmitting = false;
        this.locked = false;
      }
    });
  }

  ngOnInit(): void {
    const draft = this.initialDraft();
    if (draft) {
      this.form.patchValue({
        caseType: draft.caseType,
        category: draft.category,
        modelName: draft.modelName,
        purchaseDate: draft.purchaseDate ? parseIsoLocalDate(draft.purchaseDate) : null,
        reason: draft.reason,
      });
    }
    const photo = this.initialPhoto();
    if (photo) {
      this.form.controls.photo.setValue(photo);
    }
  }

  protected onSubmit(): void {
    if (this.submitting() || this.locked) {
      return;
    }
    this.submitAttempted.set(true);
    this.clearServerErrors();
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.focusFirstInvalid();
      return;
    }

    const value = this.form.getRawValue();
    const photo = value.photo;
    if (!value.caseType || !value.category || !value.purchaseDate || !photo) {
      return;
    }
    this.locked = true;
    this.submitted.emit({
      form: {
        caseType: value.caseType,
        category: value.category,
        modelName: value.modelName.trim(),
        purchaseDate: toIsoLocalDate(value.purchaseDate),
        reason: value.reason.trim(),
      },
      photo,
    });
  }

  protected onPhotoError(error: PhotoFileError): void {
    this.form.controls.photo.setErrors({ [error]: true });
  }

  /** Message for a field, shown only after the user touched it or a submit was attempted (AC-13). */
  protected visibleError(name: FieldName): string | null {
    const control = this.controlFor(name);
    if (!control || control.valid || !(control.touched || this.submitAttempted())) {
      return null;
    }
    const errors = control.errors ?? {};
    if (typeof errors['server'] === 'string') {
      return errors['server'];
    }
    for (const key of Object.keys(errors)) {
      const message = MESSAGES[name][key];
      if (message) {
        return message;
      }
    }
    return null;
  }

  private clearServerErrors(): void {
    for (const control of Object.values(this.form.controls) as AbstractControl[]) {
      if (control.hasError('server')) {
        control.updateValueAndValidity();
      }
    }
  }

  private focusFirstInvalid(): void {
    const name = FIELD_ORDER.find((field) => this.form.controls[field].invalid);
    if (!name) {
      return;
    }
    const host = this.host.nativeElement.querySelector<HTMLElement>(`[formcontrolname="${name}"]`);
    if (!host) {
      return;
    }
    const target = host.matches('input, textarea')
      ? host
      : host.querySelector<HTMLElement>('input[type="radio"], [role="combobox"], button');
    target?.focus();
  }

  private controlFor(name: string): AbstractControl | null {
    return (this.form.controls as Record<string, AbstractControl | undefined>)[name] ?? null;
  }
}
