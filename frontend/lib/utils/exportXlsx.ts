import * as XLSX from 'xlsx';
import type { InventoryExportRow } from '../types';

/** 멀티시트 xlsx 다운로드 */
export function downloadXlsx(
  data: Record<string, unknown>[][],
  sheetNames: string[],
  filename: string
) {
  const wb = XLSX.utils.book_new();
  data.forEach((rows, i) => {
    const ws = XLSX.utils.json_to_sheet(rows);
    applyColumnWidths(ws, rows);
    XLSX.utils.book_append_sheet(wb, ws, sheetNames[i]);
  });
  XLSX.writeFile(wb, filename);
}

/**
 * 피벗 변환: rows=품목, cols=날짜, values=delta 합계
 * - 날짜 오름차순 정렬
 * - 맨 오른쪽 총계 열, 맨 아래 총계 행 추가
 * - absValue=true: 출고/생산차감 개별 시트처럼 맥락이 명확한 경우 양수로 표시
 */
export function buildPivot(rows: InventoryExportRow[], absValue = false): Record<string, unknown>[] {
  if (!rows.length) return [];

  // 1. 날짜 목록 추출 (unique, sorted)
  const dates = Array.from(new Set(rows.map((r) => r.date))).sort();

  // 2. 품목 목록 추출 (itemCode → itemName, 입력 순서 유지)
  const itemMap = new Map<string, string>();
  rows.forEach((r) => itemMap.set(r.itemCode, r.itemName));

  // 3. itemCode → { date → delta 합산 } 맵 구성
  const dataMap = new Map<string, Map<string, number>>();
  rows.forEach((r) => {
    if (!dataMap.has(r.itemCode)) dataMap.set(r.itemCode, new Map());
    const dateMap = dataMap.get(r.itemCode)!;
    dateMap.set(r.date, (dateMap.get(r.date) ?? 0) + r.delta);
  });

  // 4. 품목별 행 생성
  const result: Record<string, unknown>[] = [];
  const colTotals: Record<string, number> = {};

  for (const [itemCode, itemName] of itemMap) {
    const dateMap = dataMap.get(itemCode) ?? new Map();
    const row: Record<string, unknown> = { 품목코드: itemCode, 품목명: itemName };
    let rowTotal = 0;

    for (const date of dates) {
      const raw = dateMap.get(date) ?? 0;
      const val = absValue ? Math.abs(raw) : raw;
      row[date] = val;
      rowTotal += val;
      colTotals[date] = (colTotals[date] ?? 0) + val;
    }

    row['총계'] = rowTotal;
    result.push(row);
  }

  // 5. 총계 행 추가
  const totalRow: Record<string, unknown> = { 품목코드: '총계', 품목명: '' };
  let grandTotal = 0;
  for (const date of dates) {
    totalRow[date] = colTotals[date] ?? 0;
    grandTotal += colTotals[date] ?? 0;
  }
  totalRow['총계'] = grandTotal;
  result.push(totalRow);

  return result;
}

/** 컬럼 너비 자동 조정 */
function applyColumnWidths(ws: XLSX.WorkSheet, rows: Record<string, unknown>[]) {
  if (!rows.length) return;
  const keys = Object.keys(rows[0]);
  ws['!cols'] = keys.map((key) => ({
    wch: Math.max(
      key.length,
      ...rows.map((r) => String(r[key] ?? '').length)
    ) + 2,
  }));
}
