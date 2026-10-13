import { expect, test } from "@playwright/test";
import {
  CONFERENCE_NAME,
  attachment,
  awaitMessages,
  confirmConsentAndCaptcha,
  fillFields,
  jsonCopy,
  literal,
  sql,
  unique,
} from "./support";

// Shipped configuration (docs/02_contracts/conference.json): one option per category for externals.
const OPTIONS = {
  Workshops: "Workshop: Cloud-native architecture",
  Events: "Welcome reception",
  Meals: "Lunch, day 1",
  "Other activities": "Guided city tour",
};

test("E2E-01 external participant registers (AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01, AC-007-02)", async ({
  page,
}) => {
  const run = unique();
  const firstName = "Eva";
  const lastName = `Zunanja${run}`;
  const email = `eva.${run}@e2e.example`;

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await expect(page.getByLabel("Student ID", { exact: true })).toHaveCount(0);
  await fillFields(page, {
    "First name": firstName,
    "Last name": lastName,
    Email: email,
    "Organization / institution": "Zavod Primer",
  });
  for (const [legend, option] of Object.entries(OPTIONS)) {
    await page
      .getByRole("group", { name: legend })
      .getByRole("checkbox", { name: option })
      .check();
  }
  await confirmConsentAndCaptcha(page);
  await page.getByRole("button", { name: "Register" }).click();

  // In-application confirmation (AC-004-01)
  await expect(
    page.getByRole("heading", { level: 2, name: "Registration received" }),
  ).toBeVisible();
  await expect(
    page.getByText(`Thank you, ${firstName} ${lastName}.`),
  ).toBeVisible();
  const selected = page.getByRole("list", { name: "Selected options" });
  for (const option of Object.values(OPTIONS)) {
    await expect(selected.getByText(option, { exact: true })).toBeVisible();
  }
  await expect(page.getByRole("button", { name: "Register" })).toHaveCount(0);

  // Database row and options (AC-005-01)
  const rows = sql(
    `SELECT id, type, first_name, last_name, organization FROM registration WHERE email = ${literal(email)}`,
  );
  expect(rows).toHaveLength(1);
  const [id, type, storedFirst, storedLast, organization] = rows[0];
  expect([type, storedFirst, storedLast, organization]).toEqual([
    "EXTERNAL",
    firstName,
    lastName,
    "Zavod Primer",
  ]);
  const optionIds = sql(
    `SELECT option_id FROM registration_option WHERE registration_id = ${literal(id)} ORDER BY option_id`,
  ).map((r) => r[0]);
  expect(optionIds).toEqual([
    "ev-welcome",
    "meal-lunch-1",
    "ot-tour",
    "ws-cloud",
  ]);

  // JSON copy (AC-005-02)
  const copy = jsonCopy(id);
  expect(copy).toMatchObject({
    id,
    type: "EXTERNAL",
    firstName,
    lastName,
    email,
  });

  // Participant and organizer emails (AC-006-01, AC-007-01, AC-007-02)
  const messages = await awaitMessages(lastName, 2);
  const participant = messages.filter((m) =>
    m.To.some((t) => t.Address === email),
  );
  expect(participant).toHaveLength(1);
  expect(participant[0].Subject).toContain(CONFERENCE_NAME);
  expect(participant[0].Text).toContain(`${firstName} ${lastName}`);
  for (const option of Object.values(OPTIONS)) {
    expect(participant[0].Text).toContain(option);
  }
  const organizer = messages.filter((m) =>
    m.Subject.startsWith("New registration"),
  );
  expect(organizer.length).toBeGreaterThanOrEqual(1);
  for (const message of organizer) {
    expect(message.Text).toContain(email);
    const json = message.Attachments.find(
      (a) => a.FileName === `registration-${id}.json`,
    );
    expect(json, "JSON attachment").toBeDefined();
    expect(JSON.parse(await attachment(message, json!.PartID))).toEqual(copy);
  }
});
