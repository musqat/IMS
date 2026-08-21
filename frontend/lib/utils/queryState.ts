import type { UseQueryResult } from '@tanstack/react-query';

/**
 * 조회가 실패했는지 판별한다.
 *
 * isError만 보면 부족하다. 서버에 아예 닿지 못하면(연결 거부·타임아웃)
 * - 5xx 응답      → isError
 * - 서버 도달 불가 → isPending + fetchStatus 'paused'
 */
export function isQueryFailed(query: Pick<
  UseQueryResult<unknown>,
  'isError' | 'isPending' | 'fetchStatus'
>): boolean {
  if (query.isError) return true;
  return query.isPending && query.fetchStatus === 'paused';
}
