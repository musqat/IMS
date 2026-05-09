'use client';
import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Zap, XCircle, Pencil } from 'lucide-react';
import { productionApi } from '@/lib/api/production';
import { productionKeys } from '@/hooks/queries/useProductions';
import { toast } from 'sonner';
import { getApiError } from '@/lib/api/client';
import { SettlementEditDialog } from './SettlementEditDialog';
import { ProductionEditDialog } from './ProductionEditDialog';
import type { ProductionResponse } from '@/lib/types';

const STATUS_LABEL: Record<string, string> = {
  PENDING:   '진행중',
  SETTLED:   '결산완료',
  CANCELLED: '취소됨',
};

const STATUS_STYLE: Record<string, string> = {
  PENDING:   'bg-amber-100 text-amber-700 border-amber-200',
  SETTLED:   'bg-emerald-100 text-emerald-700 border-emerald-200',
  CANCELLED: 'bg-stone-100 text-stone-500 border-stone-200',
};

const RESULT_LABEL: Record<string, string> = {
  SUCCESS: '성공',
  ANOMALY: '확인필요',
};

const RESULT_STYLE: Record<string, string> = {
  SUCCESS: 'bg-emerald-100 text-emerald-700 border-emerald-200',
  ANOMALY: 'bg-rose-100 text-rose-600 border-rose-200',
};

interface Props {
  records: ProductionResponse[];
}

export function ProductionTable({ records }: Props) {
  const qc = useQueryClient();
  const [editingRecord, setEditingRecord] = useState<ProductionResponse | null>(null);
  const [editingSettlement, setEditingSettlement] = useState<ProductionResponse | null>(null);

  const invalidate = (warehouseId: number) => {
    qc.invalidateQueries({ queryKey: productionKeys.counts() });
    qc.invalidateQueries({ queryKey: ['productions', 'byStatus'] });
    qc.invalidateQueries({ queryKey: productionKeys.byWarehouse(warehouseId) });
  };

  const { mutate: cancel } = useMutation({
    mutationFn: ({ warehouseId, recordId }: { warehouseId: number; recordId: number }) =>
      productionApi.cancel(warehouseId, recordId),
    onSuccess: (_, { warehouseId }) => {
      invalidate(warehouseId);
      toast.success('생산이 취소되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '취소에 실패했습니다.')),
  });

  const { mutate: forceSettle, isPending: isSettling } = useMutation({
    mutationFn: ({ warehouseId, recordId }: { warehouseId: number; recordId: number }) =>
      productionApi.forceSettle(warehouseId, recordId),
    onSuccess: (_, { warehouseId }) => {
      invalidate(warehouseId);
      toast.success('결산이 완료되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '결산에 실패했습니다.')),
  });

  return (
    <>
      <div className="rounded-lg border border-stone-200 overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="bg-stone-50">
              <TableHead>품목명</TableHead>
              <TableHead className="text-right">수량</TableHead>
              <TableHead className="text-center">상태</TableHead>
              <TableHead className="text-center">결산결과</TableHead>
              <TableHead>생성일</TableHead>
              <TableHead className="text-center">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {records.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-stone-400 py-10">
                  기록이 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              records.map((r) => (
                <TableRow key={r.id} className="hover:bg-stone-50">
                  <TableCell className="font-medium">{r.itemName}</TableCell>
                  <TableCell className="text-right">{r.quantity.toLocaleString()}</TableCell>
                  <TableCell className="text-center">
                    <Badge className={STATUS_STYLE[r.status]}>{STATUS_LABEL[r.status] ?? r.status}</Badge>
                  </TableCell>
                  <TableCell className="text-center">
                    {r.settlement ? (
                      <Badge className={RESULT_STYLE[r.settlement.result]}>
                        {RESULT_LABEL[r.settlement.result] ?? r.settlement.result}
                      </Badge>
                    ) : '-'}
                  </TableCell>
                  <TableCell className="text-sm text-stone-500">
                    {r.createdAt.slice(0, 10)}
                  </TableCell>
                  <TableCell className="text-center">
                    <div className="flex items-center justify-center gap-1.5">
                      {r.status === 'PENDING' && (
                        <>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-stone-600 hover:bg-stone-100"
                            onClick={() => setEditingRecord(r)}
                          >
                            <Pencil className="h-3.5 w-3.5" />
                            수정
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-amber-600 border-amber-200 hover:bg-amber-50 hover:text-amber-700"
                            disabled={isSettling}
                            onClick={() => {
                              if (confirm(`"${r.itemName}" 생산 기록을 지금 바로 결산하시겠습니까?`)) {
                                forceSettle({ warehouseId: r.warehouseId, recordId: r.id });
                              }
                            }}
                          >
                            <Zap className="h-3.5 w-3.5" />
                            결산
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-rose-500 border-rose-200 hover:bg-rose-50 hover:text-rose-600"
                            onClick={() => {
                              if (confirm(`"${r.itemName}" 생산 기록을 취소하시겠습니까?\n취소 후에는 되돌릴 수 없습니다.`)) {
                                cancel({ warehouseId: r.warehouseId, recordId: r.id });
                              }
                            }}
                          >
                            <XCircle className="h-3.5 w-3.5" />
                            취소
                          </Button>
                        </>
                      )}
                      {r.status === 'SETTLED' && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-stone-600 hover:bg-stone-100"
                          onClick={() => setEditingSettlement(r)}
                        >
                          <Pencil className="h-3.5 w-3.5" />
                          결산 수정
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {editingRecord && (
        <ProductionEditDialog
          open
          record={editingRecord}
          onClose={() => setEditingRecord(null)}
        />
      )}

      {editingSettlement && (
        <SettlementEditDialog
          open
          record={editingSettlement}
          onClose={() => setEditingSettlement(null)}
        />
      )}
    </>
  );
}
