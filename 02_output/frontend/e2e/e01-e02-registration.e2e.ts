import { expect, test } from "@playwright/test";
import {
  attachment,
  awaitMessage,
  clearMailpit,
  conferenceConfig,
  jsonCopy,
  onePerCategory,
  optionIdsOf,
  optionsFor,
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

test.beforeAll(async () => {
  await clearMailpit();
});

test("E2E-01 external participant registers (AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01, AC-007-02)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e.external");
  const options = onePerCategory("EXTERNAL");
  expect(options.map((o) => o.category)).toEqual(["WORKSHOP", "EVENT", "MEAL", "OTHER"]);

  await openForm(page, "External participant");
  await expect(page.getByRole("textbox")).toHaveCount(4);
  await fill(page, {
    "First name": "Maja",
    "Last name": "Zupan",
    Email: email,
    "Organization / institution": "Arnes",
  });
  await selectOptions(page, options);
  await consentAndCaptcha(page);
  await register(page);

  await expectConfirmation(page, ["Maja", "Zupan", ...options.map((o) => o.name)]);

  const rows = registrationsByEmail(email);
  expect(rows).toHaveLength(1);
  const row = rows[0];
  expect(row).toMatchObject({
    type: "EXTERNAL",
    first_name: "Maja",
    last_name: "Zupan",
    organization: "Arnes",
  });
  expect(optionIdsOf(row.id).sort()).toEqual(options.map((o) => o.id).sort());

  const copy = jsonCopy(row.id);
  expect(JSON.parse(copy.toString("utf8"))).toMatchObject({ id: row.id, type: "EXTERNAL", email });

  const confirmation = await awaitMessage(email, "Maja");
  expect(confirmation.Text).toContain(conferenceConfig().conferenceName);
  for (const address of organizer().emails) {
    const notification = await awaitMessage(address, row.id);
    expect(await attachment(notification, `registration-${row.id}.json`)).toEqual(copy);
  }
});

test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({ page }) => {
  const email = uniqueEmail("e2e.student");
  const options = onePerCategory("STUDENT");

  await openForm(page, "Student");
  await expect(page.getByRole("textbox")).toHaveCount(6);
  for (const name of [
    "First name",
    "Last name",
    "Email",
    "Study institution",
    "Study programme",
    "Student ID",
  ]) {
    await expect(page.getByRole("textbox", { name, exact: true })).toBeVisible();
  }
  for (const option of conferenceConfig().options) {
    const shown = optionsFor("STUDENT").some((o) => o.id === option.id);
    await expect(page.getByRole("checkbox", { name: option.name, exact: true })).toHaveCount(
      shown ? 1 : 0,
    );
  }

  await fill(page, {
    "First name": "Nik",
    "Last name": "Horvat",
    Email: email,
    "Study institution": "Univerza v Mariboru",
    "Study programme": "Informatika",
    "Student ID": "E1234567",
  });
  await selectOptions(page, options);
  await consentAndCaptcha(page);
  await register(page);

  await expectConfirmation(page, ["Nik", "Horvat", ...options.map((o) => o.name)]);
  const rows = registrationsByEmail(email);
  expect(rows).toHaveLength(1);
  expect(rows[0]).toMatchObject({
    type: "STUDENT",
    study_institution: "Univerza v Mariboru",
    study_programme: "Informatika",
    student_id: "E1234567",
  });
  await awaitMessage(email, "Nik");
  for (const address of organizer().emails) {
    await awaitMessage(address, rows[0].id);
  }
});
