import { useQuery } from '@tanstack/react-query';
import { warehouseApi } from '@/lib/api/warehouse';

export const warehouseKeys = {
  all: () => ['warehouses'] as const,
  lists: () => [...warehouseKeys.all(), 'list'] as const,
  detail: (id: number) => [...warehouseKeys.all(), 'detail', id] as const,
  shared: () => [...warehouseKeys.all(), 'shared'] as const,
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
