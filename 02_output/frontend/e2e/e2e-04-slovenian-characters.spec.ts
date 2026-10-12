import { expect, test } from "@playwright/test";
import {
  PRIVACY_CONSENT,
  addressedTo,
  awaitMessages,
  jsonCopy,
  organizerAuthorization,
  organizerEmails,
  readWorkbookRows,
  sql,
  submitAndCapture,
  uniqueEmail,
} from "./support/stack";

const FIRST = "Čedomir Žiga";
const LAST = "Šuštar-Čičić žšč";
const INSTITUTION = "Univerza v Ljubljani, Fakulteta ČŠŽ čšž";

test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03, NFR-01)", async ({
  page,
  request,
}) => {
  const email = uniqueEmail("e2e-sl");

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByRole("textbox", { name: "First name" }).fill(FIRST);
  await page.getByRole("textbox", { name: "Last name" }).fill(LAST);
  await page.getByRole("textbox", { name: "Email" }).fill(email);
  await page.getByRole("textbox", { name: "Organization / institution" }).fill(INSTITUTION);
  await page.getByRole("checkbox", { name: PRIVACY_CONSENT }).check();
  await page.getByRole("checkbox", { name: "I am not a robot (test mode)" }).check();

  const { status, body } = await submitAndCapture(page);
  expect(status).toBe(201);
  const id = (body as { id: string }).id;

  // Page confirmation.
  await expect(page.getByRole("heading", { name: "Registration confirmed" })).toBeVisible();
  await expect(page.locator("body")).toContainText(`${FIRST} ${LAST}`);

  // Database row.
  expect(
    sql(`SELECT first_name, last_name, organization FROM registration WHERE id = '${id}'`),
  ).toEqual([[FIRST, LAST, INSTITUTION]]);

  // JSON copy.
  const copy = JSON.parse(jsonCopy(id)!.toString("utf8"));
  expect([copy.firstName, copy.lastName, copy.organization]).toEqual([FIRST, LAST, INSTITUTION]);

  // Both emails.
  const participantMail = await awaitMessages((m) => addressedTo(m, email));
  expect(participantMail).toHaveLength(1);
  expect(participantMail[0].Text).toContain(FIRST);
  expect(participantMail[0].Text).toContain(LAST);
  const organizers = organizerEmails();
  const notifications = await awaitMessages(
    (m) => m.Subject.includes(id) && organizers.some((o) => addressedTo(m, o)),
    organizers.length,
  );
  expect(notifications.length).toBeGreaterThanOrEqual(1);
  for (const message of notifications) {
    expect(message.Text).toContain(FIRST);
    expect(message.Text).toContain(LAST);
    expect(message.Text).toContain(INSTITUTION);
  }

  // Exported workbook.
  const exported = await request.get("/api/export", {
    headers: { Authorization: organizerAuthorization() },
  });
  expect(exported.status()).toBe(200);
  const rows = readWorkbookRows(Buffer.from(await exported.body()));
  const row = rows.find((r) => r.includes(id));
  expect(row, "workbook row of the registration").toBeDefined();
  expect(row).toContain(FIRST);
  expect(row).toContain(LAST);
  expect(row).toContain(INSTITUTION);
});
