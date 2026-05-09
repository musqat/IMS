'use client';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import type { ProductionResponse } from '@/lib/types';
import { useMemo } from 'react';

interface Props {
  records: ProductionResponse[];
  startDate: string;
  endDate: string;
}

export function ProductionTrendChart({ records, startDate, endDate }: Props) {
  const data = useMemo(() => {
    const byDate: Record<string, number> = {};
    records
      .filter((r) => {
        const d = r.createdAt.slice(0, 10);
        return r.status === 'SETTLED' && d >= startDate && d <= endDate;
      })
      .forEach((r) => {
        const date = r.createdAt.slice(0, 10);
        byDate[date] = (byDate[date] ?? 0) + r.quantity;
      });

    return Object.entries(byDate)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, quantity]) => ({ date, quantity }));
  }, [records, startDate, endDate]);

  return (
    <ResponsiveContainer width="100%" height={200}>
      <AreaChart data={data}>
        <XAxis dataKey="date" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} />
        <Tooltip />
        <Area type="monotone" dataKey="quantity" stroke="#7c3aed" fill="#ede9fe" name="생산량" />
      </AreaChart>
    </ResponsiveContainer>
  );
}
