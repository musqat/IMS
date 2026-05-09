'use client';
import { useState } from 'react';
import { useWarehouses } from '@/hooks/queries/useWarehouses';
import { inventoryApi } from '@/lib/api/inventory';
import { Button } from '@/components/ui/button';
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  Minus,
  LayoutList,
  Layers,
  Download,
  Loader2,
} from 'lucide-react';
import { buildPivot, downloadXlsx } from '@/lib/utils/exportXlsx';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import type { InventoryHistoryType } from '@/lib/types';

function defaultFrom() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}
function defaultTo() {
  return new Date().toISOString().slice(0, 10);
}

const inputCls = cn(
  'h-9 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm',
  'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50',
);

export default function ExportPage() {
  const { data: warehouses = [] } = useWarehouses();
  const [warehouseId, setWarehouseId] = useState<number | null>(null);
  const [from, setFrom] = useState(defaultFrom());
  const [to, setTo] = useState(defaultTo());
  const [loadingKey, setLoadingKey] = useState<string | null>(null);

  const isLoading = loadingKey !== null;
  const disabled = !warehouseId || isLoading;

  const run = async (key: string, fn: () => Promise<void>) => {
    setLoadingKey(key);
    try { await fn(); } finally { setLoadingKey(null); }
  };

  // 출고/생산차감은 맥락상 명확하므로 양수 표시, 합산은 +/- 유지
  const ABSOLUTE_TYPES: InventoryHistoryType[] = ['OUT', 'PRODUCTION_DEDUCTION'];

  const handleExport = (types: InventoryHistoryType[], sheetName: string, filename: string) =>
    run(sheetName, async () => {
      if (!warehouseId) return;
      const rows = await inventoryApi.getWarehouseHistory(warehouseId, types, from, to);
      const absValue = types.length === 1 && ABSOLUTE_TYPES.includes(types[0]);
      downloadXlsx([buildPivot(rows, absValue)], [sheetName], `${filename}_${from}_${to}.xlsx`);
      toast.success(`${sheetName} 다운로드 완료`);
    });

  const handleExportAll = () =>
    run('전체통합', async () => {
      if (!warehouseId) return;
      // 전체 타입을 1회 조회 후 클라이언트에서 필터링 — API 4회 → 1회
      const allRows = await inventoryApi.getWarehouseHistory(
        warehouseId,
        ['IN', 'OUT', 'PRODUCTION_DEDUCTION', 'ADJUSTMENT'],
        from,
        to,
      );
      const inRows   = allRows.filter((r) => r.type === 'IN');
      const outRows  = allRows.filter((r) => r.type === 'OUT');
      const prodRows = allRows.filter((r) => r.type === 'PRODUCTION_DEDUCTION');
      const adjRows  = allRows.filter((r) => r.type === 'ADJUSTMENT');
      downloadXlsx(
        [buildPivot(inRows), buildPivot(outRows, true), buildPivot(prodRows, true), buildPivot(adjRows), buildPivot(allRows)],
        ['입고내역', '출고내역', '생산차감', '조정내역', '합산내역'],
        `전체통합_${from}_${to}.xlsx`,
      );
      toast.success('전체 통합 다운로드 완료');
    });

  const items = [
    {
      key: '입고내역',
      title: '입고 내역',
      desc: '날짜별 품목 입고 수량',
      icon: <ArrowDownToLine className="h-4 w-4" />,
      iconBg: 'bg-emerald-50 text-emerald-600',
      onClick: () => handleExport(['IN'], '입고내역', '입고내역'),
    },
    {
      key: '출고내역',
      title: '출고 내역',
      desc: '날짜별 품목 출고 수량',
      icon: <ArrowUpFromLine className="h-4 w-4" />,
      iconBg: 'bg-rose-50 text-rose-500',
      onClick: () => handleExport(['OUT'], '출고내역', '출고내역'),
    },
    {
      key: '생산차감',
      title: '생산차감 내역',
      desc: '날짜별 생산 결산 시 차감 수량',
      icon: <Minus className="h-4 w-4" />,
      iconBg: 'bg-amber-50 text-amber-500',
      onClick: () => handleExport(['PRODUCTION_DEDUCTION'], '생산차감', '생산차감내역'),
    },
    {
      key: '조정내역',
      title: '조정 내역',
      desc: '날짜별 재고 실사 보정 변화량 (+/-)',
      icon: <Minus className="h-4 w-4" />,
      iconBg: 'bg-sky-50 text-sky-500',
      onClick: () => handleExport(['ADJUSTMENT'], '조정내역', '조정내역'),
    },
    {
      key: '합산내역',
      title: '합산 내역',
      desc: '입고 / 출고 / 생산차감 / 조정 전체 순변화',
      icon: <LayoutList className="h-4 w-4" />,
      iconBg: 'bg-violet-50 text-violet-500',
      onClick: () => handleExport(['IN', 'OUT', 'PRODUCTION_DEDUCTION', 'ADJUSTMENT'], '합산내역', '합산내역'),
    },
  ];

  return (
    <div className="space-y-6 max-w-2xl">
      <h1 className="text-2xl font-bold text-stone-900">엑셀 Export</h1>

      {/* 필터 */}
      <div className="flex items-center gap-4 flex-wrap p-4 rounded-xl border border-stone-200 bg-white">
        <div className="flex items-center gap-2">
          <span className="text-sm text-stone-500 font-medium w-8">창고</span>
          <select
            className={cn(inputCls, 'w-48')}
            value={warehouseId?.toString() ?? ''}
            onChange={(e) => setWarehouseId(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="" disabled>창고를 선택하세요</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id.toString()}>{w.name}</option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-stone-500 font-medium w-8">기간</span>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className={inputCls} />
          <span className="text-stone-300">—</span>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className={inputCls} />
        </div>
      </div>

      {/* 개별 다운로드 */}
      <div className="rounded-xl border border-stone-200 bg-white overflow-hidden divide-y divide-stone-100">
        {items.map((item) => {
          const isThis = loadingKey === item.key;
          return (
            <div key={item.key} className="flex items-center gap-4 px-5 py-4 hover:bg-stone-50 transition-colors">
              <div className={cn('p-2 rounded-lg shrink-0', item.iconBg)}>
                {item.icon}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-stone-800">{item.title}</p>
                <p className="text-xs text-stone-400 mt-0.5">{item.desc}</p>
              </div>
              <Button
                size="sm"
                variant="outline"
                className="shrink-0 w-28"
                disabled={disabled}
                onClick={item.onClick}
              >
                {isThis ? (
                  <><Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />처리 중</>
                ) : (
                  <><Download className="h-3.5 w-3.5 mr-1.5" />다운로드</>
                )}
              </Button>
            </div>
          );
        })}
      </div>

      {/* 전체 통합 */}
      <div
        className={cn(
          'flex items-center gap-4 px-5 py-4 rounded-xl border transition-colors',
          disabled
            ? 'border-stone-200 bg-stone-50'
            : 'border-violet-200 bg-violet-50 hover:bg-violet-100 cursor-pointer',
        )}
        onClick={disabled ? undefined : handleExportAll}
      >
        <div className="p-2 rounded-lg bg-violet-100 text-violet-600 shrink-0">
          <Layers className="h-4 w-4" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-stone-800">전체 통합</p>
          <p className="text-xs text-stone-400 mt-0.5">입고 / 출고 / 생산차감 / 조정 / 합산 내역</p>
        </div>
        <Button
          size="sm"
          className="shrink-0 w-28 bg-violet-600 hover:bg-violet-700 text-white"
          disabled={disabled}
          onClick={(e) => { e.stopPropagation(); handleExportAll(); }}
        >
          {loadingKey === '전체통합' ? (
            <><Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />처리 중</>
          ) : (
            <><Download className="h-3.5 w-3.5 mr-1.5" />다운로드</>
          )}
        </Button>
      </div>
    </div>
  );
}
