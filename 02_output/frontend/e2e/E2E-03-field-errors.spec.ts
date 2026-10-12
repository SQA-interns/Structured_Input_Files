import { expect, test } from "@playwright/test";
import {
  formConfig,
  giveConsentsAndPassCaptcha,
  sql,
  sqlText,
  tickOnePerCategory,
  uniqueEmail,
} from "./support/stack";

// E2E-03 covers AC-004-02, AC-004-03, NFR-03.
test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e03");
  const config = await formConfig(page);
  const registrationRequests: string[] = [];
  page.on("request", (request) => {
    if (request.url().includes("/api/registrations")) {
      registrationRequests.push(request.method());
    }
  });

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByLabel("Last name", { exact: true }).fill("Novak");
  await page.getByLabel("Email", { exact: true }).fill("not-an-email");
  await page
    .getByLabel("Organization / institution", { exact: true })
    .fill("Acme Testing Ltd");
  await tickOnePerCategory(page);
  await giveConsentsAndPassCaptcha(page, config);
  await page.getByRole("button", { name: "Register" }).click();

  const firstName = page.getByLabel("First name", { exact: true });
  const emailField = page.getByLabel("Email", { exact: true });
  await expect(firstName).toHaveAccessibleDescription(/\S/);
  await expect(emailField).toHaveAccessibleDescription(/\S/);
  await expect(page.locator("#firstName-error")).toBeVisible();
  await expect(page.locator("#email-error")).toBeVisible();
  expect(registrationRequests).toHaveLength(0);

  // Now a submission the browser accepts but the backend rejects: an option that does not exist.
  await firstName.fill("Ana");
  await emailField.fill(email);
  await page.route("**/api/registrations", async (route) => {
    const body = JSON.parse(route.request().postData() ?? "{}") as {
      optionIds?: string[];
    };
    body.optionIds = [...(body.optionIds ?? []), "option-that-does-not-exist"];
    await route.continue({ postData: JSON.stringify(body) });
  });
  if (
    !(await page
      .getByRole("checkbox", { name: "I am not a robot (test mode)" })
      .isChecked())
  ) {
    await page
      .getByRole("checkbox", { name: "I am not a robot (test mode)" })
      .check();
  }
  const answer = page.waitForResponse((r) =>
    r.url().includes("/api/registrations"),
  );
  await page.getByRole("button", { name: "Register" }).click();
  expect((await answer).status()).toBe(400);

  const optionsError = page.locator("#optionIds-error");
  await expect(optionsError).toBeVisible();
  const message = ((await optionsError.textContent()) ?? "").trim();
  expect(message.length).toBeGreaterThan(0);
  await expect(
    page
      .getByRole("group", { name: "Workshops" })
      .getByRole("checkbox")
      .first(),
  ).toHaveAccessibleDescription(message);
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(firstName).toHaveValue("Ana");
  await expect(emailField).toHaveValue(email);
  await expect(
    page.getByLabel("Organization / institution", { exact: true }),
  ).toHaveValue("Acme Testing Ltd");
  expect(
    sql(`SELECT count(*) FROM registration WHERE email = ${sqlText(email)}`),
  ).toEqual([["0"]]);
});
