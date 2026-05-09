'use client';
import Link from 'next/link';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Trash2, ChevronRight } from 'lucide-react';
import { useDeleteItem } from '@/hooks/mutations/useItemMutations';
import type { ItemResponse } from '@/lib/types';

interface Props {
  items: ItemResponse[];
}

export function ItemTable({ items }: Props) {
  const { mutate: deleteItem } = useDeleteItem();

  return (
    <div className="rounded-lg border border-stone-200 overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow className="bg-stone-50">
            <TableHead>품목코드</TableHead>
            <TableHead>품목명</TableHead>
            <TableHead>설명</TableHead>
            <TableHead className="text-center">자재 명세</TableHead>
            <TableHead className="text-center">삭제</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.length === 0 ? (
            <TableRow>
              <TableCell colSpan={5} className="text-center text-stone-400 py-10">
                등록된 품목이 없습니다.
              </TableCell>
            </TableRow>
          ) : (
            items.map((item) => (
              <TableRow key={item.id} className="hover:bg-stone-50">
                <TableCell className="font-mono text-sm">{item.itemCode}</TableCell>
                <TableCell className="font-medium">{item.name}</TableCell>
                <TableCell className="text-stone-500 text-sm">{item.description ?? '-'}</TableCell>
                <TableCell className="text-center">
                  <Link href={`/items/${item.id}`}>
                    <Button size="sm" variant="ghost">
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </Link>
                </TableCell>
                <TableCell className="text-center">
                  <Button
                    size="sm"
                    variant="ghost"
                    className="text-rose-500 hover:text-rose-700"
                    onClick={() => deleteItem(item.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  );
}
