import { test, expect } from '@playwright/test';
import { ACCOUNTS, PASSWORD, login, clearSession } from './helpers';

test.describe('인증', () => {
  test.beforeEach(async ({ page }) => {
    await clearSession(page);
  });

  test('로그인 성공 - 대시보드로 이동하고 회사명이 보인다', async ({ page }) => {
    await login(page, ACCOUNTS.a);

    // 헤더에 로그인한 회사가 표시된다. 토큰만이 아니라 /users/me까지 돌았다는 뜻이다
    await expect(page.getByText('아이테크조립(주)')).toBeVisible();
  });

  test('로그인 실패 - 틀린 비밀번호는 대시보드로 넘어가지 않는다', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('이메일').fill(ACCOUNTS.a);
    await page.getByLabel('비밀번호').fill('WrongPassword123');
    await page.getByRole('button', { name: '로그인' }).click();

    // 토스트 문구보다 "넘어가지 않는다"가 본질이다.
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();

    // 토큰이 저장되지 않아야 한다
    const token = await page.evaluate(() => localStorage.getItem('accessToken'));
    expect(token).toBeNull();
  });

  test('비인증 접근 - 보호된 화면은 로그인으로 돌려보낸다', async ({ page }) => {
    await page.goto('/warehouses');

    await expect(page).toHaveURL(/\/login/);
  });

  test('로그아웃 - 세션이 지워지고 다시 들어갈 수 없다', async ({ page }) => {
    await login(page, ACCOUNTS.a);
    await page.getByRole('button', { name: '로그아웃' }).click();

    await expect(page).toHaveURL(/\/login/);

    // 뒤로 가기로 되돌아가지지 않아야 한다
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/);
  });

  test('회원가입 - 비밀번호 정책이 서버 요청 전에 막는다', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('회사명').fill('E2E테스트회사');
    await page.getByLabel('이메일').fill('e2e-weak@test.com');
    await page.getByLabel('비밀번호').fill('123456');
    await page.getByRole('button', { name: '회원가입' }).click();

    await expect(
      page.getByText('비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다')
    ).toBeVisible();
    await expect(page).toHaveURL(/\/register/);
  });
});
