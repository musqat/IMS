'use client';
import { createContext, useCallback, useContext, useRef, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

export interface ConfirmOptions {
  title: string;
  description?: string | string[];
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
}

type ConfirmFn = (options: ConfirmOptions) => Promise<boolean>;

const ConfirmContext = createContext<ConfirmFn | null>(null);

/**
 * 확인 다이얼로그
 *
 * window.confirm을 대체한다.
 *
 * 브라우저 기본 창은 스타일을 맞출 수 없고 문구도 한 덩어리로만 보여준다.
 * 강제 결산·생산 취소처럼 되돌릴 수 없는 동작에는 경고 표시가 필요하다.
 */
export function ConfirmProvider({ children }: { children: React.ReactNode }) {
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  // 열려 있는 동안 resolve를 들고 있다가 버튼을 누를 때 호출한다
  const resolveRef = useRef<((value: boolean) => void) | null>(null);

  const confirm = useCallback<ConfirmFn>((opts) => {
    setOptions(opts);
    return new Promise<boolean>((resolve) => {
      resolveRef.current = resolve;
    });
  }, []);

  const close = (result: boolean) => {
    resolveRef.current?.(result);
    resolveRef.current = null;
    setOptions(null);
  };

  const lines = options?.description
    ? Array.isArray(options.description) ? options.description : [options.description]
    : [];

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Dialog
        open={options !== null}
        // ESC나 바깥 클릭으로 닫으면 취소로 처리한다. resolve를 빠뜨리면 await가 영영 안 풀린다
        onOpenChange={(open) => !open && close(false)}
      >
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>{options?.title}</DialogTitle>
            {/* 기본 렌더가 <p>라 그 안에 <p>를 또 넣으면 잘못된 HTML이 된다 */}
            {lines.length > 0 && (
              <DialogDescription render={<div className="space-y-1" />}>
                {lines.map((line, i) => (
                  <p key={i}>{line}</p>
                ))}
              </DialogDescription>
            )}
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => close(false)}>
              {options?.cancelLabel ?? '취소'}
            </Button>
            <Button
              variant={options?.destructive ? 'destructive' : 'default'}
              onClick={() => close(true)}
            >
              {options?.confirmLabel ?? '확인'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </ConfirmContext.Provider>
  );
}

/**
 * 확인 다이얼로그를 띄우고 결과를 기다린다.
 *
 * const confirm = useConfirm();
 * if (await confirm({ title: '삭제하시겠습니까?', destructive: true })) { ... }
 */
export function useConfirm(): ConfirmFn {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error('useConfirm은 ConfirmProvider 안에서만 쓸 수 있습니다.');
  return ctx;
}
