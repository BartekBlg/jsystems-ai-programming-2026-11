import { TestBed } from '@angular/core/testing';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { PlDateAdapter } from './pl-date-adapter';

describe('PlDateAdapter', () => {
  let adapter: PlDateAdapter;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: MAT_DATE_LOCALE, useValue: 'pl-PL' },
        { provide: DateAdapter, useClass: PlDateAdapter },
      ],
    });
    adapter = TestBed.inject(DateAdapter) as PlDateAdapter;
  });

  it('parses D.M.YYYY and DD.MM.YYYY', () => {
    expect(adapter.parse('21.11.2026', null)).toEqual(new Date(2026, 10, 21));
    expect(adapter.parse('5.3.2026', null)).toEqual(new Date(2026, 2, 5));
    expect(adapter.parse(' 05.03.2026 ', null)).toEqual(new Date(2026, 2, 5));
  });

  it('parses ISO YYYY-MM-DD as a local date', () => {
    expect(adapter.parse('2026-11-21', null)).toEqual(new Date(2026, 10, 21));
  });

  it('rejects invalid dates and keeps empty input empty', () => {
    expect(adapter.isValid(adapter.parse('31.02.2026', null) as Date)).toBe(false);
    expect(adapter.isValid(adapter.parse('2026-02-31', null) as Date)).toBe(false);
    expect(adapter.isValid(adapter.parse('abc', null) as Date)).toBe(false);
    expect(adapter.parse('', null)).toBeNull();
  });

  it('passes Date objects and timestamps through', () => {
    const date = new Date(2026, 0, 2);
    expect(adapter.parse(date, null)).toEqual(date);
  });

  it('formats as DD.MM.YYYY', () => {
    expect(adapter.format(new Date(2026, 2, 5), null)).toBe('05.03.2026');
    expect(adapter.format(new Date(2026, 10, 21), null)).toBe('21.11.2026');
  });
});
