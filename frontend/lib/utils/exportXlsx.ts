import type { Workbook, Worksheet, Style } from 'exceljs';
import type { InventoryExportRow } from '../types';

// ─── 스타일 상수 ──────────────────────────────────────────────
// ExcelJS는 ARGB(알파 2자리 + RGB 6자리)를 쓴다. 앞의 FF가 불투명.
const HEADER_BG = 'FF4F46E5'; // 인디고
const TOTAL_BG  = 'FFE0E7FF'; // 연인디고
const WHITE_BG  = 'FFFFFFFF';
const GRAY_BG   = 'FFF5F5F5';
const LINE      = 'FFD1D5DB';

type Align = 'left' | 'center' | 'right';

const fill = (argb: string): Style['fill'] => ({
  type: 'pattern',
  pattern: 'solid',
  fgColor: { argb },
});

const BORDER: Style['border'] = {
  top:    { style: 'thin', color: { argb: LINE } },
  bottom: { style: 'thin', color: { argb: LINE } },
  left:   { style: 'thin', color: { argb: LINE } },
  right:  { style: 'thin', color: { argb: LINE } },
};

const headerStyle = (horizontal: Align = 'center'): Partial<Style> => ({
  fill: fill(HEADER_BG),
  font: { bold: true, color: { argb: 'FFFFFFFF' }, size: 11 },
  alignment: { horizontal, vertical: 'middle' },
  border: BORDER,
});

const totalStyle = (horizontal: Align = 'right'): Partial<Style> => ({
  fill: fill(TOTAL_BG),
  font: { bold: true, size: 10 },
  alignment: { horizontal, vertical: 'middle' },
  border: BORDER,
});

const dataStyle = (striped: boolean, horizontal: Align = 'right'): Partial<Style> => ({
  fill: fill(striped ? GRAY_BG : WHITE_BG),
  font: { size: 10 },
  alignment: { horizontal, vertical: 'middle' },
  border: BORDER,
});

// ─── 멀티시트 xlsx 다운로드 ──────────────────────────────────
/**
 * exceljs는 번들이 커서 정적 import하면 초기 로딩에 얹힌다.
 * 다운로드 시점에 동적 import해서 export 페이지에서만 받아가게 한다.
 */
export async function downloadXlsx(
  sheets: { rows: InventoryExportRow[]; name: string; absValue?: boolean }[],
  filename: string,
): Promise<void> {
  const { Workbook: WorkbookCtor } = await import('exceljs');
  const wb: Workbook = new WorkbookCtor();

  sheets.forEach(({ rows, name, absValue = false }) => {
    buildStyledSheet(wb, name, rows, absValue);
  });

  const buffer = await wb.xlsx.writeBuffer();
  saveBlob(
    new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }),
    filename,
  );
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

// ─── 스타일 적용 피벗 시트 생성 ──────────────────────────────
/**
 * 피벗 변환: rows=품목, cols=날짜, values=delta 합계
 * - 날짜 오름차순 정렬
 * - 맨 오른쪽 합계 열, 맨 아래 합계 행 추가
 * - absValue=true: 출고/생산차감처럼 맥락이 명확한 경우 양수로 표시
 */
function buildStyledSheet(
  wb: Workbook,
  name: string,
  rows: InventoryExportRow[],
  absValue: boolean,
): Worksheet {
  const ws = wb.addWorksheet(name);

  if (!rows.length) {
    const cell = ws.getCell(1, 1);
    cell.value = '데이터가 없습니다.';
    cell.style = { font: { italic: true, color: { argb: 'FF9CA3AF' } } };
    return ws;
  }

  // 1. 날짜 목록 (unique, sorted)
  const dates = Array.from(new Set(rows.map((r) => r.date))).sort();

  // 2. 품목 목록 (itemCode → itemName, 입력 순서 유지)
  const itemMap = new Map<string, string>();
  rows.forEach((r) => itemMap.set(r.itemCode, r.itemName));

  // 3. itemCode → { date → delta 합산 }
  const dataMap = new Map<string, Map<string, number>>();
  rows.forEach((r) => {
    if (!dataMap.has(r.itemCode)) dataMap.set(r.itemCode, new Map());
    const dm = dataMap.get(r.itemCode)!;
    dm.set(r.date, (dm.get(r.date) ?? 0) + r.delta);
  });

  // 4. 헤더 행: [품목코드, 품목명, ...날짜, 합계]
  const cols = ['품목코드', '품목명', ...dates, '합계'];
  cols.forEach((label, i) => {
    const cell = ws.getCell(1, i + 1);
    cell.value = label;
    cell.style = headerStyle(i === 0 ? 'center' : i === 1 ? 'left' : 'right');
  });

  // 5. 품목 데이터 행
  const colTotals: number[] = new Array(dates.length).fill(0);
  let dataRowIdx = 0;

  for (const [itemCode, itemName] of itemMap) {
    const rowNo = dataRowIdx + 2; // 1-based + 헤더 1행
    const dm = dataMap.get(itemCode) ?? new Map<string, number>();
    const striped = dataRowIdx % 2 === 1;

    ws.getCell(rowNo, 1).value = itemCode;
    ws.getCell(rowNo, 1).style = dataStyle(striped, 'center');
    ws.getCell(rowNo, 2).value = itemName;
    ws.getCell(rowNo, 2).style = dataStyle(striped, 'left');

    let rowTotal = 0;
    dates.forEach((date, di) => {
      const raw = dm.get(date) ?? 0;
      const val = absValue ? Math.abs(raw) : raw;
      const cell = ws.getCell(rowNo, di + 3);
      cell.value = val;
      cell.style = dataStyle(striped);
      rowTotal += val;
      colTotals[di] += val;
    });

    const totalCell = ws.getCell(rowNo, dates.length + 3);
    totalCell.value = rowTotal;
    totalCell.style = { ...dataStyle(striped), font: { bold: true, size: 10 } };
    dataRowIdx++;
  }

  // 6. 합계 행
  const totalRowNo = dataRowIdx + 2;
  ws.getCell(totalRowNo, 1).value = '합계';
  ws.getCell(totalRowNo, 1).style = totalStyle('center');
  ws.getCell(totalRowNo, 2).value = '';
  ws.getCell(totalRowNo, 2).style = totalStyle('left');

  let grandTotal = 0;
  colTotals.forEach((val, di) => {
    const cell = ws.getCell(totalRowNo, di + 3);
    cell.value = val;
    cell.style = totalStyle();
    grandTotal += val;
  });
  const grandCell = ws.getCell(totalRowNo, dates.length + 3);
  grandCell.value = grandTotal;
  grandCell.style = totalStyle();

  // 7. 열 너비 + 틀 고정 (품목코드·품목명 2열 + 헤더 1행)
  ws.getColumn(1).width = 14;
  ws.getColumn(2).width = 22;
  dates.forEach((_, di) => {
    ws.getColumn(di + 3).width = 12;
  });
  ws.getColumn(dates.length + 3).width = 12;

  ws.views = [{ state: 'frozen', xSplit: 2, ySplit: 1 }];

  return ws;
}
