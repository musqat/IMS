'use client';
import { useState } from 'react';
import { useAccessibleWarehouses, warehouseLabel } from '@/hooks/queries/useWarehouses';
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
import { downloadXlsx } from '@/lib/utils/exportXlsx';
import { toLocalDateString, monthsAgo } from '@/lib/utils/date';
import { getApiError } from '@/lib/api/client';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import type { InventoryHistoryType } from '@/lib/types';

function defaultFrom() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}
function defaultTo() {
  return toLocalDateString(new Date());
}

const PRESETS = [
  { label: '최근 1개월', from: () => monthsAgo(1) },
  { label: '최근 3개월', from: () => monthsAgo(3) },
  { label: '최근 1년', from: () => monthsAgo(12) },
];

/**
 * 서버와 동일한 기간 규칙을 미리 검사한다.
 * 이력 조회는 메모리와 열 수 때문에 1년으로 제한된다 — 그보다 길면 연 단위로 나눠 받아야 한다.
 */
function rangeError(from: string, to: string): string | null {
  if (from > to) return '시작일이 종료일보다 클 수 없습니다.';
  const limit = new Date(from);
  limit.setFullYear(limit.getFullYear() + 1);
  if (toLocalDateString(limit) < to) return '조회 기간은 최대 1년입니다. 연 단위로 나눠서 받아주세요.';
  return null;
}

const inputCls = cn(
  'h-9 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm',
  'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50',
);

export default function ExportPage() {
  const { data: warehouses } = useAccessibleWarehouses();
  const [warehouseId, setWarehouseId] = useState<number | null>(null);
  const [from, setFrom] = useState(defaultFrom());
  const [to, setTo] = useState(defaultTo());
  const [loadingKey, setLoadingKey] = useState<string | null>(null);

  const isLoading = loadingKey !== null;
  const disabled = !warehouseId || isLoading;

  const run = async (key: string, fn: () => Promise<void>) => {
    const err = rangeError(from, to);
    if (err) {
      toast.error(err);
      return;
    }
    setLoadingKey(key);
    try {
      await fn();
    } catch (error) {
      // catch가 없으면 네트워크 실패 시 스피너만 꺼지고 아무 안내가 없다.
      toast.error(getApiError(error, '다운로드에 실패했습니다.'));
    } finally {
      setLoadingKey(null);
    }
  };

  const handleExport = (types: InventoryHistoryType[], sheetName: string, filename: string, absValue = false) =>
    run(sheetName, async () => {
      if (!warehouseId) return;
      const rows = await inventoryApi.getWarehouseHistory(warehouseId, types, from, to);
      await downloadXlsx([{ rows, name: sheetName, absValue }], `${filename}_${from}_${to}.xlsx`);
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
      const inOutRows = [...inRows, ...outRows];
      await downloadXlsx(
        [
          { rows: inRows,    name: '입고내역',   absValue: false },
          { rows: outRows,   name: '출고내역',   absValue: true  },
          { rows: prodRows,  name: '생산차감',   absValue: true  },
          { rows: inOutRows, name: '입출고합산', absValue: false },
        ],
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
      onClick: () => handleExport(['OUT'], '출고내역', '출고내역', true),
    },
    {
      key: '생산차감',
      title: '생산차감 내역',
      desc: '날짜별 생산 결산 시 차감 수량',
      icon: <Minus className="h-4 w-4" />,
      iconBg: 'bg-amber-50 text-amber-500',
      onClick: () => handleExport(['PRODUCTION_DEDUCTION'], '생산차감', '생산차감내역', true),
    },
    {
      key: '입출고합산',
      title: '입출고 합산',
      desc: '입고·출고 순변화 (+ 입고 / − 출고)',
      icon: <LayoutList className="h-4 w-4" />,
      iconBg: 'bg-violet-50 text-violet-500',
      onClick: () => handleExport(['IN', 'OUT'], '입출고합산', '입출고합산'),
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
              <option key={w.id} value={w.id.toString()}>{warehouseLabel(w)}</option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-stone-500 font-medium w-8">기간</span>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className={inputCls} />
          <span className="text-stone-300">—</span>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className={inputCls} />
          <span className="text-xs text-stone-400 ml-1">최대 1년</span>
        </div>

        {/* 기간 프리셋 */}
        <div className="flex items-center gap-1.5 w-full">
          {PRESETS.map((p) => (
            <button
              key={p.label}
              type="button"
              className="h-7 px-2.5 rounded-md border border-stone-200 text-xs text-stone-600 hover:bg-stone-50 transition-colors"
              onClick={() => { setFrom(p.from()); setTo(toLocalDateString(new Date())); }}
            >
              {p.label}
            </button>
          ))}
          <p className="text-xs text-stone-400 ml-auto">
            1년이 넘는 기간은 연 단위로 나눠서 받아주세요
          </p>
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
          <p className="text-xs text-stone-400 mt-0.5">입고 / 출고 / 생산차감 / 합산 내역</p>
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
