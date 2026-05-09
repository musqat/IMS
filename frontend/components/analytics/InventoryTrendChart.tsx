'use client';
import { LineChart, Line, XAxis, YAxis, Tooltip, ReferenceLine, ResponsiveContainer } from 'recharts';
import type { InventoryHistoryResponse } from '@/lib/types';
import { useMemo } from 'react';

interface Props {
  history: InventoryHistoryResponse[];
  safetyStock: number;
  initialQuantity?: number;
}

export function InventoryTrendChart({ history, safetyStock, initialQuantity = 0 }: Props) {
  const data = useMemo(() => {
    const sorted = [...history].sort(
      (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );

    let cumulative = initialQuantity;
    const byDate: Record<string, number> = {};
    sorted.forEach((h) => {
      cumulative += h.delta;
      const date = h.createdAt.slice(0, 10);
      byDate[date] = cumulative;
    });

    return Object.entries(byDate).map(([date, quantity]) => ({ date, quantity }));
  }, [history, initialQuantity]);

  if (data.length === 0) {
    return <p className="text-stone-400 text-sm text-center py-10">이력 데이터가 없습니다.</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={200}>
      <LineChart data={data}>
        <XAxis dataKey="date" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} />
        <Tooltip />
        <ReferenceLine y={safetyStock} stroke="#f59e0b" strokeDasharray="4 4" label={{ value: '안전재고', fontSize: 11 }} />
        <Line type="monotone" dataKey="quantity" stroke="#7c3aed" dot={false} name="재고량" />
      </LineChart>
    </ResponsiveContainer>
  );
}
