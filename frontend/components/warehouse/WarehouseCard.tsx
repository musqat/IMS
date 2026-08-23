'use client';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { MoreVertical, Share2, Trash2, Warehouse, Archive } from 'lucide-react';
import { useDeleteWarehouse, useDeactivateWarehouse } from '@/hooks/mutations/useWarehouseMutations';
import { useConfirm } from '@/components/common/ConfirmProvider';
import type { WarehouseResponse } from '@/lib/types';

interface Props {
  warehouse: WarehouseResponse;
  isOwner: boolean;
  permission?: 'VIEW' | 'FULL';
  onShare?: () => void;
}

export function WarehouseCard({ warehouse, isOwner, permission, onShare }: Props) {
  const { mutate: deleteWarehouse } = useDeleteWarehouse();
  const { mutate: deactivateWarehouse } = useDeactivateWarehouse();
  const confirm = useConfirm();

  return (
    <Card className="border-stone-200 hover:border-violet-300 transition-colors">
      <CardHeader className="pb-2 flex flex-row items-start justify-between">
        <div className="flex items-center gap-2">
          <Warehouse className="h-5 w-5 text-violet-600" />
          <CardTitle className="text-base">
            <Link href={`/warehouses/${warehouse.id}`} className="hover:underline">
              {warehouse.name}
            </Link>
          </CardTitle>
        </div>
        {isOwner ? (
          <DropdownMenu>
            <DropdownMenuTrigger className="inline-flex items-center justify-center h-7 w-7 rounded-md hover:bg-stone-100 transition-colors">
              <MoreVertical className="h-4 w-4" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={onShare}>
                <Share2 className="h-4 w-4 mr-2" />
                공유 설정
              </DropdownMenuItem>
              {/* 재고·생산 기록이 있으면 삭제가 막히므로 비활성화를 먼저 보여준다 */}
              <DropdownMenuItem
                onClick={async () => {
                  const ok = await confirm({
                    title: `"${warehouse.name}" 창고를 비활성화하시겠습니까?`,
                    description: [
                      '목록에서 숨겨지고 입출고가 차단됩니다.',
                      '재고와 이력은 그대로 보존되며 언제든 다시 활성화할 수 있습니다.',
                    ],
                    confirmLabel: '비활성화',
                  });
                  if (ok) deactivateWarehouse(warehouse.id);
                }}
              >
                <Archive className="h-4 w-4 mr-2" />
                창고 비활성화
              </DropdownMenuItem>
              <DropdownMenuItem
                className="text-rose-600"
                onClick={async () => {
                  // 재고·생산 기록은 함께 지우지 않고 삭제를 거부한다
                  const ok = await confirm({
                    title: `"${warehouse.name}" 창고를 삭제하시겠습니까?`,
                    description: [
                      '공유 설정도 함께 삭제됩니다.',
                      '재고나 생산 기록이 남아 있으면 삭제할 수 없습니다.',
                    ],
                    confirmLabel: '삭제',
                    destructive: true,
                  });
                  if (ok) deleteWarehouse(warehouse.id);
                }}
              >
                <Trash2 className="h-4 w-4 mr-2" />
                삭제
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <Badge variant="outline" className="text-violet-600 border-violet-200">
            {permission ?? 'VIEW'}
          </Badge>
        )}
      </CardHeader>
      <CardContent>
        <p className="text-sm text-stone-500">{warehouse.location || '위치 미지정'}</p>
        <p className="text-xs text-stone-400 mt-1">{warehouse.ownerCompanyName}</p>
      </CardContent>
    </Card>
  );
}
