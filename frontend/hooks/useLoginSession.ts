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
  const setUser = useAuthStore((s) => s.setUser);

  const loginWithTokens = async (tokens: LoginResponse) => {
    // ① getMe() 호출 전에 저장 — apiClient 인터셉터가 Authorization 헤더에 첨부하려면
    //   localStorage에 토큰이 있어야 한다
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    // ② 유저 정보 조회 후 상태만 갱신한다. 토큰을 다시 쓰면 그 사이 일어난 갱신을 덮어쓴다
    const user = await authApi.getMe();
    setUser(user);
    router.push('/dashboard');
  };

  return { loginWithTokens };
}
