'use client';
import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { useShareWarehouse } from '@/hooks/mutations/useWarehouseMutations';
import { useSubPartnerships } from '@/hooks/queries/usePartnerships';
import { cn } from '@/lib/utils';

interface Props {
  open: boolean;
  onClose: () => void;
  warehouseId: number;
}

export function ShareDialog({ open, onClose, warehouseId }: Props) {
  const [companyCode, setCompanyCode] = useState('');
  const [permission, setPermission] = useState<'VIEW' | 'FULL'>('VIEW');
  const { mutateAsync, isPending } = useShareWarehouse();
  const { data: partners = [] } = useSubPartnerships();

  const acceptedPartners = partners.filter((p) => p.status === 'ACCEPTED');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await mutateAsync({ warehouseId, companyCode, permission });
      setCompanyCode('');
      onClose();
    } catch {}
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>창고 공유</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="partner">파트너 회사</Label>
            <select
              id="partner"
              className={cn(
                'h-9 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm',
                'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50'
              )}
              value={companyCode}
              onChange={(e) => setCompanyCode(e.target.value)}
            >
              <option value="" disabled>파트너를 선택하세요</option>
              {acceptedPartners.map((p) => (
                <option key={p.id} value={p.subCompanyCode}>
                  {p.alias ?? p.subCompanyName}
                </option>
              ))}
            </select>
            {acceptedPartners.length === 0 && (
              <p className="text-xs text-stone-400">수락된 파트너십이 없습니다.</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label>권한</Label>
            <div className="flex gap-2">
              {(['VIEW', 'FULL'] as const).map((p) => (
                <Button
                  key={p}
                  type="button"
                  size="sm"
                  variant={permission === p ? 'default' : 'outline'}
                  onClick={() => setPermission(p)}
                >
                  {p === 'VIEW' ? '조회' : '전체'}
                </Button>
              ))}
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending || !companyCode}>공유</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
