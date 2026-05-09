'use client';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { AlertTriangle } from 'lucide-react';
import type { SettlementResponse } from '@/lib/types';

interface AnomalyItem {
  required: number;
  stock: number;
}

interface Props {
  open: boolean;
  onClose: () => void;
  settlement: SettlementResponse;
  itemName?: string;
}

export function AnomalyDetailModal({ open, onClose, settlement, itemName }: Props) {
  let detail: Record<string, AnomalyItem> = {};
  try {
    if (settlement.anomalyDetail) {
      detail = JSON.parse(settlement.anomalyDetail);
    }
  } catch {
    // malformed JSON — leave empty
  }

  const rows = Object.entries(detail);

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-rose-500" />
            부품 부족 상세
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* 기본 정보 */}
          <div className="rounded-lg bg-stone-50 border border-stone-200 px-4 py-3 space-y-1">
            <div className="flex justify-between text-sm">
              <span className="text-stone-500">품목</span>
              <span className="font-medium text-stone-800">{itemName ?? '-'}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-stone-500">결산 시각</span>
              <span className="text-stone-600">
                {settlement.settledAt?.slice(0, 16).replace('T', ' ')}
              </span>
            </div>
            {settlement.memo && (
              <div className="flex justify-between text-sm">
                <span className="text-stone-500">메모</span>
                <span className="text-stone-600">{settlement.memo}</span>
              </div>
            )}
          </div>

          {/* 부족 부품 내역 */}
          {rows.length === 0 ? (
            <p className="text-stone-400 text-sm text-center py-4">상세 정보가 없습니다.</p>
          ) : (
            <div className="rounded-lg border border-rose-100 overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-rose-50 border-b border-rose-100">
                    <th className="py-2 px-3 text-left font-medium text-rose-400">품목코드</th>
                    <th className="py-2 px-3 text-right font-medium text-rose-400">필요</th>
                    <th className="py-2 px-3 text-right font-medium text-rose-400">재고</th>
                    <th className="py-2 px-3 text-right font-medium text-rose-600">부족</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map(([itemCode, info]) => (
                    <tr key={itemCode} className="border-b border-rose-50 last:border-0">
                      <td className="py-2.5 px-3 font-mono font-semibold text-stone-700">{itemCode}</td>
                      <td className="py-2.5 px-3 text-right text-stone-600">{info.required.toLocaleString()}</td>
                      <td className="py-2.5 px-3 text-right text-stone-600">{info.stock.toLocaleString()}</td>
                      <td className="py-2.5 px-3 text-right font-bold text-rose-600">
                        -{(info.required - info.stock).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
