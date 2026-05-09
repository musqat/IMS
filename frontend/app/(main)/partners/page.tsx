'use client';
import { useState } from 'react';
import { useSubPartnerships, useMainPartnerships } from '@/hooks/queries/usePartnerships';
import { PartnerList } from '@/components/partner/PartnerList';
import { InviteDialog } from '@/components/partner/InviteDialog';
import { AcceptTokenDialog } from '@/components/partner/AcceptTokenDialog';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { UserPlus, Handshake } from 'lucide-react';

export default function PartnersPage() {
  const { data: subs = [], isLoading: subsLoading } = useSubPartnerships();
  const { data: mains = [], isLoading: mainsLoading } = useMainPartnerships();
  const isLoading = subsLoading || mainsLoading;
  const [inviteOpen, setInviteOpen] = useState(false);
  const [acceptOpen, setAcceptOpen] = useState(false);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-stone-900">파트너</h1>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setAcceptOpen(true)}>
            <Handshake className="h-4 w-4 mr-2" />
            초대 수락
          </Button>
          <Button onClick={() => setInviteOpen(true)}>
            <UserPlus className="h-4 w-4 mr-2" />
            하청 초대
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6">
        {isLoading ? (
          <>
            <div className="space-y-2">
              <Skeleton className="h-6 w-24 mb-3" />
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
            <div className="space-y-2">
              <Skeleton className="h-6 w-24 mb-3" />
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          </>
        ) : (
          <>
            <PartnerList title="내 하청사" partnerships={subs} />
            <PartnerList title="내 본사" partnerships={mains} />
          </>
        )}
      </div>

      <InviteDialog open={inviteOpen} onClose={() => setInviteOpen(false)} />
      <AcceptTokenDialog open={acceptOpen} onClose={() => setAcceptOpen(false)} />
    </div>
  );
}
