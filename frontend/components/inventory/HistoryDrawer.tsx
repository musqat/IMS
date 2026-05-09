'use client';
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle } from '@/components/ui/drawer';
import { useInventoryHistory } from '@/hooks/queries/useInventories';
import type { InventoryHistoryType } from '@/lib/types';

const TYPE_COLOR: Record<InventoryHistoryType, string> = {
  IN: 'bg-emerald-100 text-emerald-700',
  OUT: 'bg-rose-100 text-rose-700',
  ADJUSTMENT: 'bg-amber-100 text-amber-700',
  PRODUCTION_DEDUCTION: 'bg-violet-100 text-violet-700',
};

const TYPE_LABEL: Record<InventoryHistoryType, string> = {
  IN: '입고',
  OUT: '출고',
  ADJUSTMENT: '조정',
  PRODUCTION_DEDUCTION: '생산차감',
};

interface Props {
  open: boolean;
  onClose: () => void;
  warehouseId: number;
  itemId: number;
  itemName: string;
}

export function HistoryDrawer({ open, onClose, warehouseId, itemId, itemName }: Props) {
  const { data: history = [] } = useInventoryHistory(warehouseId, itemId);

  return (
    <Drawer open={open} onOpenChange={(v) => !v && onClose()}>
      <DrawerContent>
        <DrawerHeader>
          <DrawerTitle>{itemName} 입출고 이력</DrawerTitle>
        </DrawerHeader>
        <div className="px-4 pb-6 space-y-2 max-h-[60vh] overflow-y-auto">
          {history.length === 0 ? (
            <p className="text-stone-400 text-sm text-center py-8">이력이 없습니다.</p>
          ) : (
            history.map((h) => (
              <div
                key={h.id}
                className="flex items-center justify-between py-2 border-b border-stone-100 last:border-0"
              >
                <div className="flex items-center gap-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${TYPE_COLOR[h.type]}`}>
                    {TYPE_LABEL[h.type]}
                  </span>
                  <span className="text-sm text-stone-600">{h.memo || '-'}</span>
                </div>
                <div className="flex items-center gap-4">
                  <span className={`text-sm font-semibold ${h.delta > 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                    {h.delta > 0 ? `+${h.delta}` : h.delta}
                  </span>
                  <span className="text-xs text-stone-400">
                    {h.createdAt.slice(0, 16).replace('T', ' ')}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </DrawerContent>
    </Drawer>
  );
}
