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
  const { setAuth, isAuthenticated } = useAuthStore();

  // 페이지 새로고침 시 Zustand 상태 복원 (이미 인증된 경우 스킵)
  useEffect(() => {
    if (isAuthenticated) return;
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');
    if (accessToken && refreshToken) {
      authApi
        .getMe()
        .then((me) => setAuth(me, accessToken, refreshToken))
        .catch(() => {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          router.replace('/login');
        });
    }
  }, [isAuthenticated, setAuth, router]);

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
