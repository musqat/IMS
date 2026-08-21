import { useQuery } from '@tanstack/react-query';
import { productionApi } from '@/lib/api/production';
import type { ProductionResponse, ProductionStatus } from '@/lib/types';

/** 서버 기본값(30)과 맞춘다. 훅에서 명시적으로 넘겨 키와 요청이 항상 일치하게 한다 */
const DEFAULT_PAGE_SIZE = 30;

/** 분석·결산 페이지처럼 전량이 필요한 경우 */
const LARGE_PAGE_SIZE = 500;

export const productionKeys = {
  counts: () => ['productions', 'counts'] as const,
  // size가 키에 없으면 같은 상태·페이지를 다른 크기로 조회하는 화면끼리 캐시를 공유한다.
  // 생산 탭(30건)과 결산 페이지(500건)가 서로의 결과를 덮어쓰는 문제가 있었다.
  byStatus: (status: ProductionStatus, page: number, size: number) =>
    ['productions', 'byStatus', status, page, size] as const,
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
export function useProductionsByStatus(status: ProductionStatus, page = 0, size = DEFAULT_PAGE_SIZE) {
  return useQuery({
    queryKey: productionKeys.byStatus(status, page, size),
    queryFn: () => productionApi.getByStatus(status, page, size),
  });
}

/**
 * 분석/결산 페이지용 — 전체 상태 레코드 통합
 * 3개 상태 병렬 fetch (항상 3회 고정, 창고 수 N+1 아님)
 * size=500 사용
 */
export function useAllProductions(): { data: ProductionResponse[]; isLoading: boolean } {
  const LARGE = LARGE_PAGE_SIZE;
  const pending = useQuery({ queryKey: productionKeys.byStatus('PENDING', 0, LARGE), queryFn: () => productionApi.getByStatus('PENDING', 0, LARGE) });
  const settled = useQuery({ queryKey: productionKeys.byStatus('SETTLED', 0, LARGE), queryFn: () => productionApi.getByStatus('SETTLED', 0, LARGE) });
  const cancelled = useQuery({ queryKey: productionKeys.byStatus('CANCELLED', 0, LARGE), queryFn: () => productionApi.getByStatus('CANCELLED', 0, LARGE) });

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
