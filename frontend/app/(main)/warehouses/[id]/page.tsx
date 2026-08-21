'use client';
import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useWarehouse, useSharedWarehouses } from '@/hooks/queries/useWarehouses';
import { useInventories } from '@/hooks/queries/useInventories';
import { InventoryTable } from '@/components/inventory/InventoryTable';
import { AddInventoryDialog } from '@/components/inventory/AddInventoryDialog';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Search, MapPin, Plus } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { isQueryFailed } from '@/lib/utils/queryState';
import { cn } from '@/lib/utils';
import type { ItemType } from '@/lib/types';

const TYPE_LABELS: { type: ItemType; label: string }[] = [
  { type: 'PRODUCT', label: '완성품' },
  { type: 'SEMI',    label: '반제품' },
  { type: 'PART',    label: '부품' },
];

export default function WarehouseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const warehouseId = Number(id);
  const [keyword, setKeyword] = useState('');
  const [selectedTypes, setSelectedTypes] = useState<Set<ItemType>>(new Set());
  const [addOpen, setAddOpen] = useState(false);

  const { user } = useAuthStore();
  const { data: warehouse, isLoading: warehouseLoading } = useWarehouse(warehouseId);
  const { data: sharedWarehouses = [], isLoading: sharedLoading } = useSharedWarehouses();
  const inventoriesQuery = useInventories(warehouseId, keyword || undefined);
  const inventories = inventoriesQuery.data ?? [];
  const inventoriesLoading = inventoriesQuery.isLoading;
  const inventoriesFailed = isQueryFailed(inventoriesQuery);

  // 권한 판별에 필요한 데이터가 모두 로드될 때까지 viewOnly로 보호
  const permissionLoading = warehouseLoading || sharedLoading || !user;
  const isOwner = warehouse?.ownerId === user?.id;
  const sharedEntry = sharedWarehouses.find((s) => s.warehouseId === warehouseId);
  const isViewOnly = permissionLoading || (!isOwner && sharedEntry?.permission === 'VIEW');

  const belowSafety = inventories.filter((inv) => inv.warning).length;

  const toggleType = (type: ItemType) => {
    setSelectedTypes((prev) => {
      const next = new Set(prev);
      if (next.has(type)) { next.delete(type); } else { next.add(type); }
      return next;
    });
  };

  const filteredInventories =
    selectedTypes.size === 0
      ? inventories
      : inventories.filter((inv) => selectedTypes.has(inv.itemType));

  return (
    <div>
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-1">
          {warehouseLoading
            ? <Skeleton className="h-8 w-48" />
            : <h1 className="text-2xl font-bold text-stone-900">{warehouse?.name ?? '...'}</h1>
          }
          {belowSafety > 0 && (
            <Badge className="bg-rose-100 text-rose-600 border-rose-200">
              재고부족 {belowSafety}건
            </Badge>
          )}
        </div>
        {warehouse?.location && (
          <p className="flex items-center gap-1 text-sm text-stone-500">
            <MapPin className="h-4 w-4" />
            {warehouse.location}
          </p>
        )}
      </div>

      <div className="flex items-center gap-3 mb-4 flex-wrap">
        {/* 검색 */}
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-stone-400" />
          <Input
            className="pl-9"
            placeholder="품목명 또는 코드 검색"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </div>

        {/* 타입 필터 */}
        <div className="flex items-center gap-1">
          {TYPE_LABELS.map(({ type, label }) => (
            <button
              key={type}
              onClick={() => toggleType(type)}
              className={cn(
                'h-8 px-3 rounded-md text-xs font-semibold border transition-colors',
                selectedTypes.has(type)
                  ? 'bg-stone-800 text-white border-stone-800'
                  : 'bg-white text-stone-500 border-stone-200 hover:border-stone-400 hover:text-stone-700'
              )}
            >
              {label}
            </button>
          ))}
          {selectedTypes.size > 0 && (
            <button
              onClick={() => setSelectedTypes(new Set())}
              className="h-8 px-2 rounded-md text-xs text-stone-400 hover:text-stone-600 border border-stone-200 hover:border-stone-400 transition-colors ml-0.5"
            >
              초기화
            </button>
          )}
        </div>

        {!isViewOnly && (
          <Button size="sm" onClick={() => setAddOpen(true)}>
            <Plus className="h-4 w-4 mr-1" />
            품목 추가
          </Button>
        )}
      </div>

      {inventoriesLoading ? (
        <div className="space-y-2 mt-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : inventoriesFailed ? (
        // 실패를 빈 배열로 흘리면 "등록된 재고가 없습니다"로 보인다
        <ErrorState message="재고 목록을 불러오지 못했습니다." onRetry={inventoriesQuery.refetch} />
      ) : (
        <InventoryTable warehouseId={warehouseId} items={filteredInventories} viewOnly={isViewOnly} />
      )}

      <AddInventoryDialog
        open={addOpen}
        onClose={() => setAddOpen(false)}
        warehouseId={warehouseId}
      />
    </div>
  );
}
