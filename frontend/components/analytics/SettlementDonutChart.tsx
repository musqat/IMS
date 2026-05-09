'use client';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import type { ProductionResponse } from '@/lib/types';
import { useMemo } from 'react';

interface Props {
  records: ProductionResponse[];
}

const COLORS = { SUCCESS: '#059669', ANOMALY: '#e11d48' };

export function SettlementDonutChart({ records }: Props) {
  const data = useMemo(() => {
    const settled = records.filter((r) => r.settlement);
    const success = settled.filter((r) => r.settlement?.result === 'SUCCESS').length;
    const anomaly = settled.filter((r) => r.settlement?.result === 'ANOMALY').length;
    return [
      { name: '성공', value: success, color: COLORS.SUCCESS },
      { name: '확인필요', value: anomaly, color: COLORS.ANOMALY },
    ].filter((d) => d.value > 0);
  }, [records]);

  if (data.length === 0) {
    return <p className="text-stone-400 text-sm text-center py-10">결산 데이터가 없습니다.</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={200}>
      <PieChart>
        <Pie data={data} dataKey="value" cx="50%" cy="50%" innerRadius={50} outerRadius={80}>
          {data.map((entry) => (
            <Cell key={entry.name} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}
