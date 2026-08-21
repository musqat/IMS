'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

/**
 * JWT payload의 exp 클레임을 외부 라이브러리 없이 디코딩
 * 서명 검증은 하지 않으며, 만료 여부(클라이언트 클럭 기준)만 확인
 */
function isTokenExpired(token: string): boolean {
  try {
    const payload = token.split('.')[1];
    // base64url → base64 변환 후 디코딩
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const { exp } = JSON.parse(decoded) as { exp?: number };
    if (!exp) return true;
    // exp는 Unix 초 단위; Date.now()는 밀리초
    return Date.now() >= exp * 1000;
  } catch {
    return true; // 디코딩 실패 → 만료로 간주
  }
}

/**
 * 인증 가드 — 마운트 시 1회 토큰 상태 확인
 * - accessToken이 만료됐더라도 refreshToken이 살아 있으면 통과시킨다.
 *   첫 API 호출이 401을 받고 client.ts 인터셉터가 갱신한다
 * - 둘 다 못 쓰는 상태일 때만 정리하고 /login으로 보낸다
 *
 */
export function useAuthGuard() {
  const router = useRouter();

  useEffect(() => {
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');

    // accessToken이 아직 살아 있으면 그대로 진행
    if (accessToken && !isTokenExpired(accessToken)) return;

    // 만료됐어도 refreshToken이 유효하면 인터셉터가 갱신하도록 맡긴다
    if (refreshToken && !isTokenExpired(refreshToken)) return;

    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    router.replace('/login');
  }, [router]);
}
