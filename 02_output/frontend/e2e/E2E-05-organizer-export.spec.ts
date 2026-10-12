import { expect, test } from "@playwright/test";
import {
  formConfig,
  organizerAuthorization,
  readWorkbook,
  sql,
  uniqueEmail,
} from "./support/stack";

// E2E-05 covers AC-008-01, AC-008-02, DoD-P04.
test("E2E-05 organizer exports registrations (AC-008-01, AC-008-02)", async ({
  page,
}) => {
  // Precondition: at least one stored registration.
  const config = await formConfig(page);
  const email = uniqueEmail("e2e05");
  const option = config.options.find((o) => o.availableTo.includes("EXTERNAL"));
  const created = await page.request.post("/api/registrations", {
    data: {
      type: "EXTERNAL",
      firstName: "Eva",
      lastName: "Horvat",
      email,
      organization: "Export Test Org",
      optionIds: option ? [option.id] : [],
      consents: config.consents.filter((c) => c.mandatory).map((c) => c.id),
      recaptchaToken: "test-mode-token",
    },
  });
  expect(created.status()).toBe(201);

  await page.goto("/");
  const download = await page.request.get("/api/organizer/registrations.xlsx", {
    headers: { Authorization: organizerAuthorization() },
  });
  const registrations = Number(sql("SELECT count(*) FROM registration")[0][0]);

  expect(download.status()).toBe(200);
  expect(download.headers()["content-type"]).toContain(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  );
  expect(download.headers()["content-disposition"]).toContain("attachment");
  const workbook = readWorkbook(await download.body());
  expect(registrations).toBeGreaterThan(0);
  expect(workbook.rowCount).toBe(registrations + 1);
  expect(workbook.texts).toContain(email);

  const refused = await page.request.get("/api/organizer/registrations.xlsx");
  expect(refused.status()).toBe(401);
  const refusedBody = (await refused.body()).toString("utf8");
  expect(refusedBody).not.toContain(email);
  expect(refused.headers()["content-type"] ?? "").not.toContain(
    "spreadsheetml",
  );
});
