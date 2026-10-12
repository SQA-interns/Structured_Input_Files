import { expect, test } from "@playwright/test";
import {
  formConfig,
  giveConsentsAndPassCaptcha,
  jsonCopy,
  registrationMails,
  organizerAuthorization,
  readWorkbook,
  sql,
  sqlText,
  tickOnePerCategory,
  uniqueEmail,
} from "./support/stack";

// E2E-04 covers AC-005-04, AC-008-03, NFR-01.
test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e04");
  const first = "Čedomir Žiga";
  const last = "Šuštaršič";
  const institution =
    "Univerza v Ljubljani, Fakulteta za računalništvo (ČŠŽ čšž)";
  const config = await formConfig(page);

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByLabel("First name", { exact: true }).fill(first);
  await page.getByLabel("Last name", { exact: true }).fill(last);
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page
    .getByLabel("Organization / institution", { exact: true })
    .fill(institution);
  await tickOnePerCategory(page);
  await giveConsentsAndPassCaptcha(page, config);
  await page.getByRole("button", { name: "Register" }).click();

  await expect(page.getByRole("status")).toContainText(
    `Thank you, ${first} ${last}.`,
  );

  const rows = sql(
    `SELECT id, first_name, last_name, organization FROM registration WHERE email = ${sqlText(email)}`,
  );
  expect(rows).toHaveLength(1);
  const [id, ...stored] = rows[0];
  expect(stored).toEqual([first, last, institution]);

  const copy = JSON.parse(jsonCopy(id)) as {
    firstName: string;
    lastName: string;
    organization: string;
  };
  expect(copy).toMatchObject({
    firstName: first,
    lastName: last,
    organization: institution,
  });

  const mails = await registrationMails(email);
  expect(mails.participant.Text).toContain(first);
  expect(mails.participant.Text).toContain(last);
  for (const organizerMail of mails.organizer) {
    expect(organizerMail.Text).toContain(first);
    expect(organizerMail.Text).toContain(last);
    expect(organizerMail.Text).toContain(institution);
  }

  const exported = await page.request.get("/api/organizer/registrations.xlsx", {
    headers: { Authorization: organizerAuthorization() },
  });
  expect(exported.status()).toBe(200);
  const workbook = readWorkbook(await exported.body());
  expect(workbook.texts).toEqual(
    expect.arrayContaining([first, last, institution]),
  );
});
