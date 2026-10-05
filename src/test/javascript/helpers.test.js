const { requireApp } = require('./setup/loadApp');

const { formatRate, formatMoney, barWidths, applyPreset, daysUntil } = requireApp();

describe('formatRate', () => {
  test('renders a ratio as a percentage with one decimal', () => {
    expect(formatRate(0.8981)).toBe('89.8%');
  });

  test('renders 1 as 100.0% and 0 as 0.0%', () => {
    expect(formatRate(1)).toBe('100.0%');
    expect(formatRate(0)).toBe('0.0%');
  });

  test('renders null and undefined as n/a', () => {
    expect(formatRate(null)).toBe('n/a');
    expect(formatRate(undefined)).toBe('n/a');
  });
});

describe('formatMoney', () => {
  test('adds the pound sign, thousands separators and two decimals', () => {
    expect(formatMoney(360095.5)).toBe('£360,095.50');
  });

  test('handles small and zero amounts', () => {
    expect(formatMoney(18.5)).toBe('£18.50');
    expect(formatMoney(0)).toBe('£0.00');
    expect(formatMoney(null)).toBe('£0.00');
  });
});

describe('barWidths', () => {
  test('scales values so the largest fills the given length', () => {
    expect(barWidths([10, 5, 0], 200)).toEqual([200, 100, 0]);
  });

  test('uses an explicit maximum when given', () => {
    expect(barWidths([0.5, 1], 300, 1)).toEqual([150, 300]);
  });

  test('returns an empty list for no values', () => {
    expect(barWidths([], 200)).toEqual([]);
  });
});

describe('applyPreset', () => {
  test('7 days ending on today', () => {
    expect(applyPreset(7, '2026-09-21')).toEqual({ from: '2026-09-14', to: '2026-09-21' });
  });

  test('30 days crosses the month boundary', () => {
    expect(applyPreset(30, '2026-09-21')).toEqual({ from: '2026-08-22', to: '2026-09-21' });
  });

  test('90 days reaches back to June', () => {
    expect(applyPreset(90, '2026-09-21')).toEqual({ from: '2026-06-23', to: '2026-09-21' });
  });
});

describe('daysUntil', () => {
  test('counts the days from today to a future date', () => {
    expect(daysUntil('2026-10-15', '2026-09-21')).toBe(24);
  });

  test('is zero for today', () => {
    expect(daysUntil('2026-09-21', '2026-09-21')).toBe(0);
  });

  test('is negative for a date that has passed', () => {
    expect(daysUntil('2026-09-01', '2026-09-21')).toBe(-20);
  });
});
