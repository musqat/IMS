'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthGuard } from '@/lib/auth/useAuthGuard';
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';
import { useAuthStore, useSidebarStore } from '@/store/authStore';
import { authApi } from '@/lib/api/auth';

export default function MainLayout({ children }: { children: React.ReactNode }) {
  useAuthGuard();
  const router = useRouter();
  const collapsed = useSidebarStore((s) => s.collapsed);
  const { setUser, isAuthenticated } = useAuthStore();

  // 페이지 새로고침 시 Zustand 상태 복원 (이미 인증된 경우 스킵)
  useEffect(() => {
    if (isAuthenticated) return;
    // accessToken이 만료됐어도 getMe()가 401을 받고 인터셉터가 갱신한다.
    if (localStorage.getItem('refreshToken')) {
      authApi
        .getMe()
        .then(setUser)
        .catch(() => {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          router.replace('/login');
        });
    }
  }, [isAuthenticated, setUser, router]);

  return (
    <div className="flex h-screen bg-stone-50">
      <Sidebar />
      <div
        className={`flex flex-col flex-1 overflow-hidden transition-all duration-200 ${
          collapsed ? 'ml-16' : 'ml-64'
        }`}
      >
        <Header />
        <main className="flex-1 overflow-auto p-6">{children}</main>
      </div>
    </div>
  );
}
