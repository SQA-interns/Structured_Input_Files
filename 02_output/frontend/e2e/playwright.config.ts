import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests against the running local stack (docker-compose.yml): one test per scenario in
 * e2e-scenarios.md. The stack must be up; ORGANIZER_USERNAME, ORGANIZER_PASSWORD and
 * ORGANIZER_EMAILS must be exported from .env (see the frontend README).
 */
export default defineConfig({
  testDir: ".",
  testMatch: "*.e2e.ts",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 90_000,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:8080",
    trace: "off",
    actionTimeout: 10_000,
    navigationTimeout: 15_000,
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
