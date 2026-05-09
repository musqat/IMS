import { useQuery } from '@tanstack/react-query';
import { itemApi } from '@/lib/api/item';

export const itemKeys = {
  all: () => ['items'] as const,
  lists: () => [...itemKeys.all(), 'list'] as const,
  detail: (id: number) => [...itemKeys.all(), id] as const,
  boms: (id: number) => [...itemKeys.all(), id, 'bom'] as const,
};

export function useItems() {
  return useQuery({ queryKey: itemKeys.lists(), queryFn: itemApi.getList });
}

export function useItem(id: number) {
  return useQuery({
    queryKey: itemKeys.detail(id),
    queryFn: () => itemApi.getOne(id),
    enabled: !!id,
  });
}

export function useBoms(itemId: number) {
  return useQuery({
    queryKey: itemKeys.boms(itemId),
    queryFn: () => itemApi.getBoms(itemId),
    enabled: !!itemId,
  });
}
