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

/**
 * 창고 비활성화 (소프트 삭제)
 * - 목록·비활성 창고 목록 양쪽을 무효화해야 즉시 반영된다
 */
export function useDeactivateWarehouse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => warehouseApi.deactivate(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: warehouseKeys.all() });
      toast.success('창고를 비활성화했습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '창고를 비활성화하지 못했습니다.')),
  });
}

/** 비활성 창고 활성화 */
export function useActivateWarehouse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => warehouseApi.activate(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: warehouseKeys.all() });
      toast.success('창고를 활성화했습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '창고를 활성화하지 못했습니다.')),
  });
}
