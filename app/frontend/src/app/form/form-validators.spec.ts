import { toIsoLocalDate } from './form-validators';

describe('toIsoLocalDate', () => {
  it('builds YYYY-MM-DD from local date parts, not from UTC', () => {
    expect(toIsoLocalDate(new Date(2024, 0, 15, 0, 30))).toBe('2024-01-15');
    expect(toIsoLocalDate(new Date(2024, 11, 31, 23, 59))).toBe('2024-12-31');
  });

  it('zero-pads month and day', () => {
    expect(toIsoLocalDate(new Date(2025, 2, 4, 12))).toBe('2025-03-04');
  });
});
