import { defineConfig, devices } from "@playwright/test";

// End-to-end tests run against the local stack started with `docker compose up` in 02_output.
export default defineConfig({
  testDir: "./tests/e2e",
  timeout: 60_000,
  retries: 0,
  workers: 1,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:8081",
    trace: "off",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
