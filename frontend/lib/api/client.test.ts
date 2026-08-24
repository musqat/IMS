import { describe, it, expect } from 'vitest';
import { AxiosError, AxiosHeaders } from 'axios';
import { apiClient, unwrap, isAuthError, getApiError, getErrorCode } from './client';

function axiosErrorWith(status: number, data?: unknown) {
  const err = new AxiosError('boom');
  err.response = {
    status,
    data,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
  };
  return err;
}

/**
 * 인증 실패와 그 밖의 실패를 구분한다.
 * 구분하지 않고 로그아웃시키면 서버가 잠시 불안정할 때 세션이 날아간다.
 */
describe('isAuthError', () => {
  it('401만 인증 실패다', () => {
    expect(isAuthError(axiosErrorWith(401))).toBe(true);
  });

  it.each([500, 502, 403])('%d 는 인증 실패가 아니다', (status) => {
    expect(isAuthError(axiosErrorWith(status))).toBe(false);
  });

  it('네트워크 단절은 인증 실패가 아니다 — response 자체가 없다', () => {
    expect(isAuthError(new AxiosError('Network Error'))).toBe(false);
  });

  it('axios가 아닌 예외는 인증 실패가 아니다', () => {
    expect(isAuthError(new Error('그냥 예외'))).toBe(false);
  });
});

/**
 * HTTP status만으로는 원인을 못 가린다.
 * 409 하나에 중복 등록·재고 잔존·생산 기록 잔존·비활성 창고가 전부 몰린다.
 */
describe('getErrorCode', () => {
  it('응답 본문의 code를 꺼낸다', () => {
    expect(getErrorCode(axiosErrorWith(409, { code: 'WAREHOUSE_HAS_INVENTORY' })))
      .toBe('WAREHOUSE_HAS_INVENTORY');
  });

  it('code가 없으면 null', () => {
    expect(getErrorCode(axiosErrorWith(409, { message: '실패' }))).toBeNull();
  });

  it('axios가 아니면 null', () => {
    expect(getErrorCode(new Error('x'))).toBeNull();
  });
});

describe('getApiError', () => {
  it('백엔드 message를 그대로 쓴다', () => {
    expect(getApiError(axiosErrorWith(400, { message: '수량은 1 이상이어야 합니다' })))
      .toBe('수량은 1 이상이어야 합니다');
  });

  it('message가 없으면 fallback', () => {
    expect(getApiError(axiosErrorWith(500, {}), '잠시 후 다시 시도해 주세요'))
      .toBe('잠시 후 다시 시도해 주세요');
  });
});

describe('unwrap', () => {
  it('ApiResponse의 data를 벗긴다', () => {
    expect(unwrap({ data: { data: { id: 1 } } })).toEqual({ id: 1 });
  });
});

/**
 * undefined 파라미터가 문자열 "undefined"로 나가면 서버가 그걸 값으로 받는다.
 */
describe('paramsSerializer', () => {
  const serialize = apiClient.defaults.paramsSerializer as unknown as
    (p: Record<string, unknown>) => string;

  it('undefined·null은 뺀다', () => {
    expect(serialize({ page: 0, keyword: undefined, status: null })).toBe('page=0');
  });

  it('배열은 같은 키를 반복한다', () => {
    expect(serialize({ status: ['PENDING', 'SETTLED'] }))
      .toBe('status=PENDING&status=SETTLED');
  });

  it('0과 빈 문자열은 남긴다 — falsy라고 빼면 안 된다', () => {
    expect(serialize({ page: 0, keyword: '' })).toBe('page=0&keyword=');
  });
});
