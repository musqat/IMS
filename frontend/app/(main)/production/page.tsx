'use client';
import { useState } from 'react';
import { useProductionCounts, useProductionsByStatus } from '@/hooks/queries/useProductions';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';
import { ProductionTable } from '@/components/production/ProductionTable';
import { CreateProductionDialog } from '@/components/production/CreateProductionDialog';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { isQueryFailed } from '@/lib/utils/queryState';
import type { ProductionStatus } from '@/lib/types';
import { cn } from '@/lib/utils';

const STATUSES: { value: ProductionStatus; label: string }[] = [
  { value: 'PENDING', label: '진행중' },
  { value: 'SETTLED', label: '결산완료' },
  { value: 'CANCELLED', label: '취소됨' },
];

export default function ProductionPage() {
  const [createOpen, setCreateOpen] = useState(false);
  const [activeStatus, setActiveStatus] = useState<ProductionStatus>('PENDING');
  const [page, setPage] = useState(0);

  const { data: counts } = useProductionCounts();
  const productionsQuery = useProductionsByStatus(activeStatus, page);
  const pageData = productionsQuery.data;
  const isLoading = productionsQuery.isLoading;
  const isError = isQueryFailed(productionsQuery);

  const records = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;

  const countFor = (status: ProductionStatus) => {
    if (!counts) return null;
    return status === 'PENDING' ? counts.pending
      : status === 'SETTLED' ? counts.settled
      : counts.cancelled;
  };

  const handleTabChange = (status: ProductionStatus) => {
    setActiveStatus(status);
    setPage(0);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-stone-900">생산 기록</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          생산 등록
        </Button>
      </div>

      {/* 상태 선택 버튼 그룹 */}
      <div className="flex gap-2 mb-4">
        {STATUSES.map(({ value, label }) => {
          const count = countFor(value);
          const isActive = activeStatus === value;
          return (
            <button
              key={value}
              onClick={() => handleTabChange(value)}
              className={cn(
                'flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-stone-900 text-white'
                  : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              )}
            >
              {label}
              {count !== null && (
                <span className={cn('text-xs', isActive ? 'text-stone-300' : 'text-stone-400')}>
                  ({count})
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* 테이블 */}
      {isLoading ? (
        <div className="space-y-2 mt-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full" />
          ))}
        </div>
      ) : isError ? (
        // 실패를 빈 배열로 흘리면 "생산 기록이 없습니다"로 보인다
        <ErrorState message="생산 기록을 불러오지 못했습니다." onRetry={productionsQuery.refetch} />
      ) : (
        <ProductionTable records={records} />
      )}

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-3 mt-6">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            이전
          </Button>
          <span className="text-sm text-stone-500">
            {page + 1} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </Button>
        </div>
      )}

      <CreateProductionDialog open={createOpen} onClose={() => setCreateOpen(false)} />
    </div>
  );
}
