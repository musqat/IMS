'use client';
import { Menu, LogOut, Bell } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useAuthStore, useSidebarStore } from '@/store/authStore';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api/auth';
import { useReceivedInvites } from '@/hooks/queries/usePartnerships';

export function Header() {
  const toggle = useSidebarStore((s) => s.toggle);
  const { user, clearAuth } = useAuthStore();
  const router = useRouter();
  const { data: received = [] } = useReceivedInvites();

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

      {/*
        받은 초대가 있을 때만 뜬다. 0건에도 회색 종이 늘 떠 있으면 노이즈다.
        드롭다운은 두지 않는다 — 알림 종류가 초대 하나뿐이라 빈 껍데기가 된다
      */}
      {received.length > 0 && (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => router.push('/partners')}
          className="gap-1.5 text-stone-600 hover:text-violet-700 hover:bg-violet-50"
        >
          <Bell size={15} />
          받은 초대
          <Badge variant="default">{received.length}</Badge>
        </Button>
      )}

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
