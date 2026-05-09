'use client';
import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useItems } from '@/hooks/queries/useItems';
import { useAddBom } from '@/hooks/mutations/useBomMutations';
import { cn } from '@/lib/utils';

interface Props {
  open: boolean;
  onClose: () => void;
  parentItemId: number;
}

export function AddChildDialog({ open, onClose, parentItemId }: Props) {
  const { data: items = [] } = useItems();
  const { mutateAsync, isPending } = useAddBom(parentItemId);
  const [childItemId, setChildItemId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);

  const candidates = items.filter((i) => i.id !== parentItemId);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!childItemId) return;
    try {
      await mutateAsync({ childItemId, quantity });
      setChildItemId(null);
      setQuantity(1);
      onClose();
    } catch {}
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader><DialogTitle>하위 부품 추가</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label>하위 품목</Label>
            <select
              className={cn(
                'h-9 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm',
                'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50'
              )}
              value={childItemId?.toString() ?? ''}
              onChange={(e) => setChildItemId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="" disabled>품목 선택</option>
              {candidates.map((item) => (
                <option key={item.id} value={item.id.toString()}>
                  [{item.itemCode}] {item.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="quantity">필요 수량</Label>
            <Input
              id="quantity"
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value))}
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending || !childItemId}>추가</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
