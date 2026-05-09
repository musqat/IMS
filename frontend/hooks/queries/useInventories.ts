import { useQuery } from '@tanstack/react-query';
import { inventoryApi } from '@/lib/api/inventory';
import { INVENTORY_PAGE_SIZE } from '@/lib/constants';

export const inventoryKeys = {
  all: () => ['inventories'] as const,
  list: (warehouseId: number, keyword?: string) =>
    [...inventoryKeys.all(), warehouseId, keyword ?? ''] as const,
  history: (warehouseId: number, itemId: number) =>
    [...inventoryKeys.all(), warehouseId, itemId, 'history'] as const,
  maxProducible: (warehouseId: number, itemId: number) =>
    [...inventoryKeys.all(), warehouseId, itemId, 'max'] as const,
  shortage: (warehouseId: number) =>
    [...inventoryKeys.all(), warehouseId, 'shortage'] as const,
};

export function useInventories(warehouseId: number, keyword?: string) {
  return useQuery({
    queryKey: inventoryKeys.list(warehouseId, keyword),
    queryFn: () => inventoryApi.getList(warehouseId, keyword, 0, INVENTORY_PAGE_SIZE),
    enabled: !!warehouseId,
    select: (page) => page.content,
  });
}

export function useInventoryHistory(warehouseId: number, itemId: number) {
  return useQuery({
    queryKey: inventoryKeys.history(warehouseId, itemId),
    queryFn: () => inventoryApi.getHistory(warehouseId, itemId, 0, INVENTORY_PAGE_SIZE),
    enabled: itemId > 0,
    select: (page) => page.content,
  });
}

export function useMaxProducible(warehouseId: number, itemId: number, enabled = false) {
  return useQuery({
    queryKey: inventoryKeys.maxProducible(warehouseId, itemId),
    queryFn: () => inventoryApi.getMaxProducible(warehouseId, itemId),
    enabled: enabled && !!warehouseId && itemId > 0,
  });
}

export function useShortageAnalysis(warehouseId: number) {
  return useQuery({
    queryKey: inventoryKeys.shortage(warehouseId),
    queryFn: () => inventoryApi.getShortageAnalysis(warehouseId),
    enabled: !!warehouseId,
  });
}
