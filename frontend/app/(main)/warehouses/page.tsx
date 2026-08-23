'use client';
import { useState } from 'react';
import { useWarehouses, useSharedWarehouses } from '@/hooks/queries/useWarehouses';
import { WarehouseCard } from '@/components/warehouse/WarehouseCard';
import { ShareDialog } from '@/components/warehouse/ShareDialog';
import { InactiveWarehouseSection } from '@/components/warehouse/InactiveWarehouseSection';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Plus } from 'lucide-react';
import { useCreateWarehouse } from '@/hooks/mutations/useWarehouseMutations';
import { Skeleton } from '@/components/ui/skeleton';

function CreateWarehouseDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const { mutateAsync, isPending } = useCreateWarehouse();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await mutateAsync({ name, location });
    setName(''); setLocation('');
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader><DialogTitle>창고 생성</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="name">창고명</Label>
            <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="location">위치</Label>
            <Input id="location" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="예: 서울시 강남구" />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending || !name}>생성</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function WarehousesPage() {
  const { data: owned = [], isLoading: ownedLoading } = useWarehouses();
  const { data: shared = [], isLoading: sharedLoading } = useSharedWarehouses();
  const isLoading = ownedLoading || sharedLoading;
  const [createOpen, setCreateOpen] = useState(false);
  const [shareTarget, setShareTarget] = useState<number | null>(null);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-stone-900">창고</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          창고 생성
        </Button>
      </div>

      <section className="mb-8">
        <h2 className="text-lg font-semibold text-stone-700 mb-3">내 창고</h2>
        {isLoading ? (
          <div className="grid grid-cols-3 gap-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-32 w-full" />
            ))}
          </div>
        ) : owned.length === 0 ? (
          <p className="text-stone-400 text-sm">창고가 없습니다.</p>
        ) : (
          <div className="grid grid-cols-3 gap-4">
            {owned.map((w) => (
              <WarehouseCard
                key={w.id}
                warehouse={w}
                isOwner={true}
                onShare={() => setShareTarget(w.id)}
              />
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-stone-700 mb-3">공유받은 창고</h2>
        {isLoading ? (
          <div className="grid grid-cols-3 gap-4">
            {Array.from({ length: 2 }).map((_, i) => (
              <Skeleton key={i} className="h-32 w-full" />
            ))}
          </div>
        ) : shared.length === 0 ? (
          <p className="text-stone-400 text-sm">공유받은 창고가 없습니다.</p>
        ) : (
          <div className="grid grid-cols-3 gap-4">
            {shared.map((s) => (
              <WarehouseCard
                key={s.id}
                // 백엔드가 비활성 창고를 공유 목록에서 이미 제외한다
                warehouse={{ id: s.warehouseId, name: s.warehouseName, location: s.warehouseLocation, ownerId: s.ownerId, ownerCompanyName: s.ownerCompanyName, active: true, createdAt: '' }}
                isOwner={false}
                permission={s.permission}
              />
            ))}
          </div>
        )}
      </section>

      <InactiveWarehouseSection />

      <CreateWarehouseDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      {shareTarget && (
        <ShareDialog open onClose={() => setShareTarget(null)} warehouseId={shareTarget} />
      )}
    </div>
  );
}
