import { Injectable, Provider } from '@angular/core';
import { DateAdapter, MAT_DATE_FORMATS, MAT_NATIVE_DATE_FORMATS, NativeDateAdapter } from '@angular/material/core';

const PL_DATE = /^(\d{1,2})\.(\d{1,2})\.(\d{4})$/;
const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

/** Native adapter that also understands typed Polish dates (D.M.YYYY) and ISO dates. */
@Injectable()
export class PlDateAdapter extends NativeDateAdapter {
  override parse(value: unknown, parseFormat?: unknown): Date | null {
    if (typeof value !== 'string') {
      return super.parse(value, parseFormat);
    }
    const text = value.trim();
    if (!text) {
      return null;
    }
    const pl = PL_DATE.exec(text);
    const iso = ISO_DATE.exec(text);
    if (!pl && !iso) {
      return this.invalid();
    }
    const [year, month, day] = pl ? [+pl[3], +pl[2], +pl[1]] : [+iso![1], +iso![2], +iso![3]];
    const date = new Date(year, month - 1, day);
    const real = date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
    return real ? date : this.invalid();
  }

  override format(date: Date, displayFormat?: unknown): string {
    if (!this.isValid(date)) {
      throw Error('PlDateAdapter: Cannot format invalid date.');
    }
    if (displayFormat !== null && typeof displayFormat === 'object') {
      return super.format(date, displayFormat as Intl.DateTimeFormatOptions);
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`;
  }
}

export function providePlDateAdapter(): Provider[] {
  return [
    { provide: DateAdapter, useClass: PlDateAdapter },
    {
      provide: MAT_DATE_FORMATS,
      useValue: { ...MAT_NATIVE_DATE_FORMATS, display: { ...MAT_NATIVE_DATE_FORMATS.display, dateInput: 'DD.MM.YYYY' } },
    },
  ];
}
