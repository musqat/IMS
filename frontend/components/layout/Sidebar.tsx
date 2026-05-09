'use client';
import {
  LayoutDashboard, Warehouse, Package, TrendingUp, CheckSquare,
  Users, BarChart3, FileSpreadsheet, Settings,
} from 'lucide-react';
import { NavItem } from './NavItem';
import { useSidebarStore } from '@/store/authStore';
import { cn } from '@/lib/utils';

const NAV_GROUPS = [
  {
    label: '대시보드',
    items: [{ href: '/dashboard', icon: LayoutDashboard, label: '대시보드' }],
  },
  {
    label: '마스터',
    items: [
      { href: '/warehouses', icon: Warehouse, label: '창고' },
      { href: '/items', icon: Package, label: '품목 · 자재 명세' },
    ],
  },
  {
    label: '운영',
    items: [
      { href: '/production', icon: TrendingUp, label: '생산' },
      { href: '/production/settlements', icon: CheckSquare, label: '결산' },
    ],
  },
  {
    label: '협업',
    items: [{ href: '/partners', icon: Users, label: '파트너' }],
  },
  {
    label: '데이터',
    items: [
      { href: '/analytics', icon: BarChart3, label: '분석' },
      { href: '/export', icon: FileSpreadsheet, label: '엑셀 Export' },
    ],
  },
  {
    label: '설정',
    items: [{ href: '/settings', icon: Settings, label: '설정' }],
  },
];

export function Sidebar() {
  const { collapsed } = useSidebarStore();
  return (
    <aside
      className={cn(
        'fixed left-0 top-0 h-full bg-white border-r border-stone-200 flex flex-col transition-all duration-200 z-30',
        collapsed ? 'w-16' : 'w-64'
      )}
    >
      {/* 로고 */}
      <div className={cn('flex items-center h-14 px-4 border-b border-stone-200', collapsed && 'justify-center px-2')}>
        <div className="w-7 h-7 rounded-md bg-stone-900 flex items-center justify-center text-white font-bold text-xs shrink-0">
          I
        </div>
        {!collapsed && <span className="ml-2 font-semibold text-stone-900">IMS</span>}
      </div>

      {/* 네비게이션 */}
      <nav className="flex-1 p-3 space-y-4 overflow-y-auto">
        {NAV_GROUPS.map((group) => (
          <div key={group.label}>
            {!collapsed && (
              <p className="text-[10px] uppercase tracking-widest text-stone-400 font-semibold px-3 mb-1">
                {group.label}
              </p>
            )}
            <div className="space-y-0.5">
              {group.items.map((item) => (
                <NavItem key={item.href} {...item} collapsed={collapsed} />
              ))}
            </div>
          </div>
        ))}
      </nav>
    </aside>
  );
}
