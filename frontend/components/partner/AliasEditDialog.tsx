'use client';
import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useUpdateAlias } from '@/hooks/mutations/usePartnershipMutations';
import { toast } from 'sonner';

interface Props {
  open: boolean;
  onClose: () => void;
  partnershipId: number;
  currentAlias: string | null;
}

export function AliasEditDialog({ open, onClose, partnershipId, currentAlias }: Props) {
  const [alias, setAlias] = useState(currentAlias ?? '');
  const { mutateAsync, isPending } = useUpdateAlias();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await mutateAsync({ partnershipId, alias });
      toast.success('별명이 저장되었습니다.');
      onClose();
    } catch {
      toast.error('별명 저장에 실패했습니다.');
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>파트너 별명 설정</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="alias">별명</Label>
            <Input
              id="alias"
              value={alias}
              onChange={(e) => setAlias(e.target.value)}
              placeholder="표시할 별명 입력"
            />
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
