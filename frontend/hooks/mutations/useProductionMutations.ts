import { useMutation, useQueryClient } from '@tanstack/react-query';
import { productionApi } from '@/lib/api/production';
import { productionKeys } from '../queries/useProductions';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';

/** 생산 기록 변경 후 counts + byStatus 전체 무효화 */
function invalidateProductions(qc: ReturnType<typeof useQueryClient>, warehouseId: number) {
  qc.invalidateQueries({ queryKey: productionKeys.counts() });
  qc.invalidateQueries({ queryKey: ['productions', 'byStatus'] });
  qc.invalidateQueries({ queryKey: productionKeys.byWarehouse(warehouseId) });
}

export function useCreateProduction(warehouseId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: number; quantity: number }) =>
      productionApi.create(warehouseId, itemId, quantity),
    onSuccess: () => {
      invalidateProductions(qc, warehouseId);
      toast.success('생산 기록이 등록되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '생산 기록 등록에 실패했습니다.')),
  });
}

export function useCancelProduction(warehouseId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (recordId: number) => productionApi.cancel(warehouseId, recordId),
    onSuccess: () => {
      invalidateProductions(qc, warehouseId);
      toast.success('생산이 취소되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '취소에 실패했습니다.')),
  });
}
