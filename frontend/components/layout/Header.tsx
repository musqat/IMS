'use client';
import { Menu, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuthStore, useSidebarStore } from '@/store/authStore';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api/auth';

export function Header() {
  const toggle = useSidebarStore((s) => s.toggle);
  const { user, clearAuth } = useAuthStore();
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch {
      // 실패해도 클라이언트 로그아웃은 진행
    } finally {
      clearAuth();
      router.push('/login');
    }
  };

  return (
    <header className="h-14 bg-white border-b border-stone-200 flex items-center px-4 gap-4 shrink-0">
      <Button variant="ghost" size="icon" onClick={toggle} className="shrink-0">
        <Menu size={20} />
      </Button>

      <div className="ml-auto flex items-center gap-3">
        {user && (
          <div className="text-right">
            <p className="text-sm font-semibold text-stone-900 leading-tight">{user.companyName}</p>
            <p className="text-xs text-stone-400 leading-tight">{user.companyCode}</p>
          </div>
        )}
        <Button
          variant="ghost"
          size="sm"
          onClick={handleLogout}
          className="text-stone-500 hover:text-rose-600 hover:bg-rose-50 gap-1.5"
        >
          <LogOut size={15} />
          로그아웃
        </Button>
      </div>
    </header>
  );
}
