'use client';
import { useState } from 'react';
import { useProductionsByStatus } from '@/hooks/queries/useProductions';
import { AnomalyDetailModal } from '@/components/settlement/AnomalyDetailModal';
import { CheckCircle2, AlertTriangle, Search, XCircle } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { ProductionResponse, SettlementResponse, SettlementResult } from '@/lib/types';

type FilterType = 'ALL' | SettlementResult;

const FILTERS: { value: FilterType; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'SUCCESS', label: '성공' },
  { value: 'ANOMALY', label: '확인필요' },
  { value: 'FAILED', label: '오류' },
];

export default function SettlementsPage() {
  const { data: pageData, isLoading } = useProductionsByStatus('SETTLED', 0, 500);
  const records = pageData?.content ?? [];
  const [selected, setSelected] = useState<{ settlement: SettlementResponse; itemName: string } | null>(null);
  const [activeFilter, setActiveFilter] = useState<FilterType>('ALL');

  const withSettlement = records.filter((r) => r.settlement);

  const settled = withSettlement
    .filter((r) => activeFilter === 'ALL' || r.settlement?.result === activeFilter)
    .sort((a, b) => new Date(b.settlement!.settledAt).getTime() - new Date(a.settlement!.settledAt).getTime());

  const allSettled = withSettlement;
  const successCount = allSettled.filter((r) => r.settlement?.result === 'SUCCESS').length;
  const anomalyCount = allSettled.filter((r) => r.settlement?.result === 'ANOMALY').length;

  const failedCount = allSettled.filter((r) => r.settlement?.result === 'FAILED').length;

  const counts: Record<FilterType, number> = {
    ALL: allSettled.length,
    SUCCESS: successCount,
    ANOMALY: anomalyCount,
    FAILED: failedCount,
  };

  const groups = settled.reduce<Record<string, ProductionResponse[]>>((acc, r) => {
    const date = r.settlement!.settledAt.slice(0, 10);
    (acc[date] ??= []).push(r);
    return acc;
  }, {});

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-stone-900">결산 결과</h1>
      </div>

      {/* 필터 버튼 */}
      <div className="flex gap-2 mb-6">
        {FILTERS.map(({ value, label }) => {
          const isActive = activeFilter === value;
          return (
            <button
              key={value}
              onClick={() => setActiveFilter(value)}
              className={cn(
                'flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium transition-colors',
                isActive
                  ? value === 'ANOMALY'
                    ? 'bg-rose-600 text-white'
                    : value === 'SUCCESS'
                      ? 'bg-emerald-600 text-white'
                      : value === 'FAILED'
                        ? 'bg-stone-600 text-white'
                        : 'bg-stone-900 text-white'
                  : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              )}
            >
              {label}
              <span className={cn('text-xs', isActive ? 'opacity-70' : 'text-stone-400')}>
                ({counts[value]})
              </span>
            </button>
          );
        })}
      </div>

      {isLoading && <p className="text-stone-400 text-sm">로딩 중...</p>}

      {Object.keys(groups).length === 0 && !isLoading && (
        <p className="text-stone-400 text-sm">결산 기록이 없습니다.</p>
      )}

      <div className="space-y-6">
        {Object.entries(groups).map(([date, items]) => {
          const successCount = items.filter((r) => r.settlement?.result === 'SUCCESS').length;
          const anomalyCount = items.filter((r) => r.settlement?.result === 'ANOMALY').length;
          const failedCount  = items.filter((r) => r.settlement?.result === 'FAILED').length;

          return (
            <section key={date}>
              {/* 날짜 헤더 */}
              <div className="flex items-center gap-3 mb-2">
                <h2 className="text-sm font-semibold text-stone-500">{date}</h2>
                <div className="flex items-center gap-1.5">
                  {successCount > 0 && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-600">
                      <CheckCircle2 className="h-3 w-3" />
                      성공 {successCount}건
                    </span>
                  )}
                  {anomalyCount > 0 && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-medium text-rose-600">
                      <AlertTriangle className="h-3 w-3" />
                      확인필요 {anomalyCount}건
                    </span>
                  )}
                  {failedCount > 0 && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-stone-100 px-2.5 py-0.5 text-xs font-medium text-stone-500">
                      <XCircle className="h-3 w-3" />
                      오류 {failedCount}건
                    </span>
                  )}
                </div>
              </div>

              {/* 행 목록 */}
              <div className="rounded-lg border border-stone-200 overflow-hidden divide-y divide-stone-100">
                {items.map((r) => {
                  const isAnomaly = r.settlement?.result === 'ANOMALY';
                  const isFailed  = r.settlement?.result === 'FAILED';
                  return (
                    <div
                      key={r.id}
                      className={cn(
                        'flex items-center px-4 py-3 gap-4',
                        isAnomaly ? 'bg-rose-50/40' : isFailed ? 'bg-stone-50' : 'bg-white'
                      )}
                    >
                      {/* 결산 결과 아이콘 */}
                      <div className="shrink-0">
                        {isAnomaly ? (
                          <AlertTriangle className="h-4 w-4 text-rose-400" />
                        ) : isFailed ? (
                          <XCircle className="h-4 w-4 text-stone-400" />
                        ) : (
                          <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                        )}
                      </div>

                      {/* 품목명 + 수량 */}
                      <div className="flex-1 min-w-0">
                        <span className={cn(
                          'text-sm font-medium',
                          isAnomaly ? 'text-rose-800' : isFailed ? 'text-stone-500' : 'text-stone-800'
                        )}>
                          {r.itemName}
                        </span>
                        <span className="ml-2 text-xs text-stone-400">
                          {r.quantity.toLocaleString()}개
                        </span>
                      </div>

                      {/* 결산 시각 */}
                      <span className="text-xs text-stone-400 shrink-0">
                        {r.settlement?.settledAt?.slice(11, 16)}
                      </span>

                      {/* 상세 버튼 (ANOMALY만) */}
                      {isAnomaly && (
                        <button
                          onClick={() => setSelected({ settlement: r.settlement!, itemName: r.itemName })}
                          className="shrink-0 inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium text-rose-600 bg-rose-100 hover:bg-rose-200 transition-colors"
                        >
                          <Search className="h-3 w-3" />
                          상세
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            </section>
          );
        })}
      </div>

      {selected && (
        <AnomalyDetailModal
          open
          onClose={() => setSelected(null)}
          settlement={selected.settlement}
          itemName={selected.itemName}
        />
      )}
    </div>
  );
}
