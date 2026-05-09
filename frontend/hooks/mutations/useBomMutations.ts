import { useMutation, useQueryClient } from '@tanstack/react-query';
import { itemApi } from '@/lib/api/item';
import { itemKeys } from '../queries/useItems';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';

export function useAddBom(parentItemId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ childItemId, quantity }: { childItemId: number; quantity: number }) =>
      itemApi.addBom(parentItemId, childItemId, quantity),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: itemKeys.boms(parentItemId) });
      toast.success('부품이 추가되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '부품 추가에 실패했습니다.')),
  });
}

export function useDeleteBom(parentItemId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (bomId: number) => itemApi.deleteBom(parentItemId, bomId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: itemKeys.boms(parentItemId) });
      toast.success('부품이 삭제되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '부품 삭제에 실패했습니다.')),
  });
}
