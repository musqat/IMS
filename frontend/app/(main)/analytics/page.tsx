'use client';
import { useState } from 'react';
import { useAllProductions } from '@/hooks/queries/useProductions';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ProductionTrendChart } from '@/components/analytics/ProductionTrendChart';
import { ShortageBarChart } from '@/components/analytics/ShortageBarChart';
import { DateRangeFilter } from '@/components/analytics/DateRangeFilter';
import { ProductionOutboundSection } from '@/components/analytics/ProductionOutboundSection';
import { InventoryTrendSection } from '@/components/analytics/InventoryTrendSection';
import { toLocalDateString, daysAgo } from '@/lib/utils/date';

export default function AnalyticsPage() {
  const today = toLocalDateString(new Date());
  const [startDate, setStartDate] = useState(daysAgo(30));
  const [endDate, setEndDate] = useState(today);

  const { data: productions = [], isLoading } = useAllProductions();

  const dayCount = Math.max(
    1,
    Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / (1000 * 60 * 60 * 24)),
  );

  const filtered = productions.filter((p) => {
    const d = p.createdAt.slice(0, 10);
    return d >= startDate && d <= endDate;
  });

  return (
    <div className="space-y-6">
      {/* 헤더 + 날짜 필터 */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-bold text-stone-900">분석</h1>
        <DateRangeFilter
          onRangeChange={(start, end) => {
            setStartDate(start);
            setEndDate(end);
          }}
        />
      </div>

      {/* KPI */}
      <div className="grid grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-stone-500">기간 생산량</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold text-stone-900">
              {filtered.reduce((s, p) => s + p.quantity, 0).toLocaleString()}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* 생산 추이 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">생산 추이 (결산완료 기준)</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <p className="text-stone-400 text-sm py-10 text-center">로딩 중...</p>
          ) : (
            <ProductionTrendChart records={productions} startDate={startDate} endDate={endDate} />
          )}
        </CardContent>
      </Card>

      {/* 제품별 생산·출고 추이 + 안전재고 추천 */}
      <ProductionOutboundSection
        productions={productions}
        startDate={startDate}
        endDate={endDate}
        dayCount={dayCount}
      />

      {/* 부품 부족 Top5 + 재고 추이 */}
      <div className="grid grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">자주 부족한 부품 Top 5</CardTitle>
          </CardHeader>
          <CardContent>
            <ShortageBarChart records={filtered} />
          </CardContent>
        </Card>

        <InventoryTrendSection />
      </div>
    </div>
  );
}
