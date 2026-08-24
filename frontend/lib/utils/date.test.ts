import { describe, it, expect, vi, afterEach } from 'vitest';
import { toLocalDateString, daysUntil, daysAgo, monthsAgo } from './date';

describe('toLocalDateString', () => {
  it('KST 자정 직후에도 날짜가 하루 밀리지 않는다', () => {
    // 실제로 겪은 결함이다. toISOString()은 UTC로 변환하므로
    // KST 09시 이전에는 전날 날짜가 나온다.
    const d = new Date(2026, 7, 25, 0, 30); // 2026-08-25 00:30 로컬
    expect(toLocalDateString(d)).toBe('2026-08-25');
    expect(d.toISOString().slice(0, 10)).not.toBe('2026-08-25');
  });

  it('한 자리 월·일을 0으로 채운다', () => {
    expect(toLocalDateString(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('연말에도 연도가 맞다', () => {
    expect(toLocalDateString(new Date(2026, 11, 31, 23, 59))).toBe('2026-12-31');
  });
});

describe('daysUntil', () => {
  afterEach(() => vi.useRealTimers());

  const freeze = (iso: string) => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(iso));
  };

  it('만료 전이면 양수', () => {
    freeze('2026-08-25T00:00:00Z');
    expect(daysUntil('2026-08-28T00:00:00Z')).toBe(3);
  });

  it('이미 지났으면 음수', () => {
    freeze('2026-08-25T00:00:00Z');
    expect(daysUntil('2026-08-22T00:00:00Z')).toBe(-3);
  });

  it('만료가 없으면 null', () => {
    expect(daysUntil(null)).toBeNull();
  });

  it('남은 시간이 하루 미만이어도 올림해서 1이다 — "만료됨"으로 보이면 안 된다', () => {
    freeze('2026-08-25T00:00:00Z');
    expect(daysUntil('2026-08-25T01:00:00Z')).toBe(1);
  });
});

describe('daysAgo / monthsAgo', () => {
  afterEach(() => vi.useRealTimers());

  it('월 경계를 넘어간다', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 8, 2, 12, 0)); // 2026-09-02
    expect(daysAgo(5)).toBe('2026-08-28');
  });

  it('연 경계를 넘어간다', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 0, 15, 12, 0)); // 2026-01-15
    expect(monthsAgo(3)).toBe('2025-10-15');
  });

  it('0이면 오늘', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 7, 25, 12, 0));
    expect(daysAgo(0)).toBe('2026-08-25');
  });
});
