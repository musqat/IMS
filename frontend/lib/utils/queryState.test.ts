import { describe, it, expect } from 'vitest';
import { isQueryFailed } from './queryState';

/**
 * 조회 실패를 "데이터 없음"과 구분하기 위한 판정이다.
 * isError만 보면 서버에 아예 닿지 못한 경우를 놓친다.
 */
describe('isQueryFailed', () => {
  it('5xx 응답은 실패다', () => {
    expect(isQueryFailed({ isError: true, isPending: false, fetchStatus: 'idle' })).toBe(true);
  });

  it('서버에 닿지 못한 경우도 실패다 — isError가 아직 false다', () => {
    expect(isQueryFailed({ isError: false, isPending: true, fetchStatus: 'paused' })).toBe(true);
  });

  it('정상 로딩 중은 실패가 아니다', () => {
    expect(isQueryFailed({ isError: false, isPending: true, fetchStatus: 'fetching' })).toBe(false);
  });

  it('성공은 실패가 아니다', () => {
    expect(isQueryFailed({ isError: false, isPending: false, fetchStatus: 'idle' })).toBe(false);
  });

  it('데이터가 있는 채로 백그라운드 재조회가 멈춰도 실패가 아니다', () => {
    // isPending이 false면 이미 받아둔 데이터가 있다. 화면이 비지 않는다
    expect(isQueryFailed({ isError: false, isPending: false, fetchStatus: 'paused' })).toBe(false);
  });
});
