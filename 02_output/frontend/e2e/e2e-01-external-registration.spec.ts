import { expect, test } from "@playwright/test";
import {
  OPTION_GROUPS,
  PRIVACY_CONSENT,
  addressedTo,
  attachmentBytes,
  awaitMessages,
  jsonCopy,
  organizerEmails,
  sql,
  submitAndCapture,
  uniqueEmail,
} from "./support/stack";

test("E2E-01 external participant registers (AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01, AC-007-02)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e-external");

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByRole("textbox", { name: "First name" }).fill("Ana");
  await page.getByRole("textbox", { name: "Last name" }).fill("Novak");
  await page.getByRole("textbox", { name: "Email" }).fill(email);
  await page.getByRole("textbox", { name: "Organization / institution" }).fill("Institut E2E");

  const selected: string[] = [];
  for (const group of OPTION_GROUPS) {
    const option = page.getByRole("group", { name: group }).getByRole("checkbox").first();
    await option.check();
    selected.push(
      await option.evaluate(
        (el) => (el as HTMLInputElement).labels?.[0]?.textContent?.trim() ?? "",
      ),
    );
  }
  await page.getByRole("checkbox", { name: PRIVACY_CONSENT }).check();
  await page.getByRole("checkbox", { name: "I am not a robot (test mode)" }).check();

  const { status, body } = await submitAndCapture(page);
  expect(status).toBe(201);
  const id = (body as { id: string }).id;

  // The page shows the confirmation instead of the form.
  await expect(page.getByRole("heading", { name: "Registration confirmed" })).toBeVisible();
  await expect(page.locator("body")).toContainText("Ana Novak");
  await expect(page.getByRole("button", { name: "Register" })).toHaveCount(0);

  // The database holds the registration with its options and consent.
  expect(
    sql(
      `SELECT type, first_name, last_name, email, organization FROM registration WHERE id = '${id}'`,
    ),
  ).toEqual([["EXTERNAL", "Ana", "Novak", email, "Institut E2E"]]);
  expect(
    sql(`SELECT option_name FROM registration_option WHERE registration_id = '${id}'`)
      .map((r) => r[0])
      .sort(),
  ).toEqual([...selected].sort());
  expect(
    sql(`SELECT consent_id FROM registration_consent WHERE registration_id = '${id}'`).map(
      (r) => r[0],
    ),
  ).toContain("privacy");

  // The JSON copy exists.
  const copy = jsonCopy(id);
  expect(copy).not.toBeNull();
  const parsed = JSON.parse(copy!.toString("utf8"));
  expect(parsed.id).toBe(id);
  expect(parsed.email).toBe(email);

  // The participant email is in Mailpit.
  const participantMail = await awaitMessages((m) => addressedTo(m, email));
  expect(participantMail).toHaveLength(1);

  // Every organizer address got a notification with the JSON copy attached.
  const organizers = organizerEmails();
  const organizerMail = await awaitMessages(
    (m) => m.Subject.includes(id) && organizers.some((o) => addressedTo(m, o)),
    organizers.length,
  );
  for (const organizer of organizers) {
    const message = organizerMail.find((m) => addressedTo(m, organizer));
    expect(message, `notification for ${organizer}`).toBeDefined();
    const attachment = message!.Attachments.find((a) => a.FileName.endsWith(".json"));
    expect(attachment).toBeDefined();
    expect((await attachmentBytes(message!, attachment!.PartID)).equals(copy!)).toBe(true);
  }
});
