'use client';
import { useState } from 'react';
import { useShortageAnalysis, useMaxProducible } from '@/hooks/queries/useInventories';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { isQueryFailed } from '@/lib/utils/queryState';
import { Factory, Loader2 } from 'lucide-react';
import type { InventoryResponse } from '@/lib/types';

interface Props {
  warehouseId: number;
  /** 이 창고의 재고 목록. 완성품 선택지를 뽑는 데 쓴다 */
  inventories: InventoryResponse[];
}

/**
 * 창고의 생산 가능성 조회 패널 (읽기 전용).
 * - 생산 불가 완제품: 1개도 못 만드는 완성품과 부족 부품
 * - 최대 생산 가능 수량: 완성품을 골랐을 때 현재 재고 기준 생산 가능 수량
 *
 * 두 API 모두 창고 조회 권한(VIEW)만 있으면 되므로 공유받은 창고에서도 동작한다.
 * 품목·BOM은 백엔드가 창고 소유자 기준으로 조회한다.
 */
export function ProducibilityPanel({ warehouseId, inventories }: Props) {
  const [selectedItemId, setSelectedItemId] = useState<number | null>(null);

  const shortageQuery = useShortageAnalysis(warehouseId);
  const shortages = shortageQuery.data ?? [];
  const shortageFailed = isQueryFailed(shortageQuery);

  const maxQuery = useMaxProducible(warehouseId, selectedItemId ?? 0, !!selectedItemId);

  // 완성품·반제품만 생산 대상이다
  const producibleItems = inventories.filter(
    (inv) => inv.itemType === 'PRODUCT' || inv.itemType === 'SEMI'
  );

  return (
    <Card className="border-stone-200">
      <CardHeader className="flex flex-row items-center gap-2 pb-3">
        <Factory className="h-4 w-4 text-violet-500" />
        <CardTitle className="text-base">생산 가능성</CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        {/* 최대 생산 가능 수량 */}
        <div>
          <p className="text-xs font-semibold text-stone-500 mb-2">최대 생산 가능 수량</p>
          {producibleItems.length === 0 ? (
            <p className="text-sm text-stone-400">생산할 수 있는 품목이 없습니다.</p>
          ) : (
            <div className="flex items-center gap-3 flex-wrap">
              <select
                className="h-9 rounded-md border border-stone-200 bg-white px-3 text-sm text-stone-700 focus:border-stone-400 outline-none"
                value={selectedItemId ?? ''}
                onChange={(e) => setSelectedItemId(e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">품목 선택</option>
                {producibleItems.map((inv) => (
                  <option key={inv.itemId} value={inv.itemId}>
                    {inv.itemName}
                  </option>
                ))}
              </select>

              {selectedItemId !== null && (
                maxQuery.isLoading ? (
                  <Loader2 className="h-4 w-4 text-stone-300 animate-spin" />
                ) : isQueryFailed(maxQuery) ? (
                  <span className="text-sm text-rose-500">계산에 실패했습니다.</span>
                ) : maxQuery.data ? (
                  // maxQuantity가 null이면 BOM이 없다는 뜻이라 제한이 없다
                  maxQuery.data.maxQuantity === null ? (
                    <span className="text-sm text-stone-500">BOM이 없어 제한이 없습니다.</span>
                  ) : (
                    <span className="text-sm text-stone-700">
                      현재 재고로{' '}
                      <strong className="text-lg font-bold text-violet-600">
                        {maxQuery.data.maxQuantity}
                      </strong>
                      개 생산 가능
                    </span>
                  )
                ) : null
              )}
            </div>
          )}
        </div>

        {/* 생산 불가 완제품 */}
        <div>
          <p className="text-xs font-semibold text-stone-500 mb-2">생산 불가 완제품</p>
          {shortageQuery.isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 2 }).map((_, i) => (
                <Skeleton key={i} className="h-8 w-full" />
              ))}
            </div>
          ) : shortageFailed ? (
            // 실패를 빈 배열로 흘리면 "이상 없음"으로 보인다
            <ErrorState message="부족 분석을 불러오지 못했습니다." onRetry={shortageQuery.refetch} />
          ) : shortages.length === 0 ? (
            <p className="text-sm text-stone-400">모든 완제품을 생산할 수 있습니다.</p>
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
                        <span className="text-rose-400">
                          {s.currentStock}/{s.requiredPerUnit}개
                        </span>
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
