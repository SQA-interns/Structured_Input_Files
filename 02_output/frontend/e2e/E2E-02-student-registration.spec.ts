import { expect, test } from "@playwright/test";
import {
  CATEGORY_GROUPS,
  formConfig,
  giveConsentsAndPassCaptcha,
  jsonCopy,
  registrationMails,
  sql,
  sqlText,
  tickOnePerCategory,
  uniqueEmail,
} from "./support/stack";

// E2E-02 covers AC-002-01, AC-002-07, AC-004-01, DoD-P01 to DoD-P03.
test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e02");
  const config = await formConfig(page);

  await page.goto("/");
  await page.getByRole("radio", { name: "Student" }).check();

  const textboxes = page.getByRole("textbox");
  await expect(textboxes).toHaveCount(6);
  for (const label of [
    "First name",
    "Last name",
    "Email",
    "Study institution",
    "Study programme",
    "Student ID",
  ]) {
    await expect(page.getByLabel(label, { exact: true })).toBeVisible();
  }
  await expect(
    page.getByLabel("Organization / institution", { exact: true }),
  ).toHaveCount(0);

  const shown: string[] = [];
  for (const group of CATEGORY_GROUPS) {
    const boxes = page
      .getByRole("group", { name: group })
      .getByRole("checkbox");
    for (const box of await boxes.all()) {
      shown.push(
        (
          await box.evaluate(
            (el) =>
              (el as HTMLInputElement).labels?.[0]?.textContent ??
              el.getAttribute("aria-label") ??
              "",
          )
        ).trim(),
      );
    }
  }
  const studentOptions = config.options
    .filter((o) => o.availableTo.includes("STUDENT"))
    .map((o) => o.name);
  expect(shown.sort()).toEqual([...studentOptions].sort());

  await page.getByLabel("First name", { exact: true }).fill("Bor");
  await page.getByLabel("Last name", { exact: true }).fill("Kranjc");
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page
    .getByLabel("Study institution", { exact: true })
    .fill("University of Maribor");
  await page
    .getByLabel("Study programme", { exact: true })
    .fill("Computer Science");
  await page.getByLabel("Student ID", { exact: true }).fill("63210001");
  const options = await tickOnePerCategory(page);
  await giveConsentsAndPassCaptcha(page, config);
  await page.getByRole("button", { name: "Register" }).click();

  const status = page.getByRole("status");
  await expect(
    status.getByRole("heading", { name: "Registration received" }),
  ).toBeVisible();
  await expect(status).toContainText("Thank you, Bor Kranjc.");
  for (const name of options) {
    await expect(
      status.getByRole("list", { name: "Selected options" }),
    ).toContainText(name);
  }

  const rows = sql(
    `SELECT id, registration_type, study_institution, study_programme, student_id FROM registration WHERE email = ${sqlText(email)}`,
  );
  expect(rows).toHaveLength(1);
  const [id, ...values] = rows[0];
  expect(values).toEqual([
    "STUDENT",
    "University of Maribor",
    "Computer Science",
    "63210001",
  ]);
  expect(JSON.parse(jsonCopy(id))).toMatchObject({
    id,
    type: "STUDENT",
    studentId: "63210001",
  });

  const mails = await registrationMails(email);
  expect(mails.participant.Text).toContain("Bor");
  for (const organizerMail of mails.organizer) {
    expect(organizerMail.Attachments.map((a) => a.FileName)).toContain(
      `registration-${id}.json`,
    );
  }
});
