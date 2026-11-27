import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { CaseType } from '../core/models';

export const MODEL_NAME_MIN = 2;
export const MODEL_NAME_MAX = 100;
export const REASON_MAX = 2000;
export const REASON_COMPLAINT_MIN = 10;

/** Trimmed length must be within 2–100 characters (AC-04). */
export function modelNameValidator(control: AbstractControl): ValidationErrors | null {
  const length = String(control.value ?? '').trim().length;
  return length >= MODEL_NAME_MIN && length <= MODEL_NAME_MAX ? null : { modelName: true };
}

/** Reason: max 2000 raw characters; COMPLAINT needs at least 10 trimmed characters (AC-06, AC-07). */
export function reasonValidator(caseType: AbstractControl<CaseType | null>): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = String(control.value ?? '');
    if (raw.length > REASON_MAX) {
      return { reasonTooLong: true };
    }
    if (caseType.value === 'COMPLAINT' && raw.trim().length < REASON_COMPLAINT_MIN) {
      return { reasonComplaint: true };
    }
    return null;
  };
}

/** Compares local calendar days: today is allowed, tomorrow is not (AC-05). */
export function purchaseDateNotFutureValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as Date | null;
  if (!(value instanceof Date)) {
    return null;
  }
  return startOfDay(value).getTime() > startOfDay(new Date()).getTime()
    ? { purchaseDateFuture: true }
    : null;
}

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

/** YYYY-MM-DD from the local date parts (toISOString would shift by the timezone offset). */
export function toIsoLocalDate(date: Date): string {
  const year = String(date.getFullYear()).padStart(4, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Parses YYYY-MM-DD as a local date (new Date(string) would read it as UTC). */
export function parseIsoLocalDate(value: string): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) {
    return null;
  }
  return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
}
