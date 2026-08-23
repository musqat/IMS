import { useQuery } from '@tanstack/react-query';
import { partnershipApi } from '@/lib/api/partnership';

export const partnershipKeys = {
  subs: () => ['partnerships', 'subs'] as const,
  mains: () => ['partnerships', 'mains'] as const,
  received: () => ['partnerships', 'invites', 'received'] as const,
  sent: () => ['partnerships', 'invites', 'sent'] as const,
};

/** 내가 받은 PENDING 초대 */
export function useReceivedInvites() {
  return useQuery({
    queryKey: partnershipKeys.received(),
    queryFn: partnershipApi.getReceivedInvites,
  });
}

/** 내가 보낸 PENDING 초대 */
export function useSentInvites() {
  return useQuery({
    queryKey: partnershipKeys.sent(),
    queryFn: partnershipApi.getSentInvites,
  });
}

export function useSubPartnerships() {
  return useQuery({ queryKey: partnershipKeys.subs(), queryFn: partnershipApi.getSubs });
}

export function useMainPartnerships() {
  return useQuery({ queryKey: partnershipKeys.mains(), queryFn: partnershipApi.getMains });
}
