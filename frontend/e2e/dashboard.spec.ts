import { test, expect } from '@playwright/test';
import { ACCOUNTS, login, clearSession } from './helpers';

test.describe('대시보드', () => {
  test.beforeEach(async ({ page }) => {
    await clearSession(page);
    await login(page, ACCOUNTS.a);
  });

  test('KPI가 0이 아닌 값을 표시한다', async ({ page }) => {
    // 숫자가 실제로 채워지는지 본다
    const settled = page.locator('a[href="/production?status=SETTLED"]');
    await expect(settled).toBeVisible();
    await expect(settled).not.toContainText(/^결산완료0$/);

    const value = await settled.locator('p.text-3xl').innerText();
    expect(Number(value)).toBeGreaterThan(0);
  });

  test('KPI 클릭 - 결산완료는 해당 탭으로 연결된다', async ({ page }) => {
    // KPI가 세는 축(결산 결과)과 페이지 탭의 축(생산 상태)이 달라
    // 숫자를 눌렀는데 빈 화면이 나오던 적이 있다
    await page.locator('a[href="/production?status=SETTLED"]').click();

    await expect(page).toHaveURL(/\/production\?status=SETTLED/);
    // 목록이 비어 있지 않아야 한다
    await expect(page.getByText('생산 기록이 없습니다')).toHaveCount(0);
  });

  test('부족 분석 - 창고를 펼치면 결과가 나온다', async ({ page }) => {
    const row = page.getByRole('button', { name: /서울 조립창고/ });
    await expect(row).toBeVisible();
    await row.click();

    // 분석 중이 끝나면 부족 목록이나 "이상 없음"이 뜬다.
    // 조회 실패를 "데이터 없음"으로 흘리지 않는지 확인하는 지점이다
    await expect(
      page.getByText(/개 품목 부족|모든 완제품을 생산할 수 있습니다/)
    ).toBeVisible({ timeout: 15_000 });
  });

  test('공유받은 창고도 부족 분석 목록에 나온다', async ({ page }) => {
    // 소유 창고만 보이던 회귀가 있었다. 공유 뱃지로 확인한다
    await expect(page.getByText(/공유 ·/).first()).toBeVisible();
  });
});
