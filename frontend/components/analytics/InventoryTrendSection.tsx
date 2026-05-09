'use client';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { useWarehouses } from '@/hooks/queries/useWarehouses';
import { inventoryApi } from '@/lib/api/inventory';
import { inventoryKeys } from '@/hooks/queries/useInventories';
import { InventoryTrendChart } from './InventoryTrendChart';

const SELECT_CN =
  'flex-1 h-9 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm ' +
  'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50';

export function InventoryTrendSection() {
  const [warehouseId, setWarehouseId] = useState<number | null>(null);
  const [itemId, setItemId] = useState<number | null>(null);

  const { data: warehouses = [] } = useWarehouses();

  const { data: inventoryPage } = useQuery({
    queryKey: inventoryKeys.list(warehouseId ?? 0),
    queryFn: () => inventoryApi.getList(warehouseId!, undefined, 0, 100),
    enabled: !!warehouseId,
  });
  const inventories = inventoryPage?.content ?? [];

  const { data: historyPage } = useQuery({
    queryKey: inventoryKeys.history(warehouseId ?? 0, itemId ?? 0),
    queryFn: () => inventoryApi.getHistory(warehouseId!, itemId!, 0, 200),
    enabled: !!warehouseId && !!itemId,
  });
  const history = historyPage?.content ?? [];

  const selectedInventory = inventories.find((inv) => inv.itemId === itemId);
  // 이력 합산을 역산하여 초기 재고량 추정 (차트 시작점)
  const initialQuantity = selectedInventory
    ? selectedInventory.quantity - history.reduce((sum, h) => sum + h.delta, 0)
    : 0;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">재고 추이</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex gap-2">
          <select
            className={cn(SELECT_CN)}
            value={warehouseId?.toString() ?? ''}
            onChange={(e) => {
              setWarehouseId(e.target.value ? Number(e.target.value) : null);
              setItemId(null);
            }}
          >
            <option value="" disabled>창고 선택</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id.toString()}>{w.name}</option>
            ))}
          </select>

          <select
            className={cn(SELECT_CN, !warehouseId && 'cursor-not-allowed opacity-50')}
            value={itemId?.toString() ?? ''}
            onChange={(e) => setItemId(e.target.value ? Number(e.target.value) : null)}
            disabled={!warehouseId}
          >
            <option value="" disabled>품목 선택</option>
            {inventories.map((inv) => (
              <option key={inv.itemId} value={inv.itemId.toString()}>{inv.itemName}</option>
            ))}
          </select>
        </div>

        <InventoryTrendChart
          history={history}
          safetyStock={selectedInventory?.safetyStock ?? 0}
          initialQuantity={initialQuantity}
        />
      </CardContent>
    </Card>
  );
}
