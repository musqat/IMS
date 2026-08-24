import { describe, it, expect } from 'vitest';
import { productionKeys } from './useProductions';
import { inventoryKeys } from './useInventories';

/**
 * 캐시 키는 요청을 유일하게 식별해야 한다.
 * 파라미터가 하나 빠지면 그만큼 다른 요청이 같은 것으로 취급된다.
 */
describe('productionKeys', () => {
  it('size가 다르면 키도 다르다', () => {
    // size가 키에 없어서 대시보드(5건)와 목록 페이지(20건)가
    // 서로의 응답을 덮어쓰던 결함이 있었다
    expect(productionKeys.byStatus('PENDING', 0, 5))
      .not.toEqual(productionKeys.byStatus('PENDING', 0, 20));
  });

  it('같은 인자면 같은 키다', () => {
    expect(productionKeys.byStatus('PENDING', 0, 5))
      .toEqual(productionKeys.byStatus('PENDING', 0, 5));
  });

  it('상태·페이지도 키를 가른다', () => {
    expect(productionKeys.byStatus('PENDING', 0, 5))
      .not.toEqual(productionKeys.byStatus('SETTLED', 0, 5));
    expect(productionKeys.byStatus('PENDING', 0, 5))
      .not.toEqual(productionKeys.byStatus('PENDING', 1, 5));
  });

  it('무효화 프리픽스가 byStatus 키와 맞물린다', () => {
    // 생산 기록을 바꾼 뒤 ['productions','byStatus']로 무효화한다.
    // 앞 두 칸이 어긋나면 목록이 갱신되지 않는다
    expect(productionKeys.byStatus('PENDING', 0, 5).slice(0, 2))
      .toEqual(['productions', 'byStatus']);
  });
});

describe('inventoryKeys', () => {
  it('keyword가 없을 때와 빈 문자열일 때가 같은 키다', () => {
    expect(inventoryKeys.list(1)).toEqual(inventoryKeys.list(1, ''));
  });

  it('keyword가 다르면 키도 다르다', () => {
    expect(inventoryKeys.list(1, '볼트')).not.toEqual(inventoryKeys.list(1, '너트'));
  });

  it('창고가 다르면 키도 다르다', () => {
    expect(inventoryKeys.list(1)).not.toEqual(inventoryKeys.list(2));
  });

  it('모든 키가 all() 프리픽스로 시작한다 — 일괄 무효화가 걸린다', () => {
    const prefix = inventoryKeys.all();
    for (const key of [
      inventoryKeys.list(1),
      inventoryKeys.history(1, 2),
      inventoryKeys.maxProducible(1, 2),
      inventoryKeys.shortage(1),
      inventoryKeys.depletion(1, '2026-01-01', '2026-08-01'),
    ]) {
      expect(key.slice(0, prefix.length)).toEqual([...prefix]);
    }
  });
});
