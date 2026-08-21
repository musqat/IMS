'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useWarehouses } from '@/hooks/queries/useWarehouses';
import { useProductionCounts } from '@/hooks/queries/useProductions';
import { useShortageAnalysis } from '@/hooks/queries/useInventories';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Warehouse, AlertTriangle, Clock, Activity, PackageX, ChevronDown, ChevronRight, Loader2 } from 'lucide-react';
import { ErrorState } from '@/components/common/ErrorState';
import { isQueryFailed } from '@/lib/utils/queryState';
import type { WarehouseResponse } from '@/lib/types';

function WarehouseShortageRow({ warehouse }: { warehouse: WarehouseResponse }) {
  const [open, setOpen] = useState(false);
  const { data: shortages = [], isLoading } = useShortageAnalysis(open ? warehouse.id : 0);

  return (
    <div>
      <button
        onClick={() => setOpen(!open)}
        className="w-full flex items-center justify-between py-3 px-1 text-left hover:bg-stone-50 rounded transition-colors"
      >
        <div className="flex items-center gap-2">
          {open
            ? <ChevronDown className="h-3.5 w-3.5 text-stone-400" />
            : <ChevronRight className="h-3.5 w-3.5 text-stone-400" />}
          <span className="text-sm font-medium text-stone-700">{warehouse.name}</span>
        </div>
        {isLoading ? (
          <Loader2 className="h-3.5 w-3.5 text-stone-300 animate-spin" />
        ) : shortages.length > 0 ? (
          <span className="inline-flex items-center rounded-full bg-rose-50 px-2 py-0.5 text-xs font-medium text-rose-600">
            {shortages.length}개 품목 부족
          </span>
        ) : open ? (
          <span className="text-xs text-stone-400">이상 없음</span>
        ) : null}
      </button>

      {open && (
        <div className="pb-3 pl-6">
          {isLoading ? (
            <p className="text-stone-400 text-sm flex items-center gap-1.5">
              <Loader2 className="h-3.5 w-3.5 animate-spin" /> 분석 중...
            </p>
          ) : shortages.length === 0 ? (
            <p className="text-stone-400 text-sm">모든 완제품을 생산할 수 있습니다.</p>
          ) : (
            <div className="space-y-3">
              {shortages.map((item) => (
                <div key={item.itemId} className="space-y-1.5">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-stone-800">{item.itemName}</span>
                    <span className="text-xs text-stone-400 font-mono">{item.itemCode}</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {item.shortages.map((s) => (
                      <span
                        key={s.partId}
                        className="inline-flex items-center gap-1 rounded-md bg-rose-50 px-2 py-0.5 text-xs text-rose-600 border border-rose-100"
                      >
                        {s.partName}
                        <span className="text-rose-400">{s.currentStock}/{s.requiredPerUnit}개</span>
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function DashboardPage() {
  const warehousesQuery = useWarehouses();
  const countsQuery = useProductionCounts();

  const warehouses = warehousesQuery.data ?? [];
  const counts = countsQuery.data;

  const isLoading = warehousesQuery.isLoading || countsQuery.isLoading;
  // 하나라도 실패하면 KPI 숫자가 0으로 보인다. 0건과 조회 실패를 구분해서 보여준다
  const isError = isQueryFailed(warehousesQuery) || isQueryFailed(countsQuery);

  const retry = () => {
    warehousesQuery.refetch();
    countsQuery.refetch();
  };

  const totalProductions = counts?.total ?? 0;
  const totalPending = counts?.pending ?? 0;
  const totalAnomalies = counts?.anomaly ?? 0;

  const kpis = [
    {
      title: '보유 창고',
      value: warehouses.length,
      icon: Warehouse,
      href: '/warehouses',
      accent: true,
    },
    {
      title: '전체 생산 기록',
      value: totalProductions,
      icon: Activity,
      href: '/production',
      accent: true,
    },
    {
      title: '미결산 잔여',
      value: totalPending,
      icon: Clock,
      href: '/production',
      accent: totalPending > 0,
      accentColor: 'text-amber-500',
    },
    {
      title: '확인필요',
      value: totalAnomalies,
      icon: AlertTriangle,
      href: '/production',
      accent: totalAnomalies > 0,
      accentColor: 'text-rose-600',
    },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-stone-900">대시보드</h1>

      {isError && (
        <Card className="border-stone-200">
          <CardContent className="pt-6">
            <ErrorState onRetry={retry} />
          </CardContent>
        </Card>
      )}

      {/* KPI 카드 */}
      {!isError && (
      <div className="grid grid-cols-4 gap-4">
        {isLoading
          ? Array.from({ length: 4 }).map((_, i) => (
              <Card key={i} className="border-stone-200">
                <CardHeader className="pb-2 flex flex-row items-center justify-between">
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-4 w-4 rounded-full" />
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-9 w-12" />
                </CardContent>
              </Card>
            ))
          : kpis.map((kpi) => {
              const valueColor = kpi.accentColor
                ? kpi.accent ? kpi.accentColor : 'text-stone-300'
                : 'text-stone-900';
              const iconColor = kpi.accentColor
                ? kpi.accent ? kpi.accentColor : 'text-stone-300'
                : 'text-violet-500';

              return (
                <Link key={kpi.title} href={kpi.href}>
                  <Card className="border-stone-200 hover:border-stone-300 hover:shadow-sm transition-all cursor-pointer">
                    <CardHeader className="pb-2 flex flex-row items-center justify-between">
                      <CardTitle className="text-sm text-stone-500">{kpi.title}</CardTitle>
                      <kpi.icon className={`h-4 w-4 ${iconColor}`} />
                    </CardHeader>
                    <CardContent>
                      <p className={`text-3xl font-bold ${valueColor}`}>{kpi.value}</p>
                    </CardContent>
                  </Card>
                </Link>
              );
            })}
      </div>
      )}

      {/* 생산 불가 완제품 - 창고별 */}
      {!isError && (
      <Card className="border-stone-200">
        <CardHeader className="flex flex-row items-center gap-2 pb-3">
          <PackageX className="h-4 w-4 text-rose-400" />
          <CardTitle className="text-base">현재 생산 불가 완제품</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3 py-1">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : warehouses.length === 0 ? (
            <p className="text-stone-400 text-sm py-2">등록된 창고가 없습니다.</p>
          ) : (
            <div className="divide-y divide-stone-100">
              {warehouses.map((w) => (
                <WarehouseShortageRow key={w.id} warehouse={w} />
              ))}
            </div>
          )}
        </CardContent>
      </Card>
      )}
    </div>
  );
}
