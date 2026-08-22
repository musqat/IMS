'use client';
import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { isQueryFailed } from '@/lib/utils/queryState';
import { useAccessibleWarehouses, warehouseLabel } from '@/hooks/queries/useWarehouses';
import { useDepletion } from '@/hooks/queries/useInventories';
import { cn } from '@/lib/utils';
import { TrendingDown } from 'lucide-react';
import type { StockDepletionRow } from '@/lib/types';

const SELECT_CN =
  'h-9 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm ' +
  'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50';

interface Props {
  startDate: string;
  endDate: string;
}

/** 잔여 개월에 따른 표시 색상. null(소진 없음)은 회색 */
function toneOf(monthsRemaining: number | null): string {
  if (monthsRemaining === null) return 'text-stone-400';
  if (monthsRemaining < 1) return 'text-rose-600';
  if (monthsRemaining < 3) return 'text-amber-600';
  return 'text-stone-700';
}

function DepletionRow({ row }: { row: StockDepletionRow }) {
  const belowSafety = row.currentStock <= row.safetyStock;

  return (
    <tr className="border-b border-stone-100 last:border-0">
      <td className="py-2.5 pr-3">
        <div className="flex items-center gap-2">
          <span className="text-sm text-stone-800">{row.itemName}</span>
          <span className="text-xs text-stone-400 font-mono">{row.itemCode}</span>
          {belowSafety && (
            <span className="inline-flex items-center rounded-full bg-rose-50 px-1.5 py-0.5 text-[11px] font-medium text-rose-600">
              안전재고 미달
            </span>
          )}
        </div>
      </td>
      <td className="py-2.5 px-3 text-right text-sm text-stone-700 tabular-nums">
        {row.currentStock.toLocaleString()}
      </td>
      <td className="py-2.5 px-3 text-right text-sm text-stone-500 tabular-nums">
        {row.monthlyAverage.toFixed(1)}
      </td>
      <td className={cn('py-2.5 pl-3 text-right text-sm font-semibold tabular-nums', toneOf(row.monthsRemaining))}>
        {/* null은 기간 내 소진이 없었다는 뜻. 0으로 보이면 "곧 소진"으로 읽힌다 */}
        {row.monthsRemaining === null ? '—' : `${row.monthsRemaining.toFixed(1)}개월`}
      </td>
    </tr>
  );
}

/**
 * 재고 소진 예측
 * - 기간 내 나간 양으로 월평균을 내고 현재 재고가 몇 달치인지 보여준다
 * - 급한 품목이 위로 오도록 백엔드가 정렬해서 준다
 */
export function StockDepletionSection({ startDate, endDate }: Props) {
  const [warehouseId, setWarehouseId] = useState<number | null>(null);

  const { data: warehouses, isFailed: warehousesFailed } = useAccessibleWarehouses();
  const depletionQuery = useDepletion(warehouseId ?? 0, startDate, endDate);

  const data = depletionQuery.data;
  const failed = isQueryFailed(depletionQuery) || warehousesFailed;

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-3 flex-wrap">
        <div className="flex items-center gap-2">
          <TrendingDown className="h-4 w-4 text-amber-500" />
          <CardTitle className="text-base">재고 소진 예측</CardTitle>
        </div>
        <select
          className={SELECT_CN}
          value={warehouseId?.toString() ?? ''}
          onChange={(e) => setWarehouseId(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="" disabled>창고 선택</option>
          {warehouses.map((w) => (
            <option key={w.id} value={w.id.toString()}>{warehouseLabel(w)}</option>
          ))}
        </select>
      </CardHeader>

      <CardContent>
        {failed ? (
          <ErrorState message="소진 예측을 불러오지 못했습니다." onRetry={depletionQuery.refetch} />
        ) : !warehouseId ? (
          <p className="text-sm text-stone-400 py-6 text-center">창고를 선택하세요.</p>
        ) : depletionQuery.isLoading ? (
          <div className="space-y-2 py-1">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-9 w-full" />
            ))}
          </div>
        ) : !data || data.rows.length === 0 ? (
          <p className="text-sm text-stone-400 py-6 text-center">등록된 재고가 없습니다.</p>
        ) : (
          <>
            <p className="text-xs text-stone-500 mb-3">
              최근 {data.months.toFixed(1)}개월 출고 기준 · 생산 차감 포함
            </p>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[420px]">
                <thead>
                  <tr className="border-b border-stone-200">
                    <th className="pb-2 pr-3 text-left text-xs font-semibold text-stone-500">품목</th>
                    <th className="pb-2 px-3 text-right text-xs font-semibold text-stone-500">현재재고</th>
                    <th className="pb-2 px-3 text-right text-xs font-semibold text-stone-500">월평균 소진</th>
                    <th className="pb-2 pl-3 text-right text-xs font-semibold text-stone-500">잔여</th>
                  </tr>
                </thead>
                <tbody>
                  {data.rows.map((row) => (
                    <DepletionRow key={row.itemId} row={row} />
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
