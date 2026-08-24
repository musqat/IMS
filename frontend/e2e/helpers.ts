import { Page, expect } from '@playwright/test';

/** 시드 계정. 비밀번호는 seed.sql 주석 참고 */
export const ACCOUNTS = {
  /** 메인 데모 계정. 창고 2개 소유, 3개 공유받음 */
  a: 'a@ims.dev',
  /** A가 초대. A의 서울 조립창고를 VIEW로 공유받았다 */
  b: 'b@ims.dev',
  /** A가 초대 중(PENDING). 받은 초대가 있다 */
  d: 'd@ims.dev',
} as const;

export const PASSWORD = 'Test1234!';

/**
 * 로그인 폼으로 실제 로그인한다.
 *
 * localStorage에 토큰을 직접 넣는 방식은 쓰지 않는다.
 */
export async function login(page: Page, email: string, password = PASSWORD) {
  await page.goto('/login');
  await page.getByLabel('이메일').fill(email);
  await page.getByLabel('비밀번호').fill(password);
  await page.getByRole('button', { name: '로그인' }).click();
  await expect(page).toHaveURL(/\/dashboard/);
}

/** 로그인 없이 시작하도록 저장소를 비운다 */
export async function clearSession(page: Page) {
  await page.goto('/login');
  await page.evaluate(() => localStorage.clear());
}
