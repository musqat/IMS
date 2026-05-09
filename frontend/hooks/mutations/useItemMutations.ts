import { useMutation, useQueryClient } from '@tanstack/react-query';
import { itemApi } from '@/lib/api/item';
import { itemKeys } from '../queries/useItems';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';
import type { ItemType } from '@/lib/types';

export function useCreateItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      itemCode, name, type, description,
    }: { itemCode: string; name: string; type: ItemType; description?: string }) =>
      itemApi.create(itemCode, name, type, description),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: itemKeys.lists() });
      toast.success('품목이 등록되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '품목 등록에 실패했습니다.')),
  });
}

export function useDeleteItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => itemApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: itemKeys.lists() });
      toast.success('품목이 삭제되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '품목 삭제에 실패했습니다.')),
  });
}
