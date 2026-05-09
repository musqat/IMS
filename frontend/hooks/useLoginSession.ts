'use client';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/store/authStore';
import type { LoginResponse } from '@/lib/types';

/**
 * 토큰 수령 후 getMe → setAuth → /dashboard 공통 시퀀스
 * LoginForm, RegisterForm 에서 공유
 */
export function useLoginSession() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);

  const loginWithTokens = async (tokens: LoginResponse) => {
    // ① getMe() 호출 전에 먼저 저장 — apiClient 인터셉터가 Authorization 헤더에 첨부하려면
    //   localStorage에 토큰이 있어야 함. setAuth 내부에서도 저장하지만 그건 getMe() 이후
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    // ② 유저 정보 조회 후 Zustand 상태 초기화 (setAuth 내부에서 localStorage 재저장은 idempotent)
    const user = await authApi.getMe();
    setAuth(user, tokens.accessToken, tokens.refreshToken);
    router.push('/dashboard');
  };

  return { loginWithTokens };
}
