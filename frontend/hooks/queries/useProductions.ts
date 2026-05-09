import { useQuery } from '@tanstack/react-query';
import { productionApi } from '@/lib/api/production';
import type { ProductionResponse, ProductionStatus } from '@/lib/types';

export const productionKeys = {
  counts: () => ['productions', 'counts'] as const,
  byStatus: (status: ProductionStatus, page: number) => ['productions', 'byStatus', status, page] as const,
  byWarehouse: (warehouseId: number) => ['productions', 'warehouse', warehouseId] as const,
};

/** 상태별 + ANOMALY 건수 — 대시보드 KPI / 탭 뱃지 */
export function useProductionCounts() {
  return useQuery({
    queryKey: productionKeys.counts(),
    queryFn: productionApi.getCounts,
  });
}

/** 상태 필터 + 서버 페이지네이션 — 생산 탭 테이블 */
export function useProductionsByStatus(status: ProductionStatus, page = 0, size?: number) {
  return useQuery({
    queryKey: productionKeys.byStatus(status, page),
    queryFn: () => productionApi.getByStatus(status, page, size),
  });
}

/**
 * 분석/결산 페이지용 — 전체 상태 레코드 통합
 * 3개 상태 병렬 fetch (항상 3회 고정, 창고 수 N+1 아님)
 * size=500 사용
 */
export function useAllProductions(): { data: ProductionResponse[]; isLoading: boolean } {
  const LARGE = 500;
  const pending = useQuery({ queryKey: productionKeys.byStatus('PENDING', 0), queryFn: () => productionApi.getByStatus('PENDING', 0, LARGE) });
  const settled = useQuery({ queryKey: productionKeys.byStatus('SETTLED', 0), queryFn: () => productionApi.getByStatus('SETTLED', 0, LARGE) });
  const cancelled = useQuery({ queryKey: productionKeys.byStatus('CANCELLED', 0), queryFn: () => productionApi.getByStatus('CANCELLED', 0, LARGE) });

  return {
    data: [
      ...(pending.data?.content ?? []),
      ...(settled.data?.content ?? []),
      ...(cancelled.data?.content ?? []),
    ],
    isLoading: pending.isLoading || settled.isLoading || cancelled.isLoading,
  };
}

export function useProductionsByWarehouse(warehouseId: number) {
  return useQuery({
    queryKey: productionKeys.byWarehouse(warehouseId),
    queryFn: () => productionApi.getByWarehouse(warehouseId),
    enabled: !!warehouseId,
  });
}
