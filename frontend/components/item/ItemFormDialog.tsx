'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useCreateItem } from '@/hooks/mutations/useItemMutations';
import { cn } from '@/lib/utils';
import type { ItemType } from '@/lib/types';

const TYPE_OPTIONS: { value: ItemType; label: string; desc: string }[] = [
  { value: 'PRODUCT', label: '완제품', desc: '최종 생산물' },
  { value: 'SEMI',    label: '반제품', desc: '중간 조립품' },
  { value: 'PART',    label: '부품',   desc: '원자재·부속' },
];

const schema = z.object({
  itemCode: z.string().min(1, '품목코드를 입력하세요'),
  name: z.string().min(1, '품목명을 입력하세요'),
  type: z.enum(['PRODUCT', 'PART', 'SEMI']),
  description: z.string().optional(),
});
type FormData = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onClose: () => void;
  defaultType?: ItemType;
}

export function ItemFormDialog({ open, onClose, defaultType = 'PART' }: Props) {
  const { mutateAsync, isPending } = useCreateItem();
  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { type: defaultType },
  });

  const currentType = watch('type');

  const onSubmit = async (data: FormData) => {
    try {
      await mutateAsync(data);
      reset();
      onClose();
    } catch {}
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader><DialogTitle>품목 등록</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="itemCode">품목코드</Label>
            <Input id="itemCode" {...register('itemCode')} placeholder="예: P001" />
            {errors.itemCode && <p className="text-rose-600 text-xs">{errors.itemCode.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="name">품목명</Label>
            <Input id="name" {...register('name')} />
            {errors.name && <p className="text-rose-600 text-xs">{errors.name.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label>유형</Label>
            <div className="grid grid-cols-3 gap-2">
              {TYPE_OPTIONS.map(({ value, label, desc }) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setValue('type', value)}
                  className={cn(
                    'rounded-lg border px-3 py-2 text-left transition-colors',
                    currentType === value
                      ? 'border-stone-800 bg-stone-900 text-white'
                      : 'border-stone-200 bg-white text-stone-600 hover:bg-stone-50'
                  )}
                >
                  <p className="text-sm font-medium">{label}</p>
                  <p className={cn('text-xs mt-0.5', currentType === value ? 'text-stone-300' : 'text-stone-400')}>
                    {desc}
                  </p>
                </button>
              ))}
            </div>
            <input type="hidden" {...register('type')} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="description">설명 (선택)</Label>
            <Input id="description" {...register('description')} />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>취소</Button>
            <Button type="submit" disabled={isPending}>등록</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
