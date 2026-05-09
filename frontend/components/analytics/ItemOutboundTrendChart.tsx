'use client';
import { useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, ReferenceLine } from 'recharts';
import type { InventoryHistoryResponse } from '@/lib/types/index';

interface Props {
  history: InventoryHistoryResponse[];
  startDate: string;
  endDate: string;
}

export function ItemOutboundTrendChart({ history, startDate, endDate }: Props) {
  const data = useMemo(() => {
    const byDate: Record<string, number> = {};
    history
      .filter((h) => {
        const d = h.createdAt.slice(0, 10);
        return h.type === 'OUT' && d >= startDate && d <= endDate;
      })
      .forEach((h) => {
        const date = h.createdAt.slice(0, 10);
        byDate[date] = (byDate[date] ?? 0) + Math.abs(h.delta);
      });

    return Object.entries(byDate)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, quantity]) => ({ date, quantity }));
  }, [history, startDate, endDate]);

  const avg = data.length > 0
    ? data.reduce((s, d) => s + d.quantity, 0) / data.length
    : 0;

  if (data.length === 0) {
    return <p className="text-stone-400 text-sm text-center py-10">출고 데이터가 없습니다.</p>;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <p className="text-sm font-medium text-stone-600">출고 추이</p>
        <span className="text-xs font-medium text-sky-600 bg-sky-50 border border-sky-200 rounded px-2 py-0.5">
          평균 {avg.toFixed(1)}개
        </span>
      </div>
      <ResponsiveContainer width="100%" height={200}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis dataKey="date" tick={{ fontSize: 11 }} />
          <YAxis tick={{ fontSize: 11 }} />
          <Tooltip />
          <Bar dataKey="quantity" fill="#0ea5e9" name="출고량" radius={[3, 3, 0, 0]} />
          <ReferenceLine y={avg} stroke="#0ea5e9" strokeDasharray="5 4" strokeOpacity={0.5} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
