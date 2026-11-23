import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  use: { baseURL: 'http://127.0.0.1:5174', trace: 'retain-on-failure' },
  webServer: [
    {
      command:
        'java -jar ../backend/api/target/api-1.0.0-SNAPSHOT.jar --spring.profiles.active=e2e --server.port=8081',
      port: 8081,
      timeout: 60000,
    },
    {
      command: 'npm run dev -- --port 5174 --strictPort',
      env: { API_URL: 'http://127.0.0.1:8081' },
      url: 'http://127.0.0.1:5174',
      timeout: 30000,
    },
  ],
});
