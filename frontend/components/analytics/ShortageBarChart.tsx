'use client';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import type { ProductionResponse } from '@/lib/types';
import { useMemo } from 'react';

interface Props {
  records: ProductionResponse[];
}

export function ShortageBarChart({ records }: Props) {
  const data = useMemo(() => {
    const counts: Record<string, number> = {};
    records
      .filter((r) => r.settlement?.result === 'ANOMALY' && r.settlement.anomalyDetail)
      .forEach((r) => {
        try {
          const detail = JSON.parse(r.settlement!.anomalyDetail!);
          Object.keys(detail).forEach((itemCode) => {
            counts[itemCode] = (counts[itemCode] ?? 0) + 1;
          });
        } catch {
          // skip malformed anomalyDetail
        }
      });

    return Object.entries(counts)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 5)
      .map(([itemCode, count]) => ({ itemCode, count }));
  }, [records]);

  if (data.length === 0) {
    return <p className="text-stone-400 text-sm text-center py-10">부족 부품 데이터가 없습니다.</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart layout="vertical" data={data}>
        <XAxis type="number" tick={{ fontSize: 12 }} />
        <YAxis dataKey="itemCode" type="category" tick={{ fontSize: 12 }} width={60} />
        <Tooltip />
        <Bar dataKey="count" fill="#f59e0b" name="부족 발생 횟수" />
      </BarChart>
    </ResponsiveContainer>
  );
}
