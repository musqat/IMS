import { useMutation, useQueryClient } from '@tanstack/react-query';
import { partnershipApi } from '@/lib/api/partnership';
import { partnershipKeys } from '@/hooks/queries/usePartnerships';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';

export function useInvitePartner() {
  return useMutation({
    mutationFn: (companyCode: string) => partnershipApi.invite(companyCode),
  });
}

export function useAcceptPartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (token: string) => partnershipApi.accept(token),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: partnershipKeys.mains() });
    },
  });
}

export function useUpdateAlias() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ partnershipId, alias }: { partnershipId: number; alias: string }) =>
      partnershipApi.updateAlias(partnershipId, alias),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: partnershipKeys.subs() });
      qc.invalidateQueries({ queryKey: partnershipKeys.mains() });
    },
  });
}

export function useRemovePartnership() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (partnershipId: number) => partnershipApi.remove(partnershipId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: partnershipKeys.subs() });
      qc.invalidateQueries({ queryKey: partnershipKeys.mains() });
      toast.success('파트너십이 해제되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '파트너십 해제에 실패했습니다.')),
  });
}
