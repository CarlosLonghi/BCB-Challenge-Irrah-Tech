import { defineConfig, devices } from '@playwright/test';

// E2E contra a aplicação inteira no ar (docker compose up -d --build --wait): frontend, backend, Postgres e RabbitMQ.
export default defineConfig({
  testDir: './e2e',
  // O worker leva ~8 s até DELIVERED e a leitura simulada mais ~4 s
  timeout: 60_000,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
