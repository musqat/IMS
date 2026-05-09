'use client';
import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAcceptPartner } from '@/hooks/mutations/usePartnershipMutations';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';

interface Props {
  open: boolean;
  onClose: () => void;
}

export function AcceptTokenDialog({ open, onClose }: Props) {
  const [token, setToken] = useState('');
  const { mutateAsync, isPending } = useAcceptPartner();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await mutateAsync(token);
      toast.success('파트너십이 수락되었습니다.');
      setToken('');
      onClose();
    } catch (error) {
      toast.error(getApiError(error, '수락에 실패했습니다.'));
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>초대 수락</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="token">초대 토큰</Label>
            <Input
              id="token"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="본사로부터 받은 초대 토큰"
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending || !token}>수락</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
