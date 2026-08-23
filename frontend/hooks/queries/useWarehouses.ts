import { useQuery } from '@tanstack/react-query';
import { warehouseApi } from '@/lib/api/warehouse';
import { isQueryFailed } from '@/lib/utils/queryState';
import type { AccessibleWarehouse } from '@/lib/types';

export const warehouseKeys = {
  all: () => ['warehouses'] as const,
  lists: () => [...warehouseKeys.all(), 'list'] as const,
  detail: (id: number) => [...warehouseKeys.all(), 'detail', id] as const,
  shared: () => [...warehouseKeys.all(), 'shared'] as const,
  inactive: () => [...warehouseKeys.all(), 'inactive'] as const,
};

export function useWarehouses() {
  return useQuery({ queryKey: warehouseKeys.lists(), queryFn: warehouseApi.getList });
}

export function useWarehouse(id: number) {
  return useQuery({
    queryKey: warehouseKeys.detail(id),
    queryFn: () => warehouseApi.getOne(id),
    enabled: !!id,
  });
}

export function useSharedWarehouses() {
  return useQuery({ queryKey: warehouseKeys.shared(), queryFn: warehouseApi.getShared });
}

/**
 * 드롭다운 등에서 쓰는 창고 표시명.
 * 공유받은 창고는 소유 회사를 함께 보여줘야 어느 회사 창고인지 구분된다.
 */
export function warehouseLabel(w: AccessibleWarehouse): string {
  return w.isShared ? `${w.name} (공유 · ${w.ownerCompanyName})` : w.name;
}

/**
 * 소유 창고 + 공유받은 창고를 한 목록으로 반환한다.
 * 조회·분석 화면은 두 종류를 구분할 이유가 없으므로 여기서 합친다.
 * 쓰기 화면은 permission을 보고 분기해야 한다.
 */
export function useAccessibleWarehouses() {
  const ownedQuery = useWarehouses();
  const sharedQuery = useSharedWarehouses();

  const owned: AccessibleWarehouse[] = (ownedQuery.data ?? []).map((w) => ({
    id: w.id,
    name: w.name,
    location: w.location,
    ownerId: w.ownerId,
    ownerCompanyName: w.ownerCompanyName,
    isShared: false,
    permission: 'FULL',
  }));

  const shared: AccessibleWarehouse[] = (sharedQuery.data ?? []).map((s) => ({
    id: s.warehouseId,
    name: s.warehouseName,
    location: s.warehouseLocation,
    ownerId: s.ownerId,
    ownerCompanyName: s.ownerCompanyName,
    isShared: true,
    permission: s.permission,
  }));

  return {
    data: [...owned, ...shared],
    owned,
    shared,
    isLoading: ownedQuery.isLoading || sharedQuery.isLoading,
    // 한쪽만 실패해도 목록이 조용히 짧아진다. 실패로 취급해 화면에 알린다
    isFailed: isQueryFailed(ownedQuery) || isQueryFailed(sharedQuery),
    refetch: () => {
      ownedQuery.refetch();
      sharedQuery.refetch();
    },
  };
}

/** 비활성 창고 목록 — 활성화 화면용 */
export function useInactiveWarehouses(enabled = false) {
  return useQuery({
    queryKey: warehouseKeys.inactive(),
    queryFn: warehouseApi.getInactive,
    enabled,
  });
}
