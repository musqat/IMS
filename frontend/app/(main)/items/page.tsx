'use client';
import { useState } from 'react';
import { useItems } from '@/hooks/queries/useItems';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';
import { ItemTable } from '@/components/item/ItemTable';
import { ItemFormDialog } from '@/components/item/ItemFormDialog';
import type { ItemType } from '@/lib/types';
import { cn } from '@/lib/utils';

const TYPES: { value: ItemType; label: string }[] = [
  { value: 'PRODUCT', label: '완성품' },
  { value: 'SEMI', label: '반제품' },
  { value: 'PART', label: '부품' },
];

export default function ItemsPage() {
  const { data: items = [] } = useItems();
  const [createOpen, setCreateOpen] = useState(false);
  const [activeType, setActiveType] = useState<ItemType>('PRODUCT');

  const filteredItems = items.filter((i) => i.type === activeType);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-stone-900">품목</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          품목 추가
        </Button>
      </div>

      {/* 유형 선택 버튼 그룹 */}
      <div className="flex gap-2 mb-4">
        {TYPES.map(({ value, label }) => {
          const count = items.filter((i) => i.type === value).length;
          const isActive = activeType === value;
          return (
            <button
              key={value}
              onClick={() => setActiveType(value)}
              className={cn(
                'flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-stone-900 text-white'
                  : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              )}
            >
              {label}
              <span className={cn('text-xs', isActive ? 'text-stone-300' : 'text-stone-400')}>
                ({count})
              </span>
            </button>
          );
        })}
      </div>

      {/* 테이블 */}
      <ItemTable items={filteredItems} />

      <ItemFormDialog open={createOpen} onClose={() => setCreateOpen(false)} defaultType={activeType} />
    </div>
  );
}
