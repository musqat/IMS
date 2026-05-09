'use client';
import { useParams } from 'next/navigation';
import { useItem } from '@/hooks/queries/useItems';
import { BomTreeView } from '@/components/bom/BomTreeView';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const TYPE_LABEL: Record<string, string> = {
  PRODUCT: '완성품',
  PART: '부품',
  SEMI: '반제품',
};

const TYPE_COLOR: Record<string, string> = {
  PRODUCT: 'bg-violet-100 text-violet-700 border-violet-200',
  PART: 'bg-stone-100 text-stone-600 border-stone-200',
  SEMI: 'bg-amber-100 text-amber-700 border-amber-200',
};

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>();
  const itemId = Number(id);
  const { data: item, isLoading } = useItem(itemId);

  if (isLoading) return <p className="text-stone-400">로딩 중...</p>;
  if (!item) return <p className="text-rose-500">품목을 찾을 수 없습니다.</p>;

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <h1 className="text-2xl font-bold text-stone-900">{item.name}</h1>
        <Badge className={TYPE_COLOR[item.type]}>{TYPE_LABEL[item.type]}</Badge>
      </div>

      <div className="grid grid-cols-3 gap-6">
        <div className="col-span-1">
          <Card className="border-stone-200">
            <CardHeader>
              <CardTitle className="text-base">품목 정보</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div>
                <p className="text-stone-400 text-xs mb-0.5">품목코드</p>
                <p className="font-mono font-medium text-stone-800">{item.itemCode}</p>
              </div>
              <div>
                <p className="text-stone-400 text-xs mb-0.5">유형</p>
                <p className="text-stone-800">{TYPE_LABEL[item.type]}</p>
              </div>
              <div>
                <p className="text-stone-400 text-xs mb-0.5">설명</p>
                <p className="text-stone-600">{item.description || '-'}</p>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="col-span-2">
          <Card className="border-stone-200">
            <CardHeader>
              <CardTitle className="text-base">자재 명세 · 부품 구조</CardTitle>
            </CardHeader>
            <CardContent>
              <BomTreeView rootItemId={itemId} />
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
