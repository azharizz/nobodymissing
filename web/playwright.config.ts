import { defineConfig } from '@playwright/test';

// End-to-end in DEMO mode (fakes with canned AI; no Firebase, no secrets): empty env vars override web/.env.local.
export default defineConfig({
  testDir: 'e2e',
  use: { baseURL: 'http://localhost:4174', viewport: { width: 412, height: 900 } },
  webServer: {
    command: 'npx vite build --mode e2e && npx vite preview --port 4174 --strictPort',
    url: 'http://localhost:4174', reuseExistingServer: false, timeout: 120_000,
    env: { VITE_FIREBASE_API_KEY: '', VITE_USE_EMULATOR: 'false' },
  },
});
