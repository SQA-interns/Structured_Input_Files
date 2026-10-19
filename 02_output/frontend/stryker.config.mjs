// Mutation testing (quality-requirements.md "Mutation scope"): all production source of the
// frontend, run against the full Vitest suite; test code and the mount-only bootstrap are excluded.
/** @type {import('@stryker-mutator/api/core').PartialStrykerOptions} */
export default {
  testRunner: "vitest",
  vitest: { configFile: "vite.config.ts" },
  mutate: [
    "src/**/*.{ts,tsx}",
    "!src/**/*.test.{ts,tsx}",
    "!src/**/*.d.ts",
    "!src/main.tsx",
    "!src/test-setup.ts",
    "!src/testConfig.ts",
    "!src/acceptance/**",
  ],
  coverageAnalysis: "perTest",
  reporters: ["clear-text", "progress", "html", "json"],
  htmlReporter: { fileName: "reports/mutation/mutation.html" },
  jsonReporter: { fileName: "reports/mutation/mutation.json" },
  tempDirName: ".stryker-tmp",
  concurrency: 4,
};
