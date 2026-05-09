'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useItems } from '@/hooks/queries/useItems';
import { useCreateInventory } from '@/hooks/mutations/useInventoryMutations';
import { cn } from '@/lib/utils';

const schema = z.object({
  itemId: z.coerce.number({ invalid_type_error: '품목을 선택하세요' }).int().positive('품목을 선택하세요'),
  quantity: z.coerce.number().int().min(0, '0 이상 입력하세요'),
  safetyStock: z.coerce.number().int().min(0, '0 이상 입력하세요'),
});
type FormData = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onClose: () => void;
  warehouseId: number;
}

export function AddInventoryDialog({ open, onClose, warehouseId }: Props) {
  const { data: items = [] } = useItems();
  const { mutateAsync, isPending } = useCreateInventory(warehouseId);

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    try {
      await mutateAsync({ itemId: data.itemId, quantity: data.quantity, safetyStock: data.safetyStock });
      reset();
      onClose();
    } catch {}
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>재고 품목 추가</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="itemId">품목</Label>
            <select
              id="itemId"
              className={cn(
                'h-9 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm',
                'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50'
              )}
              {...register('itemId')}
            >
              <option value="">품목을 선택하세요</option>
              {items.map((item) => (
                <option key={item.id} value={item.id}>
                  [{item.itemCode}] {item.name}
                </option>
              ))}
            </select>
            {errors.itemId && <p className="text-rose-600 text-xs">{errors.itemId.message}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="quantity">초기 재고</Label>
            <Input
              id="quantity"
              type="number"
              min={0}
              {...register('quantity')}
            />
            {errors.quantity && <p className="text-rose-600 text-xs">{errors.quantity.message}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="safetyStock">안전재고</Label>
            <Input
              id="safetyStock"
              type="number"
              min={0}
              {...register('safetyStock')}
            />
            {errors.safetyStock && <p className="text-rose-600 text-xs">{errors.safetyStock.message}</p>}
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending}>추가</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
