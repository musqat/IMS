'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useAccessibleWarehouses, warehouseLabel } from '@/hooks/queries/useWarehouses';
import { useItems } from '@/hooks/queries/useItems';
import { inventoryApi } from '@/lib/api/inventory';
import { inventoryKeys } from '@/hooks/queries/useInventories';
import { ItemProductionTrendChart } from './ItemProductionTrendChart';
import { ItemOutboundTrendChart } from './ItemOutboundTrendChart';
import type { ProductionResponse } from '@/lib/types';

const MIN_DAYS = 7;
const SELECT_CN =
  'flex-1 h-9 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm ' +
  'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50';

interface Props {
  productions: ProductionResponse[];
  startDate: string;
  endDate: string;
  dayCount: number;
}

export function ProductionOutboundSection({ productions, startDate, endDate }: Props) {
  const [warehouseId, setWarehouseId] = useState<number | null>(null);
  const [itemId, setItemId] = useState<number | null>(null);

  const { data: warehouses } = useAccessibleWarehouses();
  const { data: items = [] } = useItems();
  const productItems = items.filter((i) => i.type === 'PRODUCT');

  const qc = useQueryClient();

  const { data: inventoryPage } = useQuery({
    queryKey: inventoryKeys.list(warehouseId ?? 0),
    queryFn: () => inventoryApi.getList(warehouseId!, undefined, 0, 100),
    enabled: !!warehouseId,
  });
  const inventories = inventoryPage?.content ?? [];

  const { data: historyPage } = useQuery({
    queryKey: inventoryKeys.history(warehouseId ?? 0, itemId ?? 0),
    queryFn: () => inventoryApi.getHistory(warehouseId!, itemId!, 0, 500),
    enabled: !!warehouseId && !!itemId,
  });
  const history = historyPage?.content ?? [];

  const { mutate: updateSafetyStock, isPending: isUpdating } = useMutation({
    mutationFn: ({ safetyStock }: { safetyStock: number }) =>
      inventoryApi.updateSafetyStock(warehouseId!, itemId!, safetyStock),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: inventoryKeys.list(warehouseId ?? 0) });
      toast.success('안전재고가 업데이트되었습니다.');
    },
    onError: () => toast.error('업데이트에 실패했습니다.'),
  });

  // 기간 내 OUT 이력 필터 및 안전재고 추천 계산
  const outRecords = history.filter((h) => {
    const d = h.createdAt.slice(0, 10);
    return h.type === 'OUT' && d >= startDate && d <= endDate;
  });
  const activeDays = new Set(outRecords.map((h) => h.createdAt.slice(0, 10))).size;
  const hasEnoughData = activeDays >= MIN_DAYS;
  const totalOut = outRecords.reduce((sum, h) => sum + Math.abs(h.delta), 0);
  const avgOutPerDay = hasEnoughData ? totalOut / activeDays : 0;
  const recommendedSafetyStock = Math.ceil(avgOutPerDay * 1.2);
  const currentSafetyStock = inventories.find((inv) => inv.itemId === itemId)?.safetyStock ?? null;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">제품별 생산 · 출고 추이</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* 창고 · 품목 선택 */}
        <div className="flex gap-2">
          <select
            className={cn(SELECT_CN)}
            value={warehouseId?.toString() ?? ''}
            onChange={(e) => {
              setWarehouseId(e.target.value ? Number(e.target.value) : null);
              setItemId(null);
            }}
          >
            <option value="" disabled>창고 선택</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id.toString()}>{warehouseLabel(w)}</option>
            ))}
          </select>
          <select
            className={cn(SELECT_CN, !warehouseId && 'cursor-not-allowed opacity-50')}
            value={itemId?.toString() ?? ''}
            onChange={(e) => setItemId(e.target.value ? Number(e.target.value) : null)}
            disabled={!warehouseId}
          >
            <option value="" disabled>완성품 선택</option>
            {productItems.map((i) => (
              <option key={i.id} value={i.id.toString()}>[{i.itemCode}] {i.name}</option>
            ))}
          </select>
        </div>

        {itemId ? (
          <>
            {/* 생산 · 출고 추이 차트 */}
            <div className="grid grid-cols-2 gap-6">
              <div>
                <ItemProductionTrendChart
                  records={productions}
                  itemId={itemId}
                  startDate={startDate}
                  endDate={endDate}
                />
              </div>
              <div>
                <ItemOutboundTrendChart
                  history={history}
                  startDate={startDate}
                  endDate={endDate}
                />
              </div>
            </div>

            {/* 안전재고 추천 */}
            {hasEnoughData ? (
              <div className="mt-4 flex items-center justify-between rounded-lg border border-violet-100 bg-violet-50 px-5 py-4">
                <div className="flex gap-8">
                  <div>
                    <p className="text-xs text-stone-500 mb-0.5">현재 안전재고</p>
                    <p className="text-lg font-bold text-stone-800">
                      {currentSafetyStock !== null ? `${currentSafetyStock}개` : '-'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-stone-500 mb-0.5">출하일 평균 출하량</p>
                    <p className="text-lg font-bold text-stone-800">{avgOutPerDay.toFixed(1)}개</p>
                  </div>
                  <div>
                    <p className="text-xs text-violet-600 mb-0.5 font-medium">
                      추천 안전재고{' '}
                      <span className="text-stone-400 font-normal">(평균 출하량 × 1.2)</span>
                    </p>
                    <p className="text-lg font-bold text-violet-700">{recommendedSafetyStock}개</p>
                  </div>
                </div>
                <Button
                  disabled={isUpdating}
                  onClick={() => updateSafetyStock({ safetyStock: recommendedSafetyStock })}
                >
                  안전재고 업데이트
                </Button>
              </div>
            ) : (
              <div className="mt-4 rounded-lg border border-stone-200 bg-stone-50 px-5 py-4 flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-stone-600">출고 데이터가 부족합니다</p>
                  <p className="text-xs text-stone-400 mt-0.5">
                    추천 안전재고는 출고 이력이{' '}
                    <span className="font-medium">{MIN_DAYS}일 이상</span> 있을 때 표시됩니다.
                    현재 {activeDays}일치 데이터가 있습니다.
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-stone-400 mb-0.5">현재 안전재고</p>
                  <p className="text-base font-bold text-stone-500">
                    {currentSafetyStock !== null ? `${currentSafetyStock}개` : '-'}
                  </p>
                </div>
              </div>
            )}
          </>
        ) : (
          <p className="text-stone-400 text-sm text-center py-10">창고와 완성품을 선택하세요.</p>
        )}
      </CardContent>
    </Card>
  );
}
