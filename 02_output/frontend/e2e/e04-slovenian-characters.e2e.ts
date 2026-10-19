import { expect, test } from "@playwright/test";
import {
  attachment,
  awaitMessage,
  basicAuth,
  jsonCopy,
  onePerCategory,
  organizer,
  registrationsByEmail,
  uniqueEmail,
} from "./support/stack";
import {
  consentAndCaptcha,
  expectConfirmation,
  fill,
  openForm,
  register,
  selectOptions,
} from "./support/form";
import { readWorkbookRows } from "./support/xlsx";

test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03)", async ({
  page,
  request,
}) => {
  const email = uniqueEmail("e2e.sumniki");
  const firstName = "Čedomir Žiga";
  const lastName = "Šuštaršič Žužek";
  const institution = "Fakulteta za računalništvo, ČŠŽ čšž";

  await openForm(page, "External participant");
  await fill(page, {
    "First name": firstName,
    "Last name": lastName,
    Email: email,
    "Organization / institution": institution,
  });
  await selectOptions(page, onePerCategory("EXTERNAL").slice(0, 1));
  await consentAndCaptcha(page);
  await register(page);

  await expectConfirmation(page, [firstName, lastName]);

  const rows = registrationsByEmail(email);
  expect(rows).toHaveLength(1);
  expect(rows[0]).toMatchObject({
    first_name: firstName,
    last_name: lastName,
    organization: institution,
  });
  const copyBytes = jsonCopy(rows[0].id);
  expect(JSON.parse(copyBytes.toString("utf8"))).toMatchObject({
    firstName,
    lastName,
    organization: institution,
  });

  const participantMail = await awaitMessage(email, firstName);
  expect(participantMail.Text).toContain(lastName);
  const { username, password, emails } = organizer();
  const organizerMail = await awaitMessage(emails[0], rows[0].id);
  expect(organizerMail.Text).toContain(firstName);
  expect(organizerMail.Text).toContain(lastName);
  expect(organizerMail.Text).toContain(institution);
  expect(await attachment(organizerMail, `registration-${rows[0].id}.json`)).toEqual(copyBytes);

  const response = await request.get("/api/registrations/export", {
    headers: { Authorization: basicAuth(username, password) },
  });
  expect(response.status()).toBe(200);
  const exported = readWorkbookRows(await response.body()).find((r) => r[0] === rows[0].id);
  expect(exported).toBeDefined();
  expect(exported![3]).toBe(firstName);
  expect(exported![4]).toBe(lastName);
  expect(exported![6]).toBe(institution);
});
