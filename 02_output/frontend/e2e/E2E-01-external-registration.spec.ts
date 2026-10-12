import { expect, test } from "@playwright/test";
import {
  formConfig,
  giveConsentsAndPassCaptcha,
  jsonCopy,
  mailPart,
  registrationMails,
  sql,
  sqlText,
  tickOnePerCategory,
  uniqueEmail,
} from "./support/stack";

// E2E-01 covers AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01,
// AC-007-02, DoD-P01 to DoD-P03.
test("E2E-01 external participant registers (AC-001-01, AC-001-08, AC-004-01)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e01");
  const config = await formConfig(page);

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByLabel("First name", { exact: true }).fill("Ana");
  await page.getByLabel("Last name", { exact: true }).fill("Novak");
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page
    .getByLabel("Organization / institution", { exact: true })
    .fill("Acme Testing Ltd");
  const options = await tickOnePerCategory(page);
  expect(options).toHaveLength(4);
  await giveConsentsAndPassCaptcha(page, config);
  await page.getByRole("button", { name: "Register" }).click();

  const status = page.getByRole("status");
  await expect(
    status.getByRole("heading", { name: "Registration received" }),
  ).toBeVisible();
  await expect(status).toContainText("Thank you, Ana Novak.");
  for (const name of options) {
    await expect(
      status.getByRole("list", { name: "Selected options" }),
    ).toContainText(name);
  }
  await expect(
    page.getByRole("form", { name: "Registration form" }),
  ).toHaveCount(0);

  const rows = sql(
    `SELECT id, registration_type, first_name, last_name, organization FROM registration WHERE email = ${sqlText(email)}`,
  );
  expect(rows).toHaveLength(1);
  const [id, type, first, last, organization] = rows[0];
  expect([type, first, last, organization]).toEqual([
    "EXTERNAL",
    "Ana",
    "Novak",
    "Acme Testing Ltd",
  ]);
  expect(
    sql(
      `SELECT count(*) FROM registration_option WHERE registration_id = '${id}'`,
    ),
  ).toEqual([["4"]]);
  expect(
    sql(
      `SELECT consent_id FROM registration_consent WHERE registration_id = '${id}'`,
    ).flat(),
  ).toEqual(
    expect.arrayContaining(
      config.consents.filter((c) => c.mandatory).map((c) => c.id),
    ),
  );

  const copyText = jsonCopy(id);
  const copy = JSON.parse(copyText) as {
    id: string;
    email: string;
    type: string;
  };
  expect(copy).toMatchObject({ id, email, type: "EXTERNAL" });

  const mails = await registrationMails(email);
  expect(mails.participant.Text).toContain("Ana");
  expect(mails.participant.Text).toContain("Novak");
  for (const organizerMail of mails.organizer) {
    const attachment = organizerMail.Attachments.find(
      (a) => a.FileName === `registration-${id}.json`,
    );
    expect(attachment, "JSON attachment").toBeDefined();
    const attached = await mailPart(organizerMail.ID, attachment!.PartID);
    expect(attached.toString("utf8")).toBe(copyText);
  }
});
