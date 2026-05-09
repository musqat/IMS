'use client';
import { useState } from 'react';
import { useBoms } from '@/hooks/queries/useItems';
import { BomNode } from './BomNode';
import { AddChildDialog } from './AddChildDialog';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';

interface Props {
  rootItemId: number;
}

export function BomTreeView({ rootItemId }: Props) {
  const { data: boms = [], isLoading } = useBoms(rootItemId);
  const [addOpen, setAddOpen] = useState(false);

  if (isLoading) return <p className="text-stone-400 text-sm">로딩 중...</p>;

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-stone-700">부품 구조</h3>
        <Button size="sm" variant="outline" onClick={() => setAddOpen(true)}>
          <Plus className="h-4 w-4 mr-1" />
          부품 추가
        </Button>
      </div>

      {boms.length === 0 ? (
        <p className="text-stone-400 text-sm py-4">등록된 부품 구조가 없습니다.</p>
      ) : (
        <div className="border border-stone-200 rounded-lg overflow-hidden">
          {boms.map((bom) => (
            <BomNode key={bom.id} bom={bom} depth={0} />
          ))}
        </div>
      )}

      {addOpen && (
        <AddChildDialog open onClose={() => setAddOpen(false)} parentItemId={rootItemId} />
      )}
    </div>
  );
}
