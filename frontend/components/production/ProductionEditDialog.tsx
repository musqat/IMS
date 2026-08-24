'use client';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { productionApi } from '@/lib/api/production';
import { productionKeys } from '@/hooks/queries/useProductions';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';
import type { ProductionResponse } from '@/lib/types';

const schema = z.object({
  quantity: z.coerce.number().int().min(1, '1 이상 입력하세요'),
});
type FormData = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onClose: () => void;
  record: ProductionResponse;
}

export function ProductionEditDialog({ open, onClose, record }: Props) {
  const qc = useQueryClient();

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    if (open) reset({ quantity: record.quantity });
  }, [open, record.id, record.quantity, reset]);

  const { mutateAsync, isPending } = useMutation({
    mutationFn: (quantity: number) => productionApi.update(record.warehouseId, record.id, quantity),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['productions', 'byStatus'] });
      qc.invalidateQueries({ queryKey: productionKeys.byWarehouse(record.warehouseId) });
      toast.success('생산 기록이 수정되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '수정에 실패했습니다.')),
  });

  const onSubmit = async (data: FormData) => {
    try {
      await mutateAsync(data.quantity);
      onClose();
    } catch {}
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-xs">
        <DialogHeader>
          <DialogTitle>생산 기록 수정</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label className="text-stone-500 text-xs">품목</Label>
            <p className="text-sm font-medium text-stone-800">{record.itemName}</p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="quantity">생산 수량</Label>
            <Input
              id="quantity"
              type="number"
              min={1}
              {...register('quantity')}
            />
            {errors.quantity && <p className="text-rose-600 text-xs">{errors.quantity.message}</p>}
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending}>저장</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
