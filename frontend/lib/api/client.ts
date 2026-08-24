import axios, { isAxiosError } from 'axios';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  paramsSerializer: (params) =>
    new URLSearchParams(
      Object.entries(params)
        .filter(([, v]) => v !== undefined && v !== null)
        .flatMap(([k, v]) =>
          Array.isArray(v) ? v.map((i) => [k, String(i)]) : [[k, String(v)]]
        )
    ).toString(),
});

// 요청 인터셉터: accessToken 자동 첨부
apiClient.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('accessToken');
    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터: 401 → refreshToken으로 재발급 시도
let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((p) => (token ? p.resolve(token) : p.reject(error)));
  failedQueue = [];
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const isRefreshEndpoint = original?.url?.includes('/users/refresh');
    if (error.response?.status === 401 && !original._retry && !isRefreshEndpoint) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token) => {
              original.headers.Authorization = `Bearer ${token}`;
              resolve(apiClient(original));
            },
            reject,
          });
        });
      }
      original._retry = true;
      isRefreshing = true;
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        // C1: refreshToken 없으면 네트워크 요청 없이 바로 로그아웃 처리
        if (!refreshToken) {
          processQueue(new Error('no refresh token'), null);
          localStorage.removeItem('accessToken');
          window.location.href = '/login';
          return Promise.reject(new Error('no refresh token'));
        }
        const res = await axios.post(`${BASE_URL}/users/refresh`, null, {
          headers: { Authorization: `Bearer ${refreshToken}` },
        });
        const tokens: { accessToken: string; refreshToken: string } = res.data.data;
        localStorage.setItem('accessToken', tokens.accessToken);
        localStorage.setItem('refreshToken', tokens.refreshToken);
        processQueue(null, tokens.accessToken);
        original.headers.Authorization = `Bearer ${tokens.accessToken}`;
        return apiClient(original);
      } catch (err) {
        processQueue(err, null);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  }
);

// ApiResponse<T> 언래퍼
export function unwrap<T>(res: { data: { data: T } }): T {
  return res.data.data;
}

// 타입 추론을 돕는 래핑 헬퍼:
export function unwrapAs<T>(): (res: { data: { data: T } }) => T {
  return (res) => res.data.data;
}

/**
 * 인증 실패(401)인지 판별한다.
 *
 * 네트워크 단절·타임아웃·5xx는 인증 실패가 아니다. 이를 구분하지 않고 로그아웃시키면
 * 서버가 잠시 불안정할 때 사용자 세션이 날아간다.
 */
export function isAuthError(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 401;
}

// 백엔드 ApiResponse.message 추출 (없으면 fallback 반환)
export function getApiError(error: unknown, fallback = '오류가 발생했습니다.'): string {
  if (isAxiosError(error) && error.response?.data?.message) {
    return error.response.data.message as string;
  }
  return fallback;
}

/**
 * 백엔드 ApiResponse.code 추출
 *
 * HTTP status만으로는 원인을 못 가린다. 409 하나만 해도 중복 등록, 재고 잔존,
 * 생산 기록 잔존, 비활성 창고가 전부 409다.
 */
export function getErrorCode(error: unknown): string | null {
  if (isAxiosError(error)) {
    return (error.response?.data?.code as string | undefined) ?? null;
  }
  return null;
}
