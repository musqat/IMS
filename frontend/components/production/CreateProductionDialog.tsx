'use client';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useWarehouses } from '@/hooks/queries/useWarehouses';
import { useItems } from '@/hooks/queries/useItems';
import { useMaxProducible } from '@/hooks/queries/useInventories';
import { useCreateProduction } from '@/hooks/mutations/useProductionMutations';
import { cn } from '@/lib/utils';

const schema = z.object({
  quantity: z.number().int().min(1, '1 이상 입력하세요'),
});
type FormData = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onClose: () => void;
}

const selectClass = cn(
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm',
  'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50',
  'disabled:cursor-not-allowed disabled:opacity-50'
);

export function CreateProductionDialog({ open, onClose }: Props) {
  const { data: warehouses = [] } = useWarehouses();
  const { data: items = [] } = useItems();
  const [warehouseId, setWarehouseId] = useState<number | null>(null);
  const [itemId, setItemId] = useState<number | null>(null);

  const producibleItems = items.filter((i) => i.type === 'PRODUCT' || i.type === 'SEMI' || i.type === 'PART');
  const { data: maxProducible } = useMaxProducible(
    warehouseId ?? 0,
    itemId ?? 0,
    !!warehouseId && !!itemId
  );

  const { mutateAsync, isPending } = useCreateProduction(warehouseId ?? 0);
  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    if (!warehouseId || !itemId) return;
    try {
      await mutateAsync({ itemId, quantity: data.quantity });
      reset();
      setWarehouseId(null);
      setItemId(null);
      onClose();
    } catch {}
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader><DialogTitle>생산 기록 등록</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label>창고</Label>
            <select
              className={selectClass}
              value={warehouseId?.toString() ?? ''}
              onChange={(e) => { setWarehouseId(e.target.value ? Number(e.target.value) : null); setItemId(null); }}
            >
              <option value="" disabled>창고 선택</option>
              {warehouses.map((w) => (
                <option key={w.id} value={w.id.toString()}>{w.name}</option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <Label>생산 품목</Label>
            <select
              className={selectClass}
              value={itemId?.toString() ?? ''}
              onChange={(e) => setItemId(e.target.value ? Number(e.target.value) : null)}
              disabled={!warehouseId}
            >
              <option value="" disabled>품목 선택</option>
              {producibleItems.map((i) => (
                <option key={i.id} value={i.id.toString()}>[{i.itemCode}] {i.name} ({i.type === 'PRODUCT' ? '완성품' : i.type === 'SEMI' ? '반제품' : '부품'})</option>
              ))}
            </select>
            {maxProducible && maxProducible.maxQuantity !== null && (
              <p className="text-xs text-violet-600 font-medium">
                현재 최대 생산 가능: {maxProducible.maxQuantity}개
              </p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="quantity">수량</Label>
            <Input id="quantity" type="number" min={1} {...register('quantity', { valueAsNumber: true })} disabled={!itemId} />
            {errors.quantity && <p className="text-rose-600 text-xs">{errors.quantity.message}</p>}
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending || !warehouseId || !itemId}>등록</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
