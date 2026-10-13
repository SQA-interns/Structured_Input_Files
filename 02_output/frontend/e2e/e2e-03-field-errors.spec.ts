import { expect, test } from "@playwright/test";
import {
  confirmConsentAndCaptcha,
  fillFields,
  literal,
  sql,
  unique,
} from "./support";

test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03, NFR-03)", async ({
  page,
}) => {
  const run = unique();
  const email = `rejected.${run}@e2e.example`;
  const submissions: string[] = [];
  page.on("request", (request) => {
    if (
      request.method() === "POST" &&
      request.url().endsWith("/api/registrations")
    ) {
      submissions.push(request.url());
    }
  });

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await fillFields(page, {
    "Last name": `Napaka${run}`,
    Email: "not-an-email",
    "Organization / institution": "Zavod Primer",
  });
  await confirmConsentAndCaptcha(page);

  // 1. Client-side validation: errors next to the fields, no request (AC-004-03)
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page.getByTestId("error-firstName")).toHaveText(
    "This field is required.",
  );
  await expect(page.getByTestId("error-email")).toHaveText(
    "Enter a valid email address.",
  );
  await expect(page.getByLabel("First name", { exact: true })).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  expect(submissions).toHaveLength(0);

  // 2. Backend rejection: the request is altered to carry an inactive option (ws-legacy)
  await fillFields(page, { "First name": "Nina", Email: email });
  await page.getByRole("checkbox", { name: "Guided city tour" }).check();
  await page.route("**/api/registrations", async (route) => {
    const body = JSON.parse(route.request().postData() ?? "{}");
    body.optionIds = [...(body.optionIds ?? []), "ws-legacy"];
    await route.continue({ postData: JSON.stringify(body) });
  });
  const response = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/registrations") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Register" }).click();
  expect((await response).status()).toBe(400);

  await expect(page.getByTestId("error-options")).toContainText("ws-legacy");
  await expect(page.getByTestId("error-firstName")).toHaveCount(0);
  await expect(
    page.getByRole("heading", { name: "Registration received" }),
  ).toHaveCount(0);
  await expect(page.getByLabel("First name", { exact: true })).toHaveValue(
    "Nina",
  );
  await expect(page.getByLabel("Email", { exact: true })).toHaveValue(email);
  await expect(
    page.getByRole("checkbox", { name: "Guided city tour" }),
  ).toBeChecked();
  expect(
    sql(`SELECT id FROM registration WHERE email = ${literal(email)}`),
  ).toHaveLength(0);
});
