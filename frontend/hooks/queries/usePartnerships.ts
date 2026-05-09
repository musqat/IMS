import { useQuery } from '@tanstack/react-query';
import { partnershipApi } from '@/lib/api/partnership';

export const partnershipKeys = {
  subs: () => ['partnerships', 'subs'] as const,
  mains: () => ['partnerships', 'mains'] as const,
};

export function useSubPartnerships() {
  return useQuery({ queryKey: partnershipKeys.subs(), queryFn: partnershipApi.getSubs });
}

export function useMainPartnerships() {
  return useQuery({ queryKey: partnershipKeys.mains(), queryFn: partnershipApi.getMains });
}
