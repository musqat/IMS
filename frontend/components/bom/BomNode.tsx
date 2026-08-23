'use client';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Plus, Trash2, ChevronRight } from 'lucide-react';
import { useBoms } from '@/hooks/queries/useItems';
import { useDeleteBom } from '@/hooks/mutations/useBomMutations';
import { AddChildDialog } from './AddChildDialog';
import type { BomResponse } from '@/lib/types';
import { useConfirm } from '@/components/common/ConfirmProvider';

interface Props {
  bom: BomResponse;
  depth: number;
  maxDepth?: number;
}

export function BomNode({ bom, depth, maxDepth = 4 }: Props) {
  const [addOpen, setAddOpen] = useState(false);
  const { data: childBoms = [] } = useBoms(bom.childItemId);
  const { mutate: deleteBom } = useDeleteBom(bom.parentItemId);
  const confirm = useConfirm();

  return (
    <div>
      <div
        className="group flex items-center gap-2 py-2 px-3 rounded-lg hover:bg-stone-50 transition-colors"
        style={{ paddingLeft: `${depth * 20 + 12}px` }}
      >
        {depth > 0 && <ChevronRight className="h-3 w-3 text-stone-300 flex-shrink-0" />}
        <span className="font-mono text-xs text-stone-400 w-16 flex-shrink-0">{bom.childItemCode}</span>
        <span className="text-sm font-medium text-stone-800 flex-1">{bom.childItemName}</span>
        <span className="text-xs text-stone-500 bg-stone-100 px-2 py-0.5 rounded">×{bom.quantity}</span>
        <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          {depth < maxDepth && (
            <Button size="sm" variant="ghost" className="h-7 w-7 p-0" onClick={() => setAddOpen(true)}>
              <Plus className="h-3.5 w-3.5 text-violet-600" />
            </Button>
          )}
          <Button
            size="sm"
            variant="ghost"
            className="h-7 w-7 p-0"
            onClick={async () => {
              const ok = await confirm({
                title: `"${bom.childItemName}" BOM을 삭제하시겠습니까?`,
                description: '하위 구조에 영향을 줄 수 있습니다.',
                confirmLabel: '삭제',
                destructive: true,
              });
              if (ok) deleteBom(bom.id);
            }}
          >
            <Trash2 className="h-3.5 w-3.5 text-rose-500" />
          </Button>
        </div>
      </div>

      {childBoms.map((child) => (
        <BomNode key={child.id} bom={child} depth={depth + 1} maxDepth={maxDepth} />
      ))}

      {addOpen && (
        <AddChildDialog open onClose={() => setAddOpen(false)} parentItemId={bom.childItemId} />
      )}
    </div>
  );
}
