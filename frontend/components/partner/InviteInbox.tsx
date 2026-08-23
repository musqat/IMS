'use client';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Building2, Check, X, Inbox } from 'lucide-react';
import type { PartnershipResponse } from '@/lib/types';
import { daysUntil } from '@/lib/utils/date';
import { useAcceptInvite, useCancelInvite } from '@/hooks/mutations/usePartnershipMutations';
import { useConfirm } from '@/components/common/ConfirmProvider';

type Direction = 'received' | 'sent';

interface Props {
  direction: Direction;
  invites: PartnershipResponse[];
}

/** 만료까지 남은 기간 뱃지. 만료 시각이 없는 초대는 표시하지 않는다 */
function ExpiryBadge({ expiresAt }: { expiresAt: string | null }) {
  const left = daysUntil(expiresAt);
  if (left === null) return null;

  if (left <= 0) return <Badge variant="destructive">만료됨</Badge>;
  if (left <= 2) return <Badge variant="destructive">{left}일 남음</Badge>;
  return <Badge variant="secondary">{left}일 남음</Badge>;
}

export function InviteInbox({ direction, invites }: Props) {
  const isReceived = direction === 'received';
  const { mutate: acceptInvite, isPending: isAccepting } = useAcceptInvite();
  const { mutate: cancelInvite, isPending: isCancelling } = useCancelInvite();
  const confirm = useConfirm();

  // 초대가 없으면 섹션 자체를 띄우지 않는다.
  if (invites.length === 0) return null;

  return (
    <div className="mb-6">
      <div className="flex items-center gap-2 mb-3">
        <Inbox className="h-4 w-4 text-stone-500" />
        <h2 className="text-lg font-semibold text-stone-800">
          {isReceived ? '받은 초대' : '보낸 초대'}
        </h2>
        <Badge variant="outline">{invites.length}</Badge>
      </div>

      <div className="space-y-2">
        {invites.map((p) => {
          const partnerName = isReceived ? p.mainCompanyName : p.subCompanyName;
          const expired = (daysUntil(p.inviteExpiresAt) ?? 1) <= 0;

          return (
            <Card key={p.id} className="border-stone-200 bg-stone-50">
              <CardContent className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Building2 className="h-5 w-5 text-stone-400" />
                  <div>
                    <p className="font-medium text-stone-900">{partnerName}</p>
                    <p className="text-xs text-stone-500">
                      {isReceived ? '초대를 보냈습니다' : '수락을 기다리는 중'}
                    </p>
                  </div>
                  <ExpiryBadge expiresAt={p.inviteExpiresAt} />
                </div>

                {isReceived ? (
                  <Button
                    size="sm"
                    // 만료된 초대는 눌러도 400이 난다. 누르기 전에 막는다
                    disabled={isAccepting || expired}
                    onClick={() => acceptInvite(p.id)}
                  >
                    <Check className="h-4 w-4 mr-1" />
                    수락
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    variant="ghost"
                    className="text-rose-500 hover:text-rose-600 hover:bg-rose-50"
                    disabled={isCancelling}
                    onClick={async () => {
                      const ok = await confirm({
                        title: `${partnerName}에게 보낸 초대를 취소하시겠습니까?`,
                        description: '취소하면 상대가 더 이상 수락할 수 없습니다.',
                        confirmLabel: '초대 취소',
                        destructive: true,
                      });
                      if (ok) cancelInvite(p.id);
                    }}
                  >
                    <X className="h-4 w-4 mr-1" />
                    취소
                  </Button>
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
