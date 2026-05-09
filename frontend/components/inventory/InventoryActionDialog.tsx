'use client';
import { useEffect, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { useStockIn, useStockOut, useAdjust } from '@/hooks/mutations/useInventoryMutations';
import type { InventoryResponse } from '@/lib/types';

export type ActionMode = 'IN' | 'OUT' | 'ADJUST';

type FormData = { quantity: number; memo?: string };

const CONFIG = {
  IN:     { title: '입고',    label: '입고 수량', placeholder: '입고 사유',  minQty: 1,    btnLabel: '입고',    btnVariant: 'default'     },
  OUT:    { title: '출고',    label: '출고 수량', placeholder: '출고 사유',  minQty: 1,    btnLabel: '출고',    btnVariant: 'destructive' },
  ADJUST: { title: '재고 조정', label: '실사 수량 (새 재고량)', placeholder: '조정 사유', minQty: 0, btnLabel: '조정', btnVariant: 'default' },
} as const;

interface Props {
  open: boolean;
  onClose: () => void;
  mode: ActionMode;
  warehouseId: number;
  inventory: InventoryResponse;
}

export function InventoryActionDialog({ open, onClose, mode, warehouseId, inventory }: Props) {
  const stockIn  = useStockIn(warehouseId);
  const stockOut = useStockOut(warehouseId);
  const adjust   = useAdjust(warehouseId);

  const mutation = mode === 'IN' ? stockIn : mode === 'OUT' ? stockOut : adjust;
  const cfg = CONFIG[mode];

  const schema = useMemo(
    () => z.object({
      quantity: z.number().int().min(cfg.minQty, `${cfg.minQty} 이상 입력하세요`),
      memo: z.string().optional(),
    }),
    [cfg.minQty]
  );

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: mode === 'ADJUST' ? { quantity: inventory.quantity } : undefined,
  });

  useEffect(() => {
    if (mode === 'ADJUST') {
      reset({ quantity: inventory.quantity });
    } else {
      reset({ quantity: undefined });
    }
  }, [inventory.id, inventory.quantity, mode, reset]);

  const onSubmit = async (data: FormData) => {
    try {
      await mutation.mutateAsync({ itemId: inventory.itemId, quantity: data.quantity, memo: data.memo });
      reset();
      onClose();
    } catch {
      toast.error('처리 중 오류가 발생했습니다.');
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{cfg.title} — {inventory.itemName}</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-stone-500 -mt-2 px-6">
          현재 재고: <span className="font-semibold text-stone-800">{inventory.quantity.toLocaleString()}</span>
        </p>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 px-6 pb-2">
          <div className="space-y-1.5">
            <Label htmlFor="quantity">{cfg.label}</Label>
            <Input
              id="quantity"
              type="number"
              min={cfg.minQty}
              max={mode === 'OUT' ? inventory.quantity : undefined}
              {...register('quantity', { valueAsNumber: true })}
            />
            {errors.quantity && <p className="text-rose-600 text-xs">{errors.quantity.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="memo">메모 (선택)</Label>
            <Input id="memo" {...register('memo')} placeholder={cfg.placeholder} />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" variant={cfg.btnVariant as 'default' | 'destructive'} disabled={mutation.isPending}>
              {cfg.btnLabel}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
