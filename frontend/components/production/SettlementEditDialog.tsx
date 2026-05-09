'use client';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { productionApi } from '@/lib/api/production';
import { productionKeys } from '@/hooks/queries/useProductions';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import type { ProductionResponse, SettlementResult } from '@/lib/types';

const schema = z.object({
  result: z.enum(['SUCCESS', 'ANOMALY']),
  memo: z.string().max(500, '500자 이내로 입력하세요').optional().default(''),
});
type FormData = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onClose: () => void;
  record: ProductionResponse;
}

export function SettlementEditDialog({ open, onClose, record }: Props) {
  const qc = useQueryClient();

  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      result: (record.settlement?.result === 'FAILED' ? 'SUCCESS' : record.settlement?.result) ?? 'SUCCESS',
      memo: record.settlement?.memo ?? '',
    },
  });

  useEffect(() => {
    if (open) {
      reset({
        result: (record.settlement?.result === 'FAILED' ? 'SUCCESS' : record.settlement?.result) ?? 'SUCCESS',
        memo: record.settlement?.memo ?? '',
      });
    }
  }, [open, record.id, record.settlement?.result, record.settlement?.memo, reset]);

  const { mutateAsync, isPending } = useMutation({
    mutationFn: ({ result, memo }: { result: SettlementResult; memo: string }) =>
      productionApi.updateSettlement(record.warehouseId, record.id, result, memo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: productionKeys.counts() });
      qc.invalidateQueries({ queryKey: ['productions', 'byStatus'] });
      qc.invalidateQueries({ queryKey: productionKeys.byWarehouse(record.warehouseId) });
      toast.success('결산이 수정되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '결산 수정에 실패했습니다.')),
  });

  const onSubmit = async (data: FormData) => {
    try {
      await mutateAsync({ result: data.result, memo: data.memo ?? '' });
      onClose();
    } catch {}
  };

  const currentResult = watch('result');

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>결산 수정</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">

          {/* 결산 결과 선택 */}
          <div className="space-y-1.5">
            <Label>결산 결과</Label>
            <div className="flex gap-2">
              {(['SUCCESS', 'ANOMALY'] as const).map((v) => (
                <button
                  key={v}
                  type="button"
                  onClick={() => setValue('result', v)}
                  className={cn(
                    'flex-1 rounded-lg border px-3 py-2 text-sm font-medium transition-colors',
                    currentResult === v
                      ? v === 'SUCCESS'
                        ? 'border-emerald-500 bg-emerald-50 text-emerald-700'
                        : 'border-rose-400 bg-rose-50 text-rose-600'
                      : 'border-stone-200 bg-white text-stone-500 hover:bg-stone-50'
                  )}
                >
                  {v === 'SUCCESS' ? '성공' : '확인필요'}
                </button>
              ))}
            </div>
          </div>

          {/* 수정 메모 */}
          <div className="space-y-1.5">
            <Label htmlFor="memo">수정 메모</Label>
            <Textarea
              id="memo"
              rows={3}
              placeholder="수정 사유나 참고 내용을 입력하세요"
              {...register('memo')}
            />
            {errors.memo && <p className="text-rose-600 text-xs">{errors.memo.message}</p>}
          </div>

          {/* 기존 anomalyDetail 표시 */}
          {record.settlement?.anomalyDetail && (() => {
            let detail: Record<string, { required: number; stock: number }> = {};
            try { detail = JSON.parse(record.settlement.anomalyDetail); } catch {}
            const rows = Object.entries(detail);
            if (rows.length === 0) return null;
            return (
              <div className="space-y-1.5">
                <Label className="text-stone-500">부족 부품 내역</Label>
                <div className="rounded-lg border border-rose-100 bg-rose-50 overflow-hidden">
                  <table className="w-full text-xs">
                    <thead>
                      <tr className="border-b border-rose-100 text-rose-400">
                        <th className="py-1.5 px-3 text-left font-medium">품목코드</th>
                        <th className="py-1.5 px-3 text-right font-medium">필요</th>
                        <th className="py-1.5 px-3 text-right font-medium">재고</th>
                        <th className="py-1.5 px-3 text-right font-medium text-rose-600">부족</th>
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map(([code, info]) => (
                        <tr key={code} className="border-b border-rose-100 last:border-0">
                          <td className="py-1.5 px-3 font-mono font-semibold text-stone-700">{code}</td>
                          <td className="py-1.5 px-3 text-right text-stone-600">{info.required}</td>
                          <td className="py-1.5 px-3 text-right text-stone-600">{info.stock}</td>
                          <td className="py-1.5 px-3 text-right font-bold text-rose-600">-{info.required - info.stock}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            );
          })()}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending}>저장</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
