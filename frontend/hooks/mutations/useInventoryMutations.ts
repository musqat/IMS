import { useMutation, useQueryClient } from '@tanstack/react-query';
import { inventoryApi } from '@/lib/api/inventory';
import { inventoryKeys } from '../queries/useInventories';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';

export function useStockIn(warehouseId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, quantity, memo }: { itemId: number; quantity: number; memo?: string }) =>
      inventoryApi.stockIn(warehouseId, itemId, quantity, memo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: inventoryKeys.list(warehouseId) });
      toast.success('입고 처리되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '입고 처리에 실패했습니다.')),
  });
}

export function useStockOut(warehouseId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, quantity, memo }: { itemId: number; quantity: number; memo?: string }) =>
      inventoryApi.stockOut(warehouseId, itemId, quantity, memo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: inventoryKeys.list(warehouseId) });
      toast.success('출고 처리되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '출고 처리에 실패했습니다.')),
  });
}

export function useAdjust(warehouseId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, quantity, memo }: { itemId: number; quantity: number; memo?: string }) =>
      inventoryApi.adjust(warehouseId, itemId, quantity, memo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: inventoryKeys.list(warehouseId) });
      toast.success('재고가 조정되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '재고 조정에 실패했습니다.')),
  });
}

export function useCreateInventory(warehouseId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      itemId,
      quantity,
      safetyStock,
    }: {
      itemId: number;
      quantity: number;
      safetyStock: number;
    }) => inventoryApi.create(warehouseId, itemId, quantity, safetyStock),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: inventoryKeys.list(warehouseId) });
      toast.success('재고가 등록되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '재고 등록에 실패했습니다.')),
  });
}
