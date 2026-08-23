'use client';
import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useInvitePartner } from '@/hooks/mutations/usePartnershipMutations';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';
import { Copy, Check } from 'lucide-react';

interface Props {
  open: boolean;
  onClose: () => void;
}

export function InviteDialog({ open, onClose }: Props) {
  const [companyCode, setCompanyCode] = useState('');
  const [token, setToken] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const { mutateAsync, isPending } = useInvitePartner();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const { inviteToken } = await mutateAsync(companyCode);
      setToken(inviteToken);
    } catch (error) {
      toast.error(getApiError(error, '초대 생성에 실패했습니다.'));
    }
  };

  const handleCopy = async () => {
    if (!token) return;
    await navigator.clipboard.writeText(token);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleClose = () => {
    setCompanyCode('');
    setToken(null);
    setCopied(false);
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>파트너 초대</DialogTitle>
        </DialogHeader>
        {!token ? (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="companyCode">파트너 회사 코드</Label>
              <Input
                id="companyCode"
                value={companyCode}
                onChange={(e) => setCompanyCode(e.target.value)}
                placeholder="회사 코드 입력"
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={handleClose}>취소</Button>
              <Button type="submit" disabled={isPending || !companyCode}>초대 생성</Button>
            </DialogFooter>
          </form>
        ) : (
          <div className="space-y-4">
            {/* 수신함이 생긴 뒤로 토큰 전달은 선택이다. 상대 화면에 이미 떠 있다 */}
            <p className="text-sm text-stone-600">
              초대를 보냈습니다. 상대 파트너 화면의 &lsquo;받은 초대&rsquo;에서 바로 수락할 수 있습니다.
            </p>
            <p className="text-xs text-amber-600">7일 후 만료됩니다. 만료되면 다시 초대하세요.</p>
            <p className="text-xs text-stone-500">아래 토큰으로도 수락할 수 있습니다.</p>
            <div className="flex items-center gap-2">
              <Input value={token} readOnly className="font-mono text-xs" />
              <Button type="button" size="icon" variant="outline" onClick={handleCopy}>
                {copied ? <Check className="h-4 w-4 text-emerald-600" /> : <Copy className="h-4 w-4" />}
              </Button>
            </div>
            <DialogFooter>
              <Button onClick={handleClose}>닫기</Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
