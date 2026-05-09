'use client';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type RangePreset = '1M' | '3M' | '1Y' | 'custom';

const PRESETS: { value: RangePreset; label: string; days?: number }[] = [
  { value: '1M', label: '한달', days: 30 },
  { value: '3M', label: '분기', days: 90 },
  { value: '1Y', label: '년', days: 365 },
  { value: 'custom', label: '직접설정' },
];

const INPUT_CN =
  'h-9 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm ' +
  'outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50';

interface DateRangeFilterProps {
  onRangeChange: (startDate: string, endDate: string) => void;
}

function toDateStr(d: Date) {
  return d.toISOString().slice(0, 10);
}
function subtractDays(n: number) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return toDateStr(d);
}

export function DateRangeFilter({ onRangeChange }: DateRangeFilterProps) {
  const [preset, setPreset] = useState<RangePreset>('1M');
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');
  const today = toDateStr(new Date());

  function handlePreset(p: RangePreset) {
    setPreset(p);
    if (p !== 'custom') {
      const days = PRESETS.find((x) => x.value === p)!.days!;
      onRangeChange(subtractDays(days), today);
    }
  }

  function handleCustomStart(val: string) {
    setCustomStart(val);
    onRangeChange(val || subtractDays(30), customEnd || today);
  }

  function handleCustomEnd(val: string) {
    setCustomEnd(val);
    onRangeChange(customStart || subtractDays(30), val || today);
  }

  return (
    <div className="flex items-center gap-2 flex-wrap">
      <div className="flex gap-1">
        {PRESETS.map((p) => (
          <Button
            key={p.value}
            size="sm"
            variant={preset === p.value ? 'default' : 'outline'}
            onClick={() => handlePreset(p.value)}
          >
            {p.label}
          </Button>
        ))}
      </div>

      {preset === 'custom' && (
        <div className="flex items-center gap-1.5">
          <input
            type="date"
            className={cn(INPUT_CN)}
            value={customStart}
            max={customEnd || today}
            onChange={(e) => handleCustomStart(e.target.value)}
          />
          <span className="text-stone-400 text-sm">~</span>
          <input
            type="date"
            className={cn(INPUT_CN)}
            value={customEnd}
            min={customStart}
            max={today}
            onChange={(e) => handleCustomEnd(e.target.value)}
          />
        </div>
      )}
    </div>
  );
}
