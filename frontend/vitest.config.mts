import { defineConfig } from 'vitest/config';

export default defineConfig({
  // vitest 4는 esbuild가 아니라 oxc를 쓴다
  oxc: { jsx: { runtime: 'automatic' } },
  resolve: { tsconfigPaths: true },
  test: {
    environment: 'jsdom',
    // 시간대를 고정한다. toLocalDateString이 막는 결함이 UTC에서는 재현되지 않아,
    env: { TZ: 'Asia/Seoul' },
    setupFiles: ['./vitest.setup.ts'],
    // e2e/는 Playwright가 돈다. vitest 기본 include가 *.spec.ts라 안 빼면 같이 집는다
    exclude: ['node_modules', '.next', 'e2e'],
    include: ['**/*.test.{ts,tsx}'],
  },
});
