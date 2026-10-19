import { expect, test } from "@playwright/test";
import {
  conferenceConfig,
  onePerCategory,
  registrationsByEmail,
  uniqueEmail,
} from "./support/stack";
import { consentAndCaptcha, fill, openForm, register, selectOptions } from "./support/form";

test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e.errors");
  const posted: string[] = [];
  page.on("request", (request) => {
    if (request.method() === "POST" && new URL(request.url()).pathname === "/api/registrations") {
      posted.push(request.url());
    }
  });

  await openForm(page, "External participant");
  await fill(page, {
    "Last name": "Kovač",
    Email: "not-an-email@",
    "Organization / institution": "Arnes",
  });
  await selectOptions(page, onePerCategory("EXTERNAL").slice(0, 1));
  await consentAndCaptcha(page);
  await register(page);

  const firstName = page.getByRole("textbox", { name: "First name", exact: true });
  const emailBox = page.getByRole("textbox", { name: "Email", exact: true });
  await expect(firstName).toHaveAttribute("aria-invalid", "true");
  await expect(firstName).toHaveAttribute("aria-describedby", "error-firstName");
  await expect(page.locator("#error-firstName")).not.toBeEmpty();
  await expect(emailBox).toHaveAttribute("aria-invalid", "true");
  await expect(emailBox).toHaveAttribute("aria-describedby", "error-email");
  await expect(page.locator("#error-email")).not.toBeEmpty();
  expect(posted).toHaveLength(0);

  const inactive = conferenceConfig().options.find((o) => !o.active);
  expect(inactive).toBeDefined();
  await page.route("**/api/registrations", async (route) => {
    const body = JSON.parse(route.request().postData() ?? "{}");
    body.optionIds = [...(body.optionIds ?? []), inactive!.id];
    await route.continue({ postData: JSON.stringify(body) });
  });
  await fill(page, { "First name": "Tina", Email: email });
  await register(page);

  await expect(page.locator("#error-optionIds")).not.toBeEmpty();
  await expect(page.locator('[aria-describedby~="error-optionIds"]')).toHaveCount(1);
  expect(posted).toHaveLength(1);
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(firstName).toHaveValue("Tina");
  await expect(page.getByRole("textbox", { name: "Last name", exact: true })).toHaveValue("Kovač");
  await expect(emailBox).toHaveValue(email);
  expect(registrationsByEmail(email)).toHaveLength(0);
});
