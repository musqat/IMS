import { test, expect } from '@playwright/test';
import { ACCOUNTS, login, clearSession } from './helpers';

test.describe('창고', () => {
  test.beforeEach(async ({ page }) => {
    await clearSession(page);
  });

  test('목록 - 소유 창고와 공유받은 창고가 나뉘어 보인다', async ({ page }) => {
    await login(page, ACCOUNTS.a);
    await page.goto('/warehouses');

    await expect(page.getByRole('heading', { name: '내 창고' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '공유받은 창고' })).toBeVisible();
    await expect(page.getByRole('link', { name: '서울 조립창고' })).toBeVisible();
  });

  test('상세 - 공유받은 창고도 이름과 재고가 보인다', async ({ page }) => {
    await login(page, ACCOUNTS.a);
    await page.goto('/warehouses');

    await page.getByRole('link', { name: '인천 전자부품창고' }).click();

    await expect(page.getByRole('heading', { name: /인천 전자부품창고/ })).toBeVisible();
    // 재고 목록이 렌더된다. 조회 실패를 "없음"으로 흘리지 않는지 본다
    await expect(page.getByText('등록된 재고가 없습니다')).toHaveCount(0);
  });

  test('VIEW 권한 - 쓰기 버튼이 뜨지 않는다', async ({ page }) => {
    // B는 A의 서울 조립창고를 VIEW로 공유받았다.
    // FULL 권한 배선이 죽어 있던 적이 있어 권한별 차이를 화면에서 확인한다
    await login(page, ACCOUNTS.b);
    await page.goto('/warehouses');

    await page.getByRole('link', { name: '서울 조립창고' }).click();
    await expect(page.getByRole('heading', { name: /서울 조립창고/ })).toBeVisible();

    await expect(page.getByRole('button', { name: '입고' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: '출고' })).toHaveCount(0);
  });

  test('소유자 - 자기 창고에는 쓰기 버튼이 있다', async ({ page }) => {
    await login(page, ACCOUNTS.a);
    await page.goto('/warehouses');

    await page.getByRole('link', { name: '서울 조립창고' }).click();
    await expect(page.getByRole('heading', { name: /서울 조립창고/ })).toBeVisible();

    await expect(page.getByRole('button', { name: '품목 추가' })).toBeVisible();
  });
});
