'use client';
import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Pencil, Building2, Unlink } from 'lucide-react';
import { AliasEditDialog } from './AliasEditDialog';
import type { PartnershipResponse } from '@/lib/types';
import { useAuthStore } from '@/store/authStore';
import { useRemovePartnership } from '@/hooks/mutations/usePartnershipMutations';

interface Props {
  title: string;
  partnerships: PartnershipResponse[];
}

export function PartnerList({ title, partnerships }: Props) {
  const { user } = useAuthStore();
  const [editing, setEditing] = useState<{ id: number; alias: string | null } | null>(null);
  const { mutate: removePartnership, isPending: isRemoving } = useRemovePartnership();

  return (
    <div>
      <h2 className="text-lg font-semibold text-stone-800 mb-3">{title}</h2>
      {partnerships.length === 0 ? (
        <p className="text-stone-500 text-sm">파트너가 없습니다.</p>
      ) : (
        <div className="space-y-3">
          {partnerships.map((p) => {
            const isMain = user?.id === p.mainId;
            const partnerName = isMain ? p.subCompanyName : p.mainCompanyName;
            const displayName = p.alias ?? partnerName;

            return (
              <Card key={p.id} className="border-stone-200">
                <CardContent className="p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Building2 className="h-5 w-5 text-stone-400" />
                    <div>
                      <p className="font-medium text-stone-900">{displayName}</p>
                      {p.alias && (
                        <p className="text-xs text-stone-400">{partnerName}</p>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setEditing({ id: p.id, alias: p.alias })}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="text-rose-500 hover:text-rose-600 hover:bg-rose-50"
                      disabled={isRemoving}
                      onClick={() => {
                        if (confirm(`${displayName}과의 파트너십을 해제하시겠습니까?`)) {
                          removePartnership(p.id);
                        }
                      }}
                    >
                      <Unlink className="h-4 w-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {editing && (
        <AliasEditDialog
          open
          onClose={() => setEditing(null)}
          partnershipId={editing.id}
          currentAlias={editing.alias}
        />
      )}
    </div>
  );
}
