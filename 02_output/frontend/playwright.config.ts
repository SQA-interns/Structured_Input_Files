import { defineConfig, devices } from "@playwright/test";

declare const process: { env: Record<string, string | undefined> };

/** End-to-end tests run against the local compose stack (`02_output/docker-compose.yml`). */
export default defineConfig({
  testDir: "./e2e",
  testMatch: "**/*.e2e.spec.ts",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:8080",
    trace: "off",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
