import { test, expect } from '@playwright/test';
import { ACCOUNTS, login, clearSession } from './helpers';

/**
 * 초대 수락은 DB를 바꾼다.
 * 시드의 A→D PENDING 초대를 소비하므로 파일 안에서 순서가 중요하고,
 * 다시 돌리려면 DB를 새로 만들어야 한다.
 */
test.describe.configure({ mode: 'serial' });

test.describe('파트너', () => {
  test.beforeEach(async ({ page }) => {
    await clearSession(page);
  });

  test('파트너 목록 - 초대 방향과 무관하게 한 목록에 나온다', async ({ page }) => {
    // 내 하청사 / 내 본사로 나뉘어 있던 것을 하나로 합쳤다.
    // main/sub가 기능을 가르지 않기 때문이다
    await login(page, ACCOUNTS.a);
    await page.goto('/partners');

    await expect(page.getByRole('heading', { name: '내 파트너사' })).toBeVisible();
    // A가 초대한 쪽(비전전자)과 A를 초대한 쪽(이스마트코리아)이 같이 보인다
    await expect(page.getByText('비전전자', { exact: true })).toBeVisible();
    await expect(page.getByText('이스마트코리아(주)')).toBeVisible();
  });

  test('보낸 초대 - 대기 중인 초대와 남은 기간이 보인다', async ({ page }) => {
    await login(page, ACCOUNTS.a);
    await page.goto('/partners');

    await expect(page.getByRole('heading', { name: '보낸 초대' })).toBeVisible();
    await expect(page.getByText('디로지스(주)')).toBeVisible();
    await expect(page.getByText('수락을 기다리는 중')).toBeVisible();
  });

  test('헤더 알림 - 받은 초대가 없으면 뜨지 않는다', async ({ page }) => {
    await login(page, ACCOUNTS.a);

    await expect(page.getByRole('button', { name: /받은 초대/ })).toHaveCount(0);
  });

  test('헤더 알림 - 받은 초대가 있으면 뜨고 클릭하면 파트너로 간다', async ({ page }) => {
    await login(page, ACCOUNTS.d);

    const bell = page.getByRole('button', { name: /받은 초대/ });
    await expect(bell).toBeVisible();
    await bell.click();

    await expect(page).toHaveURL(/\/partners/);
    await expect(page.getByRole('heading', { name: '받은 초대' })).toBeVisible();
  });

  test('초대 수락 - 목록에서 사라지고 파트너가 된다', async ({ page }) => {
    await login(page, ACCOUNTS.d);
    await page.goto('/partners');

    await expect(page.getByRole('heading', { name: '받은 초대' })).toBeVisible();
    await page.getByRole('button', { name: '수락' }).click();

    // 수락하면 받은 초대 섹션이 사라지고 파트너 목록에 들어간다
    await expect(page.getByRole('heading', { name: '받은 초대' })).toHaveCount(0);
    await expect(page.getByRole('heading', { name: '내 파트너사' })).toBeVisible();
    await expect(page.getByText('아이테크조립(주)')).toBeVisible();

    // 헤더 뱃지도 같이 사라진다
    await expect(page.getByRole('button', { name: /받은 초대/ })).toHaveCount(0);
  });
});
