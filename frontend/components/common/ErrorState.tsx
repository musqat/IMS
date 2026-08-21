'use client';
import { AlertCircle, RotateCw } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface ErrorStateProps {
  /** 화면 맥락에 맞는 안내. 기본값은 일반적인 조회 실패 문구 */
  message?: string;
  /** React Query의 refetch를 넘기면 재시도 버튼이 표시된다 */
  onRetry?: () => void;
}

/**
 * 조회 실패 상태 표시
 *
 * 조회가 실패했을 때 빈 배열을 그대로 렌더하면 "데이터가 없습니다"로 보인다.
 * 서버가 죽은 것과 데이터가 원래 없는 것을 사용자가 구분할 수 없으므로 분리해서 표시한다.
 */
export function ErrorState({
  message = '데이터를 불러오지 못했습니다.',
  onRetry,
}: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-10 text-center">
      <AlertCircle className="h-6 w-6 text-rose-400" />
      <div>
        <p className="text-sm font-medium text-stone-700">{message}</p>
        <p className="text-xs text-stone-400 mt-1">
          잠시 후 다시 시도해 주세요.
        </p>
      </div>
      {onRetry && (
        <Button size="sm" variant="outline" onClick={onRetry}>
          <RotateCw className="h-3.5 w-3.5 mr-1.5" />
          다시 시도
        </Button>
      )}
    </div>
  );
}
