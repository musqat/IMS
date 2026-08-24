import { defineConfig, devices } from '@playwright/test';

/**
 * E2E 설정
 *
 * 백엔드·DB·Redis가 떠 있어야 한다. 프론트는 이 설정이 직접 띄운다.
 *
 *   docker compose up -d postgres redis
 *   cd backend && ./gradlew bootRun
 *   cd frontend && npm run e2e
 *
 * 시드 데이터에 의존한다. 계정 비밀번호는 seed.sql 주석에 있다.
 */
export default defineConfig({
  testDir: './e2e',

  // 시드 데이터를 공유하므로 병렬로 돌리면 서로 간섭한다.
  fullyParallel: false,
  workers: 1,

  // 실패를 흘려보내지 않는다. CI에서 test.only가 섞이면 나머지가 조용히 안 돈다
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,

  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : [['list']],

  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    // 실패했을 때 원인을 찾을 근거를 남긴다
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },

  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],

  // 프론트는 여기서 띄운다. 이미 떠 있으면 그걸 쓴다.
  webServer: {
    command: 'npm run start',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
