'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { AlertTriangle } from 'lucide-react';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function Error({ error, reset }: ErrorProps) {
  useEffect(() => {
    console.error('[AppError]', error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-5 text-center">
      <div className="flex items-center justify-center w-14 h-14 rounded-full bg-rose-50">
        <AlertTriangle className="h-7 w-7 text-rose-500" />
      </div>
      <div className="space-y-1.5">
        <h2 className="text-lg font-semibold text-stone-800">문제가 발생했습니다</h2>
        <p className="text-sm text-stone-500 max-w-sm">
          페이지를 불러오는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.
        </p>
      </div>
      <Button variant="outline" onClick={reset} className="mt-1">
        다시 시도
      </Button>
    </div>
  );
}
