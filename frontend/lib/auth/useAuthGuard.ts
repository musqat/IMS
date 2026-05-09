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
 * 인증 가드 — 마운트 시 1회 토큰 유무 및 만료 여부 확인
 * - 토큰 없거나 만료 시 /login으로 즉시 리다이렉트
 * - 만료된 토큰은 localStorage에서 제거
 * - 실제 갱신(refresh) 처리는 client.ts 인터셉터가 담당
 */
export function useAuthGuard() {
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      router.replace('/login');
      return;
    }
    if (isTokenExpired(token)) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      router.replace('/login');
    }
  }, []);
}
