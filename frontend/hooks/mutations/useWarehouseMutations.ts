import { useMutation, useQueryClient } from '@tanstack/react-query';
import { warehouseApi } from '@/lib/api/warehouse';
import { warehouseKeys } from '../queries/useWarehouses';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';

export function useCreateWarehouse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ name, location }: { name: string; location: string }) =>
      warehouseApi.create(name, location),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: warehouseKeys.lists() });
      toast.success('창고가 생성되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '창고 생성에 실패했습니다.')),
  });
}

export function useDeleteWarehouse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => warehouseApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: warehouseKeys.lists() });
      toast.success('창고가 삭제되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '창고 삭제에 실패했습니다.')),
  });
}

export function useShareWarehouse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      warehouseId,
      companyCode,
      permission,
    }: {
      warehouseId: number;
      companyCode: string;
      permission: 'VIEW' | 'FULL';
    }) => warehouseApi.share(warehouseId, companyCode, permission),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: warehouseKeys.shared() });
      toast.success('창고가 공유되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '공유에 실패했습니다.')),
  });
}
