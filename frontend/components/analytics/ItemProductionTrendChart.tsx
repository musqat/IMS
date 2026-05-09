'use client';
import { useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, ReferenceLine } from 'recharts';
import type { ProductionResponse } from '@/lib/types/index';

interface Props {
  records: ProductionResponse[];
  itemId: number;
  startDate: string;
  endDate: string;
}

export function ItemProductionTrendChart({ records, itemId, startDate, endDate }: Props) {
  const data = useMemo(() => {
    const byDate: Record<string, number> = {};
    records
      .filter((r) => {
        const d = r.createdAt.slice(0, 10);
        return r.itemId === itemId && r.status === 'SETTLED' && d >= startDate && d <= endDate;
      })
      .forEach((r) => {
        const date = r.createdAt.slice(0, 10);
        byDate[date] = (byDate[date] ?? 0) + r.quantity;
      });

    return Object.entries(byDate)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, quantity]) => ({ date, quantity }));
  }, [records, itemId, startDate, endDate]);

  const avg = data.length > 0
    ? data.reduce((s, d) => s + d.quantity, 0) / data.length
    : 0;

  if (data.length === 0) {
    return <p className="text-stone-400 text-sm text-center py-10">생산 데이터가 없습니다.</p>;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <p className="text-sm font-medium text-stone-600">생산 추이</p>
        <span className="text-xs font-medium text-violet-600 bg-violet-50 border border-violet-200 rounded px-2 py-0.5">
          평균 {avg.toFixed(1)}개
        </span>
      </div>
      <ResponsiveContainer width="100%" height={200}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis dataKey="date" tick={{ fontSize: 11 }} />
          <YAxis tick={{ fontSize: 11 }} />
          <Tooltip />
          <Bar dataKey="quantity" fill="#7c3aed" name="생산량" radius={[3, 3, 0, 0]} />
          <ReferenceLine y={avg} stroke="#7c3aed" strokeDasharray="5 4" strokeOpacity={0.5} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
