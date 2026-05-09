'use client';
import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { ArrowDownToLine, ArrowUpFromLine, SlidersHorizontal, History } from 'lucide-react';
import { InventoryActionDialog } from './InventoryActionDialog';
import type { ActionMode } from './InventoryActionDialog';
import { HistoryDrawer } from './HistoryDrawer';
import type { InventoryResponse } from '@/lib/types';

type DialogType = ActionMode | 'history' | null;

interface Props {
  warehouseId: number;
  items: InventoryResponse[];
  viewOnly?: boolean;
}

export function InventoryTable({ warehouseId, items, viewOnly = false }: Props) {
  const [selected, setSelected] = useState<InventoryResponse | null>(null);
  const [dialog, setDialog] = useState<DialogType>(null);

  const open = (inv: InventoryResponse, type: DialogType) => {
    setSelected(inv);
    setDialog(type);
  };
  const close = () => { setSelected(null); setDialog(null); };

  return (
    <>
      <div className="rounded-lg border border-stone-200 overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="bg-stone-50">
              <TableHead>품목코드</TableHead>
              <TableHead>품목명</TableHead>
              <TableHead className="text-right">현재재고</TableHead>
              <TableHead className="text-right">안전재고</TableHead>
              <TableHead className="text-center">상태</TableHead>
              <TableHead className="text-center">{viewOnly ? '이력' : '관리'}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {items.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-stone-400 py-10">
                  등록된 재고가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              items.map((inv) => (
                <TableRow key={inv.id} className="hover:bg-stone-50">
                  <TableCell className="font-mono text-sm">{inv.itemCode}</TableCell>
                  <TableCell>{inv.itemName}</TableCell>
                  <TableCell className={`text-right font-semibold ${inv.warning ? 'text-rose-600' : 'text-stone-800'}`}>
                    {inv.quantity.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right text-stone-500">{inv.safetyStock.toLocaleString()}</TableCell>
                  <TableCell className="text-center">
                    {inv.warning ? (
                      <Badge className="bg-rose-100 text-rose-600 border-rose-200 text-xs">재고부족</Badge>
                    ) : (
                      <Badge className="bg-emerald-100 text-emerald-700 border-emerald-200 text-xs">정상</Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-center gap-1.5">
                      {!viewOnly && (
                        <>
                          <button
                            onClick={() => open(inv, 'IN')}
                            className="inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium bg-emerald-50 text-emerald-700 hover:bg-emerald-100 transition-colors"
                          >
                            <ArrowDownToLine className="h-3 w-3" />입고
                          </button>
                          <button
                            onClick={() => open(inv, 'OUT')}
                            className="inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium bg-rose-50 text-rose-600 hover:bg-rose-100 transition-colors"
                          >
                            <ArrowUpFromLine className="h-3 w-3" />출고
                          </button>
                          <button
                            onClick={() => open(inv, 'ADJUST')}
                            className="inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium bg-amber-50 text-amber-600 hover:bg-amber-100 transition-colors"
                          >
                            <SlidersHorizontal className="h-3 w-3" />조정
                          </button>
                        </>
                      )}
                      <button
                        onClick={() => open(inv, 'history')}
                        className="inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium bg-stone-100 text-stone-500 hover:bg-stone-200 transition-colors"
                      >
                        <History className="h-3 w-3" />이력
                      </button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {selected && (dialog === 'IN' || dialog === 'OUT' || dialog === 'ADJUST') && (
        <InventoryActionDialog
          open
          onClose={close}
          mode={dialog}
          warehouseId={warehouseId}
          inventory={selected}
        />
      )}
      {selected && dialog === 'history' && (
        <HistoryDrawer
          open
          onClose={close}
          warehouseId={warehouseId}
          itemId={selected.itemId}
          itemName={selected.itemName}
        />
      )}
    </>
  );
}
