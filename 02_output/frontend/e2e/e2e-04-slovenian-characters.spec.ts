import { expect, test } from "@playwright/test";
import {
  attachment,
  awaitMessages,
  confirmConsentAndCaptcha,
  fillFields,
  jsonCopy,
  literal,
  organizerAuthorization,
  sql,
  unique,
  unzip,
  workbookStrings,
} from "./support";

test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03, NFR-01)", async ({
  page,
  request,
}) => {
  const run = unique();
  const firstName = "Žiga Črt";
  const lastName = `Šuštaršič-Čeč ŽŠČ${run}`;
  const organization = "Fakulteta za računalništvo, čšž ČŠŽ";
  const email = `ziga.${run}@e2e.example`;

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await fillFields(page, {
    "First name": firstName,
    "Last name": lastName,
    Email: email,
    "Organization / institution": organization,
  });
  await confirmConsentAndCaptcha(page);
  await page.getByRole("button", { name: "Register" }).click();

  // Page confirmation
  await expect(
    page.getByText(`Thank you, ${firstName} ${lastName}.`),
  ).toBeVisible();

  // Database row
  const rows = sql(
    `SELECT id, first_name, last_name, organization FROM registration WHERE email = ${literal(email)}`,
  );
  expect(rows).toHaveLength(1);
  const [id, storedFirst, storedLast, storedOrganization] = rows[0];
  expect([storedFirst, storedLast, storedOrganization]).toEqual([
    firstName,
    lastName,
    organization,
  ]);

  // JSON copy
  expect(jsonCopy(id)).toMatchObject({ firstName, lastName, organization });

  // Both emails, including the organizer's JSON attachment
  // Mailpit search does not match non-ASCII terms, so search by the ASCII run suffix.
  const messages = await awaitMessages(run, 2);
  const participant = messages.find((m) =>
    m.To.some((t) => t.Address === email),
  );
  expect(participant?.Text).toContain(`${firstName} ${lastName}`);
  const organizer = messages.find((m) =>
    m.Subject.startsWith("New registration"),
  );
  expect(organizer?.Text).toContain(firstName);
  expect(organizer?.Text).toContain(lastName);
  expect(organizer?.Text).toContain(organization);
  const json = organizer!.Attachments.find(
    (a) => a.FileName === `registration-${id}.json`,
  );
  expect(JSON.parse(await attachment(organizer!, json!.PartID))).toMatchObject({
    firstName,
    lastName,
    organization,
  });

  // Exported workbook
  const exported = await request.get("/api/registrations/export", {
    headers: { Authorization: organizerAuthorization() },
  });
  expect(exported.status()).toBe(200);
  const strings = workbookStrings(unzip(await exported.body()));
  expect(strings).toContain(firstName);
  expect(strings).toContain(lastName);
  expect(strings).toContain(organization);
});
