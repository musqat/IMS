import { useMutation, useQueryClient } from '@tanstack/react-query';
import { partnershipApi } from '@/lib/api/partnership';
import { partnershipKeys } from '@/hooks/queries/usePartnerships';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';

export function useInvitePartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (companyCode: string) => partnershipApi.invite(companyCode),
    // 보낸 초대 목록에 바로 뜨도록 갱신한다
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: partnershipKeys.sent() });
    },
  });
}

/** 수신함에서 수락 */
export function useAcceptInvite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (partnershipId: number) => partnershipApi.acceptById(partnershipId),
    onSuccess: () => {
      // 수락하면 받은 초대에서 빠지고 파트너 목록에 들어간다
      qc.invalidateQueries({ queryKey: partnershipKeys.received() });
      qc.invalidateQueries({ queryKey: partnershipKeys.mains() });
      toast.success('초대를 수락했습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '초대 수락에 실패했습니다.')),
  });
}

/** 내가 보낸 PENDING 초대 취소 (발신자만 가능) */
export function useCancelInvite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (partnershipId: number) => partnershipApi.cancelInvite(partnershipId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: partnershipKeys.sent() });
      toast.success('초대를 취소했습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '초대 취소에 실패했습니다.')),
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
