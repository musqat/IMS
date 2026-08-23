'use client';
import { useState } from 'react';
import {
  useSubPartnerships,
  useMainPartnerships,
  useReceivedInvites,
  useSentInvites,
} from '@/hooks/queries/usePartnerships';
import { PartnerList } from '@/components/partner/PartnerList';
import { InviteInbox } from '@/components/partner/InviteInbox';
import { InviteDialog } from '@/components/partner/InviteDialog';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { UserPlus } from 'lucide-react';

export default function PartnersPage() {
  const { data: subs = [], isLoading: subsLoading } = useSubPartnerships();
  const { data: mains = [], isLoading: mainsLoading } = useMainPartnerships();
  const { data: received = [] } = useReceivedInvites();
  const { data: sent = [] } = useSentInvites();
  const isLoading = subsLoading || mainsLoading;
  const [inviteOpen, setInviteOpen] = useState(false);

  const partners = [...subs, ...mains];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-stone-900">파트너</h1>
        {/* 토큰 붙여넣기 수락은 뺐다. 받은 초대가 아래에 뜨고 버튼으로 수락한다 */}
        <Button onClick={() => setInviteOpen(true)}>
          <UserPlus className="h-4 w-4 mr-2" />
          파트너 초대
        </Button>
      </div>

      {/* 받은 초대를 위에 둔다. 수락은 상대가 기다리는 동작이라 파트너 목록보다 급하다 */}
      <InviteInbox direction="received" invites={received} />
      <InviteInbox direction="sent" invites={sent} />

      {isLoading ? (
        <div>
          <Skeleton className="h-6 w-24 mb-3" />
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-16 w-full" />
            ))}
          </div>
        </div>
      ) : (
        <PartnerList title="내 파트너사" partnerships={partners} />
      )}

      <InviteDialog open={inviteOpen} onClose={() => setInviteOpen(false)} />
    </div>
  );
}
