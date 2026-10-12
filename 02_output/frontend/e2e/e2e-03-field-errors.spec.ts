import { expect, test } from "@playwright/test";
import { PRIVACY_CONSENT, sql, submitAndCapture, uniqueEmail } from "./support/stack";

test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03, NFR-03)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e-errors");
  const registrationRequests: string[] = [];
  page.on("request", (r) => {
    if (r.url().endsWith("/api/registrations") && r.method() === "POST") {
      registrationRequests.push(r.url());
    }
  });

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByRole("textbox", { name: "Last name" }).fill("Novak");
  await page.getByRole("textbox", { name: "Email" }).fill("not-an-email");
  await page.getByRole("textbox", { name: "Organization / institution" }).fill("Institut E2E");
  await page.getByRole("checkbox", { name: "I am not a robot (test mode)" }).check();

  // Client-side: empty required field and invalid email, no request reaches the backend.
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page.locator("[id='firstName-error']")).toHaveText("This field is required.");
  await expect(page.locator("[id='email-error']")).toHaveText("Enter a valid email address.");
  await expect(page.getByRole("textbox", { name: "First name" })).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  await page.waitForTimeout(500);
  expect(registrationRequests).toHaveLength(0);

  // Backend-side: the mandatory consent is missing, the backend rejects the submission.
  await page.getByRole("textbox", { name: "First name" }).fill("Ana");
  await page.getByRole("textbox", { name: "Email" }).fill(email);
  await expect(page.getByRole("checkbox", { name: PRIVACY_CONSENT })).not.toBeChecked();
  const { status, body } = await submitAndCapture(page);
  expect(status).toBe(400);
  const fields = (body as { fieldErrors: { field: string }[] }).fieldErrors.map((e) => e.field);
  expect(fields).toContain("consents.privacy");

  const consentError = page.locator("[id='consents.privacy-error']");
  await expect(consentError).toBeVisible();
  await expect(consentError).not.toHaveText("");
  await expect(page.getByRole("checkbox", { name: PRIVACY_CONSENT })).toHaveAttribute(
    "aria-describedby",
    /consents\.privacy-error/,
  );
  await expect(page.getByRole("heading", { name: "Registration confirmed" })).toHaveCount(0);
  await expect(page.getByRole("textbox", { name: "First name" })).toHaveValue("Ana");
  await expect(page.getByRole("textbox", { name: "Last name" })).toHaveValue("Novak");
  await expect(page.getByRole("textbox", { name: "Email" })).toHaveValue(email);
  await expect(page.getByRole("textbox", { name: "Organization / institution" })).toHaveValue(
    "Institut E2E",
  );
  expect(sql(`SELECT count(*) FROM registration WHERE email = '${email}'`)).toEqual([["0"]]);
});
