'use client';
import { useState } from 'react';
import { useInactiveWarehouses } from '@/hooks/queries/useWarehouses';
import { useActivateWarehouse } from '@/hooks/mutations/useWarehouseMutations';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Archive, ChevronDown, ChevronRight, MapPin, RotateCcw } from 'lucide-react';

/**
 * 비활성 창고 목록
 * - 평소에는 접혀 있다. 펼칠 때만 조회한다
 */
export function InactiveWarehouseSection() {
  const [open, setOpen] = useState(false);
  const { data: warehouses = [], isLoading } = useInactiveWarehouses(open);
  const { mutate: activate } = useActivateWarehouse();

  return (
    // 위 섹션(공유받은 창고)과 성격이 달라 구분선과 여백으로 떼어 놓는다
    <section className="mt-12 pt-6 border-t border-stone-200">
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 text-sm text-stone-500 hover:text-stone-700 transition-colors"
      >
        {open
          ? <ChevronDown className="h-3.5 w-3.5" />
          : <ChevronRight className="h-3.5 w-3.5" />}
        <Archive className="h-3.5 w-3.5" />
        비활성 창고
        {open && !isLoading && (
          <span className="text-xs text-stone-400">({warehouses.length})</span>
        )}
      </button>

      {open && (
        <div className="mt-3">
          {isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 2 }).map((_, i) => (
                <Skeleton key={i} className="h-14 w-full" />
              ))}
            </div>
          ) : warehouses.length === 0 ? (
            <p className="text-sm text-stone-400">비활성화된 창고가 없습니다.</p>
          ) : (
            <div className="rounded-lg border border-stone-200 divide-y divide-stone-100">
              {warehouses.map((w) => (
                <div key={w.id} className="flex items-center justify-between px-4 py-3 bg-stone-50/60">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-stone-600">{w.name}</p>
                    {w.location && (
                      <p className="flex items-center gap-1 text-xs text-stone-400 mt-0.5">
                        <MapPin className="h-3 w-3" />
                        {w.location}
                      </p>
                    )}
                  </div>
                  <Button size="sm" variant="outline" onClick={() => activate(w.id)}>
                    <RotateCcw className="h-3.5 w-3.5 mr-1.5" />
                    활성화
                  </Button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
